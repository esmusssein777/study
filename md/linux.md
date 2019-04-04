# Linux命令的使用

## 文件属性

``ls -al``出现下面的

![](https://github.com/esmusssein777/study/blob/master/src/main/resources/img/Snipaste_2019-03-16_17-31-43.png)

``-rw-r--r--``代表的是该文件的权限，这个是最需要记忆的，后面是用户、大小、修改时间名字等

1. 第一个``[-]``代表了是目录还是文件

```
[d]代表了目录
[-]代表了文件
[l]代表了链接
```

2. 后面的-rwxr--r--三个一组，分别代表了自己，用户组，别人的权限

r代表可读，为4
w代表了可写，为2
x代表了可执行，为1

权限大小就是加起来，用chmod来表示，``chmod 777 .test``表示test文件是三个权限都有


##  文件命令

cd 进入   cd.. 回到上层目录    cd - 回到刚刚的目录

pwd 显示当前目录

mkdir 创建目录  mkdir -p 创建多层空目录   mkdir -m 777 test  创建带权限的目录

rmdir 删除空的目录  -p 删除上层空目录  -r删除有文件的目录

find . -name "*.o"  | xargs rm -f  递归删除后缀文件

mv 移动

ls 查看  -a 全部文件  -d 目录   -l 全部信息(权限)

cp 复制  -a 相当于-pdr(整个特性都一模一样的，修改时间)   -i 覆盖前询问   -r目录复制，会递归复制  cp -l是硬链接  cp -s是软链接


查阅命令

cat：由第一行显示文件内容

more：一页一页显示

less：和more一样，可以向前翻页

touch 创建文件  find  查找文件




# Shell 命令

vi 进去文件后

### 在没有编辑前

ctrl + f 翻页下  ctrl + b上翻页

数字0移到一行的最前面   gg移到文件最前面   n + Enter 进入某一行

/string 寻找下面的一个字符串  dd删除光标所在的一行   yy为复制这一行  pp复制

u 复原前面的操作  Ctrl+r 重做前面的操作




# 进程

引言：

进程：正在执行的一个程序

程序：是一种写好的代码或脚本

& : 后台执行，不占用终端
如：./helloworld &

进程的挂起：

ctrl+z ：挂起，程序放到后台执行

jobs ：查看被挂起的程序工作号



进程的恢复：

fg  工作号 ：将挂起的作业放回到前台执行

bg  工作号 ： 将挂起的作业放到后台执行


1、ps 命令用于查看当前正在运行的进程

grep 是搜索

例如： ps -ef | grep java

表示查看所有进程里 CMD 是 java 的进程信息

2、ps -aux | grep java

-aux 显示所有状态

ps

3. kill 命令用于终止进程

例如： kill -9 [PID]

-9 表示强迫进程立即停止

通常用 ps 查看进程 PID ，用 kill 命令终止进程
