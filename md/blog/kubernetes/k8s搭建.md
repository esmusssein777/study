# k8s搭建

[toc]

## 安装 kubeadm 和 Docker

```
--------------关闭swap----
vim /etc/fstab
注销swap 一行
保存，并重启主机
--------------关闭swap----

--------修改Hostnmae--------
vim /etc/hostname
按照dns命名规则，修改主机名，参考RFC1123
--------修改Hostnmae--------
```

安装kukuadm

```
apt install curl

curl -s https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key add -

cat <<EOF > /etc/apt/sources.list.d/kubernetes.list
deb http://apt.kubernetes.io/ kubernetes-xenial main
EOF

apt-get update

apt-get install -y docker.io kubeadm
```

版本不对可能造成后续配置不对的情况，可以使用1.11.3版本

```
----------安装指定版本的docker----------
sudo apt-get remove docker docker-engine docker-ce docker.io
sudo apt-get update
sudo apt-get install apt-transport-https ca-certificates curl software-properties-common
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu xenial stable"

查看可用的docker版本：
apt-cache madison docker-ce
sudo apt-get install docker-ce=17.03.0~ce-0~ubuntu-xenial
----------安装指定版本的docker----------

----------安装kubeadm-------------
apt remove kubelet kubectl kubeadm
wget https://mirrors.aliyun.com/kubernetes/apt/pool/kubernetes-cni_0.6.0-00_amd64_43460dd3c97073851f84b32f5e8eebdc84fadedb5d5a00d1fc6872f30a4dd42c.deb
sudo dpkg -i kubernetes-cni_0.6.0-00_amd64_43460dd3c97073851f84b32f5e8eebdc84fadedb5d5a00d1fc6872f30a4dd42c.deb
apt install kubelet=1.11.3-00
apt install kubectl=1.11.3-00
apt install kubeadm=1.11.3-00
----------安装kubeadm-------------
```



## 准备配置文件

```
apiVersion: kubeadm.k8s.io/v1alpha1
kind: MasterConfiguration
controllerManagerExtraArgs:
  horizontal-pod-autoscaler-use-rest-clients: "true"
  horizontal-pod-autoscaler-sync-period: "10s"
  node-monitor-grace-period: "10s"
apiServerExtraArgs:
  runtime-config: "api/all=true"
kubernetesVersion: "stable-1.11"
```

