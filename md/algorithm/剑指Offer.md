## 剑指Offer的题解

[TOC]

### 1. 二维数组中的查找

在一个二维数组中（每个一维数组的长度相同），每一行都按照从左到右递增的顺序排序，每一列都按照从上到下递增的顺序排序。请完成一个函数，输入这样的一个二维数组和一个整数，判断数组中是否含有该整数。

**思路**

从左下角向下和向右前进



### 2. 替换空格

请实现一个函数，将一个字符串中的每个空格替换成“%20”。例如，当字符串为We Are Happy.则经过替换之后的字符串为We%20Are%20Happy。

**思路**

循环出每个空格符，用replace替换成%20

`str.replace(i, i + 1, "%20");`



### 3. 从尾到头打印链表

输入一个链表，按链表值从尾到头的顺序返回一个ArrayList。

**思路**

从头开始将每个node放入list的0位

`list.add(0, listNode.val);`



### 4. 重建二叉树

输入某二叉树的前序遍历和中序遍历的结果，请重建出该二叉树。假设输入的前序遍历和中序遍历的结果中都不含重复的数字。例如输入前序遍历序列{1,2,4,7,3,5,6,8}和中序遍历序列{4,7,2,1,5,3,8,6}，则重建二叉树并返回。

**思路**

将中序遍历放入map，value递增

遍历前序，从0索引也就是1开始创建新的节点，从map中找出1所在的位置3，1的左子树就是0-3中的递归，直到返回root.left，1的右子树就是4-7中的递归。

**关键代码**

```
    /**
     * 构建二叉树
     * @param pre   前序数组
     * @param pl    前序数组左边界
     * @param pr    前序数组右边界
     * @param inl   中序数组的左边界
     * @return
     */
    private TreeNode binaryTree(int[] pre, int pl, int pr, int inl) {
        if (pl > pr) return null;

        TreeNode root = new TreeNode(pre[pl]);//按前序遍历的数组确认当前值
        int inIndex = map.get(root.val);//获取在中序遍历的位置，左边为左子树的中序遍历，右边为右子树的中序遍历
        int leftTreeSize = inIndex - inl;//获取左子树的大小
        root.left = binaryTree(pre, pl + 1, pl + leftTreeSize, inl);//左子树的索引在+1位置，右子树的索引在+leftTreeSize+1的位置
        root.right = binaryTree(pre, pl + leftTreeSize + 1, pr, inIndex + 1);//前序数组分两部分 [pl+1, pl + leftTreeSize]和[pl + leftTreeSize, pr].
        return root;
    }
```



### 5. 用两个栈实现队列

用两个栈来实现一个队列，完成队列的Push和Pop操作。 队列中的元素为int类型。

**思路**

很简单，pop正常的pop()，push()就先放入一个stack中，再倒放进去就行



### 6. 旋转数组的最小数字

把一个数组最开始的若干个元素搬到数组的末尾，我们称之为数组的旋转。 输入一个非减排序的数组的一个旋转，输出旋转数组的最小元素。 例如数组{3,4,5,1,2}为{1,2,3,4,5}的一个旋转，该数组的最小值为1。 NOTE：给出的所有元素都大于0，若数组大小为0，请返回0。

**思路**

二分搜索的变种。

```
        while (left < right) {
            int mid = (left + right) >> 1;
            if (array[mid] > array[right]) left = mid + 1;
            else right = mid;
        }
```



### 7. 斐波那契数列

大家都知道斐波那契数列，现在要求输入一个整数n，请你输出斐波那契数列的第n项（从0开始，第0项为0）。

**思路**
最简单的动态规划问题, f(n) = f(n - 1) + f(n - 2)



### 8. 跳台阶

一只青蛙一次可以跳上1级台阶，也可以跳上2级。求该青蛙跳上一个n级的台阶总共有多少种跳法（先后次序不同算不同的结果）。

**思路**
还是动态规划问题, f(n) = f(n - 1) + f(n - 2)，不同的是 dp[0] = 1 而不是 0



### 9. 变态跳台阶

一只青蛙一次可以跳上1级台阶，也可以跳上2级……它也可以跳上n级。求该青蛙跳上一个n级的台阶总共有多少种跳法。

**思路**

在基本的动态规划上加上一层循环就行，原来是` f(n) = f(n - 1) + f(n - 2)`,现在是一直加到 0

```
        for (int i = 2; i < target + 1; i++) {
            for (int j = 0; j < i; j++) {
                dp[i] += dp[j];
            }
        }
```



### 10. 矩形覆盖

我们可以用2*1的小矩形横着或者竖着去覆盖更大的矩形。请问用n个2*1的小矩形无重叠地覆盖一个2*n的大矩形，总共有多少种方法？

**思路**

这道题很奇妙，不看答案要搞出来得有基本的科学归纳法的素质在。

本质还是斐波那契数列。` f(n) = f(n - 1) + f(n - 2)`，归纳法得出



### 11. 二进制中1的个数

输入一个整数，输出该数二进制表示中1的个数。其中负数用补码表示。

**思路**
理解了就很简单，n 一直和 n - 1 去与运算，直到为 0。

```
        while (n != 0) {
            count++;
            n = n & (n - 1);
        }
```





### 12. 数值的整数次方

给定一个double类型的浮点数base和int类型的整数exponent。求base的exponent次方。

**思路**

首先判断是不是负数，如果是负数的话得是 1/n 

再次的话让exponent / 2，整个再平方算，这样很快，最后判断exponent是不是单数，如果是单数的话还得乘一次 base

```
double pow = Power(base * base, exponent / 2);
```



### 13. 调整数组顺序使奇数位于偶数前面

输入一个整数数组，实现一个函数来调整该数组中数字的顺序，使得所有的奇数位于数组的前半部分，所有的偶数位于数组的后半部分，并保证奇数和奇数，偶数和偶数之间的相对位置不变

**思路**

先将原数组里面的奇数个数算出来，记为 odd

再将原数组复制，遍历复制的数组，在原数组里面改动

遍历到奇数从 索引 0 开始，偶数从 odd 开始。



### 14. 链表中倒数第k个结点

输入一个链表，输出该链表中倒数第k个结点

**思路**

很好的题，一种思路是先让一个点 one 从头走到  k 的位置，再让一个点 two 和 one 一直走，让 one 走到最后面，那么 two 的位置就是倒数第 k 个节点



### 15. 反转链表

输入一个链表，反转链表后，输出新链表的表头

**思路**

一直让下一个节点放到头部，写起来还比较绕，四个步骤

        while (node.next != null) {//put the next to head
            ListNode cur = node.next;
            node.next = node.next.next;
            cur.next = head;
            head = cur;
        }


### 16. 合并两个排序的链表

输入两个单调递增的链表，输出两个链表合成后的链表，当然我们需要合成后的链表满足单调不减规则

**思路**

比较简单，创建一个新的链表，让两个链表的值比较，最后再判断谁的链表不为 null ，直接把链表接到后面去



### 17. 树的子结构

输入两棵二叉树A，B，判断B是不是A的子结构。（ps：我们约定空树不是任意一个树的子结构）

**思路**

需要递归判断，大体就是判断 root1 和 root2 的值是否相等，再判断left 和 right 是否相等

```
    public boolean HasSubtree(TreeNode root1,TreeNode root2) {
        if (root1 == null || root2 == null) return false;
        return isSub(root1, root2) || HasSubtree(root1.left, root2) || HasSubtree(root1.right, root2);
    }
    
    public boolean isSub(TreeNode root1, TreeNode root2) {
        if (root2 == null) return true;
        if (root1 == null) return false;
        if (root1.val != root2.val) return false;
        return isSub(root1.left, root2.left) && isSub(root1.right, root2.right);
    }
```



### 18. 二叉树的镜像

操作给定的二叉树，将其变换为源二叉树的镜像。

**思路**

根节点不变，递归的将左子树变为右子树

```
    public void change(TreeNode root, TreeNode left, TreeNode right) {
        root.left = right;
        root.right = left;
        if (right != null) change(root.left, right.left, right.right);
        if (left != null) change(root.right, left.left, left.right);
    }
```



### 19. 顺时针打印矩阵

输入一个矩阵，按照从外向里以顺时针的顺序依次打印出每一个数字，例如，如果输入如下4 X 4矩阵： 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 则依次打印出数字1,2,3,4,8,12,16,15,14,13,9,5,6,7,11,10.

**思路**

这种题就比较蠢，没有技巧，右、下、左、上的依次输出

        while (r1 <= r2 && c1 <= c2) {//没什么巧妙的方法，左下右上走一圈，再减去相应的量
            for (int c = c1; c <= c2; c++) list.add(matrix[r1][c]);
            for (int r = r1 + 1; r <= r2; r++) list.add(matrix[r][c2]);
            if (r1 < r2 && c1 < c2) {
                for (int c = c2 - 1; c > c1; c--) list.add(matrix[r2][c]);
                for (int r = r2; r > r1; r--) list.add(matrix[r][c1]);
            }
            r1++;
            c1++;
            r2--;
            c2--;
        }


### 20. 包含min函数的栈

定义栈的数据结构，请在该类型中实现一个能够得到栈中所含最小元素的min函数（时间复杂度应为O（1））

**思路**

将最小的保存在属性里面，如果要插入新的最小值，将当前的最小值放到新的最小值后面，这样的话取出来最小值后可以将第二最小值当做最小值。

简单讲就是将第二小的值存起来，防止最小值被取出来后没有最小值



### 21. 栈的压入、弹出序列

输入两个整数序列，第一个序列表示栈的压入顺序，请判断第二个序列是否可能为该栈的弹出顺序。假设压入栈的所有数字均不相等。例如序列1,2,3,4,5是某栈的压入顺序，序列4,5,3,2,1是该压栈序列对应的一个弹出序列，但4,3,5,1,2就不可能是该压栈序列的弹出序列。（注意：这两个序列的长度是相等的）

**思路**

模仿一波栈的操作就行。

用一个Stack来模仿压入和弹出，如果最后不为空，则为 false



### 22. 从上往下打印二叉树

从上往下打印出二叉树的每个节点，同层节点从左至右打印

**思路**

这思路也简单，正常的广度遍历，用queue来保存每一层的节点



### 23. 二叉搜索树的后序遍历序列

输入一个整数数组，判断该数组是不是某二叉搜索树的后序遍历的结果。如果是则输出Yes,否则输出No。假设输入的数组的任意两个数字都互不相同。

**思路**

二叉搜索树的中序遍历会是递增的，如果是后序遍历的话，根节点在最后面

从头开始遍历，小于根节点的节点是左子树，也是个后序遍历

这样的话递归判断就行

```
private boolean verify(int[] seq, int left, int right) {
    if (right - left <= 1) return true;
    int mid = seq[right];
    int min = left;
    while (min < right && seq[min] <= mid) min++;
    for (int i = min; i < right; i++) {
        if (seq[i] < mid) return false;
    }
    return verify(seq, left, min - 1) && verify(seq, min, right - 1);
}
```



### 24. 二叉树中和为某一值的路径

输入一颗二叉树的跟节点和一个整数，打印出二叉树中结点值的和为输入整数的所有路径。路径定义为从树的根结点开始往下一直到叶结点所经过的结点形成一条路径。(注意: 在返回值的list中，数组长度大的数组靠前)

**思路**

关键在于向下遍历的时候 total = total - val，判断 total = 0 的时候是不是最底层的叶子节点。条件都成立的时候将该 list 加入到 lists 中

否则遍历左右节点，每次操作完后记得将 list 的最后一个值取出来

    private void search(TreeNode root, int target, ArrayList<Integer> list) {
        if (root == null) return;
        list.add(root.val);
        target = target - root.val;
        if (root.left == null && root.right == null && target == 0) {
            lists.add(new ArrayList<>(list));
        } else {
            search(root.left, target, list);
            search(root.right, target, list);
        }
        list.remove(list.size() - 1);
    }


### 25. 复杂链表的复制

输入一个复杂链表（每个节点中有节点值，以及两个指针，一个指向下一个节点，另一个特殊指针指向任意一个节点），返回结果为复制后复杂链表的head。（注意，输出结果中请不要返回参数中的节点引用，否则判题程序会直接返回空）

**思路**

这道题有点麻烦，之前连题都没看懂。。

1. 先复制新的节点在每一个原节点的后面
2. 再复制节点的random指针
3. 最后将两个链表拆开来



### 26. 二叉搜索树与双向链表

输入一棵二叉搜索树，将该二叉搜索树转换成一个排序的双向链表。要求不能创建任何新的结点，只能调整树中结点指针的指向



