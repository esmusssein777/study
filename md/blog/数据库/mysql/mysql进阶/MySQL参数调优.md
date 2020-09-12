# MySQL参数调优

> innodb_flush_log_at_trx_commit 

redo log 用于保证 crash-safe 能力。innodb_flush_log_at_trx_commit 这个参数设置成 1 的时候，表示每次事务的 redo log 都直接持久化到磁盘。这个参数我建议你设置成 1， 这样可以保证 MySQL 异常重启之后数据不丢失。

>  sync_binlog

sync_binlog 这个参数设置成 1 的时候，表示每次事务的 binlog 都持久化到磁盘。这个 参数我也建议你设置成 1，这样可以保证 MySQL 异常重启之后 binlog 不丢失。



> innodb_io_capacity

 innodb_io_capacity 这个参数了，它会告诉 InnoDB 你的磁盘能力。innodb_io_capacity 的值如果设置的太小。 InnoDB 认为这个系统的能力就这么差，所以刷脏页刷得特别慢，甚至比脏页生成的速度 还慢，这样就造成了脏页累积，影响了查询和更新性能。

> innodb_max_dirty_pages_pct

参数 innodb_max_dirty_pages_pct 是脏页比例上限，默认值是 75%。InnoDB 会在后台刷脏页，而刷脏页的过程是要将内存页写入磁盘。所 以，无论是你的查询语句在需要内存的时候可能要求淘汰一个脏页，还是由于刷脏页的逻辑会占用 IO 资源并可能影响到了你的更新语句。

要尽量避免这种情况，你就要合理地设置 innodb_io_capacity 的值，并且平时要多关注 脏页比例，不要让它经常接近 75%。

其中，脏页比例是通过 Innodb_buffer_pool_pages_dirty/Innodb_buffer_pool_pages_total 得到的



> innodb_file_per_table

表数据既可以存在共享表空间里，也可以是单独的文件。这个行为是由参数 innodb_file_per_table 控制的:

1. 这个参数设置为 OFF 表示的是，表的数据放在系统共享表空间，也就是跟数据字典放 在一起;

2. 这个参数设置为 ON 表示的是，每个 InnoDB 表数据存储在一个以 .ibd 为后缀的文件 中。

从 MySQL 5.6.6 版本开始，它的默认值就是 ON 了。

我建议你不论使用 MySQL 的哪个版本，都将这个值设置为 ON。因为，一个表单独存储 为一个文件更容易管理，而且在你不需要这个表的时候，通过 drop table 命令，系统就会 直接删除这个文件。而如果是放在共享表空间中，即使表删掉了，空间也是不会回收的。

