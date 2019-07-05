# IoC的第三步—BeanDefinition的解析和注册

[TOC]

## 导言

其实这并不能算的上是第三步，还记得上一节讲到的关于 BeanDefinition 的注册过程吗？

Resource —> BeanDefinitionReader —> Document  —> BeanDefinitionDocumentReader—>  Bean Definition

![](https://github.com/esmusssein777/study/blob/master/md/picture/Resource2BeanDefinition.png?raw=true)

在上面这个过程中，我们只是详细的解释了 Resource —> BeanDefinitionReader —> Document 的过程，但是 Document  —> BeanDefinitionDocumentReader—>  BeanDefinition的过程却没有详细的讲，因为篇幅有点长，我们单独讲。



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

Spring 有两种配置方法

- 配置文件式声明：`<bean id="*" class="*" />` 的形式是默认命名空间
- 自定义注解方式：`<tx:annotation-driven>` 

 看到上面的代码能得到

1. 如果是配置文件式声明，那么执行 `parseDefaultElement(ele, delegate);`
2. 如果是自定义注解方式，执行 `delegate.parseCustomElement(ele);`的自定义解析

下面我们针对这不同的两种情况分别的做分析。

在分析前看下我们走到那里了

![](https://github.com/esmusssein777/study/blob/master/md/picture/ParseBeanDefinitions.png?raw=true)

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

在看解析最重要的 `<bean>` 标签前，我们先开看看到了哪一步了

![](https://github.com/esmusssein777/study/blob/master/md/picture/ProcessBeanDefinition.png?raw=true)

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

分为几步：

1. 调用 `parseBeanDefinitionElement` 进行解析
2. 如果  `bdHolder != null` 那么进行自定义标签处理
3. 解析完成后，调用 `registerBeanDefinition` 进行注册
4. 通知监听器，已完成解析工作

##### 3.1 调用 `parseBeanDefinitionElement` 进行解析默认标签

 ```java
/**
 * 这个方法还没有对 bean 标签进行解析，只是在解析动作之前做了一些功能架构，主要的工作有：
 * 解析 id、name 属性，确定 aliases 集合
 * 检测 beanName 是否唯一
 * 如果 id 不为空，则 beanName = id 。
 * 如果 id 为空，但是 aliases 不空，则 beanName 为 aliases 的第一个元素
 * 如果两者都为空，则根据默认规则来设置 beanName
 */
@Nullable
public BeanDefinitionHolder parseBeanDefinitionElement(Element ele, @Nullable BeanDefinition containingBean) {
    //解析 id 和 name 属性
    String id = ele.getAttribute(ID_ATTRIBUTE);//id
    String nameAttr = ele.getAttribute(NAME_ATTRIBUTE);//name

    // 计算别名集合
    List<String> aliases = new ArrayList<>();
    if (StringUtils.hasLength(nameAttr)) {
        String[] nameArr = StringUtils.tokenizeToStringArray(nameAttr, MULTI_VALUE_ATTRIBUTE_DELIMITERS);
        aliases.addAll(Arrays.asList(nameArr));
    }
    // beanName ，优先，使用 id
    String beanName = id;
    // beanName ，其次，使用 aliases 的第一个
    if (!StringUtils.hasText(beanName) && !aliases.isEmpty()) {
        beanName = aliases.remove(0);
        if (logger.isTraceEnabled()) {// 移除出别名集合
            logger.trace("No XML 'id' specified - using '" + beanName +
                    "' as bean name and " + aliases + " as aliases");
        }
    }
    // 检查 beanName 的唯一性
    if (containingBean == null) {
        checkNameUniqueness(beanName, aliases, ele);
    }
    // 解析属性，构造 AbstractBeanDefinition 对象
    AbstractBeanDefinition beanDefinition = parseBeanDefinitionElement(ele, beanName, containingBean);
    if (beanDefinition != null) {
        // beanName ，再次，使用 beanName 生成规则
        if (!StringUtils.hasText(beanName)) {
            try {
                if (containingBean != null) {
                    // 生成唯一的 beanName
                    beanName = BeanDefinitionReaderUtils.generateBeanName(
                            beanDefinition, this.readerContext.getRegistry(), true);
                }
                else {
                    beanName = this.readerContext.generateBeanName(beanDefinition);
                    // Register an alias for the plain bean class name, if still possible,
                    // if the generator returned the class name plus a suffix.
                    // This is expected for Spring 1.2/2.0 backwards compatibility.
                    String beanClassName = beanDefinition.getBeanClassName();
                    if (beanClassName != null &&
                            beanName.startsWith(beanClassName) && beanName.length() > beanClassName.length() &&
                            !this.readerContext.getRegistry().isBeanNameInUse(beanClassName)) {
                        aliases.add(beanClassName);
                    }
                }
                if (logger.isTraceEnabled()) {
                    logger.trace("Neither XML 'id' nor 'name' specified - " +
                            "using generated bean name [" + beanName + "]");
                }
            }
            catch (Exception ex) {
                error(ex.getMessage(), ele);
                return null;
            }
        }
        String[] aliasesArray = StringUtils.toStringArray(aliases);
        // 创建 BeanDefinitionHolder 对象
        return new BeanDefinitionHolder(beanDefinition, beanName, aliasesArray);
    }

    return null;
}
 ```

1. 首先是判断 id 和 name 的属性的关系，如果 `id` 不为空，则 `beanName = id`，如果 `id` 为空，但是 `aliases` 不空，则 `beanName` 为 `aliases` 的第一个元素，如果两者都为空，则根据默认规则来设置 beanName。其中的 `checkNameUniqueness` 方法确认name是否是唯一的，原理很简单，`usedNames`就是一个 Set 的集合，判断集合里面是否有重复的就行。

```java
protected void checkNameUniqueness(String beanName, List<String> aliases, Element beanElement) {
    String foundName = null;
    // 寻找是否 beanName 已经使用
    if (StringUtils.hasText(beanName) && this.usedNames.contains(beanName)) {
        foundName = beanName;
    }
    if (foundName == null) {
        foundName = CollectionUtils.findFirstMatch(this.usedNames, aliases);
    }
    // 若已使用，使用 problemReporter 提示错误
    if (foundName != null) {
        error("Bean name '" + foundName + "' is already used in this <beans> element", beanElement);
    }
    // 添加到 usedNames 集合
    this.usedNames.add(beanName);
    this.usedNames.addAll(aliases);
}
```

2. **关键的代码是 `AbstractBeanDefinition beanDefinition = parseBeanDefinitionElement(ele, beanName, containingBean);`这一句，用得到的 beanName 和对属性进行解析并封装成 AbstractBeanDefinition 实例。得到我们真正想要的  BeanDefinition。解析 bean 标签的过程其实就是构造一个 BeanDefinition 对象的过程。`<bean>`元素标签拥有的配置属性，BeanDefinition 均提供了相应的属性，与之一一对应。**。

`#parseBeanDefinitionElement(Element ele, String beanName, BeanDefinition containingBean)` 方法

```java
/**
 * Parse the bean definition itself, without regard to name or aliases. May return
 * {@code null} if problems occurred during the parsing of the bean definition.
 * 属性进行解析并封装成 AbstractBeanDefinition 实例
 */
@Nullable
public AbstractBeanDefinition parseBeanDefinitionElement(
        Element ele, String beanName, @Nullable BeanDefinition containingBean) {

    this.parseState.push(new BeanEntry(beanName));
    // 解析 class 属性
    String className = null;
    if (ele.hasAttribute(CLASS_ATTRIBUTE)) {
        className = ele.getAttribute(CLASS_ATTRIBUTE).trim();
    }
    // 解析 parent 属性
    String parent = null;
    if (ele.hasAttribute(PARENT_ATTRIBUTE)) {
        parent = ele.getAttribute(PARENT_ATTRIBUTE);
    }

    try {
        // 创建用于承载属性的 AbstractBeanDefinition 实例
        AbstractBeanDefinition bd = createBeanDefinition(className, parent);
        // 解析默认 bean 的各种属性
        parseBeanDefinitionAttributes(ele, beanName, containingBean, bd);
        // 提取 description
        bd.setDescription(DomUtils.getChildElementValueByTagName(ele, DESCRIPTION_ELEMENT));

        // 下面的一堆是解析 <bean>......</bean> 内部的子元素，
        // 解析出来以后的信息都放到 bd 的属性中

        /**
         * 解析元数据 <meta />
         * meta ：元数据。当需要使用里面的信息时可以通过 key 获取。
         */
        parseMetaElements(ele, bd);

        /**
         * 解析 lookup-method 属性 <lookup-method />
         * <lookup-method> ：Spring 动态改变 bean 里方法的实现。
         * 方法执行返回的对象，使用 Spring 内原有的这类对象替换，通过改变方法返回值来动态改变方法。
         * 内部实现为使用 cglib 方法，重新生成子类，重写配置的方法和返回对象，达到动态改变的效果
         */
        parseLookupOverrideSubElements(ele, bd.getMethodOverrides());

        /**
         * 解析 replaced-method 属性 <replaced-method />
         * <replace-method> ：Spring 动态改变 bean 里方法的实现。
         * 需要改变的方法，使用 Spring 内原有其他类（需要继承接口support.MethodReplacer）的逻辑，
         * 替换这个方法。通过改变方法执行逻辑来动态改变方法。
         */
        parseReplacedMethodSubElements(ele, bd.getMethodOverrides());

        // 解析构造函数参数 <constructor-arg />
        parseConstructorArgElements(ele, bd);
        // 解析 property 子元素 <property />
        parsePropertyElements(ele, bd);
        // 解析 qualifier 子元素 <qualifier />
        parseQualifierElements(ele, bd);

        bd.setResource(this.readerContext.getResource());
        bd.setSource(extractSource(ele));

        return bd;
    }
    catch (ClassNotFoundException ex) {
        error("Bean class [" + className + "] not found", ele, ex);
    }
    catch (NoClassDefFoundError err) {
        error("Class that bean class [" + className + "] depends on not found", ele, err);
    }
    catch (Throwable ex) {
        error("Unexpected failure during bean definition parsing", ele, ex);
    }
    finally {
        this.parseState.pop();
    }

    return null;
}
```

又分为这么几步：

1. 创建用于承载属性的 AbstractBeanDefinition 实例
2. 再调用 `#parseBeanDefinitionAttributes`将创建好的 GenericBeanDefinition 实例当做参数，对 `bean` 标签的所有属性进行解析

###### 3.1.1 创建用于承载属性的 AbstractBeanDefinition 实例

用 `createBeanDefinition(className, parent) ` 创建用于承载属性的 AbstractBeanDefinition 实例，我们来看看 AbstractBeanDefinition 是什么，和我们最终需要的 BeanDefinition 有什么关系。

`org.springframework.beans.factory.config.BeanDefinition` ，是一个接口，它描述了一个 Bean 实例的定义，包括属性值、构造方法值和继承自它的类的更多信息。

```
# BeanDefinition.class

String SCOPE_SINGLETON = ConfigurableBeanFactory.SCOPE_SINGLETON;
String SCOPE_PROTOTYPE = ConfigurableBeanFactory.SCOPE_PROTOTYPE;


int ROLE_APPLICATION = 0;
int ROLE_SUPPORT = 1;
int ROLE_INFRASTRUCTURE = 2;

void setParentName(@Nullable String parentName);

String getParentName();

void setBeanClassName(@Nullable String beanClassName);

String getBeanClassName();

void setScope(@Nullable String scope);

String getScope();

void setLazyInit(boolean lazyInit);

boolean isLazyInit();

void setDependsOn(@Nullable String... dependsOn);

String[] getDependsOn();

void setAutowireCandidate(boolean autowireCandidate);

boolean isAutowireCandidate();

void setPrimary(boolean primary);

boolean isPrimary();

void setFactoryBeanName(@Nullable String factoryBeanName);

String getFactoryBeanName();

void setFactoryMethodName(@Nullable String factoryMethodName);

String getFactoryMethodName();

ConstructorArgumentValues getConstructorArgumentValues();

default boolean hasConstructorArgumentValues() {
	return !getConstructorArgumentValues().isEmpty();
}

MutablePropertyValues getPropertyValues();

default boolean hasPropertyValues() {
	return !getPropertyValues().isEmpty();
}

void setInitMethodName(@Nullable String initMethodName);

String getInitMethodName();

void setDestroyMethodName(@Nullable String destroyMethodName);

String getDestroyMethodName();

void setRole(int role);

int getRole();

void setDescription(@Nullable String description);

String getDescription();

boolean isSingleton();

boolean isPrototype();

boolean isAbstract();

String getResourceDescription();

BeanDefinition getOriginatingBeanDefinition();
```

这个接口长的不行。。但是和我们常用的 `<bean>` 标签的属性很像，一对比也大致的知道是什么意思了

所以。从 xml 开始， `<bean>` 标签最终是被解析成了 BeanDefinition 注册进去。

关于 BeanDefinition  的类图关系如下

![](https://github.com/esmusssein777/study/blob/master/md/picture/XMLBeanDefinition.png?raw=true)

- ChildBeanDefinition、RootBeanDefinition、GenericBeanDefinition 三者都继承 AbstractBeanDefinition 抽象类，即 AbstractBeanDefinition 对三个子类的共同的类信息进行抽象。
- 如果配置文件中定义了父 `<bean>` 和 子 `<bean>` ，则父 `<bean>` 用 RootBeanDefinition 表示，子 `<bean>` 用 ChildBeanDefinition 表示，而没有父 `<bean>` 的就使用RootBeanDefinition 表示。
- GenericBeanDefinition 为一站式服务类。

我们回到上面解析的过程，用 `createBeanDefinition(className, parent) ` 创建用于承载属性的 AbstractBeanDefinition 实例。第一步就是

```java
/**
 * 创建 GenericBeanDefinition 对象，并设置 parentName、className、beanClass 属性
 */
public static AbstractBeanDefinition createBeanDefinition(
        @Nullable String parentName, @Nullable String className, @Nullable ClassLoader classLoader) throws ClassNotFoundException {
    // 创建 GenericBeanDefinition 对象
    GenericBeanDefinition bd = new GenericBeanDefinition();
    // 设置 parentName
    bd.setParentName(parentName);
    if (className != null) {
        // 设置 beanClass
        if (classLoader != null) {
            bd.setBeanClass(ClassUtils.forName(className, classLoader));
        } // 设置 beanClassName
        else {
            bd.setBeanClassName(className);
        }
    }
    return bd;
}
```



###### 3.1.2 对 `bean` 标签的所有属性进行解析

```
// 创建用于承载属性的 AbstractBeanDefinition 实例AbstractBeanDefinition bd = createBeanDefinition(className, parent);
// 解析默认 bean 的各种属性parseBeanDefinitionAttributes(ele, beanName, containingBean, bd);
```

创建完默认的 GenericBeanDefinition 后，开始解析的过程。下面的具体代码可以看到我们解析了一些我们常用的标签属性

```java
public AbstractBeanDefinition parseBeanDefinitionAttributes(Element ele, String beanName,
        @Nullable BeanDefinition containingBean, AbstractBeanDefinition bd) {
    // 解析 scope 属性
    if (ele.hasAttribute(SINGLETON_ATTRIBUTE)) {
        error("Old 1.x 'singleton' attribute in use - upgrade to 'scope' declaration", ele);
    }
    else if (ele.hasAttribute(SCOPE_ATTRIBUTE)) {
        bd.setScope(ele.getAttribute(SCOPE_ATTRIBUTE));
    }
    else if (containingBean != null) {
        // Take default from containing bean in case of an inner bean definition.
        bd.setScope(containingBean.getScope());
    }
    // 解析 abstract 属性
    if (ele.hasAttribute(ABSTRACT_ATTRIBUTE)) {
        bd.setAbstract(TRUE_VALUE.equals(ele.getAttribute(ABSTRACT_ATTRIBUTE)));
    }
    // 解析 lazy-init 属性
    String lazyInit = ele.getAttribute(LAZY_INIT_ATTRIBUTE);
    if (isDefaultValue(lazyInit)) {
        lazyInit = this.defaults.getLazyInit();
    }
    bd.setLazyInit(TRUE_VALUE.equals(lazyInit));
    // 解析 autowire 属性
    String autowire = ele.getAttribute(AUTOWIRE_ATTRIBUTE);
    bd.setAutowireMode(getAutowireMode(autowire));

    // 解析 depends-on 属性
    if (ele.hasAttribute(DEPENDS_ON_ATTRIBUTE)) {
        String dependsOn = ele.getAttribute(DEPENDS_ON_ATTRIBUTE);
        bd.setDependsOn(StringUtils.tokenizeToStringArray(dependsOn, MULTI_VALUE_ATTRIBUTE_DELIMITERS));
    }
    // 解析 autowire-candidate 属性
    String autowireCandidate = ele.getAttribute(AUTOWIRE_CANDIDATE_ATTRIBUTE);
    if (isDefaultValue(autowireCandidate)) {
        String candidatePattern = this.defaults.getAutowireCandidates();
        if (candidatePattern != null) {
            String[] patterns = StringUtils.commaDelimitedListToStringArray(candidatePattern);
            bd.setAutowireCandidate(PatternMatchUtils.simpleMatch(patterns, beanName));
        }
    }
    else {
        bd.setAutowireCandidate(TRUE_VALUE.equals(autowireCandidate));
    }

    // 解析 primary 标签
    if (ele.hasAttribute(PRIMARY_ATTRIBUTE)) {
        bd.setPrimary(TRUE_VALUE.equals(ele.getAttribute(PRIMARY_ATTRIBUTE)));
    }

    // 解析 init-method 属性
    if (ele.hasAttribute(INIT_METHOD_ATTRIBUTE)) {
        String initMethodName = ele.getAttribute(INIT_METHOD_ATTRIBUTE);
        bd.setInitMethodName(initMethodName);
    }
    else if (this.defaults.getInitMethod() != null) {
        bd.setInitMethodName(this.defaults.getInitMethod());
        bd.setEnforceInitMethod(false);
    }

    // 解析 destroy-method 属性
    if (ele.hasAttribute(DESTROY_METHOD_ATTRIBUTE)) {
        String destroyMethodName = ele.getAttribute(DESTROY_METHOD_ATTRIBUTE);
        bd.setDestroyMethodName(destroyMethodName);
    }
    else if (this.defaults.getDestroyMethod() != null) {
        bd.setDestroyMethodName(this.defaults.getDestroyMethod());
        bd.setEnforceDestroyMethod(false);
    }

    // 解析 factory-method 属性
    if (ele.hasAttribute(FACTORY_METHOD_ATTRIBUTE)) {
        bd.setFactoryMethodName(ele.getAttribute(FACTORY_METHOD_ATTRIBUTE));
    }
    if (ele.hasAttribute(FACTORY_BEAN_ATTRIBUTE)) {
        bd.setFactoryBeanName(ele.getAttribute(FACTORY_BEAN_ATTRIBUTE));
    }

    return bd;
}
```

看到大部分的操作都是 `get` 和 `set` ，我们不一一细讲了。

###### 3.1.3 对 `meta,lookup-method,replaced-method`标签的所有属性进行解析

```java
/**
* 解析元数据 <meta />
* meta ：元数据。当需要使用里面的信息时可以通过 key 获取。
*/
parseMetaElements(ele, bd);

/**
* 解析 lookup-method 属性 <lookup-method />
* <lookup-method> ：Spring 动态改变 bean 里方法的实现。
* 方法执行返回的对象，使用 Spring 内原有的这类对象替换，通过改变方法返回值来动态改变方法。
* 内部实现为使用 cglib 方法，重新生成子类，重写配置的方法和返回对象，达到动态改变的效果
*/
parseLookupOverrideSubElements(ele, bd.getMethodOverrides());

/**
* 解析 replaced-method 属性 <replaced-method />
* <replace-method> ：Spring 动态改变 bean 里方法的实现。
* 需要改变的方法，使用 Spring 内原有其他类（需要继承接口support.MethodReplacer）的逻辑，
* 替换这个方法。通过改变方法执行逻辑来动态改变方法。
*/
parseReplacedMethodSubElements(ele, bd.getMethodOverrides());
```

代码中有部分的解释，这三个标签用的比较少，不讲为妙。

感兴趣的还是可以深究一下。

###### 3.1.4 对constructor-arg、property、qualifier标签解析

解析完上面的标签后，代码继续往后走，开始解析 constructor-arg、property、qualifier 这三个标签

```java
// 解析构造函数参数 <constructor-arg />
parseConstructorArgElements(ele, bd);
// 解析 property 子元素 <property />
parsePropertyElements(ele, bd);
// 解析 qualifier 子元素 <qualifier />
parseQualifierElements(ele, bd);
```

1. 解析` <constructor-arg /> ` 标签

```java
/**
 * 用法像这个样子
 * <bean id="People" class="org.springframework.core.service.People">
 *     <constructor-arg index="0" value="ligz"/>
 *     <constructor-arg name="age" value="18"/>
 * </bean>
 */
```
具体解析代码

```java
public void parseConstructorArgElements(Element beanEle, BeanDefinition bd) {
    NodeList nl = beanEle.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {
        Node node = nl.item(i);
        // 标签名为 constructor-arg
        if (isCandidateElement(node) && nodeNameEquals(node, CONSTRUCTOR_ARG_ELEMENT)) {
            parseConstructorArgElement((Element) node, bd);
        }
    }
}
```

```java
public void parseConstructorArgElement(Element ele, BeanDefinition bd) {
    // 提取 index、type、name 属性值
    String indexAttr = ele.getAttribute(INDEX_ATTRIBUTE);
    String typeAttr = ele.getAttribute(TYPE_ATTRIBUTE);
    String nameAttr = ele.getAttribute(NAME_ATTRIBUTE);
    if (StringUtils.hasLength(indexAttr)) {
        try {
            // 如果有 index
            int index = Integer.parseInt(indexAttr);
            if (index < 0) {
                error("'index' cannot be lower than 0", ele);
            }
            else {
                try {
                    //构造 ConstructorArgumentEntry 对象并将其加入到 ParseState 队列中。ConstructorArgumentEntry 表示构造函数的参数。
                    this.parseState.push(new ConstructorArgumentEntry(index));
                    //解析 ele 对应属性元素 解析 constructor-arg 子元素，返回结果值
                    Object value = parsePropertyValue(ele, bd, null);
                    //根据解析的属性元素构造一个 ValueHolder 对象
                    //根据解析的结果值，构造ConstructorArgumentValues.ValueHolder 实例对象，并将 type、name 设置到 ValueHolder 中
                    ConstructorArgumentValues.ValueHolder valueHolder = new ConstructorArgumentValues.ValueHolder(value);
                    if (StringUtils.hasLength(typeAttr)) {
                        valueHolder.setType(typeAttr);
                    }
                    if (StringUtils.hasLength(nameAttr)) {
                        valueHolder.setName(nameAttr);
                    }
                    valueHolder.setSource(extractSource(ele));
                    // 不允许重复指定相同参数
                    if (bd.getConstructorArgumentValues().hasIndexedArgumentValue(index)) {
                        error("Ambiguous constructor-arg entries for index " + index, ele);
                    }
                    else {// 将 ValueHolder 实例对象添加到 indexedArgumentValues 集合中
                        bd.getConstructorArgumentValues().addIndexedArgumentValue(index, valueHolder);
                    }
                }
                finally {
                    this.parseState.pop();
                }
            }
        }
        catch (NumberFormatException ex) {
            error("Attribute 'index' of tag 'constructor-arg' must be an integer", ele);
        }
    }
    else {
        try {
            this.parseState.push(new ConstructorArgumentEntry());
            // 解析 ele 对应属性元素
            Object value = parsePropertyValue(ele, bd, null);
            // 根据解析的属性元素构造一个 ValueHolder 对象
            ConstructorArgumentValues.ValueHolder valueHolder = new ConstructorArgumentValues.ValueHolder(value);
            if (StringUtils.hasLength(typeAttr)) {
                valueHolder.setType(typeAttr);
            }
            if (StringUtils.hasLength(nameAttr)) {
                valueHolder.setName(nameAttr);
            }
            valueHolder.setSource(extractSource(ele));
            // 加入到 indexedArgumentValues 中
            bd.getConstructorArgumentValues().addGenericArgumentValue(valueHolder);
        }
        finally {
            this.parseState.pop();
        }
    }
}
```

1. 首先构造 ConstructorArgumentEntry 对象并将其加入到 ParseState 队列中。ConstructorArgumentEntry 表示构造函数的参数。
2. 调用 `#parsePropertyValue(Element ele, BeanDefinition bd, String propertyName)` 方法，解析 `constructor-arg` 子元素，返回结果值
3. 根据解析的结果值，构造ConstructorArgumentValues.ValueHolder 实例对象，并将 `type`、`name` 设置到 ValueHolder 中
4. 最后，将 ValueHolder 实例对象添加到 `indexedArgumentValues` 集合中。

其中调用 `#parsePropertyValue(Element ele, BeanDefinition bd, String propertyName)` 方法，解析 `constructor-arg` 子元素，返回结果值

```java
public Object parsePropertyValue(Element ele, BeanDefinition bd, @Nullable String propertyName) {
    String elementName = (propertyName != null ?
            "<property> element for property '" + propertyName + "'" :
            "<constructor-arg> element");
    //查找子节点中，是否有 ref、value、list 等元素
    NodeList nl = ele.getChildNodes();
    Element subElement = null;
    for (int i = 0; i < nl.getLength(); i++) {
        Node node = nl.item(i);
        // meta 、description 不处理
        if (node instanceof Element && !nodeNameEquals(node, DESCRIPTION_ELEMENT) &&
                !nodeNameEquals(node, META_ELEMENT)) {
            // Child element is what we're looking for.
            if (subElement != null) {
                error(elementName + " must not contain more than one sub-element", ele);
            }
            else {
                subElement = (Element) node;
            }
        }
    }
    //是否有 ref 属性
    boolean hasRefAttribute = ele.hasAttribute(REF_ATTRIBUTE);
    //是否有 value 属性
    boolean hasValueAttribute = ele.hasAttribute(VALUE_ATTRIBUTE);
    //多个元素存在，报错，存在冲突。
    if ((hasRefAttribute && hasValueAttribute) ||
            ((hasRefAttribute || hasValueAttribute) && subElement != null)) {
        // 1. ref 和 value 都存在 2. ref he value 存在一，并且 subElement 存在
        error(elementName +
                " is only allowed to contain either 'ref' attribute OR 'value' attribute OR sub-element", ele);
    }
    //将 ref 属性值，构造为 RuntimeBeanReference 实例对象
    if (hasRefAttribute) {
        String refName = ele.getAttribute(REF_ATTRIBUTE);
        if (!StringUtils.hasText(refName)) {
            error(elementName + " contains empty 'ref' attribute", ele);
        }
        RuntimeBeanReference ref = new RuntimeBeanReference(refName);
        ref.setSource(extractSource(ele));
        return ref;
    }
    // 将 value 属性值，构造为 TypedStringValue 实例对象
    else if (hasValueAttribute) {
        TypedStringValue valueHolder = new TypedStringValue(ele.getAttribute(VALUE_ATTRIBUTE));
        valueHolder.setSource(extractSource(ele));
        return valueHolder;
    }
    // 解析子元素
    else if (subElement != null) {
        return parsePropertySubElement(subElement, bd);
    }
    else {
        // Neither child element nor "ref" or "value" attribute found.
        error(elementName + " must specify a ref or value", ele);
        return null;
    }
}
```

1. 提取 `constructor-arg` 的子元素、`ref` 属性值和 `value` 属性值，对其进行判断。以下两种情况是不允许存在的：
   1. 存在 `ref` 或者 `value` 且又有子元素。
   2. `ref` 和 `value` 属性同时存在 。
2. 若存在 `ref` 属性，则获取其值并将其封装进 `org.springframework.beans.factory.config.RuntimeBeanReference` 实例对象中。
3. 若存在 `value` 属性，则获取其值并将其封装进 `org.springframework.beans.factory.config.TypedStringValue` 实例对象中。
4. 如果子元素不为空，则调用 `#parsePropertySubElement(Element ele, BeanDefinition bd)` 方法，对于 `constructor-arg` 子元素的嵌套子元素。

第四步—对于 `constructor-arg` 子元素的嵌套子元素，需要调用 `#parsePropertySubElement(Element ele, BeanDefinition bd)` 方法，进一步处理。

```java
public Object parsePropertySubElement(Element ele, @Nullable BeanDefinition bd, @Nullable String defaultValueType) {
	if (!isDefaultNamespace(ele)) {
		return parseNestedCustomElement(ele, bd);
	} else if (nodeNameEquals(ele, BEAN_ELEMENT)) { // bean 标签
		BeanDefinitionHolder nestedBd = parseBeanDefinitionElement(ele, bd);
		if (nestedBd != null) {
			nestedBd = decorateBeanDefinitionIfRequired(ele, nestedBd, bd);
		}
		return nestedBd;
	} else if (nodeNameEquals(ele, REF_ELEMENT)) { // ref 标签
		// A generic reference to any name of any bean.
		String refName = ele.getAttribute(BEAN_REF_ATTRIBUTE);
		boolean toParent = false;
		if (!StringUtils.hasLength(refName)) {
			// A reference to the id of another bean in a parent context.
			refName = ele.getAttribute(PARENT_REF_ATTRIBUTE);
			toParent = true;
			if (!StringUtils.hasLength(refName)) {
				error("'bean' or 'parent' is required for <ref> element", ele);
				return null;
			}
		}
		if (!StringUtils.hasText(refName)) {
			error("<ref> element contains empty target attribute", ele);
			return null;
		}
		RuntimeBeanReference ref = new RuntimeBeanReference(refName, toParent);
		ref.setSource(extractSource(ele));
		return ref;
	} else if (nodeNameEquals(ele, IDREF_ELEMENT)) { // idref 标签
		return parseIdRefElement(ele);
	} else if (nodeNameEquals(ele, VALUE_ELEMENT)) { // value 标签
		return parseValueElement(ele, defaultValueType);
	} else if (nodeNameEquals(ele, NULL_ELEMENT)) { // null 标签
		// It's a distinguished null value. Let's wrap it in a TypedStringValue
		// object in order to preserve the source location.
		TypedStringValue nullHolder = new TypedStringValue(null);
		nullHolder.setSource(extractSource(ele));
		return nullHolder;
	} else if (nodeNameEquals(ele, ARRAY_ELEMENT)) { // array 标签
		return parseArrayElement(ele, bd);
	} else if (nodeNameEquals(ele, LIST_ELEMENT)) { // list 标签
		return parseListElement(ele, bd);
	} else if (nodeNameEquals(ele, SET_ELEMENT)) { // set 标签
		return parseSetElement(ele, bd);
	} else if (nodeNameEquals(ele, MAP_ELEMENT)) { // map 标签
		return parseMapElement(ele, bd);
	} else if (nodeNameEquals(ele, PROPS_ELEMENT)) { // props 标签
		return parsePropsElement(ele);
	} else { // 未知标签
		error("Unknown property sub-element: [" + ele.getNodeName() + "]", ele);
		return null;
	}
}
```

这样一来我们大致的把`解析` <constructor-arg /> ` 标签`给将清楚了。

2. 解析`property` 标签

`property` 子元素和`constructor-arg` 子元素的解析过程差不多的，感兴趣的可以自己研究。

3. 解析`<qualifier>` 标签

`<qualifier>` 标签用的不多，限于篇幅也不写在这里了，有需求的可以去看一看。

##### 3.2 decorateBeanDefinitionIfRequired 方法解析自定义标签

我们在开头的时候说到在第一步解析成功的

```java
protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
    // 进行 bean 元素解析。
    // 1. 如果解析成功，则返回 BeanDefinitionHolder 对象。而 BeanDefinitionHolder 为 name 和 alias 的 BeanDefinition 对象
    // 如果解析失败，则返回 null 。
    BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
    if (bdHolder != null) {
        //2. 进行自定义标签处理
        bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
        try {
            // Register the final decorated instance.
            //3. 进行 BeanDefinition 的注册
            BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
        }
        catch (BeanDefinitionStoreException ex) {
            getReaderContext().error("Failed to register bean definition with name '" +
                    bdHolder.getBeanName() + "'", ele, ex);
        }
        // Send registration event.
        //4. 发出响应事件，通知相关的监听器，已完成该 Bean 标签的解析。
        getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
    }
}
```

如果在对默认解析完成后，得到的 BeanDefinitionHolder 不为 null,那么进行下一步自定义解析方法decorateBeanDefinitionIfRequired 方法：

对 `<bean>` 标签里面的自定义标签进行解析。不过自定义的标签也挺复杂，而且大部分的人并不用上。暂时不讲吧。

##### 3.3 对 BeanDefinition 进行注册

我们最后一遍来看一次这段代码 DefaultBeanDefinitionDocumentReader 的 ﻿`#processBeanDefinition()` 方法，完成 Bean 标签解析的核心工作

```java
// DefaultBeanDefinitionDocumentReader.java

protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
    // 进行 bean 元素解析。
    // 如果解析成功，则返回 BeanDefinitionHolder 对象。而 BeanDefinitionHolder 为 name 和 alias 的 BeanDefinition 对象
    // 如果解析失败，则返回 null 。
    BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
    if (bdHolder != null) {
        // 进行自定义标签处理
        bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
        try {
            // 进行 BeanDefinition 的注册
            // Register the final decorated instance.
            BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
        } catch (BeanDefinitionStoreException ex) {
            getReaderContext().error("Failed to register bean definition with name '" +
                    bdHolder.getBeanName() + "'", ele, ex);
        }
        // 发出响应事件，通知相关的监听器，已完成该 Bean 标签的解析。
        // Send registration event.
        getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
    }
}
```

可以看到关键的注册代码在解析完自定义的标签后进行。

注册 BeanDefinition ，由 `BeanDefinitionReaderUtils.registerBeanDefinition()` 完成。

```java
// BeanDefinitionReaderUtils.java
 
public static void registerBeanDefinition(
        BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry)
        throws BeanDefinitionStoreException {

    // 注册 beanName
    String beanName = definitionHolder.getBeanName();
    registry.registerBeanDefinition(beanName, definitionHolder.getBeanDefinition());

    // 注册 alias
    String[] aliases = definitionHolder.getAliases();
    if (aliases != null) {
        for (String alias : aliases) {
            registry.registerAlias(beanName, alias);
        }
    }
}
```

###### 3.3.1 通过 beanName 注册

调用 BeanDefinitionRegistry 的 `#registerBeanDefinition(String beanName, BeanDefinition beanDefinition)` 方法通过 `beanName` 注册 BeanDefinition。

```java
// DefaultListableBeanFactory.java

/** 注册BeanDefinition的容器，是一个 map */
private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);

/** 注册 bean name 的集合. */
private volatile List<String> beanDefinitionNames = new ArrayList<>(256);

/** List of names of manually registered singletons, in registration order. */
private volatile Set<String> manualSingletonNames = new LinkedHashSet<>(16);

/** Cached array of bean definition names in case of frozen configuration. */
@Nullable
private volatile String[] frozenBeanDefinitionNames;

@Override
public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
        throws BeanDefinitionStoreException {

    // 校验 beanName 与 beanDefinition 非空
    Assert.hasText(beanName, "Bean name must not be empty");
    Assert.notNull(beanDefinition, "BeanDefinition must not be null");

    //1. 校验 BeanDefinition 。
    // 这是注册前的最后一次校验了，主要是对属性 methodOverrides 进行校验。
    if (beanDefinition instanceof AbstractBeanDefinition) {
        try {
            ((AbstractBeanDefinition) beanDefinition).validate();
        } catch (BeanDefinitionValidationException ex) {
            throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,
                    "Validation of bean definition failed", ex);
        }
    }

    //2. 从缓存中获取指定 beanName 的 BeanDefinition
    BeanDefinition existingDefinition = this.beanDefinitionMap.get(beanName);
    //3. 如果已经存在
    if (existingDefinition != null) {
        // 如果存在但是不允许覆盖，抛出异常
        if (!isAllowBeanDefinitionOverriding()) {
            throw new BeanDefinitionOverrideException(beanName, beanDefinition, existingDefinition);
        // 覆盖 beanDefinition 大于 被覆盖的 beanDefinition 的 ROLE ，打印 info 日志
        } else if (existingDefinition.getRole() < beanDefinition.getRole()) {
            // e.g. was ROLE_APPLICATION, now overriding with ROLE_SUPPORT or ROLE_INFRASTRUCTURE
            if (logger.isInfoEnabled()) {
                logger.info("Overriding user-defined bean definition for bean '" + beanName +
                        "' with a framework-generated bean definition: replacing [" +
                        existingDefinition + "] with [" + beanDefinition + "]");
            }
        // 覆盖 beanDefinition 与 被覆盖的 beanDefinition 不相同，打印 debug 日志
        } else if (!beanDefinition.equals(existingDefinition)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Overriding bean definition for bean '" + beanName +
                        "' with a different definition: replacing [" + existingDefinition +
                        "] with [" + beanDefinition + "]");
            }
        // 其它，打印 debug 日志
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace("Overriding bean definition for bean '" + beanName +
                        "' with an equivalent definition: replacing [" + existingDefinition +
                        "] with [" + beanDefinition + "]");
            }
        }
        // 允许覆盖，直接覆盖原有的 BeanDefinition 到 beanDefinitionMap 中。
        this.beanDefinitionMap.put(beanName, beanDefinition);
    //4. 如果未存在
    } else {
        // 检测创建 Bean 阶段是否已经开启，如果开启了则需要对 beanDefinitionMap 进行并发控制
        if (hasBeanCreationStarted()) {
            // beanDefinitionMap 为全局变量，避免并发情况
            // Cannot modify startup-time collection elements anymore (for stable iteration)
            synchronized (this.beanDefinitionMap) {
                // 添加到 BeanDefinition 到 beanDefinitionMap 中。
                this.beanDefinitionMap.put(beanName, beanDefinition);
                // 添加 beanName 到 beanDefinitionNames 中
                List<String> updatedDefinitions = new ArrayList<>(this.beanDefinitionNames.size() + 1);
                updatedDefinitions.addAll(this.beanDefinitionNames);
                updatedDefinitions.add(beanName);
                this.beanDefinitionNames = updatedDefinitions;
                // 从 manualSingletonNames 移除 beanName
                if (this.manualSingletonNames.contains(beanName)) {
                    Set<String> updatedSingletons = new LinkedHashSet<>(this.manualSingletonNames);
                    updatedSingletons.remove(beanName);
                    this.manualSingletonNames = updatedSingletons;
                }
            }
        } else {
            // Still in startup registration phase
            // 添加到 BeanDefinition 到 beanDefinitionMap 中。
            this.beanDefinitionMap.put(beanName, beanDefinition);
            // 添加 beanName 到 beanDefinitionNames 中
            this.beanDefinitionNames.add(beanName);
            // 从 manualSingletonNames 移除 beanName
            this.manualSingletonNames.remove(beanName);
        }
        
        this.frozenBeanDefinitionNames = null;
    }

    //5. 重新设置 beanName 对应的缓存
    if (existingDefinition != null || containsSingleton(beanName)) {
        resetBeanDefinition(beanName);
    }
}
```

处理过程如下：

-  对 BeanDefinition 进行校验，主要是对 AbstractBeanDefinition 的 `methodOverrides` 属性进行校验。
- 根据 `beanName` 从缓存中获取 BeanDefinition 对象。
- 如果缓存中存在，则根据 `allowBeanDefinitionOverriding` 标志来判断是否允许覆盖。如果允许则直接覆盖。否则，抛出 BeanDefinitionStoreException 异常。
- 若缓存中没有指定 beanName 的 BeanDefinition，则判断当前阶段是否已经开始了 Bean 的创建阶段？如果是，则需要对 beanDefinitionMap 进行加锁控制并发问题，否则直接设置即可
- 若缓存中存在该 `beanName` 或者单例 bean 集合中存在该 `beanName` ，则调用 `#resetBeanDefinition(String beanName)` 方法，重置 BeanDefinition 缓存。

###### 3.3.2 注册 `alias` 和 `beanName` 的映射

调用 BeanDefinitionRegistry 的 `#registerAlias(String name, String alias)` 方法，注册 `alias` 和 `beanName` 的映射关系。

```java
// SimpleAliasRegistry.java

/** 是一个 alias 映射 bean name 的map集合. */
private final Map<String, String> aliasMap = new ConcurrentHashMap<>(16);

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
        } else {
            // 获取 alias 已注册的 beanName
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
            // 注册 alias
            this.aliasMap.put(alias, name);
            if (logger.isTraceEnabled()) {
                logger.trace("Alias definition '" + alias + "' registered for name '" + name + "'");
            }
        }
    }
}
```



看完这两个的注册发现，其实 Bean Definition的注册其实就是一个 Map 里面存储着 key 为 beanName，value 存储着 beanDefinition。了解了这个就会发现这个注册很简单。其他的代码主要是做校验使用。

## 小结

这么多的解析，我们需要来小结一下，否则不知道自己已经走到哪里了。

先来看一张稍微完整的图吧

![](https://github.com/esmusssein777/study/blob/master/md/picture/BeanDefinition.png?raw=true)

我们从起始开始分析，一开始我们是从Document解析，如果是默认的文件声明，那么开始解析— >解析 import 标签 —> 解析 alias标签 —> 解析 bean标签，分析 bean 默认标签的解析过程，包括 基本属性、`meta`、`lookup-method`、`replaced-method`、`constructor-arg`、`property`、`qualifier`这些默认的标签。解析完这些默认的标签后进行 decorateBeanDefinitionIfRequired 解析自定义的 bean 标签。解析完自定义的后开始注册 BeanDefinition 到一个 Map中，完成了我们需要的任务。

我们结合前面的图来看一看

![](https://github.com/esmusssein777/study/blob/master/md/picture/XML2BeanDefinition.png?raw=true)

可以看到从 xml 配置文件的位置开始，我们做了哪些任务把它变成 BeanDefinition 并且注入到Spring里面去，可以让我们初始化成Bean。

## 自定义的配置解析(可以不看)

 回到 `DefaultBeanDefinitionDocumentReader`类的`parseBeanDefinitions(root, this.delegate)`方法

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

前面的解析只是解析了默认的 `<bean>` 标签，还有一些可以自己配置方法。回顾一下：

Spring 有两种配置方法

- 配置文件式声明：`<bean id="*" class="*" />` 的形式是默认命名空间
- 自定义注解方式：`<tx:annotation-driven>` 

 看到上面的代码能得到

1. 如果是配置文件式声明，那么执行 `parseDefaultElement(ele, delegate);`那么就到了我们上面那些代码
2. 如果是自定义注解方式，执行 `delegate.parseCustomElement(ele);`的自定义解析

parseCustomElement 方法是解析什么的呢？

比如我们使用spring的 task 任务配置，我们可能看到这样的配置

```
	<task:executor id="executor" pool-size="5" />
	<task:scheduler id="scheduler" pool-size="10" />
	<task:annotation-driven executor="executor" scheduler="scheduler" />
```

那么我们使用 `<task>` 标签需要做哪些呢？

1. 创建一个需要扩展的组件，可能就是一个普通的 Java Bean 。
2. 定义一个 XSD 文件，用于描述组件内容。比如你需要在头文件里面声明 `http://www.springframework.org/schema/task/spring-task-3.0.xsd`
3. 创建一个实现 `org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser` 接口的类，用来解析 XSD 文件中的定义和组件定义。
4. 创建一个 Handler，继承 `org.springframework.beans.factory.xml.NamespaceHandlerSupport` 抽象类 ，用于将组件注册到 Spring 容器。
5. 编写 `spring.handlers` 和 `Spring.schemas` 文件

做完这些就可以在 xml 文件里面使用自己自定义的标签，不过这样的需求很少，我们关于这块的源码也暂时不讲为妙。把注意力放在我们关键的上面可以快速让自己入门。