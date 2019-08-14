# 优先考虑泛型

​      **使用泛型比使用需要在客户端代码中进行装换的类型来的更加安全，也更加容易。在设计新类型的时候，要确保他们不需要这种装换就可以使用。这通常意味着要不类做出泛型的，只要时间允许，就把现有的类型都泛型化。这对于类型的新用户来说会变得更加轻松，更不会破坏现有的客户端。**

编写自己的泛型会比较困难一些，但是值得花些时间去学习如何编写。

一般来说，将集合声明参数化，以及使用JDK所提供的泛型和泛型方法，这些都不太困难，编写自己的泛型会比较困难一些，但是值得花些时间去学习如何编写。

考虑第6条中这个简单的堆栈实现：

```
// Object -based Collection - a prime candidate for generics
public class Stack{
    private Object[] elements;
    private int size = 0;
    private static final int DEFAULT_INITIAL_CAPACITY = 16;

    public Stack() {
        elements = new E[DEFAULT_INITIAL_CAPACITY];
    }

    public void push(Object e) {
        ensureCapacity();
        elements[size++] = e;
    }

    public Objcet pop() {
        if (size == 0)
            throw new EmptyStackException();
        Objcet result = elements[--size];
        elements[size] = null;
        return result;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    private void ensureCapacity() {
        if (elements.length == size)
            elements = Arrays.copyOf(elements, 2 * size + 1);
    }
}
```



这个类是泛型化的主要备选对象，换句话说，可以适当的强化这个类来利用泛型。根据实例情况来看，必须转换从堆栈里弹出的对象，以及可能运行时失败的那些转换。

将类泛型化的第一个步骤是给他的声明添加一或者多个类型参数，在这个例子中有一个类型参数，他表示堆栈的元素类型，这个参数的名称通常为E。

下一步用相应的类型参数替换所有的Object类型，让后试着编译最终的程序：

```
// Initial attempt to generify Stack = won't compile!
public class Stack<E> {
    private E[] elements;
    private int size = 0;
    private static final int DEFAULT_INITIAL_CAPACITY = 16;

    public Stack() {
        elements = new E[DEFAULT_INITIAL_CAPACITY];
    }

    public void push(E e) {
        ensureCapacity();
        elements[size++] = e;
    }

    public E pop() {
        if (size == 0)
            throw new EmptyStackException();
        E result = elements[--size];
        elements[size] = null;
        return result;
    }

    ...// no changes is isEmpty or ensureCapacity
}
```



通常，你将至少得到一个错误或警告，这个类也不例外。幸运的是，这个类只产生一个错误，如下：

```
    Stack.java 8: generic array creation
        elements =new E[DEFAULT_INITIAL_CAPACITY];
```



如25条条所述，你不能创建不可具体化的类型的数组，如E。没当编写用数组支持的泛型时，都会出现这个问题。 
解决这个问题有两种方法： 
**1.直接绕过创建泛型数组的禁令：创建一个Object的数组，并将它装换泛型数组类型。**

错误是消除了，但是编译器会产生一条警告。这种用法是合法的。但不是类型安全。

```
Stack.java:8 warning:[unchecked] unchecked cast
found : Object[].required:E[]
    elments =(E()) new Objcet[DEFAULT_INITIAL_CAPACITY]
```



编译器不可能证明你的程序是类型安全的，但是你可以证明。你自己必须确保未受检的转换不会危及到程序的类型安全性。相关的数据保存一个在私有的域中，永远不会被返回到客户端，或者传给任何其他方法。这个数组中保存的唯一元素，是传给push方法的那些元素，它们的类型为E，因此未受检的转换不会有任何危害。

一旦你证明了未受检的转换时安全的，就要在尽可能小的范围禁止警告，在这种情况下，构造器只包含未受检的数组创建，因此可以在整个构造器中禁止这条警告。通过增加一条注解来完成禁止，Stack能够正确无误的进行编译，你就可以使用它了，无需显式的转换，也无需担心会出现ClassCastException异常：

```
//The elements array will contain only E instances from push(E).
//This is sufficient to ensure type safety,but the runtime 
//type of the array won't be E[];it will always be Object[]！
@SuppressWarnings("unchecked")
public Stack(){
    elements = (E[])new Object[DEFAULT_INITIAL_CAPACITY];
```



2.将elements域的类型从E[]改为Object[]。这么做会得到一条不同的错误：

```
Stack.java 8: incompatible types
    found : Object, required:ECField
        E result=elements[--size];
```



通过把从数组中获取到的元素由Object转换成E，可以将这条错误变成一条警告：

```
Stack.java 19: warning : [unchecked] unchecked cast
        E result=elements[--size];
```



由于E是一个不可具体化的类型，编译器无法再运行时检验转换。你是可以自己证实未受检的转换是安全的，因此可以禁止该警告。根据第24条的建议，我们只要在包含未受检转换的任务上禁止警告，而不是在整个pop方法上就可以了，如下：

```
//Appropriate suppression of unchecked warning
public E pop(){
    if(size==0)
        throw new EmptyStackException();

    //push requires elements to be of type E,so cast is correct
    @SuppressWarnings("unchecked")
    E result=(E) elements[--size];

    elements[size]=null;//Eliminate obsolete reference

    return result;
```



具体选择哪一种方法来处理泛型数组创建错误，则看个人的偏好。但是禁止数组类型的未受检比禁止标量类型的更加危险，所以采用第二种方案。但是在比Stac更实际的泛型类中，或许代码中会有多个地方需要从数组中读取元素，因此选择第二种方案需要多次装换成E，而不是只装换E[]，这也是第一种方案之所以更常用的原因。

下面的程序示范了泛型Stack类的使用，程序以相反的顺序打印出他的命令行参数，并装换成大写字母。如果要在从堆栈中弹出的元素上调用String的toUpperCase,并不需要显示的转换，并且会确保自动生成的转换会成功：

```
//Little program to exercise our generic Stack
public static void main(String[] args){
    Stack<String> stact=new Stack<String>();
    for(String arg:args)
        stack.push(arg);
    while(!stack.isEmpty())
        System.out.println(stack.pop.toUpperCase());
}
```



上述示例与第25条相矛盾，第25条鼓励优先使用列表而非数组。实际上并不可能或者总想在泛型中使用列表。java并不是生来就支持列表，因此有些泛型如ArrayLis，则必须在数组上实现。为了提升性能，其他泛型如HashMap也在数组上实现。

绝大多数泛型就像我梦Stack示例一样，因为它们的类型参数没有限制：你可以创建Stack< Object>、Stack< int[]>、Stack< List< String>>,或者任何其他对象引用类型的Stack。注意不能创建基本类型的Stack：企图创建Stack< int>或者Stack< double>会产生一个编译器错误。这是Java泛型系统根本的局限性。你可以通过使用基本包装类型来避开这条限制。

有一下泛型限制了可允许的类型参数值。例如，考虑javautil.concurrent.DelayQueue,其声明如下:

```
class DelayQueue<E extends Delayed> implements BlockingQueue<E>;
```



类型参数列表要求实际的类型参数E必须是java,util.concurrent.Delayed的一个子类型，它允许DelayQueue实现及其客户端在DelayQueue元素上利用Delayed方法，无需显示的转换，也没有出现ClassCastException的风险。类型参数E被称作有限制的类型参数。注意，子类型关系确认了每个类型都是都是它自身的子类型，因此创建DelayQueue< Delayed>是合法的。

# 优先考虑泛型方法

**泛型方法就像泛型一样，使用起来比要求客户端转换输入参数并返回值的方法来的更加安全，也更加容易。就像类型一样，你应该确保新的方法可以不用转换就能使用，这通常意味着要将它们泛型化。并且就像类型一样，还应该将现有的方法泛型化，使新用户使用起来更加轻松，且不会破坏现有的客户端。**

**就如类可以从泛型中受益一般，方法也一样。静态工具方法尤其适合于泛型化。**

编写泛型方法与编写类型类型相类似。 
例：他返回两个集合的联合：

```
// Users raw types - unaccepable!
public static Set union(Set s1, Set s2) {
    Set result = new HashSet(s1);
    result.addAll(s2);
    return result;
}
```



这个方法可以编译，但是有两条警告：

```
Unioc.java:5:warning:[unchecked] unchecked call to
HastSet(Collection< ? extends E> as a member fo raw type HastSet
    Set result = new HashSet(s1);

Unioc.java:5:warning:[unchecked] unchecked call to
addAll(Collection< ? extends E> as a member fo raw type HastSet
    result.addAll(s2);
```



为了修正这些警告，使方法变成类型安全的，要将方法声名修改为声明一个类型参数，表示这三个元素类型(两个参数及一个返回值)，并在方法中使用类型参数。声名类型参数的类型参数列表，处在方法的修饰符及其返回类型之间。在这个实例中，类型参数列表为< E>，返回类型为Set< E>。类型参数的命名惯例与泛型方法以及泛型的相同。

```
// Generic method
public static <E> Set<E> union(Set<E> s1, Set<E> s2) {
    Set<E> result = new HashSet<E>(s1);
    result.addAll(s2);
    return result;
}
```



至少对于简单的泛型方法而言，就是这么回事了。现在改方法编译时不会产生任何警告，并提供了类型安全性，也更容易使用。以下是一个执行该方法的简单程序。程序不包含装换，编译时不会有错误或者警告：

```
//Simple program to exercise generic method
public static void main(String[] args){
    Set<String> guys =new HashSet<String>{
        Array.asList("Tom","Dick","Harry"));

    Set<String> stooges =new HashSet<String>{
        Array.asList("Larry","Moe","Curly"));

    Set<String> aflCio=unioc(guys,stooges);
    System.out.printle(aflCio);
    }
}
```



运行这段程序是，会打印 [Moe,Harry,Tom,Curly,Larry,Dick]。 元素的顺序是依赖于实现的。

union方法局限在于，三个集合的类型(两个输入参数及一个返回值)必须全部相同。利用有限制的通配符类型可以使这个方法变得更回灵活。

泛型方法的一个显著特征是，无需明确指定类型参数的值，不像调用泛型构造器的时候是必须指定的。对于上述程序而言，编译器发现uniond的两个参数都是Set< String>类型，因此知道类型参数E必须为String，这个过程称作为类型推导。

如第一条所述，可以利用泛型方法调用所提供的类型推导，是创建参数化类型实例的过程变得更加轻松。提醒一下：在调用泛型构造器的时候，要明确传递类型参数的值可能有点麻烦。类型参数出现在了变量的声明的左右两边，显得冗余：

```
// Parameterized type instance creation with constructor
Map<String, List<String>> anagrams = 
                 new HashMap<String, List<String>>();
```



为了消除冗余，可以编写一个泛型静态工厂方法，与想要使用的每个构造器相对应。例如，下面是一个无参的HashMap构造器相对应的泛型静态工厂方法：

```
// Generic static factory method
public static <K, V> HashMap<K, V> newHashMap() {
    return new HashMap<K, V>();

}
```



通过这个泛型静态工厂方法，可以用下面这段简洁的大码来取代上面那个重复的声明：

```
//Parameterized type instance creation with static factory
Map<String,List<String>> anagrans=newHashMap(); 
```



相关的模式泛型单例工厂。有时，会需要创建不可变但又适合于许多不同类型的对象。由于泛型是通过擦除来实现的，可以给所有的必要的类型参数使用同一个单个对象，但是需要编写一个静态的工厂方法，重复地给每个必要的类型参数分发对象。这种模式叫做“泛型单例工厂”，这种模式最常用于函数对象。如Collections.reverseOrder,但也适用于像Collections.emptySet这样的集合。

假设有一个接口，描述了一个方法，该方法接受和返回某个类型T的值：

```
public interface UnaryFunction<T> {
    T apply(T arg);
}
```



现在假设要提供一个恒等函数。如果 在每次需要的时候都重新创建一个，这样会很浪费，因为它是无状态的。如果泛型被具体化，每个类型都需要一个桓等函数，但是它们被擦除以后，就只需要一个泛型单例。请看以下示例：

```
// Generic singleton factory pattern
private static UnaryFunction<Object> INDENTITY_FUNCTION =
     new UnaryFunction<Object> {
         public Object apply(Object arg) { return arg; }
     };

// IDENTITY_FUNCTION is stateless and its type parameter is
// unbounded so it's safe to share one instance across all types.
@SuppressWarnings("unchecked")
public static <T> UnaryFunction<T> identityFunction() {
    return (UnaryFunction<T>)INDENTITY_FUNCTION;
}
```



IDENTITY_FUNCTION装换成（UnaryFunction< T>）,产生一条为受检的装换警告。因为UnaryFunction< Object>对于每个T来说并非额每个都是UnaryFunction< T>。但是恒等函数很特殊；他返回未被修改的参数，因此我们知道无论T的值是什么，用它作为UnaryFunction< T>都是类型安全的。因此：我们可以放心地禁止由这个装换所产生的未受检转换警告。一旦禁止，代码在编译时就不会出现任何错误或者警告。

以下是一个范例程序，利用泛型单例作为UnaryFunction< String>和UnaryFunction< Number>。像往常一样，它不包含，编译时没有出现错误或者警告：

```
 //Simple program to exercise generic singleton
public static void main(String[] args){
    String[] strings ={"jute","hemp","nylon"};

    UnaryFunction<String> sameString=identityFunction();

    for(String s:strings)
        System.out.printle(sameString.addly(s);

    Number[] numbers={1,2.0,3L};
    UnaryFunction<Number> sameNumber=identityFunction();

    for(Number n:numbers)
        System.out.printle(sameNumber.addly(n);
    }
}
```



虽然相对少见，但是通过某个包含该类型参数本身的表达式来限制类型参数是允许的。这就是递归类型限制。递归类型限制最普遍的用途与Comparable接口有关，它定义类型的自然顺序：

```
public interface Comparable<T>{
        int compareTo(T o);
    }
```



类型参数T定义的类型，可以与实现Comparable< T> 的类型的元素进行比较。实际上，几乎所有类型都只能与他们自身的类型的元素相比较。因此，例如String实现Comparable< String>,Integer实现Comparable< Integer>,等等。

许多方法都带有一个实现Comparable接口的元素列表，并在其中进行搜索，计算出它的最小值或者最大值，等等。要完成这其中的任何一项工作，要求列表中的每个元素都能够与列表中的其他元素相比较，换句话说，列表的元素可以相互比较。下面是如何表达这种约束条件的一个示例：

```
// Using a recursive type bound to express mutual comparability
public static <T extends Comparable<T>> T max(List<T> list) {
    ...
```



类型限制< T extends Comparable>,可以读作“针对可以与自身进行比较的每个类型T”，这与互比性的概念或多或少有一些一致。

下面的方法就带有上述声明。它根据元素的自然顺序计算列表的最大值，编译时没有出现错误或者警告：

```
//Returns the maximun value in a list - uses recursive type bound
public static <T extends Comparable<T>> T max(List<T> list){
    Iterator<T> i=list.iterator();
    T result=i.next();
    while(i.hasNext){
        T t=i.next();
        if(t.compareTo(result)>0)
            result=t;
    }
    return result;
}
```



递归类型限制可能比这个要复杂得多，但幸运的是，这种情况并不经常发生。如果你理解这种习惯用法以及其通配符变量，就能够处理在实践中遇到的许多递归类型限制了。



# **利用有限制通配符来提升API的灵活性**

**如果参数化类型表示一个T生产者，就使用<? extends T>，如果表示一个T的消费者，就使用<? super T>。**

为什么要这些？

上面的Stack类

现在需要增加一个方法:

```
    public void pushAll(Iterable<E> src){
        src.forEach(e->push(e));
    }
```



这个方法在编译的时候正确无误，但是也不能总是正确，比如：

```
 public static void main(String[] args) {
        Stack<Number> s = new Stack<>();
        List<Integer> list = new ArrayList<>();
        s.pushAll(list);
    }
```



在这种情况下，由于泛型的不可变性，导致不能添加，编译无法通过,但是从理解层面上来说，这应该是被允许的。number是可以接受integer类型的

增加方法的灵活性，可以这样编写：

```
// Wildcard type for parameter that serves as an E producer
public void pushAll(Iterable<? extends E> src) {
	for (E e: src)
		push(e);
}
```



与pushAll相对应的，我们在新增一个popAll 方法：

```
    public void popAll(Collection<E> dst){
        while(!isEmpty())
            dst.add(pop());
    }
```



和上面方法相似，这个方法初一看并没有什么不妥。但是并不总是正确：

比如我想传递一个List<Object>进去接收，就像这样：

```
    public static void main(String[] args) {
        Stack<Number> s = new Stack<>();
        List<Object> list = new ArrayList<>();
        s.popAll(list);
    }
```



同样编译无法通过,Collection<Number> c = new ArrayList<Object>()这是错误的：

但是从实际角度出发，这应该是被允许的，List<Object> 列表是可以添加Number类型的，所以这个方法依然有漏洞：

这个时候可以修改如下：

```
    public void popAll(Collection<? super  E> dst){
        while(!isEmpty())
            dst.add(pop());
    }
```



这样的话，编译可以通过，而且类型也是安全的：

结论很明显。为了获得最大限度的灵活性，要在表示生产者或者消费者的输入参数上使用统配符类型。如果某个输入参数既是生产者也是消费者，那么统配符就不在适用了。

对 union进行修改：

```
  public static <E> Set<E> union(Set<E> s1,Set s2){
        Set<E> result = new HashSet<>(s1);
        result.addAll(s2);
        return result;
    }
```



由于s1,s2,对于整个类来说是属于生产者，所以应该用extends:

```
    public static <E> Set<E> union(Set<? extends E > s1,Set<? extends E> s2){
        Set<E> result = new HashSet<>(s1);
        result.addAll(s2);
        return result;
    }
```



注意返回类型依然是set<E>.不要用通配符类型作为返回类型。除了为用户提供额外额灵活性外，它也会要求用户必须使用通配符类型。

统配符类型对于用户来说应该是无形的，如果用户必须考虑通配符类型，类的API或许就会出错。

有这样一个方法：

```
   public static <T extends Comparable<T>> T max(List<T> list){
       Iterator<T> i = list.iterator();
       T result = i.next();
       while(i.hasNext()){
           T t = i.next();
           if(t.compareTo(result) > 0)
               result = t;
       }
       return result;
   }
```



根据原则该如何修改呢?

```
   public static <T extends Comparable<? super T>> T max(List<? extends T> list){
       Iterator<? extends T> i = list.iterator();
       T result = i.next();
       while(i.hasNext()){
           T t = i.next();
           if(t.compareTo(result) > 0)
               result = t;
       }
       return result;
   }
```



我们来分析一下：

list中的泛型，对于类来说，无疑是生产者，生产出最大值，所以应该是extends
comparable中的泛型，对于整个类来说，是用来消费产生顺序关系的，所以应该用super
针对于一下方法：

```
   public static <E> void swap(List<E> list,int i,int j){};
   public static void swap(List<?> list,int i,int j);
```



这两种方法，那种方法更好呢？第二种会更好一些，因为它更加简单，在整个静态方法中，泛型其实只出现了一次，，这种情况下，是可以用通配符取代它的。但是第二个方法有一个问题：

```
   public static void swap(List<?> list,int i,int j){
       list.set(i,list.set(j,list.get(i)));
   };
```



无法编译，这里可以使用一个辅助方法来捕捉通配符类型：

```
   public static void swap(List<?> list,int i,int j){
       swapHelper(list,i,j);
   };
   private static <E> void swapHelper(List<E> list,int i,int j){
        list.set(i,list.set(j,list.get(i)));
   }
```



这样既能提供简洁的api,也能达到捕获通配符的目的

# **优先考虑类型安全的异构容器**

集合API说明泛型的用法：限制容器只能由固定数目的类型参数。

可以通过将类型参数放在键上而不是容器上来避开这一限制

对于类型安全的异构容器，可以用Class对象作为键，以这种方式使用的Class对象称作类型令牌

我们创建一个Favorite类来模拟这种情况

```
public class Favorites {       
        private Map<Class<?>, Object> favoties = new HashMap<Class<?>, Object>();        
        public <T> void putFavorite(Class<T> type, T instance) {
            if (type == null) {
                throw new NullPointerException("Type is Null");
            }
            favoties.put(type, instance);
        }
        public <T> T getFavorite(Class<T> type) {
            return type.cast(favoties.get(type));
        }
    }
```



class方法返回的是一个class<T>的形式。Favorites是类型安全的，它总是按照键返回正确的值，同时它也是异构的，因为它的容器的键不是同一种类型，这有别于传统的Map，因此，将Favorites称作类型安全的异构容器。异构来自哪？答案是无限制通配符的键Class<？>，在这里它仅代表是某种class，因此允许将不同类的class放入同一个Map，这就是异构的原因。注意，因为我们使用的值类型是Object，因此Map并无法保证键一对能对应正确的值，它只知道值是一个Object就可以了，这种对应的关系是实现者自己来确保的。手动重新建立类型与值之间的关系是在getFavorite方法中进行的，利用Class的cast方法，将对象动态地转换成Class对象所表示的类型。cast只是检验它的参数是不是为Class对象所表示的类型的实例。


 Favorites类有两种局限：一是恶意用户可以通过使用原生态形式的Class来破坏年Favorites实例的类型安全。这种方式可以通知在putFavorite中进行类型检查来确保实例对象进行检查。

```
	public <T> void putFavorite(Class<T> type, T instance) {
		if (type == null)
			throw new NullPointerException("Type is null");
		favorites.put(type, type.cast(instance);
	}
```



 第二个局限性在于它不能用在不可具体化的类型中。比如说可以存储喜爱的String,String[]，但是不能存储List<String>。因为 List<String>.class是语法错误。因为在运行时他们的类型会被擦除，所在List<String>与List<Integer>实际上是共用一个Class。如果需要限制些可以传递给方法的类型，则可以使用有限制的通配符类型。



```
public <T extends Annotation>
      T getAnnotation(Class<T> annotationType);
```



在这面这段代码里，如果想把一个Class<?>传递给getAnnotation方法，那么按照要求，可能想到可以将其转换成Class<? extends Annotation>，但是这种行为是非受检的，会收到编译器的警告，但是，可以利用Class类提供的一个安全且动态地执行这种转换的实例方法，asSubclass，它将调用它的Class对象转换成用其参数表示的类的一个子类型，如果转换成功，该方法就返回它的参数，如果失败则抛出ClassCastException。见如下例子：

```
public <T extends Annotation>
      T getAnnotation(Class<T> annotationType);
 
// Use of asSubclass to safely cast to a bounded type token
static Annotation getAnnocation(AnnocationElement element,
		                        String annotationTypeName) {
	Class<?> annotationType = null;// Unbounded type token
	try {
		annotationType = Class.forName(annotationTypeName);
	} catch (Exception ex) {
		throw new IllegalArgumentException(ex);
	}
	return element.getAnnotation {
		annotationType.asSubclass(Annotation.class);
	}
}
```

