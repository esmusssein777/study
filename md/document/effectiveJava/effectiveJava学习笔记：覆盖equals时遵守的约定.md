hashCode 方法用于散列集合的查找，equals 方法用于判断两个对象是否相等。

### 我们为什么需要重写hashCode()方法和equals()方法?

有时在我们的业务系统中判断对象时有时候需要的不是一种严格意义上的相等，而是一种业务上的对象相等。在这种情况下，原生的equals方法就不能满足我们的需求了.我们所知道的JavaBean的超类(父类)是Object类,JavaBean中的equals方法是继承自Object中的方法.Object类中定义的equals()方法是用来比较两个引用所指向的对象的内存地址是否一致.并不是比较两个对象的属性值是否一致,所以这时我们需要重写equals()方法.

# 覆盖equals时遵守下面的约定

- 自反性(reflexive):对于任何非null的引用值x，x.equals(x)必须返回true
- 对称性(symmetric):对于任何非null的引用值x和y，当且仅当y.equals(x)返回true时，x.equals(y)必须返回true
- 传递性(transitive):对于任何非null的引用值x、y和z，如果x.equals(y)返回true，并且y.equals(z)返回true，那么x.equals(z)必须返回true
- 一致性(consistent):对于任何非null的引用值x和y，只要equals的比较操作在对象中所用的信息没有被修改，多次调用x.equals(y)就会一致地返回true，或者一致地返回false
- 非空性(Non—nullity):对于任何非null的引用值x，x.equals(null)必须返回false

### 1、自反性。

如果不是特意一般是不会在第一条犯错的。不需要特意说明。

### 2、对称性。

NotSring类中的equals方法是忽略大小写的，只要值相等即返回true。但是String类中的equals并不会忽略大小写，即使String类本身已经重写了equals，只要值相等就返回true，但这里恰恰是大小写不同值相同的一种方式，所以就使得s.equals(ns)返回false了。

```
package com.ligz.two.equals;

/**
 * @author ligz
 */
public class NotString {
	private final String s;
	
	public NotString(String s) {
		if(s == null) {
			throw new NullPointerException();
		}
		this.s = s;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof NotString) {
			return s.equalsIgnoreCase(((NotString) obj).s);
		}
		
		if(obj instanceof String) {
			return s.equalsIgnoreCase((String)obj);
		}
		return false;
	}
}
```



### ![img](https://img-blog.csdn.net/2018091516035567?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM5MDcxNTMw/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)

### 3、传递性。

下面的Point和其子类ColorPoint，在子类新加一个颜色特性，就会很容易违反这条约定。

首先我们看违反对称性的代码

Point.java

```
package com.ligz.two.equals;

/**
 * @author ligz
 */
public class Point {
	private final int x;
    private final int y;

    public Point(int x, int y) {
        super();
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Point)) 
            return false;
        else {
            Point p = (Point)obj;
            return p.x==x&&p.y==y;
        }
    }
}
```



ColorPoint.java

```
package com.ligz.two.equals;

/**
 * @author ligz
 */
public class ColorPoint extends Point{
	private final String color;

    public ColorPoint(int x, int y, String color) {
        super(x, y);
        this.color = color;
    }


    //违背对称性
    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof ColorPoint))
            return false;
        else
            return super.equals(obj)&&((ColorPoint)obj).color==color;
    }
}
```



测试对称性

```
package com.ligz.two.equals;

/**
 * @author ligz
 */
public class ColorTest {
	public static void main(String[] args) {
		Point p = new Point(1, 2);
        ColorPoint cp = new ColorPoint(1, 2, "red");
        ColorPoint cp2 = new ColorPoint(1, 2, "blue");
        System.out.println("p覆盖的equals");
        System.out.println(p.equals(cp));
        System.out.println("cp覆盖的equals");
        System.out.println(cp.equals(p));
	}
}
```



![img](https://img-blog.csdn.net/20180915162009405?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM5MDcxNTMw/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)

不满足对称性，我们在ColorPoint.java中加上忽略颜色信息的代码

```
package com.ligz.two.equals;

/**
 * @author ligz
 */
public class ColorPoint extends Point{
	private final String color;

    public ColorPoint(int x, int y, String color) {
        super(x, y);
        this.color = color;
    }


    //违背对称性
    @Override
    public boolean equals(Object obj) {
    	//这是改变的地方
        if(!(obj instanceof Point))
            return false;
        
        if(!(obj instanceof ColorPoint))
        	return obj.equals(this);
        
        return super.equals(obj)&&((ColorPoint)obj).color==color;
    }
}
```



发现测试都是true

![img](https://img-blog.csdn.net/20180915162742505?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM5MDcxNTMw/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)

但是这样却牺牲了传递性

![img](https://img-blog.csdn.net/20180915163228768?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM5MDcxNTMw/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)

我们无法在扩展可实例化类的同时，既增加新的值组件，同时又保留equals约定，除非愿意放弃面向对象的优势即组合优先于继承的方法实现。

### 4、一致性

如果两个对象相等，它们就必须保持相等，除非它们中有一个对象(或者两个都)被修改了

### 5、非空性

```
@override
public boolean equals(Object obj){
    if(obj == null)
        return false;
    ....
}
```



## 实现高质量equals的诀窍

1、使用==操作符检查“参数是否为这个对象的引用”。如果是则返回true。

2、使用instanceof操作符检查“参数是否为正确的类型”，如果不是则返回false。所谓的正确的类型是指equals方法所在的那个类，或者是该类的父类或接口

3、把参数转化成正确的类型：因为上一步已经做过instanceof测试，所以确保转化会成功

4、对于该类的每个“关键”域，检查参数中的域是否与该对象中对应的域相匹配(其实就是比较两个对象的值是否相等了)

5、当你编写完equals方法之后，应该问自己三个问题：它是否是对称的、传递的、一致的。



### 下面是根据上面的约定和诀窍覆盖equals的方法。

```
package com.ligz.two.equals;

/**
 * @author ligz
 */
public final class PhoneNumber {
	private final short areaCode;
    private final short prefix;
    private final short lineNumber;

    public PhoneNumber(short areaCode, short prefix, short lineNumber) {
        rangeCheck(areaCode, 999, "area code");
        rangeCheck(prefix, 999, "prefix");
        rangeCheck(lineNumber, 9999, "line number");
        this.areaCode = (short)areaCode;
        this.prefix = (short)prefix;
        this.lineNumber = (short)lineNumber;
    }

    private static void rangeCheck(int arg,int max,String name) {
        if(arg < 0 || arg > max)
            throw new IllegalArgumentException(name +": "+ arg);
    }

    @Override
    public boolean equals(Object obj) {
        //1、参数是否为这个对象的引用
        if(obj == this)
            return true;
        //2、使用instanceof检查
        if(!(obj instanceof PhoneNumber))
            return false;
        //3、把参数转化成正确的类型
        PhoneNumber pn = (PhoneNumber)obj;
        //4、比较两个对象的值是否相等
        return pn.lineNumber == lineNumber
            && pn.prefix == prefix
            && pn.areaCode == areaCode;
    }
}
```

