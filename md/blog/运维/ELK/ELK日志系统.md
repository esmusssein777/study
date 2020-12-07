# ELK日志系统

## 部署ELK

使用docker-compose来部署

```
version: '2.2'
services:
  cerebro:
    image: lmenezes/cerebro:0.8.3
    container_name: cerebro
    ports:
      - "9000:9000"
    command:
      - -Dhosts.0.host=http://elasticsearch:9200
    networks:
      - elastic
  kibana:
    image: docker.elastic.co/kibana/kibana:7.6.2
    container_name: kibana
    environment:
      ELASTICSEARCH_URL: http://es01:9200
      ELASTICSEARCH_HOSTS: http://es01:9200
    ports:
      - "5601:5601"
    networks:
      - elastic
  logstash:
    image: docker.elastic.co/logstash/logstash:7.6.2
    container_name: logstash
    environment:
      LS_JAVA_OPTS: "-Xmx256m -Xms256m"
      ELASTICSEARCH_URL: http://es01:9200
      ELASTICSEARCH_HOSTS: http://es01:9200
    volumes:
      - configlogstash:/usr/share/logstash/config
      - pipelinelogstash:/usr/share/logstash/pipeline
    ports:
      - "4560:4560"
      - "9600:9600"
    networks:
      - elastic
  es01:
    image: docker.elastic.co/elasticsearch/elasticsearch:7.6.2
    container_name: es01
    environment:
      - node.name=es01
      - cluster.name=es-docker-cluster
      - discovery.seed_hosts=es02,es03
      - cluster.initial_master_nodes=es01,es02,es03
      - bootstrap.memory_lock=true
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ulimits:
      memlock:
        soft: -1
        hard: -1
    volumes:
      - data01:/usr/share/elasticsearch/data
    ports:
      - 9200:9200
      - 9300:9300
    networks:
      - elastic
  es02:
    image: docker.elastic.co/elasticsearch/elasticsearch:7.6.2
    container_name: es02
    environment:
      - node.name=es02
      - cluster.name=es-docker-cluster
      - discovery.seed_hosts=es01,es03
      - cluster.initial_master_nodes=es01,es02,es03
      - bootstrap.memory_lock=true
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ulimits:
      memlock:
        soft: -1
        hard: -1
    volumes:
      - data02:/usr/share/elasticsearch/data
    networks:
      - elastic
  es03:
    image: docker.elastic.co/elasticsearch/elasticsearch:7.6.2
    container_name: es03
    environment:
      - node.name=es03
      - cluster.name=es-docker-cluster
      - discovery.seed_hosts=es01,es02
      - cluster.initial_master_nodes=es01,es02,es03
      - bootstrap.memory_lock=true
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ulimits:
      memlock:
        soft: -1
        hard: -1
    volumes:
      - data03:/usr/share/elasticsearch/data
    networks:
      - elastic

volumes:
  configlogstash:
    driver: local
  pipelinelogstash:
    driver: local
  data01:
    driver: local
  data02:
    driver: local
  data03:
    driver: local

networks:
  elastic:
    driver: bridge
```

需要注意的点是

* 挂载问题
  * Linux中，按logstash举例，挂载的默认路径是`vim /var/lib/docker/volumes/guangzheng_pipelinelogstash/_data/logstash.conf`,Linux会自动加前缀，我们可以使用命令`sudo su -`进入 root 权限来docker-compose up
  * Mac中，不会主动加载到本地目录，需要先通过`screen ~/Library/Containers/com.docker.docker/Data/vms/0/tty`的方式进入docker中，然后去找挂载的文件`var/lib/docker/volumes/`
* 分词问题
  * 可以通过命令 `docker-compose exec es01 elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v7.6.2/elasticsearch-analysis-ik-7.6.2.zip`来直接安装分词

## 配置logstash

在前面挂载文件时，需要修改挂载的文件

pipeline/logstash.conf

```
input {
    tcp{
        port => 4560
        codec => json_lines
    }
}

output {
  elasticsearch {
    hosts => ["es01:9200"]
    action => "index"
    index => "%{appname}"
  }
}
```

config/logstash.yml

```
注释掉监控
```

## 配置logbaxk

```
<springProperty scope="context" name="env" source="spring.profiles.active"/>
<springProperty scope="context" name="appname" source="spring.application.name"/>
<springProperty scope="context" name="logstash" source="logstash.host"/>
    
    <springProfile name="dev,qa,prod">

        <appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
            <destination>${logstash}</destination>
            <queueSize>262144</queueSize>
            <encoder class="net.logstash.logback.encoder.LogstashEncoder" >
                <customFields>{"appname":"${appname}-${env}"}</customFields>
            </encoder>
        </appender>

        <root level="INFO">
            <appender-ref ref="LOGSTASH"/>
        </root>
    </springProfile>
```

${logstash} = logstash的url，包括 host:port

${appname} = 后端名字

${env} = 当前的环境

添加引用：

```
implementation 'net.logstash.logback:logstash-logback-encoder:5.2'
```

