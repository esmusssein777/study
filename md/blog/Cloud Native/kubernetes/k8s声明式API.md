## 声明式 API

当我把一个 YAML 文件提交给 Kubernetes 之后，它究竟是如何创建出 一个 API 对象的呢?

这得从声明式 API 的设计谈起了。

在 Kubernetes 项目中，一个 API 对象在 Etcd 里的完整资源路径，是由:Group(API 组)、 Version(API 版本)和 Resource(API 资源类型)三个部分组成的。

![xtxoqI](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/xtxoqI.png)

在这个 YAML 文件中，“CronJob”就是这个 API 对象的资源类型 (Resource)，“batch”就是它的组(Group)，v2alpha1 就是它的版本(Version)。

```
apiVersion: batch/v2alpha1
kind: CronJob
```

首先，Kubernetes 会匹配 API 对象的组。

需要明确的是，对于 Kubernetes 里的核心 API 对象，比如:Pod、Node 等，是不需要 Group 的(即:它们 Group 是“”)。所以，对于这些 API 对象来说，Kubernetes 会直接在 /api 这个层级进行下一步的匹配过程。

而对于 CronJob 等非核心 API 对象来说，Kubernetes 就必须在 /apis 这个层级里查找它对应 的 Group，进而根据“batch”这个 Group 的名字，找到 /apis/batch。

不难发现，这些 API Group 的分类是以对象功能为依据的，比如 Job 和 CronJob 就都属 于“batch” (离线业务)这个 Group。

然后，Kubernetes 会进一步匹配到 API 对象的版本号。

对于 CronJob 这个 API 对象来说，Kubernetes 在 batch 这个 Group 下，匹配到的版本号就 是 v2alpha1。

在 Kubernetes 中，同一种 API 对象可以有多个版本，这正是 Kubernetes 进行 API 版本化管 理的重要手段。这样，比如在 CronJob 的开发过程中，对于会影响到用户的变更就可以通过升 级新版本来处理，从而保证了向后兼容。

最后，Kubernetes 会匹配 API 对象的资源类型。

在前面匹配到正确的版本之后，Kubernetes 就知道，我要创建的原来是一个 /apis/batch/v2alpha1 下的 CronJob 对象。

这时候，APIServer 就可以继续创建这个 CronJob 对象了。

![uJUWMC](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/uJUWMC.png) 

首先，当我们发起了创建 CronJob 的 POST 请求之后，我们编写的 YAML 的信息就被提交给 了 APIServer。

而 APIServer 的第一个功能，就是过滤这个请求，并完成一些前置性的工作，比如授权、超时 处理、审计等。

然后，请求会进入 MUX 和 Routes 流程。如果你编写过 Web Server 的话就会知道，MUX 和 Routes 是 APIServer 完成 URL 和 Handler 绑定的场所。而 APIServer 的 Handler 要做的事情，就是按照刚刚介绍的匹配过程，找到对应的 CronJob 类型定义。

接着，APIServer 最重要的职责就来了:根据这个 CronJob 类型定义，使用用户提交的 YAML 文件里的字段，创建一个 CronJob 对象。

而在这个过程中，APIServer 会进行一个 Convert 工作，即:把用户提交的 YAML 文件，转换成一个叫作 Super Version 的对象，它正是该 API 资源类型所有版本的字段全集。这样用户提交的不同版本的 YAML 文件，就都可以用这个 Super Version 对象来进行处理了。

接下来，APIServer 会先后进行 Admission() 和 Validation() 操作。比如，Admission Controller 和 Initializer，就都属于 Admission 的内容。

而 Validation，则负责验证这个对象里的各个字段是否合法。这个被验证过的 API 对象，都保存在了 APIServer 里一个叫作 Registry 的数据结构中。也就是说，只要一个 API 对象的定义能 在 Registry 里查到，它就是一个有效的 Kubernetes API 对象。

最后，APIServer 会把验证过的 API 对象转换成用户最初提交的版本，进行序列化操作，并调 用 Etcd 的 API 把它保存起来。



Kubernetes 能够对 API 对象进行在线更新的能力， 这也正是Kubernetes“声明式 API”的独特之处:

* 首先，所谓“声明式”，指的就是我只需要提交一个定义好的 API 对象来“声明”，我所期望的状态是什么样子
* 其次，“声明式 API”允许有多个 API 写端，以 PATCH 的方式对 API 对象进行修改，而无需关心本地原始 YAML 文件的内容。
* 最后，也是最重要的，有了上述两个能力，Kubernetes 项目才可以基于对 API 对象的增、 删、改、查，在完全无需外界干预的情况下，完成对“实际状态”和“期望状态”的调谐 (Reconcile)过程

Istio 项目的设计与实现，其实都依托于 Kubernetes 的声明式 API 和它所提供的各种编排能力。使用 sidecar 容器来代理所有的流量，并且使用 Initializer 来实现 PATCH 对所有代理的 Pod 进行修改，开起流量代理功能。