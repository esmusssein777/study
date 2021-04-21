# Servlet 规范

## 前言

浏览器发给服务端的是一个 HTTP 格式的请求，HTTP 服务器收到这个请求后，需要调用服务端程序来处理，所谓的服务端程序就是你写的 Controller 类，一般来说不同的请求需要由不同的 Controller 类来处理。

但是服务器又不可能直接调用你的 Controller 类，因为那样 HTTP 服务器和你的程序耦合在一起，改程序代码时还得修改服务器，所以使用 Servlet 规范解决直接调用问题，和Servlet容器解决加载和管理问题。

HTTP 服务器不直接跟业务类打交道，而是把请求交给 Servlet 容器去处理，Servlet 容器会将请求转发到具体的 Servlet，如果这个 Servlet 还没创建，就加载并实例化这个 Servlet，然后调用这个 Servlet 的接口方法。因此 Servlet 接口其实是Servlet 容器跟具体业务类之间的接口。

![TRbGcy](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/TRbGcy.png)

## Servlet 接口

```
public interface Servlet {
 void init(ServletConfig config)

 ServletConfig getServletConfig();

 void service(ServletRequest req, ServletResponse res)
 
 String getServletInfo();

 void destroy();
}
```

其中最重要是的 service 方法，具体业务类在这个方法里实现处理逻辑。这个方法有两个参 数:ServletRequest 和 ServletResponse。ServletRequest 用来封装请求信息， ServletResponse 用来封装响应信息，因此本质上这两个类是对通信协议的封装。

比如 HTTP 协议中的请求和响应就是对应了 HttpServletRequest 和 HttpServletResponse 这两个类。你可以通过 HttpServletRequest 来获取所有请求相关 的信息，包括请求路径、Cookie、HTTP 头、请求参数等。

你可以看到接口中还有两个跟生命周期有关的方法 init 和 destroy，这是一个比较贴心的设 计，Servlet 容器在加载 Servlet 类的时候会调用 init 方法，在卸载的时候会调用 destroy 方法。我们可能会在 init 方法里初始化一些资源，并在 destroy 方法里释放这些资源，比 如 Spring MVC 中的 DispatcherServlet，就是在 init 方法里创建了自己的 Spring 容器。

ServletConfig 的作用就是封装 Servlet 的初始化参数。你可以在 web.xml 给 Servlet 配置参数，并在程序里通过 getServletConfig 方法拿到 这些参数。

我们知道，有接口一般就有抽象类，抽象类用来实现接口和封装通用的逻辑，因此 Servlet 规范提供了 GenericServlet 抽象类，我们可以通过扩展它来实现 Servlet。虽然 Servlet 规 范并不在乎通信协议是什么，但是大多数的 Servlet 都是在 HTTP 环境中处理的，因此 Servet 规范还提供了 HttpServlet 来继承 GenericServlet，并且加入了 HTTP 特性。我们通过继承 HttpServlet 类来实现自己的 Servlet，只需要重写两个方法: doGet 和 doPost。

## Servlet 容器

当客户请求某个资源时，HTTP 服务器会用一个 ServletRequest 对象把客户的请求信息封 装起来，然后调用 Servlet 容器的 service 方法，Servlet 容器拿到请求后，根据请求的 URL 和 Servlet 的映射关系，找到相应的 Servlet，如果 Servlet 还没有被加载，就用反射 机制创建这个 Servlet，并调用 Servlet 的 init 方法来完成初始化，接着调用 Servlet 的 service 方法来处理请求，把 ServletResponse 对象返回给 HTTP 服务器，HTTP 服务器会 把响应发送给客户端。同样我通过一张图来帮助你理解。

![VgVNes](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/VgVNes.png)

Servlet 容器会实例化和调用 Servlet，那 Servlet 是怎么注册到 Servlet 容器中的呢?一般 来说，我们是以 Web 应用程序的方式来部署 Servlet 的，而根据 Servlet 规范，Web 应用程序有一定的目录结构，在这个目录下分别放置了 Servlet 的类文件、配置文件以及静态 资源，Servlet 容器通过读取配置文件，就能找到并加载 Servlet。Web 应用的目录结构大概是下面这样的:

```
| -  MyWebApp
      | -  WEB-INF/web.xml  -- 配置文件，用来配置 Servlet 等
      | -  WEB-INF/lib/     -- 存放 Web 应用所需各种 JAR 包
      | -  WEB-INF/classes/ -- 存放你的应用类，比如 Servlet 类
      | -  META-INF/        -- 目录存放工程的一些信息
```

## 扩展机制

为了满足不同业务的需求，Servlet 规范提供了两种扩展机制:Filter和Listener。

Filter是过滤器，这个接口允许你对请求和响应做一些统一的定制化处理。过滤器的工作原理是这样的: Web 应用部署完成后，Servlet 容器需要实例化 Filter 并把 Filter 链接成一个 FilterChain。当请求进来时，获取第一个 Filter 并调用 doFilter 方法，doFilter 方法负责 调用这个 FilterChain 中的下一个 Filter。

Listener是监听器，这是另一种扩展机制。当 Web 应用在 Servlet 容器中运行时，Servlet 容器内部会不断的发生各种事件，如 Web 应用的启动和停止、用户请求到达等。 Servlet容器提供了一些默认的监听器来监听这些事件，当事件发生时，Servlet 容器会负责调用监 听器的方法。