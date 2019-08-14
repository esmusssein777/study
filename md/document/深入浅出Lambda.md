@[toc]
## 从需求到策略模式，再到匿名函数，最后到Lambda
首先我们有一个apple对象

```
public static class Apple {
    private int weight = 0;
    private String color = "";

    public Apple(int weight, String color){
        this.weight = weight;
        this.color = color;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String toString() {
        return "Apple{" +
                "color='" + color + '\'' +
                ", weight=" + weight +
                '}';
    }
}
```

#### 疲于不同的需求

我们需要给他筛选出绿色的苹果

我们会这样写

```
public static List<Apple> filterGreenApples(List<Apple> inventory){
    List<Apple> result = new ArrayList<>();
    for(Apple apple: inventory){
        if("green".equals(apple.getColor())){
            result.add(apple);
        }
    }
    return result;
}
```

如果又有不用的需求，可能我们要筛选出不同颜色的苹果

可能我们又会这样的改动，将颜色作为条件参数

```
public static List<Apple> filterApplesByColor(List<Apple> inventory, String color){
    List<Apple> result = new ArrayList<>();
    for(Apple apple: inventory){
        if(apple.getColor().equals(color)){
            result.add(apple);
        }
    }
    return result;
}
```

又过了一阵子，又给你一个新的需求，叫你筛选出重量大于150克的苹果

你可能会认为，这好办，我复制一波代码，改个参数就OK，就像下面这样

```
public static List<Apple> filterApplesByWeight(List<Apple> inventory, int weight){
    List<Apple> result = new ArrayList<>();
    for(Apple apple: inventory){
        if(apple.getWeight() > weight){
            result.add(apple);
        }
    }
    return result;
}
```

这样的代码极其重合，可能你觉得这样不太好，实际上确实不好

你又想把它改成一个代码，用一个标志位 flag 来判断他到底需要哪个方法。

```
public static List<Apple> filterApples(List<Apple> apples, String color, int weight, boolean flag) {
    List<Apple> result = new ArrayList<>();
    for(Apple apple: apples){
        if((flag && apple.getColor().equals(color)) || (!flag && apple.getWeight() > weight)){
            result.add(apple);
        }
    }
    return result;
}
```

但这样如果后面还有新的需求，还有新的条件，估计你会陷入绝望当中

这回严重的打击我们写代码的积极性，我们需要创造性的方法来帮助我们解决这样的死亡循环

#### 行为参数化

我们使用策略模式来帮助我们解决一直来的新的需求

```
interface ApplePredicate{
    public boolean test(Apple a);
}

static class AppleWeightPredicate implements ApplePredicate{
    public boolean test(Apple apple){
        return apple.getWeight() > 150;
    }
}
static class AppleColorPredicate implements ApplePredicate{
    public boolean test(Apple apple){
        return "green".equals(apple.getColor());
    }
}

static class AppleRedAndHeavyPredicate implements ApplePredicate{
    public boolean test(Apple apple){
        return "red".equals(apple.getColor())
                && apple.getWeight() > 150;
    }
}
```

我们先定义一个接口，实现不同的算法，将这个接口作为参数传入方法。这样我们可以更加灵活的解决需求

我们将方法写成这样

```
public static List<Apple> filter(List<Apple> inventory, ApplePredicate p){
    List<Apple> result = new ArrayList<>();
    for(Apple apple : inventory){
        if(p.test(apple)){
            result.add(apple);
        }
    }
    return result;
}
```

这样下来，比如你想实现筛选一个大于150克重量并且是红色苹果的方法，你只需传入以`AppleRedAndHeavyPredicate`的参数即可

我们重新看一下`AppleRedAndHeavyPredicate`的实现

```
static class AppleRedAndHeavyPredicate implements ApplePredicate{
    public boolean test(Apple apple){
        return "red".equals(apple.getColor())
                && apple.getWeight() > 150;
    }
```

他实现了ApplePredicate接口，所以他可以作为下面方法的参数传入

```
public static List<Apple> filter(List<Apple> inventory, ApplePredicate p){
    List<Apple> result = new ArrayList<>();
    for(Apple apple : inventory){
        if(p.test(apple)){
            result.add(apple);
        }
    }
    return result;
}
```

这样他实现`test`方法的时候就自然而然的完成了他的任务



#### 匿名类优化

这样你可能会发现你需要写很多的接口实现，可能一个实现你只需要用到一次，你也必须去写一个实现类，你厌倦了这样的庞大类，你需要找东西来优化。

首先你可能会想到匿名类

比如筛选颜色

```
List<Apple> redApples2 = filter(inventory, new ApplePredicate() {
    public boolean test(Apple a){
        return a.getColor().equals("red");
    }
});
```

这样的代码用匿名类实现接口就行，写Android的同学或者写GUI的同学可能会大量的接触到这种匿名类来创建事件处理器

但是会发现一个类会非常的大，长到你不愿意看，也占用很多的空间

我之前写一个Android的时候就发现一个类充斥了大量的监听器和通信，虽然用起来很灵活，但是看起来还是有点笨拙



#### Lambda优化

在Java1.8以前，你可能最多做到之前的程度，但是1.8加入的Lambda表达式就是你的救星

在有上面这样的接口的情况下我们只需要下面的代码

```
List<Apple> result = filter(inventory, (Apple apple) -> "red".equals(apple.getColor()));
```

我们看下他调用了什么代码

```
public static List<Apple> filter(List<Apple> inventory, ApplePredicate p){
    List<Apple> result = new ArrayList<>();
    for(Apple apple : inventory){
        if(p.test(apple)){
            result.add(apple);
        }
    }
    return result;
}
```

这样的代码匿名、干净、传递、简洁



我们甚至可以修改成下面这样

```
public interface Predicate<T> {
    boolean test(T t);
}

public static <T> List<T> filter(List<T> list, Predicate<T> p) {
    List<T> result = new ArrayList<>();
    for (T e : list) {
        if (p.test(e)) {
            result.add(e);
        }
    }
    return result;
}
```

通过修改泛型，我们不限于传递苹果参数，我们可以 传递梨子、西瓜等不限。

完整的代码在 [这里](https://github.com/esmusssein777/study/blob/master/src/main/java/com/ligz/java8/chapter2/FilteringApples.java)

参考内容：Java8实战