# 1、考虑使用静态方法代替构造器

​      构造器：理解构造器之前，首先我们需要知道Java中为什么要引入构造器，以及构造器的作用。假设我们每一次编写一个类都要执行一个initialize()方法，该方法是提醒你，在使用对象之前，应首先调用initialize()方法进行初始化，这就意味着每一用户都能去执行这个方法。Java中引入构造器，确保每一个对象都得到初始化，Java在有能力操作对象之前，系统会自动调用相应的构造器，保证初始化的进行。

​      构造器最大的用处就是在创建对象时执行初始化，当创建一个对象时，系统会为这个对象的实例进行默认的初始化。如果想改变这种默认的初始化，就可以通过自定义构造器来实现。

### 构造器：

```
public class Person {
    public String name;
    public int age;

    // 这是系统自动提供的构造器public void Person(){}
    // 自定义构造器
    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public static void main(String[] args) {
        // 使用自定义的构造器创建对象（构造器是创建对象的重要途径）
        Person p = new Person("小明", 12);
        System.out.println(p.age);
        System.out.println(p.name);
    }
}
```



### 静态方法：

```
public static Boolean valueOf(boolean b) {
    return (b ? TRUE : FALSE);
}

使用：
StaticFactory.valueOf(false)
```



### 好处：

静态工厂方法有名称，不必每次调用的时候都创建一个新对象，可以返回原返回类型的任何子类型的对象，代码简洁。

### 缺点：

类如果不含公有的或者受保护的构造器，就不能被子类化。与其他静态方法没有区别。

这里附上静态工厂方法的一些约定俗成的名称：

valueOf/Of——类型转换，返回的实例和入参具有相同的值，比如Boolean.valueOf()、EnumSet.valueOf()

getInstance——返回一个预先创建好的实例

newInstance——返回一个新的实例


1.静态方法：方法用static关键字修饰，静态方法与静态成员变量一样，属于类本身，在类装载的时候被装载到内存，不自动进行销毁，会一直存在于内存中，直到JVM关闭。使用时也是不需要实例化类，能够直接使用。**静态方法无法被重写**

​      需要注意的是：**在静态方法中只能访问类中的静态成员跟静态方法，不能直接访问类中的实例变量跟实例方法**，原因是静态方法在JVM中的加载顺序也在对象之前，直接使用实例变量跟实例方法的话，可能实例变量跟实例方法所依附的对象并没有被创建，会导致无法找到所使用的实例变量跟实例方法。 

2.实例化方法：属于实例对象，实例化后才会分配内存，必须通过类的实例来引用。不会常驻内存，当实例对象被JVM 回收之后，也跟着消失。

# 2、遇到多个构造参数的时候需要考虑使用构建器

静态工厂和构造器有个共同的局限性，它们都不能很好的扩展大量的可选参数。我们来看三种模式的区别

### 重叠构造函数的写法：

```
public class NutritionFacts {
    private final int servingSize; // (mL) required
    private final int servings; // (per container) required
    private final int calories; // optional
    private final int fat; // (g) optional
    private final int sodium; // (mg) optional
    private final int carbohydrate; // (g) optional

    //定义了多个构造函数
    //顺序都是往上调用的
    public NutritionFacts(int servingSize, int servings) {
        this(servingSize, servings, 0);
    }

    public NutritionFacts(int servingSize, int servings, int calories) {
        this(servingSize, servings, calories, 0);
    }

    public NutritionFacts(int servingSize, int servings, int calories, int fat) {
        this(servingSize, servings, calories, fat, 0);
    }

    public NutritionFacts(int servingSize, int servings, int calories, int fat,
            int sodium) {
        this(servingSize, servings, calories, fat, sodium, 0);
    }

    public NutritionFacts(int servingSize, int servings, int calories, int fat,
            int sodium, int carbohydrate) {
        this.servingSize = servingSize;
        this.servings = servings;
        this.calories = calories;
        this.fat = fat;
        this.sodium = sodium;
        this.carbohydrate = carbohydrate;
    }

    public static void main(String[] args) {
        NutritionFacts cocaCola = new NutritionFacts(240, 8, 100, 0, 35, 27);
    }
}
```



参数多的时候后真的很傻，我们来看最常见的javabean

### 采用javabean：

```
public class NutritionFacts {
    // Parameters initialized to default values (if any)
    private int servingSize = -1; // Required; no default value
    private int servings = -1; // "     " "      "
    private int calories = 0;
    private int fat = 0;
    private int sodium = 0;
    private int carbohydrate = 0;

    public NutritionFacts() {
    }

    // Setters
    public void setServingSize(int val) {
        servingSize = val;
    }

    public void setServings(int val) {
        servings = val;
    }

    public void setCalories(int val) {
        calories = val;
    }

    public void setFat(int val) {
        fat = val;
    }

    public void setSodium(int val) {
        sodium = val;
    }

    public void setCarbohydrate(int val) {
        carbohydrate = val;
    }

    public static void main(String[] args) {
        NutritionFacts cocaCola = new NutritionFacts();
        cocaCola.setServingSize(240);
        cocaCola.setServings(8);
        cocaCola.setCalories(100);
        cocaCola.setSodium(35);
        cocaCola.setCarbohydrate(27);
    }
}
```



缺点：1、在于构造过程被分配到了几个调用当中，javabean可能处于不一致的状态 。
           2、javabean模式阻止了把类做成不可变的可能（需要额外的努力来保证线程安全）

### 采用Builder模式

```
public class NutritionFacts {
    private final int servingSize;
    private final int servings;
    private final int calories;
    private final int fat;
    private final int sodium;
    private final int carbohydrate;
    //一个内部类
    //以后维护的时候，添加新参数的时候也很方便
    public static class Builder {
        // Required parameters
        private final int servingSize;
        private final int servings;

        // Optional parameters - initialized to default values
        private int calories = 0;
        private int fat = 0;
        private int carbohydrate = 0;
        private int sodium = 0;

        public Builder(int servingSize, int servings) {
            this.servingSize = servingSize;
            this.servings = servings;
        }

        public Builder calories(int val) {
            calories = val;
            return this;
        }

        public Builder fat(int val) {
            fat = val;
            return this;
        }

        public Builder carbohydrate(int val) {
            carbohydrate = val;
            return this;
        }

        public Builder sodium(int val) {
            sodium = val;
            return this;
        }

        public NutritionFacts build() {
            return new NutritionFacts(this);
        }
    }
    //构造函数需要传builder
    //可以使用单个builder构建多个对象
    private NutritionFacts(Builder builder) {
        servingSize = builder.servingSize;
        servings = builder.servings;
        calories = builder.calories;
        fat = builder.fat;
        sodium = builder.sodium;
        carbohydrate = builder.carbohydrate;
    }

    public static void main(String[] args) {
        //具有安全性和可读性
        //build的时候才会验证参数是否正确
        NutritionFacts cocaCola = new NutritionFacts.Builder(240, 8)
                .calories(100).sodium(35).carbohydrate(27).build();
    }
}
```



简而言之，如果类的构造器或者静态工厂具有多个参数，设计这种类时，Builder模式就是中不错的选择，特别是当大多数的参数都是可选的时候，更易于编写和阅读，构建器也比javabeans更加安全。

# 3、用私有构造类或者枚举类实现Singleton（单例模式）

一. 什么是单例模式

因程序需要，有时我们只需要某个类同时保留一个对象，不希望有更多对象，此时，我们则应考虑单例模式的设计。

二. 单例模式的特点

1. 单例模式只能有一个实例。

2. 单例类必须创建自己的唯一实例。

3. 单例类必须向其他对象提供这一实例。

三. 单例模式VS静态类

在知道了什么是单例模式后，我想你一定会想到静态类，“既然只使用一个对象，为何不干脆使用静态类？”，这里我会将单例模式和静态类进行一个比较。

1. 单例可以继承和被继承，方法可以被override，而静态方法不可以。

2. 静态方法中产生的对象会在执行后被释放，进而被GC清理，不会一直存在于内存中。

3. 静态类会在第一次运行时初始化，单例模式可以有其他的选择，即可以延迟加载。

4. 基于2， 3条，由于单例对象往往存在于DAO层（例如sessionFactory），如果反复的初始化和释放，则会占用很多资源，而使用单例模式将其常驻于内存可以更加节约资源。

5. 静态方法有更高的访问效率。

6. 单例模式很容易被测试

实现的三种方法：

### 1.将final域设置为公有

```
//饿汉式单利
public class Elivis {
    public static final Elivis INSTANCE= new Elivis();
    private Elivis(){...}
    //确保只返回一个Elivis实例，并且让GC关注伪装的Elivis对象
    private Object readResolve() {
        return INSTANCE;
    }
    public void leaveTheBuilding() {...}
}

//懒汉式单利
public class Singletion2 {

    private static Singletion2 instance;
    public Singletion2(){}

    public static Singletion2 getInstance(){
        if(instance == null){
            synchronized(Singletion2.class){
                if(instance == null){
                    instance = new Singletion2();
                }
            }
        }
        return instance;
    }
}
```



### 2.静态工厂方法设为公有

```
public class Elivis {
    private static final Elivis INSTANCE= new Elivis();
    private Elivis(){...}
    public static Elivis getInstance() {
        return INSTANCE;
    }
    //确保只返回一个Elivis实例，并且让GC关注伪装的Elivis对象
    private Object readResolve() {
        return INSTANCE;
    }
    public void leaveTheBuilding() {...}
}
```



### 3.自Java 1.5发布之后，还有实现Singleton的第三种方式，即是使用单个元素枚举

```
public enum Elivis{
    INSTANCE;

    public void leaveTheBuilding() {...}
}
```



该方法提供了序列化机制，绝对防止多次实例化，即使是在面对复杂的序列化或者反射攻击时。

# 4、通过私有构造器强化不可实例化的能力

1、在创建工具类的时候，大部分是无需实例化的，实例化对它们没有意义。
      在这种情况下，创建的类，要确保它是不可以实例化的。

2、在创建不可实例化的类时，虽然没有定义构造器。客户端在使用该类的时候，依然可以实例化它。
      客户端，可以继承该类，通过实例化其子类来实现实例化；客户端可以调用默认的构造器来实例化该类。

3、定义一个私有的构造器避免实例化类

```
public class UtilityClass {
    private UtilityClass() {
    	throw new AssertionError();
    }
}
```



添加 throw new AssertionError()，是避免在UtilityClass实例化UtilityClass类。
因为有了私有的无参构造器，这样客户端就没有办法调用默认构造器来实例化该类；
也避免了继承的子类被实例化的问题。

```
4、避免实例化
public class UtilityClass {
// Suppress default constructor for noninstantiability
    private UtilityClass() {
    	throw new AssertionError();
    }
    public static UtilityClass getInstance() {
        return new UtilityClass();
    }
}
    public static void main(String[] args) {
        UtilityClass one = UtilityClass.getInstance();
    }


实例化报错：
Exception in thread "main" java.lang.AssertionError
at demo4.UtilityClass.<init>(UtilityClass.java:6)


5、避免继承：
public class SubUtilityClass extends UtilityClass {
}
```



**保证该类在如何时候都不会被实例化**

# 5、避免创建不必要的对象

### **1、极端反面的例子**

```
String str = new String("abc");
```



上面这条代码执行过后，会生成两个对象，参数”abc”本身就是一个String对象，new String()又会产生新的String对象。

```
String str = "abc";
```



无论这条语句执行多少次，对象只会有一个。

### 2、重用那些已知不会被修改的可变变量

```
public class Person{
        private final Date birthDate;
        //Dont do this
        public boolean isBabyBoomer(){
            // Unecessary allocationof expensive object
            Calendar gmtCal = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
            gmtCal.set(1946,Calendar.JANUARY,1,0,0,0);
            Data boomStart = gmtCal.getTime();
            gmtCal = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
            gmtCal.set(1965,Calendar.JANUARY,1,0,0,0);
            Data boomEnd = gmtCal.getTime();
            return birthDate.compareTo(boomStart) >= 0 &&
                bitrthDate.compareTo(boomEnd) < 0;
        }
    }
```



看下面优化的代码

```
public class Person{
        private final Date birthDate;

        private static final Date BOOMT_START;
        private static final Date BOOMT_END;
        static{//我们在静态方法块中初始化这些字段
            Calendar gmtCal = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
            gmtCal.set(1946,Calendar.JANUARY,1,0,0,0);
            BOOMT_START = gmtCal.getTime();
            gmtCal = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
            gmtCal.set(1965,Calendar.JANUARY,1,0,0,0);
            BOOMT_END = gmtCal.getTime();
        }

        //Dont do this
        public boolean isBabyBoomer(){
            return birthDate.compareTo(BOOM_START)>=0&&
            birthDate.compareTo(BOOM_END) <0;
    }
```



改进后该Person类只会在初始化的时候创建Calendar、TimeZone、Date一次，而不是每次调用isBabyBoomer时都会创建这些实例，如果该方法被频繁的调用，则会显著的提升性能。

但是如果该类被初始化了，但是这方法一次都没有被调用，那么就显得没有必要了，我们可以通过延迟初始化的方式——直到该方法第一次被调用的时候才初始化。

### 3、要优先使用基本类型而不是装箱基本类型



**不要错误的认为”创建对象的代价非常昂贵，我们应该尽**

```
static void basic() {
		long sum = 0L;
		long start = System.currentTimeMillis();
		for (int i = 0; i< Integer.MAX_VALUE; i++) {
		    sum += i;
		}
		long end = System.currentTimeMillis();
		System.out.println(sum + " long用时: " + (end - start) + "ms");
	}
	
	static void auto() {
		Long sum = 0L;
		long start = System.currentTimeMillis();
		for (int i = 0; i< Integer.MAX_VALUE; i++) {
		    sum += i;
		}
		long end = System.currentTimeMillis();
		System.out.println(sum + " Long用时: " + (end - start) + "ms");
	}
	public static void main(String[] args) {
		basic();
		auto();
	}
```



我们得出来的结果是

2305843005992468481 long用时: 1640ms
2305843005992468481 Long用时: 7485ms

sum变量被声明为Long而不是long，意味着程序构造了大约2^31个多余的Long实例，每往Long sum 中增加long时构造一个实例，**当心无意识的装箱行为。**

**不要错误的认为”创建对象的代价非常昂贵，我们应该尽可能地避免创建对象”**

​       由于小对象的构造器只做很少量的事情，所以它们的创建和销毁的代价都是非常廉价的，尤其是在现代的的ＪＶＭ实现上更是的如此。通过创建附加的对象，提升程序的清晰性、间接性、和功能性，这通常是件好事。 
　　反之，通过维护不必要的线程池来避免创建兑现更并不是一种好的做法，除非对象池中的对象非常重量级比如 数据库连接池、线程池，这些对象创建的代价非常昂贵、因此重用这些对象就显得非常有意义。一般而言，维护自己的对象池，可能会把代码弄的非常乱，同时增加了内存的占用（除非你的对象池有非常好释放）。