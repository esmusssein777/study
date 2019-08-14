# 慎用重载

在Java中，同一个类中的多个方法可以有相同的方法名称，但是有不同的参数列表，这就称为方法重载（method overloading）。

参数列表又叫参数签名，包括参数的类型、参数的个数、参数的顺序，只要有一个不同就叫做参数列表不同。

如下面的例子：

```
public class Demo {
 
	//一个普通得方法，不带参数，无返回值
	public void add(){
		//method body
	}
	
	//重载上面的方法，并且带了一个整形参数，无返回值
	public void add(int a){
		//method body
	}
	
        //重载上面的方法，并且带了两个整型参数，返回值为int型
	public int add(int a,int b){
		//method body
		return 0;
	}
 
}
```



方法的重载的规则：

- 方法名称必须相同。
- 参数列表必须不同。
- 方法的返回类型可以相同也可以不相同。
- 仅仅返回类型不同不足以称为方法的重载

### **但是，我们应该避免胡乱使用重载机制**

**重载容易产生的问题：重载是根据参数的静态类型选择执行方法，而方法重写是根据参数的动态类型选择执行方法。 例如People p = new Man()；那么People是静态类型，Man是动态类型。 覆盖机制很容易让期望落空。因为如果不知道重载是根据参数的静态类型选择执行方法，那么覆盖就不能执行期待执行的方法。**



```
public class CollectionClassifier  
{  
    public static String classify(Set < ? > s)  
    {  
        return "Set";  
    }  
  
    public static String classify(List < ? > lst)  
    {  
        return "List";  
    }  
  
    public static String classify(Collection < ? > c)  
    {  
        return "Unknown Collection";  
    }  
  
    public static void main(String[] args)  
    {  
        Collection < ? >[] collections = {new HashSet < String >(),new ArrayList < BigInteger >(),new HashMap < String,String >().values()};  
        for(Collection < ? > c:collections)  
            System.out.println(classify(c));  
    }  
}
```



结果出现的是三行一样的答案Unknown Collection，这就是因为重载是静态类型选择方法。

# 慎用可变参数

可变参数的机制是**通过先创建一个数组，数组的大小为在调用位置所传递的参数数量，然后将参数值传到数组中，最后将数组传递给方法**

```
package com.ligz.Chapter7.Item42;

/**
 * @author ligz
 */
public class Main {
	static int sum(int... args) {
	    int sum=0;
	    for(int arg : args)
	        sum += arg;
	    return sum;
	}
	public static void main(String[] args) {
		int[] i = {1,2,3};
		System.out.println(sum(i));
	}
}
```



但是，不传参也是可以的，这样容易导致错误的出现，所以常见的策略是首先指定正常参数，把可选参数放在后面。

```
//可变参数必须放在参数列表的最后
static int min(int firstArg, int... remainingArgs) {

    int min = firstArg;
    for(int arg : remainingArgs)
        if(arg < min)
            min = arg;
    return min;
}
```



 在重视性能的情况下，使用可变参数机制要小心，因为可变参数方法的每次调用都会导致进行一次数组分配和初始化，有一种折中的解决方案，假设确定某个方法大部分调用会有3个或者更少的参数，就声明该方法的5个重载，每个重载带有0至3个普通参数，当参数数目超过3个时，使用可变参数方法。

```
public void foo() {}
public void foo() {int a1}
public void foo() {int a1, int a2}
public void foo() {int a1, int a2, int a3}
public void foo() {int a1, int a2, int a3, int... rest}
```



# 返回零长度的数组或者集合，而不是null

有人觉得，null的返回值比零长度数组更好，因为它避免了分配数组所需要的开销，显然这种说法是站不住脚的：

对于这个问题，逻辑出错比性能下降造成的后果更严重，除非有足够多的证据证明确实是在这里造成的性能问题；
零长度的数组，其实并不比null占用太多的额外开销；
如果真的返回次数太多，其实我们可以使用同一个零长度的数组。
**返回空：**

```
	private final List<Cheese> cheesesInStock=new ArrayList<>();
	public Cheese[] getCheeses()
	{
		if(cheesesInStock.size()==0)
		{
			return null;
		}
		....
	}
```



**数组返回0长度：**

```
	private final List<Cheese> cheesesInStock = new ArrayList<>();
	private static final Cheese[] EMPTY_CHEESE_ARRAY = new Cheese[0];
 
	public Cheese[] getCheeses() {
		return cheesesInStock.toArray(EMPTY_CHEESE_ARRAY);
	}
```



对于集合返回0长度：

```
	public List<Cheese> getCheeseList() {
		if (cheesesInStock.isEmpty()) {
			return Collections.emptyList();
		} else {
			return cheesesInStock;
		}
	}
```



在Collections中有专门针对List,Set,Map的空的实现。如：

Collections.emptyList()

Collections.emptySet();

Collections.emptyMap();

总之，返回类型为数组或者集合的方法，没有理由返回null，而是返回一个零长度的数组或者集合。这种习惯的做法（返回null）可能来自于C，因为C中，数组和数组的长度是分开计算的（ sizeof(数组名)/sizeof(数组名[0])），如果返回的数组长度为0，再分配一个数组，就没有任何意义了。