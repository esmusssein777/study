# IoC第五步—Bean的创建

[TOC]

## 前言

在 bean 的加载时候，我们从不同的 Scope 的创建 Bean 的过程中，发现研究到 `#createBean(String beanName, RootBeanDefinition mbd, Object[] args)` 方法后我们停止了。

因为这个方法也有很多要讲的，我们放在这一节来讲。



![](https://github.com/esmusssein777/study/blob/master/md/picture/Name2Scope.png?raw=true)

```
	// AbstractBeanFactory.java
	/**
	 * @param beanName bean 的名字
	 * @param mbd 已经合并了父类属性的（如果有的话）BeanDefinition 对象
	 * @param args 用于构造函数或者工厂方法创建 Bean 实例对象的参数
	 *
	 * 根据给定的 BeanDefinition 和 args 实例化一个 Bean 对象
	 */
	protected abstract Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
			throws BeanCreationException;
```

该抽象方法的默认实现是在类 AbstractAutowireCapableBeanFactory 中实现

```
	/**
	 * @param beanName bean 的名字
	 * @param mbd 已经合并了父类属性的（如果有的话）BeanDefinition 对象
	 * @param args 用于构造函数或者工厂方法创建 Bean 实例对象的参数
	 *
	 * 根据给定的 BeanDefinition 和 args 实例化一个 Bean 对象
	 */
	@Override
	protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
			throws BeanCreationException {

		if (logger.isTraceEnabled()) {
			logger.trace("Creating instance of bean '" + beanName + "'");
		}
		RootBeanDefinition mbdToUse = mbd;

		// 解析指定 BeanDefinition 的 class 属性。
		// 如果获取的class 属性不为null，则克隆该 BeanDefinition
		// 主要是因为该动态解析的 class 无法保存到到共享的 BeanDefinition
		Class<?> resolvedClass = resolveBeanClass(mbd, beanName);
		if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
			mbdToUse = new RootBeanDefinition(mbd);
			mbdToUse.setBeanClass(resolvedClass);
		}

		// Prepare method overrides.
		try {
			// 验证和准备覆盖方法 处理 override 属性。
			mbdToUse.prepareMethodOverrides();
		}
		catch (BeanDefinitionValidationException ex) {
			throw new BeanDefinitionStoreException(mbdToUse.getResourceDescription(),
					beanName, "Validation of method overrides failed", ex);
		}

		try {
			// 实例化的前置处理
			// 给 BeanPostProcessors 一个机会用来返回一个代理类而不是真正的类实例
			// AOP 的功能就是基于这个地方
			Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
			if (bean != null) {
				return bean;
			}
		}
		catch (Throwable ex) {
			throw new BeanCreationException(mbdToUse.getResourceDescription(), beanName,
					"BeanPostProcessor before instantiation of bean failed", ex);
		}

		try {
			//创建 Bean 对象
			Object beanInstance = doCreateBean(beanName, mbdToUse, args);
			if (logger.isTraceEnabled()) {
				logger.trace("Finished creating instance of bean '" + beanName + "'");
			}
			return beanInstance;
		}
		catch (BeanCreationException | ImplicitlyAppearedSingletonException ex) {
			// A previously detected exception with proper bean creation context already,
			// or illegal singleton state to be communicated up to DefaultSingletonBeanRegistry.
			throw ex;
		}
		catch (Throwable ex) {
			throw new BeanCreationException(
					mbdToUse.getResourceDescription(), beanName, "Unexpected exception during bean creation", ex);
		}
	}
```

我们按照顺序来看看代码

```
		// 解析指定 BeanDefinition 的 class 属性。
		// 如果获取的class 属性不为null，则克隆该 BeanDefinition
		// 主要是因为该动态解析的 class 无法保存到到共享的 BeanDefinition
		Class<?> resolvedClass = resolveBeanClass(mbd, beanName);
		if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
			mbdToUse = new RootBeanDefinition(mbd);
			mbdToUse.setBeanClass(resolvedClass);
		}
```

这段代码主要是解析指定 BeanDefinition 的 class。`#resolveBeanClass(final RootBeanDefinition mbd, String beanName, final Class<?>... typesToMatch)` 方法，主要是解析 bean definition 的 class 类，并将已经解析的 Class 存储在 bean definition 中以供后面使用。

接下来的代码

```
		// Prepare method overrides.
		try {
			// 验证和准备覆盖方法 处理 override 属性。
			mbdToUse.prepareMethodOverrides();
		}
		catch (BeanDefinitionValidationException ex) {
			throw new BeanDefinitionStoreException(mbdToUse.getResourceDescription(),
					beanName, "Validation of method overrides failed", ex);
		}

		try {
			// 实例化的前置处理
			// 给 BeanPostProcessors 一个机会用来返回一个代理类而不是真正的类实例
			// AOP 的功能就是基于这个地方
			Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
			if (bean != null) {
				return bean;
			}
		}
		catch (Throwable ex) {
			throw new BeanCreationException(mbdToUse.getResourceDescription(), beanName,
					"BeanPostProcessor before instantiation of bean failed", ex);
		}
```

`mbdToUse.prepareMethodOverrides()` 代码块，只是对 `methodOverrides` 属性做了一些简单的校验而已。

后面这个处理就比较重要了。我们单独来讲吧

## 前置后置处理

我们具体的看 resolveBeforeInstantiation(beanName, mbdToUse); 方法

```
// AbstractAutowireCapableBeanFactory.java

@Nullable
protected Object resolveBeforeInstantiation(String beanName, RootBeanDefinition mbd) {
    Object bean = null;
    if (!Boolean.FALSE.equals(mbd.beforeInstantiationResolved)) {
        // Make sure bean class is actually resolved at this point.
        if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
            Class<?> targetType = determineTargetType(beanName, mbd);
            if (targetType != null) {
                // 前置
                bean = applyBeanPostProcessorsBeforeInstantiation(targetType, beanName);
                if (bean != null) {
                    // 后置
                    bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
                }
            }
        }
        mbd.beforeInstantiationResolved = (bean != null);
    }
    return bean;
}
```

Spring 的容器设计的很好，它有很多的扩展的地方，这里的 BeanPostProcessor 前置后置处理就是典型的扩展 Bean 的地方。我们先来了解一下 BeanPostProcessor 

BeanPostProcessor 的作用：在 Bean 完成实例化后，对其进行一些配置、增加一些自己的处理逻辑等

`org.springframework.beans.factory.config.BeanPostProcessor` 接口，代码如下：

```
public interface BeanPostProcessor {
	default Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	default Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}
}
```

可以看到这是一个接口，就是实现对实例化 bean 阶段的前置处理和后置处理

BeanPostProcessor 可以理解为是 Spring 的一个工厂 hook（其实 Spring 提供一系列的 hook，如 Aware 、InitializingBean、DisposableBean），允许 Spring 在实例化 bean 阶段对其进行定制化修改，比较常见的使用场景是处理标记接口实现类或者为当前对象提供代理实现（例如 AOP）

来看看一张关于Spring 的周期的一张图

![](http://static2.iocoder.cn/images/Spring/2018-12-24/08.png)

可以看到 postProcessBeforeInitialization  和 postProcessAfterInitialization 的方法使用时间，我们看看这些不同的定制化的 hook 有什么不同吧

* Spring对Bean进行实例化

* Spring将值和Bean的引用注入进Bean对应的属性中

* 容器通过Aware接口把容器信息注入Bean

* BeanPostProcessor。进行进一步的构造，会在InitialzationBean前后执行对应方法，当前正在初始化的bean对象会被传递进来，我们就可以对这个bean作任何处理

* InitializingBean。这一阶段也可以在bean正式构造完成前增加我们自定义的逻辑，但它与前置处理不同，由于该函数并不会把当前bean对象传进来，因此在这一步没办法处理对象本身，只能增加一些额外的逻辑。

* DisposableBean。Bean将一直驻留在应用上下文中给应用使用，直到应用上下文被销毁，如果Bean实现了接口，Spring将调用它的destory方法

我们这里还是主要谈 BeanPostProcessor 的前置后置处理的代码。

```
// AbstractAutowireCapableBeanFactory.java

@Override
public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName)
		throws BeansException {
	Object result = existingBean;
	// 遍历 BeanPostProcessor 数组
	for (BeanPostProcessor processor : getBeanPostProcessors()) {
	    // 处理
		Object current = processor.postProcessBeforeInitialization(result, beanName);
        // 返回空，则返回 result
		if (current == null) {
			return result;
		}
		// 修改 result
		result = current;
	}
	return result;
}

@Override
public Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName)
		throws BeansException {
	Object result = existingBean;
	// 遍历 BeanPostProcessor
	for (BeanPostProcessor processor : getBeanPostProcessors()) {
	    // 处理
		Object current = processor.postProcessAfterInitialization(result, beanName);
		// 返回空，则返回 result
		if (current == null) {
			return result;
		}
		// 修改 result
		result = current;
	}
	return result;
}
```

代码很简单，唯一需要注意的地方是BeanFactory 和 ApplicationContext 对 BeanPostProcessor 的处理不同，ApplicationContext 会自动检测所有实现了 BeanPostProcessor 接口的 bean，并完成注册，但是使用 BeanFactory 容器时则需要手动调用 `AbstractBeanFactory#addBeanPostProcessor(BeanPostProcessor beanPostProcessor)` 方法来完成注册

我们暂时回顾一下做过的工作

![](https://github.com/esmusssein777/study/blob/master/md/picture/CreateBean.png?raw=true)

接下来是重头戏，真正的创建 Bean。

## 创建 Bean

我们第一段代码在处理完前置后置处理后，就是真正创建 Bean 的逻辑代码了

```
		try {
			//创建 Bean 对象
			Object beanInstance = doCreateBean(beanName, mbdToUse, args);
			if (logger.isTraceEnabled()) {
				logger.trace("Finished creating instance of bean '" + beanName + "'");
			}
			return beanInstance;
		}
```

调用的是 doCreateBean 方法。这又是一段很长的代码，需要一些耐心

```
	protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final @Nullable Object[] args)
			throws BeanCreationException {

		// BeanWrapper 是对 Bean 的包装，其接口中所定义的功能很简单包括设置获取被包装的对象，获取被包装 bean 的属性描述器
		BeanWrapper instanceWrapper = null;
		//单例模型，则从未完成的 FactoryBean 缓存中删除
		if (mbd.isSingleton()) {
			instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
		}
		//使用合适的实例化策略来创建新的实例：工厂方法、构造函数自动注入、简单初始化
		if (instanceWrapper == null) {
			instanceWrapper = createBeanInstance(beanName, mbd, args);
		}
		// 包装的实例对象
		final Object bean = instanceWrapper.getWrappedInstance();
		// 包装的实例对象的类型
		Class<?> beanType = instanceWrapper.getWrappedClass();
		if (beanType != NullBean.class) {
			mbd.resolvedTargetType = beanType;
		}

		//判断是否有后置处理
		//如果有后置处理，则允许后置处理修改 BeanDefinition
		synchronized (mbd.postProcessingLock) {
			if (!mbd.postProcessed) {
				try {
					// 后置处理修改 BeanDefinition
					applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
				}
				catch (Throwable ex) {
					throw new BeanCreationException(mbd.getResourceDescription(), beanName,
							"Post-processing of merged bean definition failed", ex);
				}
				mbd.postProcessed = true;
			}
		}

		//解决单例模式的循环依赖
		boolean earlySingletonExposure = (mbd.isSingleton()// 单例模式
				&& this.allowCircularReferences &&// 运行循环依赖
				isSingletonCurrentlyInCreation(beanName));// 当前单例 bean 是否正在被创建
		if (earlySingletonExposure) {
			if (logger.isTraceEnabled()) {
				logger.trace("Eagerly caching bean '" + beanName +
						"' to allow for resolving potential circular references");
			}
			// 提前将创建的 bean 实例加入到 singletonFactories 中
			// 这里是为了后期避免循环依赖
			addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
		}

		// 开始初始化 bean 实例对象
		Object exposedObject = bean;
		try {
			//对 bean 进行填充，将各个属性值注入，其中，可能存在依赖于其他 bean 的属性
			// 则会递归初始依赖 bean
			populateBean(beanName, mbd, instanceWrapper);
			//调用初始化方法
			exposedObject = initializeBean(beanName, exposedObject, mbd);
		}
		catch (Throwable ex) {
			if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
				throw (BeanCreationException) ex;
			}
			else {
				throw new BeanCreationException(
						mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
			}
		}

		if (earlySingletonExposure) {//循环依赖处理
			// 获取 earlySingletonReference
			Object earlySingletonReference = getSingleton(beanName, false);
			// 只有在存在循环依赖的情况下，earlySingletonReference 才不会为空
			if (earlySingletonReference != null) {
				// 如果 exposedObject 没有在初始化方法中被改变，也就是没有被增强
				if (exposedObject == bean) {
					exposedObject = earlySingletonReference;
				}
				// 处理依赖
				else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
					String[] dependentBeans = getDependentBeans(beanName);
					Set<String> actualDependentBeans = new LinkedHashSet<>(dependentBeans.length);
					for (String dependentBean : dependentBeans) {
						if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
							actualDependentBeans.add(dependentBean);
						}
					}
					if (!actualDependentBeans.isEmpty()) {
						throw new BeanCurrentlyInCreationException(beanName,
								"Bean with name '" + beanName + "' has been injected into other beans [" +
								StringUtils.collectionToCommaDelimitedString(actualDependentBeans) +
								"] in its raw version as part of a circular reference, but has eventually been " +
								"wrapped. This means that said other beans do not use the final version of the " +
								"bean. This is often the result of over-eager type matching - consider using " +
								"'getBeanNamesOfType' with the 'allowEagerInit' flag turned off, for example.");
					}
				}
			}
		}

		//注册 bean
		try {
			registerDisposableBeanIfNecessary(beanName, bean, mbd);
		}
		catch (BeanDefinitionValidationException ex) {
			throw new BeanCreationException(
					mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
		}

		return exposedObject;
	}
```

### (一) 实例化 Bean

首先是创建一个 BeanWrapper 为 null

如果 RootBeanDefinition 是一个单例模型，从未完成的 FactoryBean 缓存中删除

```
BeanWrapper instanceWrapper = null;
		//单例模型，则从未完成的 FactoryBean 缓存中删除
		if (mbd.isSingleton()) {
			instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
		
```

接下来是 createBeanInstance 方法，将 BeanDefinition 转换为 `org.springframework.beans.BeanWrapper` 对象

BeanWrapper 是对 Bean 的包装，其接口中所定义的功能很简单包括设置获取被包装的对象，获取被包装 bean 的属性描述器

```
//使用合适的实例化策略来创建新的实例：工厂方法、构造函数自动注入、简单初始化
if (instanceWrapper == null) {
	instanceWrapper = createBeanInstance(beanName, mbd, args);
}
```



#### createBeanInstance 将 BeanDefinition 转换为 BeanWrapper 

```
// AbstractAutowireCapableBeanFactory.java

protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) {
    // 解析 bean ，将 bean 类名解析为 class 引用。
    Class<?> beanClass = resolveBeanClass(mbd, beanName);

    if (beanClass != null && !Modifier.isPublic(beanClass.getModifiers()) && !mbd.isNonPublicAccessAllowed()) { // 校验
        throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                "Bean class isn't public, and non-public access not allowed: " + beanClass.getName());
    }

    //如果存在 Supplier 回调，则使用给定的回调方法初始化策略
    Supplier<?> instanceSupplier = mbd.getInstanceSupplier();
    if (instanceSupplier != null) {
        return obtainFromSupplier(instanceSupplier, beanName);
    }

    //使用 FactoryBean 的 factory-method 来创建，支持静态工厂和实例工厂
    if (mbd.getFactoryMethodName() != null)  {
        return instantiateUsingFactoryMethod(beanName, mbd, args);
    }

    boolean resolved = false;
    boolean autowireNecessary = false;
    if (args == null) {
        // constructorArgumentLock 构造函数的常用锁
        synchronized (mbd.constructorArgumentLock) {
            // 如果已缓存的解析的构造函数或者工厂方法不为空，则可以利用构造函数解析
            // 因为需要根据参数确认到底使用哪个构造函数，该过程比较消耗性能，所有采用缓存机制
            if (mbd.resolvedConstructorOrFactoryMethod != null) {
                resolved = true;
                autowireNecessary = mbd.constructorArgumentsResolved;
            }
        }
    }
    // 已经解析好了，直接注入即可
    if (resolved) {
        //autowire 自动注入，调用构造函数自动注入
        if (autowireNecessary) {
            return autowireConstructor(beanName, mbd, null, null);
        } else {
            //使用默认构造函数构造
            return instantiateBean(beanName, mbd);
        }
    }

    //确定解析的构造函数?
    // 主要是检查已经注册的 SmartInstantiationAwareBeanPostProcessor
    Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
    //有参数情况时，创建 Bean 。先利用参数个数，类型等，确定最精确匹配的构造方法。
    if (ctors != null || mbd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR ||
            mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args))  {
        return autowireConstructor(beanName, mbd, ctors, args);
    }

    //选择构造方法，创建 Bean 。
    ctors = mbd.getPreferredConstructors();
    if (ctors != null) {
        return autowireConstructor(beanName, mbd, ctors, null); // args = null
    }

    //有参数时，又没获取到构造方法，则只能调用无参构造方法来创建实例了
    return instantiateBean(beanName, mbd);
}
```

##### obtainFromSupplier

    //如果存在 Supplier 回调，则使用给定的回调方法初始化策略
    Supplier<?> instanceSupplier = mbd.getInstanceSupplier();
    if (instanceSupplier != null) {
        return obtainFromSupplier(instanceSupplier, beanName);
    }
具体的看这个代码

```
// AbstractAutowireCapableBeanFactory.java

/**
 * 当前线程，正在创建的 Bean 对象的名字
 */
private final NamedThreadLocal<String> currentlyCreatedBean = new NamedThreadLocal<>("Currently created bean");

protected BeanWrapper obtainFromSupplier(Supplier<?> instanceSupplier, String beanName) {
    Object instance;
    // 获得原创建的 Bean 的对象名
    String outerBean = this.currentlyCreatedBean.get();
    // 设置新的 Bean 的对象名，到 currentlyCreatedBean 中
    this.currentlyCreatedBean.set(beanName);
    try {
        //调用 Supplier 的 get()，返回一个 Bean 对象
        instance = instanceSupplier.get();
    } finally {
        // 设置原创建的 Bean 的对象名，到 currentlyCreatedBean 中
        if (outerBean != null) {
            this.currentlyCreatedBean.set(outerBean);
        } else {
            this.currentlyCreatedBean.remove();
        }
    }

    // 未创建 Bean 对象，则创建 NullBean 对象
    if (instance == null) {
        instance = new NullBean();
    }
    //创建 BeanWrapper 对象
    BeanWrapper bw = new BeanWrapperImpl(instance);
    //初始化 BeanWrapper 对象
    initBeanWrapper(bw);
    return bw;
}
```

我们看关键的代码是

```
//调用 Supplier 的 get()，返回一个 Bean 对象
instance = instanceSupplier.get();
```

那么 Supplier<?> instanceSupplier 是什么呢？

`java.util.function.Supplier` 接口，代码如下：

```
public interface Supplier<T> {
    T get();
}
```

这个接口就一个 get 方法有什么作用？用于指定创建 bean 的回调。如果我们设置了这样的回调，那么其他的构造器或者工厂方法都会没有用。

##### instantiateUsingFactoryMethod 工厂方法创建Bean

```
   //使用 FactoryBean 的 factory-method 来创建，支持静态工厂和实例工厂
    if (mbd.getFactoryMethodName() != null)  {
        return instantiateUsingFactoryMethod(beanName, mbd, args);
    }
```

​	它调用了`org.springframework.expression.ConstructorResolver`的 instantiateUsingFactoryMethod 方法，这个方法非常的复杂。看的我云里雾里。大致的就是获取构造函数和参数然后来创建 Bean 实例。

如何创建的呢？

调用 `org.springframework.beans.factory.support.InstantiationStrategy` 对象的 `instantiate(RootBeanDefinition bd, String beanName, BeanFactory owner, Object factoryBean, final Method factoryMethod, @Nullable Object... args)` 方法，来创建 bean 实例。这个方法主要就是利用 Java 反射来创建实例。

代码巨长无比，还是自行研究比较好

##### autowireConstructor 带参数的构造器初始化 Bean

```
// AbstractAutowireCapableBeanFactory.java
protected BeanWrapper autowireConstructor(String beanName, RootBeanDefinition mbd, @Nullable Constructor<?>[] ctors, @Nullable Object[] explicitArgs) {
    return new ConstructorResolver(this).autowireConstructor(beanName, mbd, ctors, explicitArgs);
}

// ConstructorResolver.java
public BeanWrapper autowireConstructor(String beanName, RootBeanDefinition mbd,
        @Nullable Constructor<?>[] chosenCtors, @Nullable Object[] explicitArgs) {...}
```

autowireConstructor 这个方法和前面讲的 instantiateUsingFactoryMethod的具体实现差不多，确定构造函数参数、构造函数，然后调用相应的初始化策略进行 bean 的初始化。关于如何确定构造函数、构造参数和 `#instantiateUsingFactoryMethod(...)` 方法差不多。

instantiateUsingFactoryMethod ，autowireConstructor 这两个方法之所以如此的复杂，是因为参数和构造函数没有确认，必须要花上大把的精力来满足不同的情况，才能确认参数和构造函数的具体情况。如果不需要确认这么多的复杂情况，那么直接使用默认的构造函数就行。

```
//使用默认构造函数构造
return instantiateBean(beanName, mbd);
```

这个方法就可以放上来了

```
// AbstractAutowireCapableBeanFactory.java

protected BeanWrapper instantiateBean(final String beanName, final RootBeanDefinition mbd) {
    try {
        Object beanInstance;
        final BeanFactory parent = this;
        // 安全模式
        if (System.getSecurityManager() != null) {
            beanInstance = AccessController.doPrivileged((PrivilegedAction<Object>) () ->
                    // 获得 InstantiationStrategy 对象，并使用它，创建 Bean 对象
                    getInstantiationStrategy().instantiate(mbd, beanName, parent),
                    getAccessControlContext());
        } else {
            // 获得 InstantiationStrategy 对象，并使用它，创建 Bean 对象
            beanInstance = getInstantiationStrategy().instantiate(mbd, beanName, parent);
        }
        // 封装 BeanWrapperImpl  并完成初始化
        BeanWrapper bw = new BeanWrapperImpl(beanInstance);
        initBeanWrapper(bw);
        return bw;
    } catch (Throwable ex) {
        throw new BeanCreationException(
                mbd.getResourceDescription(), beanName, "Instantiation of bean failed", ex);
    }
}
```

关键的是 

```
// 获得 InstantiationStrategy 对象，并使用它，创建 Bean 对象
beanInstance = getInstantiationStrategy().instantiate(mbd, beanName, parent);
```

仔细研究这个代码，看到它是根据不同的策略有不一样的实现方法

```
// SimpleInstantiationStrategy.java

@Override
public Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner) {
    // Don't override the class with CGLIB if no overrides.
    // 没有覆盖，直接使用反射实例化即可
    if (!bd.hasMethodOverrides()) {
        Constructor<?> constructorToUse;
        synchronized (bd.constructorArgumentLock) {
            // 获得构造方法 constructorToUse
            constructorToUse = (Constructor<?>) bd.resolvedConstructorOrFactoryMethod;
            if (constructorToUse == null) {
                final Class<?> clazz = bd.getBeanClass();
                // 如果是接口，抛出 BeanInstantiationException 异常
                if (clazz.isInterface()) {
                    throw new BeanInstantiationException(clazz, "Specified class is an interface");
                }
                try {
                    // 从 clazz 中，获得构造方法
                    if (System.getSecurityManager() != null) { // 安全模式
                        constructorToUse = AccessController.doPrivileged(
                                (PrivilegedExceptionAction<Constructor<?>>) clazz::getDeclaredConstructor);
                    } else {
                        constructorToUse =  clazz.getDeclaredConstructor();
                    }
                    // 标记 resolvedConstructorOrFactoryMethod 属性
                    bd.resolvedConstructorOrFactoryMethod = constructorToUse;
                } catch (Throwable ex) {
                    throw new BeanInstantiationException(clazz, "No default constructor found", ex);
                }
            }
        }
        // 通过 BeanUtils 直接使用构造器对象实例化 Bean 对象
        return BeanUtils.instantiateClass(constructorToUse);
    } else {
        // Must generate CGLIB subclass.
        // 生成 CGLIB 创建的子类对象
        return instantiateWithMethodInjection(bd, beanName, owner);
    }
}
```

具体的不同策略是` !bd.hasMethodOverrides()`判断是否有需要覆盖或者动态替换掉的方法。如果不存在覆盖的话直接利用反射的方式，否则如果是存在覆盖的情况需要创建动态代理将方法织入，这个时候就只能选择 CGLIB 的方式来实例化。

###### 反射创建Bean

```
// BeanUtils.java

public static <T> T instantiateClass(Constructor<T> ctor, Object... args) throws BeanInstantiationException {
    Assert.notNull(ctor, "Constructor must not be null");
    try {
        // 设置构造方法，可访问
        ReflectionUtils.makeAccessible(ctor);
        // 使用构造方法，创建对象
        return (KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(ctor.getDeclaringClass()) ?
                KotlinDelegate.instantiateClass(ctor, args) : ctor.newInstance(args));
    // 各种异常的翻译，最终统一抛出 BeanInstantiationException 异常
    } catch (InstantiationException ex) {
       ...
       ...
    }
}
```

###### CGLIB 创建Bean

```
// SimpleInstantiationStrategy.java

protected Object instantiateWithMethodInjection(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner) {
	throw new UnsupportedOperationException("Method Injection not supported in SimpleInstantiationStrategy");
}


// CglibSubclassingInstantiationStrategy.java
@Override
protected Object instantiateWithMethodInjection(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner) {
    return instantiateWithMethodInjection(bd, beanName, owner, null);
}
@Override
protected Object instantiateWithMethodInjection(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner, @Nullable Constructor<?> ctor, Object... args) {
    // Must generate CGLIB subclass...
    // 通过CGLIB生成一个子类对象
    return new CglibSubclassCreator(bd, owner).instantiate(ctor, args);
}
```

实现是

```
// CglibSubclassingInstantiationStrategy.java

public Object instantiate(@Nullable Constructor<?> ctor, Object... args) {
    // 通过 Cglib 创建一个代理类
    Class<?> subclass = createEnhancedSubclass(this.beanDefinition);
    Object instance;
    // 没有构造器，通过 BeanUtils 使用默认构造器创建一个bean实例
    if (ctor == null) {
        instance = BeanUtils.instantiateClass(subclass);
    } else {
        try {
            // 获取代理类对应的构造器对象，并实例化 bean
            Constructor<?> enhancedSubclassConstructor = subclass.getConstructor(ctor.getParameterTypes());
            instance = enhancedSubclassConstructor.newInstance(args);
        } catch (Exception ex) {
            throw new BeanInstantiationException(this.beanDefinition.getBeanClass(),
                    "Failed to invoke constructor for CGLIB enhanced subclass [" + subclass.getName() + "]", ex);
        }
    }

    // 为了避免 memory leaks 异常，直接在 bean 实例上设置回调对象
    Factory factory = (Factory) instance;
    factory.setCallbacks(new Callback[] {NoOp.INSTANCE,
            new LookupOverrideMethodInterceptor(this.beanDefinition, this.owner),
            new ReplaceOverrideMethodInterceptor(this.beanDefinition, this.owner)});
    return instance;
}
```

深究 CGLIB 的代码的话又要很久了，这里使用的就是策略模式。判断是否覆盖方法的实现，如果没有覆盖，直接反射即可。如果覆盖了，那么需要CGLIB 的方式来实例化

回顾一下上面的过程

![](https://github.com/esmusssein777/study/blob/master/md/picture/BeanWrapper.png?raw=true)

### (二) 循环依赖处理

我们在 IoC 之 Bean 的加载时的 2.1节 getSingleton(beanName) 时已经讲过如何处理依赖循环的事了，但是讲的不多，可能没有讲的不清楚。

这里再结合循环依赖的情况多说一些。

循环依赖出现的情况就是当多个的 Bean 互相的引用了，比如学生引用课程，课程引用老师，老师又引用学生。当初始化 Student 时 引用 Class, 初始化 Class 时引用 Teacher， 初始化 Teather 时又回到要初始化 Student，这样就永远的初始化不出来了。

那么Spring 是怎么解决的呢？

我们在 IoC 之 Bean 的加载时的 2.1节 getSingleton(beanName) 时又讲到过这三个Map

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

这三个 Map 就相当于 三个缓存

- 一级缓存 `singletonObjects`
- 二级缓存 `earlySingletonObjects`
- 三级缓存 `singletonFactories`

看下面的代码来理解

```
// DefaultSingletonBeanRegistry.java

@Nullable
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

这一段代码很清楚的解释了这三个 Map 是如何作用的。

1. 首先从一级缓存 `singletonObjects`中获取
2. 没有得到，且当前指定的 `beanName` 正在创建，那么就再从二级缓存 `earlySingletonObjects` 中获取
3. 如果还是没有获取到且允许 `singletonFactories` 通过 `#getObject()` 获取，则从三级缓存 `singletonFactories` 获取。如果获取到，则通过其 `#getObject()` 方法，获取对象，并将其加入到二级缓存 `earlySingletonObjects` 中，并从三级缓存 `singletonFactories` 删除

所以呢，当 Spring 创建 Bean 时，会提前的将 创建中的 bean 加入到 `singletonFactories` 缓存中。下次再初始化的时候，就不需要再次创建该 bean 了，直接的从  `singletonFactories` 获取就行。如果创建完成了，则会从三级缓存  `singletonFactories` 删除，将它加入二级缓存 `earlySingletonObjects`中。下次创建时直接从二级缓存中获取

看完了上面的解释再来看 doCreateBean 方法的循环依赖的解决就能通俗易懂了

```
//解决单例模式的循环依赖
		boolean earlySingletonExposure = (mbd.isSingleton()// 单例模式
				&& this.allowCircularReferences &&// 运行循环依赖
				isSingletonCurrentlyInCreation(beanName));// 当前单例 bean 是否正在被创建
		if (earlySingletonExposure) {
			if (logger.isTraceEnabled()) {
				logger.trace("Eagerly caching bean '" + beanName +
						"' to allow for resolving potential circular references");
			}
			// 提前将创建的 bean 实例加入到 singletonFactories 中
			// 这里是为了后期避免循环依赖
			addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
		}

		// 开始初始化 bean 实例对象
		Object exposedObject = bean;
		try {
			//对 bean 进行填充，将各个属性值注入，其中，可能存在依赖于其他 bean 的属性
			// 则会递归初始依赖 bean
			populateBean(beanName, mbd, instanceWrapper);
			//调用初始化方法
			exposedObject = initializeBean(beanName, exposedObject, mbd);
		}
		catch (Throwable ex) {
			if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
				throw (BeanCreationException) ex;
			}
			else {
				throw new BeanCreationException(
						mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
			}
		}

		if (earlySingletonExposure) {//循环依赖处理
			// 获取 earlySingletonReference
			Object earlySingletonReference = getSingleton(beanName, false);
			// 只有在存在循环依赖的情况下，earlySingletonReference 才不会为空
			if (earlySingletonReference != null) {
				// 如果 exposedObject 没有在初始化方法中被改变，也就是没有被增强
				if (exposedObject == bean) {
					exposedObject = earlySingletonReference;
				}
				// 处理依赖
				else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
					String[] dependentBeans = getDependentBeans(beanName);
					Set<String> actualDependentBeans = new LinkedHashSet<>(dependentBeans.length);
					for (String dependentBean : dependentBeans) {
						if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
							actualDependentBeans.add(dependentBean);
						}
					}
					if (!actualDependentBeans.isEmpty()) {
						throw new BeanCurrentlyInCreationException(beanName,
								"Bean with name '" + beanName + "' has been injected into other beans [" +
								StringUtils.collectionToCommaDelimitedString(actualDependentBeans) +
								"] in its raw version as part of a circular reference, but has eventually been " +
								"wrapped. This means that said other beans do not use the final version of the " +
								"bean. This is often the result of over-eager type matching - consider using " +
								"'getBeanNamesOfType' with the 'allowEagerInit' flag turned off, for example.");
					}
				}
			}
		}
```

这里面循环依赖的解释了，还有一些其它的代码

```
		// 开始初始化 bean 实例对象
		Object exposedObject = bean;
		try {
			//对 bean 进行填充，将各个属性值注入，其中，可能存在依赖于其他 bean 的属性
			// 则会递归初始依赖 bean
			populateBean(beanName, mbd, instanceWrapper);
			//调用初始化方法
			exposedObject = initializeBean(beanName, exposedObject, mbd);
		}
```

就是关于属性填充的代码，将 RootBeanDefinition 的属性填充到 BeanWrapper 中。

还有调用初始化的方法 exposedObject = initializeBean(beanName, exposedObject, mbd)

我们具体的看看这两个方法

### (三) BeanWrapper 属性填充

我们具体的看看 populateBean(beanName, mbd, instanceWrapper); 方法

```
// AbstractAutowireCapableBeanFactory.java

protected void populateBean(String beanName, RootBeanDefinition mbd, @Nullable BeanWrapper bw) {
    // 没有实例化对象
    if (bw == null) {
        // 有属性，则抛出 BeanCreationException 异常
        if (mbd.hasPropertyValues()) {
            throw new BeanCreationException(
                    mbd.getResourceDescription(), beanName, "Cannot apply property values to null instance");
            // 没有属性，直接 return 返回
        } else {
            // Skip property population phase for null instance.
            return;
        }
    }

    //在设置属性之前给 InstantiationAwareBeanPostProcessors 最后一次改变 bean 的机会
    boolean continueWithPropertyPopulation = true;
    if (!mbd.isSynthetic()  // bean 不是"合成"的，即未由应用程序本身定义
            && hasInstantiationAwareBeanPostProcessors()) { // 是否持有 InstantiationAwareBeanPostProcessor
        // 迭代所有的 BeanPostProcessors
        for (BeanPostProcessor bp : getBeanPostProcessors()) {
            if (bp instanceof InstantiationAwareBeanPostProcessor) { // 如果为 InstantiationAwareBeanPostProcessor
                InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                // 返回值为是否继续填充 bean
                // postProcessAfterInstantiation：如果应该在 bean上面设置属性则返回 true，否则返回 false
                // 一般情况下，应该是返回true 。
                // 返回 false 的话，将会阻止在此 Bean 实例上调用任何后续的 InstantiationAwareBeanPostProcessor 实例。
                if (!ibp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
                    continueWithPropertyPopulation = false;
                    break;
                }
            }
        }
    }
    // 如果后续处理器发出停止填充命令，则终止后续操作
    if (!continueWithPropertyPopulation) {
        return;
    }

    // bean 的属性值
    PropertyValues pvs = (mbd.hasPropertyValues() ? mbd.getPropertyValues() : null);

    //自动注入
    if (mbd.getResolvedAutowireMode() == AUTOWIRE_BY_NAME || mbd.getResolvedAutowireMode() == AUTOWIRE_BY_TYPE) {
        // 将 PropertyValues 封装成 MutablePropertyValues 对象
        // MutablePropertyValues 允许对属性进行简单的操作，并提供构造函数以支持Map的深度复制和构造。
        MutablePropertyValues newPvs = new MutablePropertyValues(pvs);
        // Add property values based on autowire by name if applicable.
        // 根据名称自动注入
        if (mbd.getResolvedAutowireMode() == AUTOWIRE_BY_NAME) {
            autowireByName(beanName, mbd, bw, newPvs);
        }
        // Add property values based on autowire by type if applicable.
        // 根据类型自动注入
        if (mbd.getResolvedAutowireMode() == AUTOWIRE_BY_TYPE) {
            autowireByType(beanName, mbd, bw, newPvs);
        }
        pvs = newPvs;
    }

    // 是否已经注册了 InstantiationAwareBeanPostProcessors
    boolean hasInstAwareBpps = hasInstantiationAwareBeanPostProcessors();
    // 是否需要进行【依赖检查】
    boolean needsDepCheck = (mbd.getDependencyCheck() != AbstractBeanDefinition.DEPENDENCY_CHECK_NONE);

    //BeanPostProcessor 处理
    PropertyDescriptor[] filteredPds = null;
    if (hasInstAwareBpps) {
        if (pvs == null) {
            pvs = mbd.getPropertyValues();
        }
        // 遍历 BeanPostProcessor 数组
        for (BeanPostProcessor bp : getBeanPostProcessors()) {
            if (bp instanceof InstantiationAwareBeanPostProcessor) {
                InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                // 对所有需要依赖检查的属性进行后处理
                PropertyValues pvsToUse = ibp.postProcessProperties(pvs, bw.getWrappedInstance(), beanName);
                if (pvsToUse == null) {
                    // 从 bw 对象中提取 PropertyDescriptor 结果集
                    // PropertyDescriptor：可以通过一对存取方法提取一个属性
                    if (filteredPds == null) {
                        filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
                    }
                    pvsToUse = ibp.postProcessPropertyValues(pvs, filteredPds, bw.getWrappedInstance(), beanName);
                    if (pvsToUse == null) {
                        return;
                    }
                }
                pvs = pvsToUse;
            }
        }
    }
    
    //依赖检查
    if (needsDepCheck) {
        if (filteredPds == null) {
            filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
        }
        // 依赖检查，对应 depends-on 属性
        checkDependencies(beanName, mbd, filteredPds, pvs);
    }

    //将属性应用到 bean 中
    if (pvs != null) {
        applyPropertyValues(beanName, mbd, bw, pvs);
    }
}
```

#### autowireByName和autowireByType

上面的代码首先是根据 `hasInstantiationAwareBeanPostProcessors` 属性来判断，是否需要在注入属性之前给 InstantiationAwareBeanPostProcessors 最后一次改变 bean 的机会。**此过程可以控制 Spring 是否继续进行属性填充**

然后统一存入到 PropertyValues 中，PropertyValues 用于描述 bean 的属性。具体是根据注入类型( `AbstractBeanDefinition#getResolvedAutowireMode()` 方法的返回值 )的不同来判断

`#autowireByName(String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs)` 方法，是根据**属性名称**，完成自动依赖注入的

`#autowireByType(String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs)` 方法，是根据**属性类型**，完成自动依赖注入的

代码如下：

```
// AbstractAutowireCapableBeanFactory.java

protected void autowireByName(String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {
    //对 Bean 对象中非简单属性
    String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
    //遍历 propertyName 数组
    for (String propertyName : propertyNames) {
        // 如果容器中包含指定名称的 bean，则将该 bean 注入到 bean中
        if (containsBean(propertyName)) {
            // 递归初始化相关 bean
            Object bean = getBean(propertyName);
            // 为指定名称的属性赋予属性值
            pvs.add(propertyName, bean);
            // 属性依赖注入
            registerDependentBean(propertyName, beanName);
            if (logger.isTraceEnabled()) {
                logger.trace("Added autowiring by name from bean name '" + beanName +
                        "' via property '" + propertyName + "' to bean named '" + propertyName + "'");
            }
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace("Not autowiring property '" + propertyName + "' of bean '" + beanName +
                        "' by name: no matching bean found");
            }
        }
    }
}
```

```
// AbstractAutowireCapableBeanFactory.java

protected void autowireByType(String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {

    // 获取 TypeConverter 实例
    // 使用自定义的 TypeConverter，用于取代默认的 PropertyEditor 机制
    TypeConverter converter = getCustomTypeConverter();
    if (converter == null) {
        converter = bw;
    }

    Set<String> autowiredBeanNames = new LinkedHashSet<>(4);
    // 获取非简单属性
    String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
    // 遍历 propertyName 数组
    for (String propertyName : propertyNames) {
        try {
            // 获取 PropertyDescriptor 实例
            // 不要尝试按类型
            if (Object.class != pd.getPropertyType()) {
                // 探测指定属性的 set 方法
                MethodParameter methodParam = BeanUtils.getWriteMethodParameter(pd);
                boolean eager = !PriorityOrdered.class.isInstance(bw.getWrappedInstance());
                DependencyDescriptor desc = new AutowireByTypeDependencyDescriptor(methodParam, eager);
                // 解析指定 beanName 的属性所匹配的值，并把解析到的属性名称存储在 autowiredBeanNames 中
                // 当属性存在过个封装 bean 时将会找到所有匹配的 bean 并将其注入
                Object autowiredArgument = resolveDependency(desc, beanName, autowiredBeanNames, converter);
                if (autowiredArgument != null) {
                    pvs.add(propertyName, autowiredArgument);
                }
                // 遍历 autowiredBeanName 数组
                for (String autowiredBeanName : autowiredBeanNames) {
                    // 属性依赖注入
                    registerDependentBean(autowiredBeanName, beanName);
                    if (logger.isTraceEnabled()) {
                        logger.trace("Autowiring by type from bean name '" + beanName + "' via property '" +
                                propertyName + "' to bean named '" + autowiredBeanName + "'");
                    }
                }
                // 清空 autowiredBeanName 数组
                autowiredBeanNames.clear();
            }
        } catch (BeansException ex) {
            throw new UnsatisfiedDependencyException(mbd.getResourceDescription(), beanName, propertyName, ex);
        }
    }
}
```

发现这两个都调用了 `#registerDependentBean(String beanName, String dependentBeanName)` 方法

我们先是获取需要依赖注入的属性后，通过迭代、递归的方式初始化相关的 bean

然后调用这个方法，完成注册依赖

```
// DefaultSingletonBeanRegistry.java

/**
 * 依赖 beanName 之间的映射关系：beanName - > 依赖 beanName 的Set
 */
private final Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<>(64);

/**
 * 依赖 beanName 之间的映射关系：依赖 beanName - > beanName 的Set
 */
private final Map<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<>(64);

public void registerDependentBean(String beanName, String dependentBeanName) {
    // 获取 beanName
    String canonicalName = canonicalName(beanName);
    // 添加 <canonicalName, <dependentBeanName>> 到 dependentBeanMap 中
    synchronized (this.dependentBeanMap) {
        Set<String> dependentBeans =
                this.dependentBeanMap.computeIfAbsent(canonicalName, k -> new LinkedHashSet<>(8));
        if (!dependentBeans.add(dependentBeanName)) {
            return;
        }
    }
    // 添加 <dependentBeanName, <canonicalName>> 到 dependenciesForBeanMap 中
    synchronized (this.dependenciesForBeanMap) {
        Set<String> dependenciesForBean =
                this.dependenciesForBeanMap.computeIfAbsent(dependentBeanName, k -> new LinkedHashSet<>(8));
        dependenciesForBean.add(canonicalName);
    }
}
```

#### applyPropertyValues 属性转换

经过上面的步骤属性已经存储在 PropertyValues 中，我们需要将它转换到 bean 里面，需要调用`#applyPropertyValues(String beanName, BeanDefinition mbd, BeanWrapper bw, PropertyValues pvs)` 方法

```
// AbstractAutowireCapableBeanFactory.java

protected void applyPropertyValues(String beanName, BeanDefinition mbd, BeanWrapper bw, PropertyValues pvs) {
    if (pvs.isEmpty()) {
        return;
    }

    // 设置 BeanWrapperImpl 的 SecurityContext 属性
    if (System.getSecurityManager() != null && bw instanceof BeanWrapperImpl) {
        ((BeanWrapperImpl) bw).setSecurityContext(getAccessControlContext());
    }

    // MutablePropertyValues 类型属性
    MutablePropertyValues mpvs = null;

    // 原始类型
    List<PropertyValue> original;
    // 获得 original
    if (pvs instanceof MutablePropertyValues) {
        mpvs = (MutablePropertyValues) pvs;
        // 属性值已经转换
        if (mpvs.isConverted()) {
            // Shortcut: use the pre-converted values as-is.
            try {
                // 为实例化对象设置属性值 ，依赖注入真真正正地实现在此！！！！！
                bw.setPropertyValues(mpvs);
                return;
            } catch (BeansException ex) {
                throw new BeanCreationException(
                        mbd.getResourceDescription(), beanName, "Error setting property values", ex);
            }
        }
        original = mpvs.getPropertyValueList();
    } else {
        // 如果 pvs 不是 MutablePropertyValues 类型，则直接使用原始类型
        original = Arrays.asList(pvs.getPropertyValues());
    }

    // 获取 TypeConverter = 获取用户自定义的类型转换
    TypeConverter converter = getCustomTypeConverter();
    if (converter == null) {
        converter = bw;
    }

    // 获取对应的解析器
    BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(this, beanName, mbd, converter);

    // Create a deep copy, resolving any references for values.
    List<PropertyValue> deepCopy = new ArrayList<>(original.size());
    boolean resolveNecessary = false;
    // 遍历属性，将属性转换为对应类的对应属性的类型
    for (PropertyValue pv : original) {
        // 属性值不需要转换
        if (pv.isConverted()) {
            deepCopy.add(pv);
        // 属性值需要转换
        } else {
            String propertyName = pv.getName();
            Object originalValue = pv.getValue(); // 原始的属性值，即转换之前的属性值
            Object resolvedValue = valueResolver.resolveValueIfNecessary(pv, originalValue); // 转换属性值，例如将引用转换为IoC容器中实例化对象引用 ！！！！！ 对属性值的解析！！
            Object convertedValue = resolvedValue; // 转换之后的属性值
            boolean convertible = bw.isWritableProperty(propertyName) &&
                    !PropertyAccessorUtils.isNestedOrIndexedProperty(propertyName);  // 属性值是否可以转换
            // 使用用户自定义的类型转换器转换属性值
            if (convertible) {
                convertedValue = convertForProperty(resolvedValue, propertyName, bw, converter);
            }
            // Possibly store converted value in merged bean definition,
            // in order to avoid re-conversion for every created bean instance.
            // 存储转换后的属性值，避免每次属性注入时的转换工作
            if (resolvedValue == originalValue) {
                if (convertible) {
                    // 设置属性转换之后的值
                    pv.setConvertedValue(convertedValue);
                }
                deepCopy.add(pv);
            // 属性是可转换的，且属性原始值是字符串类型，且属性的原始类型值不是
            // 动态生成的字符串，且属性的原始值不是集合或者数组类型
            } else if (convertible && originalValue instanceof TypedStringValue &&
                    !((TypedStringValue) originalValue).isDynamic() &&
                    !(convertedValue instanceof Collection || ObjectUtils.isArray(convertedValue))) {
                pv.setConvertedValue(convertedValue);
                deepCopy.add(pv);
            } else {
                resolveNecessary = true;
                // 重新封装属性的值
                deepCopy.add(new PropertyValue(pv, convertedValue));
            }
        }
    }
    // 标记属性值已经转换过
    if (mpvs != null && !resolveNecessary) {
        mpvs.setConverted();
    }

    // Set our (possibly massaged) deep copy.
    // 进行属性依赖注入，依赖注入的真真正正实现依赖的注入方法在此！！！
    try {
        bw.setPropertyValues(new MutablePropertyValues(deepCopy));
    } catch (BeansException ex) {
        throw new BeanCreationException(
                mbd.getResourceDescription(), beanName, "Error setting property values", ex);
    }
}
```

总体来讲就是分下情况，属性值不需要转换就直接注入，需要转换就先转换再注入

### (四)初始化Bean

还记得我们走到哪了吗？

		// 开始初始化 bean 实例对象
		Object exposedObject = bean;
		try {
			//对 bean 进行填充，将各个属性值注入，其中，可能存在依赖于其他 bean 的属性
			// 则会递归初始依赖 bean
			populateBean(beanName, mbd, instanceWrapper);
			//调用初始化方法
			exposedObject = initializeBean(beanName, exposedObject, mbd);
		}
在将属性注入到Bean后，我们走到了最后一步的`#initializeBean(final String beanName, final Object bean, RootBeanDefinition mbd)` 方法。真正的初始化 Bean

```
    // AbstractAutowireCapableBeanFactory.java

protected Object initializeBean(final String beanName, final Object bean, @Nullable RootBeanDefinition mbd) {
    if (System.getSecurityManager() != null) { // 安全模式
        AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
            //激活 Aware 方法，对特殊的 bean 处理：Aware、BeanClassLoaderAware、BeanFactoryAware
            invokeAwareMethods(beanName, bean);
            return null;
        }, getAccessControlContext());
    } else {
        //激活 Aware 方法，对特殊的 bean 处理：Aware、BeanClassLoaderAware、BeanFactoryAware
        invokeAwareMethods(beanName, bean);
    }

    //后处理器，applyBeanPostProcessorsBeforeInitialization
    Object wrappedBean = bean;
    if (mbd == null || !mbd.isSynthetic()) {
        wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
    }

    //激活用户自定义的 init method
    try {
        invokeInitMethods(beanName, wrappedBean, mbd);
    } catch (Throwable ex) {
        throw new BeanCreationException(
                (mbd != null ? mbd.getResourceDescription() : null),
                beanName, "Invocation of init method failed", ex);
    }

    //后处理器applyBeanPostProcessorsAfterInitialization
    if (mbd == null || !mbd.isSynthetic()) {
        wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
    }

    return wrappedBean;
}
```

#### 激活Aware接口

```
// AbstractAutowireCapableBeanFactory.java

private void invokeAwareMethods(final String beanName, final Object bean) {
    if (bean instanceof Aware) {
        // BeanNameAware
        if (bean instanceof BeanNameAware) {
            ((BeanNameAware) bean).setBeanName(beanName);
        }
        // BeanClassLoaderAware
        if (bean instanceof BeanClassLoaderAware) {
            ClassLoader bcl = getBeanClassLoader();
            if (bcl != null) {
                ((BeanClassLoaderAware) bean).setBeanClassLoader(bcl);
            }
        }
        // BeanFactoryAware
        if (bean instanceof BeanFactoryAware) {
            ((BeanFactoryAware) bean).setBeanFactory(AbstractAutowireCapableBeanFactory.this);
        }
    }
}
```

主要是处理 BeanNameAware、BeanClassLoaderAware、BeanFactoryAware接口

什么是 Aware接口呢？

`org.springframework.beans.factory.Aware` 接口，定义如下

```
public interface Aware {}
```

Aware 接口是一个空接口，实际的方法签名由各个子接口来确定。而且该接口通常只会有一个接收单参数的 set 方法。Spring有许许多多的Aware接口， BeanNameAware、BeanClassLoaderAware、BeanFactoryAware接口只是冰山一角。

```
public interface BeanNameAware extends Aware {
    /**
    * 在创建此 bean 的 bean工厂中设置 beanName
    */
    void setBeanName(String name);

}

public interface BeanClassLoaderAware extends Aware {
    /**
    * 将 BeanClassLoader 提供给 bean 实例回调
    * 在 bean 属性填充之后、初始化回调之前回调，
    */
    void setBeanClassLoader(ClassLoader classLoader);

}

public interface BeanFactoryAware extends Aware {
    /**
    * 将 BeanFactory 提供给 bean 实例回调
    * 调用时机和 setBeanClassLoader 一样
    */
    void setBeanFactory(BeanFactory beanFactory) throws BeansException;

}

```

看完这个简单的方法 set 后我们就明白了，如果某一个 Bean 实现了某一个 Aware 的接口，那么在初始化 Bean 的时候就从 Spring 容器里面取得那个资源即可。

#### 后置处理

这个我们已经讲过很多遍了，在这里就不再讲了

#### 激活自定义的 init 方法

这个 init-method 方法是我们在 bean 标签里面自己定义的，在这里处理

```
// AbstractAutowireCapableBeanFactory.java

protected void invokeInitMethods(String beanName, final Object bean, @Nullable RootBeanDefinition mbd)
        throws Throwable {
    // 首先会检查是否是 InitializingBean ，如果是的话需要调用 afterPropertiesSet()
    boolean isInitializingBean = (bean instanceof InitializingBean);
    if (isInitializingBean && (mbd == null || !mbd.isExternallyManagedInitMethod("afterPropertiesSet"))) {
        if (logger.isTraceEnabled()) {
            logger.trace("Invoking afterPropertiesSet() on bean with name '" + beanName + "'");
        }
        if (System.getSecurityManager() != null) { // 安全模式
            try {
                AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
                    //属性初始化的处理
                    ((InitializingBean) bean).afterPropertiesSet();
                    return null;
                }, getAccessControlContext());
            } catch (PrivilegedActionException pae) {
                throw pae.getException();
            }
        } else {
            //属性初始化的处理
            ((InitializingBean) bean).afterPropertiesSet();
        }
    }

    if (mbd != null && bean.getClass() != NullBean.class) {
        String initMethodName = mbd.getInitMethodName();
        if (StringUtils.hasLength(initMethodName) &&
                !(isInitializingBean && "afterPropertiesSet".equals(initMethodName)) &&
                !mbd.isExternallyManagedInitMethod(initMethodName)) {
            //激活用户自定义的初始化方法
            invokeCustomInitMethod(beanName, bean, mbd);
        }
    }
}
```

总结一下前面的工作

![](https://github.com/esmusssein777/study/blob/master/md/picture/InvokeInitMethods.png?raw=true)

## 小结

完结，撒花。

![](https://github.com/esmusssein777/study/blob/master/md/picture/Bean.png?raw=true)