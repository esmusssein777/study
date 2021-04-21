# Tomcat 类加载器

## Tomcat 自定义类加载器

Tomcat 的自定义类加载器 WebAppClassLoader 打破了双亲委托机制，它首先自己尝试去加载某个类，如果找不到再代理给父类加载器，**其目的是优先加载 Web 应用自己定义的类**。

具体实现就是重写 ClassLoader 的两个方法: findClass 和 loadClass。

在 findClass 方法里，主要有三个步骤:

1. 先在 Web 应用本地目录下查找要加载的类。

2. 如果没有找到，交给父加载器去查找，它的父加载器就是上面提到的系统类加载AppClassLoader。

3. 如何父加载器也没找到这个类，抛出 ClassNotFound 异常。

```java
public Class<?> findClass(String name) throws ClassNotFound {
		...
    Class<?> clazz = null;
    try {
				//1. 先在 Web 应用目录下查找类
        clazz = findClassInternal(name);
    } catch (RuntimeException e) {
				throw e; 
		}
    if (clazz == null) {
    try {
				//2. 如果在本地目录没有找到，交给父加载器去查找
				clazz = super.findClass(name);
    } catch (RuntimeException e) {
        throw e;
		}
		//3. 如果父类也没找到，抛出 ClassNotFoundException
		if (clazz == null) {
        throw new ClassNotFoundException(name);
     }
    return clazz;
}
```



接着我们再来看 Tomcat 类加载器的 loadClass 方法的实现

```java
public Class<?> loadClass(String name, boolean resolve)
    synchronized (getClassLoadingLock(name)) {
        Class<?> clazz = null;
				//1. 先在本地 cache 查找该类是否已经加载过
  			clazz = findLoadedClass0(name);
				if (clazz != null) {
            if (resolve)
                resolveClass(clazz);
            return clazz;
        }
  
				//2. 从系统类加载器的 cache 中查找是否加载过 
  			clazz = findLoadedClass(name);
				if (clazz != null) {
            if (resolve)
                resolveClass(clazz);
            return clazz;
        }
  
				// 3. 尝试用 ExtClassLoader 类加载器类加载
  			ClassLoader javaseLoader = getJavaseClassLoader 
        try {
            clazz = javaseLoader.loadClass(name);
            if (clazz != null) {
                if (resolve)
                    resolveClass(clazz);
                return clazz;
            }
        } catch (ClassNotFoundException e) {
            // Ignore
				}
				// 4. 尝试在本地目录搜索 class 并加载 
  			try {
            clazz = findClass(name);
            if (clazz != null) {
                if (resolve)
                    resolveClass(clazz);
                return clazz;
            }
        } catch (ClassNotFoundException e) {
            // Ignore
				}
				// 5. 尝试用系统类加载器 (也就是 AppClassLoader) 
				try {
                clazz = Class.forName(name, false, pare
                if (clazz != null) {
                    if (resolve)
                        resolveClass(clazz);
                    return clazz;
                }
            } catch (ClassNotFoundException e) {
                // Ignore
				}
				//6. 上述过程都加载失败，抛出异常
   			throw new ClassNotFoundException(name);
}
```

loadClass 方法稍微复杂一点，主要有六个步骤 :

1. 先在本地 Cache 查找该类是否已经加载过，也就是说 Tomcat 的类加载器是否已经加载过这个类。

2. 如果 Tomcat 类加载器没有加载过这个类，再看看系统类加载器是否加载过。

3. 如果都没有，就让ExtClassLoader去加载，这一步比较关键，目的防止 Web 应用自己的类覆盖 JRE 的核心类。因为 Tomcat 需要打破双亲委托机制，假如 Web 应用里自定义了一个叫 Object 的类，如果先加载这个 Object 类，就会覆盖 JRE 里面的那个 Object 类，这就是为什么 Tomcat 的类加载器会优先尝试用 ExtClassLoader 去加载，因为 ExtClassLoader 会委托给 BootstrapClassLoader 去加载， BootstrapClassLoader 发现自己已经加载了 Object 类，直接返回给 Tomcat 的类加载器，这样 Tomcat 的 类加载器就不会去加载 Web 应用下的 Object 类了，也就避免了覆盖 JRE 核心类的问题。

4. 如果 ExtClassLoader 加载器加载失败，也就是说 JRE 核心类中没有这类，那么就在本地 Web 应用目录下查找并加载。

5. 如果本地目录下没有这个类，说明不是 Web 应用自己定义的类，那么由系统类加载器去加载。这里请你注意， Web 应用是通过Class.forName调用交给系统类加载器的，因为Class.forName的默认加载器就是系统类加载器。

6. 如果上述加载过程全部失败，抛出 ClassNotFound 异常。

从上面的过程我们可以看到，Tomcat 的类加载器打破了双亲委托机制，没有一上来就直接委托给父加载器，而是先在本地目录下加载，为了避免本地目录下的类覆盖 JRE 的核心类，先尝试用 JVM 扩展类加载器 ExtClassLoader 去加载。那为什么不先用系统类加载器 AppClassLoader 去加载?很显然，如果是这样的话，那就变成双亲委托机制了， 这就是 Tomcat 类加载器的巧妙之处。

## **Tomcat** 类加载器的层次结构

为了解决几个问题：

* 如何隔离不同web容器的同名的 Servlet 类？
* 不同 web 容器依赖同一个第三方 jar 包如何共享？
* 跟 JVM 一样，如何隔离 Tomcat 本身的类和 Web 应用的类？

Tomcat 设计了类层次结构的加载器来解决

<img src="/Users/guangzheng.li/Library/Application Support/typora-user-images/image-20210407161012380.png" alt="image-20210407161012380" style="zoom:50%;" />

**如何隔离不同web容器的同名的 Servlet 类？**

Tomcat 的解决方案是自定义一个类加载器 WebAppClassLoader， 并且给每个 Web 应用创建一个类加载器实例。我们知道，Context 容器组件对应一个 Web 应用，因此，每个 Context 容器负责创建和维护一个 WebAppClassLoader 加载器实例。这背后的原理是，不同的加载器实例加载的类被认为是不同的类，即使它们的类名相同。这就相当于在 Java 虚拟机内部创建了一个个相互隔离的 Java 类空间，每一个 Web 应用都有自己的类空间， Web 应用之间通过各自的类加载器互相隔离。

**不同 web 容器依赖同一个第三方 jar 包如何共享？**

Tomcat 的设计者加了一个类加载器 SharedClassLoader，作为 WebAppClassLoader 的父加载器，专门来加载 Web 应用之间共享的类。如果 WebAppClassLoader 自己没有加载到某个类，就会委托父加载器 SharedClassLoader 去加载这个类， SharedClassLoader 会在指定目录下加载共享类，之后返回给 WebAppClassLoader，这样共享的问题就解决了

**如何隔离 Tomcat 本身的类和 Web 应用的类？**

我们知道，要共享可以通过父子关系，要隔离那就需要兄弟关系了。兄弟关系就是指两个类加载器是平行的，它们可能拥有同一个父加载器，但是两个兄弟类加载器加载的类是隔离的。基于此 Tomcat 又设计一个类加载器 CatalinaClassloader，专门来加载 Tomcat 自身的类。这 样设计有个问题，那 Tomcat 和各 Web 应用之间需要共享一些类时该怎么办呢?

老办法，还是再增加一个 CommonClassLoader，作为 CatalinaClassloader 和 SharedClassLoader 的父加载器。 CommonClassLoader 能加载的类都可以被CatalinaClassLoader 和 SharedClassLoader 使用，而 CatalinaClassLoader 和 SharedClassLoader 能加载的类则与对方相互隔离。WebAppClassLoader 可以使用 SharedClassLoader 加载到的类，但各个 WebAppClassLoader 实例之间相互隔离。

### Spring 加载问题

在 JVM 的实现中有一条隐含的规则，默认情况下，如果一个类由类加载器 A 加载，那么这个类的依赖类也是由相同的类加载器加载。比如 Spring 作为一个 Bean 工厂，它需要创建业务类的实例，并且在创建业务类实例之前需要加载这些类。Spring 是通过调用Class.forName来加载业务类的。

Web 应用之间共享的 JAR 包可以交给 SharedClassLoader 来加载，从而避免重复加载。Spring 作为共享的第三方 JAR 包，它本身是由 SharedClassLoader 来加载的，Spring 又要去加载业务类，按照前面那条规则，加载 Spring 的类加载器也会用来加载业务类，但是业务类在 Web 应用目录下，不在 SharedClassLoader 的加载路径下。

于是线程上下文加载器登场了，它其实是一种类加载器传递机制。为什么叫作“线程上下文加载器”呢，因为这个类加载器保存在线程私有数据里，只要是同一个线程，一旦设置了线程上下文加载器，在线程后续执行过程中就能把这个类加载器取出来用。因此 Tomcat 为每个 Web 应用创建一个 WebAppClassLoarder 类加载器，并在启动 Web 应用的线程里设置线程上下文加载器，这样 Spring 在启动时就将线程上下文加载器取出来，用来加载 Bean。