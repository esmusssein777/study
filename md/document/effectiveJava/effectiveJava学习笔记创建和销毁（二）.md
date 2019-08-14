# 1、消除过期的对象引用

如果你从使用手动内存管理的语言(如C或c++)切换到像Java这样的带有垃圾收集机制的语言，那么作为程序员的工作就会变得容易多了，因为你的对象在使用完毕以后就自动回收了。当你第一次体验它的时候，它就像魔法一样。这很容易让人觉得你不需要考虑内存管理，但这并不完全正确。

```
// Can you spot the "memory leak"?
public class Stack {
    private Object[] elements;
    private int size = 0;
    private static final int DEFAULT_INITIAL_CAPACITY = 16;

    public Stack() {
        elements = new Object[DEFAULT_INITIAL_CAPACITY];
    }

    public void push(Object e) {
        ensureCapacity();
        elements[size++] = e;
    }

    public Object pop() {
        if (size == 0)
            throw new EmptyStackException();
        return elements[--size];
    }

    /**
     * Ensure space for at least one more element, roughly
     * doubling the capacity each time the array needs to grow.
     */
    private void ensureCapacity() {
        if (elements.length == size)
            elements = Arrays.copyOf(elements, 2 * size + 1);
    }
}
```



笼统地说，程序有一个“内存泄漏”，由于垃圾回收器的活动的增加，或内存占用的增加，静默地表现为性能下降。 

那么哪里发生了内存泄漏？ 如果一个栈增长后收缩，那么从栈弹出的对象不会被垃圾收集，即使使用栈的程序不再引用这些对象。 这是因为栈维护对这些对象的过期引用（ obsolete references）。 过期引用简单来说就是永远不会解除的引用。

这类问题的解决方法很简单：一旦对象引用过期，将它们设置为 null。 在我们的Stack类的情景下，只要从栈中弹出，元素的引用就设置为过期。 

```
public Object pop() {
    if (size == 0)
        throw new EmptyStackException();
    Object result = elements[--size];
    elements[size] = null; // Eliminate obsolete reference
    return result;
}
```



一般来说，**当一个类自己管理内存时，程序员应该警惕内存泄漏问题**。 每当一个元素被释放时，元素中包含的任何对象引用都应该被清除。

**另一个常见的内存泄漏来源是缓存**。一旦将对象引用放入缓存中，很容易忘记它的存在，并且在它变得无关紧要之后，仍然保留在缓存中。这可以通过一个后台线程(也许是`ScheduledThreadPoolExecutor`)或将新的项添加到缓存时顺便清理。`LinkedHashMap`类使用它的`removeEldestEntry`方法实现了后一种方案。对于更复杂的缓存，可能直接需要使用`java.lang.ref`

第三个常见的内存泄漏来源是监听器和其他回调。如果你实现了一个API，其客户端注册回调，但是没有显式地撤销注册回调，除非采取一些操作，否则它们将会累积。确保回调是垃圾收集的一种方法是只存储弱引用（weak references），例如，仅将它们保存在`WeakHashMap`的键（key）中。

# **2、避免使用终结方法**

### 避免使用终结方法(finalizer)

终结方法(finalizer)通常是不可预测的，也是很危险的，一般情况下是不必要的。

不要把finalizer当成C++中析构函数的对应物。java中，当对象不可达时（即没有引用指向这个对象时），会由垃圾回收器来回收与该对象相关联的内存资源；而其他的内存资源，则一般由try-finally代码块来完成类似的工作。

### **一、finalizer的缺点：**

1. 终结方法的缺点在于不能保证会被及时地执行。

及时执行finalizer方法是JVM垃圾回收方法的一个主要功能。由于不同JVM的垃圾回收算法不同，JVM会“非故意的”延迟执行终结方法，因此终结方法的执行时间点是非常不稳定的。

2. finalizer方法的线程优先级比当前程序的其他线程优先级要低，且JAVA语言规范不保证哪个线程可以执行finalizer方法。

3. JAVA语言规范不仅不保证及时执行finalizer方法，还不保证一定会执行finalizer方法。当程序终止时，有可能一些对象的finalizer方法还没有执行。——不应该依赖finalizer方法来更新重要的持久状态。

4. System.gc和System.runFinalization不保证finalizer一定执行。

调用System.gc() 只是建议JVM执行垃圾回收（GC），但什么时候执行、是否要执行由JVM决定。

```
public class C {
	public static void main(String[] args) {
		A a = new A();
		a.b = new B();
		a = null;
		System.gc();
	}
}
class A {
	B b;
	public void finalize() {
		System.out.println("method A.finalize at " + System.nanoTime());
	}
}
class B {
	public void finalize() {
		System.out.println("method B.finalize at " + System.nanoTime());
	}
}
```



得到：

method B.finalize at 37855471408611
method A.finalize at 37855471930669

5. System.runFinalizersOnExit Runtime.runFinalizersOnExit 可以保证finalizer一定执行，但是这两个方法已经废弃。

6. 如果未捕获的异常在finalizer方法中抛出来，这个异常可以被忽略（警告都不会打印出来），且finalizer方法会终止。这样这个异常就使对象处于“被破坏”的状态，如果另一个线程要使用这个对象，就可能发生不确定的行为。

7. finalizer方法会有非常严重的（Severe）性能损失

### **二、不用finalizer方法，怎么来实现线程中对象资源的终止呢？**

使用显示终止方法。

显示终止方法的要求：

1. 实例必须记录下自己是否已经被终止了

2. 显示终止方法必须在一个私有域中记录下“该对象已经不再有效”

3. 在执行终止方法之后，执行对象其他方法时要检查“该对象已经不再有效”私有域，抛出IllegalStateException。

显示终止方法的例子：

1. InputStream

2. OutputStream

3. java.sql.Connection

4. java.util.Timer

显示终止方法的使用方法：通常和try-finally一起使用，以确保及时终止。

FileInputStream fileInputStream = new FileInputStream();

try{

​    //Do something about fileInputStream;

}finally{

​    fileInputStream.close();

}

### 三、终结方法的合法用途。

1. 作为安全网——显示终止方法忘记调用的时候

2. 本地对等体——普通对象通过本地方法委托给一个本地对象，因为本地对等体不是一个普通对象，所以垃圾回收器不会知道他。当Java对等体被回收的时候，他不会被回收。

### 四、终结方法的执行过程中要保证：如果子类的终结过程出现异常，超类的终结过程也会得到执行。

由于终结方法链不会自动执行，因此我们需要手动保证这一点。

方法一：使用try – finalize 代码结构

@Override  
protected void finalize() throws Throwable {  
    try{  
        ...//Finalize subclass state  
    } finally {  
        super.finalize();  
    }  
} 

方法二：使用finalizer guardian(终结方法守卫者)

### 终结方法守卫者

如果子类实现者覆盖了超类的终结方法，但是忘了调用超类的终结方法，那么超类的终结方法永远不会调用。为了防止此种情况出现，可以使用终结方法守卫者。即为每个被终结的对象创建一个附加的对象，该附加对象为一个匿名类，将外围类的终结操作如释放资源放入该匿名类的终结方法中。同时，外围类保存着对该匿名类的唯一引用，即复制给私有变量域。

至于为什么会这样，我还没想明白。。。。哪位大神明白这样做为什么会调用超类的终结守卫者给我讲一讲。。。

```
class A {

    @SuppressWarnings("unused")
    //终结守卫者
    private final Object finalizerGuardian = new Object() {

        @Override
        //终结守卫者的终结方法将被执行
        protected void finalize() {
            System.out.println("A finalize by the finalizerGuardian");
        }
    };


    @Override
    //由于终结方法被子类覆盖，该终结方法并不会被执行
    protected void finalize() {
        System.out.println("A finalize by the finalize method");
    }


    public static void main(String[] args) throws Exception {
        B b = new B();
        b = null;
        System.gc();
        Thread.sleep(500);
    }
}

class B extends A {

    @Override
    public void finalize() {
        System.out.println("B finalize by the finalize method");
    }

}
```



![img](https://img-blog.csdn.net/20180926194441909?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM5MDcxNTMw/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)

### 总之，

**1.除非是作为安全网或者是为了终结非关键的本地资源，否则请不要使用终结方法。**

**2.如果确实需要，可以使用显示终止方法**

**2.如果没办法真的使用了finalize，别忘记了调用super.finalize()。还可以考虑是否使用终结方法守卫者，使未调用super.finalize()方法的类的父类的终结方法也会被执行。**