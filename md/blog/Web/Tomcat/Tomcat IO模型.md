# Tomcat I/O 模型

## 同步

Tomcat 的 NioEndPoint 组件实现了 I/O 多路复用模型，Tomcat 的 NioEndpoint 组件虽然实现比较复杂，但基本原理就是两步：

* 创建一个 Seletor，在它身上注册各种感兴趣的事件，然后调用 select 方法，等待感兴趣的事情发生
* 感兴趣的事情发生了，比如可以读了，这时便创建一个新的线程从 Channel 中读数据

![ncTgnY](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/ncTgnY.png)

### LimitLatch

LimitLatch 用来控制连接个数，当连接数到达最大时阻塞线程，直到后续组件处理完一个连接后将连接数减 1。请你注意到达最大连接数后操作系统底层还是会接收客户端连接，但用户层已经不再接收。

```
public class LimitLatch {
    private class Sync extends AbstractQueuedSynchronizer {
        @Override
        protected int tryAcquireShared() {
            long newCount = count.incrementAndGet();
            if (newCount > limit) {
                count.decrementAndGet();
                return -1;
            } else {
							return 1; }
						}
						
        @Override
        protected boolean tryReleaseShared(int arg) {
            count.decrementAndGet();
            return true;
        }
	}
	
	private final Sync sync;
	private final AtomicLong count;
	private volatile long limit;

	// 线程调用这个方法来获得接收新连接的许可，线程可能被阻塞 
	public void countUpOrAwait() throws InterruptedException {
  	sync.acquireSharedInterruptibly(1);
	}
	
	// 调用这个方法来释放一个连接许可，那么前面阻塞的线程可能被唤醒 
	public long countDown()	{
  	sync.releaseShared(0);
  	long result = getCount();
		return result;
	}
}
```

LimitLatch 内步定义了内部类 Sync，而 Sync 扩展了 AQS， AQS 是 Java 并发包中的一个核心类，它在内部维护一个状态和一个线程队列，可以用来控制线程什么时候挂起，什么时候唤醒。

1. 用户线程通过调用 LimitLatch 的 countUpOrAwait 方法来拿到锁，如果暂时无法获取，这个线程会被阻塞到 AQS 的队列中。那 AQS 怎么知道是阻塞还是不阻塞用户线程呢?其实这是由 AQS 的使用者来决定的，也就是内部类 Sync 来决定的，因为 Sync 类重写了 AQS 的tryAcquireShared() 方法。它的实现逻辑是如果当前连接数 count 小于 limit，线程能获取锁，返回 1，否则返回 -1。

2. 如何用户线程被阻塞到了 AQS 的队列，那什么时候唤醒呢?同样是由 Sync 内部类决定，Sync 重写了 AQS 的releaseShared() 方法，其实就是当一个连接请求处理完了，这时又可以接收一个新连接了，这样前面阻塞的线程将会被唤醒。

### Acceptor

Acceptor 实现了 Runnable 接口，因此可以跑在单独线程里。一个端口号只能对应一个 ServerSocketChannel，因此这个 ServerSocketChannel 是在多个 Acceptor 线程之间共享的，它是 Endpoint 的属性，由 Endpoint 完成初始化和端口绑定。初始化过程如下:

```
serverSock = ServerSocketChannel.open();
serverSock.socket().bind(addr,getAcceptCount()); serverSock.configureBlocking(true);
```

1. bind 方法的第二个参数表示操作系统的等待队列长度，我在上面提到，当应用层面的连接数到达最大值时，操作系统可以继续接收连接，那么操作系统能继续接收的最大连接数就 是这个队列长度，可以通过 acceptCount 参数配置，默认是 100。

2. ServerSocketChannel 被设置成阻塞模式，也就是说它是以阻塞的方式接收连接的。ServerSocketChannel 通过 accept() 接受新的连接，accept() 方法返回获得 SocketChannel 对象，然后将 SocketChannel 对象封装在一个 PollerEvent 对象中，并将 PollerEvent 对象压入 Poller 的 Queue 里，这是个典型的生产者 - 消费者模式， Acceptor 与 Poller 线程之间通过 Queue 通信。

### Poller

Poller 本质是一个 Selector，它内部维护一个 Queue，这个 Queue 定义如下:

```
private final SynchronizedQueue<PollerEvent> events = new SynchronizedQueue<>();
```

SynchronizedQueue 的方法比如 offer、poll、size 和 clear 方法，都使用了 Synchronized 关键字进行修饰，用来保证同一时刻只有一个 Acceptor 线程对 Queue 进行读写。同时有多个 Poller 线程在运行，每个 Poller 线程都有自己的 Queue。每个 Poller 线程可能同时被多个 Acceptor 线程调用来注册 PollerEvent。同样 Poller 的个数可以通过 pollers 参数配置。

Poller 不断的通过内部的 Selector 对象向内核查询 Channel 的状态，一旦可读就生成任务类 SocketProcessor 交给 Executor 去处理。Poller 的另一个重要任务是循环遍历检查自己所管理的 SocketChannel 是否已经超时，如果有超时就关闭这个 SocketChannel。

### SocketProcessor

我们知道，Poller 会创建 SocketProcessor 任务类交给线程池处理，而 SocketProcessor 实现了 Runnable 接口，用来定义 Executor 中线程所执行的任务，主要就是调用 Http11Processor 组件来处理请求。Http11Processor 读取 Channel 的数据来生成 ServletRequest 对象，这里请你注意:

Http11Processor 并不是直接读取 Channel 的。这是因为 Tomcat 支持同步非阻塞 I/O 模型和异步 I/O 模型，在 Java API 中，相应的 Channel 类也是不一样的，比如有 AsynchronousSocketChannel 和 SocketChannel，为了对 Http11Processor 屏蔽这些差异，Tomcat 设计了一个包装类叫作 SocketWrapper，Http11Processor 只调用 SocketWrapper 的方法去读写数据。

### Executor

Executor 是 Tomcat 定制版的线程池，它负责创建真正干活的工作线程，干什么活呢?就是执行 SocketProcessor 的 run 方法，也就是解析请求并通过容器来处理请求，最终会调用到我们的 Servlet。



高并发就是能快速地处理大量的请求，需要合理设计线程模型让 CPU 忙起来，尽量不要让线程阻塞，因为一阻塞，CPU 就闲下来了。另外就是有多少任务，就用相应规模的线程数去处理。我们注意到 NioEndpoint 要完成三件事情:接收连接、检测 I/O 事件以及处理请求，那么最核心的就是把这三件事情分开，用不同规模的线程去处理，比如用专门的线程组去跑 Acceptor，并且 Acceptor 的个数可以配置;用专门的线程组去跑 Poller，Poller 的个数也可以配置;最后具体任务的执行也由专门的线程池来处理，也可以配置线程池的大小。

## 异步

![image-20210401155030344](/Users/guangzheng.li/Library/Application Support/typora-user-images/image-20210401155030344.png)

从图上看，总体工作流程跟 NioEndpoint 是相似的。 LimitLatch 是连接控制器，它负责控制最大连接数。

Nio2Acceptor 扩展了 Acceptor，用异步 I/O 的方式来接收连接，跑在一个单独的线程里，也是一个线程组。Nio2Acceptor 接收新的连接后，得到一个 AsynchronousSocketChannel，Nio2Acceptor 把 AsynchronousSocketChannel 封装成一个 Nio2SocketWrapper，并创建一个 SocketProcessor 任务类交给线程池处理，并且 SocketProcessor 持有 Nio2SocketWrapper 对象。

Executor 在执行 SocketProcessor 时，SocketProcessor 的 run 方法会调用 Http11Processor 来处理请求，Http11Processor 会通过 Nio2SocketWrapper 读取和解析请求数据，请求经过容器处理后，再把响应通过 Nio2SocketWrapper 写出。

需要你注意 Nio2Endpoint 跟 NioEndpoint 的一个明显不同点是，Nio2Endpoint 中没有 Poller 组件，也就是没有 Selector。因为在异步 I/O 模式下， Selector 的工作交给内核来做了。

### Nio2Acceptor

和 NioEndpint 一样，Nio2Endpoint 的基本思路是用 LimitLatch 组件来控制连接数，但是 Nio2Acceptor 的监听连接的过程不是在一个死循环里不断的调 accept 方法，而是通过回调函数来完成的。我们来看看它的连接监听方法:

```
serverSock.accept(null, this);
```

其实就是调用了 accept 方法，注意它的第二个参数是 this，表明 Nio2Acceptor 自己就是处理连接的回调类，因此 Nio2Acceptor 实现了 CompletionHandler 接口。那么它是如何实现 CompletionHandler 接口的呢?

```java
protected class Nio2Acceptor extends Acceptor<AsynchronousSocketChannel>
    implements CompletionHandler<AsynchronousSocketChannel, Void> {
	@Override
	public void completed(AsynchronousSocketChannel socket, Void attachment) {
    if (isRunning() && !isPaused()) {
        if (getMaxConnections() == -1) {
						// 如果没有连接限制，继续接收新的连接
            serverSock.accept(null, this);
        } else {
						// 如果有连接限制，就在线程池里跑 Run 方法，Run 方法会检查连接数
            getExecutor().execute(this);
        }
				// 处理请求
				if (!setSocketOptions(socket)) {
            closeSocket(socket);
        }
	}
}
```

可以看到 CompletionHandler 的两个模板参数分别是 AsynchronousServerSocketChannel 和 Void，我在前面说过第一个参数就是 accept 方法的返回值，第二个参数是附件类，由用户自己决定，这里为 Void。completed 方法的处理逻辑比较简单:

* 如果没有连接限制，继续在本线程中调用 accept 方法接收新的连接。

* 如果有连接限制，就在线程池里跑 run 方法去接收新的连接。那为什么要跑 run 方法呢，因为在 run 方法里会检查连接数，当连接达到最大数时，线程可能会被 LimitLatch 阻塞。为什么要放在线程池里跑呢?这是因为如果放在当前线程里执行，completed 方法可能被阻塞，会导致这个回调方法一直不返回。

接着 completed 方法会调用 setSocketOptions 方法，在这个方法里，会创建 Nio2SocketWrapper 和 SocketProcessor，并交给线程池处理。

### Nio2SocketWrapper

Nio2SocketWrapper 的主要作用是封装 Channel，并提供接口给 Http11Processor 读写数据。Http11Processor 是不能阻塞等待数据的，按照异步 I/O 的套路，Http11Processor 在调用 Nio2SocketWrapper 的 read 方法时需要注册回调类，read 调用会立即返回，问题是立即返回后 Http11Processor 还没有读到数据，怎么办呢? 这个请求的处理不就失败了吗?

为了解决这个问题，Http11Processor 是通过 2 次 read 调用来完成数据读取操作的。

* 第一次 read 调用: 连接刚刚建立好后，Acceptor 创建 SocketProcessor 任务类交给线程池去处理，Http11Processor 在处理请求的过程中，会调用 Nio2SocketWrapper 的 read 方法发出第一次读请求，同时注册了回调类 readCompletionHandler，因为数据没读到，Http11Processor 把当前的 Nio2SocketWrapper 标记为数据不完整。接着 SocketProcessor 线程被回收，Http11Processor 并没有阻塞等待数据。这里请注意，Http11Processor 维护了一个 Nio2SocketWrapper 列表，也就是维护了连接的状态。
* 第二次 read 调用:当数据到达后，内核已经把数据拷贝到 Http11Processor 指定的 Buffer 里，同时回调类 readCompletionHandler 被调用，在这个回调处理方法里会重新创建一个新的 SocketProcessor 任务来继续处理这个连接，而这个新的 SocketProcessor 任务类持有原来那个 Nio2SocketWrapper，这一次 Http11Processor 可以通过 Nio2SocketWrapper 读取数据了，因为数据已经到了应用层的 Buffer。

