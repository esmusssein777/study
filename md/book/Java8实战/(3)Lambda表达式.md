[TOC]

### Lambda表达式

#### Lambda实战

最开始的时候读取一行文件，当我们需要读取两行的时候，我们需要复制代码改造

```
    public static String processFileLimited() throws IOException {
        try (BufferedReader br =
            new BufferedReader(new FileReader("lambdasinaction/chap3/data.txt"))) {
            	return br.readLine();
        	}
    }
```

后来我们改造成Lambda

```
public interface BufferedReaderProcessor{
	public String process(BufferedReader b) throws IOException;
}
	
public static String processFile(BufferedReaderProcessor p) throws IOException {
	try(BufferedReader br = new BufferedReader(
		new FileReader("lambdasinaction/chap3/data.txt"))){
			return p.process(br);
		}

}
```

我们使用函数式接口来传递行为，控制我们读取或者操作的逻辑。下面改成读取两行

```
String oneLine = processFile((BufferedReader b) -> b.readLine());

String twoLines = processFile((BufferedReader b) -> b.readLine() + b.readLine());
```

我们每一次的使用自己的接口，这样接口会很多，很杂乱，Java8自带了一些函数式的接口给我们。

#### 自带的函数式接口

##### Predicate

Predicate接口定义的是一个`test`方法接口，返回的是一个Boolean值

```
@FunctionalInterface
public interface Predicate<T> {
	boolean test(T t);
}
```

用法

```
public static <T> List<T> filter(List<T> list, Predicate<T> p){
    List<T> result = new ArrayList<>();
    for(T t : list){
        if(p.test(t)){
            result.add(t);
        }
    }
    return result;
}

Predicate<String> noEmpty = (String s) -> !s.isEmpty();
List<String> list = filter(lists, noEmpty);//判断是否为空，不为空的加入到list中
```

##### Consumer

Consumer接口定义的是一个`accept`方法接口，是一个void方法

```
@FunctionalInterface
public interface Consumer<T> {
	void accept(T t);
}
```

用法

```
public static <T> void forEach(List<T> list, Consumer<T> c) {
	for (T t : list) {
		c.accpt(t);
	}
}

forEach(list, (Interger i) -> sout(i));//输出list中的所有值
```



##### Function

Function接口定义的是一个`apply`方法接口，返回的是一个泛型

```
@FunctionalInterface
public interface Function<T, R> {
	R apply(T t);
}
```

用法：

```
public static <T, R> list<R> map(List<T> list, Function<T, R> f) {
	List<R> result = new ArrayList<>();
	for (T t : list) {
		result.add(f.apply(t));
	}
	return result;
} 

List<Integer> list = map(list, (String s) -> s.length);//将传入的String值的长度加入到list中去
```



对比function包下面的这三个接口，我们发现他们只是返回的值不同，我们三次都是传入了一个list和一个泛型，predicate是判断list的值是否为空，Consumer是将list里面的值输出，Function是将list的值根据传入的泛型规则加入到list里面去。非常的灵活。



### 方法引用

#### 定义

当你需要使用方法引用时，目标引用放在分隔符 : : 前面，方法的名称放在后面

比如``Apple::getWeight``就是引用了Apple的getWight方法，不需要括号，因为没有实际的调用这个方法

方法引用就是Lambda表达式`(Apple a) -> a.getWeight()`的快捷方式

#### 使用

**指向静态方法**

| Lambda   | (args) -> ClassName.staticMethod(args) |
| -------- | -------------------------------------- |
| 方法引用 | ClassName::staticMethod                |

**指向实例方法**

| Lambda   | (args0, rest) -> args0.instanceMethod(rest)//args0是ClassName类型 |
| -------- | ------------------------------------------------------------ |
| 方法引用 | ClassName::staticMethod                                      |

**指向现有对象的实例方法**

| Lambda   | (args) -> expr.instanceMethod(args) |
| -------- | ----------------------------------- |
| 方法引用 | expr::instanceMethod                |

