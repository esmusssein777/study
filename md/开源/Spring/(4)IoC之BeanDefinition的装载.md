# IoC的第二步—BeanDefinition的加载

[TOC]

## 导言

我们在上一节讲了 Resource 的“前世今生”，我们今天就来看看 Resource 的用处。我们得到的 Resource 用在哪里。

回顾一下

![](https://github.com/esmusssein777/study/blob/master/md/picture/XML2Resource.png?raw=true)

就是xml文件的位置 —> ResourceLoader —> Resource的过程。

我们大致的分析下之后的过程。

我们在前面得到了 Resource，通过 BeanDefinitionReader 得到 Document 对象。在将 Document  对象通过 BeanDefinitionReader 解析得到我们需要的 BeanDefinition。最后将 BeanDefinition 注册到容器中。

即 Resource —> BeanDefinitionReader —> Document  —> BeanDefinitionDocumentReader—>  Bean Definition

继续画我们之前的图，得到

![](https://github.com/esmusssein777/study/blob/master/md/picture/Resource2BeanDefinition.png?raw=true)

看不懂没关系。我们最后的时候还会回顾这张图。

## BeanDefinition的加载

### BeanDefinitionReader 读取 Resource

我们大致的分析下这个过程。

我们在前面得到了 Resource，通过 BeanDefinitionReader 得到 Document 对象。

我们先去 BeanDefinitionReader 中查看具体的代码。

```java
public interface BeanDefinitionReader {

	/**
	 * 返回 BeanDefinitionRegistry 以注册 BeanDefinition
	 */
	BeanDefinitionRegistry getRegistry();

	/**
	 * 返回ResourceLoader.
	 */
	@Nullable
	ResourceLoader getResourceLoader();

	/**
	 * 返回用于bean的ClassLoader
	 * Return the class loader to use for bean classes.
	 */
	@Nullable
	ClassLoader getBeanClassLoader();

	/**
	 * Return the BeanNameGenerator to use for anonymous beans
	 */
	BeanNameGenerator getBeanNameGenerator();


	/**
	 * 从指定的资源加载 BeanDefinition
	 */
	int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException;

	/**
	 * 从指定的资源加载 BeanDefinition
	 */
	int loadBeanDefinitions(Resource... resources) throws BeanDefinitionStoreException;

	/**
	 * 从指定的位置加载 BeanDefinition
	 */
	int loadBeanDefinitions(String location) throws BeanDefinitionStoreException;

	/**
	 * 从指定的位置加载 BeanDefinition
	 */
	int loadBeanDefinitions(String... locations) throws BeanDefinitionStoreException;

}
```

可以看到这个也是一个接口，和我们之前看到的 Resource 很像，它也有一个默认的实现类，也是一个抽象骨架类 AbstractBeanDefinitionReader。我们去查看这个抽象骨架类默认实现了哪些方法。

```java
	#AbstractBeanDefinitionReader.class
	
	public int loadBeanDefinitions(String location, @Nullable Set<Resource> actualResources) throws BeanDefinitionStoreException {
		// 获得 ResourceLoader 对象
		ResourceLoader resourceLoader = getResourceLoader();
		if (resourceLoader == null) {
			throw new BeanDefinitionStoreException(
					"Cannot load bean definitions from location [" + location + "]: no ResourceLoader available");
		}

		if (resourceLoader instanceof ResourcePatternResolver) {
			// Resource pattern matching available.
			try {
				// 获得 Resource 数组，因为 Pattern 模式匹配下，可能有多个 Resource 。例如说，Ant 风格的 location
				Resource[] resources = ((ResourcePatternResolver) resourceLoader).getResources(location);
				// 加载 BeanDefinition 们
				int count = loadBeanDefinitions(resources);
				if (actualResources != null) {
					// 添加到 actualResources 中
					Collections.addAll(actualResources, resources);
				}
				if (logger.isTraceEnabled()) {
					logger.trace("Loaded " + count + " bean definitions from location pattern [" + location + "]");
				}
				return count;
			}
			catch (IOException ex) {
				throw new BeanDefinitionStoreException(
						"Could not resolve bean definition resource pattern [" + location + "]", ex);
			}
		}
		else {
			// Can only load single resources by absolute URL.
			// 获得 Resource 对象
			Resource resource = resourceLoader.getResource(location);
			// 加载 BeanDefinition 们
			int count = loadBeanDefinitions(resource);
			// 添加到 actualResources 中
			if (actualResources != null) {
				actualResources.add(resource);
			}
			if (logger.isTraceEnabled()) {
				logger.trace("Loaded " + count + " bean definitions from location [" + location + "]");
			}
			return count;
		}
	}
```

结果发现 AbstractBeanDefinitionReader 只实现了 loadBeanDefinitions(String location) 这么个方法。而我们需要的 loadBeanDefinitions(Resource resource) 方法却没有看到。说明这个方法还在 AbstractBeanDefinitionReader  的子类实现。

并且我们仔细的看上面的方法，它将 location 转成了 Resource ，并交给了它的子类去解决。

于是我们只能把目光看向子类，我们以子类 XmlBeanDefinitionReader 为例来分析。

```java
/**
	 * 当前线程，正在加载的 EncodedResource 集合。
	 */
	private final ThreadLocal<Set<EncodedResource>> resourcesCurrentlyBeingLoaded =
			new NamedThreadLocal<>("XML bean definition resources currently being loaded");
			
	@Override
	public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(new EncodedResource(resource));
	}

	/**
	 * 从 XML 文件加载 bean definitions
	 */
	public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
		Assert.notNull(encodedResource, "EncodedResource must not be null");
		if (logger.isTraceEnabled()) {
			logger.trace("Loading XML bean definitions from " + encodedResource);
		}
		// 获取已经加载过的资源
		Set<EncodedResource> currentResources = this.resourcesCurrentlyBeingLoaded.get();
		if (currentResources == null) {
			currentResources = new HashSet<>(4);
			this.resourcesCurrentlyBeingLoaded.set(currentResources);
		}
		if (!currentResources.add(encodedResource)) {
			// 将当前资源加入记录中。如果已存在，抛出异常
			throw new BeanDefinitionStoreException(
					"Detected cyclic loading of " + encodedResource + " - check your import definitions!");
		}
		try {
			// 从 EncodedResource 获取封装的 Resource ，并从 Resource 中获取其中的 InputStream
			InputStream inputStream = encodedResource.getResource().getInputStream();
			try {
				InputSource inputSource = new InputSource(inputStream);
				if (encodedResource.getEncoding() != null) {// 设置编码
					inputSource.setEncoding(encodedResource.getEncoding());
				}
				// 核心逻辑部分，执行加载 BeanDefinition
				return doLoadBeanDefinitions(inputSource, encodedResource.getResource());
			}
			finally {
				inputStream.close();
			}
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException(
					"IOException parsing XML document from " + encodedResource.getResource(), ex);
		}
		finally {
			// 从缓存中剔除该资源
			currentResources.remove(encodedResource);
			if (currentResources.isEmpty()) {
				this.resourcesCurrentlyBeingLoaded.remove();
			}
		}
	}		
```

从过程来看，资源经历了从 Resource —> EncodedResource —> InputStream —> InputSource —> doLoadBeanDefinitions(inputSource, encodedResource.getResource())这么多复杂的过程。

这里为什么需要将 Resource 封装成 EncodedResource 呢？主要是为了对 Resource 进行编码，保证内容读取的正确性。

然后，再调用 `#loadBeanDefinitions(EncodedResource encodedResource)` 方法，执行真正的逻辑实现

```java
	protected int doLoadBeanDefinitions(InputSource inputSource, Resource resource)
			throws BeanDefinitionStoreException {

		try {
			/**
			 * 1. 获取 XML Document 实例
			 * 调用 #getValidationModeForResource(Resource resource) 方法，获取指定资源（xml）的验证模式
			 * 调用 DocumentLoader#loadDocument方法，获取 XML Document 实例
			 */
			Document doc = doLoadDocument(inputSource, resource);
			// 2. 根据 Document 实例，注册 Bean 信息
			int count = registerBeanDefinitions(doc, resource);
			if (logger.isDebugEnabled()) {
				logger.debug("Loaded " + count + " bean definitions from " + resource);
			}
			return count;
		}
		catch (BeanDefinitionStoreException ex) {
			throw ex;
		}
		catch (SAXParseException ex) {
			throw new XmlBeanDefinitionStoreException(resource.getDescription(),
					"Line " + ex.getLineNumber() + " in XML document from " + resource + " is invalid", ex);
		}
		catch (SAXException ex) {
			throw new XmlBeanDefinitionStoreException(resource.getDescription(),
					"XML document from " + resource + " is invalid", ex);
		}
		catch (ParserConfigurationException ex) {
			throw new BeanDefinitionStoreException(resource.getDescription(),
					"Parser configuration exception parsing XML from " + resource, ex);
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException(resource.getDescription(),
					"IOException parsing XML document from " + resource, ex);
		}
		catch (Throwable ex) {
			throw new BeanDefinitionStoreException(resource.getDescription(),
					"Unexpected exception parsing XML document from " + resource, ex);
		}
	}
```

到了这一步，我们终于看到我们之前获得的 Resource 的用处了。

1. 首先是 Resource  通过 doLoadDocument(inputSource, resource) 获得 Document。
2. 其次是 Document 通过 registerBeanDefinitions(doc, resource) 注册 BeanDefinition。

我们也能和之前的流程图对应上Resource —> BeanDefinitionReader —> Document 



### 获取Document

Resource  通过 doLoadDocument(inputSource, resource) 获得 Document。

我们看看 doLoadDocument 的具体实现

```java
	protected Document doLoadDocument(InputSource inputSource, Resource resource) throws Exception {
		return this.documentLoader.loadDocument(inputSource, getEntityResolver(), this.errorHandler,
				getValidationModeForResource(resource), isNamespaceAware());
	}
```

1. 先调用 `#getValidationModeForResource(Resource resource)` 方法，获取指定资源（xml）的验证模式
2. 调用 `DocumentLoader#loadDocument`方法，获取 XML Document 实例

#### 获取验证模型

什么是验证模式呢？

你可能见过这样的代码

```
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE beans PUBLIC  "-//SPRING//DTD BEAN//EN"  "http://www.springframework.org/dtd/spring-beans.dtd">
```

在Spring 的 XML配置文件中，出现这样的声明，声明这个文件是DTD(Document Type Definition)，即文档类型定义，为 XML 文件的验证机制，属于 XML 文件中组成的一部分。

还有与之相应的XSD 验证模式。

具体的代码感兴趣的同学可以去深究一波，这里就不展开来讲了。你只需要知道通过调用 `#getValidationModeForResource(Resource resource)` 方法获得验证模型，可以来看文档是否符合规范，元素和标签使用是否正确。

#### 获取 XML Document 实例

上面我们可以看到 documentLoader.loadDocument(...) 方法获取 Document 。这个 loadDocument  方法由接口 `org.springframework.beans.factory.xml.DocumentLoader` 定义。

```java
/**
 * 定义从资源文件加载到转换为 Document 的功能。
 */
public interface DocumentLoader {
	/**
	 * Load a {@link Document document} from the supplied {@link InputSource source}.
	 * @param inputSource 加载 Document 的 Resource 资源。
	 * @param entityResolver 解析文件的解析器。
	 * @param errorHandler 处理加载 Document 对象的过程的错误。
	 * @param validationMode 验证模式
	 * @param namespaceAware 命名空间支持。如果要提供对 XML 名称空间的支持，则需要值为 true
	 * @return the loaded {@link Document document}
	 * @throws Exception if an error occurs
	 */
	Document loadDocument(
			InputSource inputSource, EntityResolver entityResolver,
			ErrorHandler errorHandler, int validationMode, boolean namespaceAware)
			throws Exception;
}
```

可以看到这些需要的参数我们前面都得到了，其中 EntityResolver 的得到是通过 `getEntityResolver()` 获得，目的在于如何获取【验证文件】，从而验证用户写的 XML 是否通过验证。这里我们不展开细讲，感兴趣的同学可以深入研究。我们的重点在于，如何获得 Document 

它的实现是在 `DocumentLoader` 的子类 `DefaultDocumentLoader` 类里面，我们查看具体的代码

```java
@Override
	public Document loadDocument(InputSource inputSource, EntityResolver entityResolver,
			ErrorHandler errorHandler, int validationMode, boolean namespaceAware) throws Exception {
		// 创建 DocumentBuilderFactory
		DocumentBuilderFactory factory = createDocumentBuilderFactory(validationMode, namespaceAware);
		if (logger.isTraceEnabled()) {
			logger.trace("Using JAXP provider [" + factory.getClass().getName() + "]");
		}
		// 创建 DocumentBuilder
		DocumentBuilder builder = createDocumentBuilder(factory, entityResolver, errorHandler);
		// 解析 XML InputSource 返回 Document 对象
		return builder.parse(inputSource);
	}

	/**
	 * 创建 DocumentBuilderFactory
	 */
	protected DocumentBuilderFactory createDocumentBuilderFactory(int validationMode, boolean namespaceAware)
			throws ParserConfigurationException {
		// 创建 DocumentBuilderFactory
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		// 设置命名空间支持
		factory.setNamespaceAware(namespaceAware);

		if (validationMode != XmlValidationModeDetector.VALIDATION_NONE) {
			factory.setValidating(true);//开启校验
			//XSD模式下，设置factory的属性
			if (validationMode == XmlValidationModeDetector.VALIDATION_XSD) {
				// XSD 模式下，强制设置命名空间支持
				factory.setNamespaceAware(true);
				try {// 设置 SCHEMA_LANGUAGE_ATTRIBUTE
					factory.setAttribute(SCHEMA_LANGUAGE_ATTRIBUTE, XSD_SCHEMA_LANGUAGE);
				}
				catch (IllegalArgumentException ex) {
					ParserConfigurationException pcex = new ParserConfigurationException(
							"Unable to validate using XSD: Your JAXP provider [" + factory +
							"] does not support XML Schema. Are you running on Java 1.4 with Apache Crimson? " +
							"Upgrade to Apache Xerces (or Java 1.5) for full XSD support.");
					pcex.initCause(ex);
					throw pcex;
				}
			}
		}

		return factory;
	}

	/**
	 * 创建 DocumentBuilder
	 */
	protected DocumentBuilder createDocumentBuilder(DocumentBuilderFactory factory,
			@Nullable EntityResolver entityResolver, @Nullable ErrorHandler errorHandler)
			throws ParserConfigurationException {
		// 创建 DocumentBuilder 对象
		DocumentBuilder docBuilder = factory.newDocumentBuilder();
		if (entityResolver != null) {// <x> 设置 EntityResolver 属性
			docBuilder.setEntityResolver(entityResolver);
		}
		if (errorHandler != null) {// 设置 ErrorHandler 属性
			docBuilder.setErrorHandler(errorHandler);
		}
		return docBuilder;
	}
```

具体过程

1. 调用 #`createDocumentBuilderFactory(...)` 方法，创建 `javax.xml.parsers.DocumentBuilderFactory` 对象
2. 调用 `#createDocumentBuilder(DocumentBuilderFactory factory, EntityResolver entityResolver,ErrorHandler errorHandler)` 方法，创建 `javax.xml.parsers.DocumentBuilder` 对象
3. 调用 `DocumentBuilder#parse(InputSource)` 方法，解析 InputSource ，返回 Document 对象

就这样，我们终于终于获得了 Document 对象。然后我们需要做的是通过 Document 对象获得  BeanDefinition 然后将他注册。我们就完成了这一阶段的使命。

看下我们走到哪啦？

Resource —> BeanDefinitionReader —> Document  —> BeanDefinitionDocumentReader—>  Bean Definition

我们整个的流程也已经走完了一半！

## BeanDefinition的注册

上面讲了如何从 Resource 到 Document 的详细过程，接下来分析的是如何将得到的 Document 去得到 BeanDefinition 并将它注册。

我们在上面的代码中 XmlBeanDefinitionReader 的 doLoadDocument 方法获得了 Document

```
Document doc = doLoadDocument(inputSource, resource)
```

再通过执行 registerBeanDefinitions 方法将 Document 注册

```java
// 2. 根据 Document 实例，注册 Bean 信息
int count = registerBeanDefinitions(doc, resource);
```

我们仔细的看下具体代码

```java
	public int registerBeanDefinitions(Document doc, Resource resource) throws BeanDefinitionStoreException {
		// 创建 BeanDefinitionDocumentReader 对象
		BeanDefinitionDocumentReader documentReader = createBeanDefinitionDocumentReader();
		// 获取已注册的 BeanDefinition 数量
		int countBefore = getRegistry().getBeanDefinitionCount();
		// 创建 XmlReaderContext 对象
		// 注册 BeanDefinition
		documentReader.registerBeanDefinitions(doc, createReaderContext(resource));
		// 计算新注册的 BeanDefinition 数量
		return getRegistry().getBeanDefinitionCount() - countBefore;
	}
```

1. BeanDefinitionDocumentReader 用来从给定的 Document 对象中解析定义的 BeanDefinition 并将他们注册到注册表中。createBeanDefinitionDocumentReader() 创建默认的 BeanDefinitionDocumentReader 
2. documentReader.registerBeanDefinitions(doc, createReaderContext(resource)) 方法才是真正的解析 Document 到 BeanDefinition 的过程。

解析的过程很复杂，我们下一节来专门将解析的过程，到底是如何从 Document  解析到 BeanDefinition 的。

我们只需记住，经过了 XmlBeanDefinitionReader 的 doLoadDocument 和 registerBeanDefinitions两个方法，我们完成了从Resource 到 Document 再到完成 BeanDefinition 的注册就行。

Resource —> BeanDefinitionReader —> Document  —> BeanDefinitionDocumentReader—>  Bean Definition

这个流程就走完了。

## 小结

我们在这一节里面讲了如何从 Resource 到 Document 的详细过程。再得到 BeanDefinition 并将它注册的过程。

我们再来看这个流程图是不是已经很清晰了

![](https://github.com/esmusssein777/study/blob/master/md/picture/Resource2BeanDefinition.png?raw=true)

我们后面先不急着讲从 BeanDefinition 到  Bean 的过程。我们继续我们没有讲清楚的内容。到底是如何从 Document  解析到 BeanDefinition 的。

 