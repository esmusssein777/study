# 一、使类和成员的可访问性最小化

### 对于类

对于类，只有public和package-private两种访问级别。package-private是缺省的，也就是默认的。

1.对于顶层的类来说，只有包级私有和公有两种可能，区别是包级私有意味着只能在当前包中使用，不会成为导出api的一部分，而公有意味着导出api，你有责任去永远支持它。所以，为了使访问最小化，能包级私有就应该声明为包级私有。

2.对于包级私有类来说，如果只在某一个类中被使用，那么就直接让这个包级私有类成为这个类的嵌套类，这样就能让访问级别再次缩小。

### 对于成员

成员包括域，方法，嵌套类和嵌套接口

访问级别有私有的，包级私有的，受保护的和公有的四种。

1.**实例域绝对不能是公有的**，声明实例域是公有的，相当于限制了对储存在这个域中的值进行限制的能力，破坏了封装性。 
而静态域 也只有在提供常量的抽象类中，通过公有的静态final域来暴露。 

Employee类添加一个实例域id和一个静态域nextld：

class Employee

 {

​     private int id;

​     private static int nextId=1；

 }

常量的抽象类中通过公有的静态final域来暴露 public final static int i = 1;

\2. 设计类时，应当把所有的其他成员都变成私有的。 
只有当同一个包中另一个类真正需要访问一个成员的时候，才应该删除private修饰符，把该成员变成包级私有的。 
其实这两者都是类的实现的一部分，不会影响到他的api。

3.如果对于公有类的成员，访问级别从包级私有变成保护级别时，要额外小心，因为保护的成员是导出api的一部分，必须得到永久支持。

4.**方法覆盖了超类中的一个方法，子类中的访问级别就不允许低于父类的访问级别**。这个规则限制了方法的可访问性的能力，保证可以使用超类的地方都可以使用到子类。 

### **二、**在公有类中使用访问方法而非公有域

我们不能这样做，

```
Class Point {
public double x;
public double y;
}
```


Point类的数据域是可以直接被访问的，这样的类没有提供封装。
1、如果不改变API，就无法改变它的数据表示法（比如，使用一个比double更高精度的类来表示x和y）

2、也无法强加任何约束条件（比如以后我们可能会希望x和y不会超过某个值）。

下面是我们推荐使用的方法。使用私有域和公有访问方法的公有类是比较合适的。在它所在的包的外部访问时，提供访问方法，以保留将来改变该类的内部表示法的灵活性。

```
	Class Point {
		private double x;
		private double y;
		//构造方法
		public Point(double x, double y) {
			this.x = x;
			this.y = y;
		}
 
		public double getX() { return x; }
		public double getY() { return y; }
 
		public void setX(double x) { this.x = x; }
		public void setY(double y) { this.y = y; }
	}
```


让公有类暴露域不是好办法，但如果域是不可变的，这种做法的危害会较小。

```
	public final class Time {
		private static final int HOURS_PER_DAY = 24;
		private static final int MINUTES_PER_HOURS = 60;
 
 
		public final int hour;// hour是不可变域
		public final int minute;// minute是不可变域
 
 
		public Time(int hour, int minute) {
			// 无法改变类的表示法，但是可以强加约束条件
			if (hour < 0 || hour > HOURS_PER_DAY)
				throw new IllegalArgumentException("Hour: " + hour);
			if (minute < 0 || minute >= MINUTES_PER_HOUR)
				throw new IllIllegalArgumentException("Minute: " + minute);
			this.hour = hour;
			this.minute = minte;
		}
	}
```


总之。公有类永远都不应该暴露可变的域。

虽然还是有问题，但是让公有类暴露不可变的域其危害比较小。但是，有时候会需要用包级私有的或者私有的嵌套类来暴露域，无论这个类是可变还是不可变的。

### 三、使可变性最小化

不可变类是它的实例不能被修改的类。每个实例中所有的信息，必须在创建的时候提供，并在其整个对象周期内固定不变。

为了使类成为不可变的，一般遵循以下几个原则：

1. 不要提供任何会修改对象状态的方法（改变对象属性的方法，也称为mutator，也就是set方法）。
2. 保证类不会被扩展。防止恶意的子类假装对象的状态已经改变，一般是将该类设置为final。
3. 使所有的域都为final。通过系统的强制方法，清晰的表示你的意图。
4. 使所有的域都为private。防止通过继承获取访问被域引用的可变对象的权限，实际上用final修饰的public域也足够满足这个条件，但是不建议这么做 ，为以后的版本的维护作考虑。
5. 确保对于任何可变组件的互斥访问。如果类具有指向可变对象的域，则必须确保该类的客户端无法获取指向这些对象的引用。并且， 永远不要用客户端提供的对象引用来初始化这样的域，也不要从任何方法（access）中返回该对象的引用。在构造器中和访问方法中，请使用保护性拷贝（defensive copy）。（保护性拷贝，比如在构造器中，需要传递某个对象进行初始化，那么初始化的时候不要使用这个对象的引用，因为外部是可以修改这个引用中的数据的。因此初始化的时候，应该使用这个引用中的数据重新初始化这个对象，可参考39条）PS:没怎么看懂，以后再看。。。

下面给出一个关于不可变类的例子：

```
// Immutable class - pages 76-78
package effectiveJava.Chapter4.Item15;
 
public final class Complex {
	private final double re;
	private final double im;
 
	public Complex(double re, double im) {
		this.re = re;
		this.im = im;
	}
 
	public static final Complex ZERO = new Complex(0, 0);
	public static final Complex ONE = new Complex(1, 0);
	public static final Complex I = new Complex(0, 1);
 
	// Accessory with no corresponding mutators
	public double realPart() {
		return re;
	}
 
	public double imaginaryPart() {
		return im;
	}
 
	public Complex add(Complex c) {
		return new Complex(re + c.re, im + c.im);
	}
 
	public Complex subtract(Complex c) {
		return new Complex(re - c.re, im - c.im);
	}
 
	public Complex multiply(Complex c) {
		return new Complex(re * c.re - im * c.im, re * c.im + im * c.re);
	}
 
	public Complex divide(Complex c) {
		double tmp = c.re * c.re + c.im * c.im;
		return new Complex((re * c.re + im * c.im) / tmp, (im * c.re - re * c.im) / tmp);
	}
 
	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Complex))
			return false;
		Complex c = (Complex) o;
 
		// See page 43 to find out why we use compare instead of ==
		return Double.compare(re, c.re) == 0 && Double.compare(im, c.im) == 0;
	}
 
	@Override
	public int hashCode() {
		int result = 17 + hashDouble(re);
		result = 31 * result + hashDouble(im);
		return result;
	}
 
	private int hashDouble(double val) {
		long longBits = Double.doubleToLongBits(re);
		return (int) (longBits ^ (longBits >>> 32));
	}
 
	@Override
	public String toString() {
		return "(" + re + " + " + im + "i)";
	}
}
```


以上是一个关于复数的类，其中部分方法，如：加减乘除，可以返回新的对象，而不是修改当前的对象。很多不可变类都使用了这种方法，这是一种常见的函数式（functional）做法，因为它们返回的是函数的结果，对对象进行运算的结果，而不改变这些对象。

使用：

事实上，不可变对象非常简单，它只有一种状态，创建时的状态。只要你在构造器中能保证这个类的约束关系，并遵守以上几条原则，那么在该对象的整个生命周期里，永远都不会再发生改变，维护人员也不需要过额外的时间来维护它。

另外，可变的对象拥有任意复杂的状态空间。如果文档中没有对其精确的描述，那么要可靠的使用一个可变类是非常困难的。

由于不可变对象本身的特点，它本质上就是线程安全的，不需要对其进行同步。因为不可变对象的状态永远不发生改变，所以当多个线程同时访问这个对象的时候，对其不会有任何影响。基于这一点，不可变类应该尽量鼓励重用现有的实例，而不是new一个新的实例。方法之一就是，对于频繁用到的值，使用public static final。如下：

```
	public static final Complex ZERO = new Complex(0, 0);
	public static final Complex ONE = new Complex(1, 0);
	public static final Complex I = new Complex(0, 1);
```


不可变类还可以提供一些静态工厂（见第1条），将频繁请求的实例保存起来，所有基本类型的包装类和BigInteger都有这样的工厂。静态工厂还可以代替公有构造器，使得以后可以有添加缓存的灵活性。

永远不需要对不可变对象进行保护性拷贝，因为不可变对象内部的数据不可变，没有保护性拷贝的必要。

不可变对象唯一的缺点是，对于每个不同的值，都需要一个单独的对象。如果是一个大型对象，那么创建这种对象的代价可能很高。如果你执行一个多步骤操作，然而除了最后的结果之外，其他的对象都被抛弃了，此时性能将会是一个比较大的问题。有两种常用的方法：

1. 将多步操作作为一个安全的基本操作提供，这样就免除了中间的多步对象。
2. 如果无法精准的预测客户端将会在不可变的类上执行那些复杂的多步操作，不如提供一个公有的可变配套类。StringBuilder就是一个很好的例子。

# 奇妙的方法

除了使类成为final这种方法之外，还有另外一种更加灵活的方法来实现不可变类。让类的所以构造器都变成私有的，并添加静态工厂来代替公有的构造器。

```
package com.ligz.three;

/**
 * @author ligz
 */
public class Complex {
	private final double re;
	private final double im;
	
	public Complex(double re, double im) {
		this.re = re;
		this.im = im;
	}
	
	public static Complex valueOf(double re, double im) {
		return new Complex(re, im);
	}
	
	public static void main(String[] args) {
		final Complex c= Complex.valueOf(1, 2);
	}
	
}	
```


这种方法最大的好处是它允许多个包级私有类的实现，并且在后续的维护中扩展静态工厂的功能。例如，你想添加一个通过极坐标生成复数的功能。如果通过构造器，可能会显得非常凌乱，但是只用添加第二个静态工厂即可。

```
	public static Complex valueOfPolar(double r, double theta) {
		return new Complex(r * Math.cos(theta), r * Math.sin(theta));
	}
```


# 总结

开头所提到的几个原则，其实强硬了，为了提高性能，往往有所放松。事实上应该是，没有一个方法能够对对象的状态产生外部可见的改变（在初始化之后，自身还是可以改变自身内部的数据），许多不可变对象拥有一个或多个非final的域，在第一次请求的时候，将一些昂贵的计算结果缓存在这些域里，如果将来还有同样的计算，直接将缓存的数据返回即可。因为对象是不可变的，因此如果相同的计算再次执行，一定会返回相同的结果。例如，延迟初始化（lazy  initialization）就是一个很好的例子。

总之，不要为每个get方法都写一个set方法，除非有必要。换句话说，除非有很好的理由让类成为可变的类，否则就应该是不可变。尽管它存在一些性能问题，但是你总可以找到让一些较小的类成为不可变类。

构造器应该创建完整的构造方法，并建立起所有的关系约束。不应该在构造器或者静态工厂外，再提供公有的初始化方法，或者是重新初始化方法。