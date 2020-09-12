# 部署
## 部署ELK的docker compose
写好集群所需的compose
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
    links:
      - es01
    environment:
      LS_JAVA_OPTS: -Xms2g -Xmx2g
      #ELASTICSEARCH_URL: http://es01:9200
      #ELASTICSEARCH_HOSTS: http://es01:9200
    volumes:
      - configlogstash:/usr/share/logstash/config
      - pipelinelogstash:/usr/share/logstash/pipeline
      - /var/lib/docker/volumes/mysql:/usr/share/logstash/mysql
    ports:
      - "4560:4560"
      - "9600:9600"
    depends_on:
      - es01
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
命令 `docker-compose up` 启动

需要注意的点是

* 挂载问题
  * Linux中，按logstash举例，例如挂载的默认路径是`vim /var/lib/docker/volumes/guangzheng_pipelinelogstash/_data/logstash.conf`,Linux会自动加前缀，我们可以使用命令`sudo su -`进入 root 权限来docker-compose up
  * Mac中，不会主动加载到本地目录，需要先通过`screen ~/Library/Containers/com.docker.docker/Data/vms/0/tty`的方式进入docker中，然后去找挂载的文件`var/lib/docker/volumes/`
* 分词问题
  * 可以通过命令 `docker-compose exec es01 elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v7.6.2/elasticsearch-analysis-ik-7.6.2.zip`来直接安装分词
  
为了同步mysql数据和开启日志接口，我们需要修改`pipeline/logstash.conf`文件
```
input {
    tcp{
        port => 4560
        codec => json_lines
	type => "log"
    }
    jdbc {
	jdbc_driver_library => "/usr/share/logstash/mysql/mysql-connector-java-5.1.48.jar"
        jdbc_driver_class => "com.mysql.jdbc.Driver"
        jdbc_connection_string => "jdbc:mysql://prod-data-1.mysql.database.chinacloudapi.cn:3306/scene_dev?useSSL=true&requireSSL=false&characterEncoding=UTF-8&connectionCollation=utf8mb4_bin&useLegacyDatetimeCode=false&serverTimezone=UTC"
        jdbc_user => "scene_dev@prod-data-1"
        jdbc_password => "TWr0ys1ngh4m"
        schedule => "/5 * * * * *"
        statement => "SELECT id,name,create_time as createTime,update_time as updateTime,panorama_url as panoramaUrl,is_deleted as isDeleted FROM panorama WHERE update_time >= :sql_last_value order by update_time asc"
        use_column_value => true
        tracking_column => "updateTime"
	tracking_column_type => "timestamp"
	lowercase_column_names => false
	last_run_metadata_path => "/usr/share/logstash/last_record_logstash_dev.txt"
	record_last_run => true
	tags => ["mysql-dev"]
	clean_run => false
    }
    jdbc {
        jdbc_driver_library => "/usr/share/logstash/mysql/mysql-connector-java-5.1.48.jar"
        jdbc_driver_class => "com.mysql.jdbc.Driver"
        jdbc_connection_string => "jdbc:mysql://prod-data-1.mysql.database.chinacloudapi.cn:3306/scene_test?useSSL=true&requireSSL=false&characterEncoding=UTF-8&connectionCollation=utf8mb4_bin&useLegacyDatetimeCode=false&serverTimezone=UTC"
        jdbc_user => "scene_test@prod-data-1"
        jdbc_password => "TWr0ys1ngh4m"
        schedule => "/5 * * * * *"
        statement => "SELECT id,name,create_time as createTime,update_time as updateTime,panorama_url as panoramaUrl,is_deleted as isDeleted FROM panorama WHERE update_time >= :sql_last_value order by update_time asc"
        use_column_value => true
        tracking_column => "updateTime"
        tracking_column_type => "timestamp"
        lowercase_column_names => false
        last_run_metadata_path => "/usr/share/logstash/last_record_logstash_qa.txt"
        record_last_run => true
        tags => ["mysql-qa"]
        clean_run => false
    }
    jdbc {
        jdbc_driver_library => "/usr/share/logstash/mysql/mysql-connector-java-5.1.48.jar"
        jdbc_driver_class => "com.mysql.jdbc.Driver"
        jdbc_connection_string => "jdbc:mysql://prod-data-1.mysql.database.chinacloudapi.cn:3306/scene_prod?useSSL=true&requireSSL=false&characterEncoding=UTF-8&connectionCollation=utf8mb4_bin&useLegacyDatetimeCode=false&serverTimezone=UTC"
        jdbc_user => "scene_prod@prod-data-1"
        jdbc_password => "TWr0ys1ngh4m"
        schedule => "/5 * * * * *"
        statement => "SELECT id,name,create_time as createTime,update_time as updateTime,panorama_url as panoramaUrl,is_deleted as isDeleted FROM panorama WHERE update_time >= :sql_last_value order by update_time asc"
        use_column_value => true
        tracking_column => "updateTime"
        tracking_column_type => "timestamp"
        lowercase_column_names => false
        last_run_metadata_path => "/usr/share/logstash/last_record_logstash_prod.txt"
        record_last_run => true
        tags => ["mysql-prod"]
        clean_run => false
    }
}

filter {
if "mysql-dev" in [tags] {
	mutate {
		remove_field => ["@version", "@timestamp"]
	}
}
if "mysql-qa" in [tags] {
        mutate {
                remove_field => ["@version", "@timestamp"]
        }
}
if "mysql-prod" in [tags] {
        mutate {
                remove_field => ["@version", "@timestamp"]
        }
}
}

output {
if "mysql-dev" in [tags] {
  elasticsearch {
    hosts => ["es01:9200"]
    index => "data-dev-panorama"
    document_id => "%{id}"
  }
}
if "mysql-qa" in [tags] {
  elasticsearch {
    hosts => ["es01:9200"]
    index => "data-qa-panorama"
    document_id => "%{id}"
  }
}
if "mysql-prod" in [tags] {
  elasticsearch {
    hosts => ["es01:9200"]
    index => "data-prod-panorama"
    document_id => "%{id}"
  }
}
if [type]=="log" {
  elasticsearch {
    hosts => ["es01:9200"]

   index => "%{appname}"
  }
}
}
```

修改 `config/logstash.yml` 文件

```
http.host: "0.0.0.0"
#xpack.monitoring.elasticsearch.hosts: [ "http://elasticsearch:9200" ]
pipeline.batch.size: 500
pipeline.batch.delay: 20
pipeline.workers: 8
```

重启 elk 命令 `docker-compose restart`