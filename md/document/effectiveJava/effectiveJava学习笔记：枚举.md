# 使用枚举代替int常量

在枚举类型出现之前，一般都常常使用`int`常量或者`String`常量表示列举相关事物。如：

```
public static final int APPLE_FUJI = 0;
public static final int APPLE_PIPPIN = 1;
public static final int APPLE_GRANNY_SMITH = 2;

public static final int ORANGE_NAVEL = 0;
public static final int ORANGE_TEMPLE = 1;
public static final int ORANGE_BLOOD = 2;
```



int常量的缺点：

\1. 在类型安全方面，如果你想使用的是ORANGE_NAVEL，但是传递的是APPLE_FUJI，编译器并不能检测出错误； 
\2. 因为int常量是编译时常量，被编译到使用它们的客户端中。若与枚举常量关联的int发生了变化，客户端需重新编译，否则它们的行为就不确定； 
\3. 没有便利方法将int常量翻译成可打印的字符串。这里的意思应该是比如你想调用的是ORANGE_NAVEL，debug的时候显示的是0，但你不能确定是APPLE_FUJI还是ORANGE_NAVEL。如果你想使用String常量，虽然提供了可打印的字符串，但是性能会有影响。特殊是对于有些新手开发，有可能会直接将String常量硬编码到代码中，导致以后修改很困难。

4.遍历一个组中所有的int枚举常量，获得int枚举组的大小，没有可靠的方法，如想知道APPLE的常量有多少个，除了查看int枚举常量所在位置的代码外，别无他法，而且靠的是观察APPLE_前缀有多少个。

枚举：

```
public enum Apple {
    APPLE_FUJI,
    APPLE_PIPPIN,
    APPLE_GRANNY_SMITH;
}

public enum Orange {
    ORANGE_NAVEL,
    ORANGE_TEMPLE,
    ORANGE_BLOOD;
}
```



### `enum`枚举常量与数据关联

我们先看我们常用到的判断状态的常量类在枚举中的用法

```
/**
 * @author ligz
 */
public enum Database {
	mysql(0),
	mongodb(1),
	redis(2),
	hbase(3);
	
	private int status;
	
	Database(int status){
		this.status = status;
	}
	
	public int Status() {
		return status;
	}
}
```



```
/**
 * @author ligz
 */
public class TestDataBase {
	public static void main(String[] args) {
		for(Database d : Database.values()) {
			if(0 == d.Status()) {
				System.out.println("mysql");
			}
		}
				
	}
}
```



我们再来看书上的例子

如以太阳系为例，每个行星都拥有质量和半径，可以依据这两个属性计算行星表面物体的重量。代码如下：

```
package com.ligz.Chapter6.Item30;

/**
 * @author ligz
 */
public enum Planet {
	MERCURY(3.302e+23, 2.439e6),
    VENUS (4.869e+24, 6.052e6),
    EARTH (5.975e+24, 6.378e6),
    MARS (6.419e+23, 3.393e6),
    JUPITER(1.899e+27, 7.149e7),
    SATURN (5.685e+26, 6.027e7),
    URANUS (8.683e+25, 2.556e7),
    NEPTUNE(1.024e+26, 2.477e7);
	
	private final double mass; // In kilograms
    private final double radius; // In meters
    private final double surfaceGravity; // In m / s^2

    // Universal gravitational constant in m^3 / kg s^2
    private static final double G = 6.67300E-11;

    Planet(double mass, double radius){
    	this.mass = mass;
    	this.radius = radius;
    	surfaceGravity = G * mass / (Math.pow(radius, 2));
    }

	public double getMass() {
		return mass;
	}

	public double getRadius() {
		return radius;
	}

	public double getSurfaceGravity() {
		return surfaceGravity;
	}
    
    public double getSurfaceWight(double mass) {
    	return mass * surfaceGravity; // F = ma
    }
    
}
```



```
package com.ligz.Chapter6.Item30;

/**
 * @author ligz
 */
public class WeightTable {
public static void main(String[] args) {
	double earthWeight = 30;
	double mass = earthWeight / Planet.EARTH.getSurfaceGravity();
	
	for(Planet p : Planet.values()) {
		System.out.printf("Weight on %s is %f%n", p, p.getSurfaceWight(mass));
	}
}
}
```



输出为

Weight on MERCURY is 11.337201
Weight on VENUS is 27.151530
Weight on EARTH is 30.000000
Weight on MARS is 11.388120
Weight on JUPITER is 75.890383
Weight on SATURN is 31.965423
Weight on URANUS is 27.145664
Weight on NEPTUNE is 34.087906

### `enum`枚举常量与行为关联

有些时候将`enum`枚举常量与数据关联还不够，还需要将枚举常量与行为关联。

如采用枚举来写加、减、乘、除的运算。代码如下：

```
public enum Operation {
    PLUS, MINUS, TIMES, DIVIDE;

    double apply(double x, double y) {
        switch(this) {
            case PLUS: return x + y;
            case MINUS: return x - y;
            case TIMES: return x * y;
            case DIVIDE: return x / y;
        }
        throw new AssertionError("Unknown op: " + this);
    }
}
```



大家一开始都会这样写的。实际开发中，有很多开发者也这样写。但是有个不足：如果需要新增加运算，譬如模运算，不仅仅需要添加枚举类型常量，还需要修改`apply`方法。万一忘记修改了，那就是运行时错误。

```
public enum Operation {
  PLUS("+") {
    @Override
    double apply(double x, double y) {
      return x + y;
    }
  },

  MINUS("-") {
    @Override
    double apply(double x, double y) {
      return x - y;
    }
  },

  TIMES("*") {
    @Override
    double apply(double x, double y) {
      return x * y;
    }
  },

  DIVIDE("/") {
    @Override
    double apply(double x, double y) {
      return x / y;
    }
  };

  private String symbol;
  Operation(String symbol) {
    this.symbol = symbol;
  }

  @Override
  public String toString() {
    return symbol;
  }

  abstract double apply(double x, double y);
}

public class OperationDemo {

  public static void main(String[] args) {
    double x = 2;
    double y = 4;

    for (Operation op : Operation.values()) {
      System.out.println(String.format("%f %s %f = %f%n", x, op, y, op.apply(x, y)));
    }
}
```



 输入2 4
    2.000000 + 4.000000 = 6.000000
    2.000000 - 4.000000 = -2.000000
    2.000000 * 4.000000 = 8.000000
    2.000000 / 4.000000 = 0.500000
  }

一般，`enum`中重写了`toString`方法之后，`enum`中自生成的`valueOf(String)`方法不能根据枚举常量的字符串(`toString`生成)来获取枚举常量。我们通常需要在`enum`中新增个静态常量来获取。如：

```
public enum Operation {
  PLUS("+") {
    @Override
    double apply(double x, double y) {
      return x + y;
    }
  },

  MINUS("-") {
    @Override
    double apply(double x, double y) {
      return x - y;
    }
  },

  TIMES("*") {
    @Override
    double apply(double x, double y) {
      return x * y;
    }
  },

  DIVIDE("/") {
    @Override
    double apply(double x, double y) {
      return x / y;
    }
  };

  private String symbol;
  public static final Map<String, Operation> OPERS_MAP = Maps.newHashMap();

  static {
    for (Operation op : Operation.values()) {
      OPERS_MAP.put(op.toString(), op);
    }
  }

  Operation(String symbol) {
    this.symbol = symbol;
  }

  @Override
  public String toString() {
    return symbol;
  }

  abstract double apply(double x, double y);
}
```



可以通过调用Operation.OPERS_MAP.get(op.toString())来获取对应的枚举常量。

在有些特定的情况下，此写法有个缺点，即如果每个枚举常量都有公共的部分处理该怎么办，如果每个枚举常量关联的方法里都有公共的部分，那不仅不美观，还违反了DRY原则。这就是下面的枚举策略模式

```
enum PayrollDay {
    MONDAY, 
    TUESDAY, 
    WEDNESDAY, 
    THURSDAY, 
    FRIDAY,
    SATURDAY, 
    SUNDAY;

    private static final int HOURS_PER_SHIFT = 8;

    double pay(double hoursWorked, double payRate) {
        double basePay = hoursWorked * payRate;

        double overtimePay; // Calculate overtime pay
        switch(this) {
            case SATURDAY: case SUNDAY:
                overtimePay = hoursWorked * payRate / 2;
                break;
            default: // Weekdays
                overtimePay = hoursWorked <= HOURS_PER_SHIFT ?
                0 : (hoursWorked - HOURS_PER_SHIFT) * payRate / 2;
        }

        return basePay + overtimePay;
    }
}
```



以上代码是计算工人工资。平时工作8小时，超过8小时，以加班工资方式另外计算；如果是双休日，都按照加班方式处理工资。

上面代码的写法和上一小节给出的差不多，通过switch来分拆计算。还是一样的问题，如果此时新增加一种工资的计算方式，枚举常量需要改，pay方法也需要改。按上一小节的介绍继续修改：



```
public enum PayRoll {
  MONDY(PayType.WEEKDAY),
  TUESDAY(PayType.WEEKDAY),
  WEDNESDAY(PayType.WEEKDAY),
  THURSDAY(PayType.WEEKDAY),
  FRIDAY(PayType.WEEKDAY),
  SATURDAY(PayType.WEEKEND),
  SUNDAY(PayType.WEEKEND);

  private final PayType payType;
  PayRoll(PayType payType) {
    this.payType = payType;
  }

  double pay(double hoursWorked, double payRate) {
    return payType.pay(hoursWorked, payRate);
  }

  private enum PayType {
    WEEKDAY {
      @Override
      double overtimePay(double hoursWorked, double payRate) {
        double overtime = hoursWorked - HOURS_PER_SHIFT;
        return overtime <= 0 ? 0 : overtime * payRate / 2;
      }
    },

    WEEKEND {
      @Override
      double overtimePay(double hoursWorked, double payRate) {
        return hoursWorked * payRate / 2;
      }
    };

    private static final int HOURS_PER_SHIFT = 8;
    abstract double overtimePay(double hoursWorked, double payRate);

    double pay(double hoursWorked, double payRate) {
      double basePay = hoursWorked * payRate;
      return basePay + overtimePay(hoursWorked, payRate);
    }
  }
}
```



# **用实例域代替序数**

这个其实就是上面的Database例子，以自己定的顺序来确定，而不是用自带的ordinal。

```
/**
 * @author ligz
 */
public enum Ensemble {
	SOLO, DUET, TRIO, QUARTET, QUINTET;
	
	public int numberOfMusicians() {
		return ordinal() + 1;
	}
}
```



# **用EnumSet代替位域**

EnumSet底层是用位向量实现的

如果一个枚举类型的元素主要用在集合中，一般就使用int枚举模式，将2的不同倍数赋予每个常量

```
// Bit field enumeration constants - OBSOLETE!

public class Test {

public static final int STYLE_BOLD = 1 << 0; // 1

public static final int STYLE_ITALIC  = 1 << 1; // 2

public static final int STYLE_UNDERLINE = 1 << 2; // 4

public static final int STYLE_STRIKETHROUGH  = 1 << 3;  // 8

// Parameter is bitwise OR of zero or more SYTLE_ constants

public void applyStyles(int styles) { ... }

}
```



**这种表示法让你用OR位运算将几个常量合并到一个集合中，称作位域（bit field）。**

text.applyStyles(STYLE_BOLD | STYLE_ITALIC);

位域表示法也允许利用位操作，有效地执行像union（联合）和intersection（交集）这样的集合操作。但位域有着int枚举常量的所有缺点，甚至更多。当位域以数字形式打印时，翻译位域比翻译简单的int枚举常量要困难得多。甚至，要遍历位域表示的所有元素也没有很容易的方法。

有些程序员优先使用枚举而非int常量，他们在需要传递多组常量集时，仍然倾向于使用位域。其实没有理由这么做，因为还有更好地替代方法。java.util包提供了EnumSet类来有效的表示从单个枚举类型中提取的多个值得多个集合。这个类实现Set接口，提供了丰富的功能、类型安全性，以及可以从任何其他Set实现中得到的互用性。但是在内部具体的实现上，每个EnumSet就是用单个long来表示，因此它的性能比得上位域的性能。批处理，如removeAll何retainAll，都是利用位算法来实现的，就像手工替位域实现的那样。但是可以避免手工位操作时容易出现的错误以及不大雅观的代码，因为EnumSet替你完成了这项艰巨的工作。

下面是前一个范例改成用枚举代替位域后的代码，他更加简短、更加清楚，也更加安全：

```
// EnumSet - a modern replacement for bit fields

public class Text {

public enum Style {BOLD , ITALIC , UNDERLINE , STRIKETHROUGH}

// Any Set could be passed in , but EnumSet is clearly best

public void applyStyles(Set<Style> styles) { ... }

}
```



下面是将EnumSet实例传递给applyStyles方法的客户端代码。EnumSet提供了丰富的静态工厂来轻松创建集合，其中一个如这个代码所示：

text.applyStyles(EnumSet.of(Style.BOLD , Style.ITALIC));

注意applyStyles方法采用的是Set<Style>而非EnumSet<Style>，虽然看起来好像所有的客户端都可以将EnumSet传到这个方法，但是最好还是接受接口类型而非接受实现类型。这是考虑到可能会有特殊的客户端要传递一些其他的Set实现，并且没有什么明显的缺点。

总而言之，**正是因为枚举类型要用在集合（Set）中，所以没有理由用位域来表示他。**EnumSet类集位域的简洁和性能优势及枚举类型的所有优点于一身。实际上EnumSet有个缺点，即截止Java 1.6发行版本，他都无法创建不可变的EnumSet，但是这一点很可能在即将出现的版本中得到修正。同时，可以用Collections.unmodifiableSet将EnumSet封装起来，但是间接性和性能会受到影响。

# **用EnumMap代替序数索引**

```
// 将集合放到一个按照类型的序数进行索引的数组中来实现  替换
    Herb[] garden = { new Herb("1", Type.ANNUAL), new Herb("2", Type.BIENNIAL), new Herb("3", Type.PERENNTAL) };

    Set<Herb>[] herbsByType = (Set<Herb>[])new Set[Herb.Type.values().length];
    for(int i = 0;i<herbsByType.length;i++){
        herbsByType[i] = new HashSet<Herb>();
    }
    for(Herb h:garden){
        herbsByType[h.type.ordinal()].add(h);
    }
```



变为

```
Herb[] garden = { new Herb("1", Type.ANNUAL), new Herb("2", Type.BIENNIAL), new Herb("3", Type.PERENNTAL) };
    Map<Herb.Type, Set<Herb>> herbsByType = new EnumMap<Herb.Type,Set<Herb>>(Herb.Type.class);
    for(Herb.Type t:Herb.Type.values()){
        herbsByType.put(t, new HashSet<Herb>());
    }
    for(Herb h:garden)
        herbsByType.get(h.type).add(h); 
    System.out.println(herbsByType);
```



# **用接口模拟可伸缩的枚举**

```
/**
 * 虽然枚举类型是不能扩展的 , 但是可以通过接口类表示API中的操作的接口类型 . 
 * 你可以定义一个功能完全不同的枚举类型来实现这个接口 .  
 */
public interface Operation {
    double apply(double x,double y);
}
//它的一个实现类
public enum BasicOperation implements Operation {   
    PLUS("+"){      
        public double apply(double x, double y) {           
            return x + y;
        }
    },
    MINUS("-"){ 
        public double apply(double x, double y) {           
            return x - y;
        }
    };  
    private final String symbol;
    BasicOperation(String symbol) {
        this.symbol = symbol;
    }   
    @Override
    public String toString(){
        return symbol;
    }
}
```



```
//他的另一个实现类
public enum ExtendedOperation implements Operation{
    Exp("^"){
        public double apply(double x,double y){
            //次幂计算
            return Math.pow(x, y);
        }
    },
    REMAINDER("%"){
        public double apply(double x,double y){
            return x % y;
        }
    };

    private final String symbol;
    ExtendedOperation(String symbol) {
        this.symbol = symbol;
    }
    @Override
    public String toString(){
        return symbol;
    }
}
```

