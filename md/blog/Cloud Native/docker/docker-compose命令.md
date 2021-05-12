#启动
docker-compose up

# 暂停

docker-compose stop

#停止容器
docker-compose down

#停止容器并且移除数据
docker-compose down -v

#一些docker 命令
docker ps
docker stop Name/ContainerId
docker start Name/ContainerId

#删除单个容器
$docker rm Name/ID
-f, –force=false; -l, –link=false Remove the specified link and not the underlying container; -v, –volumes=false Remove the volumes associated to the container

#删除所有容器
$docker rm `docker ps -a -q`  
停止、启动、杀死、重启一个容器
$docker stop Name/ID  
$docker start Name/ID  
$docker kill Name/ID  
$docker restart name/ID



```
version: '3'
services:
  # 指定服务名称
  db:
    # 指定服务使用的镜像
    image: mysql:5.7
    # 指定容器名称
    container_name: dockerDB
    # 指定服务运行的端口
    ports:
      - 13306:3306
    # 指定容器中需要挂载的文件
    volumes:
      - "./conf/my.cnf:/etc/my.cnf"
      - "./init:/docker-entrypoint-initdb.d/"
    # 指定容器的环境变量
    environment:
      - MYSQL_ROOT_PASSWORD=root
  # 指定服务名称
  docker:
    # 指定服务使用的镜像
    image: esmusssein777/docker-awesome:2.0-SNAPSHOT
    # 指定容器名称
    container_name: docker-awesome
    # 指定服务运行的端口
    ports:
      - 8080:8080
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://db:3306/docker?characterEncoding=UTF-8&connectionCollation=utf8mb4_bin
    depends_on:
      - db
```

