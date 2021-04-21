[toc]

## 一、事务简介

[![img](https://mydlq-club.oss-cn-beijing.aliyuncs.com/images/springboot-transaction-1002.png?x-oss-process=style/shuiyin)](https://mydlq-club.oss-cn-beijing.aliyuncs.com/images/springboot-transaction-1002.png?x-oss-process=style/shuiyin)

### 1、事务是什么

事务是一组原子操作单元，从数据库角度说，就是一组 SQL 指令向数据库提交，要么全部执行成功，要么撤销不执行。

### 2、事务的作用

​    事务的概念是为解决数据安全操作提出的，事务控制实际上就是控制数据的安全访问。而事务管理对于企业级应用而言至关重要，它保证了用户的每一次操作都是可靠的，即便出现了异常的访问情况，也不至于破坏后台数据的完整性。

就像银行的自动提款机 ATM，通常 ATM 都可以正常为客户服务，但是也难免遇到操作过程中及其突然出故障的情况，此时事务的作用就会凸显出来，它能保障在 ATM 出现故障前未完成的操作不会生效，从而使用户和银行双方利益不受损失。

### 3、事务的四个属性

事务概念包含四个（ACID）特性，主要有：

- **原子性（Atomicity）：** 原子性是指，这一个事务当中执行的多个操作，它要么都执行完成，要么都不完成，它不会出现只完成其中一部分这种情况。
- **一致性（Consistency）：** 一致性是指，这个事务完成以后，它们的状态改变是一致的，它的结果是完整的。一致性更多的是从数据的状态或者结果来表现。
- **隔离性（Isolation）：** 隔离性是指，在执行不同的事务，它们试图操纵同样的数据的时候，它们之间是相互隔离的。
- **持久性（Durability）：** 持久性是指，当事务提交以后，数据操作的结果会存储到数据库中永久保存。如果事务还没有提交前，就出现一些故障或者系统宕机等情况，导致事务没有提交，数据的修改不会出现在数据库当中。

### 4、什么是 Spring 事务

这里要说的 Spring 事务其实指的是 Spring 框架中的事务模块。在 Spring 框架中，对执行数据库事务的操作进行了一系列封装，其本质的实现还是在数据库，假如数据库不支持事务的话 Spring 的事务也不会起作用，且 Spring 对事务进行了加强，添加了事务传播行为等功能。

## 二、Spring 事务抽象

### 1、Spring 中事务核心接口类

在 Spring 中核心的事务接口主要由以下组成：

- **PlatformTransactionManager：** 事务管理器。
- **TransactionDefinition：** 事务的一些基础属性定义，例如事务的传播属性、隔离级别、超时时间等。
- **TransactionStatus：** 事务的一些状态信息，如是否是一个新的事务、是否已被标记为回滚。

[![img](https://mydlq-club.oss-cn-beijing.aliyuncs.com/images/springboot-transaction-1003.png?x-oss-process=style/shuiyin)](https://mydlq-club.oss-cn-beijing.aliyuncs.com/images/springboot-transaction-1003.png?x-oss-process=style/shuiyin)

### 2、事务管理器（PlatformTransactionManager）

​    在 `Spring` 框架中并不直接管理事务，而是提供 `PlatformTransactionManager` 事务管理器接口类，对事务的概念进行抽象。它将事务的实现交由其它持久层框架。例如 Hibernate 或 Mybatis 等都是实现了 `Spring` 事务的第三方持久层框架，由于每个框架中事务的实现方式各不相同，所以 `Spring` 对事务接口进行了统一，事务的提交、回滚等操作全部交由 `PlatformTransactionManager` 实现类来进行实现。

实现了事务管理器接口的实现类主要有：

- **org.springframework.jdbc.datasource.DataSourceTransactionManager：** 使用 JDBC 或者 MyBatis 进行持久化数据时使用。
- **org.springframework.transaction.jta.JtaTransactionManager：** 使用一个 JTA 实现来管理事务，在一个事务跨越多个资源时必须使用。
- **org.springframework.orm.hibernate5.HibernateTransactionManager：** 使用 Hibernate5 版本进行持久化数据时使用。
- **org.springframework.orm.jpa.JpaTransactionManager：** 使用 JPA 进行持久化数据时使用。
- **org.springframework.jdo.JdoTransactionManager：** 当持久化机制是 JDO 时使用。
- **org.springframework.jms.connection.JmsTransactionManager：** 当使用支持 JMS 协议的消息队列时使用。

下面再来看看 `PlatformTransactionManager` 接口类中存在哪些需要实现的方法，其接口类内容如下：

- **PlatformTransactionManager 接口类**

```java
public interface PlatformTransactionManager {
    /** 根据设定的事务传播行为，返回当前活动的事务或创建一个新的事务务 **/
    TransactionStatus getTransaction(@Nullable TransactionDefinition definition) throws TransactionException;
    
    /** 提交事务 **/
    void commit(TransactionStatus status) throws TransactionException;
    
    /** 回滚事务 **/
    void rollback(TransactionStatus status) throws TransactionException;
}
```

### 3、接口类-事务定义（TransactionDefinition）

事务定义接口类 `TransactionDefinition` 中，主要定义了事务的`传播级别`、`隔离级别`、`超时时间`等属性，和一些获取其相关属性的方法，接口类内容如下：

- **TransactionDefinition 接口类**

```java
public interface TransactionDefinition {
    /** 事务的传播行为 **/
    int PROPAGATION_REQUIRED = 0;
    int PROPAGATION_SUPPORTS = 1;
    int PROPAGATION_MANDATORY = 2;
    int PROPAGATION_REQUIRES_NEW = 3;
    int PROPAGATION_NOT_SUPPORTED = 4;
    int PROPAGATION_NEVER = 5;
    int PROPAGATION_NESTED = 6;
    
    /** 事务的隔离级别 **/
    int ISOLATION_DEFAULT = -1;
    int ISOLATION_READ_UNCOMMITTED = 1;
    int ISOLATION_READ_COMMITTED = 2;
    int ISOLATION_REPEATABLE_READ = 4;
    int ISOLATION_SERIALIZABLE = 8;
    
    /** 事务默认超时时间 **/
    int TIMEOUT_DEFAULT = -1;

    /** 返回事务传播行为 **/
    default int getPropagationBehavior() {
        return PROPAGATION_REQUIRED;
    }
    /** 返回事务的隔离级别 **/   
    default int getIsolationLevel() {
        return ISOLATION_DEFAULT;
    }
    /** 返回事务超时时间 **/
    default int getTimeout() {
        return TIMEOUT_DEFAULT;
    }
    /** 返回是否作为只读事务进行优化 **/
    default boolean isReadOnly() {
        return false;
    }
    /** 返回当前事务的名称 **/
    String getName();
}
```

**部分参数解释：**

- 事务是否可读：利用数据库事务的只读属性，进行事务优化处理。
- 事务超时：事务超时就是事务的一个定时器，在特定时间内事务如果没有执行完毕，那么就会自动回滚，而不是一直等待其结束。
- 事务回滚：默认情况下，事务只有遇到运行时异常时才会回滚，而在遇到检查异常时不会回滚。

**注意事项：**

- 不同的数据库对事务是否“只读”属性支持不同，Oracle 的只读属性不起作用，不影响其增删改查。而 Mysql 的只读属性只能查，增删改则会异常。
- 为了使应用程序很好的运行，事务不能运行太长的时间。因为事务执行时会涉及对后端的数据库的锁定，长时间的事务会不必要的占用数据库资源。

### 4、接口类-事务状态（TransactionStatus）

这个 `TransactionStatus` 接口类顾名思义，是事务的状态有关的接口类，在这个接口类中提供了获取事务相关状态的方法，比如 `hasSavepoint()` 方法可以获取事务是否有回滚点，`isCompleted()` 事务是否执行完成等等。

- **TransactionStatus 接口类**

```java
public interface TransactionStatus extends TransactionExecution, SavepointManager, Flushable {
    /** 判断是否有回滚点 **/
    boolean hasSavepoint();
    /** 对数据存储中的底层会话执行刷新，一般需要接口实现中实现这个刷新机制 **/
    @Override
    void flush();
}
```

可以看到 `TransactionStatus` 接口还继承了另外两个接口类，分别为 `TransactionExecution`、`SavepointManager` 这里也进行一下介绍。

- **TransactionExecution 接口类**

```java
public interface TransactionExecution {
    /** 返回当前事务是否为新事务 **/
    boolean isNewTransaction();
    /** 设置事务仅回滚 **/
    void setRollbackOnly();
    /** 返回事务是否已标记为仅回滚 **/ 
    boolean isRollbackOnly();
    /** 返回是否完成事务 **/
    boolean isCompleted();
}
```

- **SavepointManager 接口类**

```java
public interface SavepointManager {
    /** 创建回滚点 **/
    Object createSavepoint() throws TransactionException;
    /** 回滚到回滚点 **/
    void rollbackToSavepoint(Object savepoint) throws TransactionException;
    /** 释放回滚点 **/
    void releaseSavepoint(Object savepoint) throws TransactionException;
}
```

## 三、Spring 事务隔离机制

### 1、为什么需要事务隔离机制

当数据库中有多个事务同时执行时，就可能出现一些问题，比如发生 `脏读`、`不可重复读`、`幻读` 等，下面简单说明了下数据库操作过程中问题出现的原因：

- 脏读：

   

  一个事务读取了另一个事务未提交的数据。

  - 流程：
  - ①、事务1读取一条数据并进行修改；
  - ②、事务2读取了事务1修改后的那条数据；
  - ③、事务1在执行另外逻辑时发生错误，进行回滚操作；
  - ④、事务2现在得到的数据仍然是事务1未提交前的数据（发生回滚导致未提交），即脏数据。

- 不可重复读：

   

  一个事务先后读取相同的数据，发现两次读取的数据内容不一致。

  - 流程：
  - ①、事务1读取一条数据；
  - ②、事务2对事务1读取的那条数据进行了修改，进行提交；
  - ③、数据1在再次读取那条数据，发现其和第一次读取的内容不一致（被事务2修改过了），这就发生了不可重复读；

- 幻读：

   

  一个事务按相同的查询条件重新读取以前检索过的数据，却发现其它事务插入了满足其查询条件的新数据。

  - 流程：
  - ①、事务1根据条件 a 检索到 n 行数据。
  - ②、事务2新插入了一条符合条件 a 数据。
  - ③、事务1再次根据条件 a 检索，发现数据变为 n+1 行，多了一条数据（事务2插入的数据），让人以为是发生了幻觉，这就是幻读。

- **三种问题的级别高低：** 脏读 < 不可重复读 < 幻读

为了解决上面这些问题，就有了事务隔离级别概念，其中数据库中标准隔离级别有：

- 读未提交（read uncommitted）
- 读已提交（read commited）
- 可重复读（repeatable read）
- 串行化（serializable）

其中隔离级别越高安全性也就越高，但安全性提高的同时会使并发性能降低，在正常使用事务隔离级别时，往往都会从中寻找一个比较平衡的级别。

### 2、事务的隔离级别

**数据库中定义的隔离级别：**

在 Spring 中事务本质是使用数据库事务，而数据库中一般会定义事务隔离级别，比如 Mysql 中会定义下面隔离级别：

- read-uncommitted（读未提交）： 可以看到未提交的数据，也就是可能发生”脏读”、”不可重复度”、”幻读”。
- read-committed（读已提交）： 可以读取提交的数据，但可能发生”不可重复读”、”幻读”导致多次读取的数据结果不一致。
- repeatable-read（可重复读）： 可以重复读取，但可能发生”幻读”。
- serializable（串行化）： 可读不可写，这时最高隔离级别，不会发生上面三种可能出现的问题，但是该模式下写数据必须等待另一个事务结束，对性能有较大影响。

**Spring 中定义的隔离级别：**

设置不同的事务隔离级别能解决不同的问题，在 Spring 中定义了五个事务隔离级别，每个隔离级别都有不同作用，如下：

- TransactionDefinition.ISOLATION_DEFAULT： 使用数据库中配置的默认的隔离级别。
- TransactionDefinition.ISOLATION_READ_UNCOMMITTED： 最低的隔离级别，允许读取已改变而没有提交的数据，可能会导致脏读、幻读或不可重复读。
- TransactionDefinition.ISOLATION_READ_COMMITTED： 允许读取事务已经提交的数据，可以阻止脏读，但是可能发生幻读、不可重复读。
- TransactionDefinition.ISOLATION_READ_REPEATABLE_READ： 对同一字段的多次读取结果都是一致的，除非数据事务本身改变，可以阻止脏读、不可重复读，但是可能发生幻读。
- TransactionDefinition.ISOLATION_SERIALIZABLE： 最高的隔离级别，完全服从 ACID 的隔离级别，确保不发生脏读、不可重复读以及幻读，也是最慢的事务隔离级别，因为它通常是通过完全锁定事务相关的数据库表来实现的。

**事务隔离级别归纳到表格：**

其中可能发生的问题，将其归纳到到表格中进行整理，内容如下：

- **√：可能发生**  **–： 不会发生**

| 隔离级别                                   | 脏读                       | 不可重复读 | 幻读 |
| :----------------------------------------- | :------------------------- | :--------- | :--- |
| ISOLATION_DEFAULT（默认级别）              | 使用数据库中设置的隔离级别 |            |      |
| ISOLATION_READ_UNCOMMITTED（读未提交）     | √                          | √          | √    |
| ISOLATION_READ_COMMITTED（读已提交）       | –                          | √          | √    |
| ISOLATION_READ_REPEATABLE_READ（可重复读） | –                          | –          | √    |
| ISOLATION_SERIALIZABLE（序列化）           | –                          | –          | –    |

## 四、Spring 事务传播行为

### 1、为什么需要事务传播行为

​    在 Spring 中定义了七种事务传播行为，这种传播行为主要是为了解决 `事务方法` 调用 `事务或非事务方法` 时，如何处理事务的传播行为。例如，在 Spring 中对事物的控制是通过 `AOP` 切面实现的，大部分都是通过使用 `@Transactional` 注解来使用事务，这样一来存在一些问题。

比如存在 ServiceA 和 ServiceB 两个类，且两个类中都存在一些方法，模拟如下：

- **ServiceA 类：mA()**
- **ServiceB 类：mB()**

那么这时候可以出现下面问题：

- **场景一：** `ServiceA` 的 `mA()` 方法调用了 `ServiceB` 中的 `mB()` 方法（**即 Service.mA()->ServiceB.mB()**），两个方法中都添加了 `@Transactional` 注解，设置了事务。这时候如果 `ServiceB.mB()` 执行出现异常，那么是 `Service.mB()` 单独回滚还是与 `Service.mA()` 一起回滚？
- **场景二：** `ServiceA` 的 `mA()` 方法调用了 `ServiceB` 中的 `mB()` 方法（**即 Service.mA()->ServiceB.mB()**），但是只有 `ServiceA.mA()` 中设置了事务，而 `ServiceB.mB()` 没有事务，这时候是否也把 `ServiceB.mB()` 加入到 `ServiceA.mA()` 中的事务中？如果 `ServiceB.mB()` 发生错误 `ServiceA.mA()` 是否也跟着回滚？
- …（场景比较多，省略）

正是存在上面这些问题，所以 Spring 中设置了事务传播行来供开发者自己定义，事务方法在调用”事务方法”或者”非事务方法”间的行为，是加入到当前事务中还是新建事务或抛出异常等。

### 2、传播机制生效条件

因为 Spring 是使用 AOP 来实现事务控制的，是针对接口或者类的，所以在同一个类中的两个方法的调用，事务传播机制是不生效的。

- 事务方法调用本类中的方法（无效）
- 事务方法调用另一个类中的方法（有效）
- 非事务方法调用本类的事务方法（无效）
- 非事务方法调用另一个类中的事务方法（有效）

### 3、七种事务传播行为

- TransactionDefinition.PROPAGATION_REQUIRED（默认）：
  - 如果该方法执行在没有事务的方法中，就创建一个新的事务。
  - 如果执行在已经存在事务的方法中，则加入到这个事务中，合并成一个事务。
- TransactionDefinition.PROPAGATION_SUPPORTS：
  - 如果该方法执行在没有事务的方法中，就以非事务方式执行。
  - 如果执行在已经存在事务的方法中，则加入到这个事务中，合并成一个事务。
- TransactionDefinition.PROPAGATION_MANDATORY：
  - 如果该方法执行在没有事务的方法中，就抛出异常。
  - 如果执行在已经存在事务的方法中，则加入到这个事务中，合并成一个事务。
- TransactionDefinition.PROPAGATION_REQUIRES_NEW：
  - 无论该方法是否执行在事务的方法中，都创建一个新的事务。
  - 不过如果执行在存在事务的方法中，就将方法中的事务暂时挂起。
  - 新的事务会独立提交与回滚，不受调用它的父方法的事务影响。
- TransactionDefinition.PROPAGATION_NOT_SUPPORTED：
  - 无论该方法是否执行在事务的方法中，都以非事务方式执行。
  - 不过如果执行在存在事务的方法中，就将该事务暂时挂起。
- TransactionDefinition.PROPAGATION_NEVER：
  - 如果该方法执行在没有事务的方法中，就也以非事务方式执行。
  - 不过如果执行在存在事务的方法中，就抛出异常。
- TransactionDefinition.PROPAGATION_NESTED：
  - 如果该方法执行在没有事务的方法中，就创建一个新的事务。
  - 如果执行在已经存在事务的方法中，则在当前事务中嵌套创建子事务执行。
  - 被嵌套的事务可以独立于封装事务进行提交或回滚。
  - 如果外部事务提交嵌套事务也会被提交，如果外部事务回滚嵌套事务也会进行回滚。

### 4、事务传播行为详解

还是按照上面”为什么需要事务传播行为”中将的场景，存在 ServiceA 和 ServiceB 两个类，且两个类中都存在方法，模拟如下：

- ServiceA 类：mA()
- ServiceB 类：mB()

**(1)、PROPAGATION_REQUIRED**

**①如果该方法执行在没有事务的方法中，就创建一个新的事务。
②如果执行在已经存在事务的方法中，则加入到这个事务中，合并成一个事务。**

ServiceA：

```java
@Service
public class ServiceA {

    @Autowired
    private ServiceB serviceB;

    public void mA() {
        // 业务逻辑（略）
        // 调用另一个类中方法，测试事务传播行为
        serviceB.mB();
    }
    
}
```

ServiceB：

```java
@Service
public class ServiceB {

    @Transactional(propagation = Propagation.REQUIRED)
    public void mB() {
        System.out.println("方法 mB()");
        // 业务逻辑（略）
    }

}
```

**①解释：**

上面两个类中，只有 `ServiceB.B()` 设置了事务，且设置的事务传播行为是 `PROPAGATION_REQUIRED`，当设置此传播行为时，当 `ServiceA.mA1()` 运行调用 `ServiceB.mB()` 时，`ServiceB.mB()` 发现自己执行在没有事务的 `ServiceA.mA1()` 方法中，这时 `ServiceB.mB()` 会新建一个事务。

**②解释：**

上面两个类中的方法都设置了事务，且设置的事务传播行为是 `PROPAGATION_REQUIRED`，当设置此传播行为时，当 `ServiceA.mA2()` 运行调用 `ServiceB.mB()` 时，`ServiceB.mB()` 发现自己执行在已经存在 `ServiceA.mA2()` 设置的事务中，这时 `ServiceB.mB()` 不会再创建事务，而是直接加入到 `ServiceA.mA2()` 设置的事务中。这样，当 `ServiceA.mA2()` 或者 `ServiceB.mB()` 方法内发生异常时，两者都会回滚。

**(2)、PROPAGATION_SUPPORTS**

**①如果该方法执行在没有事务的方法中，就以非事务方式执行。
②如果执行在已经存在事务的方法中，则加入到这个事务中，合并成一个事务。**

```java
@Service
public class ServiceA {

    @Autowired
    private ServiceB serviceB;

    public void mA() {
        // 调用另一个类中方法，测试事务传播行为
        serviceB.mB();
    }
    
    @Transactional
    public void mA() {
        // 调用另一个类中方法，测试事务传播行为
        serviceB.mB();
    }

}
@Service
public class ServiceB {

    @Transactional(propagation = Propagation.SUPPORTS)
    public void mB() {
        System.out.println("方法 mB()");
        // 业务逻辑（略）
    }
    
}
```

**①解释：**

上面两个类中，只有 `ServiceB.B()` 设置了事务，且设置的事务传播行为是 `PROPAGATION_SUPPORTS`，当设置此传播行为时，当 `ServiceA.mA1()` 运行调用 `ServiceB.mB()` 时，`ServiceB.mB()` 发现自己执行在没有事务的 `ServiceA.mA1()` 方法中，这时 `ServiceB.mB()` 也以非事务方式执行。

**②解释：**

上面两个类中方法都设置了事务，但 `ServiceA.mA2()` 没有指定事务传播行为，所以会使用默认的传播行为 `PROPAGATION_REQUIRED`。在 `ServiceB.mB()` 中设置的事务传行为是 `PROPAGATION_SUPPORTS`，当设置此传播行为时，当 `ServiceA.mA2()` 运行调用 `ServiceB.mB()` 时，`ServiceB.mB()` 发现自己执行在已经存在 `ServiceA.mA2()` 设置的事务中，这时 `ServiceB.mB()` 不会再创建事务，而是直接加入到 `ServiceA.mA2()` 设置的事务中。这样，当 `ServiceA.mA2()` 或者 `ServiceB.mB()` 方法内发生异常时，两者都会回滚。

**(3)、PROPAGATION_MANDATORY**

**①如果该方法执行在没有事务的方法中，就抛出异常。
②如果执行在已经存在事务的方法中，则加入到这个事务中。**

```java
@Service
public class ServiceA {

    @Autowired
    private ServiceB serviceB;

    public void mA() {
        // 调用另一个类中方法，测试事务传播行为
        serviceB.mB();
    }
        
    @Transactional
    public void mA2() {
        // 调用另一个类中方法，测试事务传播行为
        serviceB.mB();
    }
    
}
@Service
public class ServiceB {

    @Transactional(propagation = Propagation.MANDATORY)
    public void mB() {
        System.out.println("方法 mB()");
        // 业务逻辑（略）
    }

}
```

**①解释：**

上面两个类中，方法 ServiceA.mA1() 没有设置事务，而方法 ServiceB.mB() 设置了事务，且事务的传播行为是 `PROPAGATION_MANDATORY`。`ServiceA.mA1()` 运行调用 `ServiceB.mB()` 时，方法 `ServiceB.mB()` 发现调用自己的方法并没设置事务，这时就抛出 `org.springframework.transaction.IllegalTransactionStateException` 异常。

**②解释：**

上面两个类中方法都设置了事务，但 `ServiceA.mA2()` 没有指定事务传播行为，所以会使用默认的传播行为 `PROPAGATION_REQUIRED`。在 `ServiceB.mB()` 中设置的事务传行为是 `PROPAGATION_MANDATORY`，当设置此传播行为时，当 `ServiceA.mA2()` 运行调用 `ServiceB.mB()` 时，`ServiceB.mB()` 发现自己执行在已经存在 `ServiceA.mA2()` 设置的事务中，这时 `ServiceB.mB()` 不会创建新的事务，也不抛出异常，而是直接加入到 `ServiceA.mA2()` 设置的事务中。

**(4)、PROPAGATION_REQUIRES_NEW**

**①无论该方法是否执行在事务的方法中，都创建一个新的事务。
②不过如果执行在存在事务的方法中，就将方法中的事务暂时挂起。
③新的事务会独立提交与回滚，不受调用它的父方法的事务影响。**

```java
@Service
public class ServiceA {

    @Autowired
    private ServiceB serviceB;

    public void mA1() {
        // 调用另一个类中方法，测试事务传播行为
        serviceB.mB();
    }
    
    @Transactional
    public void mA2() {
        // 调用另一个类中方法，测试事务传播行为
        serviceB.mB();
    }
    
}
```

ServiceB：

```java
@Service
public class ServiceB {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void mB() {
        System.out.println("方法 mB()");
        // 业务逻辑（略）
    }

}
```

**①解释：**

上面方法中 `ServiceA.mA1()` 没有设置事务，而 `ServiceB.mB()` 设置了事务，且设置的事务行为是 `PROPAGATION_REQUIRES_NEW`。`ServiceA.mA1()` 运行调用 `ServiceB.mB()` 时，方法 `ServiceB.mB()` 发现调用自己的方法并没设置事务，这时方法 `ServiceB.mB()` 会创建一个新的事务。新事务中的提交与回滚不受调用它的父方法事务影响。

**②③解释：**

上面方法中 `ServiceA.mA2()` 和 `ServiceB.mB()` 都存在事务，当 `ServiceA.mA2()` 运行调用 `ServiceB.mB()` 时，`ServiceB.mB()` 发现自己执行在已经存事务的方法中，这时 `ServiceB.mB()` 会创建一个新的事务，且将 `ServiceA.mA2()` 中的事务暂时挂起，等 `ServiceB.mB()` 完成后，恢复 `ServiceA.mA2()` 的事务。

由于 `ServiceB.mB()` 是新起一个事务，那么 `ServiceA.mA2()` 在调用 `ServiceB.mB()` 执行时 `ServiceA.mA()` 事务被挂起，那么：

- 假设 `ServiceB.mB()` 已经提交，那么 `ServiceA.mA2()` 抛出异常进行回滚，这时 `ServiceB.mB()` 是不会回滚的。
- 假设 `ServiceB.mB()` 异常回滚，假设他抛出的异常被 `ServiceA.mA2()` 捕获，`ServiceA.mA2()` 事务仍然可能提交。

**(5)、PROPAGATION_NOT_SUPPORTED**

**①无论该方法是否执行在事务的方法中，都以非事务方式执行。
②不过如果执行在存在事务的方法中，就将该事务暂时挂起。**

```java
@Service
public class ServiceA {

    @Autowired
    private ServiceB serviceB;

    public void mA1() {
        // 调用另一个类中方法，测试事务传播行为
        serviceB.mB();
    }
    
    @Transactional
    public void mA2() {
        // 调用另一个类中方法，测试事务传播行为
        serviceB.mB();
    }
    
}
@Service
public class ServiceB {

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void mB() {
        System.out.println("方法 mB()");
        // 业务逻辑（略）
    }

}
```

**①解释：**

上面方法中 `ServiceA.mA1()` 没有设置事务，而 `ServiceB.mB()` 设置了事务，且设置的事务行为是 `PROPAGATION_NOT_SUPPORTED`。`ServiceA.mA1()` 运行调用 `ServiceB.mB()` 时，方法 `ServiceB.mB()` 发现调用自己的方法并没设置事务，这时方法 `ServiceB.mB()` 以非事务方式执行。

**②解释：**

上面两个类中方法都设置了事务，在 `ServiceB.mB()` 中设置的事务传行为是 `PROPAGATION_NOT_SUPPORTED`，当设置此传播行为时，当 `ServiceA.mA2()` 运行调用 `ServiceB.mB()` 时，`ServiceB.mB()` 发现调用自己的方法 `ServiceA.mA2()` 中存在事务，所以这时候会将 `ServiceA.mA2()` 的事务暂时挂起，等 `ServiceB.mB()` 逻辑执行完成后，再开启 `ServiceA.mA2()` 的事务，以继续执行 `ServiceA.mA2()` 中的逻辑。

**(6)、PROPAGATION_NEVER**

**①如果该方法执行在没有事务的方法中，就也以非事务方式执行。
②不过如果执行在存在事务的方法中，就抛出异常。**

```java
@Service
public class ServiceA {

    @Autowired
    private ServiceB serviceB;

    public void mA1() {
        // 调用另一个类中方法，测试事务传播行为
        serviceB.mB();
    }
    
    @Transactional
    public void mA2() {
        // 调用另一个类中方法，测试事务传播行为
        serviceB.mB();
    }
    
}
@Service
public class ServiceB {

    @Transactional(propagation = Propagation.NEVER)
    public void mB() {
        System.out.println("方法 mB()");
        // 业务逻辑（略）
    }

}
```

**①解释：**

上面方法中 `ServiceA.mA1()` 没有设置事务，而 `ServiceB.mB()` 设置了事务，且设置的事务行为是 `PROPAGATION_NEVER`。`ServiceA.mA1()` 运行调用 `ServiceB.mB()` 时，方法 `ServiceB.mB()` 发现调用自己的方法并没有设置事务，这时方法 `ServiceB.mB()` 以非事务方式执行。

**②解释：**

上面示例中两个方法都设置了事务，`ServiceB.mB()` 设置的事务行为是 `PROPAGATION_NEVER`。`ServiceA.mA2()` 运行调用 `ServiceB.mB()` 时，方法 `ServiceB.mB()` 发现调用自己方法也存在事务，这时方法 `ServiceB.mB()` 抛出 `org.springframework.transaction.IllegalTransactionStateException` 异常。

**(7)、PROPAGATION_NESTED**

**①如果该方法执行在没有事务的方法中，就创建一个新的事务。
②如果执行在已经存在事务的方法中，则在当前事务中嵌套创建子事务执行。
③被嵌套的事务可以独立于封装事务进行提交或回滚。
④如果外部事务提交嵌套事务也会被提交，如果外部事务回滚嵌套事务也会进行回滚。**

```java
@Service
public class ServiceA {

    @Autowired
    private ServiceB serviceB;

    public void mA1() {
        // 调用另一个类中方法，测试事务传播行为
        serviceB.mB();
    }
    
    @Transactional
    public void mA2() {
        // 调用另一个类中方法，测试事务传播行为
        serviceB.mB();
    }
    
}
@Service
public class ServiceB {

    @Transactional(propagation = Propagation.NESTED)
    public void mB() {
        System.out.println("方法 mB()");
        // 业务逻辑（略）
    }

}
```

**①解释：**

上面方法中 `ServiceA.mA1()` 没有设置事务，而 `ServiceB.mB()` 设置了事务，且设置的事务行为是 `PROPAGATION_NEVER`。`ServiceA.mA1()` 运行调用 `ServiceB.mB()` 时，方法 `ServiceB.mB()` 发现调用自己的方法并没有设置事务，这时方法 `ServiceB.mB()` 就会创建一个新的事务。

**②③④解释：**

上面示例中两个方法都设置了事务，`ServiceB.mB()` 设置的事务行为是 `PROPAGATION_NESTED`。`ServiceA.mA2()` 运行调用 `ServiceB.mB()` 时，方法 `ServiceB.mB()` 发现调用自己方法也存在事务，这时方法 `ServiceB.mB()` 也会创建一个新的事务，与 `ServiceA.mA2()` 的事务形成嵌套事务。被嵌套的事务可以独立于封装事务进行提交或回滚。如果外部事务提交嵌套事务也会被提交，如果外部事务回滚嵌套事务也会进行回滚。

### 5、事务传播行为总结

| 传播行为      | 描述                                  | 是否支持事务 | 是否开启新事务 | 回滚规则                                                     |
| :------------ | :------------------------------------ | :----------- | :------------- | :----------------------------------------------------------- |
| REQUIRED      | 存在事务加入， 不存在创新事务         | √            | 不一定         | 存在一个事务： 1、外部有事务加入，异常回滚； 2、外部没事务创建新事务，异常回滚； |
| SUPPORTS      | 存在事务加入， 不存在以非事务         | √            | x              | 最多只存在一个事务： 1、外部有事务加入，异常回滚； 2、外部没事务，内部非事务，异常不回滚； |
| MANDATORY     | 存在事务加入， 不存在则抛异常         | √            | x              | 最多只存在一个事务： 1、外部存在事务加入，异常回滚； 2、外部不存在事务，异常无法回滚； |
| REQUIRES_NEW  | 存在事务挂起创新事务， 不存在创新事务 | √            | x              | 可能存在1-2个事务： 1、外部存在事务挂起，创建新，异常回滚自己的事务 2、外部不存在事务，创建新， 异常只回滚新事务； |
| NOT_SUPPORTED | 存在事务挂起， 不存在以非事务         | √            | x              | 最多只存在一个事务： 1、 外部有事务挂起，外部异常回滚；内部非事务，异常不回滚； 2、外部无事务，内部非事务，异常不回滚； |
| NEVER         | 存在事务抛异常                        | √            | x              | 最多只存在一个事务： 1、外部有事务，外部异常回滚；内部非事务不回滚； 2、外部非事务，内部非事务，异常不回滚； |
| NESTED        | 存在事务进行嵌套事务， 不存在创新事务 | √            | x              | 存在一个事务： 1、 外部有事务，嵌套事务创建保存点，外部异常回滚全部事务； 内部嵌套事务异常回滚到保存点； 2、外部不存在事务，内部创建新事务，内部异常回滚； |

### 6、传播行为间的差异

**(1)、ROPAGATION_REQUIRES_NEW 与 PROPAGATION_NESTED 的差异**

传播行为 `PROPAGATION_REQUIRES_NEW` 与 `PROPAGATION_NESTED` 事务比较相似，很多时候很难分清，这里简要说明下俩个的区别：

**# ROPAGATION_REQUIRES_NEW：**

- 设置为 ROPAGATION_REQUIRES_NEW 时会创建一个”新事务”，而不依赖于环境的”内部”事务。
- 新事务的”提交”或”回滚”不依赖于外部事务，它拥有自己的隔离范围、锁等等。
- 当新的内部事务开始执行时，外部事务将被挂起，内务事务结束时，外部事务将继续执行。

**# PROPAGATION_NESTED：**

- PROPAGATION_NESTED 开始一个”嵌套的事务”，它是已经存在事务的一个真正的子事务。
- 嵌套事务开始执行时，它将取得一个 Savepoint 回滚点，如果这个嵌套事务失败，我们将回滚到此回滚点。
- 嵌套事务是外部事务的一部分，只有外部事务结束后它才会被提交。

**(2)、事务的差异总结**

方法 mA 调用方法 mB 时：

| 异常状态      | PROPAGATION_REQUIRES_NEW (两个独立事务)                      | PROPAGATION_NESTED (B的事务嵌套在A的事务中) | PROPAGATION_REQUIRED (同一个事务) |
| :------------ | :----------------------------------------------------------- | :------------------------------------------ | :-------------------------------- |
| mA正常 mB正常 | B先提交，A再提交                                             | A、B一起提交                                | A、B一起提交                      |
| mA异常 mB正常 | B先提交，A再回滚                                             | A与B一起回滚                                | A与B一起回滚                      |
| mA正常 mB异常 | （1）、如果A中捕获B的异常，并没有继续向上抛异常， 则B先回滚，A再正常提交； （2）、如果A未捕获B的异常，默认则会将B的异常向上抛， 则B先回滚，A再回滚 | B先回滚，A再正常提交                        | A与B一起回滚                      |
| mA异常 mB异常 | B先回滚，A再回滚                                             | A与B一起回滚                                | A与B一起回滚                      |

## 五、Spring 事务的两种实现

### 1、编程式事务和声明式事务

Spring 支持“编程式事务”管理和“声明式事务”管理两种方式：

- **编程式事务：** 编程式事务使用 TransactionTemplate 或者直接使用底层的 PlatformTransactionManager 实现事务。 对于编程式事务 Spring 比较推荐使用 TransactionTemplate 来对事务进行管理。
- **声明式事务：** 声明式事务是建立在 AOP 之上的。其本质是对方法前后进行拦截，然后在目标方法开始之前创建或者加入一个事务，在执行完目标方法之后根据执行情况“提交”或者“回滚”事务。

### 2、两种事务管理间的区别

- 编程式事务允许用户在代码中精确定义事务的边界。
- 声明式事务有助于用户将操作与事务规则进行解耦，它是基于 AOP 交由 Spring 容器实现，是开发人员只重点关注业务逻辑实现。

​    编程式事务侵入到了业务代码里面，但是提供了更加纤细的事务管理。而声明式事务基于 AOP，所以既能起到事务作用，又可以不影响业务代码的具体实现。一般而言比较推荐使用声明式事务，尤其是使用 `@Transactional` 注解，它能很好地帮助开发者实现事务的同时，也减少代码开发量，且使代码看起来更加清爽整洁。

## 六、Spring 编程式事务

### 1、编程式事务实现方式

​    Spring 中编程式事务是实现事务的两种方式之一，所谓编程式事务就是将执行事务的方法嵌入到业务代码中，手动的进行事务的提交与回滚操作，虽然对代码有一个的侵入性，但是其细度可控，可以在方法内非常方便设置事务个隔离级别、传播行为及事务的提交与回滚等操作。

一般来说编程式事务有两种方法可以实现：

- **模板事务的方式（TransactionTemplate）：** 主要是使用 TransactionTemplate 类实现事务，这也是 Spring 官方比较推荐的一种编程式使用方式；
- **平台事务管理器方式（PlatformTransactionManager）：** 这里使用最基本的事务管理局对事务进行管理，借助 Spring 事务的 PlatformTransactionManager 及 TransactionDefinition 和 TransactionStatus 三个核心类对事务进行操作。

### 2、模板事务实现

模板事务方式实现事务步骤：

- ① 获取模板对象 TransactionTemplate；
- ② 选择事务结果类型；
- ③ 业务数据操作处理；
- ④ 业务执行完成事务提交或者发生异常进行回滚；

其中 TransactionTemplate 的 execute 能接受两种类型参数执行事务，分别为：

- **TransactionCallback<Object>()：** 执行事务且可以返回一个值。
- **TransactionCallbackWithoutResult()：** 执行事务没有返回值。

下面是使用 TransactionTemplate 的实例：

```java
@Service
public class TransactionExample {
    
    /** 1、获取 TransactionTemplate 对象 **/
    @Autowired
    private TransactionTemplate transactionTemplate;
    
    public void addUser() {
        // 2、使用 TransactionCallback 或者 TransactionCallbackWithoutResult 执行事务
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                try {
                    // 3、执行业务代码（这里进行模拟，执行多个数据库操作方法）
                    userMapper.delete(1);
                    userMapper.delete(2);
                } catch (Exception e) {
                    // 4、发生异常，进行回滚
                    transactionStatus.setRollbackOnly();
                }
            }
        });
    }
    
}
```

### 3、事务管理器方式实现

事务管理器方式实现事务步骤：

- ① 获取事务管理器 PlatformTransactionManager；
- ② 获取事务属性定义对象 TransactionDefinition；
- ③ 获取事务状态对象 TransactionStatus；
- ④ 业务数据操作处理；
- ⑤ 进行事务提交 commit 操作或者发生异常进行事务回滚 rollback 操作；

```java
@Service
public class TransactionExample {
    
    /** 1、获取 PlatformTransactionManager 对象 **/
    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    public void addUser() {
        // 2、获取默认事务定义
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        // 设置事务传播行为
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        // 3、根据事务定义对象设置的属性，获取事务状态
        TransactionStatus status = platformTransactionManager.getTransaction(def);
        try {
            // 4、执行业务代码（这里进行模拟，执行多个数据库操作方法）
            userMapper.delete(1);
            userMapper.delete(2);
            // 5、事务进行提交
            platformTransactionManager.commit(status);
        } catch(Exception e){
            // 5、事务进行回滚
            platformTransactionManager.rollback(status);
        }
    }
    
}
```

## 七、Spring 声明式事务

### 1、什么是声明式事务

​    声明式事务（declarative transaction management）顾名思义就是使用声明的方式来处理事务。该方式是基于 `Spring AOP` 实现的，将具体业务逻辑和事务处理解耦，其本质是在执行方法前后进行拦截，在方法开始之前创建或者加入一个事务，在执行完目标方法之后根据执行情况提交或者回滚事务。

常用的声明式事务使用方法有 `XML` 和 `@Transactional` 注解两种方法，由于近几年 `SpringBoot` 的流行，提供很方便的自动化配置，致使 `XML` 方式已经逐渐淘汰，比较推荐使用注解的方式。

### 2、@Transactional 的作用范围

​    注解 `@Transactional` 不仅仅可以添加在方法上面，还可以添加到类级别上，当注解放在类级别时，表示所有该类的公共方法都配置相同的事务属性信息。如果类级别配置了 `@transactional`，方法级别也配置了 `@transactional`，应用程序会以方法级别的事务属性信息来管理事务，换言之，方法级别的事务属性信息会覆盖类级别的相关配置。

> 注意：一般而言，不推荐将 @Transaction 配置到类上，因为这样很可能使后来的维护人员必须强制使用事务。

### 3、@Transactional 注解使用方法

使用 `@Transactional` 实现事务非常简单，只要在类或者方法上添加 `@Transactional` 该注解即可，如下：

```java
@Service   // 实现
public class TestService {

    @Transactional(rollbackFor = Exception.class)
    public void test() {
        // 业务逻辑
    }
    
}
```

在 @Transactional 注解中存在很多参数可以配置，如下：

- **value：** **事务管理器**，此配置项是设置 Spring 容器中的 Bean 名称，这个 Bean 需要实现接口 PlatformTransactionManager。
- **transactionManager：** **事务管理器**，该参数和 value 配置保持一致，是同一个东西。
- **isolation：** **事务隔离级别**，默认为 Isolation.DEFAULT 级别。
- **propagation：** **事务传播行为**，默认为 Propagation.REQUIRED。
- **timeout：** **事务超时时间**，单位为秒，默认值为-1，当事务超时时会抛出异常，进行回滚操作。
- **readOnly：** **是否开启只读事务**，是否开启只读事务，默认 false。
- **rollbackForClassName：** **回滚事务的异常类名定义**，同 rollbackFor，只是用类名定义。
- **noRollbackForClassName：** **指定发生哪些异常名不回滚事务**，参数为类数组，同 noRollbackFor，只是使用类的名称定义。
- **rollbackFor：** **回滚事务异常类定义**，当方法中出异常，且异常类和该参数指定的类相同时，进行回滚操作，否则提交事务。
- **noRollbackFor：** **指定发生哪些异常不回滚事务**，当方法中出异常，且异常类和该参数指定的类相同时，不回滚而是将继续提交事务。

### 4、@Transactional 回滚规则

异常分为 `运行时异常`、`非运行时异常` 和 `Error`，其中：

- 当发生 Error 错误时，@Transactional 默认会自动回滚。
- 当发生运行时异常（即 RuntimeException 和其继承的子类）时，@Transactional 默认会自动回滚。
- 当发生非运行时异常（即 Exception 和其继承的子类）时，@Transactional 默认不会自动回滚，需要配置参数 @Transactional(rollbackFor=Exception.class) 才能使其进行回滚。
- 如果 @Transactional(propagation=Propagation.NOT_SUPPORTED) 参数时，默认不支持事务，发生错误和异常不会进行回滚。

### 5、@Transactional 事务实现机制

[![img](https://mydlq-club.oss-cn-beijing.aliyuncs.com/images/springboot-transaction-1004.png?x-oss-process=style/shuiyin)](https://mydlq-club.oss-cn-beijing.aliyuncs.com/images/springboot-transaction-1004.png?x-oss-process=style/shuiyin)

​    在应用系统调用声明 `@Transactional` 的目标方法时，Spring 默认使用 `AOP` 代理，在代码运行时生成一个代理对象，根据 `@transactional` 的属性配置信息，这个代理对象决定该声明 `@transactional` 的目标方法是否由拦截器 `TransactionInterceptor` 来使用拦截，在 `TransactionInterceptor` 拦截时，会在在目标方法开始执行之前创建并加入事务，并执行目标方法的逻辑, 最后根据执行情况是否出现异常, 进行业务事务提交或者回滚操作。

更具体的介绍请参考： [Spring 官方事务相关文档](https://docs.spring.io/spring/docs/current/spring-framework-reference/data-access.html#tx-decl-explained)

## 八、Spring 事务使用示例

在接下来的示例中我们分别使用编程式 `TransactionTemplate`、`PlatformTransactionManager` 和声明式 `@Transactional` 注解几种方式进行事务演示。在演示中使用 `Mybatis Plus` 持久层框架操作 `Mysql` 数据库进行事务过程，数据库内容如下：

- **数据库地址：** 127.0.0.1:3306
- **数据库名称：** test
- **测试的表名：** user
- **表中存在的数据：**

| 字段名称 | 类型    | 注释           |
| :------- | :------ | :------------- |
| id       | int     | 主键，自动递增 |
| name     | varchar | 姓名           |
| age      | int     | 岁数           |

### 1、Maven 引入相关依赖

这里是使用 `Maven` 管来管演示项目中的依赖，添加 `SpringBoot`、`mysql`、`mybatis-plus` 等依赖包，`pom.xml` 文件内容如下：

**pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.3.3.RELEASE</version>
    </parent>

    <artifactId>spring-boot-transaction-example</artifactId>
    <packaging>jar</packaging>
    <name>spring-boot-transaction-example</name>
    <description>spring boot transaction example</description>

    <dependencies>
        <!-- web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <!--mysql-->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
        </dependency>
        <!--mybatis plus-->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>3.3.2</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

### 2、配置文件中数据库相关参数

文件 `application.yml` 是 `SpringBoot` 项目的配置文件，可以配置一些配置参数，如数据库、数据库连接池等参数匹配都是需要写在这里，当前示例项目配置文件内容如下：

**applciation.yml**

```yaml
spring:
  datasource:
    type: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/test?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&useSSL=false&allowMultiQueries=true&allowPublicKeyRetrieval=true
    hikari:
      pool-name: DatebookHikariCP
      minimum-idle: 5
      maximum-pool-size: 15
      max-lifetime: 1800000
      connection-timeout: 180000
      username: root
      password: 123456
```

说明：

- url：数据库地址
- spring.datrasource.type：配置数据库连接池类型，这里是使用 Hikari 数据库连接池。
- spring.datrasource.driverClassName：配置连接的数据库驱动，这里使用的是 Mysql JDBC 数据库驱动。

### 3、创建实体类

创建用于测试的实体类对象 `User` 类，其属性和数据库中的字段对应，再加上一些 `Mybatis Plus` 的一些注解完成其需要的配置，实体类内容如下：

**User.java**

```java
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 用户实体类
 */
@TableName(value = "user")
public class User {
    /** 主键 **/
    @TableId(value = "id",type = IdType.AUTO)
    private Integer id;
    /** 姓名 **/
    private String name;
    /** 岁数 **/
    private Integer age;

    /** 构造方法 **/
    public User() {
    }
    public User(String name, Integer age) {
        this.name = name;
        this.age = age;
    }

    /** Set、Get方法 **/
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Integer getAge() {
        return age;
    }
    public void setAge(Integer age) {
        this.age = age;
    }
}
```

### 4、创建持久层 UserMapper 类

创建 `UserMapper` 接口类，该类继承了 `Mybatis Plus` 中的 `BaseMapper` 类，该类已经默认实现了常用的数据库操作方法，例如插入数据、删除数据、更新数据、查询数据等，所以继承了该类我们就能方便的操作数据库中的表，方便下面进行事务示例演示。

**UserMapper.java**

```java
import club.mydlq.model.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper 类
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
```

### 5、创建用户 Service 类

这里使用 `TransactionTemplate`、`PlatformTransactionManager` 和 `@Transactional` 三种方式演示事务示例，设置插入用户，在执行后抛出异常信息，观测能否正常回滚来判断是否成功执行事务。

**(1)、编程式事务 PlatformTransactionManager 方式**

```java
import club.mydlq.mapper.UserMapper;
import club.mydlq.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * 编程式事务 PlatformTransactionManager
 */
@Service
public class PlatformTransactionManagerService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    public void test() {
        // 获取事务定义对象，方便配置事务隔属性
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        // 设置事务隔离级别
        definition.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        // 设置事务传播行为
        definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        // 设置事务超时时间
        definition.setTimeout(30000);
        // 获取事务状态对象
        TransactionStatus status = platformTransactionManager.getTransaction(definition);
        try {
            // 设置待插入用户信息
            User user = new User("小豆丁", 20);
            // 数据库中插入数据
            userMapper.insert(user);
            // 创建异常，方便测试回滚
            int exception = 1 / 0;
            // 事务提交
            platformTransactionManager.commit(status);
        } catch (Exception e) {
            // 发生异常，进行回滚
            platformTransactionManager.rollback(status);
        }
    }

}
```

**(2)、编程式事务 TransactionTemplate 方式**

```java
import club.mydlq.mapper.UserMapper;
import club.mydlq.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 编程式事务 TransactionTemplate
 */
@Service
public class TransactionTemplateService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private TransactionTemplate transactionTemplate;

    public void test() {
        // 设置事务传播属性
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        // 设置事务的隔离级别
        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        // 设置事务超时时间
        transactionTemplate.setTimeout(30000);
        // 执行业务逻辑
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                try {
                    // 设置待插入用户信息
                    User user = new User("小豆丁", 20);
                    // 数据库中插入数据
                    userMapper.insert(user);
                    // 创建异常，方便测试回滚
                    int exception = 1 / 0;
                } catch (Exception e) {
                    // 发生异常，进行回滚
                    transactionStatus.setRollbackOnly();
                }
            }
        });
    }

}
```

**(3)、声明式事务 Transactional 注解方式**

```java
import club.mydlq.mapper.UserMapper;
import club.mydlq.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 声明式事务 @Transactional
 */
@Service
public class TransactionalService {

    @Autowired
    private UserMapper userMapper;

    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Exception.class, timeout = 30000)
    public void test() {
        // 设置待插入用户信息
        User user = new User("小豆丁", 20);
        // 数据库中插入数据
        userMapper.insert(user);
        // 创建异常，方便测试回滚
        int exception = 1 / 0;
    }

}
```

### 6、创建接口 UserController 类

创建测试的 `Controller` 类，提供测试接口，方便对事务方法进行测试。

```java
import com.aspirecn.service.TransactionTemplateService;
import com.aspirecn.service.PlatformTransactionManagerService;
import com.aspirecn.service.TransactionalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 事务 Controller
 */
@RestController
public class UserController {

    @Autowired
    private PlatformTransactionManagerService platformTransactionManagerService;
    @Autowired
    private TransactionTemplateService transactionTemplateService;
    @Autowired
    private TransactionalService transactionalService;

    @GetMapping("/test1")
    public void addUser1(){
        platformTransactionManagerService.test();
    }

    @GetMapping("/test2")
    public void addUser2(){
        transactionTemplateService.test();
    }

    @GetMapping("/test3")
    public void addUser3(){
        transactionalService.test();
    }

}
```

### 7、创建 SpringBoot 启动类

创建 `SpringBoot` 启动类，该类主要用于启动 `SpringBoot` 项目。需要注意的是 `Spring` 项目是需要添加 `@EnableTransactionManagement` 注解开启事务，不过 `SpringBoot` 已经自动配置了该注解，不需要手动添加该注解开启事务。

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SpringBoot 启动类
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
```

## 九、常见 Spring 事务中注意事项

### 1、不要在事务方法中手动捕获异常

这是非常容易出错的一点，在下面代码中通过 `catch` 手动捕获了异常，导致异常并不会上抛，所以 `Spring` 无法感知到发生异常，自然无法进行回滚等操作。所以使用 `@Transactional` 使用事务时，千万别再事务方法中进行手动捕获异常，而是将异常上抛，让 `Spring` 能够正常捕获。

```java
@Transactional
public void test() throws Exception {
    try {
        userMapper.delete(1);
        throw new SQLException();
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

### 2、遇到非运行时异常时事务默认不回滚

在下面代码中，模拟发生 `SQLException` 异常，但是在执行时会发现出现指定的异常时候并没有执行回滚操作，这是因为 `SQLException` 是继承的 `Exception`，而 Spring 的默认事务回滚规则是遇到运行时异常（RuntimeException）或者 Error 时才会进行回滚操作。

```java
@Transactional
public void test() throws Exception {
    userMapper.delete(1);
    throw new SQLException();
}
```

解决上面中的问题其实很简单，可以在 `@Transactional` 注解中设置 `rollbackFor` `=` `Exception.class` 属性即可，如下：

```java
@Transactional(rollbackFor = Exception.class)
public void test() throws Exception {
    userMapper.delete(1);
    throw new SQLException();
}
```

### 3、使用 public 来修饰事务方法

如果 `@Transactional` 修饰的是 `private` 方法，那么该事务是不生效的，如下：

```java
@Transactional
private void test() throws Exception {
    userMapper.delete(1);
    throw new SQLException();
}
```

因为 `@Transactional` 注解是通过 `Spring AOP` 代理实现的，而 `AOP` 是不能直接获取非 `pulic` 方法的，如果要用在非 `public` 方法上，可以开启 `AspectJ` 代理模式。

### 4、不要调用类自身事务方法

在使用 `@Transactional` 时，最好不要发生 `自身非事务方法调用事务方法` 和 `事务方法调用自身类中的另一个事务方法` 这两种情况，如下：

- **同一类中非事务方法调用事务调用，事务不生效**

```java
@Service
public class ServiceA {

    public void mA1() {
        // 调用同类中的事务方法 mA2()
        mA2();  
    }

    @Transactional
    public void mA2() {
        // 业务逻辑（略）
    }

}
```

- **同一类中事务方法调用另一个事务方法，事务不生效**

```java
@Service
public class ServiceB{

    @Transactional
    public void mB1() {
        // 调用同类中的事务方法 mB2()
        mB2();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void mB2() {
        // 业务逻辑（略）
    }

}
```

上面方法都不生效，这是因为它们发生了自身调用，就调该类自己的方法，而没有经过 `Spring AOP` 的代理类，默认只有在外部调用事务才会生效。一般来说我们比较推荐写两个类进行事务方法的调用，如下：

- **类A**

```java
@Service
public class ServiceA {
    
    @Autowired
    private ServiceB serviceB;

    @Transactional
    public void mA() {
        // 调用 ServiceB 中的事务方法
        serviceB.mB();
    }

}
```

- **类B**

```java
@Service
public class ServiceB {

    @Transactional
    public void mB() {
        // 执行业务逻辑
    }

}
```

—END—