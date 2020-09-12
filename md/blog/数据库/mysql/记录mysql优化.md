# 数据库设计
## 需求分析
数据库优良设计：
A、减少数据冗余
B、避免数据维护异常
C、节约存储空间
D、高效的访问
*************************************************************************************
数据库设计的步骤：
A、需求分析（数据分析）
B、逻辑设计（ER图）
C、物理设计（数据库特点转换）
D、维护优化（需求建表、索引优化，大表拆分）

# 数据库结构优化
### 选择合适的数据类型
选择合适的数据类型

1. 使用可存下数据的最小的数据类型

2. 使用简单地数据类型，Int要比varchar类型在mysql处理上更简单

3. 尽可能使用not null定义字段，这是由innodb的特性决定的，

    ``因为非not null的数据可能需要一些额外的字段进行存储，这样就会增加一些IO。
    可以对非null的字段设置一个默认值``

4. 尽量少用text，非用不可最好分表，
     将text字段存放到另一张表中，在需要的时候再使用联合查询，这样可提高查询主表的效率

例子1、用Int存储日期时间

from_unixtime()可将Int类型的时间戳转换为时间格式
```/
select from_unixtime(1392178320); 输出为 2014-02-12 12:12:00
unix_timestamp()可将时间格式转换为Int类型
select unix_timestamp('2014-02-12 12:12:00'); 输出为1392178320
```
例子2
```/
存储IP地址——bigInt
利用inet_aton(),inet_ntoa()转换
select inet_aton('192.169.1.1'); 输出为3232301313
select inet_ntoa(3232301313); 输出为192.169.1.1
```

### 表的垂直拆分和水平拆分
表的垂直拆分的原则

所谓垂直拆分，就是把原来一个有很多列的表拆分成多个表解决表的宽度问题，通常拆分原则如下：
1. 把不常用的字段单独存放到一个表中
2. 把大字段独立存放到一个表中
3. 把经常一起使用的字段放到一起



垂直拆分:一个表的列太多,可以分为多个表

水平拆分:一个表中的数据太多,分多表结构不变

为了解决单表数据量过大的问题，每个水平拆分表的结构完全一致

方法
1. 哈希取模：hash(key) % N；
2. 范围：可以是 ID 范围也可以是时间范围；
3. 映射表：使用单独的一个数据库来存储映射关系

range 来分，好处在于说，扩容的时候很简单，因为你只要预备好，给每个月都准备一个库就可以了，到了一个新的月份的时候，自然而然，就会写新的库了；缺点，但是大部分的请求，都是访问最新的数据。实际生产用 range，要看场景。

hash 分发，好处在于说，可以平均分配每个库的数据量和请求压力；坏处在于说扩容起来比较麻烦，会有一个数据迁移的过程，之前的数据需要重新计算 hash 值重新分配到不同的库或表。


水平拆分之后的挑战

1. 跨分区进行数据查询

2. 统计及后台报表操作

前后台使用的表进行分开，前台要求查询效率，所以可以说会用拆分之后的表，后台在统计数据时可以使用汇总表

# mysql索引优化
### 如何选择合适的列简历索引
1. 在进行查询时，索引列不能是表达式的一部分，也不能是函数的参数，否则无法使用索引
2. 在需要使用多个列作为条件进行查询时，使用多列索引比使用多个单列索引性能更好。例如下面的语句中，最好把 actor_id 和 film_id 设置为多列索引
3. 让选择性最强的索引列放在前面。索引的选择性是指：不重复的索引值和记录总数的比值。最大值为 1，此时每个记录都有唯一的索引与其对应。选择性越高，查询效率也越高。（选择性对应的是下面的离散值）
4. 对于 BLOB、TEXT 和 VARCHAR 类型的列，必须使用前缀索引，只索引开始的部分字符。
5. 索引包含所有需要查询的字段的值。

选择合适的索引列
1. 在where，group by，order by，on从句中出现的列
2. 索引字段越小越好(因为数据库的存储单位是页，一页中能存下的数据越多越好 )
3. 离散度大得列放在联合索引前面

``select count(distinct customer_id), count(distinct staff_id) from payment;``
查看离散度 通过统计不同的列值来实现 count越大 离散程度越高

离散度，我的理解就是唯一性了，比如主键，绝对是离散度最大的，而一些用来标识状态标识的列，基本只有几个可选项，离散度就很小
### 索引维护的方法

通过统计信息库information_schma查找一些重复冗余的索引,
1. 通过查询统计信息表
2. 通过拿用第三方的统计工具pt-duplicate-key-checker(用户名,密码,数据库服务器ip),会给出一些优化建议.
   如果因为业务变更一些索引已经未使用Mysql当前只能使用慢查询日志配合pt-index-usage来进行index使用分析
3. 过多的索引不仅影响增加、修改、删除数据的效率，而且也影响查询的效率， 这是因为查询的时候数据库需要选择使用索引进行查询呢,那么需要更合理的使用索引(增加合适的索引、删除重复的索引)
4. 过多的索引不但影响写入，而且影响查询，索引越多，分析越慢如何找到重复和多余的索引，主键已经是索引了，所以primay key 的主键不用再设置unique唯一索引了 冗余索引，是指多个索引的前缀列相同，innodb会在每个索引后面自动加上主键信息
5. 冗余索引查询工具 pt-duplicate-key-checker

# mysql的sql语句优化
### mysql慢查日志
一 通过慢查日志记录带索引的sql语句  进行sql优化
1)查看mysql是否开启慢查询日志

	show variables like 'slow_query_log';

2)设置没有索引的记录到慢查询日志

	set global log_queries_not_using_indexes=on;

3)查看超过多长时间的sql进行记录到慢查询日志

	show variables like 'long_query_time'

4)开启慢查询日志

	set global slow_query_log=on

### mysql慢查日志分析工具
用mysql官方提供的日志分析工具查看慢日志

``mysqldumpslow -t 3 /home/mysql/data/mysql-slow.log | more``

linux系统下如果使用mysqldumpslow出现报错:
``-bash: mysqldumpslow: command not found的话,建立链接即可:   ln -s /usr/local/mysql/bin/mysqldumpslow /usr/bin``

mysqldumpslow 在windows中是一个perl文件，所以需要你配置perl环境变量并使用perl运行``mysqldumpslow.pl``
*****************
通过慢查日志发现问题
1. 查询次数多且每次查询占用时间长的SQL,通常为pt-query-digest分析的前几个查询
2. IO大的SQL,注意pt-query-digest分析中的Rows examine项
3. 未命中索引的SQL,注意pt-query-digest分析中的Rows examine和Row send 的对比

**********************
使用explain查询和分析语句

返回各列的含义
```
table：显示这一行的数据是关于哪张表的

type:这是重要的列,显示连接使用了何种类型。从最好到最差的连接类型为const、eq_reg、ref、range、index和ALL

possible_keys：显示可能应用在这张表中的索引。如果为空，没有可能的索引。

key:实际使用的索引。如果为NULL，则没有使用索引。

key_len:使用的索引的长度。在不损失精确性的情况下，长度越短越好。

ref:显示索引的哪一列被使用了,如果可能的话，是一个常数

rows：MYSQL认为必须检查的用来返回请求数据的行数
```
extra列需要注意的返回值

Using filesort:看到这个的时候，查询就需要优化了。MYSQL需要进行额外的步骤来发现如何对返回的行排序。它根据连接类型以及存储排序键值和匹配条件的全部行的行指针来排序全部行

Using temporary:看到这个的时候，查询需要优化了。这里，MYSQL徐哟创建一个临时表来存储接口，这通常发生在对不同的列表进行ORDER BY上，而不是GROUP BY上

************************************
Max()和Count()的优化

1. 对max()查询，可以为表创建索引，create index index_name on table_name(column_name 规定需要索引的列),然后在进行查询

2. count()对多个关键字进行查询，比如在一条SQL中同时查出2006年和2007年电影的数量，语句：
```
select count(release_year='2006' or null) as '2006年电影数量',
       count(release_year='2007' or null) as '2007年电影数量'
from film;
```
3. count(*) 查询的结果中，包含了该列值为null的结果

**************************************************
子查询一般优化成join的查询方式，同时需考虑关联键是否存在一对多的关系
如果存在一对多的关系，则可以使用distinct关键字去重.

*************************************************************************************************
limit常用于分页处理，时常会伴随order by从句使用，因此大多时候会使用Filesorts这样会造成大量的io问题
1. 使用有索引的列或主键进行order by操作
2. 记录上次返回的主键，在下次查询时使用主键过滤
   使用这种方式有一个限制，就是主键一定要顺序排序和连续的，如果主键出现空缺可能会导致最终页面上显示的列表不足5条，解决办法是附加一列，保证这一列是自增的并增加索引就可以了

# mysql开发技巧
DDL：数据定义语言 --- CREATE、ALTER、DROP、TRUNCATE
TPL：事务处理语言 --- COMMIT、ROLLBACK、SAVEPOINT、SET TRANSACTION
DCL：数据控制语言 --- GRANT、REVOKE
DML：数据操作语言 --- SELECT、UPDATE、INSERT、DELETE

算了，各种join和groupby什么的就不举例子了，写了一下，发现太啰嗦了。
基础的用法在这
https://github.com/CyC2018/CS-Notes/blob/master/docs/notes/SQL.md
想要掌握的话，还是去LeetCode里面练习吧
https://leetcode-cn.com/problemset/database/

参考资料：
https://github.com/CyC2018/CS-Notes/blob/master/docs/notes/MySQL.md