## shiro的教程

[TOC]

### 认证

认证在shiro中被称为Authentication，用来验证用户是不是有相应的身份，也可以理解为校验登陆凭证的过程

下面的代码大致解释一下，摘自[w3c](<https://www.w3cschool.cn/shiro/co4m1if2.html>)

**Subject**：主体，代表了当前 “用户”，这个用户不一定是一个具体的人，与当前应用交互的任何东西都是 Subject，如网络爬虫，机器人等；即一个抽象概念；所有 Subject 都绑定到 SecurityManager，与 Subject 的所有交互都会委托给 SecurityManager；可以把 Subject 认为是一个门面；SecurityManager 才是实际的执行者；

**SecurityManager**：安全管理器；即所有与安全有关的操作都会与 SecurityManager 交互；且它管理着所有 Subject；可以看出它是 Shiro 的核心，它负责与后边介绍的其他组件进行交互，如果学习过 SpringMVC，你可以把它看成 DispatcherServlet 前端控制器；

**Realm**：域，Shiro 从从 Realm 获取安全数据（如用户、角色、权限），就是说 SecurityManager 要验证用户身份，那么它需要从 Realm 获取相应的用户进行比较以确定用户身份是否合法；也需要从 Realm 得到用户相应的角色 / 权限进行验证用户是否能进行操作；可以把 Realm 看成 DataSource，即安全数据源。

也就是说对于我们而言，最简单的一个 Shiro 应用：

1. 应用代码通过 Subject 来进行认证和授权，而 Subject 又委托给 SecurityManager；
2. 我们需要给 Shiro 的 SecurityManager 注入 Realm，从而让 SecurityManager 能得到合法的用户及其权限进行判断。

```
// 创建一个 Realm
private SimpleAccountRealm simpleAccountRealm = new SimpleAccountRealm();

//为 Realm 添加一个账户
simpleAccountRealm.addAccount("ligz", "123456");

// 构建 SecurityManager 环境
DefaultSecurityManager defaultSecurityManager = new DefaultSecurityManager();

// 为 SecurityManager 设置 Realm
defaultSecurityManager.setRealm(simpleAccountRealm);

// 将 SecurityManager 放入 SecurityUtils 这个工具类中
SecurityUtils.setSecurityManager(defaultSecurityManager);

// 获取一个 Subject
Subject subject = SecurityUtils.getSubject();

// 创建一个账号密码, 在 web 应用中一般为表单上填写并传入后台.
UsernamePasswordToken token = new UsernamePasswordToken("ligz", "123456");

// 进行登陆操作
subject.login(token);

// 验证是否为登陆状态
System.out.println("是否登陆: " + subject.isAuthenticated());
```

如果登陆成功，输出 `true`

否则失败的异常

```
UnknownAccountException             # 未知账户/没找到帐号
IncorrectCredentialsException       # 错误的凭证(密码)异常
```

### 授权

授权在shiro中是Authorization，用来验证用户是否具有某个角色或者权限

整个授权的流程如下

```
// 创建一个 Realm
private SimpleAccountRealm simpleAccountRealm = new SimpleAccountRealm();

//为 Realm 添加一个账户, 并赋予 admin 角色
simpleAccountRealm.addAccount("ligz", "123456", "admin");

// 构建 SecurityManager 环境
DefaultSecurityManager defaultSecurityManager = new DefaultSecurityManager();

// 为 SecurityManager 设置 Realm
defaultSecurityManager.setRealm(simpleAccountRealm);

// 将 SecurityManager 放入 SecurityUtils 这个工具类中
SecurityUtils.setSecurityManager(defaultSecurityManager);

// 获取一个 Subject
Subject subject = SecurityUtils.getSubject();

// 创建一个账号密码
UsernamePasswordToken token = new UsernamePasswordToken("ligz", "123456");

// 进行登陆操作
subject.login(token);

// 验证是否为登陆状态
System.out.println("是否登陆: " + subject.isAuthenticated());

// 验证是否具备某个角色
System.out.println("是否具备admin角色: " + subject.hasRole("admin"));
```

这样我们验证的时候发现我们是这个 admin 的角色，返回`true`，否则返回`false`

如果是`subject.checkRole("admin")`，这个没有返回值，发如果没有这个角色，抛出异常`UnauthorizedException`

### Realm

realm我们上面有详细的解释，简单的解释一下就是

Realm：存储了用户的身份、角色和权限

这里主要有两种方式

##### IniRealm

IniRealm 顾名思义，即通过读取 `.ini` 文件来获取用户，角色，权限信息。

配置用户名/密码及其角色, 格式: “用户名=密码，角色1，角色2”

配置角色及权限之间的关系, 格式: “角色=权限1, 权限2”

```
[users]
ligz = 123456, admin, user
wang = 123456, user

[roles]
admin = user:delete
user = user:select
```

##### JdbcRealm

JdbcRelam 顾名思义，即通过通过访问数据库来获取用户，角色，权限信息

```
/**
 * Shiro JdbcRealm 测试
 */
public class JdbcRealmTest {
    private DruidDataSource dataSource = new DruidDataSource();

    /**
     * 初始化 DataSource
     */
    @Before
    public void before() {
        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://172.16.2.163:3306/shiro");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
    }

    @Test
    public void testJdbcRealm() {
        DefaultSecurityManager defaultSecurityManager = new DefaultSecurityManager();

        // 构建 JdbcRelam
        JdbcRealm jdbcRealm = new JdbcRealm();
        // 为 JdbcRelam 设置数据源
        jdbcRealm.setDataSource(dataSource);
        // 设置启用权限查询, 默认为 false
        jdbcRealm.setPermissionsLookupEnabled(true);

        defaultSecurityManager.setRealm(jdbcRealm);
        SecurityUtils.setSecurityManager(defaultSecurityManager);

        Subject subject = SecurityUtils.getSubject();

        UsernamePasswordToken token = new UsernamePasswordToken("ligz", "123456");

        try {
            subject.login(token);
        } catch (AuthenticationException e) {
            e.printStackTrace();
            System.out.println("登陆失败");
        }

        System.out.println("--------------------认证--------------------");
        System.out.println("是否具备 admin 权限: " + subject.hasRole("admin"));
        System.out.println("是否具备 user 权限: " + subject.hasRole("user"));
        System.out.println("是否同时具备 admin 和 user 权限: " + subject.hasAllRoles(Arrays.asList("admin", "user")));
        System.out.println("--------------------授权--------------------");
        System.out.println("是否具备 user:delete 权限" + subject.isPermitted("user:delete"));
        System.out.println("是否具备 user:select 权限" + subject.isPermitted("user:select"));
        System.out.println("是否同时具备 user:delete 和 user:select 权限" + subject.isPermittedAll("user:delete", "user:select"));
    }
}
```

这里需要注意的是，我们建立的表需要和JdbcRealm里面的查询条件一致，否则的话我们需要修改sql语句来覆盖原有的查询语句

源码里面的sql语句是

```
protected static final String DEFAULT_AUTHENTICATION_QUERY = "select password from users where username = ?";
protected static final String DEFAULT_SALTED_AUTHENTICATION_QUERY = "select password, password_salt from users where username = ?";
protected static final String DEFAULT_USER_ROLES_QUERY = "select role_name from user_roles where username = ?";
protected static final String DEFAULT_PERMISSIONS_QUERY = "select permission from roles_permissions where role_name = ?";
```

这样的话我们需要建立的表是 `users`, `user_role`, `roles_permission`

如果自己的表名和sql语句不一致，我们修改如下

```
jdbcRealm.setAuthenticationQuery(String authenticationQuery);
jdbcRealm.setPermissionsQuery(String permissionsQuery);
jdbcRealm.setUserRolesQuery(String userRolesQuery);
```

##### 重写Realm

在真实开发时，我们常常需要自己写一些自己的功能，比如说常见的账号多次登陆我们需要判断是不是机器人在暴力破解密码，我们需要限制他的登陆次数

我们创建一个MyRealm来继承`AuthorizingRealm`，实现抽象方法

```
public class MyRealm extends AuthorizingRealm {


    /**
     * 根据用户凭证查询所用拥有的角色和权限
     * @param principalCollection 用户凭证
     * @return 返回授权信息，包含所拥有的角色和权限
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        String username = (String)principalCollection.getPrimaryPrincipal();

        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();

        // 根据用户名其所拥有的角色和权限
        Set<String> roles = selectRolesByUserName(username);
        Set<String> permissions = selectPermissionsByUserName(username);

        authorizationInfo.setRoles(roles);
        authorizationInfo.setStringPermissions(permissions);
        return authorizationInfo;
    }
    
    private Set<String> selectPermissionsByUserName(String username) {
        HashSet<String> permissions = new HashSet<>();
        // 假设只有 ligz 这个用户具备 select 权限
        if ("ligz".equals(username)) {
            permissions.add("select");
        }
        return permissions;
    }

    private Set<String> selectRolesByUserName(String username) {
        HashSet<String> roles = new HashSet<>();

        // 假设只有 ligz 这个用户具备 user 角色
        if ("ligz".equals(username)) {
            roles.add("user");
        }
        return roles;
    }

    private User selectUserByUserName(String username) {
        User user = null;

        // 假设当前只有 ligz - 123465 这个账户.
        if ("ligz".equals(username)) {
            user = new User("ligz", "123456");
        }
        return user;
    }


    /**
     * 根据用户提交的凭证查询是否具有这个用户 (这里不判断密码是否正确)
     * @param authenticationToken 用户凭证 (账户密码)
     * @return 相应的用户信息
     * @throws AuthenticationException 当用户不存在或具备其他状态, 如被锁定, 等状态会抛出相应的异常
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {

        // 这个方法也可以使用 DAO 层的方法来查询数据库，返回 user 对象。
        User user = selectUserByUserName((String) authenticationToken.getPrincipal());

        if (user == null) {
            throw new UnknownAccountException("账号不存在");
        }

        return new SimpleAuthenticationInfo(user.getUsername(), user.getPassword(), super.getName());
    }
}
```

`doGetAuthorizationInfo`获取用户授权信息，授权信息包括所拥有的角色和权限信息

`doGetAuthenticationInfo`获取用户认证信息。如果我们需要限制次数的话，我们在获取用户认证信息 `doGetAuthenticationInfo` 方法返回认证的信息前判断用户是否存在、被冻结、登陆超过次数等信息抛出异常或者返回失败

##### 简单的加密

明文的密码在数据库是会被唾弃的。。

所以我们需要将密码进行加密，常见的加密算法有MD5等，但是我们也会发现常见的加密手段在网上都会有反解密，我们应对的策略是用`slat`来加密

比如密码为`123456`，`slat`为`shiro`

```
    String password = "123456";
    String slat = "shiro";
    Md5Hash md5Hash = new Md5Hash(password, ByteSource.Util.bytes(slat));
```

底层的算法可以自己去查一下，这里就不再详细将了

我们在验证密码的时候，也需要加盐

```
// 告诉 Relam, 校验密码时需要加的盐.
ByteSource slat = ByteSource.Util.bytes("shiro");
return new SimpleAuthenticationInfo(user.getUsername(), user.getPassword(), slat, super.getName());
```

我们创建一个密码匹配器，使用的是MD5算法

```
        DefaultSecurityManager securityManager = new DefaultSecurityManager();

        // 创建 Relam
        MyCustomRealm realm = new MyCustomRealm();

        // 创建密码匹配器
        HashedCredentialsMatcher credentialsMatcher = new HashedCredentialsMatcher("md5");

        // 告诉 Realm 密码匹配方式
        realm.setCredentialsMatcher(credentialsMatcher);

        securityManager.setRealm(realm);

        SecurityUtils.setSecurityManager(securityManager);

        Subject subject = SecurityUtils.getSubject();
        UsernamePasswordToken token = new UsernamePasswordToken("zhao", "123456");

        try {
            subject.login(token);
        } catch (AuthenticationException e) {
            System.out.println("登陆失败");
            e.printStackTrace();
        }
        System.out.println("当前登陆状态: " + subject.isAuthenticated());
    }
```

### 总结

最后我们还是整个来看一下shiro框架，这里还是借用w3c的解释

![shiro框架](https://7n.w3cschool.cn/attachments/image/wk/shiro/3.png)



**Subject**：主体，可以看到主体可以是任何可以与应用交互的 “用户”；

**SecurityManager**：相当于 SpringMVC 中的 DispatcherServlet 或者 Struts2 中的 FilterDispatcher；是 Shiro 的心脏；所有具体的交互都通过 SecurityManager 进行控制；它管理着所有 Subject、且负责进行认证和授权、及会话、缓存的管理。

**Authenticator**：认证器，负责主体认证的，这是一个扩展点，如果用户觉得 Shiro 默认的不好，可以自定义实现；其需要认证策略（Authentication Strategy），即什么情况下算用户认证通过了；

**Authrizer**：授权器，或者访问控制器，用来决定主体是否有权限进行相应的操作；即控制着用户能访问应用中的哪些功能；

**Realm**：可以有 1 个或多个 Realm，可以认为是安全实体数据源，即用于获取安全实体的；可以是 JDBC 实现，也可以是 LDAP 实现，或者内存实现等等；由用户提供；注意：Shiro 不知道你的用户 / 权限存储在哪及以何种格式存储；所以我们一般在应用中都需要实现自己的 Realm；

**SessionManager**：如果写过 Servlet 就应该知道 Session 的概念，Session 呢需要有人去管理它的生命周期，这个组件就是 SessionManager；而 Shiro 并不仅仅可以用在 Web 环境，也可以用在如普通的 JavaSE 环境、EJB 等环境；所有呢，Shiro 就抽象了一个自己的 Session 来管理主体与应用之间交互的数据；这样的话，比如我们在 Web 环境用，刚开始是一台 Web 服务器；接着又上了台 EJB 服务器；这时想把两台服务器的会话数据放到一个地方，这个时候就可以实现自己的分布式会话（如把数据放到 Memcached 服务器）；

**SessionDAO**：DAO 大家都用过，数据访问对象，用于会话的 CRUD，比如我们想把 Session 保存到数据库，那么可以实现自己的 SessionDAO，通过如 JDBC 写到数据库；比如想把 Session 放到 Memcached 中，可以实现自己的 Memcached SessionDAO；另外 SessionDAO 中可以使用 Cache 进行缓存，以提高性能；

**CacheManager**：缓存控制器，来管理如用户、角色、权限等的缓存的；因为这些数据基本上很少去改变，放到缓存中后可以提高访问的性能

**Cryptography**：密码模块，Shiro 提高了一些常见的加密组件用于如密码加密 / 解密的