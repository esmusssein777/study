### IOC源码解析

![](http://ww1.sinaimg.cn/large/007JYYsTgy1g3wb43464yj30m50nm41a.jpg)

该图为 ClassPathXmlApplicationContext 的类继承体系结构，虽然只有一部分，但是它基本上包含了 IoC 体系中大部分的核心类和接口。

下面我们就针对这个图进行简单的拆分和补充说明

### 1.1 Resource 体系

`org.springframework.core.io.Resource`，对资源的抽象。它的每一个实现类都代表了一种资源的访问策略，如 ClassPathResource、RLResource、FileSystemResource 等

![](http://ww1.sinaimg.cn/large/007JYYsTgy1g3wbjuwp4zj31fe0h0myd.jpg)

### 1.2 ResourceLoader 体系

有了资源，就应该有资源加载，Spring 利用 `org.springframework.core.io.ResourceLoader` 来进行统一资源加载，类图如下：

![](http://ww1.sinaimg.cn/large/007JYYsTgy1g3wbk28fgkj318k0d4k6f.jpg)

## 2.1 BeanFactory 体系

`org.springframework.beans.factory.BeanFactory`，是一个非常纯粹的 bean 容器，它是 IoC 必备的数据结构，其中 BeanDefinition 是它的基本结构。BeanFactory 内部维护着一个BeanDefinition map ，并可根据 BeanDefinition 的描述进行 bean 的创建和管理

![](http://ww1.sinaimg.cn/large/007JYYsTgy1g3wbk93xcwj317w0m0ab3.jpg)

- BeanFactory 有三个直接子类 ListableBeanFactory、HierarchicalBeanFactory 和 AutowireCapableBeanFactory 。
- DefaultListableBeanFactory 为最终默认实现，它实现了所有接口。

## 2.2 BeanDefinition 体系

`org.springframework.beans.factory.config.BeanDefinition` ，用来描述 Spring 中的 Bean 对象。

![](http://ww1.sinaimg.cn/large/007JYYsTgy1g3wbkero4oj30mi0h03z0.jpg)

## 2.4 BeanDefinitionReader 体系

`org.springframework.beans.factory.support.BeanDefinitionReader` 的作用是读取 Spring 的配置文件的内容，并将其转换成 Ioc 容器内部的数据结构 ：BeanDefinition 。

![](http://ww1.sinaimg.cn/large/007JYYsTgy1g3wbklcu3aj315q0cm0ta.jpg)

## 2.5 ApplicationContext 体系

`org.springframework.context.ApplicationContext` ，这个就是大名鼎鼎的 Spring 容器，它叫做应用上下文，与我们应用息息相关。它继承 BeanFactory ，所以它是 BeanFactory 的扩展升级版，如果BeanFactory 是屌丝的话，那么 ApplicationContext 则是名副其实的高富帅。由于 ApplicationContext 的结构就决定了它与 BeanFactory 的不同，其主要区别有：

1. 继承 `org.springframework.context.MessageSource` 接口，提供国际化的标准访问策略。
2. 继承 `org.springframework.context.ApplicationEventPublisher` 接口，提供强大的**事件**机制。
3. 扩展 ResourceLoader ，可以用来加载多种 Resource ，可以灵活访问不同的资源。
4. 对 Web 应用的支持。

![](http://ww1.sinaimg.cn/large/007JYYsTgy1g3wbkqf3zxj30nz0kxmyr.jpg)