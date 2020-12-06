# MySQL分布式

[toc]

## M-S 主备结构

<img src="https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/pO1inC.png" alt="pO1inC" style="zoom:50%;" />

在状态 1 中，客户端的读写都直接访问节点 A，而节点 B 是 A 的备库，只是将 A 的更新 都同步过来，到本地执行。这样可以保持节点 B 和 A 的数据是相同的。

当需要切换的时候，就切成状态 2。这时候客户端读写访问的都是节点 B，而节点 A 是 B 的备库。

在状态 1 中，虽然节点 B 没有被直接访问，但是我依然建议你把节点 B(也就是备库)设置成只读(readonly)模式。这样做，有以下几个考虑:

1. 有时候一些运营类的查询语句会被放到备库上去查，设置为只读可以防止误操作; 
2. 防止切换逻辑有 bug，比如切换过程中出现双写，造成主备不一致;

3. 可以用 readonly 状态，来判断节点的角色。

readonly 设置对超级 (super) 权限用户是无效的，而用于同步更新的线程，就拥有超级权限。这样就能在 readonly 也同步更新。

接下来，我们再看看节点 A 到 B 这条线的内部流程是什么样的。图中画出的就是一个 update 语句在节点 A 执行，然后同步到节点 B 的完整流程图。

<img src="https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/sdU00H.png" alt="sdU00H" style="zoom:50%;" />

主库接收到客户端的更新请求后，执行内部事务的更新逻辑，同时写 binlog。

备库 B 跟主库 A 之间维持了一个长连接。主库 A 内部有一个线程，专门用于服务备库 B 的这个长连接。一个事务日志同步的完整过程是这样的:

1. 在备库 B 上通过 change master 命令，设置主库 A 的 IP、端口、用户名、密码，以及要从哪个位置开始请求 binlog，这个位置包含文件名和日志偏移量。

2. 在备库 B 上执行 start slave 命令，这时候备库会启动两个线程，就是图中的 io_thread 和 sql_thread。其中 io_thread 负责与主库建立连接。

3. 主库 A 校验完用户名、密码后，开始按照备库 B 传过来的位置，从本地读取 binlog， 发给 B。
4. 备库 B 拿到 binlog 后，写到本地文件，称为中转日志(relay log)。
5. sql_thread 读取中转日志，解析出日志里的命令，并执行。

## 双M 结构

<img src="https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/7P3nhI.png" alt="7P3nhI" style="zoom:50%;" /> 

你可以发现，双 M 结构和 M-S 结构，其实区别只是多了一条线，即: 节点 A 和 B 之间总是互为主备关系。这样在切换的时候就不用再修改主备关系。

双 M 结构还有一个问题需要解决。如果节点 A 同时是节点 B 的备库，相当于又把节点 B 新生成的 binlog 拿过来执行 了一次，然后节点 A 和 B 间，会不断地循环执行这个更新语句，也就是循环复制了。这个 要怎么解决呢?

MySQL 在 binlog 中记录了这个命令第一次执行时所在实例 的 server id。因此，我们可以用下面的逻辑，来解决两个节点间的循环复制的问题:

1. 规定两个库的 server id 必须不同，如果相同，则它们之间不能设定为主备关系;

2. 一个备库接到 binlog 并在重放的过程中，生成与原 binlog 的 server id 相同的新的binlog;

3. 每个库在收到从自己的主库发过来的日志后，先判断 server id，如果跟自己的相同，表示这个日志是自己生成的，就直接丢弃这个日志

按照这个逻辑，如果我们设置了双 M 结构，日志的执行流就会变成这样:

1. 从节点 A 更新的事务，binlog 里面记的都是 A 的 server id;

2. 传到节点 B 执行一次以后，节点 B 生成的 binlog 的 server id 也是 A 的 server id; 
3. 再传回给节点 A，A 判断到这个 server id 与自己的相同，就不会再处理这个日志。所 以，死循环在这里就断掉了。

## 主备延迟

与数据同步有关的时间点主要包括以下三个:

1. 主库 A 执行完成一个事务，写入 binlog，我们把这个时刻记为 T1; 
2. 之后传给备库 B，我们把备库 B 接收完这个 binlog 的时刻记为 T2; 
3. 备库 B 执行完成这个事务，我们把这个时刻记为 T3。所谓主备延迟，就是同一个事务，在备库执行完成的时间和主库执行完成的时间之间的差 值，也就是 T3-T1。

你可以在备库上执行 show slave status 命令，它的返回结果里面会显示 seconds_behind_master，用于表示当前备库延迟了多少秒。

seconds_behind_master 的计算方法是这样的:

1. 每个事务的 binlog 里面都有一个时间字段，用于记录主库上写入的时间;

2. 备库取出当前正在执行的事务的时间字段的值，计算它与当前系统时间的差值，得seconds_behind_master

### 延迟的原因

有些部署条件下，备库所在机器的性能要比主库所在的机器性能差。其次还有备库的压力大的情况，大事务和大表的 DDL 也会延迟很久。

由于主备延迟的存在，所以在主备切换的时候，就相应的有不同的策略。

#### 可靠性优先策略

1. 判断备库 B 现在的 seconds_behind_master，如果小于某个值(比如 5 秒)继续下一 步，否则持续重试这一步;

2. 把主库 A 改成只读状态，即把 readonly 设置为 true;

3. 判断备库 B 的 seconds_behind_master 的值，直到这个值变成 0 为止; 
4. 把备库 B 改成可读写状态，也就是把 readonly 设置为 false;

5. 把业务请求切到备库 B。

可以看到，这个切换流程中是有不可用时间的。因为在步骤 2 之后，主库 A 和备库 B 都 处于 readonly 状态，也就是说这时系统处于不可写状态，直到步骤 5 完成后才能恢复。

在这个不可用状态中，比较耗费时间的是步骤 3，可能需要耗费好几秒的时间。这也是为 什么需要在步骤 1 先做判断，确保 seconds_behind_master 的值足够小。



还有一种是可用性优先，但是这种会导致数据不一致的情况

#### 并行复制

如果备库执行日志的速度持续低于主库生成日志的速度，那这个延迟就有可能成了小时级别。而且对于一个压力持续比较高的主库来说，备库很可能永远都追不上主库的节奏。下面就是几种备库并行复制能力。

#### 自己设计

**按表分发和按行分发策略**

按表分发事务的基本思路是，如果两个事务更新不同的表，它们就可以并行。因为数据是 存储在表里的，所以按表分发，可以保证两个 worker 不会更新同一行。

按行复制和按表复制的数据结构差不多，也是为每个 worker，分配一个 hash 表。只是要 实现按行分发，这时候的 key，就必须是“库名 + 表名 + 唯一键的值”。

#### MySQL 5.6版本

MySQL 5.6 版本的并行复制策略官方 MySQL5.6 版本，支持了并行复制，只是支持的粒度是按库并行。理解了上面介绍的 按表分发策略和按行分发策略，你就理解了，用于决定分发策略的 hash 表里，key 就是数据库名。

这个策略的并行效果，取决于压力模型。如果在主库上有多个 DB，并且各个 DB 的压力 均衡，使用这个策略的效果会很好。

#### MariaDB 的并行复制策略

 redo log 组提交 (group commit) 优化， 而 MariaDB

的并行复制策略利用的就是这个特性:

1. 能够在同一组里提交的事务，一定不会修改同一行;

2. 主库上可以并行执行的事务，备库上也一定是可以并行执行的。

在实现上，MariaDB 是这么做的:

1. 在一组里面一起提交的事务，有一个相同的 commit_id，下一组就是 commit_id+1; 
2. commit_id 直接写到 binlog 里面;

3. 传到备库应用的时候，相同 commit_id 的事务分发到多个 worker 执行;

4. 这一组全部执行完成后，coordinator 再去取下一批。

MariaDB 的这个策略，目标是“模拟主库的并行模式”。

#### MySQL 5.7的并行复制策略

在 MariaDB 并行复制实现之后，官方的 MySQL5.7 版本也提供了类似的功能，由参数

slave-parallel-type 来控制并行复制策略:

1. 配置为 DATABASE，表示使用 MySQL 5.6 版本的按库并行策略;

2. 配置为 LOGICAL_CLOCK，表示的就是类似 MariaDB 的策略。不过，MySQL 5.7 这 个策略，针对并行度做了优化。

#### MySQL 5.7.22 的并行复制策略

在 2018 年 4 月份发布的 MySQL 5.7.22 版本里，MySQL 增加了一个新的并行复制策略，基于 WRITESET 的并行复制。

相应地，新增了一个参数 binlog-transaction-dependency-tracking，用来控制是否启 用这个新策略。这个参数的可选值有以下三种。

1. COMMIT_ORDER，表示的就是前面介绍的，根据同时进入 prepare 和 commit 来判断是否可以并行的策略。

2. WRITESET，表示的是对于事务涉及更新的每一行，计算出这一行的 hash 值，组成集 合 writeset。如果两个事务没有操作相同的行，也就是说它们的 writeset 没有交集，就可以并行。

3. WRITESET_SESSION，是在 WRITESET 的基础上多了一个约束，即在主库上同一个线程先后执行的两个事务，在备库执行的时候，要保证相同的先后顺序。

当然为了唯一标识，这个 hash 值是通过“库名 + 表名 + 索引名 + 值”计算出来的。如 果一个表上除了有主键索引外，还有其他唯一索引，那么对于每个唯一索引，insert 语句 对应的 writeset 就要多增加一个 hash 值。

## 一主多从

<img src="https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/sUvg1M.png" alt="sUvg1M" style="zoom:50%;" />

图中，虚线箭头表示的是主备关系，也就是 A 和 A’互为主备， 从库 B、C、D 指向的是 主库 A。一主多从的设置，一般用于读写分离，主库负责所有的写入和一部分读，其他的 读请求则由从库分担。

我们讨论下在一主多从架构下，主库故障后的主备切换问题。相比于一主一备的切换流程，一主多从结构在切换完成后，A’会成为新的主库，从库 B、C、D 也要改接到 A’。正是由于多了从库 B、C、D 重新指向的这个过程，所以主备 切换的复杂性也相应增加了。

### 主备切换

当我们把节点 B 设置成节点 A’的从库的时候，需要执行一条 change master 命令:

```
CHANGE MASTER TO
MASTER_HOST=$host_name
MASTER_PORT=$port
MASTER_USER=$user_name
MASTER_PASSWORD=$password
MASTER_LOG_FILE=$master_log_name
MASTER_LOG_POS=$master_log_pos
```

这条命令有这么 6 个参数:

MASTER_HOST、MASTER_PORT、MASTER_USER 和 MASTER_PASSWORD 四个 参数，分别代表了主库 A’的 IP、端口、用户名和密码。

最后两个参数 MASTER_LOG_FILE 和 MASTER_LOG_POS 表示，要从主库的 master_log_name 文件的 master_log_pos 这个位置的日志继续同步。而这个位置就 是我们所说的同步位点，也就是主库对应的文件名和日志偏移量。

那么，这里就有一个问题了，节点 B 要设置成 A’的从库，就要执行 change master 命 令，就不可避免地要设置位点的这两个参数，但是这两个参数到底应该怎么设置呢?

原来节点 B 是 A 的从库，本地记录的也是 A 的位点。但是相同的日志，A 的位点和 A’的位点是不同的。因此，从库 B 要切换的时候，就需要先经过“找同步位点”这个逻辑。考虑到切换过程中不能丢数据，所以我们找位点的时候，总是要找一个“稍微往前”的， 然后再通过判断跳过那些在从库 B 上已经执行过的事务。

通常情况下，我们在切换任务的时候，要先主动跳过这些已执行过导致的错误，有两种常用的方 法。

一种做法是，主动跳过一个事务，即发生错误停下来报错后，手动执行一次跳过命令。

```
set global sql_slave_skip_counter=1;
start slave;
```

另外一种方式是，通过设置 slave_skip_errors 参数，直接设置跳过指定的错误。 在执行主备切换时，有这么两类错误，是经常会遇到的:

1062 错误是插入数据时唯一键冲突; 1032 错误是删除数据时找不到行。

因此，我们把slave_skip_errors 设置为 “1032,1062”。

#### GTID

通过 sql_slave_skip_counter 跳过事务和通过 slave_skip_errors 忽略错误的方法，虽然 都最终可以建立从库 B 和新主库 A’的主备关系，但这两种操作都很复杂，而且容易出错。所以，MySQL 5.6 版本引入了 GTID，彻底解决了这个困难。

GTID 的全称是 Global Transaction Identifier，也就是全局事务 ID，是一个事务在提交 的时候生成的，是这个事务的唯一标识。它由两部分组成，格式是:

```
GTID=server_uuid:gno
```

其中 server_uuid 是一个实例第一次启动时自动生成的，是一个全局唯一的值; gno 是一个整数，初始值是 1，每次提交事务的时候分配给这个事务，并加 1。

GTID 模式的启动也很简单，我们只需要在启动一个 MySQL 实例的时候，加上参数 gtid_mode=on 和 enforce_gtid_consistency=on 就可以了。

在 GTID 模式下，每个事务都会跟一个 GTID 一一对应。这个 GTID 有两种生成方式，而 使用哪种方式取决于 session 变量 gtid_next 的值。

1. 如果 gtid_next=automatic，代表使用默认值。这时，MySQL 就会把 server_uuid:gno 分配给这个事务。

   * 记录 binlog 的时候，先记录一行 SET @@SESSION.GTID_NEXT=‘server_uuid:gno’;

   * 把这个 GTID 加入本实例的 GTID 集合。

2. 如果 gtid_next 是一个指定的 GTID 的值，比如通过 set gtid_next='current_gtid’指 定为 current_gtid，那么就有两种可能:

   * 如果 current_gtid 已经存在于实例的 GTID 集合中，接下来执行的这个事务会直接被 系统忽略;

   * 如果 current_gtid 没有存在于实例的 GTID 集合中，就将这个 current_gtid 分配给 接下来要执行的事务，也就是说系统不需要给这个事务生成新的 GTID，因此 gno 也不 用加 1。

实现就是每个 MySQL 实例都维护了一个 GTID 集合，用来对应“这个实例执行过的所有事务”。

在 GTID 模式下，备库 B 要设置为新主库 A’的从库的语法如下:

```
CHANGE MASTER TO
MASTER_HOST=$host_name
MASTER_PORT=$port
MASTER_USER=$user_name
MASTER_PASSWORD=$password
master_auto_position=1
```

其中，master_auto_position=1 就表示这个主备关系使用的是 GTID 协议。我们把现在这个时刻，实例 A’的 GTID 集合记为 set_a，实例 B 的 GTID 集合记为 set_b。接下来，我们就看看现在的主备切换逻辑。

我们在实例 B 上执行 start slave 命令，取 binlog 的逻辑是这样的: 

1. 实例 B 指定主库 A’，基于主备协议建立连接。

2. 实例 B 把 set_b 发给主库 A’。

3. 实例 A’算出 set_a 与 set_b 的差集，也就是所有存在于 set_a，但是不存在于 set_b的 GITD 的集合，判断 A’本地是否包含了这个差集需要的所有 binlog 事务。
   * 如果不包含，表示 A’已经把实例 B 需要的 binlog 给删掉了，直接返回错误;
   * 如果确认全部包含，A’从自己的 binlog 文件里面，找出第一个不在 set_b 的事 务，发给 B;

4. 之后就从这个事务开始，往后读文件，按顺序取 binlog 发给 B 去执行。

## Proxey

<img src="https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/QunfVK.png" alt="QunfVK" style="zoom:50%;" />

在 MySQL 和客户端之间有一个中间代理层 proxy，客户端只连接 proxy， 由 proxy 根据请求类型和上下文决定请求的分发路由。

带 proxy 的架构，对客户端比较友好。客户端不需要关注后端细节，连接维护、后端信 息维护等工作，都是由 proxy 完成的。但这样的话，对后端维护团队的要求会更高。而 且，proxy 也需要有高可用架构。因此，带 proxy 架构的整体就相对比较复杂。

## 过期读

于主从可能存在延迟，客户端执行完一个更新事务后马上发起查询，如果查询选择的是从库的话，就有可能读到刚刚的事务更新之前的状态。

### 强制走主库方案

强制走主库方案其实就是，将查询请求做分类。通常情况下，我们可以将查询请求分为这么两类:

1. 对于必须要拿到最新结果的请求，强制将其发到主库上。比如，在一个交易平台上，卖家发布商品以后，马上要返回主页面，看商品是否发布成功。那么，这个请求需要拿到最新的结果，就必须走主库。

2. 对于可以读到旧数据的请求，才将其发到从库上。在这个交易平台上，买家来逛商铺页面，就算晚几秒看到最新发布的商品，也是可以接受的。那么，这类请求就可以走从库。

### 判断主备无延迟方案

**第一种确保主备无延迟的方法是:**

* 每次从库执行查询请求前，先判断 seconds_behind_master 是否已经等于 0。如果还不等于 0 ，那就必须等到这个参数变 为 0 才能执行查询请求。

**第二种方法，对比位点确保主备无延迟:**

* Master_Log_File 和 Read_Master_Log_Pos，表示的是读到的主库的最新位点; Relay_Master_Log_File 和 Exec_Master_Log_Pos，表示的是备库执行的最新位点。

* 如果 Master_Log_File 和 Relay_Master_Log_File、Read_Master_Log_Pos 和 Exec_Master_Log_Pos 这两组值完全相同，就表示接收到的日志已经同步完成

**第三种方法，对比 GTID 集合确保主备无延迟:**

* Auto_Position=1, 表示这对主备关系使用了 GTID 协议。 

* Retrieved_Gtid_Set，是备库收到的所有日志的 GTID 集合; 

* Executed_Gtid_Set，是备库所有已经执行完成的 GTID 集合。

如果这两个集合相同，也表示备库接收到的日志都已经同步完成。

虽然后面两种比第一种要准确，但是这三种都没有达到完美的程度。



我们现在一起来回顾下，一个事务的 binlog 在主备库之间的状态:

1. 主库执行完成，写入 binlog，并反馈给客户端; 
2. binlog 被从主库发送给备库，备库收到;

3. 在备库执行 binlog 完成。

我们上面判断主备无延迟的逻辑，是“备库收到的日志都执行完成了”。但是，从 binlog 在主备之间状态的分析中，不难看出还有一部分日志，处于客户端已经收到提交确认，而 备库还没收到日志的状态。

<img src="https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/Gp3Hee.png" alt="Gp3Hee" style="zoom:50%;" />

假设主库上执行完成了三个事务 trx1、trx2 和 trx3，其中:

1. trx1 和 trx2 已经传到从库，并且已经执行完成了;

2. trx3 在主库执行完成，并且已经回复给客户端，但是还没有传到从库中。

如果这时候你在从库 B 上执行查询请求，按照我们上面的逻辑，从库认为已经没有同步延迟，但还是查不到 trx3 的。严格地说，就是出现了过期读。

### 配合 semi-sync

要解决这个问题，就要引入半同步复制，也就是 semi-sync replication。

semi-sync 做了这样的设计:

1. 事务提交的时候，主库把 binlog 发给从库;

2. 从库收到 binlog 以后，发回给主库一个 ack，表示收到了;

3. 主库收到这个 ack 以后，才能给客户端返回“事务完成”的确认。

也就是说，如果启用了 semi-sync，就表示所有给客户端发送过确认的事务，都确保了备库已经收到了这个日志。但是也存在两个问题:

1. 一主多从的时候，某个从库返回ack，但是其它还没有，在某些从库执行查询请求会存在过期读的现象;
2. 在持续延迟的情况下，可能出现过度等待的问题。

接下来，我要和你介绍的等主库位点方案，就可以解决这两个问题。

### 等主库位点方案

要理解等主库位点方案，我需要先和你介绍一条命令: `select master_pos_wait(file, pos[, timeout]);`

这条命令的逻辑如下:

1. 它是在从库执行的;

2. 参数 file 和 pos 指的是主库上的文件名和位置;

3. timeout 可选，设置为正整数 N 表示这个函数最多等待 N 秒。

它的返回值是

1. 正常结果是正整数 M，表示从命令开始执行，到应用完 file 和 pos 表示的 binlog 位置，执行了多少事务。
2. 如果执行期间，备库同步线程发生异常，则返回 NULL;

2. 如果等待超过 N 秒，就返回 -1;

3. 如果刚开始执行的时候，就发现已经执行过这个位置了，则返回 0。

对于图中先执行 trx1，再执行一个查询请求的逻辑，要保证能够查到正确的数据，我们可以使用这个逻辑:

1. trx1 事务更新完成后，马上执行 show master status 得到当前主库执行到的 File 和 Position;

2. 选定一个从库执行查询语句;

3. 在从库上执行 select master_pos_wait(File, Position, 1); 
4. 如果返回值是 >=0 的正整数，则在这个从库执行查询语句;
5. 否则，到主库执行查询语句。

#### GTID 方案

如果你的数据库开启了 GTID 模式，对应的也有等待 GTID 的方案。

MySQL 中同样提供了一个类似的命令:`select wait_for_executed_gtid_set(gtid_set, 1);`

这条命令的逻辑是:

1. 等待，直到这个库执行的事务中包含传入的 gtid_set，返回 0; 
2. 超时返回 1。

这时，等 GTID 的执行流程就变成了:

1. trx1 事务更新完成后，从返回包直接获取这个事务的 GTID，记为 gtid1; 
2. 选定一个从库执行查询语句;

3. 在从库上执行 select wait_for_executed_gtid_set(gtid1, 1);

4. 如果返回值是 0，则在这个从库执行查询语句;

5. 否则，到主库执行查询语句。

