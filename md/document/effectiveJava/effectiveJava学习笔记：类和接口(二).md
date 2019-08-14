# 一、复合优先于继承

为什么复合优先于继承? 
1.继承违反了封装原则,打破了封装性 
2.继承会不必要的暴露API细节,称为隐患
3.继承限定了类的性能,它会把它的缺陷传递给子类

下面的代码通过继承来试图记录插入元素的数量。HashSet包含两个可以增加元素的方法，add和addAll。因此这两个方法都需要覆盖。

```
package com.ligz.Chapter4.Item16;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author ligz
 */
public class InstrumentedHashSet<E> extends HashSet<E> {
    //The number of attempted element insertions
    //试图插入元素的数量
    private int addCount = 0;

    public InstrumentedHashSet(){

    }

    public InstrumentedHashSet(int initCap,float loadFactor){
        super(initCap,loadFactor);
    }

    @Override
    public boolean add(E e){
        addCount++;
        return super.add(e);
    }

    @Override
    public boolean addAll(Collection<? extends E> c){
        addCount+=c.size();
        return super.addAll(c);
    }

    public int getAddCount(){
        return addCount;
    }

}
```


使用main方法测试

```
InstrumentedHashSet<String> s = new InstrumentedHashSet<>();
        s.addAll(Arrays.asList("Snap","Crackle","Pop"));

        System.out.println(s.getAddCount());
```


我们想要得到的是3，即插入了3条记录。

但是返回的值为6
因为在hashSet内部 addAll方法是基于add实现的。等于再次实现了三遍add方法。



**1.复合:不必扩展现有的Set类,而是在此类中加一个私有域,它引用现有类的一个实例** 

**2.它的封装特性使得它更加健壮灵活** 

**3.复合允许设计新的API隐藏父类的缺点**

复合方法：先创建一个转发类，该转发类实现接口内的所有方法，新类中的每个实例方法都可以调用被包含的现有类实例中对应的方法并返回结果。

复合:不必扩展现有的Set类,而是在此类中加一个私有域,它引用现有类的一个实例

```
package com.ligz.Chapter4.Item16;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * @author ligz
 */
public class ForwardingSet<E> implements Set<E> {
    private final Set<E> s;
 
    public ForwardingSet(Set<E> s) { this.s = s; }
 
    public void clear() { s.clear(); }
 
    public boolean contains(Object o) { return s.contains(o); }
 
    public boolean isEmpty() { return s.isEmpty(); }
 
    public int size() { return s.size(); }
 
    public Iterator<E> iterator() { return s.iterator(); }
 
    public boolean add(E e) { return s.add(e); }
 
    public boolean remove(Object o) { return s.remove(o); }
 
    public boolean containsAll(Collection<?> c) { return s.containsAll(c); }
 
    public boolean addAll(Collection<? extends E> c) { return s.addAll(c); }
 
    public boolean removeAll(Collection<?> c) { return s.removeAll(c); }
 
    public boolean retainAll(Collection<?> c) { return s.retainAll(c); }
 
    public Object[] toArray() { return s.toArray(); }
 
    public <T> T[] toArray(T[] a) { return s.toArray(a); }
 
    @Override
    public boolean equals(Object o) { return s.equals(o); }
 
    @Override
    public int hashCode() { return s.hashCode(); }
 
    @Override
    public String toString() { return s.toString(); }
}
```


```
package com.ligz.Chapter4.Item16;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author ligz
 */
public class InstrumentedSet<E> extends ForwardingSet<E> {
    private int addCount = 0;
 
    public InstrumentedSet(Set<E> s) {
        super(s);
    }
 
    @Override
    public boolean add(E e) {
        addCount++;
        return super.add(e);
    }
 
    @Override
    public boolean addAll(Collection<? extends E> c) {
        addCount += c.size();
        return super.addAll(c);
    }
 
    public int getAddCount() {
        return addCount;
    }
 
    public static void main(String[] args) {
        InstrumentedSet<String> s =
                new InstrumentedSet<String>(new HashSet<String>());
        s.addAll(Arrays.asList("Snap", "Crackle", "Pop"));
        System.out.println(s.getAddCount());
    }
}
```


InstrumentedSet被称为包装类，包装类不适合回调框架。

**当子类真正是超类的子类型的时候,可以使用继承**

# 二、要么为继承而设计，并提供文档说明，要么就禁止继承

上面在子类化HashSet的时候，并无法说明覆盖add方法是否会影响addAll的方法行为。

对于为了继承而设计的类，唯一的测试方法就是编写子类。

为了允许继承，类还必须遵守其他约束。构造器决不能调用可被覆盖的方法。

```
package com.ligz.Chapter4.Item17;

/**
 * @author ligz
 */
public class Super {
    
    public Super(){
    	System.out.println("父类初始化");
        overrideMe();
    }
    
    public void overrideMe(){
    	System.out.println("父类override");
    }
}
```


```
package com.ligz.Chapter4.Item17;

/**
 * @author ligz
 */
import java.util.Date;

public class Sub extends Super{
    private final Date date;
    
    Sub(){
    	System.out.println("子类初始化");
        date =new Date();
    }
    
    @Override
    public void overrideMe(){
    	System.out.println("子类override");
        System.out.println("时间"+date);
    }
    
    public static void main(String[] args) {
        Sub sub = new Sub();
        sub.overrideMe();
    }
}
```


我们得到的答案是

```
父类初始化
子类override
时间null
子类初始化
子类override
时间Wed Sep 19 19:44:01 CST 2018
```


在debug中发现this指的是Sub，date是空的，这样知道就知道父类的构造方法中调用的overrideMe是那里的，所以也就明白了父类为啥调用的是子类的方法了。main里面就是实例化了子类，this代表的就是子类，所以父类中构造方法中调用overrideMe的就是子类的overrideMe。

如果对于那些并非是为了安全进行子类化而设计和编写文档的类，要禁止继承。两种方法是把类声明为final，另一种是把构造器都变为私有的。

# 三、接口优于抽象类

![img](https://img-blog.csdn.net/20180919203136300?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM5MDcxNTMw/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)

Java程序设计语言提供两种机制，可以用来定义允许多个实现的类型：接口和抽象方法，这两者直接醉为明显的区别在于，抽象类允许某些方法的实现，但接口不允许，一个更为重要的区别在于，为了实现由抽象类定义的类型，类必须成为抽象类的一个子类。任何一个类，只要定义了所有必要的方法，并且遵守通用约定，它就被允许实现一个借口，而不管这个类是处于类层次的哪个位置。因为Java只允许单继承，所有抽象类作为类型定义受到类极大的限制。


### 接口优点:

- 现有的类可以很容易被更新, 以实现新的接口
- 接口是定义混合类型的理想选择: 一个类可以实现多个接口以表现出不同的类型
- 接口允许我们构造非层次结构的类型框架: 一个类可以实现多个接口以表现出不同的类型, 接口是可以多继承的, 一个接口可以继承其它接口以构造非层次结构的类型

### 接口缺点:

- 接口一旦被公开发行, 并且已被广泛实现, 想在改变这个接口几乎是不可能的, 必须在初次设计的时候就保证接口是正确的, 如果接口具有严重的缺陷, 它可以导致API彻底失败

​      假如我们有两个接口，一个表示歌唱家，另一个表示作曲家，在现实生活中，有很多人即是歌唱家又是作曲家，如果是接口，我只需要同时实现这两个接口就可以，如果是抽象类，因为Java是单继承的，我就没有办法描述这一类的人。

　　虽然接口不允许包含方法的实现，但是，使用接口来定义类型并不妨碍你为程序猿提供实现上的帮助。**通过对你导出的每个重要接口都提供一个抽象骨架的实现类，把接口和抽象类的优点都结合起来。**接口的作用仍然是定义类型，但是骨架的实现类接管类所有与接口实现相关的工作。

　　骨架为接口提供实现上的帮助，但又不强加“抽象类被用作类型定义时”所特有的严格限制。对于接口大多数的实现来讲，扩展骨架实现类是个很显然的选择，但不是必须的。如果预制的类无法扩展骨架实现类，这个类始终可以收工实现这个接口。此外，骨架实现类仍然有助于接口的实现。实现类这个接口的类可以把对于这个接口方法的调用，转发到一个内部私有类的实例上，这个内部私有类扩展骨架实现类。这种方法被称作模拟多重继承。这项技术具有多重继承的绝大多数有点，同时又避免了相应的缺陷。

例如，下面是Map.Entry接口的骨架实现类：

```
package com.ligz.Chapter4.Item18;

import java.util.Map;

/**
 * @author ligz
 */
public abstract class AbstractMapEntry<K, V> implements Map.Entry<K, V>{
	 
    // Primitive operations
    public abstract K getKey();
    public abstract V getValue();
     
    // Entries in modifiable maps must override this method
    public V setValue(V value) {
        throw new UnsupportedOperationException();
    }
     
    // Implements the general contract of Map.Entry.equals
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Map.Entry)) {
            return false;
        }
        Map.Entry<?, ?> arg = (Map.Entry) obj;
        return equals(getKey(), arg.getKey()) && equals(getValue(), arg.getValue());
    }
     
    private static boolean equals(Object obj1, Object obj2) {
        return obj1 == null ? obj2 == null : obj1.equals(obj2);
    }
     
    // Implements the general contract of Map.Entry.hashCode
    @Override
    public int hashCode() {
        return hashCode(getKey()) ^ hashCode(getValue());
    }
     
    private static int hashCode(Object obj) {
        return obj == null ? 0 : obj.hashCode();
    }
}
```


天才！！abstract中不用实现interface中的所有实现，在继承中把未实现的补上即可。

骨架实现方法：

首先是，接口定义了功能性的方法； 
然后，一个抽象类实现了这个接口，作为骨架实现类，它完成了接口的一部分实现（默认实现） 
最后是一个普通类，继承自这个抽象的骨架实现类，并完成了抽象类中未实现的方法。

我们简单的分析Java的源码发现

public abstract class AbstractCollection<E> implements Collection<E>

public abstract class AbstractList<E> extends AbstractCollection<E> implements List<E>

public interface List<E> extends Collection<E> 

如果我们要继承AbstractCollection类我们必须实现Collection类在AbstractCollection未实现的类，如iterator()和size()。

# 三、接口只用于定义类型

1、当类实现接口时，接口就充当可以引用这个类的实例类型。
因此，类实现了接口，就表明客户端对这个类的实例可以实施某些动作。为了任何其他目的而定义的接口是不恰当的。

2、常量接口是对接口的一种不良使用。类在内部使用某些常量，纯粹是实现细节，实现常量接口，会导致把这样的实现细节泄露到该类的导出API中，因为接口中所有的域都是及方法public的。类实现常量接口，这对于这个类的用户来讲并没有实际的价值。
实际上，这样做返回会让他们感到更糊涂，这还代表了一种承诺：如果在将来的发行版本中，这个类被修改了，它不再需要使用这些常量了，依然必须实现这个接口，以确保二进制兼容性。
如果非final类实现了常量接口，它的所有子类的命名空间都受到了污染。Java平台类库中存在几个常量接口，如java.io.ObjectStreamConstants，这些接口都是反面典型，不值得效仿。

![img](https://img-blog.csdn.net/2018092018390140?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM5MDcxNTMw/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)

3、 那既然不适合存在全部都是导出常量的常量接口，那么如果需要导出常量，它们应该放在哪里呢？
如果这些常量与某些现有的类或者接口紧密相关，就应该把这些常量添加到这个类或者接口中，注意，这里说添加到接口中并不是指的常量接口。在Java平台类库中所有的数值包装类都导出MIN_VALUE和MAX_VALUE常量。
如果这些常量最好被看作是枚举类型成员，那就应该用枚举类型来导出。否则，应该使用不可实例化的工具类来导出这些常量。

```
public class PhysicalConstants {  
private PhysicalConstants() {}  
public static final double AVOGADROS_NUMBER = 6.23156412e23;  
public static final double BOLTZMANN_CONSTANT = 1.12588456e-23;  
...  
} 
```


### 接口应该只被用来定义类型，它们不应该用来导出常量

# 四、类层次优于标签类

标签类很少有适用的时候.当你想要编写一个包含显示标签域的类时,应该考虑一下,这个标签是否可以被取消,这个类是否可以用类层次来代替.当你遇到一个包含标签域的现有类时,就要考虑它重构到一个层次结构中去.

标签类：

```
public class Figure {
    enum Shape{RECTANGLE,CIRCLE};

    final Shape shape;

    double width;
    double length;

    double radius;

    Figure(Double radius){
        shape = Shape.CIRCLE;
        this.radius = radius;
    }

    Figure(double width,double length){
        shape = Shape.RECTANGLE;
        this.width = width;
        this.length = length;
    }

    double area(){
        switch(shape){
            case RECTANGLE:
                return width*length;
            case CIRCLE:
                return Math.PI*(radius*radius);
            default:
                return 0;
        }
    }
}
```


类层次：

```
/**
 * 子类型化:定义能表示多种风格对象的单个数据类型,标签类是对其的一种效仿
 */
public abstract class Figure {
    abstract double area();
}

public class Circle extends Figure {
    final double radius;

    Circle(double radius) {
        this.radius = radius;
    }

    @Override
    double area() {
        return Math.PI * (radius * radius);
    }

}

public class Rectangle extends Figure {

    final double length;
    final double width;

    Rectangle(double length, double width) {
        this.length = length;
        this.width = width;
    }

    @Override
    double area() {
        return length * width;
    }
}
```


# 五、用函数对象表示策略

如果一个类仅仅导出一个方法，它的实例就等同于一个指向该方法的指针，这样的实例就是**函数对象**

```
public class StringLengthComparator {  
    public int compare(String s1, String s2) {  
        return s1.length() - s2.length();  
    }  
} 
```


指向StringLengthComparator对象的引用可以被当作一个指向该比较器的“函数指针（function pointer）”，可以在任意一对字符串上被调用。换句话说。StringLengthComparator实例适用于字符串比较操作的具体策略（concrete strategy）。

# 六、优先考虑静态成员类

如果一个嵌套类需要在单个方法之外仍然是可见的，或者他太长了，不适合方法内部，就应该使用成员类。如果成员类的每个实例都需要一个指向其外围实例的引用，就要把成员类做成非静态的；否则就做成静态的。假设这个嵌套类属于一个方法的内部，如果你需要在一个地方创建实例，并且已经有了一个预置的类型可以说明这个类的特征，就把他做成匿名类；否则，就做成局部类。