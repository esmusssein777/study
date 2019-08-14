解决访问GitHub太慢

解决方案：

**第一步**：访问<https://www.ipaddress.com/>网站，分别输入github.global.ssl.fastly.net和github.com

例如在里面输入github.global.ssl.fastly.net可得到下面的图

​                  ![img](https://img-blog.csdn.net/201807311919040?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM5MDcxNTMw/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)

我们将输入github.global.ssl.fastly.net得到的ip记住151.101.13.194。

同理我们在里面输入github.com 得到ip，例如我是得到192.30.253.113。

**第二步**：以window为例，在C:\Windows\System32\drivers\etc目录下找到hosts文件，在里面添加下面

\#Github
151.101.13.194 github.global.ssl.fastly.net 
192.30.253.113 github.com

如图                     

​    ![img](https://img-blog.csdn.net/20180731192809613?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM5MDcxNTMw/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)

保存，如果是win10的话要注意权限问题，给自己添加写入的权限，具体的可以百度

**第三步**：打开cmd，输入ipconfig /flushdns刷新dns。



大功告成，再次访问GitHub，可以愉快的开始GitHub之旅了。