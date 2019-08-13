我们搭建redis分Windows版和Linux都试了，Windows我们用的是win10，Linux用的是centos7.

**Windows版：**

我们先从https://github.com/ServiceStack/redis-windows下载最新的redis。

下载后解压如图，解压redis-latest.zip，

![img](https://img-blog.csdn.net/2018092016035362?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM5MDcxNTMw/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

解压后在目录下新建startup.bat,在里面写redis-server.exe  redis.windows.conf。

启动bat文件如下

![img](https://img-blog.csdn.net/20180920160647230?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM5MDcxNTMw/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

**Linux版：**

获取redis：

```
$ wget http://download.redis.io/releases/redis-4.0.10.tar.gz
$ tar xzf redis-4.0.10.tar.gz
$ cd redis-4.0.10
```

![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

安装：

```
$ make
# 将redis-server redis-cli执行程序安装在/usr/local/redis目录下
$ make PREFIX=/usr/local/redis install
```

![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

**安装过程可能出现的问题：**

1. CentOS5.7默认没有安装gcc，这会导致我们无法make成功。使用yum安装：

   ```html
   yum -y install gcc
   ```

2. make时报如下错误：

   ```html
   zmalloc.h:50:31: error: jemalloc/jemalloc.h: No such file or directoryzmalloc.h:55:2: error: #error "Newer version of jemalloc required"make[1]: *** [adlist.o] Error 1make[1]: Leaving directory `/data0/src/redis-2.6.2/src'make: *** [all] Error 2
   ```

   原因是jemalloc重载了Linux下的ANSI C的malloc和free函数。解决办法：make时添加参数。

   ```html
   make MALLOC=libc
   ```

3. make之后，会出现一句提示

   ```html
   Hint: To run 'make test' is a good idea ;) 
   ```

   但是不测试，通常是可以使用的。若我们运行make test ，会有如下提示

   ```html
   [devnote@devnote src]$ make testYou need tcl 8.5 or newer in order to run the Redis testmake: ***[test] Error_1
   ```

   解决办法是用yum安装tcl8.5（或去tcl的官方网站http://www.tcl.tk/下载8.5版本，并参考官网介绍进行安装）

   ```html
   yum install tcl
   ```



 修改redis.conf配置文件：

```
## 为了redis客户端远程能够访问

1.将`bind 127.0.0.1`改为`#bind 127.0.0.1`

2.将`protected-mode yes`改为`protected-mode no`,

## 指定日志文件目录
logfile "/var/log/redis/server-out.log"

## 默认启动时为后台启动
daemonize yes
```

![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

启动：

```
$ cp redis.conf /usr/local/redis/bin/redis.conf
# 加入环境变量
$ ln -s /usr/local/redis/bin/* /usr/sbin
$ /usr/local/redis/bin/redis-server /usr/local/redis/bin/redis.conf
```

![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

 这样就后台启动成功，我们可以通过输入ps -ef | grep redis得到下面的样子

![img](https://img-blog.csdn.net/20181019135217600?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM5MDcxNTMw/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

这样就说明可以在其他的机器上连接此台的redis

我们输入/usr/local/redis/bin/redis-cli 可以进入命令行，这里是我里面存入了数据才可以得到。你也可以自己去测试.

![img](https://img-blog.csdn.net/20181019135349962?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM5MDcxNTMw/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)