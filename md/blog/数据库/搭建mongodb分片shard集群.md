MongoDB是一个高性能，开源，无模式的文档型数据库，开发语言是C ++。它在许多场景下可用于替代传统的关系型数据库或键/值存储方式。

优点：

1.面向集合存储，易存储对象类型的数据。

2.支持动态查询。

3.支持完全索引，包含内部对象。

4.使用高效的二进制数据存储，包括大型对象（如视频等）。

缺点：

\1. mongodb不支持事务操作。

\2. mongodb占用空间过大。（在集群分片中的数据分布不均匀）

3.大数据量持续插入，写入性能有较大波动

4.单机可靠性比较差

**适用场景****：**

1.适用于实时的插入，更新与查询的需求，并具备应用程序实时数据存储所需的复制及高度伸缩性;

2.非常适合文档化格式的存储及查询;

3.高伸缩性的场景：MongoDB非常适合由数十或者数百台服务器组成的数据库。

4.对性能的关注超过对功能的要求。



### 搭建MongoDB的需要的了解的概念

我们先了解什么是复制集

**replica set（复制集）**

![img](https://img-blog.csdn.net/20181008165017135?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM5MDcxNTMw/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

最基础的复制集架构是由三个成员组成的，且成员个数应该是奇数，包括一个主节点（主），一个从节点（二次），还有一个投票节点（仲裁器）。投票节点的作用仅仅是在选举过程中参与投票，该节点中并不包含数据集，故所需资源很少，无需一个专用物理机。主节点和从节点的数据相同，用以备份数据，当主机宕机时，仲裁节点将投票，选出一个从节点做主节点。

**碎片（分片）**

我们再来看一张图

![img](https://img-blog.csdn.net/20181008164131561?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM5MDcxNTMw/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

从图中可以看到有四个组件：mongos，config server，shard，replica set。

**mongos**，集群请求的入口，分发读写请求到分片服务器，前端应用透明访问。

**配置服务器**，存储所有数据库元信息（路由，分片）的配置，mongos从配置服务器加载配置信息。生产环境中，为确保冗余与安全，一般使用3台配置服务器，且必须部署在不同的机器上。

**shard**，分片是存储了集群一部分数据的mongod或者replica set，所有分片存储组成了集群的全部数据。在生产环境中，为保证高可用的分片架构，至少要保证2个分片， -个分片都应该是一个复制集。复制集为每个分片的数据提供了冗余和高可靠性。



了解之后开始搭建，我们首先来简单的架构，首先确定各个组件的数量，mongos 3个，配置服务器3个，数据分3片shard server 3个，每个碎片有一个副本一个仲裁也就是3 * 2 = 6个，总共需要部署15个实例。

![âmongodbåçâçå¾çæç'¢ç»æ](https://imgconvert.csdnimg.cn/aHR0cHM6Ly9pbWFnZXMyMDE1LmNuYmxvZ3MuY29tL2Jsb2cvNTQ1NjU3LzIwMTUwOS81NDU2NTctMjAxNTA5MjMyMTQ2NTgzMDMtMTIzMTU2MDU0LnBuZw)![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

1，这里准备了三个IP，分别是：172.16.2.245,172.16.2.161,172.16.7.52。

2，安装了wget

```
yum -y install wget
```

![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

3，在分别**每台机器上**建立mongodb的分片对应测试文件夹。这里是三台机器都创建

  创建MongoDB的数据文件

```
mkdir -p /data/mongodbtest
```

![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

  进入MongoDB的文件夹 

```
cd  /data/mongodbtest
```

![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

 下载

```
wget http://fastdl.mongodb.org/linux/mongodb-linux-x86_64-2.4.8.tgz
```

![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

  解压下载的压缩包

```
tar xvzf mongodb-linux-x86_64-2.4.8.tgz
```

![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

  建立mongos，config，shard1，shard2，shard3五个目录

```
#建立mongos目录
mkdir -p /data/mongodbtest/mongos/log

#建立config server 数据文件存放目录
mkdir -p /data/mongodbtest/config/data

#建立config server 日志文件存放目录
mkdir -p /data/mongodbtest/config/log

#建立config server 日志文件存放目录
mkdir -p /data/mongodbtest/mongos/log

#建立shard1 数据文件存放目录
mkdir -p /data/mongodbtest/shard1/data

#建立shard1 日志文件存放目录
mkdir -p /data/mongodbtest/shard1/log

#建立shard2 数据文件存放目录
mkdir -p /data/mongodbtest/shard2/data

#建立shard2 日志文件存放目录
mkdir -p /data/mongodbtest/shard2/log

#建立shard3 数据文件存放目录
mkdir -p /data/mongodbtest/shard3/data

#建立shard3 日志文件存放目录
mkdir -p /data/mongodbtest/shard3/log
```

![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

我们这里使用mobaxterm给大家看一看结构

![img](https://img-blog.csdn.net/20181009185243947?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM5MDcxNTMw/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)



4，规划5个组件对应的端口号，由于一个机器需要同时部署mongos，配置服务器，shard1，shard2，shard3，所以需要用端口进行区分。
这个端口可以自由定义，在本文mongos为20000，配置服务器为21000，shard1为22001，shard2为22002，shard3为22003。

5，在每一台服务器分别启动配置服务器。

```
/data/mongodbtest/mongodb-linux-x86_64-2.4.8/bin/mongod --configsvr --dbpath /data/mongodbtest/config/data --port 21000 --logpath /data/mongodbtest/config/log/config.log --fork
```

![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

如图6所示，在每一台服务器分别启动mongos服务器。

```
/data/mongodbtest/mongodb-linux-x86_64-2.4.8/bin/mongos  --configdb 172.16.2.245:21000,172.16.2.161:21000,172.16.7.52:21000  --port 20000   --logpath  /data/mongodbtest/mongos/log/mongos.log --fork
```

![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

如图7所示，配置各个分片的副本集。

在每个机器里分别设置分片1服务器及副本集shard1

```
/data/mongodbtest/mongodb-linux-x86_64-2.4.8/bin/mongod --shardsvr --replSet shard1 --port 22001 --dbpath /data/mongodbtest/shard1/data  --logpath /data/mongodbtest/shard1/log/shard1.log --fork --nojournal  --oplogSize 10
```

![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

在每个机器里分别设置分片2服务器及副本集shard2

```
/data/mongodbtest/mongodb-linux-x86_64-2.4.8/bin/mongod --shardsvr --replSet shard2 --port 22002 --dbpath /data/mongodbtest/shard2/data  --logpath /data/mongodbtest/shard2/log/shard2.log --fork --nojournal  --oplogSize 10
```

![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

在每个机器里分别设置分片3服务器及副本集shard3

```
/data/mongodbtest/mongodb-linux-x86_64-2.4.8/bin/mongod --shardsvr --replSet shard3 --port 22003 --dbpath /data/mongodbtest/shard3/data  --logpath /data/mongodbtest/shard3/log/shard3.log --fork --nojournal  --oplogSize 10
```

![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

如图8所示，分别对每个分片配置副本集

登录某一个IP，注意不要登录用来投票的仲裁服务器，因为仲裁服务器不能设置副本集。

我们这里登录的是172.16.2.245

```
#设置第一个分片副本集
/data/mongodbtest/mongodb-linux-x86_64-2.4.8/bin/mongo  127.0.0.1:22001

#使用admin数据库
use admin

#定义副本集配置
config = { _id:"shard1", members:[
                     {_id:0,host:"172.16.2.245:22001"},
                     {_id:1,host:"172.16.2.161:22001"},
                     {_id:2,host:"172.16.7.52:22001",arbiterOnly:true}
                ]
         }

#初始化副本集配置
rs.initiate(config);

#退出
exit

#设置第二个分片副本集
/data/mongodbtest/mongodb-linux-x86_64-2.4.8/bin/mongo  127.0.0.1:22002

#使用admin数据库
use admin

#定义副本集配置
config = { _id:"shard2", members:[
                     {_id:0,host:"172.16.2.245:22002"},
                     {_id:1,host:"172.16.2.161:22002"},
                     {_id:2,host:"172.16.7.52:22002",arbiterOnly:true}
                ]
         }

#初始化副本集配置
rs.initiate(config);

#退出
exit

#设置第三个分片副本集
/data/mongodbtest/mongodb-linux-x86_64-2.4.8/bin/mongo    127.0.0.1:22003

#使用admin数据库
use admin

#定义副本集配置
config = { _id:"shard3", members:[
                     {_id:0,host:"172.16.2.245:22003"},
                     {_id:1,host:"172.16.2.161:22003"},
                     {_id:2,host:"172.16.7.52:22003",arbiterOnly:true}
                ]
         }

#初始化副本集配置
rs.initiate(config);
```

![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

以上的操作都在172.16.2.245机器上完成，将172.16.7.52设置为投票节点。

注意：本机配置的碎片时，不能把本机设置为“仲裁者”，否则会报错，要去必须节点其他设置
在配置shard1，shard2时都是在节点1上配置的，因为仲裁节点分别是节点3，节点2。当节点1为仲裁节点时，必须
要去节点2或者是节点3上去配置



9，目前搭建了mongodb配置服务器，路由服务器，各个分片服务器，不过应用程序连接到mongos路由服务器并不能使用分片机制，还需要在程序里设置分片配置，让分片生效

```
#连接到mongos
/data/mongodbtest/mongodb-linux-x86_64-2.4.8/bin/mongo  127.0.0.1:20000

#使用admin数据库
use  admin
```

![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

 串联路由服务器与分配副本集1

```
db.runCommand( { addshard : "shard1/172.16.2.245:22001,172.16.2.161:22001,172.16.7.52:22001"});
```

![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

串联路由服务器与分配副本集2

```
db.runCommand( { addshard : "shard2/172.16.2.245:22002,172.16.2.161:22002,172.16.7.52:22002"});
```

![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

串联路由服务器与分配副本集3

```
db.runCommand( { addshard : "shard3/172.16.2.245:22003,172.16.2.161:22003,172.16.7.52:22003"});
```

![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

查看分片服务器的配置

```
db.runCommand( { listshards : 1 } );
```

![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

 可以看到

![img](https://img-blog.csdn.net/20181009190013123?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM5MDcxNTMw/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

因为172.16.7.52是每个分片副本集的仲裁节点，所以在上面结果没有列出来。

10，目前配置服务，路由服务，分片服务，副本集服务都已经串联起来了，但我们的目的是希望插入数据，数据能够自动分片，我们添加分片键

首先指定一个数据库，我们假设是mongotest数据库，集合为用户。

指定mongotest分片生效

```
db.runCommand( { enablesharding :"mongotest"});
```

![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

指定数据库里需要分片的集合和片键

```
db.runCommand( { shardcollection : "mongotest.user",key : {id: 1} } )
```

![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

效果是

![img](https://img-blog.csdn.net/20181009190627760?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM5MDcxNTMw/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

 我们首先退出退出，如果你没有令三台机器的时间同步的话，那么你插入假设10万条数据，你会发现全部插入到了一个shard里面。

就像这样

![img](https://img-blog.csdn.net/20181009191322212?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM5MDcxNTMw/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

我们还要做一步的是使三台机器的时间同步。

```
安装ntpdate
 # yum install -y ntpdate

时间同步
 # ntpdate pool.ntp.org
```

![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

这里为了展示结果，给大家展示一下之前成功过的分片

```
/data/mongodbtest/mongodb-linux-x86_64-2.4.8/bin/mongo  127.0.0.1:20000
use admin
db.runCommand( { enablesharding :"testdb"});
db.runCommand( { shardcollection : "testdb.table1",key : {id: 1} } )
```

![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

再使用

```
#连接mongos服务器
/data/mongodbtest/mongodb-linux-x86_64-2.4.8/bin/mongo  127.0.0.1:20000

#使用testdb
use  testdb;

#插入数据
for (var i = 1; i <= 100000; i++){db.table1.save({id:i,"test1":"testval1"})}
```

![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

最后通过

```
db.user.stats();
```

![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

可以查看成功的分片。

![img](https://img-blog.csdn.net/20181015174323499?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM5MDcxNTMw/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

sh.status（）

![img](https://img-blog.csdn.net/20181015174358104?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM5MDcxNTMw/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

导出和导入

导出
[root@mongodb114 bin]# ./mongoexport -d allinfo -c generalInfo -o generalInfo.dat -h 172.16.2.211:20000

导入
./mongoimport -d allinfo -c generalInfo generalInfo.dat -h 172.16.2.211:20000