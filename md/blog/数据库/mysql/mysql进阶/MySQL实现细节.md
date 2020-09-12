# MySQL实现细节

[toc]

## delete

### 删除数据流程

InnoDB 里的数据都是用 B+ 树的结构组织的。ID为主键的聚簇索引。如果我们用 delete 命令把整个表的数据删除呢?结果就是，所有的数据页都 会被标记为可复用。但是磁盘上，文件不会变小。

你现在知道了，delete 命令其实只是把记录的位置，或者数据页标记为了“可复用”，但 磁盘文件的大小是不会变的。也就是说，通过 delete 命令是不能回收表空间的。这些可 以复用，而没有被使用的空间，看起来就像是“空洞”。

实际上，不止是删除数据会造成空洞，插入数据也会。 如果数据是按照索引递增顺序插入的，那么索引是紧凑的。但如果数据是随机插入的，就可能造成索引的数据页分裂。

### 重建表

可以使用 alter table A engine=InnoDB 命令来重建表。

MySQL 5.6 版本开始引入的 Online DDL，对这个操作流程做了优化。 我给你简单描述一下引入了 Online DDL 之后，重建表的流程:

1. 建立一个临时文件，扫描表 A 主键的所有数据页;

2. 用数据页中表 A 的记录生成 B+ 树，存储到临时文件中;

3. 生成临时文件的过程中，将所有对 A 的操作记录在一个日志文件(row log)中，对应的是图中 state2 的状态;

4. 临时文件生成后，将日志文件中的操作应用到临时文件，得到一个逻辑数据上与表 A 相同的数据文件，对应的就是图中 state3 的状态;

5. 用临时文件替换表 A 的数据文件。

由于日志文件记录和重放操作这个功能的存在， 这个方案在重建表的过程中，允许对表 A 做增删改操作。这也就是 Online DDL 名字的来源。

对于很大的 表来说，这个操作是很消耗 IO 和 CPU 资源的。因此，如果是线上服务，你要很小心地控 制操作时间。如果想要比较安全的操作的话，我推荐你使用 GitHub 开源的 gh-ost 来 做。

## count(*)

在不同的 MySQL 引擎中，count(*) 有不同的实现方式。

MyISAM 引擎把一个表的总行数存在了磁盘上，因此执行 count(*) 的时候会直接返回 这个数，效率很高;

而 InnoDB 引擎就麻烦了，它执行 count(*) 的时候，需要把数据一行一行地从引擎里面 读出来，然后累积计数。

这和 InnoDB 的事务设计有关系，可重复读是它默认的隔离级别，在代码上就是通过多版 本并发控制，也就是 MVCC 来实现的。每一行记录都要判断自己是否对这个会话可见，因 此对于 count(*) 请求来说，InnoDB 只好把数据一行一行地读出依次判断，可见的行才能 够用于计算“基于这个查询”的表的总行数。

你知道的，InnoDB 是索引组织表，主键索引树的叶子节点是数据，而普通索引树的叶子 节点是主键值。所以，普通索引树比主键索引树小很多。对于 count(*) 这样的操作，遍历 哪个索引树得到的结果逻辑上都是一样的。因此，MySQL 优化器会找到最小的那棵树来 遍历。在保证逻辑正确的前提下，尽量减少扫描的数据量，是数据库系统设计的通用法则 之一。

假设我创建一张表

```
CREATE TABLE `metrics_class`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `package_id` bigint(20) NOT NULL,
  `class_name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_package_id` (`package_id`)
) ENGINE=InnoDB;
```

```
EXPLAIN SELECT COUNT(1) FROM metrics_class;
```

会发现命中了 `idx_package_id` 这个索引，这是因为这个索引数比主键索引的聚簇索引要小，mysql会遍历这棵树。

### count(*) count(1) count(id) count(field)

对于 count(主键 id) 来说，InnoDB 引擎会遍历整张表，把每一行的 id 值都取出来，返 回给 server 层。server 层拿到 id 后，判断是不可能为空的，就按行累加。

对于 count(1) 来说，InnoDB 引擎遍历整张表，但不取值。server 层对于返回的每一 行，放一个数字“1”进去，判断是不可能为空的，按行累加。

单看这两个用法的差别的话，你能对比出来，count(1) 执行得要比 count(主键 id) 快。因 为从引擎返回 id 会涉及到解析数据行，以及拷贝字段值的操作。

对于 count(字段) 来说:

1. 如果这个“字段”是定义为 not null 的话，一行行地从记录里面读出这个字段，判断不 能为 null，按行累加;

2. 如果这个“字段”定义允许为 null，那么执行的时候，判断到有可能是 null，还要把值 取出来再判断一下，不是 null 才累加

但是 count(*) 是例外，并不会把全部字段取出来，而是专门做了优化，不取值。 count(*) 肯定不是 null，按行累加。

count(字段)<count(主键 id)<count(1)≈count(*)

## order

创建表

```
CREATE TABLE `t` (
`id` int(11) NOT NULL,
`city` varchar(16) NOT NULL,
`name` varchar(16) NOT NULL,
`age` int(11) NOT NULL,
`addr` varchar(128) DEFAULT NULL, 
PRIMARY KEY (`id`),
KEY `city` (`city`)
)ENGINE=InnoDB;
```

### 全字段排序

查询

```
select city,name,age from t where city='杭州' order by name limit 1000 ;
```

为避免全表扫描，我们需要在 city 字段加 上索引。在 city 字段上创建索引之后，我们用 explain 命令来看看这个语句的执行情况。

![GbuWgz](https://gitee.com/Esmusssein/picture/raw/master/uPic/GbuWgz.png)

Extra 这个字段中的“Using filesort”表示的就是需要排序，MySQL 会给每个线程分配 一块内存用于排序，称为 sort_buffer。

这个语句执行流程如下所示 :

1. 初始化 sort_buffer，确定放入 name、city、age 这三个字段;

2. 从索引 city 找到第一个满足 city='杭州’条件的主键 id，也就是图中的 ID_X;

3. 到主键 id 索引取出整行，取 name、city、age 三个字段的值，存入 sort_buffer 中;

4. 从索引 city 取下一个记录的主键 id;

5. 重复步骤 3、4 直到 city 的值不满足查询条件为止，对应的主键 id 也就是图中的ID_Y;

6. 对 sort_buffer 中的数据按照字段 name 做快速排序;

7. 按照排序结果取前 1000 行返回给客户端。

我们暂且把这个排序过程，称为全字段排序。步骤6中“按 name 排序”这个动作，可能在内存中完成，也可能需要使用外部排序，这取决 于排序所需的内存和参数 sort_buffer_size。

sort_buffer_size，就是 MySQL 为排序开辟的内存(sort_buffer)的大小。如果要排序 的数据量小于 sort_buffer_size，排序就在内存中完成。但如果排序数据量太大，内存放不下，则不得不利用磁盘临时文件辅助排序。内存放不下时，就需要使用外部排序，外部排序一般使用归并排序算法。 可以这么简单理解，MySQL 将需要排序的数据分成 12 份，每一份单独排序后存在这些临 时文件中。然后把这 12 个有序文件再合并成一个有序的大文件。

### rowed 排序

在上面这个算法过程里面，只对原表的数据读了一遍，剩下的操作都是在 sort_buffer 和 临时文件中执行的。但这个算法有一个问题，就是如果查询要返回的字段很多的话，那么 sort_buffer 里面要放的字段数太多，这样内存里能够同时放下的行数很少，要分成很多个 临时文件，排序的性能会很差。

所以如果单行很大，这个方法效率不够好。那么，如果 MySQL 认为排序的单行长度太大会怎么做呢?

`SET max_length_for_sort_data = 16;`

命令表示单行的长度超过这个16，MySQL 就认为单行太大，要换一个算法，即rowid排序。

新的算法放入 sort_buffer 的字段，只有要排序的列(即 name 字段)和主键 id。

但这时，排序的结果就因为少了 city 和 age 字段的值，不能直接返回了，整个执行流程 就变成如下所示的样子:

1. 初始化 sort_buffer，确定放入两个字段，即 name 和 id;

2. 从索引 city 找到第一个满足 city='杭州’条件的主键 id，也就是图中的 ID_X;

3. 到主键 id 索引取出整行，取 name、id 这两个字段，存入 sort_buffer 中;

4. 从索引 city 取下一个记录的主键 id;

5. 重复步骤 3、4 直到不满足 city='杭州’条件为止，也就是图中的 ID_Y;

6. 对 sort_buffer 中的数据按照字段 name 进行排序;

7. 遍历排序结果，取前 1000 行，并按照 id 的值回到原表中取出 city、name 和 age 三个字段返回。

### 优化

其实，并不是所有的 order by 语句，都需要排序操作的。从上面分析的执行过程，我们 可以看到，MySQL 之所以需要生成临时表，并且在临时表上做排序操作，其原因是原来 的数据都是无序的。

你可以设想下，如果能够保证从 city 这个索引上取出来的行，天然就是按照 name 递增排 序的话，是不是就可以不用再排序了呢?

所以，我们可以在这个市民表上创建一个 city 和 name 的联合索引，对应的 SQL 语句 是:

`alter table t add index city_user(city, name);`

然后步骤就变为

1. 从索引 (city,name) 找到第一个满足 city='杭州’条件的主键 id;

2. 到主键 id 索引取出整行，取 name、city、age 三个字段的值，作为结果集的一部分直接返回;

3. 从索引 (city,name) 取下一个记录主键 id;

4. 重复步骤 2、3，直到查到第 1000 条记录，或者是不满足 city='杭州’条件时循环结束。

用explain会发现 Extra 字段中没有了 Using filesort了。不需要排序。这个查询也不用把 4000 行全都读一遍，只要找 到满足条件的前 1000 条记录就可以退出了。也就是说，在我们这个例子里，只需要扫描 1000 次。

如果你还想要优化，不想要回表的话，可以用覆盖索引

```
alter table t add index city_user_age(city, name, age);
```

不过这个就要自己去考虑，毕竟索引还是有维护代价的。这是一个需要权衡的决定。

### order by range

我在这个表里面插入了 10000 行记录。接下来，我们就一起看看要随 机选择 3 个单词，最简单的就是`select word from words order by rand() limit 3;`这条语句的执行流程是这样的:

1. 创建一个临时表。这个临时表使用的是 memory 引擎，表里有两个字段，第一个字段 是 double 类型，为了后面描述方便，记为字段 R，第二个字段是 varchar(64) 类型，记为字段 W。并且，这个表没有建索引。

2. 从 words 表中，按主键顺序取出所有的 word 值。对于每一个 word 值，调用 rand()函数生成一个大于 0 小于 1 的随机小数，并把这个随机小数和 word 分别存入临时表的R 和 W 字段中，到此，扫描行数是 10000。

3. 现在临时表有 10000 行数据了，接下来你要在这个没有索引的内存临时表上，按照字段 R 排序。

4. 初始化 sort_buffer。sort_buffer 中有两个字段，一个是 double 类型，另一个是整型。

5. 从内存临时表中一行一行地取出 R 值和位置信息(我后面会和你解释这里为什么是“位置信息”)，分别存入 sort_buffer 中的两个字段里。这个过程要对内存临时表做全表扫描，此时扫描行数增加 10000，变成了 20000。

6. 在 sort_buffer 中根据 R 的值进行排序。注意，这个过程没有涉及到表操作，所以不会增加扫描行数。

7. 排序完成后，取出前三个结果的位置信息，依次到内存临时表中取出 word 值，返回给客户端。这个过程中，访问了表的三行数据，总扫描行数变成了 20003。

接下来，我们通过慢查询日志(slow log)来验证一下我们分析得到的扫描行数是否正确。

```
# Query_time: 0.900376 Lock_time: 0.000347 Rows_sent: 3 Rows_examined: 20003
SET timestamp=1541402277;
select word from words order by rand() limit 3;
```

其中，Rows_examined:20003 就表示这个语句执行过程中扫描了 20003 行，也就验证了我们分析得出的结论。

#### 优化

1. 取得整个表的行数，记为 C;

2. 根据相同的随机方法得到 Y1、Y2、Y3; 

3. 再执行三个 limit Y, 1 语句得到三行数据。

```
1 mysql> select count(*) into @C from t;
2 set @Y1 = floor(@C * rand());
3 set @Y2 = floor(@C * rand());
4 set @Y3 = floor(@C * rand());
5 select * from t limit @Y1，1; // 在应用代码里面取 Y1、Y2、Y3 值，拼出 SQL 后执行
6 select * from t limit @Y2，1;
7 select * from t limit @Y3，1;
```

MySQL 处理 limit Y,1 的做法就是按顺序一个一个地读出来，丢掉前 Y 个，然后把下一个 记录作为返回结果，因此这一步需要扫描 Y+1 行。再加上，第一步扫描的 C 行，总共需 要扫描 C+Y+1 行，执行代价比随机算法 1 的代价要高。

当然=跟直接 order by rand() 比起来，执行代价还是小很多的。

你可能问了，如果按照这个表有 10000 行来计算的话，C=10000，要是随机到比较大的 Y 值，那扫描行数也跟 20000 差不多了，接近 order by rand() 的扫描行数

取 Y1、Y2 和 Y3 里面最大的一个数，记为 M，最小的一个数记为 N，然后执行下面这条 SQL 语句:

```
mysql> select * from t limit N, M-N+1;
```

再加上取整个表总行数的 C 行，这个方案的扫描行数总共只需要 C+M+1 行。

当然也可以先取回 id 值，在应用中确定了三个 id 值以后，再执行三次 where id=X 的语 句也是可以的。

## fun(index)

对索引字段做函数操作，可能会破坏索引值的有序性，因此优化器就决定放弃走树搜索功能.

### 一、条件字段函数操作

```
CREATE TABLE `tradelog` (
`id` int(11) NOT NULL,
`tradeid` varchar(32) DEFAULT NULL, 
`operator` int(11) DEFAULT NULL, 
`t_modified` datetime DEFAULT NULL,
PRIMARY KEY (`id`),
KEY `tradeid` (`tradeid`),
KEY `t_modified` (`t_modified`)
ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

查询7月份的数据

```
select count(*) from tradelog where month(t_modified)=7;
```

下面是这个 t_modified 索引的示意图。方框上面的数字就是 month() 函数对应的值。

![3P8fGa](https://gitee.com/Esmusssein/picture/raw/master/uPic/3P8fGa.png)

如果你的 SQL 语句条件用的是 where t_modified='2018-7-1’的话，引擎就会按照上面 绿色箭头的路线，快速定位到 t_modified='2018-7-1’需要的结果。

实际上，B+ 树提供的这个快速定位能力，来源于同一层兄弟节点的有序性。

但是，如果计算 month() 函数的话，你会看到传入 7 的时候，在树的第一层就不知道该怎 么办了。也就是说，对索引字段做函数操作，可能会破坏索引值的有序性，因此优化器就决定放弃 走树搜索功能。

在这个例子里，放弃了树搜索功能，优化器可以选择遍历主键索引，也可以选择遍历索引 t_modified，优化器对比索引大小后发现，索引 t_modified 更小，遍历这个索引比遍历 主键索引来得更快。因此最终还是会选择索引 t_modified。

### 二、隐式类型转换

我们一起看一下这条 SQL 语句:

```
mysql> select * from tradelog where tradeid=110717
```

交易编号 tradeid 这个字段上，本来就有索引，但是 explain 的结果却显示，这条语句需 要走全表扫描。你可能也发现了，tradeid 的字段类型是 varchar(32)，而输入的参数却是 整型，所以需要做类型转换

对于优化器来说，这个语句相当于:

```
mysql> select * from tradelog where CAST(tradid AS signed int) = 110717;
```

### 三、隐式字符编码转换

两个表的字符集不同，一个是 utf8， 一个是 utf8mb4，做表连接查询的时候用不上关联字段的索引。

```
select * from trade_detail where CONVERT(traideid USING utf8mb4)=$L2.tradeid.value;
```

CONVERT() 函数，在这里的意思是把输入的字符串转成 utf8mb4 字符集。

这就再次触发了我们上面说到的原则:对索引字段做函数操作，优化器会放弃走树搜索功能。

## Join



## kill

kill 并不是马上停止的意思，而是告诉执行线程说，这条语句已经不需要继续 执行了，可以开始“执行停止的逻辑了”。

其实，这跟 Linux 的 kill 命令类似，kill -N pid 并不是让进程直接停止，而 是给进程发一个信号，然后进程处理这个信号，进入终止逻辑。

kill 无效的第一类情况，即:线程没有执行到判断线程状态的逻辑。

另一类情况是，终止逻辑耗时较长。这时候，从 show processlist 结果上看也是 Command=Killed，需要等到终止逻辑完成，语句才算真正完成。

### kill query thread_id

当用户执行 kill query thread_id_B 时，MySQL 里处理 kill 命令的线程做了两 件事:

1. 把 session B 的运行状态改成 THD::KILL_QUERY (将变量 killed 赋值为 THD::KILL_QUERY) ;

2. 给 session B 的执行线程发一个信号。

session B 处于锁等待状态，如果只是把 session B 的线程 状态设置 THD::KILL_QUERY，线程 B 并不知道这个状态变化，还是会继续等待。发一个 信号的目的，就是让 session B 退出等待，来处理这个THD::KILL_QUERY 状态。

上面的分析中，隐含了这么三层意思:

1. 一个语句执行过程中有多处“埋点”，在这些“埋点”的地方判断线程状态，如果发现线程状态是 THD::KILL_QUERY，才开始进入语句终止逻辑;

2. 如果处于等待状态，必须是一个可以被唤醒的等待，否则根本不会执行到“埋点”处;

3. 语句从开始进入终止逻辑，到终止逻辑完全完成，是有一个过程的。

### kill connection 

而当 session E 执行 kill connection C 命令时，是这么做的，

1. 把 C 线程状态设置为 KILL_CONNECTION;

2. 关掉 C 线程的网络连接。因为有这个操作，所以你会看到，这时候 session C 收到了断开连接的提示。

但是客户端退出了，这个线程的状态仍然是在等待中。那这个线程什么时候会退出呢?

答案是，只有等到满足进入 InnoDB 的条件后，session C 的查询语句继续执行，然后才有可能判断到线程状态已经变成了 KILL_QUERY 或者 KILL_CONNECTION，再进入终止逻辑阶段。