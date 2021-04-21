## Tomcat系统架构

[toc]

## 总体架构

Tomcat要实现一个 Servlet 规范的服务器，需要实现两个核心功能：

* 处理 Socket 连接，负责网络字节流与 Request 和 Response 对象的转化。

* 加载和管理 Servlet，以及具体处理 Request 请求。

因此 Tomcat 设计了两个核心组件连接器(Connector)和容器(Container)来分别做这两件事情。连接器负责对外交流，容器负责内部处理。

Tomcat 支持多种IO模型：NIO非阻塞型IO，NIO2异步IO等，还有HTTP/1.1和HTTP/2不同的应用层协议。

所以Tomcat 为了实现支持多种 I/O 模型和应用层协议，一个容器可能对接多个连接器。但是单独的连接器或者容器都不能对外提供服务，需要把它们组装起来才能工作，组装后这个整体叫作 Service 组件。

![image-20210328173536448](/Users/guangzheng.li/Library/Application Support/typora-user-images/image-20210328173536448.png)

从图上你可以看到，最顶层是 Server，这里的 Server 指的就是一个 Tomcat 实例。一个 Server 中有一个或者多个 Service，一个 Service 中有多个连接器和一个容器。连接器与容器之间通过标准的 ServletRequest 和 ServletResponse 通信。

## 连接器

连接器对 Servlet 容器屏蔽了协议及 I/O 模型等的区别，无论是 HTTP 还是 AJP，在容器 中获取到的都是一个标准的 ServletRequest 对象。

我们可以把连接器的功能需求进一步细化，比如:

1. 监听网络端口。

2. 接受网络连接请求。

3. 读取请求网络字节流。

4. 根据具体应用层协议(HTTP/AJP)解析字节流，生成统一的 Tomcat Request 对象。 将 Tomcat Request 对象转成标准的 ServletRequest。

5. 调用 Servlet 容器，得到 ServletResponse。

6. 将 ServletResponse 转成 Tomcat Response 对象。

7. 将 Tomcat Response 转成网络字节流。 将响应字节流写回给浏览器。

通过分析连接器的详细功能列表，我们发现连接器需要完成 3 个高内聚的功能:

* 网络通信。

* 应用层协议解析。

* Tomcat Request/Response 与 ServletRequest/ServletResponse 的转化。

因此 Tomcat 的设计者设计了 3 个组件来实现这 3 个功能，分别是 EndPoint、Processor 和 Adapter。

EndPoint 负责提供字节流给 Processor，Processor 负责提供 Tomcat Request 对象给 Adapter，Adapter 负责提供 ServletRequest 对象给容器。

如果要支持新的 I/O 方案、新的应用层协议，只需要实现相关的具体子类，上层通用的处 理逻辑是不变的。

由于 I/O 模型和应用层协议可以自由组合，比如 NIO + HTTP 或者 NIO2 + AJP。Tomcat 的设计者将网络通信和应用层协议解析放在一起考虑，设计了一个叫 ProtocolHandler 的     

接口来封装这两种变化点。各种协议和通信模型的组合有相应的具体实现类。比如: Http11NioProtocol 和 AjpNioProtocol。

除了这些变化点，系统也存在一些相对稳定的部分，因此 Tomcat 设计了一系列抽象基类 来封装这些稳定的部分，抽象基类 AbstractProtocol 实现了 ProtocolHandler 接口。每 一种应用层协议有自己的抽象基类，比如 AbstractAjpProtocol 和 AbstractHttp11Protocol，具体协议的实现类扩展了协议层抽象基类。下面整理一下它们的继承关系。

连接器模块用三个核心组件:Endpoint、Processor 和 Adapter 来分别做三件事情，其中 Endpoint 和 Processor 放在一起抽象成了 ProtocolHandler 组件，它们的关系如下图所示。

![BrQWYw](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/BrQWYw.png)

### ProtocolHandler 组件

连接器用 ProtocolHandler 来处理网络连接和应用层协议，包含了 2 个重要组件:EndPoint 和 Processor，下面我来详细介绍它们的工作原理。

* EndPoint

EndPoint 是通信端点，即通信监听的接口，是具体的 Socket 接收和发送处理器，是对传 输层的抽象，因此 EndPoint 是用来实现 TCP/IP 协议的。

EndPoint 是一个接口，对应的抽象实现类是 AbstractEndpoint，而 AbstractEndpoint 的具体子类，比如在 NioEndpoint 和 Nio2Endpoint 中，有两个重要的子组件: Acceptor 和 SocketProcessor。

其中 Acceptor 用于监听 Socket 连接请求。SocketProcessor 用于处理接收到的 Socket 请求，它实现 Runnable 接口，在 Run 方法里调用协议处理组件 Processor 进行处理。为 了提高处理能力，SocketProcessor 被提交到线程池来执行。

* Processor

如果说 EndPoint 是用来实现 TCP/IP 协议的，那么 Processor 用来实现 HTTP 协议， Processor 接收来自 EndPoint 的 Socket，读取字节流解析成 Tomcat Request 和Response 对象，并通过 Adapter 将其提交到容器处理，Processor 是对应用层协议的抽 象。

Processor 是一个接口，定义了请求的处理等方法。它的抽象实现类 AbstractProcessor 对一些协议共有的属性进行封装，没有对方法进行实现。具体的实现有 AJPProcessor、 HTTP11Processor 等，这些具体实现类实现了特定协议的解析方法和请求处理方式。

我们再来看看连接器的组件图:

![image-20210328181306566](/Users/guangzheng.li/Library/Application Support/typora-user-images/image-20210328181306566.png)

从图中我们看到，EndPoint 接收到 Socket 连接后，生成一个 SocketProcessor 任务提交 到线程池去处理，SocketProcessor 的 Run 方法会调用 Processor 组件去解析应用层协 议，Processor 通过解析生成 Request 对象后，会调用 Adapter 的 Service 方法。

### Adapter 组件

ProtocolHandler 接口负责解析请求并生成 Tomcat Request 类。但是这个 Request 对象不是标准的 ServletRequest，也就意味着， 不能用 Tomcat Request 作为参数来调用容器。

Tomcat 设计者的解决方案是引入 CoyoteAdapter，这是适配器模式的经典运用，连接器调用 CoyoteAdapter 的 Sevice 方法，传入的是 Tomcat Request 对象，CoyoteAdapter 负责将 Tomcat Request 转成 ServletRequest，再调用容器的 Service 方法。

## 容器

Tomcat 有两个核心组件:连接器和容器，其中连接器负责外部交流，容器负责内部处理。具体来说就是，连接器处理 Socket 通信和应用层协议的解析，得到 Servlet 请求;而容器则负责处理 Servlet 请求。

Tomcat 设计了 4 种容器，分别是 Engine、Host、Context 和 Wrapper。这 4 种容器不 是平行关系，而是父子关系。

![image-20210328181839004](/Users/guangzheng.li/Library/Application Support/typora-user-images/image-20210328181839004.png)

Tomcat 通过一种分层的架构，使得 Servlet 容器具有很好的灵活性。

Engine 表示引擎，用来管理多个虚拟站点，一个 Service 最多只能有一个 Engine。

Host 代表的是一个虚拟主机，或者说一个站点，可以给 Tomcat 配置多个虚拟主机地址，而一个虚拟主机下可以部署多个 Web 应用程序;

Context 表示一个 Web 应用程序;

Wrapper 表示一个 Servlet，一个 Web 应用程序中可能会有多个 Servlet;

这些容器具有父子关系，形成一个树形结构，设计模式中的组合模式可以帮助来管理这些容器。

具体实现方法是，所有容器组件都实现了 Container 接口，因此组合模式可以使得用户对单容器对象和组合容器对象的使用具有一致性。这里单容器对象指的是最底层的 Wrapper，组合容器对象指的是上面的 Context、Host 或者 Engine。Container 接口定义如下:

```
public interface Container extends Lifecycle {
    public void setName(String name);
    public Container getParent();
    public void setParent(Container container);
    public void addChild(Container child);
    public void removeChild(Container child);
    public Container findChild(String name);
}
```

我们在上面的接口看到了 getParent、SetParent、addChild 和 removeChild 等方法。你可能还注意到 Container 接口扩展了 LifeCycle 接口，LifeCycle 接口用来统一管理各组件的生命周期。

### 请求定位 Servlet 的过程

Tomcat 用 多层多Mapper结构来完成定位过程。一个请求的URL最后只会定位到一个 Wrapper 容器，也就是一个Servlet。

![yvTWAn](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/yvTWAn.png)

其中每一层的容器都会对请求做一些处理，做完一些处理后，才会将请求传给下一层的容器。这是通过责任链模式来实现的。

Pipeline-Valve 是指在一个请求处理的过程中有很多处理者依次对请求进行处理，每个处理者负责做自己相应的处理，处理完之后将再调用下一个处理者继续处理。其中关键方法如下：

```
public interface Valve {
	public Valve getNext();
  public void setNext(Valve valve);
  public void invoke(Request request, Response response)
}

public interface Pipeline extends Contained {
	public void addValve(Valve valve);
	public Valve getBasic();
	public void setBasic(Valve valve);
	public Valve getFirst();
}
```

每一个容器都有一个 Pipeline 对象，只要触发这个 Pipeline 的第一个 Valve，这个容器里 Pipeline 中的 Valve 就都会被调用到。Valve 完成自己的处理后，调用 getNext.invoke() 来触发下一个 Valve 调用。

Pipeline 中还有个 getBasic 方法。这个 BasicValve 处于 Valve 链表的末端，它是 Pipeline 中必不可少的一个 Valve，负责调用下层容器的 Pipeline 里的第一个 Valve。

![image-20210329105952134](/Users/guangzheng.li/Library/Application Support/typora-user-images/image-20210329105952134.png)

## 启动Tomcat

![jPyVXE](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/jPyVXE.png)

可以看到整体的架构图如上所示，那么有一个问题，就是我们要怎样才能管理好上面的这些组件呢？

设计就是要找到系统的变化点和不变点。这里的不变点就是每个组件都要经历创建、初始化、启动这几个过程，这些状态以及状态的转化是不变的。而变化点是每个具体组件的初始化方法，也就是启动方法是不一样的。

因此，我们把不变点抽象出来成为一个接口，这个接口跟生命周期有关，叫作 LifeCycle。 LifeCycle 接口里应该定义这么几个方法:init()、start()、stop() 和 destroy()，每个具体的组件去实现这些方法。

理所当然，在父组件的 init() 方法里需要创建子组件并调用子组件的 init() 方法。同样，在 父组件的 start() 方法里也需要调用子组件的 start() 方法，因此调用者可以无差别的调用各 组件的 init() 方法和 start() 方法，这就是组合模式的使用，并且只要调用最顶层组件，也 就是 Server 组件的 init() 和 start() 方法，整个 Tomcat 就被启动起来了。下面是 LifeCycle 接口的定义。

```
public interface LifeCycle {
	void init();
	void start();
	void stop();
	void destory
}
```

我们知道可以通过 Tomcat 的 /bin 目录下的脚本 startup.sh 来启动 Tomcat。在执行脚本后，会启动一个 JVM 来运行 Tomcat 的启动类 Bootstrap。Bootstrap 的主要任务是初始化 Tomcat 的类加载器，并且创建 Catalina。Catalina 是一个启动类，它通过解析 server.xml、创建相应的组件，并调用 Server 的 start 方法。接着会按照上面的流程图一直start下去。

![ZpjhOF](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/ZpjhOF.png)