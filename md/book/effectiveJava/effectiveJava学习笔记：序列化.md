# 谨慎地实现Serializable

序列化**：**将一个对象编码成一个字节流，通过保存或传输这些字节流数据来达到数据持久化的目的； 
反序列化**：**将字节流转换成一个对象；

### **1.序列化的含义和作用**。

序列化用来将对象编码成字节流，反序列化就使将字节流编码重新构建对象。 
序列化实现了对象传输和对象持久化，所以它能够为远程通信提供对象表示法，为JavaBean组件提供持久化数据。

### **2.序列化的危害**

**1.降低灵活性**：为实现Serializable而付出的最大代价是，一旦一个类被发布，就大大降低了”改变这个类的实现”的灵活性。如果一个类实现了Serializable，它的字节流编码（或者说序列化形式，serialized form）就变成了它的导出的API的一部分，必须永远支持这种序列化形式。

而且，特殊地，每个可序列化类都有唯一的标志（serial version id，在类体现为私有静态final的long域serialVersionUID），如果没有显式指示，那么系统就会自动生成一个serialVersionUID，如果下一个版本改变了这个类，那么系统就会重新自动生成一个serialVersionUID。因此如果没有声明显式的uid，会破坏版本之间的兼容性，运行时产生InvalidClassException。

**2.降低封装性**：如果你接受了默认的序列化形式，这个类中私有的和包级私有的实例域将都变成导出的API的一部分，这不符合”最低限度地访问域”的实践准则。

**3.降低安全性**：增加了bug和漏洞的可能性，反序列化的过程其实类似于调用对象的构造器，但是这个过程又没有用到构造器，因此如果字节流被无意修改或被用心不测的人修改，那么服务器很可能会产生错误或者遭到攻击。

**4.降低可测试性：**随着类版本的不断更替，必须满足版本兼容问题，所以发行的版本越多，测试的难度就越大。

**5.降低性能：**序列化对象时，不仅会序列化当前对象本身，还会对该对象引用的其他对象也进行序列化。如果一个对象包含的成员变量是容器类等并深层引用时（对象是链表形式），此时序列化开销会很大，这时必须要采用其他一些手段处理。

### **3.序列化的使用场景**

**1**.**需要实现一个类的对象传输或者持久化。 2.A是B的组件，当B需要序列化时，A也实现序列化会更容易让B使用。**

### **4.序列化不适合场景**

为了继承而设计的类应该尽可能少地去实现Serializable接口，用户接口也应该尽可能不继承Serializable接口，原因是子类或实现类也要承担序列化的风险。

### **5.序列化需要注意的地方**

1)如果父类实现了Serializable，子类自动序列化了，不需要实现Serializable；

2)若父类未实现Serializable,而子类序列化了，父类属性值不会被保存，反序列化后父类属性值丢失，需要父类有一个无参的构造器，子类要负责序列化(反序列化)父类的域，子类要先序列化自身，再序列化父类的域。

至于为什么需要父类有一个无参的构造器，是因为子类先序列化自身的时候先调用父类的无参的构造器。 
实例：

```
private void writeObject(java.io.ObjectOutputStream out) 
　　throws IOException{ 
　　　out.defaultWriteObject();//先序列化对象 
　　　out.writeInt(parentvalue);//再序列化父类的域 
　　} 
　　private void readObject(java.io.ObjectInputStream in) 
　　throws IOException, ClassNotFoundException{ 
　　　in.defaultReadObject();//先反序列化对象 
     parentvalue=in.readInt();//再反序列化父类的域 
　　}
```



3)序列化时，只对对象状态进行了保存，对象方法和类变量等并没有保存，因此序列化并不保存静态变量值。

4)当一个对象的实例变量引用其他对象，序列化该对象时也把引用对象序列化了。所以组件也应该序列化。

5)不是所有对象都可以序列化，基于安全和资源方面考虑，如Socket/thread若可序列化，进行传输或保存，无法对他们重新分配资源。

#  **考虑使用自定义的序列化形式**

**若一个对象的物理表示法等同于它的逻辑内容，则可以使用默认的序列化形式**

默认序列化形式描述对象内部所包含的数据，及每一个可以从这个对象到达其他对象的内部数据。

若一个对象的物理表示法与逻辑数据内容有实质性区别时，如下面的类：

```
public final class StringList implements Serializable {
    private int size = 0;
    private Entry head = null;

    private static class Entry implements Serializable {
        String data;
        Entry next;
        Entry previous;
    }
}
```



我们知道，序列一个类，会同时序列化它的组件。 
也就是说，如果我序列化了A对象， B是双向链表，它要序列化它的内部成员B和C对象，但是序列化B和C对象的时候，A同时也是它们的组件，也要序列化A
于是就进入了无穷的死循环中。会有下面的问题

a) 该类导出API被束缚在该类的内部表示法上，链表类也变成了公有API的一部分，若将来内部表示法发生变化，仍需要接受链表形式的输入，并产生链式形式的输出。 
b) 消耗过多空间：像上面的例子，序列化既表示了链表中的每个项，也表示了所有链表关系，而这是不必要的。这样使序列化过于庞大，把它写到磁盘中或网络上发送都很慢； 
c) 消耗过多时间：序列化逻辑并不了解对象图的拓扑关系，所以它必须要经过一个图遍历过程。 
d) 引起栈溢出：默认的序列化过程要对对象图执行一遍递归遍历，这样的操作可能会引起栈溢出。

这时候，我们的需求很简单，对于每个对象的Entry，我只序列化一次就行了，不需要迭代序列化。 
于是就有了transient关键字,不会被序列化。

```
public final class StringList implements Serializable {
    private transient int size = 0;
    private transient Entry head = null;

    //此类不再实现Serializable接口
    private static class Entry {
        String data;
        Entry next;
        Entry previous;
    }

    private final void add(String s) {
        size++;
        Entry entry = new Entry();
        entry.data = s;
        head.next = entry;
    }

    /**
     * 自定义序列化
     * @param s
     * @throws IOException
     */
    private void writeObject(ObjectOutputStream s) throws IOException{
        s.defaultWriteObject();
        s.writeInt(size);
        for (Entry e = head; e != null; e = e.next) {
            s.writeObject(e.data);
        }
    }

    /**
     * 自定义反序列化
     * @param s
     * @throws IOException
     */
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException{
        s.defaultReadObject();
        size = s.readInt();
        for (Entry e = head; e != null; e = e.next) {
            add((String) s.readObject());
        }
    }

}
```



标记为transient的不会自动序列化，这就防止默认序列化做出错误的事情，然后调用writeObject手动序列化transient字段，做自己认为正确的事。readObject同理。

总结，自定义序列化目的就是**做自己认为正确的事情**，经典的例子有ArrayList和HashMap。



# **保护性地编写readObject方法**

这个和之前公有的构造器一样，将参数进行保护性拷贝。

readObject相当于一个公有构造器，而构造器需要检查参数有效性及必要时对参数进行保护性拷贝。而如果序列化的类包含了私有的可变组件，就需要在readObject方法中进行保护性拷贝。 

我们看两者：

```
public final class Period implements Serializable {
        private Date start;
        private Date end;
        public Period(Date start, Date end) {
            this.start = new Date(start.getTime());//保护性拷贝
            this.end = new Date(end.getTime());
            if (this.start.compareTo(this.end) > 0) {
                throw new IllegalArgumentException("start bigger end");
            }
        }

        private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
            s.defaultReadObject();
            start = new Date(start.getTime());//保护性拷贝
            end = new Date(end.getTime());
            if (this.start.compareTo(this.end) > 0) {
                throw new IllegalArgumentException("start bigger end");
            }
        }
    }
```



如果实体域被声明为final，为了实现可序列化可能需要去掉final
①对于对象引用必须保持为私有的类，要保护性拷贝这些域中的每个对象
②对于任何约束条件，如果检查失败，则抛出一个InvalidObjectException异常
③如果整个对象图在被反序列化之后必须进行验证，就应该使用ObjectInputValidation接口
④无论是直接还是间接方式，都不要调用类中任何可被覆盖的方法



## 单例模式序列化，枚举类型优先于readObsolve

对单例：

```
public class Elvis {
        private static final Elvis INSTANCE = new Elvis();
        private Elvis() { }
        public static Elvis getINSTANCE() {
            return INSTANCE;
        }
    }
```



通过序列化工具，可以将一个类的单例的实例对象写到磁盘再读回来，从而有效获得一个实例。

如果想要单例实现Serializable，任何readObject方法，它会返回一个新建的实例，这个新建实例不同于该类初始化时创建的实例。从而导致单例获取失败。但序列化工具可以让开发人员通过readResolve来替换readObject中创建的实例，即使构造方法是私有的。在反序列化时，新建对象上的readResolve方法会被调用，返回的对象将会取代readObject中新建的对象。

```
//该方法忽略了被反序列化的对象，只返回该类初始化时创建的那个Elvis实例
private Object readResolve() {
            return INSTANCE;
        }
```



 采用readResolve的一些缺点： 
1) readResolve的可访问性需要控制好，否则很容易出问题。如果readResolve方法是受保护或是公有的，且子类没有覆盖它，序列化的子类实例进行反序列化时，就会产生一个超类实例，这时可能导致ClassCastException异常。 
2) readResolve需要类的所有实例域都用transient来修饰，否则可能被攻击。 
而将一个可序列化的实例受控类用枚举实现，可以保证除了声明的常量外，不会有别的实例。 
所以如果一个单例需要序列化，最好用枚举来实现：

```
public enum Elvis implements Serializable {
        INSTANCE;
        private String[] favriteSongs = {"test", "abc"};//如果不是枚举，需要将该变量用transient修饰
    }
```



# 考虑用序列化代理代替序列化实例 

序列化代理类：为可序列化的类设计一个私有静态嵌套类，精确地表示外部类的实例的逻辑状态。

(1) 使用场景
1) 必须在一个不能被客户端扩展的类上编写readObject或writeObject方法时，可以考虑使用序列化代理模式； 
2) 想稳定地将带有重要约束条件的对象序列化时

(2) 序列化代理类的使用方法
序列代理类应该有一个单独的构造器，参数就是外部类，此构造器只能参数中拷贝数据，不需要一致性检查或是保护性拷贝。外部类及其序列化代理类都必须声明实现Serializable接口。 
writeReplace: 如果实现了writeReplace方法后，在序列化时会先调用writeReplace方法将当前对象替换成另一个对象（该方法会返回替换后的对象）并将其写入数据流。 
具体实现如下：

```
public class Period implements Serializable{  

    private static final long serialVersionUID = 1L;  
    private final Date start;  
    private final Date end;  

    public Period(Date start, Date end) {  

        if(null == start || null == end || start.after(end)){  

            throw new IllegalArgumentException("Time Uncurrent");  
        }  
        this.start = start;  
        this.end = end;  
    }  

    public Date start(){  

        return new Date(start.getTime());  
    }  

    public Date end(){  

        return new Date(end.getTime());  
    }  

    @Override  
    public String toString(){  

        return "startTime：" + start + " , endTime：" + end;  
    }  

    /** 
     * 序列化外围类时，虚拟机会转掉这个方法，最后其实是序列化了一个内部的代理类对象！ 
     * @return 
     */  
    private Object writeReplace(){  

        System.out.println("进入writeReplace()方法！");  
        return new SerializabtionProxy(this);  
    }  

    /** 
     * 如果攻击者伪造了一个字节码文件，然后来反序列化也无法成功，因为外围类的readObject方法直接抛异常！ 
     * @param ois 
     * @throws InvalidObjectException 
     */  
    private void readObject(ObjectInputStream ois) throws InvalidObjectException{  

        throw new InvalidObjectException("Proxy required!");  
    }  

    /** 
     * 序列化代理类，他精确表示了其当前外围类对象的状态！最后序列化时会将这个私有内部内进行序列化！ 
     */  
    private static class SerializabtionProxy implements Serializable{  

        private static final long serialVersionUID = 1L;  
        private final Date start;  
        private final Date end;  
        SerializabtionProxy(Period p){  

            this.start = p.start;  
            this.end = p.end;  
        }  

        /** 
         * 反序列化这个类时，虚拟机会调用这个方法，最后返回的对象是一个Period对象！这里同样调用了Period的构造函数， 
         * 会进行构造函数的一些校验！  
         */  
        private Object readResolve(){  

            System.out.println("进入readResolve()方法，将返回Period对象！");  
            // 这里进行保护性拷贝！  
            return new Period(new Date(start.getTime()), new Date(end.getTime()));  
        }  

    }
```



(3) 代理类的局限性
不能与可以被客户端扩展的类兼容； 
不能与对象图中包仿循环的某些类兼容；

(4) 使用writeReplace的几点注意事项
1) 实现了writeReplace就不要实现writeObject方法，因为writeReplace返回值会被自动写入输出流中，相当于自动调用了writeObject(writeReplace()) 
2) writeReplace的返回值必须是可序列化的； 
3) 若返回的是自定义类型的对象，该类型必须是实现了序列化。 
4) 使用writeReplace替换写入后的对象不能通过实现readObject方法实现自动恢复，因为对象默认被彻底替换了，就不存在自定义序列化问题，直接自动反序列化了。 
5) writeObject和readObject配合使用，实现了writeReplace就不再需要writeObject和readObject