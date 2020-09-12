先给大家列出需要的术语

![img](https://img-blog.csdn.net/20180715092815692?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NoYXJKYXlfTGlu/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)

### **在新代码中（jdk1.5以后）不要使用原生态的类型。**

为什么会有泛型 list<E>

如果没有泛型的话，我们从一个没有定义类型的集合里面取数据的时候就不知道是什么类型，强制转换会出现ClassCastException异常

这时候原生态类型就需要升级->泛型，有了泛型的规约这种情况就会避免了

**简单说来，就是在运行前即可发现错误。**

原生态的List和参数化的类型List<Object>之间有什么区别呢？不严格的讲，前者逃避了泛型检查，后者明确告诉编译器，它能够持有任意类型的对象。虽然可以将List<String>传递给类型为List的参数，但是不能将它传给类型为List<Object>的参数。

```
// Use raw type (List) - fails at runtime!
public static void main(String[] args) {
    List<String> strings = new ArrayList<String>();
    unsafeAdd(strings, new Integer(42));
    String s = strings.get(0); // Compiler - generated cast
}

private static void unsafeAdd(List list, Object o) {
    list.add(o);
}
```



Set<?>：无限制的通配符类型（unbounded wildcard type）

如果要使用泛型，但不确定或者不关心实际的类型参数，就可以使用一个问号代替

**总结：**

Set<Object>是个参数话类型，表示可以包含任何对象类型的一个集合；

Set<?>则是一个通配符类型，表示只能包含某种未知对象类型的一个集合；

Set则是个原生态类型，它脱离了泛型系统。

前两种是安全的，最后一种不安全。

**我们常常在源码中看到的Java泛型中的标记符含义：** 

 E - Element (在集合中使用，因为集合中存放的是元素)

 T - Type（Java 类）

 K - Key（键）

 V - Value（值）

 N - Number（数值类型）

？ -  表示不确定的java类型

### **消除非受检警告**

​         在使用泛型时，使用编译器完成代码编写的时候，可能会有黄色的波浪线提示警告，不要忽略它，每一处警告可能都是一个ClassCastException。

​        可以使用@SuppressWarnings("uncheched")消除警告，这个声明可以作用的范围十分广泛，类、方法、变量上都可以，尽可能的使作用范围小一些，在声明这个注解的时候最好加上注释，注明理由为什么这里是安全无须检查？

​       **但是在使用@SuppressWarnings("uncheched")时，必须确保代码的正确**

```
 public <T> T[] toArray(T[] a){
        if(a.length < size){
            T[] result=(T[])Arrays.copyOf(elements,size,a.getClass());
            return result
        }
        System.arraycopy(elements,0,a,0,size);
        if(a.length > size)
            a[size] = null;
        return a;
    }


public <T> T[] toArray(T[] a) {
        if (a.length < size) {
            @SuppressWarning("unchecked")
            T[] result = (T[]) Arrays.copyOf(elements, size, a.getClass());
            return result;
        }
        System.arraycopy(elements, 0, a, 0, size);
        if (a.length > size)
            a[size] = null;
        return a;
    }
```



# 列表优先于数组

一般来说，数组和泛型不是很好的能混合使用，在使用泛型的时候优先使用列表

具体原因有：

**1、数组是协变的，泛型是不可变的。**

什么是协变？ 如果Sub是Super的子类型，那么数组类型Sub[] 就是Super[]子类型。 
什么是不可变？ 对于任意两个不同类型的type1和type2，List< type1 >既不是List< type2 >的子类型，也不是List< type2 >的超类型。 
那么你可能会觉得泛型是有缺陷的，恰恰相反，有缺陷的是数组。 
下面代码是合法的：

```java
//Fails at runtime
Object[] objectArray=new Long[1];
ObjectArray[0]="I don't fit in";//Throws ArrayStoreException
```



下面的代码是不合法的：

```java
//Won't compile
List<Object> o1=new ArrayList<Long>();//Incompatible types
o1.add("I don't fit in")
```



其实都是错误的，但是数组只有在运行时才会报错。列表在编译时就会报错。

**2.数组是具体化的，泛型是通过擦除来实现的。**

数组是具体化的（reified）。因此数组会在运行时才知道并检查他们的元素类型约束。泛型是通过擦除来实现的。因此泛型只在编译时强化他们的类型信息，并在运行时丢弃（或者擦除）他们元素的类型信息。

创建泛型数组是非法的：

```
//Cannot create a generic array of List<String>
List<String>[] stringLists = new List<String>[1];
```

