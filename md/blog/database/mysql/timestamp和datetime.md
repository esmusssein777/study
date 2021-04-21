# 选择合适的 MySQL 日期时间类型来存储你的时间

构建数据库写程序避免不了使用日期和时间，对于数据库来说，有多种日期时间字段可供选择，如 timestamp 和 datetime 以及使用 int 来存储 unix timestamp。

不仅新手，包括一些有经验的程序员还是比较迷茫，究竟我该用哪种类型来存储日期时间呢？

那我们就一步一步来分析他们的特点，这样我们根据自己的需求选择合适的字段类型来存储 (优点和缺点是比较出来的  , 跟父母从小喜欢拿邻居小孩子跟自己比一样的)

## datetime 和 timestamp
1. datetime 更像日历上面的时间和你手表的时间的结合，就是指具体某个时间。

2. timestamp 更适合来记录时间，比如我在东八区时间现在是 2016-08-02 10:35:52， 你在日本（东九区此时时间为 2016-08-02 11:35:52），我和你在聊天，数据库记录了时间，取出来之后，对于我来说时间是 2016-08-02 10:35:52，对于日本的你来说就是 2016-08-02 11:35:52。所以就不用考虑时区的计算了。

3. 时间范围是 timestamp 硬伤（1970-2038），datetime （1000-9999）

## timestamp 和 UNIX timestamp
1. 显示直观，出问题了便于排错，比好多很长的 int 数字好看多了

2. int 是从 1970 年开始累加的，但是 int 支持的范围是 1901-12-13 到 2038-01-19 03:14:07，如果需要更大的范围需要设置为 bigInt。但是这个时间不包含毫秒，如果需要毫秒，还需要定义为浮点数。datetime 和 timestamp 原生自带 6 位的微秒。

3. timestamp 是自带时区转换的，同上面的第 2 项。

4. 用户前端输入的时间一般都是日期类型，如果存储 int 还需要存前取后处理

## 项目统一时间

**UTC时间是什么**

```
协调世界时，又称世界统一时间、世界标准时间、国际协调时间。
由于英文（CUT）和法文（TUC）的缩写不同，作为妥协，简称UTC。

    UTC +时区差＝本地时间

```

**中国时间**

```
中国大陆、中国香港、中国澳门、中国台湾、蒙古国、新加坡、
马来西亚、菲律宾、西澳大利亚州的时间与UTC的时差均为+8，也就是UTC+8。

```

1.**系统采用多时区设计的时候，往往我们需要统一时区，需要统一的地方如下：**

```
        服务器（Tomcat服务）
        数据库（JPA + Hibernate）
        前端数据（前端采用Vuejs）
        
    思路为：
        将数据库和服务器的时间都采用标准时区UTC存储处理。
        前端拿到标准时区的数据，统一根据用户所在时区进行转换。
        这样保证了后端数据时区的一致性，前端根据实际情况进行渲染。

```

2.**保证服务器时区为UTC**

```
    服务启动的时候，将当前时区设置为UTC，代码如下：

    @SpringBootApplication
    public class Application {
      @PostConstruct
      void started() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

```

3.**保证数据库时区为UTC**

```
    Hibernate支持设置时区，在Springboot中增加配置如下：
        spring.jpa.properties.hibernate.jdbc.time_zone = UTC
    如果是MySQL数据库，连接池链接后面增加配置如下：
        ?serverTimezone=TimeZone&useLegacyDatetimeCode=false
    如：
        spring.datasource.url=jdbc:mysql://localhost:3306/db?useUnicode=true&characterEncoding=utf-8&useLegacyDatetimeCode=false&serverTimezone=UTC

其中useLegacyDatetimeCode参数默认是true，我们需要手动设置为false，否则无效。
```

