## Step1：Kubernetes API元件及本节实验介绍

Kubernetes的API 元件（Primitive）用于描述在Kubernetes上运行应用程序的基本组件，这些元件也就是俗称的Kubernetes对象（Object），它们持久存储于API Server上，用于描述集群的状态，例如，支行了哪些容器化的应用程序，这些应用支行于哪些节点之上以及使用了哪些存储，可用的资源有哪些，受控于何种编排机制以完成升级、回滚和容错等等。

绝大多数的Kubernetes对象都包含spec和status两个嵌套字段：

- spec字段存储对象的期望状态（或称为应有状态），由用户在创建时提供，随后也可按需进行更新（但有些属性并不支持就地更新机制）；
- status字段存储对象的实际状态（或称为当前状态），由Kubernetes系统控制平面相关的组件负责实时维护和更新；

每个对象还会有名称、标签、注解和隶属的名称空间（不包括集群级别的资源）等元数据，它们统一定义在metadata这一嵌套字段中，统称为对象元数据。

除此之外，kind和apiVersion两个字段负责指明对象的类型（资源类型）元数据，前者用于指定类型标识，而后者负责标明该类型所隶属的API群组（API Group）。

> 为了便于维护和扩展，Kubernetes将其API按功能等标准划分成了多个逻辑组合（Group），每个组合可称为一个API群组，支持多个版本并存。

API Server基于HTTP(S)协议暴露了一个RESTful风格的API，我们可借助于kubectl命令或其它UI通过该API查询或请求变更API对象的状态。施加于对象之上的基本操作包括增、删、改、查等，它们通过HTTP协议的GET、POST、DELETE和PUT等方法完成，而对应于kubectl命令，它们则是create、get、describe、delete、patch和edit等子命令。

创建对象时，我们必须向API Server提供描述其所需状态的对象规范、对象元数据及类型元数据，这些信息需要在请求报文的body中以JSON格式提供。但更常见的做法是，以YAML格式定义对象，提交给API Server后由其自行完成格式转换。

> kubectl create 命令支行于dry-run模式时生成的结果可作为定义相应资源的基础框架模板；

在本节的实验中，我们会带你学习Kubernetes中使kubectl命令管理对象的基本用法，这包括：

- 认识Kubernetes API的常用对象
- 以Pod为例，使用kubectl创建API对象
- 以Pod为例，使用kubectl获取API对象的信息
- 使用Pod的交互式接口
- 以Pod为例，删除API对象

> **容器服务 Kubernetes 版（简称 ACK）** 本节课使用的 Kubernetes(k8s) 集群就是由 ACK 提供的，本实验涵盖的都是一些基本操作。更多高级用法，可以去[ACK 的产品页面](https://www.aliyun.com/product/kubernetes)了解哦。

## Step2：Kubernetes的核心资源类型

依据资源的主要功能作为分类标准，Kubernetes的API对象大体可分为工作负载（Workload）、发现和负载均衡（Discovery & LB）、配置和存储（Config & Storage）、集群（Cluster）和元数据（Metadata）几个类别。它们基本都是围绕一个核心目的而设计：如何更好地运行和丰富Pod资源，从而为容器化应用提供更灵活和更完善的操作与管理组件。

![FeaQ8v](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/FeaQ8v.jpg)

工作负载型资源用于确保Pod资源对象更好地运行容器化应用，具有同一种负载的各Pod对象需要以“负载均衡”的方式服务于各请求，而各种容器化应用彼此间需要彼此“发现”以完成工作协同。Pod资源具有生命周期，存储型资源能够为重构的Pod对象提供持久化数据存储机制，共享同一配置的Pod资源可从“配置”型资源中统一获取配置改动信息，这些资源作为“配置中心”为管理容器化应用的配置文件提供了极为便捷的管理机制。集群型资源为管理集群本身的工作特性提供了配置接口，而元数据型资源用于配置集群内部的其他资源的行为。

Pod用于承载容器化应用，它代表着Kubernetes之上工作负载的表现形式。它负责运行容器，并为其解决环境性的依赖，例如向容器注入临时或持久化的存储空间、配置信息或密钥数据等。而诸如滚动更新、扩容和缩容一类的编排任务则由“控制器”对象负责，专用于Pod编排任务的控制器也可统称为Pod控制器。

应用程序分为无状态和有状态两种类型，无状态应用中的每个Pod实例均可被其他同类实例所取代，但有状态应用的每个Pod实例均有其独特性，必须单独标识和管理，因而它们分属两种不同类型的Pod控制器进行管理，ReplicationController、ReplicaSet和Deployment负责管理无状态应用，StatefulSet则专用于管控有状态类应用。还有些应用较为独特，有些需要在集群中的每个节点上运行单个Pod资源，负责收集日志或运行系统服务等任务，该类编排操作由DaemonSet控制器对象进行，而那些需要在正常完成后退出而无需始终处于运行状态任务的编排工作则隶属Job控制器对象，CronJob控制器对象还能为Job型的任务提供定期执行机制。

- ReplicationController：用于确保每个pod副本在任一时刻均能满足目标数量，换言之，即它用于保证每个容器或容器组总是运行并可访问；它是上一代的无状态Pod应用控制器，建议读者使用新型控制器Deployment和ReplicaSet来取代它。
- ReplicaSet：新一代ReplicationController，它与ReplicationController的唯一不同之处仅在于支持的标签选择器不同：ReplicationController只支持“等值选择器”，而ReplicaSet还额外支持基于集合的选择器。
- Deployment：用于管理无状态的持久化应用，例如http服务等；它用于为Pod和ReplicaSet提供声明式更新，是建构在ReplicaSet之上的更为高级的控制器。
- StatefulSet：用于管理有状态的持久化应用，例如database服务程序；与Deployment的不同之处在于StatefulSet会为每个Pod创建一个独有的持久性标识符，并会确保各Pod间的顺序性。
- DaemonSet：用于确保每个节点都运行了某Pod的一个副本，包括后来新增的节点；而节点移除将导致Pod回收；DaemonSet常用于运行各类系统级守护进程，例如kube-proxy和flannel网络插件，以及日志收集和临近系统的agent应用，例如fluentd、logstash、Prometheus的Node Exporter等。
- Job：用于管理运行完成后即可终止的应用，例如批处理作业任务； Job创建一个或多个Pod，并确保其符合目标数量，直到应用完成而终止。

Service是Kubernetes标准的资源类型之一，用于为工作负载实例提供固定的访问入口及负载均衡服务，它把每个可用后端实例定义为Endpoint资源对象，通过IP地址和端口等属性映射至Pod实例或相应的服务端点。Ingress资源则为工作负载提供7层（HTTP/HTTPS）代理及负载均衡功能。

Kubernetes也支持在Pod级别附加Volume资源对象为容器附加可用的外部存储。它支持众多类型的存储设备或存储系统，例如GlusterFS、CEPH RBD和Flocker等，还可通过FlexVolume及CSI（Container Storage Interface）存储接口扩展支持更多类型的存储系统。

## Step3：创建Pod对象（指令式命令）

[kubectl](https://kubernetes.io/docs/reference/kubectl/kubectl/)命令提供了三种类型的对象管理机制：

- 指令式命令（Imperative commands）：直接作用于集群上的活动对象（Live objects），适合在开发环境中完成一次性的操作任务，且易于学习和使用；
- 指令式对象配置（Imperative object configuration）：基于配置文件执行对象管理操作，但只能独立引用每个配置清单文件，可用于生产环境的管理任务；
- 声明式对象配置（Declarative object configuration）：基于配置文件执行对象管理操作，可直接引用目录下的所有配置清单文件，也可直接作用于单个配置文件，是较为推荐的使用方式，也是掌握难度最高的方式；

例如，创建一个Pod对象的简单方式就是直接使用“[kubectl run](https://kubernetes.io/docs/reference/generated/kubectl/kubectl-commands#run)”命令，同时使用“--dry-run”和“-o yaml”选项，该命令将在打印相应的资源清单后退出，例如下面的命令。

```bash
kubectl run demoapp --image="ikubernetes/demoapp:v1.0" --dry-run=client -o yaml
```

若需要完成创建，我们可通过移除上面的命令的“--dry-run”选项完成,如下面的命令所示。为避免对象名称冲突，我们有意在下面的命令上添加了“-$RANDOM”后缀。

```bash
kubectl run demoapp-$RANDOM --image="ikubernetes/demoapp:v1.0"
```

或者，也可以使用类似如下命令完成资源创建，管道后面命令的最后一个连接符“-”用于从标准输入接收对象配置信息。

```bash
kubectl run demoapp-$RANDOM --image="ikubernetes/demoapp:v1.0" --dry-run=client -o yaml | kubectl create -f -
```

若需要进一步定制Pod对象的配置信息，我们也可以将上面测试命令的结果保存于配置清单文件中。

```bash
kubectl run demoapp-$RANDOM --image="ikubernetes/demoapp:v1.0" --dry-run=client -o yaml > demoapp.yaml
```

而后可使用任何文本编辑工具进行对象配置的修改。下面的命令，将Pod的重启策略从默认值Always改为了“OnFailure”。

```bash
sed -i "s@\(^[[:space:]]*restartPolicy: \)Always@\1OnFailure@" demoapp.yaml
```

在完成demoapp.yaml文件的编辑后，再使用如下命令完成资源创建。

```bash
kubectl create -f demoapp.yaml
```

事实上，创建Pod对象时，如果需要创建具有较多自定义的配置，或者无法由命令行直接生成相关的配置，我们都能够以dry-run模式的命令生成一个基础的配置框架保存于文件中，而后再编辑该文件生成最终配置。后面的实验中用到的对象配置清单有很多，我们将不再一一给出该命令，而直接给出配置清单。

## Step4：获取Pod对象的状态信息

列出指定名称空间下的所有Pod对象,我们以使用如下命令。省略-n选项时，则表示列出当前名称空间下的所有Pod对象。

```bash
kubectl get pods -n handson-22084b6dfd81c0d5e414222b5f461693
```

```console
NAME            READY   STATUS    RESTARTS   AGE
demoapp-2121    1/1     Running   0          5s
demoapp-28726   1/1     Running   0          3m46s
demoapp-5204    1/1     Running   0          3m31s
```

若要打印更多的字段，我们可在上面的命令上同时使用“-o wide”选项。在该实验中，handson-22084b6dfd81c0d5e414222b5f461693就是默认的名称空间。

```bash
kubectl get pods -o wide
```

```console
NAME            READY   STATUS    RESTARTS   AGE     IP             NODE                       NOMINATED NODE   READINESS GATES
demoapp-2121    1/1     Running   0          94s     172.20.1.20    cn-shanghai.192.168.0.28   <none>           <none>
demoapp-28726   1/1     Running   0          5m15s   172.20.0.241   cn-shanghai.192.168.0.29   <none>           <none>
demoapp-5204    1/1     Running   0          5m      172.20.1.97    cn-shanghai.192.168.0.27   <none>           <none>
```

若需要列出所有名称空间下的Pod资源，我们可以将“-n NS_NAME”的选项替换为“--all-namespaces”或“-A”，如下面的命令所示。需要注意的是，该命令在实验室环境中可能无权限执行，因而，我们只能选择忽略执行该命令。

```bash
kubectl get pods --all-namespaces
```

## Step5：对象的规范

“-o jsonpath”或“-o custom-columes”或“-o go-template”选项允许我们指定要打印的字段，但它们各自使用了不同的模板格式。下面的命令用于打印当前名称空间下各Pod对象的IP地址，你获取到的结果可能会有所不同。

```bash
kubectl get pods -o "jsonpath={.items[*].status.podIP}"
```

```console
172.20.1.20 172.20.0.241 172.20.1.97
```

向API Server上创建对象时，其资源规范中的省略的字段将会由称为准入控制器的组件自动补充完整后再存入etcd中。这些完整的信息可由使用了“-o yaml”或“-o json”选项的“kubectl get pods”命令打印。例如，我们先获取任意一个Pod对象的名称后，打印其完整格式的资源规范。

```bash
podName=$(kubectl get pods -o "jsonpath={.items[0].metadata.name}")
kubectl get pods $podName -o yaml --show-managed-fields=false
```

```console
apiVersion: v1    # 资源群组及版本，格式为“GROUP_NAME/VERSION”，核心群组“core”，但引用时，其名称会省略
kind: Pod    # 资源的类别
metadata:   # 对象元数据
  annotations:   # 资源注解信息
    kubernetes.io/psp: ack.privileged
  creationTimestamp: "2021-04-19T09:27:32Z"
  labels:    # 资源标签
    run: demoapp-15425
  name: demoapp-15425
  namespace: handson-22084b6dfd81c0d5e414222b5f461693      # 所属的名称空间
  resourceVersion: "55961052"
  selfLink: /api/v1/namespaces/handson-22084b6dfd81c0d5e414222b5f461693/pods/demoapp-15425     # 该对象在API Server上的Path
  uid: 3cf1399e-161d-451b-a673-272a0ea1f871
spec:    # 对象的应有状态
  containers:
  - image: ikubernetes/demoapp:v1.0
    imagePullPolicy: IfNotPresent
    name: demoapp-15425
    resources: {}
    terminationMessagePath: /dev/termination-log
    terminationMessagePolicy: File
    volumeMounts:
    - mountPath: /var/run/secrets/kubernetes.io/serviceaccount
      name: default-token-mftnd
      readOnly: true
  dnsPolicy: ClusterFirst    # DNS解析策略
  enableServiceLinks: true
  imagePullSecrets:
  - name: acr-credential-d8cb8c4195a48ca6dbefe56f6399ef1f
  - name: acr-credential-27b7396c3bc51e6df6ffbf8189648a39
  - name: acr-credential-2b442760ae1031e228cdeea8831392a1
  - name: acr-credential-278a682b6f1cc97ae2bdfefacfa9b8a8
  - name: acr-credential-e0de33735568ed2f86e4b7795df1e710
  nodeName: cn-shanghai.192.168.0.29    # 绑定的节点
  priority: 0
  restartPolicy: OnFailure    # 重启策略
  schedulerName: default-scheduler    # 调度器名称
  securityContext: {}    # 安全上下文
  serviceAccount: default    # 使用的ServiceAccount
  serviceAccountName: default
  terminationGracePeriodSeconds: 30
  tolerations:     # 污点容忍度
  - effect: NoExecute
    key: node.kubernetes.io/not-ready
    operator: Exists
    tolerationSeconds: 300
  - effect: NoExecute
    key: node.kubernetes.io/unreachable
    operator: Exists
    tolerationSeconds: 300
  volumes:
  - name: default-token-mftnd
    secret:
      defaultMode: 420
      secretName: default-token-mftnd
status:     # 对象的当前状态
  conditions:   # 对象的Condition
  - lastProbeTime: null
    lastTransitionTime: "2021-04-19T09:27:32Z"
    status: "True"
    type: Initialized
  - lastProbeTime: null
    lastTransitionTime: "2021-04-19T09:27:33Z"
    status: "True"
    type: Ready
  - lastProbeTime: null
    lastTransitionTime: "2021-04-19T09:27:33Z"
    status: "True"
    type: ContainersReady
  - lastProbeTime: null
    lastTransitionTime: "2021-04-19T09:27:32Z"
    status: "True"
    type: PodScheduled
  containerStatuses:   # 容器状态
  - containerID: docker://fa117c10ccc593f1dd3802569977dd9dcb901144ed94385b1635f827e7128fb8
    image: ikubernetes/demoapp:v1.0
    imageID: docker-pullable://ikubernetes/demoapp@sha256:6698b205eb18fb0171398927f3a35fe27676c6bf5757ef57a35a4b055badf2c3
    lastState: {}
    name: demoapp-15425
    ready: true
    restartCount: 0
    started: true
    state:
      running:
        startedAt: "2021-04-19T09:27:33Z"
  hostIP: 192.168.0.29
  phase: Running    # Pod的相位
  podIP: 172.20.0.246    # Pod的IP地址
  podIPs:
  - ip: 172.20.0.246
  qosClass: BestEffort
  startTime: "2021-04-19T09:27:32Z"
```

不同类型资源上可使用的字段也不尽相同，甚至几乎完全不同。各字段的意义，我们可通过API Server内嵌的文档来了解。例如，下面的命令能打印出Pod资源上的一级字段及其功用。

```bash
kubectl explain pods
```

内嵌文档上的字段支持以点号分隔符进行字段分级，例如，若需要打印pods资源上的一级字段spec上要嵌套使用的二级字段，可使用类似如下命令进行。

```bash
kubectl explain pods.spec 
```

其它级别字段文档的获取方法类同。

## Step6：交互式Pod

我们也可创建一个临时性Pod对象，设定其运行指定的命令，以进行任何所需要测试操作。下面的命令中，“--restart=Never”表示在Pod健康状态探测失败后不会试图重启该Pod，而“--rm”选项表示在Pod转为终止状态后直接删除Pod。

```bash
kubectl run client-$RANDOM --image="ikubernetes/admin-box:v1.2" --rm -it --restart=Never --command -- wget -q -O- $(kubectl get pods -o "jsonpath={.items[0].status.podIP}")
```

或者，我们也可以进入Pod的交互式接口，以便于进行多次测试。

```bash
kubectl run client-$RANDOM --image="ikubernetes/admin-box:v1.2" --rm -it --restart=Never --command -- /bin/sh 
```

这时，会打开Pod中默认容器应用上的交互式shell接口，以便于用户运行任何由该容器支持的命令。

```console
root@client-20631 # 
```

例如，测试解析kubernetes.default.svc这一主机名至IP地址。

```bash
nslookup -query=A kubernetes.default.svc
```

```console
Server:         172.21.0.10
Address:        172.21.0.10#53

Name:   kubernetes.default.svc.cluster.local
Address: 172.21.0.1
```

退出该交互式接口后，Pod即会终止，且会被删除。

```bash
exit
```

## Step7：删除资源对象

执行如下命令，查看创建的Pod资源。

```bash
kubectl get pods
```

```console
NAME            READY   STATUS    RESTARTS   AGE
demoapp-2121    1/1     Running   0          7m5s
demoapp-28726   1/1     Running   0          11m
demoapp-5204    1/1     Running   0          12m
```

确认测试完成后，且不再需要这些Pod，可执行如下命令，删除创建的所有Pod资源以释放系统资源。

```bash
kubectl delete pods --all --force --grace-period=0
```

