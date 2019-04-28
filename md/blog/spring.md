### spring IOC

ioc 中的两个容器，一个是beanFactory，还有一个是ApplicationContext。

beanFactory就是加载配置文件，解析成BeanDefinition放到Map里面，在调用getBean时，从BeanDefinition所属的Map里面拿出Class文件进行实例化，同时，如果有依赖关系，将递归的调用getBean方法完成依赖注入

ApplicationContext高级容器，除了低级的容器功能，还有加载不同的资源和层级容器等



Spring Bean的初始化的流程如下：

+ Spring 容器根据配置中的Bean Definition中实例化Bean对象

> Bean Definition 可以通过xml，Java注解和Java Config代码提供

+ spring根据依赖注入填充属性，如Bean中所定义的配置
+ Aware 相关的属性，注入到 Bean 中所定义的配置
  + 如果 Bean 实现了 BeanNameAware 接口，则工厂通过传递 Bean 的 beanName 来调用 ``setBeanName(string name) ``方法
  + 如果 Bean 实现了BeanFactoryAware 接口，则工厂通过传递自身的实例来调用 ``setBeanFactory(BeanFactory beanFactory)`` 方法
  + 如果实现了ApplicationContextAware的方法``setApplicationContext()``方法
+ 调用相关的方法，进一步的初始化 Bean 对象
  + 如果存在和 Bean 关联的 BeanPostProcessor ，则调用预初始化方法（before）`#preProcessBeforeInitialization(Object bean, String beanName)` 方法
  + 如果 Bean 实现了 InitializingBean 接口，则会调用 `#afterPropertiesSet()` 方法
  + 如果 Bean 指定了 **init** ,比如 ``<init-method>``属性，则调用该方法
  + 调用 Bean 相关联的 BeanPostProcessor 的后初始化方法
+ scope 如果为singleton的缓存在SpringIOC容器中，为protoType的生命周期交给客户端

Spring Bean 的销毁流程如下：

+ 如果 Bean 实现了 DisposableBean 接口，当 spring 容器关闭时，会调用distory方法
+ 如果 Bean 指定了 destroy 方法，那么将调用该方法

![](http://static2.iocoder.cn/images/Spring/2018-12-24/08.png)



### spring AOP

#### 术语

Aspect(切面)：aspect  由 pointcut 和 advice 组成 **可以简单地认为, 使用 @Aspect 注解的类就是切面**

advice(增强) ： 

连接点(join point)

切点(point cut)

**满足 pointcut 规则的 joinpoint 会被添加相应的 advice 操作。这一整个动作可以被认为是一个 aspect**



切点标志符(designator)

AspectJ5 的切点表达式由标志符(designator)和操作参数组成. 如 "execution( *greetTo(..))" 的切点表达式, **execution** 就是 标志符, 而圆括号里的* greetTo(..) 就是操作参数

##### execution

匹配 join point 的执行, 例如 "execution(* hello(..))" 表示匹配所有目标类中的 hello() 方法. 这个是最基本的 pointcut 标志符.

##### within

匹配特定包下的所有 join point, 例如 `within(com.xys.*)` 表示 com.xys 包中的所有连接点, 即包中的所有类的所有方法. 而 `within(com.xys.service.*Service)` 表示在 com.xys.service 包中所有以 Service 结尾的类的所有的连接点.

##### this 与 target

this 的作用是匹配一个 bean, 这个 bean(Spring AOP proxy) 是一个给定类型的实例(instance of). 而 target 匹配的是一个目标对象(target object, 即需要织入 advice 的原始的类), 此对象是一个给定类型的实例(instance of).

##### bean

匹配 bean 名字为指定值的 bean 下的所有方法



### spring 事务

