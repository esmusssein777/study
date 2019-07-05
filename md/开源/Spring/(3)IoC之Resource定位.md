# IoC的第一步—**Resource 定位**

[TOC]

我们一般用外部资源来描述 Bean 对象，所以在初始化 IoC 容器的第一步就是需要定位这个外部资源。

比如我们常用的xml就是用来描述 Bean 对象的，我们需要定位到`<bean>`标签才能解析。而从XML到 Resource 需要经过哪些呢。

![](https://github.com/esmusssein777/study/blob/master/md/picture/XML2Resource.png?raw=true)

xml文件的位置 —> ResourceLoader —> Resource的过程。

我们先看一下这个过程所需要用到的类是哪些

![](https://github.com/esmusssein777/study/blob/master/md/picture/Resource.png?raw=true)

![](https://github.com/esmusssein777/study/blob/master/md/picture/ResourceLoader.png?raw=true)



* Spring 将资源定义成 Resource，由 ResourceLoader 来加载。过程举个例子就是  xml 文件的位置 —> ResourceLoader —> Resource的流程。

* Resource 接口的默认实现是 AbstractResource  ，是一个抽象骨架类，它对 Resource 接口做了一个统一的实现。
* ResourceLoader 接口的默认实现是 DefaultResourceLoader ，在自定义 ResourceLoader 的时候我们除了可以继承该类外还可以实现 ProtocolResolver 接口来实现自定资源加载协议。
* DefaultResourceLoader 每次只能返回单一的资源，所以 Spring 针对这个提供了另外一个接口 ResourcePatternResolver ，该接口提供了根据指定的 locationPattern 返回多个资源的策略。

如果看不懂没有关系，我们先明白大致的类之间的关系，开始看源码就行。最后我们再回顾这里。

## 加载资源

### ResourceLoader 资源定位

`org.springframework.core.io.ResourceLoader` 为 Spring 资源加载的统一抽象，具体的资源加载则由相应的实现类来完成,关键的就是 getResource 方法获取资源。

```java
/**
 * Spring 将资源的定义和资源的加载区分开了
 * Resource 定义了统一的资源
 * 那资源的加载则由 ResourceLoader 来统一定义
 * ResourceLoader，定义资源加载器，主要应用于根据给定的资源文件地址，返回对应的 Resource
 */
public interface ResourceLoader {

	/** CLASSPATH URL 前缀。默认为："classpath:" */
	String CLASSPATH_URL_PREFIX = ResourceUtils.CLASSPATH_URL_PREFIX;

	/**
	 * 根据所提供资源的路径 location 返回 Resource 实例
	 * 但是它不确保该 Resource 一定存在，需要调用 Resource#exist() 方法来判断
	 *
	 * 该方法支持以下模式的资源加载：
	 * URL位置资源，如 "file:C:/test.dat" 。
	 * ClassPath位置资源，如 "classpath:test.dat 。
	 * 相对路径资源，如 "WEB-INF/test.dat" ，此时返回的Resource 实例，根据实现不同而不同
	 */
	Resource getResource(String location);
	
	/**
	 * Expose the ClassLoader used by this ResourceLoader.
	 */
	@Nullable
	ClassLoader getClassLoader();
}
```

### DefaultResourceLoader默认实现 ResourceLoader 

我们主要看 getResource 的默认实现

```java
/**
	 *ResourceLoader 中最核心的方法为 #getResource(String location)
	 * 它根据提供的 location 返回相应的 Resource 。
	 * 而 DefaultResourceLoader 对该方法提供了核心实现
	 * （因为，它的两个子类都没有提供覆盖该方法，
	 *  所以可以断定 ResourceLoader 的资源加载策略就封装在 DefaultResourceLoader 中)
	 */
@Override
public Resource getResource(String location) {
    Assert.notNull(location, "Location must not be null");
    // 首先，通过 ProtocolResolver 来加载资源
    for (ProtocolResolver protocolResolver : this.protocolResolvers) {
        Resource resource = protocolResolver.resolve(location, this);
        if (resource != null) {
            return resource;
        }
    }

    // 其次，以 / 开头，返回 ClassPathContextResource 类型的资源
    if (location.startsWith("/")) {
        return getResourceByPath(location);
    }// 再次，以 classpath: 开头，返回 ClassPathResource 类型的资源
    else if (location.startsWith(CLASSPATH_URL_PREFIX)) {
        return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()), getClassLoader());
    }// 然后，根据是否为文件 URL ，是则返回 FileUrlResource 类型的资源，否则返回 UrlResource 类型的资源
    else {
        try {
            URL url = new URL(location);
            return (ResourceUtils.isFileURL(url) ? new FileUrlResource(url) : new UrlResource(url));
        }
        catch (MalformedURLException ex) {
            // 最后，返回 ClassPathContextResource 类型的资源
            return getResourceByPath(location);
        }
    }
}
```

如果觉得很抽象，我们看下他解析的都是哪些路径。参考[《Spring 揭秘》] 

```
D:/Users/chenming673/Documents/spark.txt

/Users/chenming673/Documents/spark.txt

file:/Users/chenming673/Documents/spark.txt

http://www.baidu.com
```



DefaultResourceLoader 的子类都是针对一些特定的类型资源实现的。比如 FileSystemResourceLoader 就是继承 DefaultResourceLoader ，且覆写了 `#getResourceByPath(String)` 方法，使之从文件系统加载资源并以 FileSystemResource 类型返回，这样我们就可以得到想要的资源类型。所以 DefaultResourceLoader 的子类我们就不一一的细讲了，感兴趣的同学可以继续研究。

#### ProtocolResolver

`org.springframework.core.io.ProtocolResolver` 用户自定义协议资源解决策略，它允许用户自定义资源加载协议。有了 ProtocolResolver 后，我们不需要直接继承 DefaultResourceLoader，改为实现 ProtocolResolver 接口也可以实现自定义的 ResourceLoader

调用 `DefaultResourceLoader的addProtocolResolver(ProtocolResolver)` 方法加入自定义的ProtocolResolver

```
public void addProtocolResolver(ProtocolResolver resolver) {
	Assert.notNull(resolver, "ProtocolResolver must not be null");
	this.protocolResolvers.add(resolver);
}
```

ProtocolResolver 接口，仅有一个方法 `Resource resolve(String location, ResourceLoader resourceLoader)` 





### ResourcePatternResolver

ResourceLoader 的 `Resource getResource(String location)` 方法，每次只能根据 location 返回**一个** Resource 。`org.springframework.core.io.support.ResourcePatternResolver` 是 ResourceLoader 的扩展，它支持根据指定的资源路径匹配模式每次返回**多个** Resource 实例。

```java
/**
 * ResourceLoader 的 Resource getResource(String location) 方法，
 * 每次只能根据 location 返回一个 Resource 。
 * 当需要加载多个资源时，我们除了多次调用 #getResource(String location) 方法外，别无他法。
 * ResourcePatternResolver 是 ResourceLoader 的扩展，
 * 它支持根据指定的资源路径匹配模式每次返回多个 Resource 实例
 */
public interface ResourcePatternResolver extends ResourceLoader {

	/**
	 * 新增了一种新的协议前缀 "classpath*:"，该协议前缀由其子类负责实现
	 */
	String CLASSPATH_ALL_URL_PREFIX = "classpath*:";

	/**
	 * 支持根据路径匹配模式返回多个 Resource 实例
	 */
	Resource[] getResources(String locationPattern) throws IOException;

}
```



### PathMatchingResourcePatternResolver

在处理的时候。

- **非** `"classpath*:"` 开头，且路径**不包含**通配符，直接委托给相应的 ResourceLoader 来实现。
- 其他情况，调用 `#findAllClassPathResources(...)`、或 `#findPathMatchingResources(...)` 方法，返回多个 Resource 。

加载多个 Resource 的实现

```java
/**
 * 非 "classpath*:" 开头，且路径不包含通配符，直接委托给相应的 ResourceLoader 来实现。
 * 其他情况，调用 #findAllClassPathResources(...)、或 #findPathMatchingResources(...) 方法
 * 返回多个 Resource
 */
@Override
public Resource[] getResources(String locationPattern) throws IOException {
    Assert.notNull(locationPattern, "Location pattern must not be null");
    // 以 "classpath*:" 开头
    if (locationPattern.startsWith(CLASSPATH_ALL_URL_PREFIX)) {
        // a class path resource (multiple resources for same name possible)
        // 路径包含通配符
        if (getPathMatcher().isPattern(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()))) {
            // a class path resource pattern
            return findPathMatchingResources(locationPattern);
        }
        else {// 路径不包含通配符
            // all class path resources with the given name
            return findAllClassPathResources(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()));
        }
    }
    else {// 不以 "classpath*:" 开头
        // 通常只在这里的前缀后面查找模式
        // 而在 Tomcat 上只有在 “*/ ”分隔符之后才为其 “war:” 协议
        int prefixEnd = (locationPattern.startsWith("war:") ? locationPattern.indexOf("*/") + 1 :
                locationPattern.indexOf(':') + 1);
        // 路径包含通配符
        if (getPathMatcher().isPattern(locationPattern.substring(prefixEnd))) {
            // a file pattern
            return findPathMatchingResources(locationPattern);
        }
        else {
            // a single resource with the given name
            return new Resource[] {getResourceLoader().getResource(locationPattern)};
        }
    }
}

/**
 * 当 locationPattern 以 "classpath*:" 开头但是不包含通配符
 * 则调用 #findAllClassPathResources(...) 方法加载资源。
 * 该方法返回 classes 路径下和所有 jar 包中的所有相匹配的资源
 */
protected Resource[] findAllClassPathResources(String location) throws IOException {
    String path = location;
    if (path.startsWith("/")) {
        path = path.substring(1);
    }
    // 真正执行加载所有 classpath 资源
    Set<Resource> result = doFindAllClassPathResources(path);
    if (logger.isTraceEnabled()) {
        logger.trace("Resolved classpath location [" + location + "] to resources " + result);
    }
    // 转换成 Resource 数组返回
    return result.toArray(new Resource[0]);
}

/**
 * 真正执行加载所有 classpath 资源
 */
protected Set<Resource> doFindAllClassPathResources(String path) throws IOException {
    Set<Resource> result = new LinkedHashSet<>(16);
    ClassLoader cl = getClassLoader();
    // 根据 ClassLoader 加载路径下的所有资源
    Enumeration<URL> resourceUrls = (cl != null ? cl.getResources(path) : ClassLoader.getSystemResources(path));
    while (resourceUrls.hasMoreElements()) {
        // 将 URL 转换成 UrlResource
        URL url = resourceUrls.nextElement();
        result.add(convertClassLoaderURL(url));
    }
    if ("".equals(path)) {
        // The above result is likely to be incomplete, i.e. only containing file system references.
        // We need to have pointers to each of the jar files on the classpath as well...
        // 加载路径下得所有 jar 包
        addAllClassLoaderJarRoots(cl, result);
    }
    return result;
}
```

## 资源

### Resource 资源

我们用 ResourceLoader 加载了半天的 Resource 到底是什么?

Resource 为 Spring 框架所有资源的抽象和访问接口

先看代码的定义吧

```
public interface Resource extends InputStreamSource {

	/**
	 * 资源是否存在
	 */
	boolean exists();

	/**
	 * 资源是否可读
	 */
	default boolean isReadable() {
		return exists();
	}

	/**
	 * Indicate whether this resource represents a handle with an open stream.
	 * 资源所代表的句柄是否被 stream 打开
	 */
	default boolean isOpen() {
		return false;
	}

	/**
	 * 是否是 file
	 */
	default boolean isFile() {
		return false;
	}

	/**
	 * 返回资源的 URL 的句柄
	 */
	URL getURL() throws IOException;

	/**
	 * 返回资源的 URI 的句柄
	 */
	URI getURI() throws IOException;

	/**
	 * 返回资源的 File 的句柄
	 */
	File getFile() throws IOException;

	/**
	 * 返回 ReadableByteChannel
	 */
	default ReadableByteChannel readableChannel() throws IOException {
		return Channels.newChannel(getInputStream());
	}

	/**
	 * 资源内容的长度
	 */
	long contentLength() throws IOException;

	/**
	 * 资源最后的修改时间
	 */
	long lastModified() throws IOException;

	/**
	 * 根据资源的相对路径创建新资源
	 */
	Resource createRelative(String relativePath) throws IOException;

	/**
	 * 资源的文件名
	 */
	@Nullable
	String getFilename();

	/**
	 * 资源的描述
	 */
	String getDescription();

}
```

它继承 `org.springframework.core.io.InputStreamSource`接口。作为所有资源的统一抽象，Resource 定义了一些通用的方法,由子类 `AbstractResource` 提供统一的默认实现。

### Resource 的默认实现 AbstractResource

它实现了 Resource 接口的大部分的公共实现,也是一个抽象骨架类

```
/**
 * 为 Resource 接口的默认抽象实现。
 * 它实现了 Resource 接口的大部分的公共实现，作为 Resource 接口中的重中之重
 *
 * 骨架抽象类，如果想要自定义一个Resource，不要实现Resource，而是继承AbstractResource
 * 实现自己所需要的接口即可
 */
public abstract class AbstractResource implements Resource {

	/**
	 * 判断文件是否存在，若判断过程产生异常（因为会调用SecurityManager来判断），就关闭对应的流
	 */
	@Override
	public boolean exists() {
		// 基于 File 进行判断有没有
		try {
			return getFile().exists();
		}
		catch (IOException ex) {
			// Fall back to stream existence: can we open the stream?
			try {
				getInputStream().close();
				return true;
			}
			catch (Throwable isEx) {
				return false;
			}
		}
	}

	/**
	 * 直接返回true，表示可读
	 */
	@Override
	public boolean isReadable() {
		return exists();
	}

	/**
	 * 直接返回 false，表示未被打开
	 */
	@Override
	public boolean isOpen() {
		return false;
	}

	/**
	 * 直接返回false，表示不为 File
	 */
	@Override
	public boolean isFile() {
		return false;
	}

	/**
	 * 抛出 FileNotFoundException 异常，交给子类实现
	 */
	@Override
	public URL getURL() throws IOException {
		throw new FileNotFoundException(getDescription() + " cannot be resolved to URL");
	}

	/**
	 * 基于 getURL() 返回的 URL 构建 URI
	 */
	@Override
	public URI getURI() throws IOException {
		URL url = getURL();
		try {
			return ResourceUtils.toURI(url);
		}
		catch (URISyntaxException ex) {
			throw new NestedIOException("Invalid URI [" + url + "]", ex);
		}
	}

	/**
	 * 抛出 FileNotFoundException 异常，交给子类实现
	 */
	@Override
	public File getFile() throws IOException {
		throw new FileNotFoundException(getDescription() + " cannot be resolved to absolute file path");
	}

	/**
	 * 根据 getInputStream() 的返回结果构建 ReadableByteChannel
	 */
	@Override
	public ReadableByteChannel readableChannel() throws IOException {
		return Channels.newChannel(getInputStream());
	}

	/**
	 * 获取资源的长度
	 * 这个资源内容长度实际就是资源的字节长度，通过全部读取一遍来判断
	 */
	@Override
	public long contentLength() throws IOException {
		InputStream is = getInputStream();
		try {
			long size = 0;
			byte[] buf = new byte[256];
			int read;
			while ((read = is.read(buf)) != -1) {
				size += read;
			}
			return size;
		}
		finally {
			try {
				is.close();
			}
			catch (IOException ex) {
			}
		}
	}

	 /**
	 * 返回资源最后的修改时间
	 */
	@Override
	public long lastModified() throws IOException {
		File fileToCheck = getFileForLastModifiedCheck();
		long lastModified = fileToCheck.lastModified();
		if (lastModified == 0L && !fileToCheck.exists()) {
			throw new FileNotFoundException(getDescription() +
					" cannot be resolved in the file system for checking its last-modified timestamp");
		}
		return lastModified;
	}


	protected File getFileForLastModifiedCheck() throws IOException {
		return getFile();
	}

	/**
	 * 根据资源的相对路径创建新资源
	 * 抛出 FileNotFoundException 异常，交给子类实现
	 */
	@Override
	public Resource createRelative(String relativePath) throws IOException {
		throw new FileNotFoundException("Cannot create a relative resource for " + getDescription());
	}

	/**
	 * 获取资源名称，默认返回 null ，交给子类实现
	 */
	@Override
	@Nullable
	public String getFilename() {
		return null;
	}


	/**
	 * This implementation compares description strings.
	 * @see #getDescription()
	 */
	@Override
	public boolean equals(Object other) {
		return (this == other || (other instanceof Resource &&
				((Resource) other).getDescription().equals(getDescription())));
	}

	/**
	 * This implementation returns the description's hash code.
	 * @see #getDescription()
	 */
	@Override
	public int hashCode() {
		return getDescription().hashCode();
	}

	/**
	 * This implementation returns the description of this resource.
	 * @see #getDescription()
	 */
	@Override
	public String toString() {
		return getDescription();
	}

}
```

它的子类有

- FileSystemResource ：对 `java.io.File` 类型资源的封装
- ByteArrayResource ：对字节数组提供的数据的封装
- UrlResource ：对 `java.net.URL`类型资源的封装
- ClassPathResource ：class path 类型资源的实现
- InputStreamResource ：将给定的 InputStream 作为一种资源的 Resource 的实现类

## 小结

我们再来看这两张图片是不是感觉很舒服了

![](https://github.com/esmusssein777/study/blob/master/md/picture/Resource.png?raw=true)

![](https://github.com/esmusssein777/study/blob/master/md/picture/ResourceLoader.png?raw=true)

再看前面的这段话

- Spring 将资源定义成 Resource，由 ResourceLoader 来加载。过程举个例子就是  xml 文件的位置 —> ResourceLoader —> Resource的流程。

- Resource 接口的默认实现是 AbstractResource  ，是一个抽象骨架类，它对 Resource 接口做了一个统一的实现。
- ResourceLoader 接口的默认实现是 DefaultResourceLoader ，在自定义 ResourceLoader 的时候我们除了可以继承该类外还可以实现 ProtocolResolver 接口来实现自定资源加载协议。
- DefaultResourceLoader 每次只能返回单一的资源，所以 Spring 针对这个提供了另外一个接口 ResourcePatternResolver ，该接口提供了返回多个资源的策略。

是不是已经明白了IoC的第一步定位资源的过程。

