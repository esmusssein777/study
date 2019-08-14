我们在谈注解前，顺便先谈一谈Java反射

**Java反射：**

​      在运行状态中，对于任意一个类，都能知道这个类所有的属性和方法，对于任何一个对象，都能调用他的属性和方法。并且能改变属性。

​       反射机制允许程序在运行时取得任何一个已知的名称的class内部信息，包括修饰、属性和方法。并且在运行时改变属性或者是调用方法。那么我们便可以更灵活的编写代码，代码可以在运行时装配，无需在组件之间进行源代码链接，降低代码的耦合度；还有动态代理的实现等等。

**class：**

​       java 类在编译后会产生一个以.class结尾的字节码文件，该文件内存储了Class对象的相关信息，Class对象表示的是类在运行时的类型信息， Class与java.lang.reflect构成了java的反射技术  。
​       当我们要使用类时，例如使用new 操作符实例化一个新对象,访问类的静态方法，jvm会先检查该类的有无加载，若有加载了就会直接进行相应的操作，若检查到没有加载,jvm就会先去加载这个类的对应的字节码文件(这里会进行相应的检查)  
​        当加载成功后，就可以进行相应的操作了。

```
public class Person implements Serializable {

    private String name;
    private int age;
// get/set方法
}
public static void main(String[] args) {
    Person person = new Person("ligz", 18);
    Class clazz = person.getClass();

    Field[] fields = clazz.getDeclaredFields();
    for (Field field : fields) {
        String key = field.getName();
        PropertyDescriptor descriptor = new PropertyDescriptor(key, clazz);
        Method method = descriptor.getReadMethod();
        Object value = method.invoke(person);

        System.out.println(key + ":" + value);

    }
}
```



最后输出的结果是

name:ligz

age:18

以上通过getReadMethod()方法调用类的get函数，可以通过getWriteMethod()方法来调用类的set方法。

# 注解优先于命名模式

注解的语法比较简单，除了@符号的使用之外，它基本与Java固有语法一致。Java SE5内置了三种标准注解：

​     @Override，表示当前的方法定义将覆盖超类中的方法。

​     @Deprecated，使用了注解为它的元素编译器将发出警告，因为注解@Deprecated是不赞成使用的代码，被弃用的代码。

​     @SuppressWarnings，关闭不当编译器警告信息。

​     上面这三个注解多少我们都会在写代码的时候遇到。Java还提供了4中注解，专门负责新注解的创建。

| @Target    | 表示该注解可以用于什么地方，可能的ElementType参数有：CONSTRUCTOR：构造器的声明FIELD：域声明（包括enum实例）LOCAL_VARIABLE：局部变量声明METHOD：方法声明PACKAGE：包声明PARAMETER：参数声明TYPE：类、接口（包括注解类型）或enum声明 |
| ---------- | ------------------------------------------------------------ |
| @Retention | 表示需要在什么级别保存该注解信息。可选的RetentionPolicy参数包括：SOURCE：注解将被编译器丢弃CLASS：注解在class文件中可用，但会被VM丢弃RUNTIME：VM将在运行期间保留注解，因此可以通过反射机制读取注解的信息。 |
| @Document  | 将注解包含在Javadoc中                                        |
| @Inherited | 允许子类继承父类中的注解                                     |

**使用注解的例子：**

```
package com.ligz.Annotation.two;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ligz
 */
@Target({ElementType.METHOD,ElementType.FIELD})
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface MyTag {
 
	String name() default "车";
	int size() default 10;
}
```



```
package com.ligz.Annotation.two;

/**
 * @author ligz
 */
public class Car {
	 
	private String name;
	private int size;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getSize() {
		return size;
	}
	public void setSize(int size) {
		this.size = size;
	}
	
	public Car(){
		
	}
	public Car(String name, int size){
		this.size = size;
		this.name = name;
	}
	@Override
	public String toString() {
		return "Car [name=" + name + ", size=" + size + "]";
	}
	
}
```



```
package com.ligz.Annotation.two;

/**
 * 定义一个使用注解的类AnnotationDemo类
 * @author ligz
 */
public class AnnotationDemo{
	
	@MyTag(name = "audi", size = 10)
	private Car car;
 
	public Car getCar() {
		return car;
	}
 
	public void setCar(Car car) {
		this.car = car;
	}
 
	@Override
	public String toString() {
		return "Annotation [car=" + car + "]";
	}
}
```



```
package com.ligz.Annotation.two;

import java.lang.reflect.Field;
/**
 * 定义一个操作注解即让注解起作用的类AnnotationProccessor类
 * @author ligz
 */

public class AnnotationProccessor {
	
	public  static void annoProcess(AnnotationDemo annotation){
		
		for(Field field : annotation.getClass().getDeclaredFields()){
			if(field.isAnnotationPresent(MyTag.class)){  //如果存在MyTag标签
				MyTag myTag = field.getAnnotation(MyTag.class);
				annotation.setCar(new Car(myTag.name(),myTag.size()));
			}
		}
	}
	public static void main(String[] args) {
		AnnotationDemo ann = new AnnotationDemo();
		annoProcess(ann);
		System.out.println(ann.getCar());
		
	}
}
```



运行结果为

Car [name=audi, size=10]

**在effectiveJava中，作者使用junit作为例子来对比了命名模式和注解。**

命名模式的缺点：

- 文字拼写错误导致失败，测试方法没有执行，也没有报错
- 无法确保它们只用于相应的程序元素上，如希望一个类的所有方法被测试，把类命名为test开头，但JUnit不支持类级的测试，只在test开头的方法中生效
- 没有提供将参数值与程序元素关联起来的好方法。

总的说来，有注解可以不使用命名模式。

结合之前来看，注解通过反射获取了类的方法名和属性值。我们发现得到了类似xml的功能。

以前，XML是各大框架的青睐者，它以松耦合的方式完成了框架中几乎所有的配置，但是随着项目越来越庞大，XML的内容也越来越复杂，维护成本变高。

于是就有人提出来一种标记式高耦合的配置方式，注解。方法上可以进行注解，类上也可以注解，字段属性上也可以注解，反正几乎需要配置的地方都可以进行注解。

关于注解和XML两种不同的配置模式，争论了好多年了，各有各的优劣，注解可以提供更大的便捷性，易于维护修改，但耦合度高，而 XML 相对于注解则是相反的。

追求低耦合就要抛弃高效率，追求效率必然会遇到耦合。

我们好是要根据情况来选择。

# 坚持使用Override注解

这个没啥好说的，如果你使用 @Override 告诉编译器你想要覆盖它 , 如果你出了错误，那么你将会得到一条编译错误的提示。

# 学会使用标记注解与标记接口

**标识接口**是没有任何方法和属性的接口，它仅仅表明它的类属于一个特定的类型,供其他代码来测试允许做一些事情。

一些容器例如Ejb容器，`servlet`容器或运行时环境依赖标记接口识别类是否需要进行某种处理，比如serialialbe接口标记类需要进行序列化操作。

**标记注解**是特殊类型的注解，其中不包含成员。标记注解的唯一目的就是标记声明，因此，这种注解作为注解而存在的理由是充分的。确定标记注解是否存在的最好方式是使用isAnnotationPresent()方法，该方法是由AnnotatedElement接口定义的。

**标记注解优点：**

1.标记注解可以通过默认的方式添加一个或者多个注解类型元素 , 给已被实用的注解类型添加更多地信息 . 
2.标记注解是更大注解机制的一部分 , 这意味这它在那些支持注解作为编程元素之一的框架中同样具有一致性

**标记接口优点：**

1.标机接口定义的类型是由被标记类的实例实现的 ; 标记注解则没有定义这样的类型 . 这个类型允许你在编译时捕捉在使用标记注解的情况下要到运行时才能捕捉到的错误 . 
2.标记接口可以更加精确地进行锁定

### 总之 , 标记接口和标记注解各有用处 . 如果想要定义一个任何新方法都不会与之关联的类型 , 标记接口就是最好的选择 . 如果想要标记程序元素而非类和接口 , 考虑到未来可能要给标记添加更多地信息 , 或者标记要适合于已经广泛使用了注解类型的框架 , 那么标记注解是正确的选择   

