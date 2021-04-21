# MySQL问题分析

[toc]

## 查询时间长

插入10万行数据

```
mysql> CREATE TABLE `t` (
`id` int(11) NOT NULL,
`c` int(11) DEFAULT NULL,
PRIMARY KEY (`id`) ) ENGINE=InnoDB;

delimiter ;;
create procedure idata()
begin
	declare i int;
	set i=1;
  while(i<=100000)do
		insert into t values(i,i);
		set i=i+1; 
	end while;
end;; 
delimiter ;

call idata();
```

### 长时间不返回结果

在表 t 执行下面的 SQL 语句: `select * from t where id=1` 查询结果长时间不返回。

一般碰到这种情况的话，大概率是表 t 被锁住了。接下来分析原因的时候，一般都是首先 执行一下 show processlist 命令，看看当前语句处于什么状态。

然后我们再针对每种状态，去分析它们产生的原因、如何复现，以及如何处理。

#### 等 MDL 锁

使用 show processlist 命令查看 Waiting for table metadata lock 的示意图。

![JihbDz](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/JihbDz.png)

出现这个状态表示的是，现在有一个线程正在表 t 上请求或者持有 MDL 写锁，把 select 语句堵住了。

这类问题的处理方式，就是找到谁持有 MDL 写锁，然后把它 kill 掉。通过查询 sys.schema_table_lock_waits 这张表，我们就可以直接找出造成阻塞的 process id，把这个连接用 kill 命令断开即可。

```
select blocking_pid from sys.schema_table_lock_waits
```

#### 等行锁

`mysql> select * from t where id=1 lock in share mode;`

由于访问 id=1 这个记录时要加读锁，如果这时候已经有一个事务在这行记录上持有一个 写锁，我们的 select 语句就会被堵住。

这个问题并不难分析，但问题是怎么查出是谁占着这个写锁。如果你用的是 MySQL 5.7 版本，可以通过 sys.innodb_lock_waits 表查到。

```
select * from t sys.innodb_lock_waits where locked_table=`'test'.'t'`\G
```

查看 blocking_pid 即可，kill 4即可

### 查询慢

![6Vb0xv](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/6Vb0xv.png)

`select * from t where id=1`花费800ms，

而`select * from t where id=1 lock in share mode`只要1ms。

这是因为session B 更新完 100 万次，生成了 100 万个回滚日志 (undo log)。

带 lock in share mode 的 SQL 语句，是当前读，因此会直接读到 1000001 这个结果， 所以速度很快;而 select * from t where id=1 这个语句，是一致性读，因此需要从 1000001 开始，依次执行 undo log，执行了 100 万次以后，才将 1 这个结果返回。

## 幻读

```
CREATE TABLE `t` (
`id` int(11) NOT NULL, 
`c` int(11) DEFAULT NULL,
`d` int(11) DEFAULT NULL, 
PRIMARY KEY (`id`),
KEY `c` (`c`) )ENGINE=InnoDB;
insert into t values(0,0,0),(5,5,5),
10 (10,10,10),(15,15,15),(20,20,20),(25,25,25);
```

查询语句

```
begin;
select * from t where d=5 for update;
commit;
```

这个语句会命中 d=5 的这一行，对应的主键 id=5，因此在 select 语句 执行完成后，id=5 这一行会加一个写锁，而且由于两阶段锁协议，这个写锁会在执行 commit 语句的时候释放。

![CCYz1t](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/CCYz1t.png)

首先是语义上的。session A 在 T1 时刻就声明了，“我要把所有 d=5 的行锁住，不准别的事务进行读写操作”。而实际上，这个语义被破坏了。***其次，是数据一致性的问题。***

update 的加锁语义和 select ...for update 是一致的，所以这时候加上这条 update 语句 也很合理。session A 声明说“要给 d=5 的语句加上锁”，就是为了要更新数据，新加的这条 update 语句就是把它认为加上了锁的这一行的 d 值修改成了 100。

现在，我们来分析一下执行完成后，数据库里会是什么结果。

1. 经过 T1 时刻，id=5 这一行变成 (5,5,100)，当然这个结果最终是在 T6 时刻正式提交 的;

2. 经过 T2 时刻，id=0 这一行变成 (0,5,5);

3. 经过 T4 时刻，表里面多了一行 (1,5,5);

4. 其他行跟这个执行序列无关，保持不变。

这样看，这些数据也没啥问题，但是我们再来看看这时候 binlog 里面的内容。

1. T2 时刻，session B 事务提交，写入了两条语句;

2. T4 时刻，session C 事务提交，写入了两条语句;

3. T6 时刻，session A 事务提交，写入了 update t set d=100 where d=5 这条语句

好，你应该看出问题了。这个语句序列，不论是拿到备库去执行，还是以后用 binlog 来 克隆一个库，这三行的结果，都变成了 (0,5,100)、(1,5,100) 和 (5,5,100)。

现在你知道了，产生幻读的原因是，行锁只能锁住行，但是新插入记录这个动作，要更新 的是记录之间的“间隙”。因此，为了解决幻读问题，InnoDB 只好引入新的锁，也就是 间隙锁 (Gap Lock)。

顾名思义，间隙锁，锁的就是两个值之间的空隙。跟间隙锁存在冲突关系的，是“往这个间隙中插入一个记录”这个操 作。间隙锁之间都不存在冲突关系。间隙锁和行锁合称 next-key lock，每个 next-key lock 是前开后闭区间。

### 间隙锁和 next-key lock 的使用

我总结的加锁规则里面，包含了两个“原则”、两个“优化”和一个“bug”。

1. 原则 1:加锁的基本单位是 next-key lock。希望你还记得，next-key lock 是前开后闭 区间。

2. 原则 2:查找过程中访问到的对象才会加锁。

3. 优化 1:索引上的等值查询，给唯一索引加锁的时候，next-key lock 退化为行锁。

4. 优化 2:索引上的等值查询，向右遍历时且最后一个值不满足等值条件的时候，next-key lock 退化为间隙锁。

5. 一个 bug:唯一索引上的范围查询会访问到不满足条件的第一个值为止。

#### 等值查询间隙锁

![image-20200723221314148](/Users/guangzheng.li/Library/Application Support/typora-user-images/image-20200723221314148.png)

由于表 t 中没有 id=7 的记录，所以用我们上面提到的加锁规则判断一下的话:

1. 根据原则 1，加锁单位是 next-key lock，session A 加锁范围就是 (5,10];

2. 同时根据优化 2，这是一个等值查询 (id=7)，而 id=10 不满足查询条件，next-keylock 退化成间隙锁，因此最终加锁的范围是 (5,10)。

所以，session B 要往这个间隙里面插入 id=8 的记录会被锁住，但是 session C 修改 id=10 这行是可以的。

#### 非唯一索引等值锁

![KHRgqb](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/KHRgqb.png)

这里 session A 要给索引 c 上 c=5 的这一行加上读锁。

1. 根据原则 1，加锁单位是 next-key lock，因此会给 (0,5] 加上 next-key lock。

2. 要注意 c 是普通索引，因此仅访问 c=5 这一条记录是不能马上停下来的，需要向右遍历，查到 c=10 才放弃。根据原则 2，访问到的都要加锁，因此要给 (5,10] 加 next-key lock。

3. 但是同时这个符合优化 2:等值判断，向右遍历，最后一个值不满足 c=5 这个等值条件，因此退化成间隙锁 (5,10)。

4. 根据原则 2 ，只有访问到的对象才会加锁，这个查询使用覆盖索引，并不需要访问主键索引，所以主键索引上没有加任何锁，这就是为什么 session B 的 update 语句可以执 行完成。



其它还有对主键和其它案例，详细看MySQL实战21节。

## 短时间提高性能

### 短连接风暴

正常的短连接模式就是连接到数据库后，执行很少的 SQL 语句就断开，下次需要的时候再重连。如果使用的是短连接，在业务高峰期的时候，就可能出现连接数突然暴涨的情况。

MySQL 建立 连接的过程，成本是很高的。除了正常的网络连接三次握手外，还需要做登录权限判断和 获得这个连接的数据读写权限。

在数据库压力比较小的时候，这些额外的成本并不明显。

但是，短连接模型存在一个风险，就是一旦数据库处理得慢一些，连接数就会暴涨。 max_connections 参数，用来控制一个 MySQL 实例同时存在的连接数的上限，超过这 个值，系统就会拒绝接下来的连接请求，并报错提示“Too many connections”。对于 被拒绝连接的请求来说，从业务角度看就是数据库不可用。

在机器负载比较高的时候，处理现有请求的时间变长，每个连接保持的时间也更长。这 时，再有新建连接的话，就可能会超过 max_connections 的限制。

碰到这种情况时，一个比较自然的想法，就是调高 max_connections 的值。但这样做是 有风险的。因为设计 max_connections 这个参数的目的是想保护 MySQL，如果我们把 它改得太大，让更多的连接都可以进来，那么系统的负载可能会进一步加大，大量的资源 耗费在权限验证等逻辑上，结果可能是适得其反，已经连接的线程拿不到 CPU 资源去执行 业务的 SQL 请求。

第一种方法:先处理掉那些占着连接但是不工作的线程。

* 如果是连接数过多，你可以优先断开事务外空闲太久的连接;如果这样还不够，再考虑断开事务内空闲太久的连接。可能有损

第二种方法:减少连接过程的消耗。

* 跳过权限验证阶段。（不推荐）

### 慢查询性能问题

#### 索引没有设计好

比较理想的是能够在备库先执行。假设你现在的服务是一主一备，主库 A、备库 B，这个 方案的大致流程是这样的:

1. 在备库 B 上执行 set sql_log_bin=off，也就是不写 binlog，然后执行 alter table 语 句加上索引;

2. 执行主备切换;

3. 这时候主库是 B，备库是 A。在 A 上执行 set sql_log_bin=off，然后执行 alter table语句加上索引。

这是一个“古老”的 DDL 方案。平时在做变更的时候，你应该考虑类似 gh-ost 这样的方案

#### SQL 语句没写好

这时，我们可以通过改写 SQL 语句来处理。MySQL 5.7 提供了 query_rewrite 功能，可 以把输入的一种语句改写成另外一种模式。

#### MySQL 选错了索引

给原来的语句加上 force index 或其他方案