# Java生产环境下问题排查

在生产环境中，我们无法通过断点调试、新增log、可视化工具去立马查看当前的运行状态和拿到错误信息，此时，借助Java自带的命令行工具以及相关dump分析工具以及一些小技巧，可以大大提升我们排查问题的效率

## 运行参数

下面会列出一些常用且非常有效的命令以及参数来查看运行时Java程序的信息，从而辅助你了解程序运行状态。还有大量可用的功能由其他参数提供，自行参阅[oracle文档](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/toc.html)

### 查看JVM参数

`jps -l` 查看所有正在运行的Java程序，同时显示启动类类名，获取到PID

```
`4706 org.apache.catalina.startup.Bootstrap 5023 sun.tools.jps.Jps`
```

`jinfo -flags PID` 查看运行时进程参数与JVM参数

```
`Attaching to process ID 28987, please wait... Debugger attached successfully. Server compiler detected. JVM version is 25.171-b11 Non-default VM flags: -XX:CICompilerCount=3 -XX:InitialHeapSize=132120576 -XX:MaxHeapSize=2092957696 -XX:MaxNewSize=697303040 -XX:MinHeapDeltaBytes=524288 -XX:NewSize=44040192 -XX:OldSize=88080384 -XX:+UseCompressedClassPointers -XX:+UseCompressedOops -XX:+UseParallelGC Command line:  -Dspring.config.location=application.properties -Dspring.profiles.active=staging`
```

`java -XX:+PrintFlagsFinal -version` 查看当前虚拟机默认JVM参数

### 查看即时GC状态

`jstat -gc PID 1000 10` 每秒查看一次gc信息，共10次

输出比较多的参数，每个字段的解释参看 <https://docs.oracle.com/javase/8/docs/technotes/tools/unix/jstat.html>

```
` S0C    S1C    S0U    S1U      EC       EU        OC         OU       MC     MU    CCSC   CCSU   YGC     YGCT    FGC    FGCT     GCT    512.0  512.0   15.3   0.0    4416.0   1055.2   11372.0     7572.5   14720.0 14322.5 1664.0 1522.8     40    0.137   8      0.039    0.176`
```

期间可能碰到提示`sun.jvm.hotspot.runtime.VMVersionMismatchException: Supported versions are 24.181-b01. Target VM is 25.171-b11`的问题，原因在于安装了多个版本，使用`which`、`ls -l`可简介定位到与当前执行Java程序相同的Java版本

## 错误排查

### 内存问题

内存泄露导致OOM？内存占用异常的高？这是生产环境常常出现的问题，Java提供dump文件供我们对内存里发生过的事情进行了记录，我们需要借助一些工具从中获取有价值的信息。

#### 导出Dump文件

1. 提前对Java程序加上这些**参数**印dump文件 `-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./`
2. 对正在运行的程序使用**jmap**：`jmap -dump:format=b,file=heap.hprof PID`

#### 分析Dump文件

如果Dump文件不太大的话，可以传到 <http://heaphero.io/index.jsp> 来分析

文件比较大，且想进行更加系统的分析，推荐使用[MAT](https://www.eclipse.org/mat/)分析，有如下几种常用查看方式

1. 首页中的【Leak Suspects】能推测出问题所在
2. 点击【Create a histogram from an arbitrary set of objects】查到所有对象的数量
3. 右键点击某个对象【Merge Shortest Paths to GC Roots】-> 【exclude all phantom/weak/soft etc. references】能查询到大量数量的某个对象是哪个GC ROOT引用的

### 线程问题

任务长时间不退出？CPU 负载过高？很可能因为死循环或者死锁，导致某些线程一直执行不被中断，但是不报错是最烦人的，所以日志里看不到错误信息，并且又不能用dump文件分析，因为跟内存无关。这个时候就需要用线程分析工具来帮我们了。

#### 导出jstack文件

使用`jstack PID > 文件`，如果失败请加`-F`参数，如果还失败请使用Java程序启动时使用的用户执行jstack，下面是jstack的部分输出格式

```
`      线程名                                                              PID的16进制 "http-nio-8080-Acceptor-0" #17 daemon prio=5 os_prio=0 tid=0x00007fac2c4bd000 nid=0x29f4 runnable [0x00007fac192f6000]    java.lang.Thread.State: RUNNABLE（tomcat的工作线程正在运行，有NEW/RUNNABLE/BLOCKED/WAITING/TIMED_WATING/TERMINATED状态）     at sun.nio.ch.ServerSocketChannelImpl.accept0(Native Method)     at sun.nio.ch.ServerSocketChannelImpl.accept(ServerSocketChannelImpl.java:422)     at sun.nio.ch.ServerSocketChannelImpl.accept(ServerSocketChannelImpl.java:250)     - locked <0x00000000faf845a8> (a java.lang.Object)     at org.apache.tomcat.util.net.NioEndpoint$Acceptor.run(NioEndpoint.java:682)     at java.lang.Thread.run(Thread.java:748)`
```

jstack的输出可以看到所有的线程以及他们的状态，我们就可以看有哪些我们自己创建的正在运行的线程，那很可能就是那个一直在执行的线程了，此时**线程名**就格外重要了，所以建议创建新线程时指定有意义的线程名。当然，通过PID查找也非常方便。

#### 排查步骤

1. `top` 查看到哪个java程序负载高
2. `top -p PID -H` 查看该进程所有进程的运行状态
3. 记录下高负载的线程ID，`printf "&x" PID`转换成16进制
4. `jstack PID > 文件`
5. 在jstack文件中用转换成16进制之后的线程ID查询线程运行堆栈
6. 从堆栈中了解到线程在执行什么任务，并结合业务与代码判断问题所在