# Kafka实践

下载镜像

```
docker pull wurstmeister/zookeeper  
docker pull wurstmeister/kafka  
```

启动zookeeper容器

```
docker run -d --name zookeeper -p 2181:2181 -t wurstmeister/zookeeper	
```

启动kafka容器

```
docker run  -d --name kafka -p 9092:9092 -e KAFKA_BROKER_ID=0 -e KAFKA_ZOOKEEPER_CONNECT=192.168.0.100:2181 -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://192.168.0.100:9092 -e KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092 -t wurstmeister/kafka
```

这里面主要设置了4个参数

KAFKA_BROKER_ID=0
KAFKA_ZOOKEEPER_CONNECT=192.168.0.100:2181
KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://192.168.0.100:9092
KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092
中间两个参数的192.168.0.100改为宿主机器的IP地址，如果不这么设置，可能会导致在别的机器上访问不到kafka。



测试kafka 进入kafka容器的命令行

```
docker exec -it kafka /bin/bash
```

进入kafka所在目录

```
cd opt/kafka_2.11-2.0.0/
```

启动消息发送方

```
./bin/kafka-console-producer.sh --broker-list localhost:9092 --topic mytopic
```

新建一个窗口，克隆会话，进入kafka所在目录

```
cd opt/kafka_2.11-2.0.0/
```

启动消息接收方

```
./bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic mytopic --from-beginning
```

在消息发送方输入`hello world!`
在消息接收方查看
如果看到`hello world!`消息发送完成



删除 topic

```
./bin/kafka-topics.sh --delete --zookeeper 192.168.0.100:2181 --topic test-topic
```

如果 kafka 启动时加载的配置文件中 server.properties 没有配置delete.topic.enable=true，那么此时的删除并不是真正的删除，而是把 topic 标记为：marked for deletion

​	

查看所有 topic

```
./bin/kafka-topics.sh --zookeeper 192.168.0.100:2181 --list 
```



### 集群搭建
使用docker命令可快速在同一台机器搭建多个kafka，只需要改变brokerId和端口

```
docker run -d --name kafka1 -p 9093:9093 -e KAFKA_BROKER_ID=1 -e KAFKA_ZOOKEEPER_CONNECT=192.168.0.100:2181 -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://192.168.0.100:9093 -e KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9093 -t wurstmeister/kafka
```

创建Replication为2，Partition为2的topic 在kafka容器中的opt/kafka_2.12-1.1.0/目录下输入

```
./bin/kafka-topics.sh --create --zookeeper 192.168.0.100:2181 --replication-factor 2 --partitions 2 --topic partiton-topic
```

查看topic的状态 在kafka容器中的opt/kafka_2.12-1.1.0/目录下输入

```
./bin/kafka-topics.sh --describe --zookeeper 192.168.0.100:2181 --topic partiton-topic
```

输出结果：

![](https://gitee.com/Esmusssein/picture/raw/master/uPic/Dhf2zK.png)

```
Topic:partiton-topic  PartitionCount:2    ReplicationFactor:2 Configs:
    Topic: partiton-topic Partition: 0    Leader: 1   Replicas: 1,0   Isr: 1,0
    Topic: partiton-topic Partition: 1    Leader: 0   Replicas: 1,1   Isr: 0,1
```



**Zoolytic**

IDEA上有插件名字叫做Zookeeper-tool，可以看到zk下面的Kafka注册情况

<img src="https://gitee.com/Esmusssein/picture/raw/master/uPic/wE4bYT.png" alt="zooker" style="zoom:50%;" />