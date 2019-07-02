# IoC的第三步—详细解析Document成BeanDefinition的过程

[TOC]

## 导言

其实这并不能算的上是第三步，还记得上一节讲到的关于 BeanDefinition 的注册过程吗？

Resource —> BeanDefinitionReader —> Document  —> BeanDefinitionDocumentReader—>  Bean Definition

![](https://github.com/esmusssein777/study/blob/master/md/picture/Resource2BeanDefinition.png?raw=true)

在上面这个过程中，我们只是详细的解释了 Resource —> BeanDefinitionReader —> Document 的过程，但是 Document  —> BeanDefinitionDocumentReader—>  Bean 的过程却没有详细的讲，因为篇幅有点长，我们单独讲。



## 解析Document

上一节有讲到，获取 XML Document 对象后，会根据该对象和 Resource 资源对象调用 `XmlBeanDefinitionReader#registerBeanDefinitions(Document doc, Resource resource)` 方法注册 BeanDefinitions 

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

其中的关键在于

```
documentReader.registerBeanDefinitions(doc, createReaderContext(resource));
```

那么我们看 documentReader 是被 createBeanDefinitionDocumentReader() 创建的，看代码我们得知，得到的是 DefaultBeanDefinitionDocumentReader。

那么我们就看`DefaultBeanDefinitionDocumentReader#registerBeanDefinitions()`方法就行。

```
	//DefaultBeanDefinitionDocumentReader.class
	public void registerBeanDefinitions(Document doc, XmlReaderContext readerContext) {
		this.readerContext = readerContext;
		// 获得 XML Document Root Element
		// 执行注册 BeanDefinition
		doRegisterBeanDefinitions(doc.getDocumentElement());
	}
	
    protected void doRegisterBeanDefinitions(Element root) {
		//BeanDefinitionParserDelegate 它负责解析 BeanDefinition
		BeanDefinitionParserDelegate parent = this.delegate;
		//1. 创建 BeanDefinitionParserDelegate 对象，并进行设置到 delegate
		this.delegate = createDelegate(getReaderContext(), root, parent);
		//2. 检查 <beans /> 根标签的命名空间是否为空
		if (this.delegate.isDefaultNamespace(root)) {
			// 处理 profile 属性
			String profileSpec = root.getAttribute(PROFILE_ATTRIBUTE);
			if (StringUtils.hasText(profileSpec)) {
				// 使用分隔符切分，可能有多个 profile
				String[] specifiedProfiles = StringUtils.tokenizeToStringArray(
						profileSpec, BeanDefinitionParserDelegate.MULTI_VALUE_ATTRIBUTE_DELIMITERS);
				// 如果所有 profile 都无效，则不进行注册
				if (!getReaderContext().getEnvironment().acceptsProfiles(specifiedProfiles)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Skipped XML bean definition file due to specified profiles [" + profileSpec +
								"] not matching: " + getReaderContext().getResource());
					}
					return;
				}
			}
		}
		//3. 解析前处理
		preProcessXml(root);
		//4. 解析
		parseBeanDefinitions(root, this.delegate);
		//5. 解析后处理
		postProcessXml(root);
		this.delegate = parent;
	}
```

1. 创建 BeanDefinitionParserDelegate 对象,负责**解析 BeanDefinition**

   ```java
   protected BeanDefinitionParserDelegate createDelegate(
           XmlReaderContext readerContext, Element root, @Nullable BeanDefinitionParserDelegate parentDelegate) {
       // 创建 BeanDefinitionParserDelegate 对象
       BeanDefinitionParserDelegate delegate = new BeanDefinitionParserDelegate(readerContext);
       // 初始化默认
       delegate.initDefaults(root, parentDelegate);
       return delegate;
   }
   ```

2. 检查 `<beans />` 根标签的命名空间是否为空,判断是否配置了 `profile` 属性

3. 解析前处理，交给子类处理

4. 调用 `#parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate)` 方法，进行解析逻辑

5. 解析后处理，交给子类处理

我们于是把目光看向最关键的 `parseBeanDefinitions(root, this.delegate)`

```java
	protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
		//1. 如果根节点使用默认命名空间，执行默认解析
		if (delegate.isDefaultNamespace(root)) {
			// 遍历子节点
			NodeList nl = root.getChildNodes();
			for (int i = 0; i < nl.getLength(); i++) {
				Node node = nl.item(i);
				if (node instanceof Element) {
					Element ele = (Element) node;
					//对应配置文件式声明：<bean id="studentService" class="org.springframework.core.StudentService" />
					if (delegate.isDefaultNamespace(ele)) {
						parseDefaultElement(ele, delegate);
					}
					else {
                        // 如果该节点非默认命名空间，执行自定义解析
						//对应<tx:annotation-driven>
						delegate.parseCustomElement(ele);
					}
				}
			}
		}
		else {//2. 如果根节点非默认命名空间，执行自定义解析
			delegate.parseCustomElement(root);
		}
	}
```

上面的代码首先判断了根节点是否用了默认的命名空间，那么什么是默认空间呢？

Spring 有**两种**

- 配置文件式声明：`<bean id="*" class="*" />` 的形式是默认命名空间
- 自定义注解方式：`<tx:annotation-driven>` 

 看到上面的代码能得到

1. 如果是配置文件式声明，那么执行 `parseDefaultElement(ele, delegate);`
2. 如果是自定义注解方式，执行 `delegate.parseCustomElement(ele);`的自定义解析

下面我们针对这不同的两种情况分别的做分析。

### 解析默认文件声明

下面是 `parseDefaultElement(ele, delegate);`的具体代码

```
	private void parseDefaultElement(Element ele, BeanDefinitionParserDelegate delegate) {
		if (delegate.nodeNameEquals(ele, IMPORT_ELEMENT)) {
			importBeanDefinitionResource(ele);// 1.import
		}
		else if (delegate.nodeNameEquals(ele, ALIAS_ELEMENT)) {
			processAliasRegistration(ele);// 2.alias
		}
		else if (delegate.nodeNameEquals(ele, BEAN_ELEMENT)) {
			processBeanDefinition(ele, delegate);// 3.bean
		}
		else if (delegate.nodeNameEquals(ele, NESTED_BEANS_ELEMENT)) {
			doRegisterBeanDefinitions(ele);// 4.recurse
		}
	}
```

#### 1. 解析 import标签

使用 Spring 的同学写项目的时候一般都不会将所有的配置都写在同一个 spring.xml 里面，我们会将不同的配置拆开，有需要的时候再将它 import 进去，就像下面这样

```
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd">

    <import resource="spring.xml"/>

</beans>
```

使用 `import` 标签的方式导入其他模块的配置文件

知道了怎么使用我们看具体的代码过程,`importBeanDefinitionResource(ele);`

```
	/**
	 * 解析 import 标签
	 */
	protected void importBeanDefinitionResource(Element ele) {
		//1. 获取 resource 的属性值
		String location = ele.getAttribute(RESOURCE_ATTRIBUTE);
		// 为空，直接退出
		if (!StringUtils.hasText(location)) {
			// 使用 problemReporter 报错
			getReaderContext().error("Resource location must not be empty", ele);
			return;
		}

		//2. 解析系统属性，格式如 ："${user.dir}"
		location = getReaderContext().getEnvironment().resolveRequiredPlaceholders(location);
		// 实际 Resource 集合，即 import 的地址，有哪些 Resource 资源
		Set<Resource> actualResources = new LinkedHashSet<>(4);

		//3. 判断 location 是相对路径还是绝对路径
		boolean absoluteLocation = false;
		try {
			absoluteLocation = ResourcePatternUtils.isUrl(location) || ResourceUtils.toURI(location).isAbsolute();
		}
		catch (URISyntaxException ex) {
			// cannot convert to an URI, considering the location relative
			// unless it is the well-known Spring prefix "classpath*:"
		}

		// Absolute or relative?
		//4. 绝对路径
		if (absoluteLocation) {
			try {
				// 添加配置文件地址的 Resource 到 actualResources 中，并加载相应的 BeanDefinition 们
				int importCount = getReaderContext().getReader().loadBeanDefinitions(location, actualResources);
				if (logger.isTraceEnabled()) {
					logger.trace("Imported " + importCount + " bean definitions from URL location [" + location + "]");
				}
			}
			catch (BeanDefinitionStoreException ex) {
				getReaderContext().error(
						"Failed to import bean definitions from URL location [" + location + "]", ele, ex);
			}
		}
		else {//5. 相对路径
			try {
				int importCount;
				// 创建相对地址的 Resource
				Resource relativeResource = getReaderContext().getResource().createRelative(location);
				if (relativeResource.exists()) {// 存在
					// 加载 relativeResource 中的 BeanDefinition 们
					importCount = getReaderContext().getReader().loadBeanDefinitions(relativeResource);
					// 添加到 actualResources 中,实际 Resource 集合，即 import 的地址，有哪些 Resource 资源
					actualResources.add(relativeResource);
				}
				else {// 不存在
					// 获得根路径地址
					String baseLocation = getReaderContext().getResource().getURL().toString();
					// 添加配置文件地址的 Resource 到 actualResources 中，并加载相应的 BeanDefinition 们
					importCount = getReaderContext().getReader().loadBeanDefinitions(
							StringUtils.applyRelativePath(baseLocation, location), actualResources);
				}
				if (logger.isTraceEnabled()) {
					logger.trace("Imported " + importCount + " bean definitions from relative location [" + location + "]");
				}
			}
			catch (IOException ex) {
				getReaderContext().error("Failed to resolve current resource location", ele, ex);
			}
			catch (BeanDefinitionStoreException ex) {
				getReaderContext().error(
						"Failed to import bean definitions from relative location [" + location + "]", ele, ex);
			}
		}//6. 解析成功后，进行监听器激活处理
		Resource[] actResArray = actualResources.toArray(new Resource[0]);
		getReaderContext().fireImportProcessed(location, actResArray, extractSource(ele));
	}
```

过程大致的理解为获取 source 属性值，得到正确的资源路径，然后调用 XmlBeanDefinitionReader#loadBeanDefinitions(Resource... resources) 方法，进行递归的 BeanDefinition 加载：

1. 获取资源的路径
2. 判断资源是相对路径还是绝对路径，如果是绝对路径，则调 loadBeanDefinitions 递归调用 Bean 的解析过程，进行另一次的解析；如果是相对路径，则先计算出绝对路径得到 Resource，然后进行解析
3. 解析成功后通知监听器

#### 2. 解析 alias 标签

上面第二步是`processAliasRegistration(ele);// alias`解析 alias 别名标签。

```
	//DefaultBeanDefinitionDocumentReader.class
	/**
	 * 解析 alias 标签
	 */
	protected void processAliasRegistration(Element ele) {
		String name = ele.getAttribute(NAME_ATTRIBUTE);
		String alias = ele.getAttribute(ALIAS_ATTRIBUTE);
		boolean valid = true;
		if (!StringUtils.hasText(name)) {
			getReaderContext().error("Name must not be empty", ele);
			valid = false;
		}
		if (!StringUtils.hasText(alias)) {
			getReaderContext().error("Alias must not be empty", ele);
			valid = false;
		}
		if (valid) {
			try {
				//注册别名
				getReaderContext().getRegistry().registerAlias(name, alias);
			}
			catch (Exception ex) {
				getReaderContext().error("Failed to register alias '" + alias +
						"' for bean with name '" + name + "'", ele, ex);
			}
			getReaderContext().fireAliasRegistered(name, alias, extractSource(ele));
		}
	}
```

```

	@Override
	public void registerAlias(String name, String alias) {
		// 校验 name 、 alias
		Assert.hasText(name, "'name' must not be empty");
		Assert.hasText(alias, "'alias' must not be empty");
		synchronized (this.aliasMap) {
			// name == alias 则去掉alias
			if (alias.equals(name)) {
				this.aliasMap.remove(alias);
				if (logger.isDebugEnabled()) {
					logger.debug("Alias definition '" + alias + "' ignored since it points to same name");
				}
			}
			else {// 获取 alias 已注册的 beanName
				String registeredName = this.aliasMap.get(alias);
				// 已存在
				if (registeredName != null) {
					// 相同，则 return ，无需重复注册
					if (registeredName.equals(name)) {
						// An existing alias - no need to re-register
						return;
					}
					// 不允许覆盖，则抛出 IllegalStateException 异常
					if (!allowAliasOverriding()) {
						throw new IllegalStateException("Cannot define alias '" + alias + "' for name '" +
								name + "': It is already registered for name '" + registeredName + "'.");
					}
					if (logger.isDebugEnabled()) {
						logger.debug("Overriding alias '" + alias + "' definition for registered name '" +
								registeredName + "' with new target name '" + name + "'");
					}
				}
				// 校验，是否存在循环指向
				checkForAliasCircle(name, alias);
				this.aliasMap.put(alias, name);
				// 注册 alias
				if (logger.isTraceEnabled()) {
					logger.trace("Alias definition '" + alias + "' registered for name '" + name + "'");
				}
			}
		}
	}
```

其中就是校验 name 和 alias 标签，不同的情况执行不同的逻辑

最后注册进去 `this.aliasMap.put(alias, name);`

#### 3. 解析 bean 标签(重要)

在上面的第三步是`processBeanDefinition(ele, delegate);// 3.bean`是解析`<bean>`的过程，是最重要的解析过程了，因为我们最主要的就是解析 `bean` 最后为我们使用。

下面是`processBeanDefinition(ele, delegate);// 3.解析bean`的具体代码

```
	protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
		// 进行 bean 元素解析。
		// 1. 如果解析成功，则返回 BeanDefinitionHolder 对象。而 BeanDefinitionHolder 为 name 和 alias 的 BeanDefinition 对象
		// 如果解析失败，则返回 null 。
		BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
		if (bdHolder != null) {
			// 2. 进行自定义标签处理
			bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
			try {
				// Register the final decorated instance.
				// 3. 进行 BeanDefinition 的注册
				BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
			}
			catch (BeanDefinitionStoreException ex) {
				getReaderContext().error("Failed to register bean definition with name '" +
						bdHolder.getBeanName() + "'", ele, ex);
			}
			// Send registration event.
			// 4. 发出响应事件，通知相关的监听器，已完成该 Bean 标签的解析。
			getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
		}
	}
```

