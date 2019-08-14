# 当心字符串连接的性能

由于String是final的，不可变，他内部每次拼接都会创建一个StringBuffer对象，这样你如果拼接n次，那么他创建了n次对象，性能低下，而StringBuilder只在外面创建了一个对象，其他直接append字符串即可。

所以，我们在循环中拼接字符串的时候要尤其注意，

这时候，我们一般会选择StringBuffer或者StringBuilder,我们需要了解他们两者的区别：

HashTable是线程安全的，很多方法都是synchronized方法，而HashMap不是线程安全的，但其在单线程程序中的性能比HashTable要高。StringBuffer和StringBuilder类的区别也是如此，他们的原理和操作基本相同，区别在于StringBufferd支持并发操作，线性安全的，适 合多线程中使用。StringBuilder不支持并发操作，线性不安全的，不适合多线程中使用。新引入的StringBuilder类不是线程安全的，但其在单线程中的性能比StringBuffer高。


可以试下看下面的程序如何。

```
package com.ligz.Chapter8;

/**
 * @author ligz
 */
public class Item51 {
	public static void main(String[] args) {
        useString();
        useStringBuilder();
    }
    
    private static void useString() {
        String str = "";
        long start = System.currentTimeMillis();
        for(int i = 0; i < 100000; i ++) {
            str += i;
        }
        System.out.println(str);
        System.out.println(System.currentTimeMillis() - start);
    }
    
    private static void useStringBuilder() {
        StringBuilder str = new StringBuilder(); 
        long start = System.currentTimeMillis();
        for(int i = 0; i < 100000; i ++) {
            str.append(i);
        }
        System.out.println(str);
        System.out.println(System.currentTimeMillis() - start);
    }
}
```



**另外一种方法是，使用字符数组。**

# 通过接口引用对象

我们现在其实已经养成了这样的习惯，习惯用接口去引用一个对象，我们通常用下面的方法

```
List< String > list= new ArrayList< String >(); 
Map< K,V > map = new HashMap< K,V > ();
```



为什么不用

```
ArrayList< String > alist = new ArrayList< String >()；
```



因为如果我们使用这样的方法，我们在后面需要把他改为

```
LinkedList alist = new LinkedList();
```



的时候关于alist的代码全部都要修改，

而我们使用List< String > list= new ArrayList< String >(); 改为List list = new LinkedList();我们只需要修改一行代码，除非你需要用到实现中的某个特有的方法和属性。

便于程序代码的重构. 这就是面向接口编程的好处。



但是如果没有合适的接口存在,完全可以使用类而不是接口来引用对象。
(即可以使用父类,基类做引用对象,和接口做引用对象的本质是相同的,都是利用多态,达到方便扩展的目的)

1.值类,比如String和BigInteger.记住值类很少会用多个实现编写,它们通常是final,并且很少有对应的接口.使用值类做参数,变量,域或者返回值类型是再合适不过的。

2.不存在适当接口类型,对象属于框架,而框架的基本类型的类,不是接口.如果对象属于这种基于类的框架,那么就应该使用基类(通常是抽象类)来引用这个对象,而不是实现类。

3.类实现接口,但是它提供接口中不存在的额外方法,如果程序依赖这些额外方法,这种类应该只被用来引用它的实例

# 接口优先于反射机制

核心反射机制java.lang.reflect提供了“通过程序来访问关于已装载的类的信息”的能力，给定一个Class实例，可以获得Constructor、Method、Field实例，这些对象提供“通过程序来访问类的成员名称、域类型、方法签名等信息”的能力。

反射机制允许一个类使用另一个类，即使当前者被编译的时候后者还根本不存在，存在的代价：

1.失去编译时类型检查的好处，包括异常检查。

2.执行反射访问所需的代码很长。

3.性能上的损失。

### 反射机制的使用场景 反射功能只是在设计时被用到，通常，普通应用程序在运行时不应该以反射的方式访问对象。

有些复杂的应用程序需要使用反射机制，包括类浏览器、对象检测器、代码分析工具、解释型的内嵌式系统。在RPC中使用反射机制也是合适的，这样就不再需要存根编译器。

对于有些程序，必须用到在编译时无法获取的类，但是在编译时存在适当的接口或者超类，通过它们可以引用这个类，就可以以反射的方式创建实例，然后通过它们的接口或者超类，以正常的方式访问这些实例。

**反射机制的例子**

创建Set实例，把命令行参数插入到集合中，然后打印该集合，其中第一个参数指定打印的结果，如果是HashSet以随机的方式打印出来，如果是TreeSet按照字母顺序打印出来的程序：

```
public static void main(String[] args) {
        Class<?> c = null;
        try {
            c = Class.forName(args[0]);
        } catch(ClassNotFoundException e) {
            System.out.println("Class not found");
            System.exit(1);
        }
        Set<String> s = null;
        try {
            s = (Set<String>) c.newInstance();
        } catch(IllegalAccessException e) {
            System.out.println("Class not accessible");
            System.exit(1);
        } catch(InstantiationException e) {
            System.out.println("Class not instantiable");
            System.exit(1);
        }
        s.addAll(Arrays.asList(args).subList(1, args.length));
        System.out.println(s);
}
```



这相当于一个集合测试器，通过反射测试Set实现，同时，它也可以作为通用的集合性能分析工具。 
这种方法足以完成成熟的服务提供者框架。  

**service provider frameworks** - 服务提供者框架: 多个服务提供者实现一个服务，系统为服务提供者的客户端提供多个实现，并把他们从多个实现中解耦出来。（比如jdbc，我们不管数据库是哪个）

| 组件                                       | 说明                                                         |
| ------------------------------------------ | ------------------------------------------------------------ |
| 服务接口(Service Interface )               | 这是提供者实现的                                             |
| 提供者注册API(Provider Registration API)   | 这是系统用来注册实现，让客户端访问                           |
| 服务访问API(Service Access API)            | 是客户端用来获取服务的实例的 服务访问API一般允许但是不要求客户端指定某种选择提供者的条件，如果没有这样的规定，API就会返回默认实现的一个实例 服务访问API是“灵活的静态工厂”，它构成了SPF的基础 |
| 服务提供者接口(Service Provider Interface) | 负责创建其服务实现的实例                                     |



获取 JDBC 连接的代码如下

```
//使用调用者的类加载器，加载全限定名指定的类
Class.forName("com.mysql.jdbc.Driver");
//通过已注册的 JDBC 驱动，根据参数获取数据库连接
conn = DriverManager.getConnection(DB_URL,USER,PASS);
```



查看 com.mysql.jdbc.Driver 的静态代码块（在类被加载时执行）

```
public class Driver extends NonRegisteringDriver implements java.sql.Driver {

    //通过 DriverManager 注册当前驱动（JDBC for MySql）
    static {
        try {
            java.sql.DriverManager.registerDriver(new Driver());
        } catch (SQLException E) {
            throw new RuntimeException("Can't register driver!");
        }
    }
}
```



继续查看 DriverManager 的 registerDriver()

```
public class DriverManager {

    //已注册 JDBC 驱动的列表
    private final static CopyOnWriteArrayList<DriverInfo> registeredDrivers = new CopyOnWriteArrayList<>();

    //......

    public static synchronized void registerDriver(java.sql.Driver driver)
        throws SQLException {

        registerDriver(driver, null);
    }

    public static synchronized void registerDriver(java.sql.Driver driver,
            DriverAction da)
        throws SQLException {

        /* Register the driver if it has not already been added to our list */
        if(driver != null) {
            registeredDrivers.addIfAbsent(new DriverInfo(driver, da));
        } else {
            // This is for compatibility with the original DriverManager
            throw new NullPointerException();
        }

        println("registerDriver: " + driver);

    }
}
```



可以看到 Class.forName(“com.mysql.jdbc.Driver”); 的作用就是往 DriverManager 里注册一个 JDBC 驱动

接着查看 DriverManager 的 getConnection() 方法源码

```
 @CallerSensitive
    public static Connection getConnection(String url,
        String user, String password) throws SQLException {
        java.util.Properties info = new java.util.Properties();

        if (user != null) {
            info.put("user", user);
        }
        if (password != null) {
            info.put("password", password);
        }

        //第三个参数为调用者的类
        return (getConnection(url, info, Reflection.getCallerClass()));
    }

    //......

    private static Connection getConnection(
        String url, java.util.Properties info, Class<?> caller) throws SQLException {
        /*
         * When callerCl is null, we should check the application's
         * (which is invoking this class indirectly)
         * classloader, so that the JDBC driver class outside rt.jar
         * can be loaded from here.
         */
        //如果存在调用者的类，就使用调用者的类加载器
        ClassLoader callerCL = caller != null ? caller.getClassLoader() : null;
        synchronized(DriverManager.class) {
            // synchronize loading of the correct classloader.
            if (callerCL == null) {
                //如果缺少类加载器，使用当前线程的上下文类加载器
                callerCL = Thread.currentThread().getContextClassLoader();
            }
        }

        if(url == null) {
            throw new SQLException("The url cannot be null", "08001");
        }

        println("DriverManager.getConnection(\"" + url + "\")");

        // Walk through the loaded registeredDrivers attempting to make a connection.
        // Remember the first exception that gets raised so we can reraise it.
        SQLException reason = null;

        //遍历所有已注册的驱动，尝试获取数据库连接
        for(DriverInfo aDriver : registeredDrivers) {
            // If the caller does not have permission to load the driver then
            // skip it.
            if(isDriverAllowed(aDriver.driver, callerCL)) {
                try {
                    println("    trying " + aDriver.driver.getClass().getName());
                    Connection con = aDriver.driver.connect(url, info);
                    if (con != null) {
                        // Success!
                        println("getConnection returning " + aDriver.driver.getClass().getName());
                        return (con);
                    }
                } catch (SQLException ex) {
                    if (reason == null) {
                        reason = ex;
                    }
                }

            } else {
                println("    skipping: " + aDriver.getClass().getName());
            }

        }

        // if we got here nobody could connect.
        if (reason != null)    {
            println("getConnection failed: " + reason);
            throw reason;
        }

        println("getConnection: no suitable driver found for "+ url);
        throw new SQLException("No suitable driver found for "+ url, "08001");
    }
```



Class.forName("com.mysql.jdbc.Driver");

这里的Driver就是我们的提供者类。负责提供Connection（服务）的

Connection connection= DriverManager.getConnection("jdbc:mysql:///xxx", "root", "root");

这里的DriverManager对应提供者管理类，它对外提供一个获取服务（也就是Connection）的方法。

jdbc:mysql说明这个服务提供者是MySQL的，那么从集合(这里用的不是map，是一种list数组集合，可以看看DriverManager类的源码)里取出实现MySQL数据库连接的服务提供者，调用服务提供者的获取服务方法（前文说过，服务提供者肯定有一个获取服务的方法），然后返回我们需要的服务，也就是Connection对象。



简而言之，如果你使用的程序必须要和未知的类一起工作，那么仅仅使用发射机制来实例化对象，而访问对象时则使用编程时已知的接口或者是超类。



# **谨慎地使用本地方法**

Java Native Interface(JNI)，本地方法是指用本地程序设计语言来编写的特殊方法（C或者C++）
本地方法有三种用途：
①访问特定于平台的机制（注册表和文件锁）
②提供了访问遗留代码库的能力
③本地方法可以通过本地语言，编写应用程序中注重性能的部分，提高系统的性能
本地方法有一些严重的缺点，因为本地语言不是安全的，所以使用本地方法的应用程序也不再能免受内存毁坏错误的影响



# **谨慎地进行优化**

### 1、不要因为性能而牺牲合理的结构，要努力编写好的程序而不是快的程序。

### 2、并且一般而言，好的API设计也会带来好的性能，为了获得好的性能而对API进行包装，这是一种非常不好的想法



# **遵守普遍接受的命名惯例**

现在去对照阿里的Java编程手册可能更有效果了。

下面是书上的一些例子。

![img](https://imgconvert.csdnimg.cn/aHR0cHM6Ly9vc2NpbWcub3NjaGluYS5uZXQvb3NjbmV0LzBhNjRiMDU1MGI1NTRlYmJhNzFjNWEwOWIwYTJmMTljOGEwLmpwZw)