## Java8实战

[TOC]

### 基础知识

#### Lambda初识

我们看一个新的功能是**方法引用**

```java
File[] hiddenFile = new File("/").listFiles(new FileFilter(){
    public boolean accept(File file) {
        return file.isHidden();
    }
});
```

在Java8里面可以写成下面这样

```java
File[] hiddenFile = new File("/").listFiles(File::isHidden);
```

在使用`File::isHidden`时会创建一个方法引用，你就可以传递它。

**从方法引用到Lambda**

再看一个例子，选出颜色是绿色的苹果

```java
public static List<Apple> filterGreenApples(List<Apple> apples) {
    List<Apple> result = new ArrayList<>();
    for (Apple apple : apples) {
        if ("green".equals(apple.getColor())) {
            result.add(apple);
        }
    }
    return result;
}
```

如果你想要筛选出重量大于500克的，你需要复制整个代码，然后修改一点

```
public static List<Apple> filterWeightApples(List<Apple> apples) {
    List<Apple> result = new ArrayList<>();
    for (Apple apple : apples) {
        if (apple.getWeight() > 500) {
            result.add(apple);
        }
    }
    return result;
}
```

使用方法引用的话

```
public static boolean isGreenApple(Apple apple) {
	return "green".equals(apple.getColor()); 
}

public static boolean isHeavyApple(Apple apple) {
	return apple.getWeight() > 150;
}
    
List<Apple> greenApples =filterApples(inventory,FilteringApples::isGreenApple);

List<Apple> heavyApples =filterApples(inventory,FilteringApples::isHeavyApple);
```



如果使用Lambda的话，只需要想下面这样

```
filterApples(apples, (Apple a) -> "green".equals(a.getColor()));
filterApples(apples, (Apple a) -> a.getWeight() > 500);
```



#### 流处理

Stream可以将它看成一个比较花哨的迭代器，Stream API的很多方法可以链接成一个流水线，可以在更高的抽象层次写程序，另一个好处是可以透明的将输入的不相关几部分拿到几个CPU内核上分别执行你的流水线。

```
List<Apple> heavyApples = apples.stream().filter((Apple a -> a.getWeight() > 500)).collect(toList());
```

**还可以免费的并行**

```
List<Apple> heavyApples = apples.parallelStream().filter((Apple a -> a.getWeight() > 500)).collect(toList());
```

比如说有5个苹果，可能3个在A线程上运行，2个在B线程上运行。最后将结果合并。



#### 默认方法

接口如今可以包含实现类没有提供实现的方法签名，用`default`来声明一个默认实现，由接口提供，而不是实现类提供

比如`Collections.sort`方法

```
default void sort(Comparator<? super E> c) {
    Collections.sort(this, c);
}
```



