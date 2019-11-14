## 在Mac上安装docker

[Docker部署](https://macrozheng.github.io/mall-learning/#/deploy/mall_deploy_docker)



`brew cask install docker `  安装docker

### 安装mysql

- 下载mysql5.7的docker镜像：

  ```shell
  docker pull mysql:5.7
  ```

- 使用docker命令启动：

  ```shell
  docker run -p 3306:3306 --name mysql \
  -v $PWD/mydata/mysql/log:/var/log/mysql \
  -v $PWD/mydata/mysql/data:/var/lib/mysql \
  -v $PWD/mydata/mysql/conf:/etc/mysql \
  -e MYSQL_ROOT_PASSWORD=root  \
  -d mysql:5.7
  ```

- 参数说明

  - -p 3306:3306：将容器的3306端口映射到主机的3306端口
  - -v $PWD/mydata/mysql/conf:/etc/mysql：将配置文件夹挂在到主机
  - -v $PWD/mydata/mysql/log:/var/log/mysql：将日志文件夹挂载到主机
  - -v $PWD/mydata/mysql/data:/var/lib/mysql/：将数据文件夹挂载到主机
  - -e MYSQL_ROOT_PASSWORD=root：初始化root用户的密码

- 进入运行mysql的docker容器：

  ```shell
  docker exec -it mysql /bin/bash
  ```

- 使用mysql命令打开客户端：

  ```shell
  mysql -uroot -proot --default-character-set=utf8
  ```





## Redis安装

- 下载redis3.2的docker镜像：

  ```shell
  docker pull redis:3.2
  ```

- 使用docker命令启动：

  ```shell
  docker run -p 6379:6379 --name redis \
  -v $PWD/mydata/redis/data:/data \
  -d redis:3.2 redis-server --appendonly yes
  ```

- 进入redis容器使用redis-cli命令进行连接：

  ```shell
  docker exec -it redis redis-cli
  ```



## Nginx安装

### 下载nginx1.10的docker镜像：

```shell
docker pull nginx:1.10
```

### 从容器中拷贝nginx配置

- 先运行一次容器（为了拷贝配置文件）：

  ```shell
  docker run -p 80:80 --name nginx \
  -v $PWD/mydata/nginx/html:/usr/share/nginx/html \
  -v $PWD/mydata/nginx/logs:/var/log/nginx  \
  -d nginx:1.10
  ```

- 将容器内的配置文件拷贝到指定目录：

  ```shell
  docker container cp nginx:/etc/nginx $PWD/mydata/nginx/
  ```

- 修改文件名称：

  ```shell
  把 $PWD/mydata/nginx/ 修改成$PWD/mydata/conf/
  ```

- 终止并删除容器：

  ```shell
  docker stop nginx
  docker rm nginx
  ```

### 使用docker命令启动：

```shell
docker run -p 80:80 --name nginx \
-v $PWD/mydata/nginx/html:/usr/share/nginx/html \
-v $PWD/mydata/nginx/logs:/var/log/nginx  \
-v $PWD/mydata/nginx/conf:/etc/nginx \
-d nginx:1.10
```

## RabbitMQ安装

- 下载rabbitmq3.7.15的docker镜像：

  ```shell
  docker pull rabbitmq:3.7.15Copy to clipboardErrorCopied
  ```

- 使用docker命令启动：

  ```shell
  docker run -d --name rabbitmq \
  --publish 5671:5671 --publish 5672:5672 --publish 4369:4369 \
  --publish 25672:25672 --publish 15671:15671 --publish 15672:15672 \
  rabbitmq:3.7.15Copy to clipboardErrorCopied
  ```

- 进入容器并开启管理功能：

  ```shell
  docker exec -it rabbitmq /bin/bash
  rabbitmq-plugins enable rabbitmq_managementCopy to clipboardErrorCopied
  ```

  

- 开启防火墙：

  ```shell
  firewall-cmd --zone=public --add-port=15672/tcp --permanent
  firewall-cmd --reloadCopy to clipboardErrorCopied
  ```

- 访问地址查看是否安装成功：http://192.168.3.101:15672/ ![展示图片](https://macrozheng.github.io/mall-learning/images/refer_screen_76.png)

- 输入账号密码并登录：guest guest

- 创建帐号并设置其角色为管理员：mall mall ![展示图片](https://macrozheng.github.io/mall-learning/images/refer_screen_77.png)

- 创建一个新的虚拟host为：/mall ![展示图片](https://macrozheng.github.io/mall-learning/images/refer_screen_78.png)

- 点击mall用户进入用户配置页面 ![展示图片](https://macrozheng.github.io/mall-learning/images/refer_screen_79.png)

- 给mall用户配置该虚拟host的权限 ![展示图片](https://macrozheng.github.io/mall-learning/images/refer_screen_80.png)

## [Elasticsearch安装](https://macrozheng.github.io/mall-learning/#/deploy/mall_deploy_docker?id=elasticsearch安装)

- 下载elasticsearch6.4.0的docker镜像：

  ```shell
  docker pull elasticsearch:6.4.0Copy to clipboardErrorCopied
  ```

- 修改虚拟内存区域大小，否则会因为过小而无法启动:

  ```shell
  sysctl -w vm.max_map_count=262144Copy to clipboardErrorCopied
  ```

- 使用docker命令启动：

  ```shell
  docker run -p 9200:9200 -p 9300:9300 --name elasticsearch \
  -e "discovery.type=single-node" \
  -e "cluster.name=elasticsearch" \
  -v /mydata/elasticsearch/plugins:/usr/share/elasticsearch/plugins \
  -v /mydata/elasticsearch/data:/usr/share/elasticsearch/data \
  -d elasticsearch:6.4.0Copy to clipboardErrorCopied
  ```

- 启动时会发现/usr/share/elasticsearch/data目录没有访问权限，只需要修改/mydata/elasticsearch/data目录的权限，再重新启动。

  ```shell
  chmod 777 /mydata/elasticsearch/data/Copy to clipboardErrorCopied
  ```

- 安装中文分词器IKAnalyzer，并重新启动：

  ```shell
  docker exec -it elasticsearch /bin/bash
  #此命令需要在容器中运行
  elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v6.4.0/elasticsearch-analysis-ik-6.4.0.zip
  docker restart elasticsearchCopy to clipboardErrorCopied
  ```

- 开启防火墙：

  ```shell
  firewall-cmd --zone=public --add-port=9200/tcp --permanent
  firewall-cmd --reloadCopy to clipboardErrorCopied
  ```

- 访问会返回版本信息：http://192.168.3.101:9200/ ![展示图片](https://macrozheng.github.io/mall-learning/images/refer_screen_81.png)

## [kibana安装](https://macrozheng.github.io/mall-learning/#/deploy/mall_deploy_docker?id=kibana安装)

- 下载kibana6.4.0的docker镜像：

  ```shell
  docker pull kibana:6.4.0Copy to clipboardErrorCopied
  ```

- 使用docker命令启动：

  ```shell
  docker run --name kibana -p 5601:5601 \
  --link elasticsearch:es \
  -e "elasticsearch.hosts=http://es:9200" \
  -d kibana:6.4.0Copy to clipboardErrorCopied
  ```

- 开启防火墙：

  ```shell
  firewall-cmd --zone=public --add-port=5601/tcp --permanent
  firewall-cmd --reloadCopy to clipboardErrorCopied
  ```

- 访问地址进行测试：[http://192.168.3.101:5601](http://192.168.3.101:5601/) ![展示图片](https://macrozheng.github.io/mall-learning/images/refer_screen_90.png)

## [Mongodb安装](https://macrozheng.github.io/mall-learning/#/deploy/mall_deploy_docker?id=mongodb安装)

- 下载mongo3.2的docker镜像：

  ```shell
  docker pull mongo:3.2Copy to clipboardErrorCopied
  ```

- 使用docker命令启动：

  ```shell
  docker run -p 27017:27017 --name mongo \
  -v /mydata/mongo/db:/data/db \
  -d mongo:3.2Copy to clipboardErrorCopied
  ```

## [Docker全部环境安装完成](https://macrozheng.github.io/mall-learning/#/deploy/mall_deploy_docker?id=docker全部环境安装完成)

- 所有下载镜像文件： ![展示图片](https://macrozheng.github.io/mall-learning/images/refer_screen_82.png)
- 所有运行在容器里面的应用： ![展示图片](https://macrozheng.github.io/mall-learning/images/refer_screen_83.png)