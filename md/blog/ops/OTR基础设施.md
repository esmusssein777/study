# infrastructure

## pipeline

在 otr-pipeline-as-code 中文件中比如是 as-order 服务，那么对应一个环境有 yaml 文件

```
pipelines:
  aftersales-order-management-int-release:
    environment_variables:
      SERVICE_NAME: aftersales-order-management
      PROJECT_NAME: plus-rwo
      PIPELINE_NAME: aftersales-order-management-int-release
      FORK_RELEASE: True
      GRADLE: "6.6"
    group: plus-rwo-release
    label_template: "${COUNT}"
    locking: off
    # setting materials
    materials:
      app-repo:
        git: https://1532f2acc40ef52d812f451a7c26830405466b1c@git.daimler.com/china/otr-aftersales-order-management.git
        branch: release_wuyong
        destination: app
        shallow_clone: true
      devops-buildtools:
        git: http://oauth2:UiT2hGrZ7LJUZ8EM-iU9@git-tmp.cn.bg.corpintra.net/otr/buildtools.git
        blacklist:
          - "**/*.*"
        branch: master
        auto_update: false
        destination: buildtools
    # setting stages
    stages:
      - code-scan:
          clean_workspace: true
          jobs:
            pmd-verification:
              artifacts:
                - build:
                    source: app/**/build/reports
                    destination: app
              elastic_profile_id: agent-gradle-6.6
              tasks:
                - exec:
                    command: cp
                    arguments:
                      - "-f"
                      - "buildtools/scripts/java_style_check_v2.sh"
                      - "app"
                    run_if: passed
                - exec:
                    command: bash
                    arguments:
                      - "-e"
                      - "java_style_check_v2.sh"
                    working_directory: app
                    run_if: passed
      - test-integration:
          fetch_materials: yes
          clean_workspace: yes
          jobs:
            test:
              artifacts:
                - build:
                    source: app/**/build/reports
                    destination: app
              elastic_profile_id: agent-gradle-6.6
              tasks:
                - exec:
                    command: cp
                    arguments:
                      - "-f"
                      - "buildtools/scripts/java_test_v2.sh"
                      - "buildtools/scripts/publish_contracts_v2.sh"
                      - "app"
                    run_if: passed
                - exec:
                    command: bash
                    arguments:
                      - "-e"
                      - "java_test_v2.sh"
                      - "release_wuyong"
                    working_directory: app
                    run_if: passed
                - exec:
                    command: bash
                    arguments:
                      - "-e"
                      - "publish_contracts_v2.sh"
                      - "release_wuyong"
                    working_directory: app
                    run_if: passed
      - build:
          fetch_materials: yes
          clean_workspace: yes
          jobs:
            build:
              elastic_profile_id: agent-gradle-6.6
              tasks:
                - exec:
                    command: cp
                    arguments:
                      - "-f"
                      - "buildtools/scripts/build_and_publish_image_v3.sh"
                      - "app"
                    run_if: passed
                - exec:
                    command: bash
                    arguments:
                      - "build_and_publish_image_v3.sh"
                      - "release"
                    working_directory: app
                    run_if: passed
      - flyway-integ:
          fetch_materials: yes
          clean_workspace: no
          approval:
            type: manual
            roles:
              - deploy-plus-int
              - ops
          environment_variables:
            ENVIRONMENT: integ
          jobs:
            deploy:
              elastic_profile_id: agent-helm-plus-v3
              tasks:
                - exec:
                    command: cp
                    arguments:
                      - "-f"
                      - "buildtools/job/flyway.yaml"
                      - "app/flyway.yaml"
                    run_if: passed
                - exec:
                    command: cp
                    arguments:
                      - "-f"
                      - "buildtools/scripts/flyway.sh"
                      - "app/flyway.sh"
                    run_if: passed
                - exec:
                    command: bash
                    arguments:
                      - "flyway.sh"
                      - "integ"
                    working_directory: app
                    run_if: passed
      - deploy-integ:
          fetch_materials: yes
          clean_workspace: no
          approval:
            type: manual
            roles:
              - deploy-plus-int
              - ops
          environment_variables:
            ENVIRONMENT: integ
            HEALTHCHECK_URL: /actuator/health
          jobs:
            deploy:
              elastic_profile_id: agent-helm-plus-v3
              tasks:
                - exec:
                    command: cp
                    arguments:
                      - "-r"
                      - "buildtools/otr-deployment-chart"
                      - "app/deployment-chart"
                    run_if: passed
                - exec:
                    command: cp
                    arguments:
                      - "-f"
                      - "buildtools/scripts/deploy.sh"
                      - "app/deploy.sh"
                    run_if: passed
                - exec:
                    command: bash
                    arguments:
                      - "deploy.sh"
                      - "integ"
                    working_directory: app
                    run_if: passed
```

对应了不用阶段需要做的事，pmd scan 阶段复制 `buildtools/scripts/java_style_check_v2.sh` 文件，然后 bash -e 执行脚本。

最后 deploy.sh 执行脚本使用 kubectl 来运行服务

```
#! /usr/bin/env bash

# This script is used to deploy service by kubectl

set -x

ACR_SERVER="iotrmcninfctreg001.azurecr.cn"
RETRY=5
INTERVAL=60

check_pod_ready() {
    # Check whether pod is ready by readiness probe
    # echo 1 if all pods are ready
    # echo 0 if at least one pod is not ready
    kubectl --kubeconfig=/var/go/.kube/${ENV}-config get pod \
    -l app=${SERVICE_NAME} \
    -o jsonpath='{.items[*].status.containerStatuses[*].ready}' | grep false >/dev/null 2>&1
    echo $?
}

if [[ -z $SERVICE_NAME ]]; then
    echo "Service name must be set in env"
    exit 1
fi

ENV=${1}
case ${ENV} in
    qa)
        HELM_RELEASE_NAME="${ENV}-${SERVICE_NAME}"
        IMAGE_VERSION="master${GO_PIPELINE_COUNTER}"
        ;;
    uat)
        HELM_RELEASE_NAME="${SERVICE_NAME}"
        if [[ $FORK_RELEASE == "True" ]]; then
          IMAGE_VERSION="release${GO_PIPELINE_COUNTER}"
        else
          IMAGE_VERSION="master${GO_PIPELINE_COUNTER}"
        fi
        ;;
    *)
        echo "unsupported version"
        exit 1
        ;;
esac

IMAGE_NAME=${ACR_SERVER}/${PROJECT_NAME}/${SERVICE_NAME}:${IMAGE_VERSION}
cat <<EOF > values.yaml
image: ${IMAGE_NAME}
project: ${PROJECT_NAME}
environment: ${ENV}
service:
  name: ${SERVICE_NAME}
  port: ${SERVICE_PORT:-8080}
EOF

if [[ $TINGYUN_ENABLE == "enabled" ]]; then
    yq w --doc 0 -i values.yaml -- service.enable_tingyun true
fi

if [[ $ELASTIC_APM_ENABLE == "enabled" ]]; then
    yq w --doc 0 -i values.yaml -- service.enable_elastic_apm true
fi

if [[ $ADCC_NETWORKING_REQUIRED == "enabled" ]]; then
    yq w --doc 0 -i values.yaml -- adcc true
fi

if [[ ! -z $PROMETHEUS_METRICS ]]; then
    yq w --doc 0 -i values.yaml -- service.metrics.url ${PROMETHEUS_METRICS}
fi

if [[ $LANG == "other" ]]; then
    yq w --doc 0 -i values.yaml -- service.env_profile true
fi

if [[ ! -z $HEALTHCHECK_URL ]]; then
    yq w --doc 0 -i values.yaml -- service.healthcheck.url ${HEALTHCHECK_URL}
fi

if [[ ! -z $MEM_LIMIT ]]; then
    yq w --doc 0 -i values.yaml -- service.resources.limits.memory ${MEM_LIMIT}
fi

if [[ ! -z $MEM_RESERV ]]; then
    yq w --doc 0 -i values.yaml -- service.resources.requests.memory ${MEM_RESERV}
fi

if [[ ! -z $CPU_LIMIT ]]; then
    yq w --doc 0 -i values.yaml -- service.resources.limits.cpu ${CPU_LIMIT}
fi

if [[ ! -z $CPU_RESERV ]]; then
    yq w --doc 0 -i values.yaml -- service.resources.requests.cpu ${CPU_RESERV}
fi

if [[ ! -z $JVM_OPTIONS ]]; then
    yq w --doc 0 -i values.yaml -- service.jvm_options ${JVM_OPTIONS}
fi

if [[ $POD_AFFINITY == "enabled" ]]; then
    yq w --doc 0 -i values.yaml -- migration true
fi

if [[ $VAN_DM == "enabled" ]]; then
    yq w --doc 0 -i values.yaml -- van_dm true
fi

if kubectl --kubeconfig=/var/go/.kube/${ENV}-config get deployment ${SERVICE_NAME}-deployment &> /dev/null; then
    REPLICAS=$(kubectl --kubeconfig=/var/go/.kube/${ENV}-config get deployment ${SERVICE_NAME}-deployment -o=jsonpath='{$.spec.replicas}')
fi

# default 1
if [[ ${REPLICAS} -le 0 ]] || [[ -z ${REPLICAS} ]];then
    REPLICAS=1
fi

if [ `kubectl --kubeconfig=/var/go/.kube/${ENV}-config get deployment -n ${ENV} | grep api-gateway | awk '{print$2}'` -eq 0 ]; then
  expiration_time=`openssl x509 -in /var/go/.kube/${ENV}-config -text -noout | grep "Not After"`
  echo "Expiration time: $expiration_time"
  echo "${ENV} is not available in Non-working time"
  exit 1
fi

if kubectl --kubeconfig=/var/go/.kube/${ENV}-config get deployment ${SERVICE_NAME}-deployment &> /dev/null; then
    echo "Scale Down To 1"
    kubectl --kubeconfig=/var/go/.kube/${ENV}-config scale --replicas=1 deployment/${SERVICE_NAME}-deployment
fi

echo "Deploying ${IMAGE_NAME} to ${ENV} environment..."
helm  --kubeconfig /var/go/.kube/${ENV}-config \
    upgrade --install ${HELM_RELEASE_NAME} \
    deployment-chart/ -f values.yaml

if [[ ! -z $HEALTHCHECK_URL ]]; then
    # Wait for service ready
    sleep 200
    retry_times=0
    while [ "$retry_times" -lt $RETRY -a "$(check_pod_ready)" -eq 0 ]; do
        retry_times=$((retry_times+1))
        sleep $INTERVAL
        echo "Retry $retry_times"
    done
    if [[ "$(check_pod_ready)" -eq 1 ]]; then
        echo 'Pods are ready.'
    else
        echo 'Pods are not ready.'
        exit -1
    fi
fi

echo "Scale Up To ${REPLICAS}"
kubectl --kubeconfig=/var/go/.kube/${ENV}-config scale --replicas=${REPLICAS} deployment/${SERVICE_NAME}-deployment

if [[ $? -eq 0 ]]; then
    echo "Scaling"
else
    echo "Failed"
    exit -1
fi
```



