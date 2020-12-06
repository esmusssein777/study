# Kafka进阶

[toc]

## Broker的设计

Broker 是Kafka 集群中的节点。负责处理生产者发送过来的消息，消费者消费的请求。以及集群节点的管理等。

<img src="https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/wE4bYT.png" alt="zooker" style="zoom:50%;" />

ZooKeeper 主要为 Kafka 提供元数据的管理的功能。

从图中我们可以看出，Zookeeper 主要为 Kafka 做了下面这些事情：

1. **Broker 注册** ：在 Zookeeper 上会有一个专门**用来进行 Broker 服务器列表记录**的节点。每个 Broker 在启动时，都会到 Zookeeper 上进行注册，即到/brokers/ids 下创建属于自己的节点。每个 Broker 就会将自己的 IP 地址和端口等信息记录到该节点中去
2. **Topic 注册** ： 在 Kafka 中，同一个**Topic 的消息会被分成多个分区**并将其分布在多个 Broker 上，**这些分区信息及与 Broker 的对应关系**也都是由 Zookeeper 在维护。比如我创建了一个名字为 mytopic 的主题并且它有两个分区，对应到 zookeeper 中会创建这些文件夹：`/brokers/topics/mytopic/Partitions/0`、`/brokers/topics/mytopic/Partitions/1`
3. **负载均衡** ：上面也说过了 Kafka 通过给特定 Topic 指定多个 Partition, 而各个 Partition 可以分布在不同的 Broker 上, 这样便能提供比较好的并发能力。 对于同一个 Topic 的不同 Partition，Kafka 会尽力将这些 Partition 分布到不同的 Broker 服务器上。当生产者产生消息后也会尽量投递到不同 Broker 的 Partition 里面。当 Consumer 消费的时候，Zookeeper 可以根据当前的 Partition 数量以及 Consumer 数量来实现动态负载均衡。
4. **故障转移**：在r `/brokers/topics/[topic]/partitions/[partition]/state` 保存了topic-partition的leader和Isr等信息。**Controller负责broker故障检查&&故障转移（fail/recover）**。

### broker负载均衡

* 分区数量负载：各台broker的partition数量应该均匀

partition Replica分配算法如下：

1. 将所有Broker（假设共n个Broker）和待分配的Partition排序
2. 将第i个Partition分配到第（i mod n）个Broker上
3. 将第i个Partition的第j个Replica分配到第（(i + j) mod n）个Broker上



* 容量大小负载：每台broker的硬盘占用大小应该均匀

在kafka1.1之前，Kafka能够保证各台broker上partition数量均匀，但由于每个partition内的消息数不同，可能存在不同硬盘之间内存占用差异大的情况。在Kafka1.1中增加了副本跨路径迁移功能kafka-reassign-partitions.sh，我们可以结合它和监控系统，实现自动化的负载均衡

### borker故障转移

#### broker宕机

1. Controller在Zookeeper上注册Watch，一旦有Broker宕机，其在Zookeeper对应的znode会自动被删除，Zookeeper会触发 Controller注册的watch，Controller读取最新的Broker信息
2. Controller确定set_p，该集合包含了宕机的所有Broker上的所有Partition
3. 对set_p中的每一个Partition，选举出新的leader、Isr，并更新结果，从`/brokers/topics/[topic]/partitions/[partition]/state`读取该Partition当前的ISR
4. 决定该Partition的新Leader和Isr。如果当前ISR中有至少一个Replica还幸存，则选择其中一个作为新Leader，新的ISR则包含当前ISR中所有幸存的Replica。否则选择该Partition中任意一个幸存的Replica作为新的Leader以及ISR（该场景下可能会有潜在的数据丢失)
5. 更新Leader、ISR、leader_epoch、controller_epoch：写入`/brokers/topics/[topic]/partitions/[partition]/state`。直接通过RPC向set_p相关的Broker发送LeaderAndISRRequest命令。Controller可以在一个RPC操作中发送多个命令从而提高效率。

#### Controller宕机

每个 broker 都会在 zookeeper 的临时节点 "/controller" 注册 watcher，当 controller 宕机时 "/controller" 会消失，触发broker的watch，每个 broker 都尝试创建新的 controller path，只有一个竞选成功并当选为 controller。

## kafka的高吞吐

消息中间件从功能上看就是写入数据、读取数据两大类，优化也可以从这两方面来看。

### 写入

为了优化写入速度 Kafak 采用以下技术：

#### 1. 顺序写入

磁盘大多数都还是机械结构（SSD不在讨论的范围内），如果将消息以随机写的方式存入磁盘，就需要按柱面、磁头、扇区的方式寻址，缓慢的机械运动（相对内存）会消耗大量时间，导致磁盘的写入速度与内存写入速度差好几个数量级。为了规避随机写带来的时间消耗，Kafka 采取了顺序写的方式存储数据，如下图所示：

![](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/UurWaQ.jpg)

每条消息都被append 到该 partition 中，属于顺序写磁盘，因此效率非常高。 但这种方法有一个缺陷：没有办法删除数据。所以Kafka是不会删除数据的，它会把所有的数据都保留下来，每个消费者（Consumer）对每个 Topic 都有一个 offset 用来表示读取到了第几条数据。

![](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/1SrEJm.jpg)

上图中有两个消费者，Consumer1 有两个 offset 分别对应 Partition0、Partition1（假设每一个 Topic 一个 Partition ）。Consumer2 有一个 offset 对应Partition2 。这个 offset 是由客户端 SDK 保存的，Kafka 的 Broker 完全无视这个东西的存在，一般情况下 SDK 会把它保存到 zookeeper 里面。 如果不删除消息，硬盘肯定会被撑满，所以 Kakfa 提供了两种策略来删除数据。一是基于时间，二是基于 partition 文件大小，具体配置可以参看它的配置文档。 即使是顺序写，过于频繁的大量小 I/O 操作一样会造成磁盘的瓶颈，所以 Kakfa 在此处的处理是把这些消息集合在一起批量发送，这样减少对磁盘 I/O 的过度操作，而不是一次发送单个消息。

#### 2. 内存映射文件

即便是顺序写入硬盘，硬盘的访问速度还是不可能追上内存。所以 Kafka 的数据并不是实时的写入硬盘，它充分利用了现代操作系统分页存储来利用内存提高I/O效率。Memory Mapped Files （后面简称mmap）也被翻译成内存映射文件，在64位操作系统中一般可以表示 20G 的数据文件，它的工作原理是直接利用操作系统的 Page 来实现文件到物理内存的直接映射。完成映射之后对物理内存的操作会被同步到硬盘上（由操作系统在适当的时候）。 通过 mmap 进程像读写硬盘一样读写内存，也不必关心内存的大小，有虚拟内存为我们兜底。使用这种方式可以获取很大的 I/O 提升，因为它省去了用户空间到内核空间复制的开销（调用文件的 read 函数会把数据先放到内核空间的内存中，然后再复制到用户空间的内存中） 但这样也有一个很明显的缺陷——不可靠，写到 mmap 中的数据并没有被真正的写到硬盘，操作系统会在程序主动调用 flush 的时候才把数据真正的写到硬盘。所以 Kafka 提供了一个参数—— producer.type 来控制是不是主动 flush，如果Kafka 写入到 mmap 之后就立即 flush 然后再返回 Producer 叫同步(sync)；如果写入 mmap 之后立即返回，Producer 不调用 flush ，就叫异步(async)。

#### 3. 标准化二进制消息格式

为了避免无效率的字节复制，尤其是在负载比较高的情况下影响是显著的。为了避免这种情况，Kafka 采用由 Producer，Broker 和 Consumer 共享的标准化二进制消息格式，这样数据块就可以在它们之间自由传输，无需转换，降低了字节复制的成本开销。

而在读取速度的优化上 Kafak 采取的主要是零拷贝

### 读取

#### 零拷贝（Zero Copy）的技术：

传统模式下我们从硬盘读取一个文件是这样的

![](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/acY0zH.jpg)

(1) 操作系统将数据从磁盘读到内核空间的页缓存区

(2) 应用将数据从内核空间读到用户空间的缓存中

(3) 应用将数据写会内核空间的套接字缓存中

(4)操作系统将数据从套接字缓存写到网卡缓存中，以便将数据经网络发出

这样做明显是低效的，这里有四次拷贝，两次系统调用。 针对这种情况 Unix 操作系统提供了一个优化的路径，用于将数据从页缓存区传输到 socket。在 Linux 中，是通过 sendfile 系统调用来完成的。Java提供了访问这个系统调用的方法：FileChannel.transferTo API。这种方式只需要一次拷贝：操作系统将数据直接从页缓存发送到网络上，在这个优化的路径中，只有最后一步将数据拷贝到网卡缓存中是需要的。

![](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/M331Ew.jpg)

零拷贝是指内核空间和用户空间的交互的拷贝次数为零。这个技术其实非常普遍，Nginx 也是用的这种技术。

## MQ常见问题Kafka解决思路

### 幂等性

既然是消费消息，那肯定要考虑会不会重复消费？能不能避免重复消费？或者重复消费了也别造成系统异常可以吗？

**什么时候Kafka会发生重复消费？**

Kafka 实际上有个 offset 的概念，每个消息写进去，都有一个 offset，代表消息的序号，然后 consumer 消费了数据之后，**每隔一段时间**（定时定期），会把自己消费过的消息的 offset 提交一下，表示“我已经消费过了，下次继续从上次消费到的 offset 继续消费。

但是凡事总有意外，就是你有时候重启系统，碰到着急的，直接 kill 进程了，再重启。这会导致 consumer 有些消息处理了，但是没来得及提交 offset。重启之后，少数消息会再次消费一次。

![Kafka](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/7vBGuj.jpg)

上述图片即是一个重复消费的例子，消费者在消费玩offset=153后被重启，没有来得及将offset提交给Kafka。所以重启后再一次消费了offset=152和offset=153。



其实重复消费有时候无法避免，那么我们就需要考虑到在重复消费发生时，保持系统的幂等性。

具体需要结合业务，比如：

* 如果是数据库写入，首先根据主键查询，如果已经有了这一条数据，那么就执行update
* 如果是redis，那么就不需要考虑，因为set的操作天然具有幂等性
* 如果更复杂的话，我们在生产者发送数据时，加一个全局唯一的ID，当消费ID的时候，去redis里面查询是否消费过，如果消费过就忽略，保证不处理同样的消息

![](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/nwZKx4.jpg)

### 可靠性传输

如何保证消息的可靠性传输？或者说，如何处理消息丢失的问题？

#### 消费端丢失数据

唯一可能导致消费者弄丢数据的情况，就是当消费到了这个消息，然后消费者那边**自动提交了 offset**，让 Kafka 以为消费者已经消费好了这个消息，但其实消费者才刚准备处理这个消息，还没处理完就挂了，此时这条消息就丢啦。

从上面知道 Kafka 会自动提交 offset，那么只要**关闭自动提交** offset，在处理完之后自己手动提交 offset，就可以保证数据不会丢。但是此时确实还是**可能会有重复消费**，比如消费者刚处理完，还没提交 offset，结果消费者挂了，此时肯定会重复消费一次，需要自己保证幂等性。

#### Kafka丢失数据

这块比较常见的一个场景，就是 Kafka 某个 broker 宕机，然后重新选举 partition 的 leader。此时其他的 follower 刚好还有些数据没有同步，结果此时 leader 挂了，然后选举某个 follower 成 leader 之后，于是就少了一些数据。

生产环境中，Kafka 的 leader 机器宕机了，将 follower 切换为 leader 之后，就会发现说这个数据就丢了。

所以此时一般是要求起码设置如下 4 个参数：

- 给 topic 设置 `replication.factor` 参数：这个值必须大于 1，要求每个 partition 必须有至少 2 个副本。
- 在 Kafka 服务端设置 `min.insync.replicas` 参数：这个值必须大于 1，这个是要求一个 leader 至少感知到有至少一个 follower 还跟自己保持联系，没掉队，这样才能确保 leader 挂了还有一个 follower 吧。
- 在 producer 端设置 `acks=all`：这个是要求每条数据，必须是**写入所有 replica 之后，才能认为是写成功了**。
- 在 producer 端设置 `retries=MAX`（很大很大很大的一个值，无限次重试的意思）：这个是**要求一旦写入失败，就无限重试**，卡在这里了。

我们生产环境就是按照上述要求配置的，这样配置之后，至少在 Kafka broker 端就可以保证在 leader 所在 broker 发生故障，进行 leader 切换时，数据不会丢失。

#### 生产者会不会弄丢数据？

如果按照上述的思路设置了 `acks=all`，一定不会丢，要求是，Kafka的 leader 接收到消息，所有的 follower 都同步到了消息之后，才认为本次写成功了。如果没满足这个条件，生产者会自动不断的重试，重试无限次。

### 消息的顺序性

如何保证消息的顺序性？

举一个例子，在mysql `binlog` 同步的系统中：在 mysql 里增删改一条数据，对应出来了增删改 3 条 `binlog` 日志，接着这三条 `binlog` 发送到 MQ 里面，再消费出来依次执行，需要保证是按照顺序来的。不然本来是：增加、修改、删除；如果换了顺序给执行成删除、修改、增加，那么就全错了。本来这个数据同步过来，应该最后这个数据被删除了；结果搞错了这个顺序，最后这个数据保留下来了，数据同步就出错了。

#### Kafka生产环境中出错的场景

比如说我们建了一个 topic，有三个 partition。生产者在写的时候，其实可以指定一个 key，比如说我们指定了某个订单 id 作为 key，那么这个订单相关的数据，一定会被分发到同一个 partition 中去，而且这个 partition 中的数据一定是有顺序的。
消费者从 partition 中取出来数据的时候，也一定是有顺序的。但是接着，我们在消费者里可能会需要**多个线程来并发处理消息**。因为如果消费者是单线程消费处理，而处理比较耗时的话，比如处理一条消息耗时几十 ms，那么 1 秒钟只能处理几十条消息，这吞吐量太低了。而多个线程并发跑的话，顺序可能就乱掉了。

![](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/XdEjT3.jpg)

#### 解决方法

- 一个 topic，一个 partition，一个 consumer，内部单线程消费，单线程吞吐量太低。
- 写 N 个内存 queue，具有相同 key 的数据都到同一个内存 queue；然后对于 N 个线程，每个线程分别消费一个内存 queue 即可，这样就能保证顺序性。

![](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/wAvtnx.jpg)