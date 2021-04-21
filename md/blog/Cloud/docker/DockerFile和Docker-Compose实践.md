[toc]

## 如何使用DockerFile和Docker Compose来构建项目

### DockerFile构建

```shell
FROM java:8
COPY build/libs/questionnaire-1.0.0.jar /questionnaire.jar
RUN bash -c 'touch /questionnaire.jar'
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/questionnaire.jar"]
MAINTAINER guangzheng.li
```

DockerFile文件如上所示



在dockerfile文件所在目录，输入下面的命令打包成镜像

`docker build -t esmusssein777/questionnaire:1.0-SNAPSHOT .`

esmusssein777是指docker hub的名字，需要一致才能上传



上传镜像到DockerHub中

`docker push esmusssein777/questionnaire:1.0-SNAPSHOT ` 



### Docker Compose来启动容器

接着使用 `docker-compose.yml` 来启动容器

首先使用

`docker-compose --version` 查看是否安装docker compose

接着编写 yml 文件

```yml
version: '3'
services:
  # 指定服务名称
  db:
    # 指定服务使用的镜像
    image: mysql:5.7
    # 指定容器名称
    container_name: qnDB
    # 指定服务运行的端口
    ports:
      - 13306:3306
    # 指定容器中需要挂载的文件
    volumes:
      - "./db:/var/lib/mysql"
      - "./conf/my.cnf:/etc/my.cnf"
      - "./init:/docker-entrypoint-initdb.d/"
    # 指定容器的环境变量
    environment:
      - MYSQL_ROOT_PASSWORD=root
  # 指定服务名称
  questionnaire:
    # 指定服务使用的镜像
    image: esmusssein777/questionnaire:1.0-SNAPSHOT
    # 指定容器名称
    container_name: questionnaire
    # 指定服务运行的端口
    ports:
      - 8080:8080
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://db:3306/questionnaire?characterEncoding=UTF-8&connectionCollation=utf8mb4_bin
    # 指定容器中需要挂载的文件
    volumes:
      - $PWD/mydata/app/questionnaire/logs:/var/logs
    depends_on:
      - db
```

对于mysql的文件需要初始化，我们使用 my.cnf 文件和 init.sql文件

my.cnf 文件

```
## my.cnf 文件
[mysqld]
user=mysql
default-storage-engine=INNODB
character-set-server=utf8
[client]
default-character-set=utf8
[mysql]
default-character-set=utf8
```

init.sql 文件

```sql
use mysql;
ALTER USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY 'root';
create database questionnaire character set utf8;
```

可以看到这三者的目录层级是如下

```
├── conf
│   └── my.cnf
├── docker-compose.yml
└── init
    └── init.sql
```



 docker compose 的命令如下

启动 `docker-compose up -d`

停止 `docker-compose stop`



接着可以使用

```
docker ps -a 查看容器id
docker logs id 查看日志
docker ps 查看是否成功启动
```



### 遇到的问题

首先中docker中启动一个mysql，暴露3306端口。

使用下面的命令可以启动springboot服务

`docker run --net=host -p 8080:8080 --name questionnaire -d esmusssein777/questionnaire:1.0-SNAPSHOT`

可以启动成功，但是在Mac中无法访问服务地址



还有一种是按下面的命令来启动连接mysql，link命令。但是服务无法连接到mysql。服务启动失败

`docker run -p 8080:8080 --name questionnaire --link mysql:db -d esmusssein777/questionnaire:1.0-SNAPSHOT`

