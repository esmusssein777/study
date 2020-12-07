# MySQL 底层

[toc]

## MySQL 架构图

大体来说，MySQL 可以分为 Server 层和存储引擎层两部分。

Server 层包括连接器、查询缓存、分析器、优化器、执行器等，涵盖 MySQL 的大多数核心服务功能，以及所有的内置函数(如日期、时间、数学和加密函数等)，所有跨存储引擎的功能都在这一层实现，比如存储过程、触发器、视图等。

而存储引擎层负责数据的存储和提取。其架构模式是插件式的，支持 InnoDB、 MyISAM、Memory 等多个存储引擎。现在最常用的存储引擎是 InnoDB，它从 MySQL 5.5.5 版本开始成为了默认存储引擎。

![8XS0r9](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/8XS0r9.png)

### 连接器

连接器负责跟客户端建立连接、获取权限、维持和管理连接。

如果用户名密码认证通过，连接器会到权限表里面查出你拥有的权限。之后，这个连接里面的权限判断逻辑，都将依赖于此时读到的权限。这就意味着，一个用户成功建立连接后，即使你用管理员账号对这个用户的权限做了修改，也不会影响已经存在连接的权限。修改完成后，只有再新建的连接才会使用新的权限设置。



客户端如果太长时间没动静，连接器就会自动将它断开。这个时间是由参数 wait_timeout 控制的，默认值是 8 小时。

如果在连接被断开之后，客户端再次发送请求的话，就会收到一个错误提醒: Lost connection to MySQL server during query。这时候如果你要继续，就需要重连，然后再执行请求了。

数据库里面，长连接是指连接成功后，如果客户端持续有请求，则一直使用同一个连接。短连接则是指每次执行完很少的几次查询就断开连接，下次查询再重新建立一个。

建立连接的过程通常是比较复杂的，建议在使用中要尽量减少建立连接的动作，也就是尽量使用长连接。

但是全部使用长连接后，你可能会发现，有些时候 MySQL 占用内存涨得特别快，这是因为 MySQL 在执行过程中临时使用的内存是管理在连接对象里面的。这些资源会在连接断开的时候才释放。所以如果长连接累积下来，可能导致内存占用太大，被系统强行杀掉 (OOM)，从现象看就是 MySQL 异常重启了。

怎么解决这个问题呢?你可以考虑以下两种方案。

1. 定期断开长连接。使用一段时间，或者程序里面判断执行过一个占用内存的大查询后， 断开连接，之后要查询再重连。

2. 如果你用的是 MySQL 5.7 或更新版本，可以在每次执行一个比较大的操作后，通过执行 mysql_reset_connection 来重新初始化连接资源。这个过程不需要重连和重新做权限验证，但是会将连接恢复到刚刚创建完时的状态。

### 查询缓存

连接建立完成后，你就可以执行 select 语句了。执行逻辑就会来到第二步:查询缓存。

MySQL 拿到一个查询请求后，会先到查询缓存看看，之前是不是执行过这条语句。之前执行过的语句及其结果可能会以 key-value 对的形式，被直接缓存在内存中。key 是查询的语句，value 是查询的结果。如果你的查询能够直接在这个缓存中找到 key，那么这个 value 就会被直接返回给客户端。

如果语句不在查询缓存中，就会继续后面的执行阶段。执行完成后，执行结果会被存入查询缓存中。你可以看到，如果查询命中缓存，MySQL 不需要执行后面的复杂操作，就可以直接返回结果，这个效率会很高。

***但是大多数情况下我会建议你不要使用查询缓存，为什么呢?因为查询缓存往往弊大于利。查询缓存的失效非常频繁，只要有对一个表的更新，这个表上所有的查询缓存都会被清空***。

可以用 `SHOW VARIABLES LIKE '%query_cache%';`来查看 type 是否开启查询缓存

需要注意的是，MySQL 8.0 版本直接将查询缓存的整块功能删掉了，也就是说 8.0 开始彻底没有这个功能了。

### 分析器

分析器先会做“词法分析”。你输入的是由多个字符串和空格组成的一条 SQL 语句， MySQL 需要识别出里面的字符串分别是什么，代表什么。

MySQL 从你输入的"select"这个关键字识别出来，这是一个查询语句。它也要把字符串“T”识别成“表名 T”，把字符串“ID”识别成“列 ID”。

如果你输入一条 `select * from T where T.name=1;` 如果表T或者字段name在数据库没有的话，那么在分析器层应该返回`Unknown column ‘k’ in ‘where clause`

### 优化器

优化器是在表里面有多个索引的时候，决定使用哪个索引;或者在一个语句有多表关联 (join)的时候，决定各个表的连接顺序。比如你执行下面这样的语句，这个语句是执行 两个表的 join:

```mysql> select * from t1 join t2 using(ID) where t1.c=10 and t2.d=20;```

既可以先从表 t1 里面取出 c=10 的记录的 ID 值，再根据 ID 值关联到表 t2，再判断 t2 里面 d 的值是否等于 20。

也可以先从表 t2 里面取出 d=20 的记录的 ID 值，再根据 ID 值关联到 t1，再判断 t1 里面 c 的值是否等于 10。

这两种执行方法的逻辑结果是一样的，但是执行的效率会有不同，而优化器的作用就是决定选择使用哪一个方案。

### 执行器

开始执行的时候，要先判断一下你对这个表 T 有没有执行查询的权限，如果没有，就会返回没有权限的错误，如下所示 (在工程实现上，如果命中查询缓存，会在查询缓存返回结果的时候，做权限验证。查询也会在优化器之前调用 precheck 验证权限)。

> 为什么要在执行器才分析权限，主要是查询的语句有时候很麻烦，join表，或者有个触发器，很多时候在执行时才能确定

比如我们这个例子中的表 T 中，ID 字段没有索引，那么执行器的执行流程是这样的:

1. 调用 InnoDB 引擎接口取这个表的第一行，判断 ID 值是不是 10，如果不是则跳过，如果是则将这行存在结果集中;

2. 调用引擎接口取“下一行”，重复相同的判断逻辑，直到取到这个表的最后一行。
3. 执行器将上述遍历过程中所有满足条件的行组成的记录集作为结果集返回给客户端。

### 日志模块

#### redo log

在 MySQL 里如果每一次的更新操作都需要写进磁盘，然后磁盘也要找到对应的那条记录，然后再更新，整个过程 IO 成本、查找成本都很高。

当有一条记录需要更新的时候，InnoDB 引擎就会先把记录写到 redo log里面，并更新内存，这个时候更新就算完成了。同时，InnoDB 引擎会在适当的时候，将这个操作记录更新到磁盘里面，而这个更新往往是在系统比较空闲的时候做。

<img src="https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/s0lyWr.png" alt="s0lyWr" style="zoom: 25%;" />

write pos 是当前记录的位置，一边写一边后移，写到第 3 号文件末尾后就回到 0 号文件开头。checkpoint 是当前要擦除的位置，也是往后推移并且循环的，擦除记录前要把记录更新到数据文件。

write pos 和 checkpoint 之间的是“粉板”上还空着的部分，可以用来记录新的操作。如果 write pos 追上 checkpoint，表示“粉板”满了，这时候不能再执行新的更新，得停下来先擦掉一些记录，把 checkpoint 推进一下。

有了 redo log，InnoDB 就可以保证即使数据库发生异常重启，之前提交的记录都不会丢失，这个能力称为crash-safe。

#### binlog

前面我们讲过，MySQL 整体来看，其实就有两块:一块是 Server 层，它主要做的是 MySQL 功能层面的事情;还有一块是引擎层，负责存储相关的具体事宜。上面我们聊到的粉板 redo log 是 InnoDB 引擎特有的日志，而 Server 层也有自己的日志，称为 binlog(归档日志)。

我想你肯定会问，为什么会有两份日志呢?

因为最开始 MySQL 里并没有 InnoDB 引擎。MySQL 自带的引擎是 MyISAM，但是 MyISAM 没有 crash-safe 的能力，binlog 日志只能用于归档。而 InnoDB 是另一个公司 以插件形式引入 MySQL 的，既然只依靠 binlog 是没有 crash-safe 能力的，所以 InnoDB 使用另外一套日志系统——也就是 redo log 来实现 crash-safe 能力。

这两种日志有以下三点不同。redo log 是 InnoDB 引擎特有的;binlog 是 MySQL 的 Server 层实现的，所有引擎都可以使用。

2. redo log 是物理日志，记录的是“在某个数据页上做了什么修改”;binlog 是逻辑日志，记录的是这个语句的原始逻辑，比如“给 ID=2 这一行的 c 字段加 1 ”。

3. redo log 是循环写的，空间固定会用完;binlog 是可以追加写入的。“追加写”是指 binlog 文件写到一定大小后会切换到下一个，并不会覆盖以前的日志。

binlog 有三种格式，分别是 statement、row、mixed。statement 格式的话是记sql语句， row格式会记录行的内容，记两条，更新前和更新后都有，mixed是mysql根据实际情况优化，两种都包含。

有了对这两个日志的概念性理解，我们再来看执行器和 InnoDB 引擎在执行这个简单的 update 语句时的内部流程。

1. 执行器先找引擎取 ID=2 这一行。ID 是主键，引擎直接用树搜索找到这一行。如果 ID=2 这一行所在的数据页本来就在内存中，就直接返回给执行器;否则，需要先从磁盘读入内存，然后再返回。

2. 执行器拿到引擎给的行数据，把这个值加上 1，比如原来是 N，现在就是 N+1，得到新的一行数据，再调用引擎接口写入这行新数据。

3. 引擎将这行新数据更新到内存中，同时将这个更新操作记录到 redo log 里面，此时 redo log 处于 prepare 状态。然后告知执行器执行完成了，随时可以提交事务。

4. 执行器生成这个操作的 binlog，并把 binlog 写入磁盘。

5. 执行器调用引擎的提交事务接口，引擎把刚刚写入的 redo log 改成提交(commit)状态，更新完成。

这里我给出这个 update 语句的执行流程图，图中浅色框表示是在 InnoDB 内部执行的， 深色框表示是在执行器中执行的。

<img src="https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/95t4P5.png" alt="95t4P5" style="zoom:50%;" />

redo log 的写入拆成了两个步骤: prepare 和 commit，这就是"两阶段提交"。

## 机制

### crash safe

<img src="https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/95t4P5.png" alt="95t4P5" style="zoom:50%;" />

时刻A属于 redolog prepare到写binlog阶段，时刻B属于写 bingo 到 commit阶段。

如果在图中时刻 A 的地方，也就是写入 redo log 处于 prepare 阶段之后、写 binlog 之前，发生了崩溃(crash)，由于此时 binlog 还没写，redo log 也还没提交，所以崩溃恢复的时候，这个事务会回滚。这时候，binlog 还没写，所以也不会传到备库。到这里，大家都可以理解。

大家出现问题的地方，主要集中在时刻 B，也就是 binlog 写完，redo log 还没 commit 前发生 crash，那崩溃恢复的时候 MySQL 会怎么处理?

我们先来看一下崩溃恢复时的判断规则。

1. 如果 redo log 里面的事务是完整的，也就是已经有了 commit 标识，则直接提交;

2. 如果 redo log 里面的事务只有完整的 prepare，则判断对应的事务 binlog 是否存在并完整:

   * 如果是，则提交事务; 

   * 否则，回滚事务。

**如何判断 binlog是否完整呢？**

* statement 格式的 binlog，最后会有 COMMIT; row 格式的 binlog，最后会有一个 XID event。

**redo log 和 binlog 是怎么关联起来的？**

* 它们有一个共同的数据字段，叫 XID。崩溃恢复的时候，会按顺序扫描 redo log:
  * 如果碰到既有 prepare、又有 commit 的 redo log，就直接提交;
  * 如果碰到只有 parepare、而没有 commit 的 redo log，就拿着 XID 去 binlog 找对应的事务。

### change buffer

当需要更新一个数据页时，如果数据页在内存中就直接更新，而如果这个数据页还没有在内存中的话，在不影响数据一致性的前提下，InooDB 会将这些更新操作缓存在 change buffer 中，这样就不需要从磁盘中读入这个数据页了。在下次查询需要访问这个数据页的时候，将数据页读入内存，然后执行 change buffer 中与这个页有关的操作。通过这种方式就能保证这个数据逻辑的正确性。

需要说明的是，虽然名字叫作 change buffer，实际上它是可以持久化的数据。也就是说，change buffer 在内存中有拷贝，也会被写入到磁盘上。

将 change buffer 中的操作应用到原数据页，得到最新结果的过程称为 merge。除了访问这个数据页会触发 merge 外，系统有后台线程会定期 merge。在数据库正常关闭 (shutdown)的过程中，也会执行 merge 操作。

显然，如果能够将更新操作先记录在 change buffer，减少读磁盘，语句的执行速度会得到明显的提升。而且，数据读入内存是需要占用 buffer pool 的，所以这种方式还能够避免占用内存，**提高内存利用率**。

>如果不使用change buffer ，一次性写入很多不在内存的数据的话，那么内存的利用率会急剧下降

我们要在表上执行这个插入语句:

```
mysql> insert into t(id,k) values(id1,k1),(id2,k2);
```

这里，我们假设当前 k 索引树的状态，查找到位置后，k1 所在的数据页在内存 (InnoDB buffer pool) 中，k2 所在的数据页不在内存中。如图 2 所示是带 change buffer 的更新状态图。

![Q55UKs](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/Q55UKs.png)

分析这条更新语句，你会发现它涉及了四个部分:内存、redo log(ib_log_fileX)、 数据表空间(t.ibd)、系统表空间(ibdata1)。 这条更新语句做了如下的操作(按照图中的数字顺序):

1. Page 1 在内存中，直接更新内存;

2. Page 2 没有在内存中，就在内存的 change buffer 区域，记录下“我要往 Page 2 插入一行”这个信息

3. 将上述两个动作记入 redo log 中(图中 3 和 4)。

做完上面这些，事务就可以完成了。所以，你会看到，执行这条更新语句的成本很低，就是写了两处内存，然后写了一处磁盘(两次操作合在一起写了一次磁盘)，而且还是顺序写的。

同时，图中的两个虚线箭头，是后台操作，不影响更新的响应时间。

那在这之后的读请求，要怎么处理呢?比如，我们现在要执行 select * from t where k in (k1, k2)。这里，我画了这两个读请求 的流程图。

如果读语句发生在更新语句后不久，内存中的数据都还在，那么此时的这两个读操作就与系统表空间(ibdata1)和 redo log(ib_log_fileX)无关了。所以，我在图中就没画出 这两部分。

![0SjiAU](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/0SjiAU.png)

1. 读 Page 1 的时候，直接从内存返回。有几位同学在前面文章的评论中问到，WAL 之后如果读数据，是不是一定要读盘，是不是一定要从 redo log 里面把数据更新以后才可以返回?其实是不用的。你可以看一下图 3 的这个状态，虽然磁盘上还是之前的数据， 但是这里直接从内存返回结果，结果是正确的。

2. 要读 Page 2 的时候，需要把 Page 2 从磁盘读入内存中，然后应用 change buffer 里面的操作日志，生成一个正确的版本并返回结果。

可以看到，直到需要读 Page 2 的时候，这个数据页才会被读入内存。

所以，如果要简单地对比这两个机制在提升更新性能上的收益的话，redo log 主要节省的是随机写磁盘的 IO 消耗(转成顺序写)，而 change buffer 主要节省的则是随机读磁盘 的 IO 消耗。

#### 使用场景

因此，对于写多读少的业务来说，页面在写完以后马上被访问到的概率比较小，此时 change buffer 的使用效果最好。这种业务模型常见的就是账单类、日志类的系统。

反过来，假设一个业务的更新模式是写入之后马上会做查询，那么即使满足了条件，将更新先记录在 change buffer，但之后由于马上要访问这个数据页，会立即触发 merge 过程。这样随机访问 IO 的次数不会减少，反而增加了 change buffer 的维护代价。所以， 对于这种业务模式来说，change buffer 反而起到了副作用。

#### 丢失问题

change buffer 一开始是写内存的，那么如果这个时候机器掉电重启，会不会导致 change buffer 丢失呢?change buffer 丢失可不是小事儿，再从磁盘读入数据可就没有了 merge 过程，就等于是数据丢失了。会不会出现这种情况呢?

答案是不会丢失。虽然是只更新内存，但是在事务提交的时候，我们把 change buffer 的操作也记录到 redo log 里了，所以崩溃恢复的时候，change buffer 也能找回来。

### binlog写入

binlog 的写入逻辑比较简单:事务执行过程中，先把日志写到 binlog cache，事务提交的时候，再把 binlog cache 写到 binlog 文件中。

一个事务的 binlog 是不能被拆开的，因此不论这个事务多大，也要确保一次性写入。

系统给 binlog cache 分配了一片内存，每个线程一个，参数 binlog_cache_size 用于控制单个线程内 binlog cache 所占内存的大小。如果超过了这个参数规定的大小，就要暂存到磁盘。

事务提交的时候，执行器把 binlog cache 里的完整事务写入到 binlog 中，并清空 binlog cache。状态如图。

![COIPn7](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/COIPn7.png)

可以看到，每个线程有自己 binlog cache，但是共用同一份 binlog 文件。

图中的 write，指的就是指把日志写入到文件系统的 page cache，并没有把数据持久化到磁盘，所以速度比较快。

图中的 fsync，才是将数据持久化到磁盘的操作。一般情况下，我们认为 fsync 才占磁盘的 IOPS。

write 和 fsync 的时机，是由参数 sync_binlog 控制的:

1. sync_binlog=0 的时候，表示每次提交事务都只 write，不 fsync;

2. sync_binlog=1 的时候，表示每次提交事务都会执行 fsync;

3. sync_binlog=N(N>1) 的时候，表示每次提交事务都 write，但累积 N 个事务后才fsync。

因此，在出现 IO 瓶颈的场景里，将 sync_binlog 设置成一个比较大的值，可以提升性能。在实际的业务场景中，考虑到丢失日志量的可控性，一般不建议将这个参数设成 0， 比较常见的是将其设置为 100~1000 中的某个数值。对应的风险是:如果主机发生异常重启，会丢失最近 N 个事务的 binlog 日志。

#### redo log写入

<img src="https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/E1Tf41.png" alt="E1Tf41" style="zoom:50%;" />

为了控制 redo log 的写入策略，InnoDB 提供了 innodb_flush_log_at_trx_commit 参数，它有三种可能取值:

1. 设置为 0 的时候，表示每次事务提交时都只是把 redo log 留在 redo log buffer 中 ; 
2. 设置为 1 的时候，表示每次事务提交时都将 redo log 直接持久化到磁盘;

3. 设置为 2 的时候，表示每次事务提交时都只是把 redo log 写到 page cache。

InnoDB 有一个后台线程，每隔 1 秒，就会把 redo log buffer 中的日志，调用 write 写到文件系统的 page cache，然后调用 fsync 持久化到磁盘。

实际上，除了后台线程每秒一次的轮询操作外，还有两种场景会让一个没有提交的事务的 redo log 写入到磁盘中。

1. 一种是，redo log buffer 占用的空间即将达到 innodb_log_buffer_size 一半的时候，后台线程会主动写盘。注意，由于这个事务并没有提交，所以这个写盘动作只是 write，而没有调用 fsync，也就是只留在了文件系统的 page cache。

2. 另一种是，并行的事务提交的时候，顺带将这个事务的 redo log buffer 持久化到磁盘。假设一个事务 A 执行到一半，已经写了一些 redo log 到 buffer 中，这时候有另外一个线程的事务 B 提交，如果 innodb_flush_log_at_trx_commit 设置的是 1，那么 按照这个参数的逻辑，事务 B 要把 redo log buffer 里的日志全部持久化到磁盘。这时候，就会带上事务 A 在 redo log buffer 里的日志一起持久化到磁盘。

这里需要说明的是，我们介绍两阶段提交的时候说过，时序上 redo log 先 prepare， 再写 binlog，最后再把 redo log commit。

如果把 innodb_flush_log_at_trx_commit 设置成 1，那么 redo log 在 prepare 阶段就 要持久化一次，因为有一个崩溃恢复逻辑是要依赖于 prepare 的 redo log，再加上 binlog 来恢复的。

每秒一次后台轮询刷盘，再加上崩溃恢复这个逻辑，InnoDB 就认为 redo log 在 commit 的时候就不需要 fsync 了，只会 write 到文件系统的 page cache 中就够了。

通常我们说 MySQL 的“双 1”配置，指的就是 sync_binlog 和 innodb_flush_log_at_trx_commit 都设置成 1。也就是说，一个事务完整提交前，需要等待两次刷盘，一次是 redo log(prepare 阶段)，一次是 binlog。

#### 组提交 group commit

日志逻辑序列号(log sequence number，LSN)的概念。LSN 是单调递增的，用来对应 redo log 的一个个写入点。每次写入长度为 length 的 redo log， LSN 的值就会加上 length。

LSN 也会写到 InnoDB 的数据页中，来确保数据页不会被多次执行重复的 redo log。

在并发更新场景下，第一个事务写完 redo log buffer 以后，接下来这个 fsync 越晚调用，组员可能越多，节约 IOPS 的效果就越好。

为了让一次 fsync 带的组员更多，MySQL 有一个很有趣的优化

<img src="https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/thyqre.png" alt="thyqre" style="zoom:50%;" />

这么一来，binlog 也可以组提交了。在执行图 5 中第 4 步把 binlog fsync 到磁盘时，如果有多个事务的 binlog 已经写完了，也是一起持久化的，这样也可以减少 IOPS 的消耗。

#### binlog 格式

假设我们输入一个sql语句

```
mysql> delete from t where a>=4 and t_modified<='2018-11-10' limit 1;
```

##### statement

当 binlog_format=statement 时，binlog 里面记录的就是 SQL 语句的原文。

运行 `mysql> show binlog events in 'master.000001' `可以看到详情

但是运行这条 delete 命令产生了一个 warning，原因是当前 binlog 设置的是 statement 格式，并且语句中有 limit，所以这个命令可能是 unsafe 的。

为什么这么说呢?这是因为 delete 带 limit，很可能会出现主备数据不一致的情况。比如 上面这个例子:

1. 如果 delete 语句使用的是索引 a，那么会根据索引 a 找到第一个满足条件的行，也就 是说删除的是 a=4 这一行;

2. 但如果使用的是索引 t_modified，那么删除的就是 t_modified='2018-11-09’也就 是 a=5 这一行。

由于 statement 格式下，记录到 binlog 里的是语句原文，因此可能会出现这样一种情 况:在主库执行这条 SQL 语句的时候，用的是索引 a;而在备库执行这条 SQL 语句的时 候，却使用了索引 t_modified。因此，MySQL 认为这样写是有风险的。

##### row

若是 binlog_format=row 时，binlog 里面记录的就是 SQL 的记录。

如果执行的是 delete 语句，row 格式的 binlog 也会把被删掉的行的整行信息保存起来。

如果执行的是 update 语句的话，binlog 里面会记录修改前整行的数据和修改后的整行数据。

所以当 binlog_format 使用 row 格式的时候，binlog 里面记录了真实删除行的主键 id，这样 binlog 传到备库去的时候，就肯定会删除 id=4 的行，不会有主备删除不同行的问题。

##### mixed

还有一种是 mixed 的格式，为什么会存在statement 和 row 两种格式都存在的情况呢？

因为有些 statement 格式的 binlog 可能会导致主备不一致，所以要使用 row 格式。

但 row 格式的缺点是，很占空间。比如你用一个 delete 语句删掉 10 万行数据，用 statement 的话就是一个 SQL 语句被记录到 binlog 中，占用几十个字节的空间。但如 果用 row 格式的 binlog，就要把这 10 万条记录都写到 binlog 中。这样做，不仅会占 用更大的空间，同时写 binlog 也要耗费 IO 资源，影响执行速度。

所以，MySQL 就取了个折中方案，也就是有了 mixed 格式的 binlog。mixed 格式的 意思是，MySQL 自己会判断这条 SQL 语句是否可能引起主备不一致，如果有可能，就 用 row 格式，否则就用 statement 格式。



当然我要说的是，现在越来越多的场景要求把 MySQL 的 binlog 格式设置成 row。这么做的理由有很多，直接看出来的好处: 恢复数据。

因为改动前和改动后的数据都保存着，所以在恢复数据的时候很快也很准确。

### 全表扫描

#### server层

InnoDB 的数据是保存在主键索引上的，所以全表扫描实际上是直接扫描表 t 的主键索引。这条查询语句由于没有其他的判断条件，所以查到的每一行都可以直接放到结果集里面，然后返回给客户端。

那么，这个“结果集”存在哪里呢?

实际上，服务端并不需要保存一个完整的结果集。取数据和发数据的流程是这样的:

1. 获取一行，写到 net_buffer 中。这块内存的大小是由参数 net_buffer_length 定义 的，默认是 16k。

2. 重复获取行，直到 net_buffer 写满，调用网络接口发出去。

3. 如果发送成功，就清空 net_buffer，然后继续取下一行，并写入 net_buffer。

4. 如果发送函数返回 EAGAIN 或 WSAEWOULDBLOCK，就表示本地网络栈(socketsend buffer)写满了，进入等待。直到网络栈重新可写，再继续发送。

也就是说，MySQL 是“边读边发的”，这个概念很重要。这就意味着，如果客户端接收得慢，会导致 MySQL 服务端由于结果发不出去，这个事务的执行时间变长。

如果客户端使用–quick 参数，会使用 mysql_use_result 方法。这个方法是读一行处理一行。你可以想象一下，假设有一个业务的逻辑比较复杂，每读一行数据以后要处理的逻辑如果很慢，就会导致客户端要过很久才会去取下一行数据。

因此，对于正常的线上业务来说，如果一个查询的返回结果不会很多的话，我都建议你使用 mysql_store_result 这个接口，直接把查询结果保存到本地内存。

#### InnoDB层

内存的数据页是在 Buffer Pool (BP) 中管理的，在 WAL 里 Buffer Pool 起到了加速更新 的作用。而实际上，Buffer Pool 还有一个更重要的作用，就是加速查询。

而 Buffer Pool 对查询的加速效果，依赖于一个重要的指标，即:内存命中率。

你可以在 show engine innodb status 结果中，查看一个系统当前的 BP 命中率。一般情况下，一个稳定服务的线上系统，要保证响应时间符合要求的话，内存命中率要在 99% 以上。

执行 show engine innodb status ，可以看到“Buffer pool hit rate”字样，显示的就是当前的命中率。InnoDB Buffer Pool 的大小是由参数 innodb_buffer_pool_size 确定的，一般建议设置 成可用物理内存的 60%~80%。

InnoDB 管理 Buffer Pool 的 LRU 算法，是用链表来实现的。

1. 链表头部是 P1，表示 P1 是最近刚刚被访问过的数据页;假设内存里只能放下这么多数据页;

2. 这时候有一个读请求访问 P3，P3 被移到最前面;

3. 这次访问的数据页是不存在于链表中的，所以需要在 Buffer Pool 中新申请一个数据页 Px，加到链表头部。但是由于内存已经满了，不能申请新的内存。于是，会清空链表末尾 Pm 这个数据页的内存，存入 Px 的内容，然后放到链表头部。

4. 从效果上看，就是最久没有被访问的数据页 Pm，被淘汰了。

这个算法乍一看上去没什么问题，但是如果考虑到要做一个全表扫描，会不会有问题呢?

假设按照这个算法，我们要扫描一个 200G 的表，而这个表是一个历史数据表，平时没有业务访问它。那么，按照这个算法扫描的话，就会把当前的 Buffer Pool 里的数据全部淘汰掉，存入扫描过程中访问到的数据页的内容。也就是说 Buffer Pool 里面主要放的是这个历史数据表的数据。对于一个正在做业务服务的库，这可不妙。你会看到，Buffer Pool 的内存命中率急剧下降，磁盘压力增加，SQL 语句响应变慢。

<img src="https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/2g2vCk.png" alt="2g2vCk" style="zoom:50%;" />

在 InnoDB 实现上，按照 5:3 的比例把整个 LRU 链表分成了 young 区域和 old 区域。图 中 LRU_old 指向的就是 old 区域的第一个位置，是整个链表的 5/8 处。也就是说，靠近链表头部的 5/8 是 young 区域，靠近链表尾部的 3/8 是 old 区域。

改进后的 LRU 算法执行流程变成了下面这样。

1. 图中状态 1，要访问数据页 P3，由于 P3 在 young 区域，因此和优化前的 LRU 算法 一样，将其移到链表头部，变成状态 2。

2. 之后要访问一个新的不存在于当前链表的数据页，这时候依然是淘汰掉数据页 Pm，但是新插入的数据页 Px，是放在 LRU_old 处。

3. 处于 old 区域的数据页，每次被访问的时候都要做下面这个判断: 若这个数据页在 LRU 链表中存在的时间超过了 1 秒，就把它移动到链表头部;

如果这个数据页在 LRU 链表中存在的时间短于 1 秒，位置保持不变。1 秒这个时间，是由参数 innodb_old_blocks_time 控制的。其默认值是 1000，单位毫秒。这个策略，就是为了处理类似全表扫描的操作量身定制的。还是以刚刚的扫描 200G 的历 史数据表为例，我们看看改进后的 LRU 算法的操作逻辑:

1. 扫描过程中，需要新插入的数据页，都被放到 old 区域 ;

2. 一个数据页里面有多条记录，这个数据页会被多次访问到，但由于是顺序扫描，这个数据页第一次被访问和最后一次被访问的时间间隔不会超过 1 秒，因此还是会被保留在old 区域;

3. 再继续扫描后续的数据，之前的这个数据页之后也不会再被访问到，于是始终没有机会移到链表头部(也就是 young 区域)，很快就会被淘汰出去。

可以看到，这个策略最大的收益，就是在扫描这个大表的过程中，虽然也用到了 Buffer Pool，但是对 young 区域完全没有影响，从而保证了 Buffer Pool 响应正常业务的查询命中率。

