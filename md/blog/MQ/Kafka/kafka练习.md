# Kafka 入门

[TOC]

## 消息队列

### 什么是消息队列？

​		我们可以把消息队列比作是一个存放消息的容器，当我们需要使用消息的时候可以取出消息供自己使用。消息队列是分布式系统中重要的组件，使用消息队列主要是为了通过异步处理提高系统性能和削峰、降低系统耦合性。目前使用较多的消息队列有Kafka，ActiveMQ，RabbitMQ，RocketMQ。



### 为什么要使用消息队列？

我觉得使用消息队列主要有两点好处：

1.  通过异步处理提高系统性能（削峰、减少响应所需时间）

2. 降低系统耦合性

#### (1) 通过异步处理提高系统性能

​		使用消息队列服务器的时候，用户的请求数据直接写入数据库，在高并发的情况下数据库压力剧增，使得响应速度变慢。但是在使用消息队列之后，用户的请求数据发送给消息队列之后立即 返回，再由消息队列的消费者进程从消息队列中获取数据，异步写入数据库。由于消息队列服务器处理速度快于数据库（消息队列也比数据库有更好的伸缩性），因此响应速度得到大幅改善。

​		通过以上分析我们可以得出**消息队列具有很好的削峰作用的功能**——即**通过异步处理，将短时间高并发产生的事务消息存储在消息队列中，从而削平高峰期的并发事务。** 举例：在电子商务一些秒杀、促销活动中，合理使用消息队列可以有效抵御促销活动刚开始大量订单涌入对系统的冲击。如下图所示：

![](https://gitee.com/Esmusssein/picture/raw/master/uPic/T9CcMF.jpg)



​		因为**用户请求数据写入消息队列之后就立即返回给用户了，但是请求数据在后续的业务校验、写数据库等操作中可能失败**。因此使用消息队列进行异步处理之后，需要**适当修改业务流程进行配合**，比如**用户在提交订单之后，订单数据写入消息队列，不能立即返回用户订单提交成功，需要在消息队列的订单消费者进程真正处理完该订单之后，甚至出库后，再通过电子邮件或短信通知用户订单成功**，以免交易纠纷。这就类似我们平时手机订火车票和电影票。

#### (2) 降低系统耦合性

​		使用消息队列还可以降低系统耦合性。我们知道如果模块之间不存在直接调用，那么新增模块或者修改模块就对其他模块影响较小，这样系统的可扩展性无疑更好一些。

​		生产者（客户端）发送消息到消息队列中去，接受者（服务端）处理消息，需要消费的系统直接去消息队列取消息进行消费即可而不需要和其他系统有耦合， 这显然也提高了系统的扩展性。

　　**消息队列使利用发布-订阅模式工作，消息发送者（生产者）发布消息，一个或多个消息接受者（消费者）订阅消息。** 从上图可以看到**消息发送者（生产者）和消息接受者（消费者）之间没有直接耦合**，消息发送者将消息发送至分布式消息队列即结束对消息的处理，消息接受者从分布式消息队列获取该消息后进行后续处理，并不需要知道该消息从何而来。**对新增业务，只要对该类消息感兴趣，即可订阅该消息，对原有系统和业务没有任何影响，从而实现网站业务的可扩展性设计**。

### 不同消息队列的对比

最关心的吞吐量数据的对比，根据阿里中间件团队的数据，在相同的测试机器上同步发送小消息(124字节)。

Kafka的吞吐量高达17.3w/s。这主要取决于它的队列模式保证了写磁盘的过程是线性IO。此时broker磁盘IO已达瓶颈。

RocketMQ吞吐量在11.6w/s，磁盘IO 已接近100%。RocketMQ的消息写入内存后即返回ack，由单独的线程专门做刷盘的操作，所有的消息均是顺序写文件。

RabbitMQ的吞吐量5.95w/s，CPU资源消耗较高。它支持AMQP协议，实现非常重量级，为了保证消息的可靠性在吞吐量上做了取舍。

结合其它方面数据，我们得出一下表格：

| 对比     | ActiveMQ | RabbitMQ         | RocketMQ       | kafka          |
| -------- | -------- | ---------------- | -------------- | -------------- |
| 吞吐量   | 万       | 万               | 十万           | 十万           |
| 延时     | ms       | μs               | ms             | ms             |
| 集群支持 | 主从     | 主从             | 分布式         | 分布式         |
| 功能     | 极其完备 | 性能好，支持事务 | 较完善，分布式 | 简单、实时计算 |



## Kafka 简介

**Kafka 是一种分布式的，基于发布 / 订阅的消息系统。**

对于 Kafka 来说客户端有两种基本类型：

1. **生产者（Producer）**
2. **消费者（Consumer）**。

除此之外，还有用来做数据集成的 Kafka Connect API 和流式处理的 Kafka Streams 等高阶客户端，但这些高阶客户端底层仍然是生产者和消费者API，它们只不过是在上层做了封装。

这很容易理解，生产者（也称为发布者）创建消息，而消费者（也称为订阅者）负责消费或者读取消息。

Kafka 一个最基本的架构认识：由多个 broker 组成，每个 broker 是一个节点；你创建一个 topic，这个 topic 可以划分为多个 partition，每个 partition 可以存在于不同的 broker 上，每个 partition 就放一部分数据

### Topic 和 Partition

![主题与分区](https://gitee.com/Esmusssein/picture/raw/master/uPic/主题与分区.png)

**Topic**

每条发布到 Kafka 的消息都有一个类别，这个类别被称为 Topic 。（物理上不同 Topic 的消息分开存储。逻辑上一个 Topic 的消息虽然保存于一个或多个broker上，但用户只需指定消息的 Topic 即可生产或消费数据而不必关心数据存于何处）

**Partition**

Topic 物理上的分组，一个 Topic 可以分为多个 Partition ，每个 Partition 是一个有序的队列。Partition 中的每条消息都会被分配一个有序的 id（offset）

### Broker 和 Cluster

![Broker和集群](https://gitee.com/Esmusssein/picture/raw/master/uPic/Broker和集群.png)

**Broker**

一个 Kafka 服务器也称为 Broker，它接受生产者发送的消息并存入磁盘；Broker 同时服务消费者拉取分区消息的请求，返回目前已经提交的消息。

**Cluster**

若干个 Broker 组成一个集群（Cluster），其中集群内某个 Broker 会成为集群控制器（Cluster Controller），它负责管理集群，包括分配分区到 Broker、监控 Broker 故障等。在集群内，一个分区由一个 Broker 负责，这个 Broker 也称为这个分区的 Leader；当然一个分区可以被复制到多个 Broker 上来实现冗余，这样当存在 Broker 故障时可以将其分区重新分配到其他 Broker 来负责。

### Producer和Consumer设计

#### Producer发送消息设计

**1. 连接**

每初始化一个producer实例，都会初始化一个Sender实例，新增到broker的长连接。

**2. 计算partition**

根据key和value的配置对消息进行序列化,然后计算partition： ProducerRecord对象中如果指定了partition，就使用这个partition。否则根据key和topic的partition数目取余，如果key也没有的话就随机生成一个counter，使用这个counter来和partition数目取余。这个counter每次使用的时候递增。

**3. 发送到batchs**

根据topic-partition获取对应的batchs（Deque），然后将消息append到batch中。如果有batch满了则唤醒Sender 线程。队列的操作是加锁执行，所以batch内消息时有序的。

**4. Sender把消息有序发到 broker**

Kafka中每台broker都保存了kafka集群的metadata信息，metadata信息里包括了每个topic的所有partition的信息：leader、 leader_epoch、 controller_epoch、 isr、replicas等；Kafka客户端从任一broker都可以获取到需要的metadata信息；同时根据metadata更新策略（定期更新metadata.max.age.ms、失效检测，强制更新：检查到metadata失效以后，调用metadata.requestUpdate()强制更新。

为实现Producer的幂等性，Kafka引入了Producer ID（即PID）和Sequence Number。对于每个PID，该Producer发送消息的每个<Topic, Partition>都对应一个单调递增的Sequence Number。同样，Broker端也会为每个<PID, Topic, Partition>维护一个序号，并且每Commit一条消息时将其对应序号递增。对于接收的每条消息，如果其序号比Broker维护的序号）大一，则Broker会接受它，否则将其丢弃。

**5. Sender处理broker发来的produce response**

一旦broker处理完Sender的produce请求，就会发送produce response给Sender，此时producer将执行我们为send（）设置的回调函数。至此producer的send执行完毕。



#### Consumer消费消息设计

**1. poll消息**

- 消费者通过fetch线程拉消息（单线程）。
- 消费者通过心跳线程来与broker发送心跳。超时会认为挂掉。
- 每个consumer group在broker上都有一个coordnator来管理，消费者加入和退出，以及消费消息的位移都由coordnator处理。

**2. 位移管理**

consumer的消息位移代表了当前group对topic-partition的消费进度，consumer宕机重启后可以继续从该offset开始消费。 在kafka0.8之前，位移信息存放在zookeeper上，由于zookeeper不适合高并发的读写，新版本Kafka把位移信息当成消息，发往\__consumers_offsets 这个topic所在的broker，__consumers_offsets默认有50个分区。 消息的key 是groupId+topic_partition,value 是offset。

**3. 重平衡**

当一些原因导致consumer对partition消费不再均匀时，kafka会自动执行reblance，使得consumer对partition的消费再次平衡。 什么时候发生rebalance？

- 组订阅topic数变更
- topic partition数变更
- consumer成员变更
- consumer 加入群组或者离开群组的时候
- consumer被检测为崩溃的时候

当新的消费者加入消费组，它会消费一个或多个分区，而这些分区之前是由其他消费者负责的；另外，当消费者离开消费组（比如重启、宕机等）时，它所消费的分区会分配给其他分区。这种现象称为**重平衡（rebalance）**。重平衡是 Kafka 一个很重要的性质，这个性质保证了高可用和水平扩展。**不过也需要注意到，在重平衡期间，所有消费者都不能消费消息，因此会造成整个消费组短暂的不可用。**而且，将分区进行重平衡也会导致原来的消费者状态过期，从而导致消费者需要重新更新状态，这段期间也会降低消费性能。后面我们会讨论如何安全的进行重平衡以及如何尽可能避免。

消费者通过定期发送心跳（hearbeat）到一个作为组协调者（group coordinator）的 broker 来保持在消费组内存活。这个 broker 不是固定的，每个消费组都可能不同。当消费者拉取消息或者提交时，便会发送心跳。

如果消费者超过一定时间没有发送心跳，那么它的会话（session）就会过期，组协调者会认为该消费者已经宕机，然后触发重平衡。可以看到，从消费者宕机到会话过期是有一定时间的，这段时间内该消费者的分区都不能进行消息消费；通常情况下，我们可以进行优雅关闭，这样消费者会发送离开的消息到组协调者，这样组协调者可以立即进行重平衡而不需要等待会话过期。

在 0.10.1 版本，Kafka 对心跳机制进行了修改，将发送心跳与拉取消息进行分离，这样使得发送心跳的频率不受拉取的频率影响。

![消费者设计概要5](https://gitee.com/Esmusssein/picture/raw/master/uPic/消费者设计概要5.png)

**一些问题**

**问题1:** Kafka 中一个 topic 中的消息是被打散分配在多个 Partition(分区) 中存储的， Consumer Group 在消费时需要从不同的 Partition 获取消息，那最终如何重建出 Topic 中消息的顺序呢？

**答案：**没有办法。Kafka 只会保证在 Partition 内消息是有序的，而不管全局的情况。

**问题2：**Partition 中的消息可以被（不同的 Consumer Group）多次消费，那 Partition中被消费的消息是何时删除的？ Partition 又是如何知道一个 Consumer Group 当前消费的位置呢？

**答案：**无论消息是否被消费，除非消息到期 Partition 从不删除消息。例如设置保留时间为 2 天，则消息发布 2 天内任何 Group 都可以消费，2 天后，消息自动被删除。

### Kafka高可用

Kafka 一个最基本的架构认识：由多个 broker 组成，每个 broker 是一个节点；你创建一个 topic，这个 topic 可以划分为多个 partition，每个 partition 可以存在于不同的 broker 上，每个 partition 就放一部分数据

每个 partition 的数据都会同步到其它机器上，形成自己的多个 replica 副本。所有 replica 会选举一个 leader 出来，那么生产和消费都跟这个 leader 打交道，然后其他 replica 就是 follower。写的时候，leader 会负责把数据同步到所有 follower 上去，读的时候就直接读 leader 上的数据即可。

如果某个 broker 宕机了，没事儿，那个 broker上面的 partition 在其他机器上都有副本的，如果这上面有某个 partition 的 leader，那么此时会从 follower 中重新选举一个新的 leader 出来，大家继续读写那个新的 leader 即可。这就有所谓的高可用性了。

#### Isr

Kafka结合同步复制和异步复制，使用ISR（与Partition Leader保持同步的Replica列表）的方式在确保数据不丢失和吞吐率之间做了平衡。Producer只需把消息发送到Partition Leader，Leader将消息写入本地Log。

Follower则从Leader pull数据。Follower在收到该消息向Leader发送ACK。一旦Leader收到了ISR中所有Replica的ACK，该消息就被认为已经commit了，Leader将增加HW并且向Producer发送ACK。这样如果leader挂了，只要Isr中有一个replica存活，就不会丢数据。

Leader会跟踪ISR，如果ISR中一个Follower宕机，或者落后太多，Leader将把它从ISR中移除。这里所描述的“落后太多”指Follower复制的消息落后于Leader后的条数超过预定值（replica.lag.max.messages）或者Follower超过一定时间（replica.lag.time.max.ms）未向Leader发送fetch请求。



#### 扩展

1. 关于 Broker 的设计原理
2. broker故障检查&&故障转移
3. Kafka高吞吐量是如何实现

**参考**

[阿里中间件团队MQ对比](http://jm.taobao.org/2016/04/01/kafka-vs-rabbitmq-vs-rocketmq-message-send-performance/)

[消息队列之Kafka](https://juejin.im/post/5a67f7e7f265da3e3c6c4f8b#heading-49)

[Kafka系统设计开篇](kafka系统设计开篇)

[消息队列其实很简单](https://github.com/Snailclimb/JavaGuide/blob/master/docs/system-design/data-communication/message-queue.md)