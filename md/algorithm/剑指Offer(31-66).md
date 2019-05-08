

## 剑指Offer(31-66)

[TOC]

### 31. 整数中1出现的次数（从1到n整数中1出现的次数）**

求出1~13的整数中1出现的次数,并算出100~1300的整数中1出现的次数？为此他特别数了一下1~13中包含1的数字有1、10、11、12、13因此共出现6次,但是对于后面问题他就没辙了。ACMer希望你们帮帮他,并把问题更加普遍化,可以很快的求出任意非负整数区间中1出现的次数（从1 到 n 中1出现的次数）

**思路**

我画张表如下：

当n = 3141592时:

| m    | a       | b    | ones                              |
| ---- | ------- | ---- | --------------------------------- |
| 1    | 3141592 | 0    | (3141592+8)/10*1+0=314160         |
| 10   | 314159  | 2    | (314159+8)/10*10+0=314160         |
| 100  | 31415   | 92   | (31415+8)/10*100+0=314200         |
| 1000 | 3141    | 592  | (3141+8)/10*1000+1*(592+1)=314593 |

当然后面还有m=10000,100000,1000000三种情况，对应着万位，十万位， 百万位为1时的情况

下面说下a+8的意义：

当考虑个位，十位，百位这三位为1的情况时：

个位 2 ，当个位取值1时，前面的六位数字可由0~314159组成，即314160种情况

十位9，当十位取值1时，前面的五位数字可由0~31415组成，十位之后的一位可由0~9组成，组合情况31416*10=314160种情况

百位5，当百位取值为1时，前面的四位数字可由0~3141组成，百位之后的两位可由0~99组成，组合情况为3142*100=314200种情况

------

注意：当考虑千位1时：

千位1，千位取值即1，前面的三位数字可由0~314组成，但是当前面的值为314时，后面的三位只有0~592种情况(特殊情况)，其余的情况即为前面的值为0~313,后面三位有0~999,情况数为314*1000，所以总情况数为314*1000   + 593=314593种情况

这时可发现和代码中的公式算的情况是吻合的，a+8的巧妙之处在于当a的最后一位(当前分析位)为0或1时，加8不产生进位，这是为需要单独算的特殊情况做准备，而当前分析位为2~9时，不需要考虑特殊情况，所以允许加8产生的进位。

```
    public int NumberOf1Between1AndN_Solution(int n) {
        int res = 0;
        for (int i = 1; i <= n; i *= 10) {
            int a = n / i, b = n % i;
            res += (a + 8) / 10 * i + (a % 10 == 1 ? b + 1 : 0);
        }
        return res;
    }
```



### 32. 把数组排成最小的数*

输入一个正整数数组，把数组里所有数字拼接起来排成一个数，打印能拼接出的所有数字中最小的一个。例如输入数组{3，32，321}，则打印出这三个数字能排成的最小数字为321323。

**思路**

这道题还是比较的灵活，想不到想不到

先将 int 转成 String，再通过比较规则来比较谁大，大的放后面

通过String的比较器来判断谁小

https://www.nowcoder.com/questionTerminal/8fecd3f8ba334add803bf2a06af1b993

来源：牛客网

```
 * 先将整型数组转换成String数组，然后将String数组排序，最后将排好序的字符串数组拼接出来。关键就是制定排序规则。
 * 排序规则如下：
 * 若ab > ba 则 a > b，
 * 若ab < ba 则 a < b，
 * 若ab = ba 则 a = b；
 * 解释说明：
 * 比如 "3"< "31"但是 "331">"313"，所以要将二者拼接起来进行比较
```

`Arrays.sort(nums, (n1, n2) -> (n1 + n2).compareTo(n2 + n1));//升序，所以是最小的`



### 33. 丑数*

把只包含质因子2、3和5的数称作丑数（Ugly Number）。例如6、8都是丑数，但14不是，因为它包含质因子7。 习惯上我们把1当做是第一个丑数。求按从小到大的顺序的第N个丑数

**思路**

用动态规划的方式很容易理解，不过我没想出来。。。

每次都获取最小的丑数，思想很简单，代码不容易写出来

    public int GetUglyNumber_Solution(int index) {
        if (index <= 6) return index;
        int i2 = 0, i3 = 0, i5 = 0;
        int[] dp = new int[index];
        dp[0] = 1;
        for (int i = 1; i < index; i++) {
            int next2 = dp[i2] * 2;
            int next3 = dp[i3] * 3;
            int next5 = dp[i5] * 5;
            dp[i] = Math.min(next2, Math.min(next3, next5));
            if (dp[i] == next2) i2++;
            if (dp[i] == next3) i3++;
            if (dp[i] == next5) i5++;
        }
        return dp[index - 1];
    }


### 34. 第一个只出现一次的字符*

在一个字符串(0<=字符串长度<=10000，全部由字母组成)中找到第一个只出现一次的字符,并返回它的位置, 如果没有则返回 -1（需要区分大小写）.

**思路**

空间换时间，用数组来保存出现的次数，用队列来保存顺序

很难想到，但是很简单，在数组中记录下字符出现的次数，在将前面出现大于一的给poll掉就OK了

```
public int FirstNotRepeatingChar(String str) {
    int[] chars = new int[256];
    char[] c = str.toCharArray();
    Queue<Character> queue = new LinkedList<>();
    for (char s : c) {
        chars[s]++;
        queue.offer(s);
    }
    //当第一个有重复时，poll直到第一个不重复
    while (!queue.isEmpty() && chars[queue.peek()] > 1) queue.poll();
    if (queue.isEmpty()) return -1;
    return str.indexOf(queue.peek());
}
```



### 35. 数组中的逆序对**

在数组中的两个数字，如果前面一个数字大于后面的数字，则这两个数字组成一个逆序对。输入一个数组,求出这个数组中的逆序对的总数P。并将P对1000000007取模的结果输出。 即输出P%1000000007

**思路**

真的麻烦，归并思想



### 36. 两个链表的第一个公共结点

输入两个链表，找出它们的第一个公共结点

**思路**

简单，一个链表走到尾了就从另一个链表的头开始走，这样相交的点就是公共节点

            node1 = node1 == null ? pHead2 : node1.next;
            node2 = node2 == null ? pHead1 : node2.next;


### 37. 数字在排序数组中出现的次数*

统计一个数字在排序数组中出现的次数

**思路**

用两次二叉搜索，找出最左和最右的位置，相减。

需要对二叉搜索很了解，找出 k 的最左位置和 k+1的最左位置

注意 right = array.length 而不是 length - 1

    private int binary(int[] array, int k) {
        int left = 0, right = array.length;
        while (left < right) {
            int mid = (left + right) >> 1;
            if (array[mid] >= k) right = mid;
            else left = mid + 1;
        }
        return left;
    }


### 38. 二叉树的深度

输入一棵二叉树，求该树的深度。从根结点到叶结点依次经过的结点（含根、叶结点）形成树的一条路径，最长路径的长度为树的深度

**思路**

将深度当作参数就行

    private void depth(TreeNode root, int step) {
        if (root != null) {
            depth(root.left, step + 1);
            depth(root.right, step + 1);
        } else {
            res = Math.max(res, step);
        }
    }


### 39. 平衡二叉树

输入一棵二叉树，判断该二叉树是否是平衡二叉树

**思路**

判断左右的高度差是不是大于1，大于就不是平衡二叉树

其实这种递归的解法，第一步一定不要忘记判断什么时候终止

    private int balance(TreeNode root) {
        if (root == null || !flag) return 0;
        int left = balance(root.left);
        int right = balance(root.right);
        int abs = Math.abs(left - right);
        if (abs > 1) flag = false;
        return 1 + Math.max(left, right);
    }


### 40. 数组中只出现一次的数字

