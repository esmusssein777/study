## mybatis

### 实现

通常一个XML映射文件，都会写一个Mapper接口与之对应，原理就是：

1. 在xml文件中，每一个标签都会被解析成MappedStatement
2. Mapper 接口的实现类，通过 Mybatis 使用 JDK Proxy 自动生成其代理对象，

而代理对象 Proxy 会拦截接口方法，从而调用对应的 MapperdStatement，最终执行sql。

![](http://static2.iocoder.cn/images/MyBatis/2020_03_15/02.png)

#### mybatis 的执行器

Mybatis 有四种 Executor 执行器， 分别是 SimpleExecutor、reuseExecutor、BatchExecutor、CachingExecutor。

- SimpleExecutor ：每执行一次 update 或 select 操作，就创建一个 Statement 对象，用完立刻关闭 Statement 对象。
- ReuseExecutor ：执行 update 或 select 操作，以 SQL 作为key 查找**缓存**的 Statement 对象，存在就使用，不存在就创建；用完后，不关闭 Statement 对象，而是放置于缓存 `Map<String, Statement>` 内，供下一次使用。简言之，就是重复使用 Statement 对象。
- BatchExecutor ：执行 update 操作（没有 select 操作，因为 JDBC 批处理不支持 select 操作），将所有 SQL 都添加到批处理中（通过 addBatch 方法），等待统一执行（使用 executeBatch 方法）。它缓存了多个 Statement 对象，每个 Statement 对象都是调用 addBatch 方法完毕后，等待一次执行 executeBatch 批处理。**实际上，整个过程与 JDBC 批处理是相同**。
- CachingExecutor ：在上述的三个执行器之上，增加**二级缓存**的功能