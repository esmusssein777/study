### 安装ELK

首先针对不同的系统给设置最小的内存值

就算使用了Docker容器，`elasticsearch`仍然不像普通镜像那么简单启动，es对虚拟内存敏感，因此服务器必须是内核虚拟化`KVM`架构，不支持`OpenVZ`虚拟，参考官方说明

> #### Production mode
>
> The `vm.max_map_count` kernel setting needs to be set to at least `262144` for production use. Depending on your platform:
>
> - Linux
>
>   The `vm.max_map_count` setting should be set permanently in /etc/sysctl.conf:
>
>   ```
>   $ grep vm.max_map_count /etc/sysctl.conf
>   vm.max_map_count=262144
>   ```
>
>   To apply the setting on a live system type: `sysctl -w vm.max_map_count=262144`
>
> - macOS with [Docker for Mac](https://docs.docker.com/engine/installation/mac/#/docker-for-mac)
>
>   The `vm.max_map_count` setting must be set within the xhyve virtual machine:
>
>   ```
>   $ screen ~/Library/Containers/com.docker.docker/Data/vms/0/tty
>   ```
>
>   Just press enter and configure the `sysctl` setting as you would for Linux:
>
>   ```
>   sysctl -w vm.max_map_count=262144
>   ```
>
>   接着退出按 Control+A和Control+\




接下来我们来安装ELK

```
docker pull sebp/elk 
```

执行命令,将镜像运行为容器，为了保证ELK能够正常运行，加了-e参数限制使用最小内存及最大内存。

```
docker run -p 5601:5601 -p 9200:9200 -p 9300:9300 -p 5044:5044 -e ES_MIN_MEM=128m  -e ES_MAX_MEM=2048m -it --name elk sebp/elk 
```

启动容器后：

1、使用命令：`docker exec -it  /bin/bash 进入容器内`

2、执行命令：`/opt/logstash/bin/logstash -e 'input { stdin { } } output { elasticsearch { hosts => ["localhost"] } }'`

 **注意：**如果看到这样的报错信息 Logstash could not be started because there is already another instance using the configured data directory. If you wish to run multiple instances, you must change the "path.data" setting. 请执行命令：service logstash stop 然后在执行就可以了。

3、当命令成功被执行后，看到：Successfully started Logstash API endpoint {:port=>9600} 信息后，输入：`this is a dummy entry` 然后回车，模拟一条日志进行测试。

4、打开浏览器，输入：http://192.168.0.100:9200/_search?pretty 如图，就会看到我们刚刚输入的日志内容



到此，ELK简单配置成功，接下来配置Logstash连接Kafka将日志发送给es

进入elk容器，进入 /etc/logstash/conf.d 文件夹地址中创建文件 logstash.conf 添加一下内容:

```
input {
  beats {
    port => 5044
  }

  kafka {
  	bootstrap_servers => ["http://192.168.0.100:9092"]
    topics => ["topic-log"]
  }

}

output {
  elasticsearch {
    hosts => ["http://localhost:9200"]
    index => "myservice-%{+YYYY.MM.dd}"
    #user => "elastic"
    #password => "changeme"
  }
}
```



logstash 和filebeat都具有日志收集功能，filebeat更轻量，使用go语言编写，占用资源更少，可以有很高的并发，但logstash 具有filter功能，能过滤分析日志。一般结构都是filebeat采集日志，然后发送到消息队列，如redis，kafka。然后logstash去获取，利用filter功能过滤分析，然后存储到elasticsearch中。

![](https://gitee.com/Esmusssein/picture/raw/master/uPic/aoNK2U.jpg)



```
filebeat.inputs:
- type: log
  enabled: true
  paths:
    - /Users/guangzheng.li/IdeaProjects/tutorial/logs/kafka-tutorial.log

output.kafka:
  enabled: true
  hosts: ["192.168.0.100:9092"]
  topic: 'topic-log'
  compression: gzip
  max_message_bytes: 1000000
```

启动命令

```
docker run -it --name filebeat --add-host kafka:192.168.0.100 -v /Users/guangzheng.li/IdeaProjects/tutorial/filebeat/filebeat.yml:/usr/share/filebeat/filebeat.yml elastic/filebeat:7.6.1
```

