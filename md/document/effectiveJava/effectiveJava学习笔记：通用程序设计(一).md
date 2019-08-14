# 将局部变量的作用域最小化

其实大部分人还是在第一次使用变量的时候声明变量的，在开头就将所有变量声明的还没见过。

要使局部变量的作用域最小化，最有力的方法就是在第一次使用它的地方声明。

但是，这里书中讲到了for循环优于while循环，值得我们注意。

for循环将变量声明在循环内，不会在后面引起手残的错误。

```
for (Element e : c) {
       doSomething(e);
   }

 for (Interator i = c.iterator(); i.hasNext();) {
       doSomething((Element)i.next())；
   }
```



但是while变量在外面，如果变量一多，就容易不小心引发错误。

```
 List<string> list =new  ArrayList<string>();
        while (list.iterator().hasNext()) {
            list.iterator().next();
        }
```



# for-each循环优先于for循环

首先，for-each比for循环要优雅简洁的多，一眼就能看的明明白白，其次不容易写错。

for-each循环在预防Bug方面有着传统的for循环无法比拟的优势，并且没有性能损失。应该尽可能的使用for-each循环。遗憾的是，有三种常见的情况无法使用for-each循环：

- **过滤**——如果需要遍历集合，并删除选定的元素，就需要使用显示的迭代器，以便可以调用他的remove方法。
- **转换**——如果需要遍历列表或者数组，并取代他部分或者全部的元素值，就需要列表迭代器或者数组索引，以便设定元素的值。
- **平行迭代**——如果需要并行的遍历多个集合，就需要显式的控制迭代器或者索引变量，以便所有迭代器或者索引变量都可以得到同步前移。

大致对我们来讲就是除了删除元素和需要索引，用for-each没什么问题。



### 使用库类没啥说的，标准库类都是大牛写的，如果你没有强烈的自信和兴趣，还是使用库类把。



# 如果需要精确的答案，请避免使用float和double

 double和float都是科学计算，就是不保证精度的。

```
public class Item48 {
	public static void main(String[] args) {
		System.out.println(1.03-.42);
	}
}
```



结果是0.6100000000000001 

float和double类型主要是为了科学计算与工程计算而设计的，它们并没有提供完全精确的结果，所以不应该被用于需要精确结果的场合。在商业计算中要用BigDecimal。BigDecimal所创建的是对象，我们不能使用传统的+、-、*、/等算术运算符直接对其对象进行数学运算，而必须调用其相对应的方法。

2.构造器描述

```
BigDecimal(int) 创建一个具有参数所指定整数值的对象
BigDecimal(double) 创建一个具有参数所指定双精度值的对象
BigDecimal(long) 创建一个具有参数所指定长整数值的对象
BigDecimal(String) 创建一个具有参数所指定以字符串表示的数值的对象
```



3.方法描述

```
add(BigDecimal) BigDecimal对象中的值相加，然后返回这个对象
subtract(BigDecimal) BigDecimal对象中的值相减，然后返回这个对象
multiply(BigDecimal) BigDecimal对象中的值相乘，然后返回这个对象
divide(BigDecimal) BigDecimal对象中的值相除，然后返回这个对象
toString() 将BigDecimal对象的数值转换成字符串
doubleValue() 将BigDecimal对象中的值以双精度数返回
floatValue() 将BigDecimal对象中的值以单精度数返回
longValue() 将BigDecimal对象中的值以长整数返回
intValue() 将BigDecimal对象中的值以整数返回
```



4.BigDecimal比较

```
BigDecimal是通过使用compareTo(BigDecimal)来比较的
```



5.BigDecimal缺点

与使用基本运算类型想比，这样做很不方便
性能比double和float差，在处理庞大，复杂的运算时尤为明显。

但是为了保证精度必要时可以使用。



# 基本类型优先于装箱基本类型

在基本类型和装箱基本类型中有3个主要区别： 
1、基本类型只有值，装箱基本类型具有与它们的值不同的统一性，就是两个装箱基本类型可以具有相同的值和不同的同一性。
2、基本类型只有功能完备的值，而每个装箱基本类型除了它对应基本类型的所有功能值外，还有个非功能值–null； 
3、基本类型比装箱基本类型更节省时间和空间。

1、对装箱基本类型运行 = 几乎都是错误的，因为他们的值相同的时候同一性不相同。

2、

```
public class Unbelievable {
    static Integer i;
    public static void main(String[] args) {
        if (i == 42) {
            System.out.println("Unbelievable");
        }
    }
}
```



会得到一个NullPointerException异常。

3、这个程序运行起来比预期的要慢一些，因为不小心将局部变量sum声明成了装箱基本类型Long，程序编译起来没有警告或者错误，但是变量被反复的装箱和拆箱，导致性能明显下降

这个在之前也讲过

```
public static void main(String[] args) {
        Long sum = 0L;
        long startTime = System.currentTimeMillis();
        for(long i = 0;i<Integer.MAX_VALUE;i++) {
            sum += i;
        }
        System.out.println(sum);
        System.out.println(System.currentTimeMillis() - startTime);
    }
```



可以试一下Long和long的时间相差多少。

# 如果其他类型更合适，则尽量避免使用字符串

1.字符串不适合代替其他的值类型。只有当数据确实是文本信息时，才应该使用字符串，如果是数值，就应该被转换为适当的数值类型，如果是一个“是-或-否”的问题答案，应该被转换为boolean类型，如果是一个对象，应该使用对象引用来引用它。

2.字符串不适合代替枚举类型：枚举类型比字符串更加适合用来表示枚举类型的常量。

3.字符串不适合代替聚集类型。如果一个实体有多个组件，用字符串来表示这个实体通常不恰当，String compundKey = className + "#" + i.next();

更好的做法是编写一个类来描述这个数据集，通常是一个私有的静态成员类。

4.字符串不适合代替能力表（暂时没看懂）