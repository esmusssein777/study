## 命令

`docker pull `下拉

`docker ps -l `查询运行的项目

`docker ps` 查询正在运行的项目

`docker start mysql`启动项目

`docker images`查询已有镜像 

`docker kill id`暂停

`docker rm id`删除



```
$ docker ps // 查看所有正在运行容器
$ docker stop containerId // containerId 是容器的ID

$ docker ps -a // 查看所有容器
$ docker ps -a -q // 查看所有容器ID

$ docker stop $(docker ps -a -q) //  stop停止所有容器
$ docker  rm $(docker ps -a -q) //   remove删除所有容器
```

