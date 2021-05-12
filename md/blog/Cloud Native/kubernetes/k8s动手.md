[toc]

# Kubernetes实战

## 部署业务应用

首先，我们需要将业务应用部署到 k8s 集群中。这里我们已经准备好了一个应用并打包成镜像，镜像地址如下：

registry.cn-shanghai.aliyuncs.com/workbench*1459088147016887/handson*ack_test:3

这是一个使用 spring boot 开发的 java web 应用。这个应用会监听 8080 端口，接受所有路径的访问，并输出一些环境基本信息。

要让这个 web 应用的镜像在 k8s 中运行，我们首先要定义一个 deployment 资源。
通过创建`deploy.yaml`来描述 deployment 资源（**点击代码框右上角自动拷贝**）：

```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp-deployment
  namespace: handson-e616306e4fa9da042f893337eaa5d538
  labels:
    app: myapp
spec:
  replicas: 1
  selector:
    matchLabels:
      name: myapp
  template:
    metadata:
      labels:
        name: myapp
      namespace: handson-e616306e4fa9da042f893337eaa5d538
    spec:
      containers:
        - name: myapp
          image: registry.cn-shanghai.aliyuncs.com/workbench_1459088147016887/handson_ack_test:3
          ports:
            - containerPort: 8080
```

其中：

- image 属性：就是这个 java web 应用的镜像地址；
- replicas 属性：代表这个应用只部署一份；

通过下面的命令执行:

```bash
kubectl apply -f ~/deploy.yaml
```

完成后，你可以通过下面的命令查看刚才部署的 pod：

```bash
kubectl get pod
```

看到如下提升代表应用部署完成，注意其中的 status 字段。只有 Running 才是运行中的状态哦，如果是 ContainerCreating 代表服务容器正在创建中，需要等待一段时间才能使用

```console
NAME                                READY   STATUS    RESTARTS   AGE
myapp-deployment-5cd4d7c78d-lxvcw   1/1     Running   0          12s
```

至此，我们已经完成了应用本身的部署，下面我们看下如何在 k8s 里配置“服务”；



## 部署服务

由于 deployment 是一个弹性组件，其管理的应用实例不是固定的，而是可以任意伸缩。这带来了很多的好处，例如可以支持弹性伸缩、滚动更新等等。
但是相反的，这也会导致应用实例IP不固定，从访问者的角度我们不可能每次去查找当前的应用实例。

所以，为了能提供稳定的访问入口，我们还需要部署“服务”来接收请求，并屏蔽内部的弹性机制。

部署服务，我们继续 yaml 文件的方式操作，创建 service.yaml（**点击代码框右上角自动拷贝**）：

```
apiVersion: v1
kind: Service
metadata:
  name: myapp-service
  namespace: handson-e616306e4fa9da042f893337eaa5d538
spec:
  ports:
    - port: 8080
      targetPort: 8080
      protocol: TCP
  type: NodePort
  selector:
    name: myapp
```

我们看到 Service 的定义相对简单很多，其中有几个关键属性需要说明：

- selector，这是一个选择器，通过 name=myapp 这个条件来选择需要代理的服务
- ports，这里定义了服务自身暴露的端口和需要访问的应用的端口

继续通过 kubectl 命令执行：

```bash
kubectl apply -f ~/service.yaml
```

然后通过下面的命令查看刚才部署的 service：

```bash
kubectl get service
```

我们会看到下面的反馈信息：

```console
NAME            TYPE       CLUSTER-IP     EXTERNAL-IP   PORT(S)          AGE
myapp-service   NodePort   172.21.12.82   <none>        8080:30163/TCP   2s
```

这里我们就完成了 service 的部署。下面通过 ingress 将内部的服务暴露出去。



## 配置 ingress 开放外部访问

k8s 是一个集群，deployment、service 都是集群内部的资源，他们通过一个内部虚拟网络互相访问。
但是对于外部的用户，这些所有的资源都是不可见的，所以我们还需要配置一个外部访问的入口到 service 的映射规则，从而将内部服务暴露出去。

这里我们就需要使用 ingress 的来实现服务对外暴露的需求。

我们继续使用 yaml 来定义 ingress 规则（**点击代码框右上角自动拷贝**）：

```
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: example-ingress
  namespace: handson-e616306e4fa9da042f893337eaa5d538
spec:
  rules:
  - http:
      paths:
      - path: /welcome
        backend:
          serviceName: myapp-service
          servicePort: 8080
```

继续通过 kubectl 命令执行：

```bash
kubectl apply -f ~/ingress.yaml
```

然后通过下面的命令查看刚才部署的 service：

```bash
kubectl get ingress
```

我们会看到下面的反馈信息：

```console
NAME              CLASS    HOSTS   ADDRESS          PORTS   AGE
example-ingress   <none>   *       47.100.138.224   80      72s
```

通过这个返回值信息，我们可以看到，访问地址是47.100.138.224，端口是80。

所以我们可以通过下面的链接访问刚才部署的应用：[http://47.100.138.224/welcome](http://47.100.138.224/welcome?spm=5176.12026607.tutorial.2.9c7732e2HRw9Zb)

除了部署服务，我们还需要能卸载相关资源，才算完整的流程



## 卸载资源

光是配置并提供服务只完成了一半的工作，完整的操作周期除了正向的安装，还包含了逆向的卸载，下面我们就来学习如何把刚才配置的各种资源给卸载掉。

资源卸载我们就不使用 yaml 脚本了，直接使用 kubectl 命令就可以完成：

- 卸载 ingress

```bash
kubectl delete ingress example-ingress
```

- 卸载 service

```bash
kubectl delete service myapp-service
```

- 卸载 deployment

```bash
kubectl delete deployment myapp-deployment
```

至此玩我们就完成了卸载工作，最后再用下面的命令确认一下我们的卸载结果：

```bash
kubectl get deployment
```

```bash
kubectl get service
```

```bash
kubectl get ingress
```

你可能会看到：

```console
NAME                                    READY   STATUS        RESTARTS   AGE
pod/myapp-deployment-5cd4d7c78d-kx7kn   0/1     Terminating   0          29m
```

Terminating 代表 pod 正在卸载中，多运行几次后，会看到下面的提示：

```console
No resources found in handson-e616306e4fa9da042f893337eaa5d538 namespace.
```

代表卸载工作完成

## Helm



