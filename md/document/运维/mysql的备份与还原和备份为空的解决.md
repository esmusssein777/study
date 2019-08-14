mysql的备份与还原和备份为空的解决

### 备份出来的sql文件为什么为空？

是因为在另外一台服务器部署的数据库在C盘默认program file的路径下，由于program file 文件夹名字存在空格，会导致数据库备份为空，因此需要把mysql中bin目录下的mysqldump.exe，mysql.exe两个应用程序复制到下面backupPath目录下，这个目录是你存储备份文件的目录，当然也可以在下面自己定义。

### 备份与还原代码

```java
package com;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

/**
 * 数据库的备份与还原
 * @author Es muss sein
 */
public class DbOperate {
	String serverUrl = "172.24.3.1";//数据库的url
	String username = "root";
	String password = "123456";
	String dbName = "test";//数据库的名字
	/**
	 * 数据备份
	 * @param backupPath文件存放的路径
	 * @throws IOException
	 */
	public void dbBackUp(String backupPath) throws IOException {
		//路径+名字
		String backupFile = backupPath + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".sql";
		//调用cmd命令的方法将数据库备份
		String mysql =backupPath + "\\" + "mysqldump" + " -h" + serverUrl + " -u" + username + " -p"+ password + " " + dbName + " > " + backupFile ;
		System.out.println("备份"+mysql);
	    java.lang.Runtime.getRuntime().exec("cmd /c " + mysql);
	    System.out.println("备份成功!");
	}
	
	/**
	 * 数据还原
	 * @param restorePath//还原文件的路径
	 * @param name//还原文件的名字
	 * @throws IOException
	 */
	public void dbRestore(String restorePath, String name) throws IOException {
		String restoreFile = restorePath + name;//将要还原的文件
		String mysql = restorePath + "mysql" + " -h" + serverUrl+" -u" + username + " -p"+ password + " " + dbName + " < " + restoreFile;
		System.out.println("开始还原"+mysql);
	    java.lang.Runtime.getRuntime().exec("cmd /c " + mysql);
	    System.out.println("还原成功!");
	}
}
```

backupPath + "\\" + "mysqldump"就是通过运行backupPath目录下的mysqldump.exe程序进行备份

备份的cmd命令就是

```
mysqldump -hserverUrl -uusername -ppassword dbname > savePath
```


还原的cmd命令是：

```
mysql -hserverUrl -uusername -ppassword dbname < filePath
```
