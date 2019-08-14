一路的坑，被别人的博客坑惨了。。
还是自己写一篇博客记录一下，帮助自己也帮助他人

我们首先的输入 
`df -h`
出现下面的情况
![](https://imgconvert.csdnimg.cn/aHR0cDovL3d3MS5zaW5haW1nLmNuL2xhcmdlLzAwNU85SU9KZ3kxZzJ0dmg2aWV3bWozMGR0MDQxZ2xsLmpwZw)
我们看到`cl-root`这个文件系统已经 100% 的使用率了，我们需要扩容

### 在VMware中设置一个新的磁盘
需要先关闭一下虚拟机才能添加磁盘
如下图所示，添加一个磁盘，然后启动虚拟机
![](https://imgconvert.csdnimg.cn/aHR0cDovL3d3MS5zaW5haW1nLmNuL2xhcmdlLzAwNU85SU9KZ3kxZzJ0dmV4b2ExZGozMGttMGlyZGdwLmpwZw)
### 查看磁盘信息
我们查看一下我们新加的磁盘加进来了没有
输入`fdisk -l`，看到有` /dev/sdb `这个新的磁盘，正是我们加的20g大小的磁盘，所以已经扫描到了。我们可以开始给他分区
![](https://imgconvert.csdnimg.cn/aHR0cDovL3d3MS5zaW5haW1nLmNuL2xhcmdlLzAwNU85SU9KZ3kxZzJ0dmpqd3ZwOGozMGZlMGNpMHRpLmpwZw)

### 分区
我们输入
`fdisk /dev/sdb`
出现下面的情况，输入 m 获取命令的帮助， 输入 p 打印分区表的信息
![](https://imgconvert.csdnimg.cn/aHR0cDovL3d3MS5zaW5haW1nLmNuL2xhcmdlLzAwNU85SU9KZ3kxZzJ0dnA3ejBrdGozMGg1MGRwZ21iLmpwZw)
我们看到信息后开始真正的创建分区
按照下面的步骤输入 
`n `（创建）
`p`（选择）
选择默认的参数（一直回车选择默认）
`t`（选择id）
`8e`（磁盘类型）
`w`（保存，一定不要忘记）
![](https://imgconvert.csdnimg.cn/aHR0cDovL3d3MS5zaW5haW1nLmNuL2xhcmdlLzAwNU85SU9KZ3kxZzJ0dnNjYnh5bmozMGZ1MGExMHRjLmpwZw)

### 创建物理磁盘pv
`pvcreate /dev/sdb1`

显示创建的磁盘
`pvdisplay`
![](https://imgconvert.csdnimg.cn/aHR0cDovL3d3MS5zaW5haW1nLmNuL2xhcmdlLzAwNU85SU9KZ3kxZzJ0d2R0anJwcmozMGZnMGE1anJvLmpwZw)
可以看到已经可以分配了
`vgextend cl /dev/sdb1`
这里要注意两个问题
1. `cl` 指的是我自己这里的VG的名字，要和自己保持一致，如果你vg的name是centos就改成centos
2. 如果你已经没有可用的空间了，完全是100%爆满的情况，在输入命令前删掉一点文件，否则没有办法创建临时的文件

输入`lvextend –L +19.9G /dev/mapper/cl-root`，来扩展大小，这里不输入20G是怕他真实没有那么大

![](https://imgconvert.csdnimg.cn/aHR0cDovL3d3MS5zaW5haW1nLmNuL2xhcmdlLzAwNU85SU9KZ3kxZzJ0d2pqeDcxZWozMGxkMDN5Z2xxLmpwZw)
### 在线调整xfs格式文件系统大小
`xfs_growfs /dev/ mapper/cl-root`
![](https://imgconvert.csdnimg.cn/aHR0cDovL3d3MS5zaW5haW1nLmNuL2xhcmdlLzAwNU85SU9KZ3kxZzJ0d2t4Zng3cmozMGh0MDhsZ200LmpwZw)
最后我们发现容量已经扩容成功，又可以继续玩耍了。