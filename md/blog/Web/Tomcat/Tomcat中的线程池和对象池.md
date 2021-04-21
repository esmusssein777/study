# Tomcat 中的线程池和对象池

​		对象池就是把用过的对象保存起来，等下一次需要这种对象的时候，直接从对象池中拿出来重复使用，避免频繁地创建和销毁。在 Java 中万物皆对象，线程也是一个对象，Java 线程是对操作系统线程的封装，创建 Java 线程也需要消耗系统资源， 因此就有了线程池。JDK 中提供了线程池的默认实现，我们也可以通过扩展 Java 原生线程池来实现自己的线程池。

## Tomcat 线程池

### Java 线程池

简单的说，Java 线程池里内部维护一个线程数组和一个任务队列，当任务处理不过来的时，就把任务放到队列里慢慢 处理。

#### ThreadPoolExecutor

我们先来看看 Java 线程池核心类 ThreadPoolExecutor 的 构造函数，你需要知道 ThreadPoolExecutor 是如何使用这些参数的，这是理解 Java 线程工作原理的关键。

```
public ThreadPoolExecutor(
	int corePoolSize,
	int maximumPoolSize,
	long keepAliveTime,
	TimeUnit unit,
	BlockingQueue<Runnable> queue
	ThreadFactory threadFactory,
	RejectedExecutionHandler handler
}
```

1. 前 corePoolSize 个任务时，来一个任务就创建一个新线程。

2. 后面再来任务，就把任务添加到任务队列里让所有的线程去抢，如果队列满了就创建临时线程。

3. 如果总线程数达到 maximumPoolSize，执行拒绝策略。

每次提交任务时，如果线程数还没达到核心线程数 corePoolSize，线程池就创建新线程来执行。当线程数达到corePoolSize后，新增的任务就放到工作队列 workQueue里，而线程池中的线程则努力地从 workQueue里拉活来干，也就是调用 poll 方法来获取任务。

如果任务很多，并且workQueue是个有界队列，队列可能会满，此时线程池就会紧急创建新的临时线程来救场，如果总的线程数达到了最大线程数maximumPoolSize，则不能再创建新的临时线程了，转而执行拒绝策略handler，比如抛出异常或者由调用者线程来执行任务等。

如果高峰过去了，线程池比较闲了怎么办?临时线程使用 poll(keepAliveTime, unit)方法从工作队列中拉活干， 请注意 poll 方法设置了超时时间，如果超时了仍然两手空空没拉到活，表明它太闲了，这个线程会被销毁回收。

那还有一个参数threadFactory是用来做什么的呢?通过它你可以扩展原生的线程工厂，比如给创建出来的线程取个有意义的名字。

####FixedThreadPool/CachedThreadPool

FixedThreadPool/CachedThreadPool 是对 ThreadPoolExecutor 的定制化

FixedThreadPool 有固定长度(nThreads)的线程数组，忙不过来时会把任务放到无限长的队列里，这是因为 LinkedBlockingQueue 默认是一个无界队列。

CachedThreadPool 的 maximumPoolSize 参数值是Integer.MAX_VALUE，因此它对线程个数不做限制，忙不过来时无限创建临时线程，闲下来时再回收。它的任务队列是SynchronousQueue，表明队列长度为 0。

### Tomcat 线程池

Tomcat 线程池扩展了原生的 ThreadPoolExecutor，通过重写 execute 方法实现了自己的任务处理逻辑:

1. 前 corePoolSize 个任务时，来一个任务就创建一个新线程。

2. 再来任务的话，就把任务添加到任务队列里让所有的线程去抢，如果队列满了就创建临时线程。

3. 如果总线程数达到 maximumPoolSize，则继续尝试把任务添加到任务队列中去。

4. 如果缓冲队列也满了，插入失败，执行拒绝策略。

## 对象池

Java 对象，特别是一个比较大、比较复杂的 Java 对象，它们的创建、初始化和 GC 都需要耗费 CPU 和内存资源，为了减少这些开销，Tomcat 和 Jetty 都使用了对象池技术。所谓的对象池技术，就是说一个 Java 对象用完之后把它保存起来，之后再拿出来重复使用，省去了对象创建、初始化和 GC 的过程。对象池技术是典型的以空间换时间的思路。

由于维护对象池本身也需要资源的开销，不是所有场景都适合用对象池。如果你的 Java 对象数量很多并且存在的时间比较短，对象本身又比较大比较复杂，对象初始化的成本比较高，这样的场景就适合用对象池技术。

Tomcat 用 SynchronizedStack 类来实现对象池，在对象池里面的对象都是无差别的。

```java
public class SynchronizedStack<T> {
 // 内部维护一个对象数组, 用数组实现栈的功能
 private Object[] stack;

 // 这个方法用来归还对象，用 synchronized 进行线程同步
 public synchronized boolean push(T obj) {
 		index++;
 		if (index == size) {
 				if (limit == -1 || size < limit) {
 						expand();// 对象不够用了，扩展对象数组
 				} else {
 					index--;
 					return false;
 				}
		}
 		stack[index] = obj;
 		return true;
 }

 // 这个方法用来获取对象
 public synchronized T pop() {
 		if (index == -1) {
 			return null;
 		}
 		T result = (T) stack[index];
 		stack[index--] = null;
 		return result;
 }

 // 扩展对象数组长度，以 2 倍大小扩展
 private void expand() {
 		int newSize = size * 2;
 		if (limit != -1 && newSize > limit) {
 				newSize = limit;
 		}
 		// 扩展策略是创建一个数组长度为原来两倍的新数组
 		Object[] newStack = new Object[newSize];
 		// 将老数组对象引用复制到新数组
 		System.arraycopy(stack, 0, newStack, 0, size);
 		// 将 stack 指向新数组，老数组可以被 GC 掉了
 		stack = newStack;
 		size = newSize;
}
```

主要是 SynchronizedStack 内部维护了一个对象数组，并且用数组来实现栈的接口:push 和 pop 方法，这两个方法分别用来归还对象和获取对象。

