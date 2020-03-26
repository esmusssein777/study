/Users/guangzheng.li/.jabba/jdk/zulu@1.11.0/Contents/Home

```
JAVA_8_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_231.jdk/Contents/Home
JAVA_11_HOME=/Users/guangzheng.li/.jabba/jdk/zulu@1.11.0/Contents/Home
alias jdk8="export JAVA_HOME=$JAVA_8_HOME" #编辑一个命令jdk8，输入则转至jdk1.8
alias jdk11="export JAVA_HOME=$JAVA_11_HOME" 
export JAVA_HOME=`/usr/libexec/java_home`  #最后安装的版本，这样当自动更新时，始终指向最新版本
```



```
JAVA_HOME=/Users/guangzheng.li/.jabba/jdk/zulu@1.11.0/Contents/Home
PATH=$JAVA_HOME/bin:$PATH:.
CLASSPATH=$JAVA_HOME/lib/tools.jar:$JAVA_HOME/lib/dt.jar:.
export JAVA_HOME
export PATH
export CLASSPATH
```



source /etc/profile