## 什么是IoC

Spring 框架的核心是 Spring IoC 容器。容器创建 Bean 对象，将它们装配在一起，配置它们并管理它们的完整生命周期。

- Spring 容器使用依赖注入来管理组成应用程序的 Bean 对象。
- 容器通过读取提供的配置元数据 Bean Definition 来接收对象进行实例化，配置和组装的指令。
- 该配置元数据 Bean Definition 可以通过 XML，Java 注解或 Java Config 代码提供

简单的来讲，即不需要你自己来管理你创建的对象了，这一切都交给Spring容器去管理。



## IoC的过程

当我们使用Spring的时候，以XML配置为例，我们在xml文件配置好`<bean>`标签，然后在项目中直接`@Autowired`注入即可使用。那么这个过程涉及到了哪些呢。我们整个IoC源码就是研究这个过程。我们在这里先做一个大致的介绍。

IoC 容器的初始化过程分为三步骤：Resource 定位、BeanDefinition 的载入和解析，BeanDefinition 注册。

TODO:未完成

需要完成上面的这些过程我们需要用到下面的三个包

* org.springframework.bean

* org.springframework.context

* org.springframework.core

### bean

org.springframework.beans 包下。这个包下的所有类主要解决了三件事：`Bean 的定义、Bean 的创建以及对 Bean 的解析`。对 Spring 的使用者来说唯一需要关心的就是 Bean 的创建，其他两个由 Spring 在内部帮你完成了

Bean 的``定义``就是完整的描述了在 Spring 的配置文件中你定义的 <bean/> 节点中所有的信息，包括各种子节点。当 Spring 成功解析你定义的一个 <bean/> 节点后，在 Spring 的内部就被转化成 BeanDefinition 对象。以后所有的操作都是对这个对象完成的。

Bean 的``解析``过程非常复杂，功能被分的很细，因为这里需要被扩展的地方很多，必须保证有足够的灵活性，以应对可能的变化。Bean 的解析主要就是对 Spring 配置文件的解析

### context

Context 组件在 Spring 中的作用，他实际上就是给 Spring 提供一个运行时的环境，用以保存各个对象的状态。下面看一下这个环境是如何构建的。

ApplicationContext 是 Context 的顶级父类，他除了能标识一个应用环境的基本信息外，他还继承了五个接口，这五个接口主要是扩展了 Context 的功能

总体来说 ApplicationContext 必须要完成以下几件事：

- 标识一个应用环境
- 利用 BeanFactory 创建 Bean 对象
- 保存对象关系表
- 能够捕获各种事件
- Context 作为 Spring 的 Ioc 容器，基本上整合了 Spring 的大部分功能，或者说是大部分功能的基础

### core

Core 组件作为 Spring 的核心组件，他其中包含了很多的关键类，其中一个重要组成部分就是定义了资源的访问方式。这种把所有资源都抽象成一个接口的方式很值得在以后的设计中拿来学习

 Resource 接口封装了各种可能的资源类型，也就是对使用者来说屏蔽了文件类型的不同

 下面看一下 Context 和 Resource 是如何建立关系的？
 Context 是把资源的加载、解析和描述工作委托给了 ResourcePatternResolver 类来完成，他相当于一个接头人，他把资源的加载、解析和资源的定义整合在一起便于其他组件使用