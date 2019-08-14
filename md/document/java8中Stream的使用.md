[TOC]

## 流

我们先来看一下有一些需求在Java7中的集合需要怎么写

```
    public static List<String> getLowCaloricDishesNamesInJava7(List<Dish> dishes){
        List<Dish> lowCaloricDishes = new ArrayList<>();
        for(Dish d: dishes){
            if(d.getCalories() < 400){
                lowCaloricDishes.add(d);
            }
        }
        List<String> lowCaloricDishesName = new ArrayList<>();
        Collections.sort(lowCaloricDishes, new Comparator<Dish>() {
            public int compare(Dish d1, Dish d2){
                return Integer.compare(d1.getCalories(), d2.getCalories());
            }
        });
        for(Dish d: lowCaloricDishes){
            lowCaloricDishesName.add(d.getName());
        }
        return lowCaloricDishesName;
    }
```

我们首先是筛选出了卡路里**小于400**的，再让他们按照**卡路里的大小**排序。最后我们将**他们的名字**放入一个新的容器中。

这样在需求在Java8中只需要下面这样简单。需要有Lambda的基础。如果有什么不清楚的地方，看我的另一篇博客[深入浅出Lambda](https://blog.csdn.net/qq_39071530/article/details/90415222)

```
    public static List<String> getLowCaloricDishesNamesInJava8(List<Dish> dishes){
        return dishes.stream()
                .filter(d -> d.getCalories() < 400)
                .sorted(comparing(Dish::getCalories))
                .map(Dish::getName)
                .collect(toList());
    }
```

关于用法我们后面再讲，我们先来讨论一下流

### 为什么要用流

Java8中的集合都支持stream方法，他会返回一个流Stream，目的在于**简便计算**，而集合的目的在与存储数据，这是他们的关键区别。流所提供的方法都是方便我们计算的。例如`filter、map、reduce、find、match、sort`等。

此外，还可以让我们和简单的就使用并行的方法计算。大大的简单了fork/join的难度和高并发的痛苦。

如果看过如何构建一个构建器模式的同学就会发现有一些的相像的。

### 使用流

#### 筛选和切片

```
 return dishes.stream()
                .filter(d -> d.getCalories() < 400)
                .collect(toList());
```

就像`filter`这样的筛选出想要的结果就行，还可以用类似sql语句中的distinct。

```
        List<Integer> numbers = Arrays.asList(1, 2, 1, 3, 3, 2, 4);
        numbers.stream()
               .filter(i -> i % 2 == 0)
               .distinct()
               .forEach(System.out::println);
```

返回一个没有重复的数字。

还有使用`limit(n)`来限制大小，只返回前面n个。

和`skip(n)`来跳过前面的n个。

```
        List<Dish> dishesLimit3 =
            menu.stream()
                .filter(d -> d.getCalories() > 300)
                .limit(3)
                .skip(2)
                .collect(toList());
```

#### 映射

先来两个例子

```
List<String> dishNames = menu.stream()
							.map(Dish::getName)
                            .collect(toList());
```

```
List<String> words = Arrays.asList("Hello", "World");
List<Integer> wordLengths = words.stream()
							.map(String::length)
                            .collect(toList());
```

map会接受一个方法作为参数，每一个元素都会执行这个方法

需要注意的是

返回的流的类型是传入方法返回的类型，比如`.map(String::length)`返回的就是Stream<Integer>。

**`flatmap`的 使用**

当我们有更加复杂的需求时，比如一张单词表，需要列出里面各不相同的字符该如何呢。

比如"example"和"apple"两个单词，列出的应该是"e,x,a,m,p,l"。

我们使用上面的方法很难实现，因为使用`s.split()`方法返回的是一个数组，我们无法将数组里面的每一个值都去重。

我们使用`flatmap`将一个流中的每个值都换成另一个流。然后再把所有的流都连接起来成为一个流。

这么说非常的抽象，比如"example"和"apple"经过了`flatmap`拆分成`e,x,a,m,p,l,e,a,p,p,l,e`这样的流，再去重distinct。

```
      words.stream()
      			 .map(word -> word.split(""))
                 .flatMap(Arrays::stream)
                 .distinct()
                 .forEach(System.out::println);
```



#### 查找和匹配

这个很好理解，判断是否流里面的某个元素符合条件，按语义也能理解，有没有匹配的，&&和||的用法。

```
    private static boolean isVegetarianFriendlyMenu(){
        return menu.stream().anyMatch(Dish::isVegetarian);
    }
    
    private static boolean isHealthyMenu(){
        return menu.stream().allMatch(d -> d.getCalories() < 1000);
    }
    
    private static boolean isHealthyMenu2(){
        return menu.stream().noneMatch(d -> d.getCalories() >= 1000);
    }
    
    private static Optional<Dish> findVegetarianDish(){
        return menu.stream().filter(Dish::isVegetarian).findAny();
    }
```

除了`findAny()`还有`findFirst()`发现第一个元素等。

最后一个是能否找的到，返回的业是Java8新出的类Optional<Dish>。这个类里面的方法

`optional.isPresent()`判断里面是否有值，类似于 `==null`的操作

#### 归约

把流约成一个值的方法称为归约。也称为查询

```
int sum = numbers.stream().reduce(0, (a,b) -> (a+b));
```

两个参数是：

- 一个初始值，这里是0
- 将两个数结合成一个数`BinaryOperator<T>`，这里我们用Lambda

还可以判断最大最小，使用`(a,b)->a<b?a:b)`或者`reduce(Integer::max)`都行



还有其他的一些终端的操作如`forEach,count`等，就不一一讲了

#### 收集器

收集器的作用就是将收集到的流最终转化成什么容器，是List还是Map。

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190619205057790.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM5MDcxNTMw,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190619205117111.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM5MDcxNTMw,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190619205125216.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM5MDcxNTMw,size_16,color_FFFFFF,t_70)