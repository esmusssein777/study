## 准备开始

- 一台兼容的 Linux 主机。Kubernetes 项目为基于 Debian 和 Red Hat 的 Linux 发行版以及一些不提供包管理器的发行版提供通用的指令
- 每台机器 2 GB 或更多的 RAM （如果少于这个数字将会影响你应用的运行内存)
- 2 CPU 核或更多
- 集群中的所有机器的网络彼此均能相互连接(公网和内网都可以)
- 节点之中不可以有重复的主机名、MAC 地址或 product_uuid。请参见[这里](https://kubernetes.io/zh/docs/setup/production-environment/tools/kubeadm/install-kubeadm/#verify-mac-address)了解更多详细信息。
- 开启机器上的某些端口。请参见[这里](https://kubernetes.io/zh/docs/setup/production-environment/tools/kubeadm/install-kubeadm/#check-required-ports) 了解更多详细信息。
- 禁用交换分区。为了保证 kubelet 正常工作，你 **必须** 禁用交换分区。

## 确保每个节点上 MAC 地址和 product_uuid 的唯一性

- 你可以使用命令 `ip link` 或 `ifconfig -a` 来获取网络接口的 MAC 地址
- 可以使用 `sudo cat /sys/class/dmi/id/product_uuid` 命令对 product_uuid 校验

一般来讲，硬件设备会拥有唯一的地址，但是有些虚拟机的地址可能会重复。 Kubernetes 使用这些值来唯一确定集群中的节点。 如果这些值在每个节点上不唯一，可能会导致安装 [失败](https://github.com/kubernetes/kubeadm/issues/31)。

## 检查网络适配器

如果你有一个以上的网络适配器，同时你的 Kubernetes 组件通过默认路由不可达，我们建议你预先添加 IP 路由规则，这样 Kubernetes 集群就可以通过对应的适配器完成连接。

## 允许 iptables 检查桥接流量

确保 `br_netfilter` 模块被加载。这一操作可以通过运行 `lsmod | grep br_netfilter` 来完成。若要显式加载该模块，可执行 `sudo modprobe br_netfilter`。

为了让你的 Linux 节点上的 iptables 能够正确地查看桥接流量，你需要确保在你的 `sysctl` 配置中将 `net.bridge.bridge-nf-call-iptables` 设置为 1。例如：

```bash
cat <<EOF | sudo tee /etc/modules-load.d/k8s.conf
br_netfilter
EOF

cat <<EOF | sudo tee /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-ip6tables = 1
net.bridge.bridge-nf-call-iptables = 1
EOF
sudo sysctl --system
```

## 安装 docker 

### Ubuntu 安装

```
sudo apt-get remove docker docker-engine docker.io containerd runc

sudo apt-get update
 
sudo apt-get install \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg \
    lsb-release
```

```
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
```

```
echo \
  "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
```

```
sudo apt-get update
sudo apt-get install docker-ce docker-ce-cli containerd.io
```

## 安装 kubeadm、kubelet 和 kubectl

你需要在每台机器上安装以下的软件包：

- `kubeadm`：用来初始化集群的指令。
- `kubelet`：在集群中的每个节点上用来启动 Pod 和容器等。
- `kubectl`：用来与集群通信的命令行工具。

1. 更新 `apt` 包索引并安装使用 Kubernetes `apt` 仓库所需要的包：

   ```shell
   sudo apt-get update
   sudo apt-get install -y apt-transport-https ca-certificates curl
   ```

2. 下载 Google Cloud 公开签名秘钥：

```shell
sudo curl -fsSLo /usr/share/keyrings/kubernetes-archive-keyring.gpg https://packages.cloud.google.com/apt/doc/apt-key.gpg
```

3. 添加 Kubernetes `apt` 仓库：

```shell
echo "deb [signed-by=/usr/share/keyrings/kubernetes-archive-keyring.gpg] https://apt.kubernetes.io/ kubernetes-xenial main" | sudo tee /etc/apt/sources.list.d/kubernetes.list
```

4. 更新 `apt` 包索引，安装 kubelet、kubeadm 和 kubectl，并锁定其版本：

```shell
sudo apt-get update
sudo apt-get install -y kubelet kubeadm kubectl
sudo apt-mark hold kubelet kubeadm kubectl
```

kubelet 现在每隔几秒就会重启，因为它陷入了一个等待 kubeadm 指令的死循环。

## kubeadm

设置主机名

```
hostnamectl set-hostname ligz-k8s-01
```

更换DNS

```
cat >> /etc/hosts <<EOF
172.16.182.130 ligz-k8s-01
172.16.182.131 ligz-k8s-02
172.16.182.132 ligz-k8s-03
EOF
```

## 关闭 swap 分区

关闭 swap 分区，否则kubelet 会启动失败(可以设置 kubelet 启动参数 --fail-swap-on 为 false 关闭 swap 检查)：

```
swapoff -a
sed -i '/ swap / s/^\(.*\)$/#\1/g' /etc/fstab 
```

执行`kubeadm init --control-plane-endpoint=ligz-k8s-01`



install CNI

```
kubectl apply -f "https://cloud.weave.works/k8s/net?k8s-version=$(kubectl version | base64 | tr -d '\n')"
```

work节点加入

```
kubeadm join ligz-k8s-01:6443 --token zbpz4w.5x4vraazn7mm0017 --discovery-token-ca-cert-hash sha256:203684b4c094450131c353b77d2c4a15a6a1ff95cde0ada8115189bdcf0aa383
```

使用命令查看节点是否OK

```
kubectl get pods -n kube-system

kubectl get nodes
```

安装dashbord

```
kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.2.0/aio/deploy/recommended.yaml
```

如果是本级的话

```
kubectl proxy

访问：
http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/.
```

修改dashbord配置

修改 `kubernetes-dashboard` service.

```
kubectl -n kubernetes-dashboard edit service kubernetes-dashboard
```

修改 `type: ClusterIP` to `type: NodePort` 保存

```
# Please edit the object below. Lines beginning with a '#' will be ignored,
# and an empty file will abort the edit. If an error occurs while saving this file will be
# reopened with the relevant failures.
#
apiVersion: v1
...
  name: kubernetes-dashboard
  namespace: kubernetes-dashboard
  resourceVersion: "343478"
  selfLink: /api/v1/namespaces/kubernetes-dashboard/services/kubernetes-dashboard
  uid: 8e48f478-993d-11e7-87e0-901b0e532516
spec:
  clusterIP: 10.100.124.90
  externalTrafficPolicy: Cluster
  ports:
  - port: 443
    protocol: TCP
    targetPort: 8443
  selector:
    k8s-app: kubernetes-dashboard
  sessionAffinity: None
  type: ClusterIP
status:
  loadBalancer: {}
```

然后执行下面命令查看结果

```
kubectl -n kubernetes-dashboard get service kubernetes-dashboard
```

```
NAME                   TYPE       CLUSTER-IP       EXTERNAL-IP   PORT(S)        AGE
kubernetes-dashboard   NodePort   10.100.124.90   <nodes>       443:31708/TCP   21h
```

可以通过访问ip和端口来访问，但是要 firefox 浏览器，访问 `https://172.16.182.130:31708/`

需要 token 的话我们要生成自己的 token。我们需要创建一个 user ,然后创建一个 role 绑定它，将它们都放入 secrect。

* vim dashbord-adminuser.yaml

```
apiVersion: v1
kind: ServiceAccount
metadata:
  name: admin-user
  namespace: kubernetes-dashboard
```

* vim dashbord-role.yaml

```
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: admin-user
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: cluster-admin
subjects:
- kind: ServiceAccount
  name: admin-user
  namespace: kubernetes-dashboard
```

```
kubectl apply -f dashbord-adminuser.yam

kubectl apply -f dashbord-role.yaml

kubectl -n kubernetes-dashboard get secret $(kubectl -n kubernetes-dashboard get sa/admin-user -o jsonpath="{.secrets[0].name}") -o go-template="{{.data.token | base64decode}}"
```

得到 token ，类似于

```
eyJhbGciOiJSUzI1NiIsImtpZCI6IkFESEVBeFdiWk1EMmpOR3VTaW11MFVRbXdiOFVVN1hmNVZXWF80Zk5KT2cifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlcm5ldGVzLWRhc2hib2FyZCIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJhZG1pbi11c2VyLXRva2VuLTlqbG50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQubmFtZSI6ImFkbWluLXVzZXIiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC51aWQiOiJjMGU2ODdlMS1mMGI0LTQ1YWItYTVkMC1jZTUyNDhjZjdmYjYiLCJzdWIiOiJzeXN0ZW06c2VydmljZWFjY291bnQ6a3ViZXJuZXRlcy1kYXNoYm9hcmQ6YWRtaW4tdXNlciJ9.dzpttAhZjUPBE-6xXJvIflUKTfCAQU-5fWeKk2i88cg6jUnC-Fz48B2nMM2R-mjyULwMv1v3Zdes7Y0isIqJPG7Vd4uwg7FXzJHOg-UJItG2gkCbFqRRObg_JoHR25vnELwLM02c_6Z-SonIPwneBWL_uftFLTTDFaLZsU3Hy5lcQa-L2RzifPqq5bhYJD38c2W6L6PBimO1TYL9TSVUvG0FcXtmFMbhLAAC1p7rRFYG78MMKqNeY_nZCS1ehuRdQdH0juSGb_05N22UAGAQDajdDAO4nehv025D6N8xWCEBLyoKXl3jgXg-R5tkP1hCY1JMdTc-xuhUnj1oXdTEuw
```

## install CSI

```
$ git clone --single-branch --branch master https://github.com/rook/rook.git
cd rook/cluster/examples/kubernetes/ceph
kubectl create -f crds.yaml -f common.yaml -f operator.yaml
kubectl create -f cluster.yaml
```

`kubectl -n rook-ceph get pod` 查看情况



最后截图

![Kb5bgg](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/Kb5bgg.png)

## 安装 helm

```
curl https://baltocdn.com/helm/signing.asc | sudo apt-key add -
sudo apt-get install apt-transport-https --yes
echo "deb https://baltocdn.com/helm/stable/debian/ all main" | sudo tee /etc/apt/sources.list.d/helm-stable-debian.list
sudo apt-get update
sudo apt-get install helm
```

