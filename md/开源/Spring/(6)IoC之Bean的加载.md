# IoC的第四步—Bean的加载

[TOC]

## 前言

在前面我们做了那么那么多的工作，我们终于将 BeanDefinition 给注册了，回顾一下我们前面的工作

![](https://github.com/esmusssein777/study/blob/master/md/picture/XML2BeanDefinition.png?raw=true)

我们可以将前面的工作归纳成容器的初始化。经过了容器的初始化过程后，我们在 xml 文件或者通过其他类型的 bean 配置信息已经加载到了系统中了，承载的载体就是  BeanDefinition ，当我们调用 `BeanFactory `的 `getBean()` 方法时，就会开始加载 Bean 。

## 调用`BeanFactory `的 `getBean()` 方法加载Bean

先来看看这个方法

```
@Override
public Object getBean(String name) throws BeansException {
    /**
     * name:要获取Bean的名字
     * requiredType:要获取Bean的类型
     * args:创建Bean时传递的参数，这个参数仅限于创建Bean时使用
     * typeCheckOnly:是否为检查参数
     */
    return doGetBean(name, null, null, false);
}
```

继续向下去看 doGetBean 这个方法

```java
protected <T> T doGetBean(final String name, @Nullable final Class<T> requiredType,
        @Nullable final Object[] args, boolean typeCheckOnly) throws BeansException {

    // 如果 name 是 alias ，则获取对应映射的 beanName
    //1. 返回 bean 名称，剥离工厂引用前缀。
    final String beanName = transformedBeanName(name);
    Object bean;

    // Eagerly check singleton cache for manually registered singletons.
    // 从缓存中或者实例工厂中获取 Bean 对象
    Object sharedInstance = getSingleton(beanName);
    if (sharedInstance != null && args == null) {
        if (logger.isTraceEnabled()) {
            if (isSingletonCurrentlyInCreation(beanName)) {//是否正在创建
                logger.trace("Returning eagerly cached instance of singleton bean '" + beanName +
                        "' that is not fully initialized yet - a consequence of a circular reference");
            }
            else {
                logger.trace("Returning cached instance of singleton bean '" + beanName + "'");
            }
        }
        //2. 完成 FactoryBean 的相关处理，并用来获取 FactoryBean 的处理结果
        bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
    }

    else {
        // Fail if we're already creating this bean instance:
        // We're assumably within a circular reference.
        if (isPrototypeCurrentlyInCreation(beanName)) {
            //3. 因为 Spring 只解决单例模式下得循环依赖，在原型模式下如果存在循环依赖则会抛出异常。
            throw new BeanCurrentlyInCreationException(beanName);
        }

        // Check if bean definition exists in this factory.
        //4. 如果容器中没有找到，则从父类容器中加载
        BeanFactory parentBeanFactory = getParentBeanFactory();
        if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
            // Not found -> check parent.
            String nameToLookup = originalBeanName(name);
            // 如果，父类容器为 AbstractBeanFactory ，直接递归查找
            if (parentBeanFactory instanceof AbstractBeanFactory) {
                return ((AbstractBeanFactory) parentBeanFactory).doGetBean(
                        nameToLookup, requiredType, args, typeCheckOnly);
            }
            // 用明确的 args 从 parentBeanFactory 中，获取 Bean 对象
            else if (args != null) {
                // Delegation to parent with explicit args.
                return (T) parentBeanFactory.getBean(nameToLookup, args);
            }
            // 用明确的 requiredType 从 parentBeanFactory 中，获取 Bean 对象
            else if (requiredType != null) {
                // No args -> delegate to standard getBean method.
                return parentBeanFactory.getBean(nameToLookup, requiredType);
            }
            // 直接使用 nameToLookup 从 parentBeanFactory 获取 Bean 对象
            else {
                return (T) parentBeanFactory.getBean(nameToLookup);
            }
        }

        if (!typeCheckOnly) {
            //5. 如果不是仅仅做类型检查则是创建bean，这里需要记录
            markBeanAsCreated(beanName);
        }

        try {
        //6. 从容器中获取 beanName 相应的 GenericBeanDefinition 对象，并将其转换为 RootBeanDefinition 对象
            final RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
            // 检查给定的合并的 BeanDefinition
            checkMergedBeanDefinition(mbd, beanName, args);

            // Guarantee initialization of beans that the current bean depends on.
            //7. 处理所依赖的 bean
            String[] dependsOn = mbd.getDependsOn();
            if (dependsOn != null) {
                for (String dep : dependsOn) {
                    // 若给定的依赖 bean 已经注册为依赖给定的 bean
                    // 循环依赖的情况
                    if (isDependent(beanName, dep)) {
                        throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                                "Circular depends-on relationship between '" + beanName + "' and '" + dep + "'");
                    }
                    // 缓存依赖调用
                    registerDependentBean(dep, beanName);
                    try {
                        getBean(dep);
                    }
                    catch (NoSuchBeanDefinitionException ex) {
                        throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                                "'" + beanName + "' depends on missing bean '" + dep + "'", ex);
                    }
                }
            }

            // Create bean instance.
            //8. bean 实例化
            if (mbd.isSingleton()) {// 单例模式
                sharedInstance = getSingleton(beanName, () -> {
                    try {
                        return createBean(beanName, mbd, args);
                    }
                    catch (BeansException ex) {
                        // Explicitly remove instance from singleton cache: It might have been put there
                        // eagerly by the creation process, to allow for circular reference resolution.
                        // Also remove any beans that received a temporary reference to the bean.
                        // 显式从单例缓存中删除 Bean 实例
                        // 因为单例模式下为了解决循环依赖，可能他已经存在了，所以销毁它。
                        destroySingleton(beanName);
                        throw ex;
                    }
                });
                bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
            }

            else if (mbd.isPrototype()) {// 原型模式
                // It's a prototype -> create a new instance.
                Object prototypeInstance = null;
                try {
                    // 加载前置处理
                    beforePrototypeCreation(beanName);
                    // 创建 Bean 对象
                    prototypeInstance = createBean(beanName, mbd, args);
                }
                finally {
                    // 加载后缀处理
                    afterPrototypeCreation(beanName);
                }
                // 从 Bean 实例中获取对象
                bean = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
            }

            else {// 从指定的 scope 下创建 bean

                // 获得 scopeName 对应的 Scope 对象
                String scopeName = mbd.getScope();
                final Scope scope = this.scopes.get(scopeName);
                if (scope == null) {
                    throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
                }
                try {
                    // 从指定的 scope 下创建 bean
                    Object scopedInstance = scope.get(beanName, () -> {
                        // 加载前置处理
                        beforePrototypeCreation(beanName);
                        try {
                            // 创建 Bean 对象
                            return createBean(beanName, mbd, args);
                        }
                        finally {
                            // 加载后缀处理
                            afterPrototypeCreation(beanName);
                        }
                    });
                    // 从 Bean 实例中获取对象
                    bean = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
                }
                catch (IllegalStateException ex) {
                    throw new BeanCreationException(beanName,
                            "Scope '" + scopeName + "' is not active for the current thread; consider " +
                            "defining a scoped proxy for this bean if you intend to refer to it from a singleton",
                            ex);
                }
            }
        }
        catch (BeansException ex) {
            cleanupAfterBeanCreationFailure(beanName);
            throw ex;
        }
    }

    // Check if required type matches the type of the actual bean instance.
    //9. 检查需要的类型是否符合 bean 的实际类型
    if (requiredType != null && !requiredType.isInstance(bean)) {
        try {
            T convertedBean = getTypeConverter().convertIfNecessary(bean, requiredType);
            if (convertedBean == null) {
                throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
            }
            return convertedBean;
        }
        catch (TypeMismatchException ex) {
            if (logger.isTraceEnabled()) {
                logger.trace("Failed to convert bean '" + name + "' to required type '" +
                        ClassUtils.getQualifiedName(requiredType) + "'", ex);
            }
            throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
        }
    }
    return (T) bean;
}
```

可以看到，最后返回的就是一个我们需要的 Bean ,所以我们只需要关心这个方法就ok

但是这个方法非常的长，具体深究就更加的长了。所以我们得耐心一点。

### 1. 将name转换得到BenaName

当我们在 `BeanFactory `的 `getBean(String name)` 方法传入我们需要的 name 时，不一定就是传入beanName，可能是 aliasName ，也有可能是 FactoryBean ，所以这里需要调用 `#transformedBeanName(String name)` 方法，对 `name` 进行一番转换。



```
// AbstractBeanFactory.java
protected String transformedBeanName(String name) {
    return canonicalName(BeanFactoryUtils.transformedBeanName(name));
}
```

继续看 BeanFactoryUtils.transformedBeanName(name)；

```
// BeanFactoryUtils.java

//缓存已经转换好的beanName
private static final Map<String, String> transformedBeanNameCache = new ConcurrentHashMap<>();

	public static String transformedBeanName(String name) {
		Assert.notNull(name, "'name' must not be null");
		//去除 FactoryBean 的修饰符 &
		if (!name.startsWith(BeanFactory.FACTORY_BEAN_PREFIX)) {
			return name;
		}
		// computeIfAbsent 方法，分成两种情况：
		// 1. 未存在，则进行计算执行，并将结果添加到缓存、
		// 2. 已存在，则直接返回，无需计算。
		return transformedBeanNameCache.computeIfAbsent(name, beanName -> {
			do {
				beanName = beanName.substring(BeanFactory.FACTORY_BEAN_PREFIX.length());
			}
			while (beanName.startsWith(BeanFactory.FACTORY_BEAN_PREFIX));
			return beanName;
		});
	}

```

主要是会将转换的 beanName 放到 map 里面，如果下次传入同一个 name，那么不需要重新转换，直接从缓存里面获取就行，得到的 name 经过 SimpleAliasRegistry.canonicalName(name) 得到beanName

```
public String canonicalName(String name) {
    // key: alias
    // value: beanNam
    String canonicalName = name;
    String resolvedName;
    // 循环，从 aliasMap 中，获取到最终的 beanName
    do {
        resolvedName = this.aliasMap.get(canonicalName);
        if (resolvedName != null) {
            canonicalName = resolvedName;
        }
    }
    while (resolvedName != null);
    return canonicalName;
}
```

这样我们就得到了 beanName。

看一下过程

![](https://github.com/esmusssein777/study/blob/master/md/picture/BeanName.png?raw=true)

### 2. 从缓存中获取 Bean

第一步得到 beanName 后，我们通过这个 BeanName 尝试得到 Bean 。

```
// 从缓存中或者实例工厂中获取 Bean 对象
Object sharedInstance = getSingleton(beanName);
if (sharedInstance != null && args == null) {
    if (logger.isTraceEnabled()) {
        if (isSingletonCurrentlyInCreation(beanName)) {//是否正在创建
            logger.trace("Returning eagerly cached instance of singleton bean '" + beanName +
                    "' that is not fully initialized yet - a consequence of a circular reference");
        }
        else {
            logger.trace("Returning cached instance of singleton bean '" + beanName + "'");
        }
    }
    // 完成 FactoryBean 的相关处理，并用来获取 FactoryBean 的处理结果
    bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
}
```

我们在使用 Spring 的时候，创建一个 bean 的时候我们都知道如果不设置 scope 的话，Bean 默认是单例的，第一次创建后 Bean 会被存入缓存当中，后面我们需要时直接从缓存中获取。所以我们首先要做的是看一看缓存当中有没有我们需要的 Bean。

#### 2.1  getSingleton(beanName)

首先呢，我们看的就是 Object sharedInstance = getSingleton(beanName) 这个方法,这个方法会到 DefaultSingletonBeanRegistry类里面的 getSingleton(String beanName, boolean allowEarlyReference)里面去。

```
/**
 * 存放的是单例 bean 的映射。
 * 对应关系为 bean name --> bean instance
 */
private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

/**
 * 存放的是 ObjectFactory，可以理解为创建单例 bean 的 factory 。
 * 对应关系是 bean name --> ObjectFactory
 **/
private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);

/**
 * 存放的是早期的 bean，对应关系也是 bean name --> bean instance。
 * bean 在创建过程中就已经加入到 earlySingletonObjects 中了
 * 所以当在 bean 的创建过程中，就可以通过 getBean() 方法获取。
 */
private final Map<String, Object> earlySingletonObjects = new HashMap<>(16);

protected Object getSingleton(String beanName, boolean allowEarlyReference) {
    // 从单例缓冲中加载 bean
    Object singletonObject = this.singletonObjects.get(beanName);
    // 缓存中的 bean 为空，且当前 bean 正在创建
    if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
        // 加锁
        synchronized (this.singletonObjects) {
            // 从 earlySingletonObjects 获取
            singletonObject = this.earlySingletonObjects.get(beanName);
            // earlySingletonObjects 中没有，且允许提前创建
            if (singletonObject == null && allowEarlyReference) {
                // 从 singletonFactories 中获取对应的 ObjectFactory
                ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
                if (singletonFactory != null) {
                    // 获得 bean
                    singletonObject = singletonFactory.getObject();
                    // 添加 bean 到 earlySingletonObjects 中
                    this.earlySingletonObjects.put(beanName, singletonObject);
                    // 从 singletonFactories 中移除对应的 ObjectFactory
                    this.singletonFactories.remove(beanName);
                }
            }
        }
    }
    return singletonObject;
}
```

这段代码和复杂，但是很重要，**这是单例的 Bean 能够解决循环依赖的关键代码**

这段代码和开头的三个 Map 的关系非常的密切。

```
// DefaultSingletonBeanRegistry.java

/**
 * 单例对象的 Cache
 * 对应关系为 bean name --> bean instance
 */
private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

/**
 * 单例对象工厂的 Cache 
 * 对应关系也是 bean name --> ObjectFactory 
 **/
private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);

/**
 * 提前曝光的单例对象的 Cache
 * 对应关系是 bean name --> early bean instance。
 */
private final Map<String, Object> earlySingletonObjects = new HashMap<>(16);
```



简单的来讲，先从 `singletonObjects` 里面去获取 singletonObject 

如果获取不到，并且**要获取的 Bean 正在创建**，那么就从 `earlySingletonObjects` 中获取 singletonObject 

如果还获取不到且**允许提前创建**。那么从 `singletonFactories` 中获取相应的 ObjectFactory 对象

如果获取到了，则调用其 `ObjectFactory#getObject(String name)` 方法，创建 Bean 对象，然后将其加入到 `earlySingletonObjects` ，然后从 `singletonFactories` 删除

##### 2.1.1 Bean 是否正在创建 

看上面的分析过程，获取很简单，直接从 Map 里面 get 就行，但是还有我们上面的黑体字部分是如何实现的呢？

判断整个工厂中是否这个 Bean 正在创建？`isSingletonCurrentlyInCreation(beanName)`方法

```
public boolean isSingletonCurrentlyInCreation(String beanName) {
    return this.singletonsCurrentlyInCreation.contains(beanName);
}

/**正在创建 Bean 的 Name. */
private final Set<String> singletonsCurrentlyInCreation =
        Collections.newSetFromMap(new ConcurrentHashMap<>(16));
```

发现也是通过一个 哈希集合的形式，正在创建就 put 进去，判断是否正在创建就 get 方法，创建完成了就 remove 就行了。

##### 2.1.2 是否允许提前创建

这个是靠我们传进来的参数决定 allowEarlyReference

```
Object getSingleton(String beanName, boolean allowEarlyReference)
```

看之前的代码，默认传进来的是 true



#### 2.2 FactoryBean 获取 Bean(getObjectForBeanInstance)

在经过了 2.1 之后，我们缓存中或者实例工厂中获取 Bean 对象 sharedInstance ，我们不知道这个 sharedInstance 究竟是我们需要的 bean 对象，还是一个原始的 FactoryBean。我们需要判断，如果确实是 FactoryBean 的话，还需要我们通过 FactoryBean 来创建 Bean

```
protected Object getObjectForBeanInstance(
        Object beanInstance, String name, String beanName, @Nullable RootBeanDefinition mbd) {
    //若为工厂类引用（name 以 & 开头） 主要是校验 beanInstance 的正确性
    if (BeanFactoryUtils.isFactoryDereference(name)) {
        // 如果是 NullBean，则直接返回
        if (beanInstance instanceof NullBean) {
            return beanInstance;
        }
        // 如果 beanInstance 不是 FactoryBean 类型，则抛出异常
        if (!(beanInstance instanceof FactoryBean)) {
            throw new BeanIsNotAFactoryException(transformedBeanName(name), beanInstance.getClass());
        }
    }

    // 到这里我们就有了一个 Bean 实例，当然该实例可能是会是是一个正常的 bean 又或者是一个 FactoryBean
    // 如果是 FactoryBean，我们则创建该 Bean 这里主要是对非 FactoryBean 类型处理。
    //如果 beanInstance 不为 FactoryBean 类型或者 name 也不是与工厂相关的，则直接返回 beanInstance 这个 Bean 对象
    if (!(beanInstance instanceof FactoryBean) || BeanFactoryUtils.isFactoryDereference(name)) {
        return beanInstance;
    }

    Object object = null;
    //若 BeanDefinition 为 null，则从缓存中加载 Bean 对象
    //使用 FactoryBean 获得 Bean 对象
    if (mbd == null) {
        object = getCachedObjectForFactoryBean(beanName);
    }
    // 若 object 依然为空，则可以确认，beanInstance 一定是 FactoryBean 。
    // 从而，使用 FactoryBean 获得 Bean 对象
    if (object == null) {
        // Return bean instance from factory.
        // containsBeanDefinition 检测 beanDefinitionMap 中也就是在所有已经加载的类中
        // 检测是否定义 beanName
        FactoryBean<?> factory = (FactoryBean<?>) beanInstance;
        // Caches object obtained from FactoryBean if it is a singleton.
        if (mbd == null && containsBeanDefinition(beanName)) {
            // 将存储 XML 配置文件的 GenericBeanDefinition 转换为 RootBeanDefinition，
            // 如果指定 BeanName 是子 Bean 的话同时会合并父类的相关属性
            mbd = getMergedLocalBeanDefinition(beanName);
        }
        // 是否是用户定义的，而不是应用程序本身定义的
        boolean synthetic = (mbd != null && mbd.isSynthetic());
        // 核心处理方法，使用 FactoryBean 获得 Bean 对象
        object = getObjectFromFactoryBean(factory, beanName, !synthetic);
    }
    return object;
}
```

* 首先是校验了`beanInstance` 的类型和 name 是否以 & 开头

* 如果前面得到的 sharedInstance 不为 FactoryBean 类型或者 name 也不是与工厂相关的，则直接返回 sharedInstance 
* 如果传入的  BeanDefinition 为空，则从下面这个 Map 中获取。如果还获取不到，那么传入的就是FactoryBean，我们将根据 FactoryBean 来获取 Bean。

```
/**
 * 缓存 FactoryBean 创建的单例 Bean 对象的映射
 * beanName ===> Bean 对象
 */
private final Map<String, Object> factoryBeanObjectCache = new ConcurrentHashMap<>(16);
```

最后发现如果真的传入的是一个 FactoryBean 的话，我们使用 FactoryBean 获得 Bean 对象。具体是通过 FactoryBeanRegistrySupport 的 getObjectFromFactoryBean方法

```
// 核心处理方法，使用 FactoryBean 获得 Bean 对象
object = getObjectFromFactoryBean(factory, beanName, !synthetic);
```

```
// FactoryBeanRegistrySupport.java

/**
 * 缓存 FactoryBean 创建的单例 Bean 对象的映射
 * beanName ===> Bean 对象
 */
private final Map<String, Object> factoryBeanObjectCache = new ConcurrentHashMap<>(16);
	
	protected Object getObjectFromFactoryBean(FactoryBean<?> factory, String beanName, boolean shouldPostProcess) {
		//为单例模式且缓存中存在
		if (factory.isSingleton() && containsSingleton(beanName)) {
			synchronized (getSingletonMutex()) {// 单例锁
				//从缓存中获取指定的 factoryBean
				Object object = this.factoryBeanObjectCache.get(beanName);
				if (object == null) {
					//2.2.1 为空，则从 FactoryBean 中获取对象
					object = doGetObjectFromFactoryBean(factory, beanName);
					// 从缓存中获取
					Object alreadyThere = this.factoryBeanObjectCache.get(beanName);
					if (alreadyThere != null) {
						object = alreadyThere;
					}
					else {//需要后续处理
						if (shouldPostProcess) {
							//2.2.2 若该 Bean 处于创建中，则返回非处理对象，而不是存储它
							if (isSingletonCurrentlyInCreation(beanName)) {
								// Temporarily return non-post-processed object, not storing it yet..
								return object;
							}
							// 单例 Bean 的前置处理
							beforeSingletonCreation(beanName);
							try {
								// 对从 FactoryBean 获取的对象进行后处理
								// 生成的对象将暴露给 bean 引用
								object = postProcessObjectFromFactoryBean(object, beanName);
							}
							catch (Throwable ex) {
								throw new BeanCreationException(beanName,
										"Post-processing of FactoryBean's singleton object failed", ex);
							}
							finally {// 单例 Bean 的后置处理
								afterSingletonCreation(beanName);
							}
						}
						//添加到 factoryBeanObjectCache 中，进行缓存
						if (containsSingleton(beanName)) {
							this.factoryBeanObjectCache.put(beanName, object);
						}
					}
				}
				return object;
			}
		}
		else {// 为空，则从 FactoryBean 中获取对象
			Object object = doGetObjectFromFactoryBean(factory, beanName);
			// 需要后续处理
			if (shouldPostProcess) {
				try {
					// 对从 FactoryBean 获取的对象进行后处理
					// 生成的对象将暴露给 bean 引用
					object = postProcessObjectFromFactoryBean(object, beanName);
				}
				catch (Throwable ex) {
					throw new BeanCreationException(beanName, "Post-processing of FactoryBean's object failed", ex);
				}
			}
			return object;
		}
	}
```

看了一波操作，还是我们熟悉的配方。

##### 2.2.1 从 FactoryBean 中获取 Bean 实例对象 doGetObjectFromFactoryBean() 

先从缓存中尝试获取，获取不到的话就从 FactoryBean 中获取 Bean 实例对象。调用的是 doGetObjectFromFactoryBean() 方法。

仔细的看这个方法，可以发现 doGetObjectFromFactoryBean 方法里面关键的就是调用 `FactoryBean.getObject()` 方法，获取 Bean 对象 。就不贴详细的代码了。

##### 2.2.2 前置后置处理

后面我们通过 isSingletonCurrentlyInCreation(beanName) 方法判断 Bean 是否正在创建？

* 若该 Bean 处于创建中，则返回非处理对象，而不是存储它
* 如果不在创建中，调用 beforeSingletonCreation 前置处理方法将它放入缓存中，标志它正在创建
* 生成的对象将暴露给 bean 引用object = postProcessObjectFromFactoryBean(object, beanName);
* 处理完成后，调用afterSingletonCreation 后置处理方法将它从缓存中 remove，标志它不在创建中



这样一来我们从缓存中尝试获取Bean，我们做了哪些呢？

![](https://github.com/esmusssein777/study/blob/master/md/picture/GetSingleton.png?raw=true)

我们通过 beanName 来获取bean，但是这个前提是缓存当中有 Bean，如果没有呢？

我们后面就会讲到。

### 3. 缓存中没有 bean

#### 3.1 检测原型模式

在缓存中尝试获取了 Bean 之后，继续往后面走

```
// AbstractBeanFactory.java
//检测
if (isPrototypeCurrentlyInCreation(beanName)) {
    //因为 Spring 只解决单例模式下得循环依赖，在原型模式下如果存在循环依赖则会抛出异常。
    throw new BeanCurrentlyInCreationException(beanName);
}
```

仔细看这个方法 isPrototypeCurrentlyInCreation(beanName)

```
			
    private final ThreadLocal<Object> prototypesCurrentlyInCreation =
new NamedThreadLocal<>("Prototype beans currently in creation");
        
    protected boolean isPrototypeCurrentlyInCreation(String beanName) {
Object curVal = this.prototypesCurrentlyInCreation.get();
return (curVal != null &&
        (curVal.equals(beanName) || (curVal instanceof Set && ((Set<?>) curVal).contains(beanName))));
}
```

这个方法是检测这个 `beanName` 是否处于原型模式下的循环依赖。检查的方法就是将它放入 ThreadLocal 里面，这个 ThreadLocal  放着正在创建的 Bean，只不过它不是一个全局的 Set，而是一个 ThreadLocal,原型模式 prototype 的定义是每次通过Spring容器获取prototype定义的bean时，容器都将创建一个新的Bean实例。所以需要的是 ThreadLocal 。

#### 3.2. parentBeanFactory从父类容器中加载

如果从单例模式没有获取 bean 则尝试从父类 beanFactory 中获取。

```
// 如果容器中没有找到，则从父类容器中加载
BeanFactory parentBeanFactory = getParentBeanFactory();
// parentBeanFactory 不为空且 beanDefinitionMap 中不存该 name 的 BeanDefinition
if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
    // 确定原始 beanName
    String nameToLookup = originalBeanName(name);
    // 如果，父类容器为 AbstractBeanFactory ，直接递归查找
    if (parentBeanFactory instanceof AbstractBeanFactory) {
        return ((AbstractBeanFactory) parentBeanFactory).doGetBean(
                nameToLookup, requiredType, args, typeCheckOnly);
    }
    // 委托给构造函数 getBean() 处理
    else if (args != null) {
        // Delegation to parent with explicit args.
        return (T) parentBeanFactory.getBean(nameToLookup, args);
    }
    // 没有 args，委托给标准的 getBean() 处理
    else if (requiredType != null) {
        return parentBeanFactory.getBean(nameToLookup, requiredType);
    }
    else {
        return (T) parentBeanFactory.getBean(nameToLookup);
    }
}
```

若 parentBeanFactory 不为空且 beanDefinitionMap 中不存该 name 的 BeanDefinition，则从 `parentBeanFactory` 中获取

#### 3.3 类型检查

继续向下

```
if (!typeCheckOnly) {
    // 如果不是仅仅做类型检查则是创建bean，这里需要记录
    markBeanAsCreated(beanName);
}
```

如果不是仅仅做类型检查，而是创建 Bean 对象，则需要调用 `#markBeanAsCreated(String beanName)` 方法，进行记录。

```
// AbstractBeanFactory.java

/**
 *  已创建 Bean 的名字集合
 */
private final Set<String> alreadyCreated = Collections.newSetFromMap(new ConcurrentHashMap<>(256));

protected void markBeanAsCreated(String beanName) {
    // 没有创建
    if (!this.alreadyCreated.contains(beanName)) {
        // 加上全局锁
        synchronized (this.mergedBeanDefinitions) {
            // 再次检查一次：DCL 双检查模式
            if (!this.alreadyCreated.contains(beanName)) {
                // 从 mergedBeanDefinitions 中删除 beanName，并在下次访问时重新创建它。
                clearMergedBeanDefinition(beanName);
                // 添加到已创建 bean 集合中
                this.alreadyCreated.add(beanName);
            }
        }
    }
}

protected void clearMergedBeanDefinition(String beanName) {
    this.mergedBeanDefinitions.remove(beanName);
}
```

#### 3.4 获取 RootBeanDefinition

```
// AbstractBeanFactory.java

// 从容器中获取 beanName 相应的 GenericBeanDefinition 对象，并将其转换为 RootBeanDefinition 对象
final RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
// 检查给定的合并的 BeanDefinition
checkMergedBeanDefinition(mbd, beanName, args);
```

调用 `#getMergedLocalBeanDefinition(String beanName)` 方法，获取相对应的 BeanDefinition 对象

```
// AbstractBeanFactory.java

/** Map from bean name to merged RootBeanDefinition. */
private final Map<String, RootBeanDefinition> mergedBeanDefinitions = new ConcurrentHashMap<>(256);

protected RootBeanDefinition getMergedLocalBeanDefinition(String beanName) throws BeansException {
    // 快速从缓存中获取，如果不为空，则直接返回
    RootBeanDefinition mbd = this.mergedBeanDefinitions.get(beanName);
    if (mbd != null) {
        return mbd;
    }
    // 获取 RootBeanDefinition，
    // 如果返回的 BeanDefinition 是子类 bean 的话，则合并父类相关属性
    return getMergedBeanDefinition(beanName, getBeanDefinition(beanName));
}
```

- 首先，直接从 `mergedBeanDefinitions` 缓存中获取相应的 RootBeanDefinition 对象，如果存在则直接返回。
- 否则，调用 `#getMergedBeanDefinition(String beanName, BeanDefinition bd)` 方法，获取 RootBeanDefinition 对象。

调用 `#checkMergedBeanDefinition()` 方法，检查给定的合并的 BeanDefinition 对象。

```
// AbstractBeanFactory.java

protected void checkMergedBeanDefinition(RootBeanDefinition mbd, String beanName, @Nullable Object[] args)
		throws BeanDefinitionStoreException {
	if (mbd.isAbstract()) {
		throw new BeanIsAbstractException(beanName);
	}
}
```

#### 3.5 处理依赖

如果一个 Bean 有依赖 Bean 的话，那么在初始化该 Bean 时是需要先初始化它所依赖的 Bean 。

```
// AbstractBeanFactory.java

// 处理所依赖的 bean
String[] dependsOn = mbd.getDependsOn();
if (dependsOn != null) {
    for (String dep : dependsOn) {
        //若给定的依赖 bean 已经注册为依赖给定的 bean
        if (isDependent(beanName, dep)) {
            throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                    "Circular depends-on relationship between '" + beanName + "' and '" + dep + "'");
        }
        //缓存依赖调用
        registerDependentBean(dep, beanName);
        try {
            //递归处理依赖 Bean
            getBean(dep);
        } catch (NoSuchBeanDefinitionException ex) {
            throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                    "'" + beanName + "' depends on missing bean '" + dep + "'", ex);
        }
    }
}
```

具体的代码还是通过两个 Map  `dependentBeanMap`、`dependenciesForBeanMap` 来处理，将该映射关系保存到两个集合来递归的处理。

看下上面的过程图

![](https://github.com/esmusssein777/study/blob/master/md/picture/RootBeanDefinition.png?raw=true)

这样我们就得到了我们暂时想要的 RootBeanDefinition

#### 3.6 不同scope 的 Bean 创建

scope 有哪些呢？

* singleton：单例模式，Spring IoC容器中只会存在一个共享的Bean实例，无论有多少个Bean引用它，始终指向同一对象。

* prototype：原型模式，每次通过Spring容器获取prototype定义的bean时，容器都将创建一个新的Bean实例，每个Bean实例都有自己的属性和状态。

* request：在一次Http请求中，容器会返回该Bean的同一实例。而对不同的Http请求则会产生新的Bean，而且该bean仅在当前Http Request内有效。

* session：在一次Http Session中，容器会返回该Bean的同一实例。而对不同的Session请求则会创建新的实例，该bean实例仅在当前Session内有效。

* global Session：在一个全局的Http Session中，容器会返回该Bean的同一个实例，仅在使用portlet context时有效

我们来分析一下

##### 3.6.1 singleton单例模式

```
if (mbd.isSingleton()) {// 单例模式
    sharedInstance = getSingleton(beanName, () -> {
        try {
            return createBean(beanName, mbd, args);
        }
        catch (BeansException ex) {
            // 显式从单例缓存中删除 Bean 实例
            // 因为单例模式下为了解决循环依赖，可能他已经存在了，所以销毁它。
            destroySingleton(beanName);
            throw ex;
        }
    });
    bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
}
```

又看到了 getSingleton ,但是和我们之前的不一样。这个方法是由 DefaultSingletonBeanRegistry类的getSingleton(String beanName, ObjectFactory<?> singletonFactory)方法来实现

```
//DefaultSingletonBeanRegistry.class
public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
    Assert.notNull(beanName, "Bean name must not be null");
    // 全局加锁
    synchronized (this.singletonObjects) {
        // 从缓存中检查一遍
        // 因为 singleton 模式其实就是复用已经创建的 bean 所以这步骤必须检查
        Object singletonObject = this.singletonObjects.get(beanName);
        if (singletonObject == null) {
            //  为空，开始加载过程
            if (this.singletonsCurrentlyInDestruction) {
                throw new BeanCreationNotAllowedException(beanName,
                        "Singleton bean creation not allowed while singletons of this factory are in destruction " +
                        "(Do not request a bean from a BeanFactory in a destroy method implementation!)");
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Creating shared instance of singleton bean '" + beanName + "'");
            }
            // 加载前置处理，前置和后置都很重要，标记bean创建到了哪一步
            beforeSingletonCreation(beanName);
            boolean newSingleton = false;
            boolean recordSuppressedExceptions = (this.suppressedExceptions == null);
            if (recordSuppressedExceptions) {
                this.suppressedExceptions = new LinkedHashSet<>();
            }
            try {
                // 初始化 bean
                // 这个过程其实是调用 createBean() 方法
                singletonObject = singletonFactory.getObject();
                newSingleton = true;
            }
            catch (IllegalStateException ex) {
                singletonObject = this.singletonObjects.get(beanName);
                if (singletonObject == null) {
                    throw ex;
                }
            }
            catch (BeanCreationException ex) {
                if (recordSuppressedExceptions) {
                    for (Exception suppressedException : this.suppressedExceptions) {
                        ex.addRelatedCause(suppressedException);
                    }
                }
                throw ex;
            }
            finally {
                if (recordSuppressedExceptions) {
                    this.suppressedExceptions = null;
                }
                //后置处理
                afterSingletonCreation(beanName);
            }
            //加入缓存中
            if (newSingleton) {
                addSingleton(beanName, singletonObject);
            }
        }
        return singletonObject;
    }
}
```

这个过程并没有真正创建 Bean 对象，仅仅只是做了一部分准备和预处理步骤。真正获取单例 bean 的方法，其实是由 `<3>` 处的 `singletonFactory.getObject()` 这部分代码块来实现，而 `singletonFactory` 由回调方法产生。

- 再次检查缓存是否已经加载过，如果已经加载了则直接返回，否则开始加载过程。
- 调用 `#beforeSingletonCreation(String beanName)` 方法，记录加载单例 bean 之前的加载状态，即前置处理。
- 调用参数传递的 ObjectFactory 的 `#getObject()` 方法，实例化 bean 。(后面还有好多要讲的)
- 调用 `#afterSingletonCreation(String beanName)` 方法，进行加载单例后的后置处理。
- 调用 `#addSingleton(String beanName, Object singletonObject)` 方法，将结果记录并加入值缓存中，同时删除加载 bean 过程中所记录的一些辅助状态

加载了单例 bean 后，调用 `#getObjectForBeanInstance(Object beanInstance, String name, String beanName, RootBeanDefinition mbd)` 方法，从 bean 实例中获取对象。该方法已经在前面有讲过了

##### 3.6.2 prototype原型模式

原型模式很简单，因为不需要缓存，直接创建Bean就行

```
// AbstractBeanFactory.java

else if (mbd.isPrototype()) {
    Object prototypeInstance = null;
    try {
       //加载前置处理
        beforePrototypeCreation(beanName);
        //创建 Bean 对象
        prototypeInstance = createBean(beanName, mbd, args);
    } finally {
       //加载后缀处理
        afterPrototypeCreation(beanName);
    }
    //从 Bean 实例中获取对象
    bean = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
}
```

它的前置和后置处理是通过 ThreadLocal 来防止重复创建，将正在创建的 bean 放入 ThreadLocal  来处理

##### 3.6.3 其他三个 scope

```
// AbstractBeanFactory.java

else {
    // 获得 scopeName 对应的 Scope 对象
    String scopeName = mbd.getScope();
    final Scope scope = this.scopes.get(scopeName);
    if (scope == null) {
        throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
    }
    try {
        // 从指定的 scope 下创建 bean
        Object scopedInstance = scope.get(beanName, () -> {
            // 加载前置处理
            beforePrototypeCreation(beanName);
            try {
                // 创建 Bean 对象
                return createBean(beanName, mbd, args);
            } finally {
                // 加载后缀处理
                afterPrototypeCreation(beanName);
            }
        });
        // 从 Bean 实例中获取对象
        bean = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
    } catch (IllegalStateException ex) {
        throw new BeanCreationException(beanName,
                "Scope '" + scopeName + "' is not active for the current thread; consider " +
                "defining a scoped proxy for this bean if you intend to refer to it from a singleton",
                ex);
    }
}
```

看到其实和 prototype 差不多，区别在于这三个获取 bean 实例是由 `Scope#get(String name, ObjectFactory<?> objectFactory)` 方法来实现。而 prototype 原型模式是通过调用 `#createBean(String beanName)` 方法，创建一个 bean 实例对象。



关于具体创建 Bean 的代码我们后面再讲，我们就暂时的复习一下我们前面的学习

![](https://github.com/esmusssein777/study/blob/master/md/picture/Scope.png?raw=true)

分析完Scope的工作后我们还有非常主要的一点没有讲，就是关于创建 Bean 的工作，我们这个留着下一节再将吧。

## 小结

来看看我们这一节的图，看完就会稍微的更加明白一点的(●ˇ∀ˇ●)

![](https://github.com/esmusssein777/study/blob/master/md/picture/Name2Scope.png?raw=true)

我们的工作暂时就分析到 CreateBean()吧。