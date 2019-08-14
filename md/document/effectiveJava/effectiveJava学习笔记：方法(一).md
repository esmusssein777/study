# 检查参数的有效性

1、一般在方法执行之前先检查参数的有效性，如果参数值无效，那么很快它就会失败，并且清楚的抛出合适的异常。

如果这个方法没有检查参数的异常，那么可能在方法处理中出现令人费解的异常。更糟糕的有可能是，方法可以正常返回，但是却使得某个对象处于被破坏的状态.

2、对于公有方法，可以在Javadoc中的@throw标签来说明违反异常时所抛出的异常类型

3、非公有方法通常应该使用断言（assert）来检查它们的参数（在生产环境中，一般是不支持assert的，因此这样可以提高效率，没有成本开销。所以，assert只在私有方法中使用，因为私有方法的调用者开发者，他和被调用者之间是一种弱契约关系，或者说没有契约关系，其间的约束是依靠开发者自己控制的，开发者应该有充分的理由相信自己传入的参数是有效的。所以，从某种角度上来说，assert只是起到一个预防开发者自己出错，或者是程序的无意出错）

```
	private static void sort(long a[], int offset, int length) {
		assert a != null;
		assert offset >= 0 && offset <= a.length;
		assert length >= 0 && length <= a.length - offset;
	}
```



4、有一些参数暂时没有直接用到，只是保存起来供以后使用，这种参数的有效性检查也是尤其重要

```
    static List<Integer> intArrayAsList(final int[] a) {
        if (a == null)
            throw new NullPointerException();
 
        return new AbstractList<Integer>() {
            public Integer get(int i) {
                return a[i];  // Autoboxing (Item 5)
            }
 
            @Override public Integer set(int i, Integer val) {
                int oldVal = a[i];
                a[i] = val;     // Auto-unboxing
                return oldVal;  // Autoboxing
            }
 
            public int size() {
                return a.length;
            }
        };
    }
```



5、尽管在构造器中检查参数的有效性非常必要，但是也有例外，可能在有效情况下检查参数的有效性是及其昂贵的，甚至是不切实际的。

**并非对参数的任何限制都是好事，一般来说要尽可能的通用， 符合实际的需要。假如方法对它能接受的参数都能完成合理的计算，那么对于参数的限制其实是越少越好的。因此，鼓励开发者把限制写到文档中，并在方法的开头显式的检查参数的有效性。**



# 必要时进行保护性拷贝

### 容易被破坏的内部约束条件

虽然如果没有主动提供公共方法和变量，外部是无法修改类内部的数据的。但是，对象可能会在无意识的情况下提供帮助。例如，下面就是一个通过引用来修改类内部的数据，而破坏对象内部的约束条件的例子：

```
public final class Period {
	private final Date start;
	private final Date end;
 
	public Period(Date start, Date end) {
		if (start.compareTo(end) > 0)
			throw new IllegalArgumentException(start + " after " + end);
		this.start = start;
		this.end = end;
	}
 
	public Date start() {
		return start;
	}
 
	public Date end() {
		return end;
	}...
```



虽然没有p.setEnd()方法修改内部信息，但是通过Date的引用，我们可以修改Person内部的信息

```
Date start = new Date();
Date end = new Date();
Period p = new Period(start, end);
end.setYear(78);  // Modifies internals of p!
System.out.println(p);
```



### 对构造器的每个可变参数进行保护性拷贝

```
package com.ligz.Chapter7.Item39;

import java.util.Date;

/**
 * @author ligz
 */
public class Period {
	private final Date start;
	private final Date end;
	
	public Period(Date start, Date end) {
		this.start = new Date(start.getTime());
		this.end = new Date(end.getTime());
 
		if (this.start.compareTo(this.end) > 0)
			throw new IllegalArgumentException(start + " after " + end);

	}
	public Date start() {
		return new Date(start.getTime());
	}
	
	public Date end() {
		return new Date(end.getTime());
	}
}
```



值得注意的是，这里用的是获取需要的Date数据来进行拷贝，才不是使用clone进行拷贝，是因为，Date本身不是final的，不能保证返回的一定是一个安全的java.util.Date类。

在这种方法中，构造器中不直接接受原对象的引用。而是，对原对象中的数据进行拷贝，使用备份对象作为Period实例的组件。

通过保护性拷贝，避免通过直接改变引用和获取返回值的引用，间接修改Period内的数据。

### 当编写方法和构造器的时候，如果允许客户端提供的对象进入到内部的数据，就需要考虑，客户端的对象是否是可变，它的可变是否会对内部数据产生影响？！！





# 谨慎设计方法签名

1、方法的名称易于理解、与其他的风格一致。

2、一个类当中的方法不要太多，当一项操作被频繁使用，才选择为他提供快捷方式。

3、避免使用参数过长的方法，参数的列表应该小于四个。使用三种方法可以避免参数过长（1.把方法分解成多个方法；2.创建辅助类来保存参数的分组，一般为静态成员；3.结合两种使用Builder模式，尤其适合可选参数，这个在我之前的博客里面有—类的创建）

3、对于参数类型优先使用接口而不是类。就像在方法中不会使用HashMap类作为输入，但是会使用Map接口作为参数。

4、对于Boolean参数，优先使用两个元素的枚举类型。(没看懂。。。下次有机会再挣扎一下)