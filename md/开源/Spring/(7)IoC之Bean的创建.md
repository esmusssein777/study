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

