> 为什么要使用内部类？在《Think in java》中有这样一句话：使用内部类最吸引人的原因是：每个内部类都能独立地继承一个（接口的）实现，所以无论外围类是否已经继承了某个（接口的）实现，对于内部类都没有影响。

​      在我们程序设计中有时候会存在一些使用接口很难解决的问题，这个时候我们可以利用内部类提供的、可以继承多个具体的或者抽象的类的能力来解决这些程序设计问题。可以这样说，接口只是解决了部分问题，而内部类使得多重继承的解决方案变得更加完整。

### 内部类基础

```
package com.ligz.Inner;

public class OuterClass {
	private String name;
	private String age;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getAge() {
		return age;
	}
	public void setAge(String age) {
		this.age = age;
	}
	
	
	//InnerClass
	public class InnerClass {
		public InnerClass() {
			name="ligz";
			age="18";
		}
		
		public void display() {
			System.out.println("name:"+name+" age:"+age);
		}
	}
	
	public static void main(String[] args) {
		OuterClass outerClass = new OuterClass();
		OuterClass.InnerClass innerClass = outerClass.new InnerClass();
		innerClass.display();
	}
}
```



引用内部类我们需要指明这个对象的类型：OuterClasName.InnerClassName。同时如果我们需要创建某个内部类对象，必须要利用外部类的对象通过.new来创建内部类： OuterClass.InnerClass innerClass = outerClass.new InnerClass();

我们也可以通过下面的方法得到：

```
public InnerClass getInner() {
		return this.new InnerClass();
	}
```



 创建的方法是

```
OuterClass.InnerClass innerClass = outerClass.getInner();
```





 同时如果我们需要生成对外部类对象的引用，可以使用OuterClassName.this，这样就能够产生一个正确引用外部类的引用了

```
public class InnerClass{
        public OuterClass getOuterClass(){
            return OuterClass.this;
        }
    }
```



不过要注意的是，当成员内部类拥有和外部类同名的成员变量或者方法时，会发生隐藏现象，即默认情况下访问的是成员内部类的成员。如果要访问外部类的同名成员，需要以下面的形式进行访问：

```
外部类.this.成员变量
外部类.this.成员方法
```



内部类可以拥有private访问权限、protected访问权限、public访问权限及包访问权限。比如上面的例子，如果成员内部类Inner用private修饰，则只能在外部类的内部访问，如果用public修饰，则任何地方都能访问；如果用protected修饰，则只能在同一个包下或者继承外部类的情况下访问；如果是默认访问权限，则只能在同一个包下访问。这一点和外部类有一点不一样，外部类只能被public和包访问两种权限修饰。我个人是这么理解的，由于成员内部类看起来像是外部类的一个成员，所以可以像类的成员一样拥有多种权限修饰。

## 2、局部内部类

```
class People{
    public People() {
         
    }
}
 
class Man{
    public Man(){
         
    }
     
    public People getWoman(){
        class Woman extends People{   //局部内部类
            int age =0;
        }
        return new Woman();
    }
}
```



局部内部类我不太熟悉，但局部内部类就像是方法里面的一个局部变量一样，是不能有public、protected、private以及static修饰符的。

### 3、最重要的匿名内部类

不用匿名内部类的时候：

```
private void setListener()
{
    scan_bt.setOnClickListener(new Listener1());       
    history_bt.setOnClickListener(new Listener2());
}
 
class Listener1 implements View.OnClickListener{
    @Override
    public void onClick(View v) {
    // TODO Auto-generated method stub
             
    }
}
 
class Listener2 implements View.OnClickListener{
    @Override
    public void onClick(View v) {
    // TODO Auto-generated method stub
             
    }
}
```



使用匿名内部类：

```
scan_bt.setOnClickListener(new OnClickListener() {
             
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                 
            }
        });
         
        history_bt.setOnClickListener(new OnClickListener() {
             
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                 
            }
        });
```



匿名内部类是唯一一种没有构造器的类。正因为其没有构造器，所以匿名内部类的使用范围非常有限，大部分匿名内部类用于接口回调。匿名内部类在编译的时候由系统自动起名为Outter$1.class。一般来说，匿名内部类用于继承其他类或是实现接口，并不需要增加额外的方法，只是对继承方法的实现或是重写。

我们在通过一个例子详细的说明匿名内部类：

```
public abstract class Dog {
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public abstract int run();
}
```





```
public class Inner {
	public void Inner(Dog dog) {
		System.out.println(dog.getName()+"能奔跑"+dog.run()+"米");
	}
	
	public static void main(String[] args) {
		Inner inner = new Inner();
		inner.Inner(new Dog() {
			
			@Override
			public int run() {
				return 10000;
			}
			
			public String getName() {
				return "阿拉伯猎犬";
			}
		});
	}
}
```



在这一段代码里面我们通过调用匿名内部类Dog。得到——阿拉伯猎犬能奔跑10000米。

​     **1、**使用匿名内部类时，我们必须是继承一个类或者实现一个接口，但是两者不可兼得，同时也只能继承一个类或者实现一个接口。

​     **2、**匿名内部类中是不能定义构造函数的。

​     **3、**匿名内部类中不能存在任何的静态成员变量和静态方法。

​     **4、**匿名内部类为局部内部类，所以局部内部类的所有限制同样对匿名内部类生效。

​     **5、**匿名内部类不能是抽象的，它必须要实现继承的类或者实现的接口的所有抽象方法。

我们给匿名内部类传递参数的时候，若该形参在内部类中需要被使用，那么该形参必须要为final。也就是说：**当所在的方法的形参需要被内部类里面使用时，该形参必须为final。**

**简单理解就是，拷贝引用，为了避免引用值发生改变，例如被外部类的方法修改等，而导致内部类得到的值不一致，于是用final来让该引用不可改变。**

 **故如果定义了一个匿名内部类，并且希望它使用一个其外部定义的参数，那么编译器会要求该参数引用是final的。**

### 4.静态内部类

了解static关键字的应该很好理解静态内部类

```
public class Test {
    public static void main(String[] args)  {
        Outter.Inner inner = new Outter.Inner();
    }
}
 
class Outter {
    public Outter() {
         
    }
     
    static class Inner {
        public Inner() {
             
        }
    }
}
```

