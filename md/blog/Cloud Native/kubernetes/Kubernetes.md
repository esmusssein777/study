# Kubernetes

[toc]

## 整体架构

<img src="https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/lx3z1s.png" alt="lx3z1s" style="zoom:50%;" />

Kubernetes 的项目架构由 master 和 node 两种节点组成，而这两种节点分别对应着控制节点和计算节点。

其中，控制节点，即 Master 节点，由三个紧密协作的独立组件组合而成，它们分别是负责 API 服务的 kube-apiserver、负责调度的 kube-scheduler，以及负责容器编排的 kube-controller- manager。

整个集群的持久化数据，则由 kube-apiserver 处理后保存在 Ectd 中。

而计算节点上最核心的部分，则是一个叫作 kubelet 的组件。

在 Kubernetes 项目中，kubelet 主要负责同容器运行时(比如 Docker 项目)打交道。而这个交互所依赖的，是一个称作 CRI(Container Runtime Interface)的远程调用接口，这个接口定义了容器运行时的各项核心操作，比如:启动一个容器需要的所有参数。

而具体的容器运行时，比如 Docker 项目，则一般通过 OCI 这个容器运行时规范同底层的 Linux 操作系统进行交互，即:把 CRI 请求翻译成对 Linux 操作系统的调用(操作 Linux Namespace 和 Cgroups 等)。

此外，kubelet 还通过 gRPC 协议同一个叫作 Device Plugin 的插件进行交互。这个插件，是 Kubernetes 项目用来管理 GPU 等宿主机物理设备的主要组件，也是基于 Kubernetes 项目进行机 器学习训练、高性能作业支持等工作必须关注的功能。

而kubelet 的另一个重要功能，则是调用网络插件和存储插件为容器配置网络和持久化存储。这两 个插件与 kubelet 进行交互的接口，分别是 CNI(Container Networking Interface)和 CSI(Container Storage Interface)。

## 编排

而 Kubernetes 项目要着重解决的问题，是运行在大规模集群中的各种任务之间，实际上存在着各种各样的关系。这些关系的处理，才是作业编排和管理系统最困难的地方。

比如，Kubernetes 项目对容器间的“访问”进行了分类：

首先总结出了一类非常常见的“紧密交互”的关系，即:这些应用之间需要非常频繁的交互和访问;又或者，它们会直接通过本地文件进行信息交换。

在常规环境下，这些应用往往会被直接部署在同一台机器上，通过 Localhost 通信，通过本地磁盘 目录交换文件。而在 Kubernetes 项目中，这些容器则会被划分为一个“Pod”，Pod 里的容器共享同一个 Network Namespace、同一组数据卷，从而达到高效率交换信息的目的。Pod 是 Kubernetes 项目中最基础的一个对象。

而对于另外一种更为常见的需求，比如 Web 应用与数据库之间的访问关系，Kubernetes 项目则提供了一种叫作“Service”的服务。像这样的两个应用，往往故意不部署在同一台机器上，这样即使 Web 应用所在的机器宕机了，数据库也完全不受影响。可是，我们知道，对于一个容器来说，它的 IP 地址等信息不是固定的，那么 Web 应用又怎么找到数据库容器的 Pod 呢?

所以，Kubernetes 项目的做法是给 Pod 绑定一个 Service 服务，而 Service 服务声明的 IP 地址等信息是“终生不变”的。这个Service 服务的主要作用，就是作为 Pod 的代理入口(Portal)，从而代替 Pod 对外暴露一个固定的网络地址。

这样，对于 Web 应用的 Pod 来说，它需要关心的就是数据库 Pod 的 Service 信息。不难想象， Service 后端真正代理的 Pod 的 IP 地址、端口等信息的自动更新、维护，则是 Kubernetes 项目的职责。

<img src="https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/poVdiS.png" alt="poVdiS" style="zoom:50%;" />

我们从容器这个最基础的概念出发，首先遇到了容器间“紧密协作”关系的难 题，于是就扩展到了 Pod;有了 Pod 之后，我们希望能一次启动多个应用的实例，这样就需要 Deployment 这个 Pod 的多实例管理器;而有了这样一组相同的 Pod 后，我们又需要通过一个固 定的 IP 地址和端口以负载均衡的方式访问它，于是就有了 Service。

可是，如果现在两个不同 Pod 之间不仅有“访问关系”，还要求在发起时加上授权信息。最典型的例子就是 Web 应用对数据库访问时需要 Credential(数据库的用户名和密码)信息。那么，在 Kubernetes 中这样的关系又如何处理呢?

Kubernetes 项目提供了一种叫作 Secret 的对象，它其实是一个保存在 Etcd 里的键值对数据。这 样，你把 Credential 信息以 Secret 的方式存在 Etcd 里，Kubernetes 就会在你指定的 Pod(比 如，Web 应用的 Pod)启动时，自动把 Secret 里的数据以 Volume 的方式挂载到容器里。这样， 这个 Web 应用就可以访问数据库了。

Kubernetes 定义了新的、基于 Pod 改进后的对象。比如 Job，用来描述一次性运行的 Pod(比如，大数据任务);再比如 DaemonSet，用来描述每个宿主机上必须且只能运行一个副本 的守护进程服务;又比如 CronJob，则用于描述定时任务等等。

在 Kubernetes 项目中，我们所推崇的使用方法是:

首先，通过一个“编排对象”，比如 Pod、Job、CronJob 等，来描述你试图管理的应用; 

然后，再为它定义一些“服务对象”，比如 Service、Secret、Horizontal Pod Autoscaler(自动水平扩展器)等。这些对象，会负责具体的平台级功能。

这种使用方法，就是所谓的“声明式 API”。这种 API 对应的“编排对象”和“服务对象”，都是 Kubernetes 项目中的 API 对象(API Object)。

## Pod

Pod，是 Kubernetes 项目的原子调度单位。

容器的本质是进程。容器，就是未来云计算系统中的进程; 容器镜像就是这个系统里的“.exe”安装包。那么 Kubernetes 呢? Kubernetes 就是操作系统!

对于操作系统来说，进程组更方便管理。举个例子，Linux 操作系统只需要将信号，比如， SIGKILL 信号，发送给一个进程组，那么该进程组中的所有进程就都会收到这个信号而终止运行。

而 Kubernetes 项目所做的，其实就是将“进程组”的概念映射到了容器技术中，并使其成为了这 个云计算“操作系统”里的“一等公民”。Google 公司的工程师们发现，他们部署的应用，往往都存 在着类似于“进程和进程组”的关系。更具体地说，就是这些应用之间有着密切的协作关系，使得 它们必须部署在同一台机器上。而如果事先没有“组”的概念，像这样的运维关系就会非常难以处理。

比如三个容器是“超亲密关系”容器，共需要 3g 的内容，如果把他们组成一个pod，那么自然就容易选择 3g 以上的虚拟机。

具有“超亲密关系”容器的典型特征包括但不限于:互相之间会发生直接的文件交换、使用 localhost 或者 Socket 文件进行本地通 信、会发生非常频繁的远程调用、需要共享某些 Linux Namespace(比如，一个容器要加入另一个 容器的 Network Namespace)等等。

这也就意味着，并不是所有有“关系”的容器都属于同一个 Pod。比如，Java 应用容器和 MySQL 虽然会发生访问关系，但并没有必要、也不应该部署在同一台机器上，它们更适合做成两个 Pod。



关于 Pod 最重要的一个事实是: 它只是一个逻辑概念，一组共享了某些资源的容器。

也就是说，Kubernetes 真正处理的，还是宿主机操作系统上 Linux 容器的 Namespace 和 Cgroups，而并不存在一个所谓的 Pod 的边界或者隔离环境。

不同于 docker-compose 的 link 加入，因为这样的话容器天生就有了拓扑关系，有了启动的先后顺序。在 Kubernetes 项目里，Pod 的实现需要使用一个中间容器，这个容器叫作 Infra 容器。在 这个 Pod 中，Infra 容器永远都是第一个被创建的容器，而其他用户定义的容器，则通过 Join Network Namespace 的方式，与 Infra 容器关联在一起。

**而对于同一个 Pod 里面的所有用户容器来说，它们的进出流量，也可以认为都是通过 Infra 容器完成的。**



明白了 Pod 的实现原理后，我们再来讨论“容器设计模式”，就容易多了。

Pod 这种“超亲密关系”容器的设计思想，实际上就是希望，当用户想在一个容器里跑多个功能并不相关的应用时，应该优先考虑它们是不是更应该被描述成一个 Pod 里的多个容器。

举一个例子就是容器的日志收集。我现在有一个应用，需要不断地把日志文件输出到容器的 /var/log 目录中。 这时，我就可以把一个 Pod 里的 Volume 挂载到应用容器的 /var/log 目录上。

然后，我在这个 Pod 里同时运行一个 sidecar 容器，它也声明挂载同一个 Volume 到自己的 /var/log 目录上。

这样，接下来 sidecar 容器就只需要做一件事儿，那就是不断地从自己的 /var/log 目录里读取日志 文件，转发到 MongoDB 或者 Elasticsearch 中存储起来。这样，一个最基本的日志收集工作就完 成了。

而这种思想，正是容器设计模式里最常用的一种模式，它的名字叫: sidecar。顾名思义，sidecar 指的就是我们可以在一个 Pod 中，启动一个辅助容器，来完成一些独立于主进程(主容器)之外的工作。

## Deployment

```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment
spec:
  selector:
    matchLabels:
      app: nginx
  replicas: 2
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: nginx:1.7.9
        ports:
        - containerPort: 80
```

这个 Deployment 定义的编排动作非常简单，即:确保携带了 app=nginx 标签的 Pod 的个数，永远等于spec.replicas 指定的个数，即 2 个。

这就意味着，如果在这个集群中，携带 app=nginx 标签的 Pod 的个数大于 2 的时候，就会有旧的 Pod 被删除;反之，就会有新的 Pod 被创建。

一个叫作 kube-controller-manager 的组件在控制这些**控制循环**。这是一系列的控制器集合，由 deployment, job, cronjob, namespace等等的控制器组成。

以 Deployment 为例，我和你简单描述一下它对控制器模型的实现：

1. Deployment 控制器从 Etcd 中获取到所有携带了“app: nginx”标签的 Pod，然后统计它们的数量，这就是实际状态;

2. Deployment 对象的 Replicas 字段的值就是期望状态;

3. Deployment 控制器将两个状态做比较，然后根据比较结果，确定是创建 Pod，还是删除已有的 Pod。

像 Deployment 这种控制器的设计原理，就是我们前面提到过的，“用一种对象管理另一种 对象”的“艺术”。其中，这个控制器对象本身，负责定义被管理对象的期望状态。比如，Deployment 里的 replicas=2 这个字段。

而被控制对象的定义，则来自于一个“模板”。比如，Deployment 里的 template 字段。

可以看到，Deployment 这个 template 字段里的内容，跟一个标准的 Pod 对象的 API 定义，丝毫不差。而所有被这个 Deployment 管理的 Pod 实例，其实都是根据这个 template 字段的内容创建出来的。

像 Deployment 定义的 template 字段，在 Kubernetes 项目中有一个专有的名字，叫作 PodTemplate(Pod 模板)。这个概念非常重要，因为后面我要讲解到的大多数控制器，都会使用 PodTemplate 来统一定义它 所要管理的 Pod。



Deployment 看似简单，但实际上，它实现了 Kubernetes 项目中一个非常重要的功能:Pod 的“水平扩展 / 收缩”(horizontal scaling out/in)。

如果你更新了 Deployment 的 Pod 模板(比如，修改了容器的镜像)，那么 Deployment 就需要遵循一种叫作“滚动更新”(rolling update)的方式，来升级现有的容器。而这个能力的实现，依赖的是 Kubernetes 项目中的一个非常重要的概念(API 对象): ReplicaSet。

```
apiVersion: apps/v1
kind: ReplicaSet
metadata:
  name: nginx-set
  labels:
    app: nginx
spec:
  replicas: 3
  selector:
    matchLabels:
      app: nginx
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: nginx:1.7.9
```

从这个 YAML 文件中，我们可以看到，一个 ReplicaSet 对象，其实就是由副本数目的定义和一 个 Pod 模板组成的。不难发现，它的定义其实是 Deployment 的一个子集。

更重要的是，Deployment 控制器实际操纵的，正是这样的 ReplicaSet 对象，而不是 Pod 对象。

其中，ReplicaSet 负责通过“控制器模式”，保证系统中 Pod 的个数永远等于指定的个数(比 如，3 个)。这也正是 Deployment 只允许容器的 restartPolicy=Always 的主要原因:只有在 容器能保证自己始终是 Running 状态的前提下，ReplicaSet 调整 Pod 的个数才有意义。

<img src="https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/y6e84I.png" alt="y6e84I" style="zoom:50%;" />

上述的更新过程叫做滚动更新，也可以理解为灰度发布。比如，在升级刚开始的时候，集群里只有 1 个新版本的 Pod。如果这时，新版本 Pod 有问题启 动不起来，那么“滚动更新”就会停止，从而允许开发和运维人员介入。而在这个过程中，由于 应用本身还有两个旧版本的 Pod 在线，所以服务并不会受到太大的影响。

通过上面的图也可以看出 Deployment 对应用进行版本控制的具体原理。升级的话就是创建新的 ReplicaSet 来控制 pod 的数量。回滚的话其实就是让这个旧 ReplicaSet(hash=1764197365)再次“扩展”成 3 个 Pod，而让新的 ReplicaSet(hash=2156724341)重新“收缩”到 0 个 Pod。

## StatefulSet

在分布式应用中，有一些实例往往有依赖关系，比如主从关系，还有一些数据存储类的应用，它的多个实例，往往都会在本地磁盘上保存一份数据。而这些实例一旦被杀掉，即便重建出来，实例与数据之间的对应关系也已经丢失，从而导致应用失败。

得益于“控制器模式”的设计思想，Kubernetes 项目很早就在 Deployment 的基础上，扩展出了对“有状态应用”的初步支持。这个编排功能，就是:StatefulSet。

StatefulSet 的设计其实非常容易理解。它把真实世界里的应用状态，抽象为了两种情况:

1. 拓扑状态。这种情况意味着，应用的多个实例之间不是完全对等的关系。这些应用实例，必须按照某些顺序启动，比如应用的主节点 A 要先于从节点 B 启动。而如果你把 A 和 B 两个 Pod 删除掉，它们再次被创建出来时也必须严格按照这个顺序才行。并且，新创建出来的 Pod，必须和原来 Pod 的网络标识一样，这样原先的访问者才能使用同样的方法，访问到这个新 Pod。

2. 存储状态。这种情况意味着，应用的多个实例分别绑定了不同的存储数据。对于这些应用实例来说，Pod A 第一次读取到的数据，和隔了十分钟之后再次读取到的数据，应该是同一 份，哪怕在此期间 Pod A 被重新创建过。这种情况最典型的例子，就是一个数据库应用的多个存储实例。

所以，StatefulSet 的核心功能，就是通过某种方式记录这些状态，然后在 Pod 被重新创建时， 能够为新 Pod 恢复这些状态。

### 拓扑状态

Kubernetes 将 Pod 的拓扑状态(比如:哪个节点先启动，哪个节点 后启动)，按照 Pod 的“名字 + 编号”的方式固定了下来。此外，Kubernetes 还为每一个 Pod 提供了一个固定并且唯一的访问入口，即:这个 Pod 对应的 DNS 记录。这些状态，在 StatefulSet 的整个生命周期里都会保持不变，绝不会因为对应 Pod 的删除或者重新创建而失效。

StatefulSet 这个控制器的主要作用之一，就是使用 Pod 模板创建 Pod 的时候， 对它们进行编号(通过 Headless Service 的方式，StatefulSet 为每个 Pod 创建了一个固定并且稳定 的 DNS 记录)，并且按照编号顺序逐一完成创建工作。而当 StatefulSet 的“控 制循环”发现 Pod 的“实际状态”与“期望状态”不一致，需要新建或者删除 Pod 进行“调谐”的时候，它会严格按照这些 Pod 编号的顺序，逐一完成这些操作。

### 存储状态

Kubernetes 项目引入了一组叫作 Persistent Volume Claim(PVC)和 Persistent Volume(PV)的 API 对象，大大降低了用户声明和使用持久化 Volume 的门槛。

Kubernetes 中 PVC 和 PV 的设计，实际上类似于“接口”和“实现”的思想。开发者 只要知道并会使用“接口”，即:PVC;而运维人员则负责给“接口”绑定具体的实现，即: PV。这种解耦，就避免了因为向开发者暴露过多的存储系统细节而带来的隐患。

StatefulSet 管理的 Pod 声明一个对应的 PVC; 这个 PVC 的名字，会被分配一个与这个 Pod 完全一致的编号。这个自动创建的 PVC，与 PV 绑定成功后，就会进入 Bound 状态，这就意味着这个 Pod 可以挂载并使用这个 PV 了。

PVC 其实就是一种特殊的 Volume。只不过一个 PVC 具体是什么类型的 Volume，要在跟某个 PV 绑定之后才知道。当然，PVC 与 PV 的绑定得以实现的前提是，运维人员已经在系统里创建好了符合条件的 PV(比如，我们在前面用到的 pv-volume);或者，你的 Kubernetes 集群运行在公有云上， 这样 Kubernetes 就会通过 Dynamic Provisioning 的方式，自动为你创建与 PVC 匹配的 PV。

当你把一个 Pod，比如 web-0，删除之后，这个 Pod 对应的 PVC 和 PV，并不会被删除，而这个 Volume 里已经写入的数据，也依然会保存在远程存储服务里(比如，我们在这个 例子里用到的 Ceph 服务器)。

此时，StatefulSet 控制器发现，一个名叫 web-0 的 Pod 消失了。所以，控制器就会重新创建 一个新的、名字还是叫作 web-0 的 Pod 来，“纠正”这个不一致的情况。

需要注意的是，在这个新的 Pod 对象的定义里，它声明使用的 PVC 的名字，还是叫作:www- web-0。这个 PVC 的定义，还是来自于 PVC 模板(volumeClaimTemplates)，这是 StatefulSet 创建 Pod 的标准流程。

所以，在这个新的 web-0 Pod 被创建出来之后，Kubernetes 为它查找名叫 www-web-0 的 PVC 时，就会直接找到旧 Pod 遗留下来的同名的 PVC，进而找到跟这个 PVC 绑定在一起的 PV。

这样，新的 Pod 就可以挂载到旧 Pod 对应的那个 Volume，并且获取到保存在 Volume 里的 数据。

通过这种方式，Kubernetes 的 StatefulSet 就实现了对应用存储状态的管理。



首先，StatefulSet 的控制器直接管理的是 Pod。这是因为，StatefulSet 里的不同 Pod 实例， 不再像 ReplicaSet 中那样都是完全一样的，而是有了细微区别的。比如，每个 Pod 的 hostname、名字等都是不同的、携带了编号的。而 StatefulSet 区分这些实例的方式，就是通过在 Pod 的名字里加上事先约定好的编号。

其次，Kubernetes 通过 Headless Service，为这些有编号的 Pod，在 DNS 服务器中生成带有 同样编号的 DNS 记录。只要 StatefulSet 能够保证这些 Pod 名字里的编号不变，那么 Service 里类似于 web-0.nginx.default.svc.cluster.local 这样的 DNS 记录也就不会变，而这条记录解 析出来的 Pod 的 IP 地址，则会随着后端 Pod 的删除和再创建而自动更新。这当然是 Service 机制本身的能力，不需要 StatefulSet 操心。

最后，StatefulSet 还为每一个 Pod 分配并创建一个同样编号的 PVC。这样，Kubernetes 就可以通过 Persistent Volume 机制为这个 PVC 绑定上对应的 PV，从而保证了每一个 Pod 都拥有 一个独立的 Volume。

在这种情况下，即使 Pod 被删除，它所对应的 PVC 和 PV 依然会保留下来。所以当这个 Pod 被重新创建出来之后，Kubernetes 会为它找到同样编号的 PVC，挂载这个 PVC 对应的 Volume，从而获取到以前保存在 Volume 里的数据。

## DaemonSet

DaemonSet 的主要作用，是让你在 Kubernetes 集群里，运行一个 Daemon Pod。 所以，这个 Pod 有如下三个特征:

1. 这个 Pod 运行在 Kubernetes 集群里的每一个节点(Node)上;

2. 每个节点上只有一个这样的 Pod 实例;

3. 当有新的节点加入 Kubernetes 集群后，该 Pod 会自动地在新节点上被创建出来;而当旧节点被删除后，它上面的 Pod 也相应地会被回收掉。

这个机制听起来很简单，但 Daemon Pod 的意义确实是非常重要的。

1. 各种网络插件的 Agent 组件，都必须运行在每一个节点上，用来处理这个节点上的容器网络;

2. 各种存储插件的 Agent 组件，也必须运行在每一个节点上，用来在这个节点上挂载远程存储目录，操作容器的 Volume 目录;

3. 各种监控组件和日志组件，也必须运行在每一个节点上，负责这个节点上的监控信息和日志搜集。

更重要的是，跟其他编排对象不一样，DaemonSet 开始运行的时机，很多时候比整个 Kubernetes 集群出现的时机都要早。那么，DaemonSet 又是如何保证每个 Node 上有且只有一个被管理的 Pod 呢? 显然，这是一个典型的“控制器模型”能够处理的问题。

DaemonSet Controller，首先从 Etcd 里获取所有的 Node 列表，然后遍历所有的 Node。这时，它就可以很容易地去检查，当前这个 Node 上是不是有一个携带了 name=fluentd- elasticsearch 标签的 Pod 在运行。然后根据数量是不是等于 1 来进行操作。

而如何保证先安装Daemon Pod 先安装呢？ DaemonSet 的“过人之处”，其实就是依靠 Toleration 实现的。

在 Kubernetes 项目中，当一个节点的网络插件尚未安装时，这个节点就会被自动加上名为 node.kubernetes.io/network-unavailable的“污点”。

而通过这样一个 Toleration，调度器在调度这个 Pod 的时候，就会忽略当前节点上的“污 点”，从而成功地将网络插件的 Agent 组件调度到这台机器上启动起来。

这种机制，正是我们在部署 Kubernetes 集群的时候，能够先部署 Kubernetes 本身、再部署网络插件的根本原因:因为当时我们所创建的 Weave 的 YAML，实际上就是一个 DaemonSet。



之前有提到过，Deployment 管理这些版 本，靠的是:“一个版本对应一个 ReplicaSet 对象”。可是，DaemonSet 控制器操作的直接就是 Pod，不可能有 ReplicaSet 这样的对象参与其中。那么，它的这些版本又是如何维护的 呢?

Kubernetes v1.7 之后添加了一个 API 对象，名叫ControllerRevision，专门用来记录某种 Controller 对象的版本。个 ControllerRevision 对象，实际上是在 Data 字段保存了该版本对应的完整的 DaemonSet 的 API 对象。并且，在 Annotation 字段保存了创建这个对象所使用的 kubectl 命令。

在 Kubernetes 项目里，ControllerRevision 其实是一个通用的版本管理对象。这样， Kubernetes 项目就巧妙地避免了每种控制器都要维护一套冗余的代码和逻辑的问题。

## Job

Deployment、StatefulSet，以及 DaemonSet 这三 个编排概念。你有没有发现它们的共同之处呢?

实际上，它们主要编排的对象，都是“在线业务”，即:Long Running Task(长作业)。比如，我在前面举例时常用的 Nginx、Tomcat，以及 MySQL 等等。这些应用一旦运行起来，除 非出错或者停止，它的容器进程会一直保持在 Running 状态。

但是，有一类作业显然不满足这样的条件，这就是“离线业务”，或者叫作 Batch Job(计算业务)。这种业务在计算完成后就直接退出了。

在 Job 对象中，负责并行控制的参数有两个:

1. spec.parallelism，它定义的是一个 Job 在任意时间最多可以启动多少个 Pod 同时运行;

2. spec.completions，它定义的是 Job 至少要完成的 Pod 数目，即 Job 的最小完成数。

## CronJob

顾名思义，CronJob 描述的，正是定时任务。

```
apiVersion: batch/v1beta1
kind: CronJob
metadata:
  name: hello
spec:
  schedule: "*/1 * * * *"
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: hello
            image: busybox
            args:
              - /bin/sh
              - -c
        			- date; echo Hello from the Kubernetes cluster
      		restartPolicy: OnFailure
```

在这个 YAML 文件中，最重要的关键词就是jobTemplate。看到它，你一定恍然大悟，原来 CronJob 是一个 Job 对象的控制器(Controller)!

没错，CronJob 与 Job 的关系，正如同 Deployment 与 Pod 的关系一样。CronJob 是一个专 门用来管理 Job 对象的控制器。只不过，它创建和删除 Job 的依据，是 schedule 字段定义的、一个标准的Unix Cron格式的表达式。

## Operator

在 Kubernetes 中，管理“有状态应用”是一个比较复杂的过程，尤其是编写 Pod 模板的时候，总有一种“在 YAML 文件里编程序”的感觉，让人很不舒服。而在 Kubernetes 生态中，还有一个相对更加灵活和编程友好的管理“有状态应用”的解决方案，它就是: Operator。

## PV & PVC

PVC 可以理解为持久化存储的“接口”，它提供了对某种持久化存储的描述，但不提供具体的实现;而这个持久化存储的实现部分则由 PV 负责完成。

在 Kubernetes 中，实际上存在着一个专门处理持久化存储的控制器，叫作 Volume Controller。PersistentVolumeController 会不断地查看当前每一个 PVC，是不是已经处于 Bound(已绑定)状态。如果不是，那它就会遍历所有的、可用的 PV，并尝试将其与这个“单身”的 PVC 进行绑定。

而所谓将一个 PV 与 PVC 进行“绑定”，其实就是将这个 PV 对象的名字，填在了 PVC 对象的 spec.volumeName 字段上。所以，接下来 Kubernetes 只要获取到这个 PVC 对象，就一定能够找到它所绑定的 PV。

所谓容器的 Volume，其实就是将一个宿主机上的目录，跟一个容器里的目录绑定挂载在了一起。

而所谓的“持久化 Volume”，指的就是这个宿主机上的目录，具备“持久性”。即:这个目录里面的内容，既不会因为容器的删除而被清理掉，也不会跟当前的宿主机绑定。这样，当容器被重启或者在其他节点上重建出来之后，它仍然能够通过挂载这个 Volume，访问到这些内容。

取决于你的 Volume 类型挂载操作也不同，大致的步骤是：

这一步为虚拟机挂载远程磁盘的操作，对应的正是“两阶段处理”的第一阶段。在 Kubernetes 中，我们把这个阶段称为 Attach。

这个将磁盘设备格式化并挂载到 Volume 宿主机目录的操作，对应的正是“两阶段处理”的第二个阶段，我们一般称为: Mount。

而经过了“两阶段处理”，我们就得到了一个“持久化”的 Volume 宿主机目录。所以，接下来，kubelet 只要把这个 Volume 目录通过 CRI 里的 Mounts 参数，传递给 Docker，然后就可以为 Pod 里的容器挂载这个“持久化”的 Volume 了。

具体的 Volume 插件的实现接口上，Kubernetes 分别给这两个阶段提供了两种不同的参数列表:

* 对于“第一阶段”(Attach)，Kubernetes 提供的可用参数是 nodeName，即宿主机的名字。

* 而对于“第二阶段”(Mount)，Kubernetes 提供的可用参数是 dir，即 Volume 的宿主机目录。

<img src="/Users/guangzheng.li/Library/Application Support/typora-user-images/image-20210513210501032.png" alt="image-20210513210501032" style="zoom:25%;" />

从图中我们可以看到，在这个体系中:

* PVC 描述的，是 Pod 想要使用的持久化存储的属性，比如存储的大小、读写权限等。

* PV 描述的，则是一个具体的 Volume 的属性，比如 Volume 的类型、挂载目录、远程存储 服务器地址等。

* 而 StorageClass 的作用，则是充当 PV 的模板。并且，只有同属于一个 StorageClass 的 PV 和 PVC，才可以绑定在一起。

当然，StorageClass 的另一个重要作用，是指定 PV 的 Provisioner(存储插件)。这时候，如果你的存储插件支持 Dynamic Provisioning 的话，Kubernetes 就可以自动为你创建 PV 了。

## Networks

![image-20210513212718912](/Users/guangzheng.li/Library/Application Support/typora-user-images/image-20210513212718912.png)

在 Linux 中，能够起到虚拟交换机作用的网络设备，是网桥(Bridge)。它是一个工作在数据链路层(Data Link)的设备，主要功能是根据 MAC 地址学习来将数据包转发到网桥的不同端 口(Port)上。

上面图片是docker的两个容器互相调用的过程，可以看到Docker 项目会默认在宿主机上创建一个名叫 docker0 的网桥，凡是连 接在 docker0 网桥上的容器，就可以通过它来进行通信。

可是，我们又该如何把这些容器“连接”到 docker0 网桥上呢? 这时候，我们就需要使用一种名叫Veth Pair的虚拟设备了。

Veth Pair 设备的特点是:它被创建出来后，总是以两张虚拟网卡(Veth Peer)的形式成对出现的。并且，从其中一个“网卡”发出的数据包，可以直接出现在与它对应的另一张“网卡”上，哪怕这两个“网卡”在不同的 Network Namespace 里。

这就使得 Veth Pair 常常被用作连接不同 Network Namespace 的“网线”。

上述图片 eth0 网卡和 veth9c02e56 虚拟网卡是 Veth Pair 的两端。熟悉了 docker0 网桥的工作方式，你就可以理解，在默认情况下，被限制在 Network Namespace 里的容器进程，实际上是通过 Veth Pair 设备 + 宿主机网桥的方式，实现了跟同 其他容器的数据交换。



同样地，当一个容器试图连接到另外一个宿主机时，比如:ping 10.168.0.3，它发出的请求数据包，首先经过 docker0 网桥出现在宿主机上。然后根据宿主机的路由表里的直连路由规则 (10.168.0.0/24 via eth0))，对 10.168.0.3 的访问请求就会交给宿主机的 eth0 处理。

![Max90T](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/Max90T.png)

在 Docker 的默认配置下，不同宿主机上的容器通过 IP 地址进行互相访问是根本做不到的。

网络插件真正要做的事情，则是通过某种方法，把不同宿主机上的特殊设备连通，从而达到容器跨主机通信的目的。Kubernetes 是通过一个叫作 CNI 的接口，维护了一个单独的网桥来代替 docker0。这个网桥的名字就叫作:CNI 网桥，它在宿主机上的设备名称默认是:cni0。

而  flannel.1 是CNI 插件，维护了 Ip 地址和 MAC 地址。

比如，当 Node 2 启动并加入 Flannel 网络之后，在 Node 1(以及所有其他节点)上，flanneld 就会添加路由规则,这条规则的意思是:凡是发往 10.244.1.1/24 网段的 IP 包，都需要经过 flannel.1 设备发出，并且，它最后被发往的网关地址是:10.244.1.0/32。

![image-20210513214925587](/Users/guangzheng.li/Library/Application Support/typora-user-images/image-20210513214925587.png)

CNI 的设计思想，就是:Kubernetes 在启动 Infra 容器之后，就可以直接调用 CNI 网络插件，为这个 Infra 容器的 Network Namespace，配置符合预期的网络栈。

初始化执行的 CNI 的基础可执行文件，按照功能可以分为三类:

第一类，叫作 Main 插件，它是用来创建具体网络设备的二进制文件。比如，bridge(网桥设备)、ipvlan、loopback(lo 设备)、macvlan、ptp(Veth Pair 设备)，以及 vlan。

Flannel、Weave 等项目，都属于“网桥”类型的 CNI 插件。所以在具体的实现中，它们往往会调用 bridge 这个二进制文件。

第二类，叫作 IPAM(IP Address Management)插件，它是负责分配 IP 地址的二进制文 件。比如，dhcp，这个文件会向 DHCP 服务器发起请求;host-local，则会使用预先配置的 IP 地址段来进行分配。

第三类，是由 CNI 社区维护的内置 CNI 插件。比如:flannel，就是专门为 Flannel 项目提供的 CNI 插件;tuning，是一个通过 sysctl 调整网络设备参数的二进制文件;portmap，是一个通 过 iptables 配置端口映射的二进制文件;bandwidth，是一个使用 Token Bucket Filter (TBF) 来进行限流的二进制文件。

### NetworkPolicy

不难发现，Kubernetes 的网络模型，以及前面这些网络方案的实现，都只关注容器之间网络的“连通”，却并不关心容器之间网络的“隔离”。在 Kubernetes 里，网络隔离能力的定义，是依靠一种专门的 API 对象来描述的，即: NetworkPolicy。

Pod 可以接收来自任何发送方的请求;或者，向任何接收方发送请求。而如果你要对这个情况作出限制，就必须通过 NetworkPolicy 对象来指定。

Kubernetes 网络插件对 Pod 进行隔离，其实是靠在宿主机上生成 NetworkPolicy 对应的 iptable 规则来实现的。

iptables 只是一个操作 Linux 内核 Netfilter 子系统的“界面”。顾名思义，Netfilter 子系统的作用，就是 Linux 内核里挡在“网卡”和“用户态进程”之间的一道“防火墙”。它们的关系，可以用如下的示意图来表示:

<img src="https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/vGsAbW.png" alt="vGsAbW" style="zoom:50%;" />

可以看到，这幅示意图中，IP 包“一进一出”的两条路径上，有几个关键的“检查点”，它们正是 Netfilter 设置“防火墙”的地方。在 iptables 中，这些“检查点”被称为:链 (Chain)。这是因为这些“检查点”对应的 iptables 规则，是按照定义顺序依次进行匹配的。这些“检查点”的具体工作原理，可以用如下所示的示意图进行描述:

![image-20210514113631383](/Users/guangzheng.li/Library/Application Support/typora-user-images/image-20210514113631383.png)

可以看到，当一个 IP 包通过网卡进入主机之后，它就进入了 Netfilter 定义的流入路径(Input Path)里。

在这个路径中，IP 包要经过路由表路由来决定下一步的去向。而在这次路由之前，Netfilter 设置了一个名叫 PREROUTING 的“检查点”。在 Linux 内核的实现里，所谓“检查点”实际上 就是内核网络协议栈代码里的 Hook(比如，在执行路由判断的代码之前，内核会先调用 PREROUTING 的 Hook)。

而在经过路由之后，IP 包的去向就分为了两种:

 第一种，继续在本机处理;

 第二种，被转发到其他目的地。

我们先说一下 IP 包的第一种去向。这时候，IP 包将继续向上层协议栈流动。在它进入传输层之前，Netfilter 会设置一个名叫 INPUT 的“检查点”。到这里，IP 包流入路径(Input Path) 结束。

接下来，这个 IP 包通过传输层进入用户空间，交给用户进程处理。而处理完成后，用户进程会通过本机发出返回的 IP 包。这时候，这个 IP 包就进入了流出路径(Output Path)。

此时，IP 包首先还是会经过主机的路由表进行路由。路由结束后，Netfilter 就会设置一个名叫 OUTPUT 的“检查点”。然后，在 OUTPUT 之后，再设置一个名叫 POSTROUTING“检查点”。

你可能会觉得奇怪，为什么在流出路径结束后，Netfilter 会连着设置两个“检查点”呢? 这就要说到在流入路径里，路由判断后的第二种去向了。

在这种情况下，这个 IP 包不会进入传输层，而是会继续在网络层流动，从而进入到转发路径 (Forward Path)。在转发路径中，Netfilter 会设置一个名叫 FORWARD 的“检查点”。

而在 FORWARD“检查点”完成后，IP 包就会来到流出路径。而转发的 IP 包由于目的地已经确定，它就不会再经过路由，也自然不会经过 OUTPUT，而是会直接来到 POSTROUTING“检查点”。

所以说，POSTROUTING 的作用，其实就是上述两条路径，最终汇聚在一起的“最终检查点”。

需要注意的是，在有网桥参与的情况下，上述 Netfilter 设置“检查点”的流程，实际上也会出现在链路层(二层)，并且会跟我在上面讲述的网络层(三层)的流程有交互。

这些链路层的“检查点”对应的操作界面叫作 ebtables。

所以说，iptables 表的作用，就是在某个具体的“检查点”(比如 Output)上，按顺序执行几个不同的检查动作(比如，先执行 nat，再执行 filter)。

在理解了 iptables 的工作原理之后，我们再回到 NetworkPolicy 上来，在 iptables 设置满足 NetworkPolicy 的规则，不满足的话就会被拒绝掉，从而实现对容器的隔离。

## 服务发现

Kubernetes 之所以需要 Service，一方面是因为 Pod 的 IP 不是固定的，另一方面则是因为一 组 Pod 实例之间总会有负载均衡的需求。

```
apiVersion: v1
kind: Service
metadata:
  name: hostnames
spec:
  selector:
    app: hostnames
  ports:
  - name: default
    protocol: TCP
		port: 80
		targetPort: 9376
```

这个 Service 的例子，相信你不会陌生。其中，我使用了 selector 字段来声明这个 Service 只代理携带了 app=hostnames 标签的 Pod。并且，这个 Service 的 80 端口，代理的是 Pod 的 9376 端口。

Deployment 执行后会生成 Service 的VIP地址，这个 VIP 地址是 Kubernetes 自动为 Service 分配的。通过三次连续不断地访问 Service 的 VIP 地址和代理端口 80，它就为我们依次返回了三个 Pod 的 hostname。这也正印证了 Service 提供的是 Round Robin 方式的负载均衡。对于这种方式，我们称为: ClusterIP 模式的 Service。

Service 是由 kube-proxy 组件，加上 iptables 来共同实现的。通过设置一组 iptables 规则，实现负载均衡。

iptables 规则的匹配是从上到下逐条进行的，所以为了保证上述三条规则每条被选中的概率都相同，我们应该将它们的 probability 字段的值分别设置为 1/3(0.333...)、1/2 和 1。

这么设置的原理很简单:第一条规则被选中的概率就是 1/3;而如果第一条规则没有被选中，那 么这时候就只剩下两条规则了，所以第二条规则的 probability 就必须设置为 1/2;类似地，最 后一条就必须设置为 1。

这样，访问 Service VIP 的 IP 包经过上述 iptables 处理之后，就已经变成了访问具体某一个后端 Pod 的 IP 包了。不难理解，这些 Endpoints 对应的 iptables 规则，正是 kube-proxy 通过监听 Pod 的变化事件，在宿主机上生成并维护的。

以上，就是 Service 最基本的工作原理。Kubernetes 的 kube-proxy 还支持一种叫作 IPVS 的模式。

kube-proxy 通过 iptables 处理 Service 的过程，其实 需要在宿主机上设置相当多的 iptables 规则。而且，kube-proxy 还需要在控制循环里不断地刷新这些规则来确保它们始终是正确的。

不难想到，当你的宿主机上有大量 Pod 的时候，成百上千条 iptables 规则不断地被刷新，会大量占用该宿主机的 CPU 资源，甚至会让宿主机“卡”在这个过程中。所以说，一直以来，基于 iptables 的 Service 实现，都是制约 Kubernetes 项目承载更多量级的 Pod 的主要障碍。

IPVS 模式的工作原理，其实跟 iptables 模式类似。当我们创建了前面的 Service 之后，kube- proxy 首先会在宿主机上创建一个虚拟网卡(叫作:kube-ipvs0)，并为它分配 Service VIP 作为 IP 地址。kube-proxy 就会通过 Linux 的 IPVS 模块，为这个 IP 地址设置三个 IPVS 虚拟主机，并设置这三个虚拟主机之间使用轮询模式 (rr) 来作为负载均衡策略。而 IPVS 模式的 Service，就是解决这个问题的一个行之有效的方法。

而相比于 iptables，IPVS 在内核中的实现其实也是基于 Netfilter 的 NAT 模式，所以在转发这 一层上，理论上 IPVS 并没有显著的性能提升。但是，IPVS 并不需要在宿主机上为每个 Pod 设 置 iptables 规则，而是把对这些“规则”的处理放到了内核态，从而极大地降低了维护这些规则的代价。

## Ingress

从外部访问 Service 有三种方式(NodePort、LoadBalancer 和 External Name)

但是每个 Service 都要有一个负载均衡服务，所以这个做法实际上既浪费成本又高。作为用户，我其实更希望看到 Kubernetes 为我内置一个全局的负载均衡器。然后，通过我访问的 URL，把请求转发给不同的后端 Service。

这种全局的、为了代理不同后端 Service 而设置的负载均衡服务，就是 Kubernetes 里的 Ingress 服务。

所谓 Ingress 对象，其实就是 Kubernetes 项目对“反向代理”的一种抽象。一个 Ingress 对象的主要内容，实际上就是一个“反向代理”服务(比如:Nginx)的配置文件 的描述。而这个代理服务对应的转发规则，就是 IngressRule。

这就是为什么在每条 IngressRule 里，需要有一个 host 字段来作为这条 IngressRule 的入口， 然后还需要有一系列 path 字段来声明具体的转发策略。而有了 Ingress 这样一个统一的抽象，Kubernetes 的用户就无需关心 Ingress 的具体细节了。

在实际的使用中，你只需要从社区里选择一个具体的 Ingress Controller，把它部署在 Kubernetes 集群里即可。

选择一个 Nginx Ingress Controller 为你提供的服务，其实是一个可以根据 Ingress 对象和被代理后端 Service 的变化，来自动进行更新的 Nginx 负载均衡器。



目前，Ingress 只能工作在七层，而 Service 只能工作在四层。所以当你想要在 Kubernetes 里为应用进行 TLS 配置等 HTTP 相关的操作时，都必须通过 Ingress 来进行。

当然，正如同很多负载均衡项目可以同时提供七层和四层代理一样，将来 Ingress 的进化中，也会加入四层代理的能力。这样，一个比较完善的“反向代理”机制就比较成熟了。

而 Kubernetes 提出 Ingress 概念的原因其实也非常容易理解，有了 Ingress 这个抽象，用户就可以根据自己的需求来自由选择 Ingress Controller。比如，如果你的应用对代理服务的中断非常敏感，那么你就应该考虑选择类似于 Traefik 这样支持“热加载”的 Ingress Controller 实现。

## 资源模型

在 Kubernetes 中，像 CPU 这样的资源被称作“可压缩资源”(compressible resources)。 它的典型特点是，当可压缩资源不足时，Pod 只会“饥饿”，但不会退出。

而像内存这样的资源，则被称作“不可压缩资源(compressible resources)。当不可压缩资源不足时，Pod 就会因为 OOM(Out-Of-Memory)被内核杀掉。

而由于 Pod 可以由多个 Container 组成，所以 CPU 和内存资源的限额，是要配置在每个 Container 的定义上的。这样，Pod 整体的资源配置，就由这些 Container 的配置值累加得到。

其中，Kubernetes 里为 CPU 设置的单位是“CPU 的个数”。比如，cpu=1 指的就是，这个 Pod 的 CPU 限额是 1 个 CPU。Kubernetes 允许你将 CPU 限额设置为分数，CPU limits 的值设置为 500m。所谓 500m，指的就是 500 millicpu，也就是 0.5 个 CPU 的意思。这样，这个 Pod 就会被分配到 1 个 CPU 一半的计算能力。

Kubernetes 里 Pod 的 CPU 和内存资源，实际上还要分为 limits 和 requests 两种情况，这两者的区别其实非常简单:在调度的时候，kube-scheduler 只会按照 requests 的值进行计算。而在真正设置 Cgroups 限制的时候，kubelet 则会按照 limits 的值来进行设置。

容器化作业在提交时所设置的资源边界，并不一定是调度系统所必须严格遵守的，这是因为在实际场景中，大多数作业使用到的资源其实远小于它所请求的资源限额。

### QoS

在理解了 Kubernetes 资源模型的设计之后，我再来和你谈谈 Kubernetes 里的 QoS 模型。在 Kubernetes 中，不同的 requests 和 limits 的设置方式，其实会将这个 Pod 划分到不同的 QoS 级别当中。

* 当 Pod 里的每一个 Container 都同时设置了 requests 和 limits，并且 requests 和 limits 值相等的时候，这个 Pod 就属于 Guaranteed 类别

* 而当 Pod 不满足 Guaranteed 的条件，但至少有一个 Container 设置了 requests。那么这 个 Pod 就会被划分到 Burstable 类别。
* 而如果一个 Pod 既没有设置 requests，也没有设置 limits，那么它的 QoS 类别就是 BestEffort

实际上，QoS 划分的主要应用场景，是当宿主机资源紧张的时候，kubelet 对 Pod 进行 Eviction (即资源回收)时需要用到的。在这个配置中，Eviction 在 Kubernetes 里其实分为 Soft 和 Hard 两种模式。

而当 Eviction 发生的时候，kubelet 具体会挑选哪些 Pod 进行删除操作，就需要参考这些 Pod 的 QoS 类别了。

* 首当其冲的，自然是 BestEffort 类别的 Pod。

* 其次，是属于 Burstable 类别、并且发生“饥饿”的资源使用量已经超出了 requests 的 Pod。

* 最后，才是 Guaranteed 类别。并且，Kubernetes 会保证只有当 Guaranteed 类别的 Pod 的资源使用量超过了其 limits 的限制，或者宿主机本身正处于 Memory Pressure 状态时， Guaranteed 的 Pod 才可能被选中进行 Eviction 操作。

在理解了 Kubernetes 里的 QoS 类别的设计之后，我再来为你讲解一下Kubernetes 里一个非常有用的特性:cpuset 的设置。我们知道，在使用容器的时候，你可以通过设置 cpuset 把容器绑定到某个 CPU 的核上，而不 是像 cpushare 那样共享 CPU 的计算能力。

这种情况下，由于操作系统在 CPU 之间进行上下文切换的次数大大减少，容器里应用的性能会得到大幅提升。事实上，cpuset 方式，是生产环境里部署在线应用类型的 Pod 时，非常常用的一种方式。

### 调度

![image-20210514154009122](/Users/guangzheng.li/Library/Application Support/typora-user-images/image-20210514154009122.png)

可以看到，Kubernetes 的调度器的核心，实际上就是两个相互独立的控制循环。

其中，第一个控制循环，我们可以称之为 Informer Path。它的主要目的，是启动一系列 Informer，用来监听(Watch)Etcd 中 Pod、Node、Service 等与调度相关的 API 对象的变化。比如，当一个待调度 Pod(即:它的 nodeName 字段是空的)被创建出来之后，调度器就会通过 Pod Informer 的 Handler，将这个待调度 Pod 添加进调度队列。

在默认情况下，Kubernetes 的调度队列是一个 PriorityQueue(优先级队列)，并且当某些集群信息发生变化的时候，调度器还会对调度队列里的内容进行一些特殊操作。这里的设计，主要是出于调度优先级和抢占的考虑。

此外，Kubernetes 的默认调度器还要负责对调度器缓存(即:scheduler cache)进行更新。 事实上，Kubernetes 调度部分进行性能优化的一个最根本原则，就是尽最大可能将集群信息 Cache 化，以便从根本上提高 Predicate 和 Priority 调度算法的执行效率。

而第二个控制循环，是调度器负责 Pod 调度的主循环，我们可以称之为 Scheduling Path。

Scheduling Path 的主要逻辑，就是不断地从调度队列里出队一个 Pod。然后，调用 Predicates 算法进行“过滤”。这一步“过滤”得到的一组 Node，就是所有可以运行这个 Pod 的宿主机列表。当然，Predicates 算法需要的 Node 信息，都是从 Scheduler Cache 里 直接拿到的，这是调度器保证算法执行效率的主要手段之一。

接下来，调度器就会再调用 Priorities 算法为上述列表里的 Node 打分，分数从 0 到 10。得分最高的 Node，就会作为这次调度的结果。最后进行 Bind。

