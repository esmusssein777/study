## Spring Security 的使用

下面首先来看一下Spring Security 的基本配置

![](https://mrbird.cc/img/QQ%E6%88%AA%E5%9B%BE20180707111356.png)

如上图所示，Spring Security包含了众多的过滤器，这些过滤器形成了一条链，所有请求都必须通过这些过滤器后才能成功访问到资源。其中`UsernamePasswordAuthenticationFilter`过滤器用于处理基于表单方式的登录认证，而`BasicAuthenticationFilter`用于处理基于HTTP Basic方式的登录验证，后面还可能包含一系列别的过滤器（可以通过相应配置开启）。在过滤器链的末尾是一个名为`FilterSecurityInterceptor`的拦截器，用于判断当前请求身份认证是否成功，是否有相应的权限，当身份认证失败或者权限不足的时候便会抛出相应的异常。`ExceptionTranslateFilter`捕获并处理，所以我们在`ExceptionTranslateFilter`过滤器用于处理了`FilterSecurityInterceptor`抛出的异常并进行处理，比如需要身份认证时将请求重定向到相应的认证页面，当认证失败或者权限不足时返回相应的提示信息。



### 自定义认证过程

默认用户密码由Spring Security生成，Spring Security支持我们自定义认证的过程，如处理用户信息获取逻辑，使用我们自定义的登录页面替换Spring Security默认的登录页及自定义登录成功或失败后的处理逻辑等。

自定义认证的过程需要实现Spring Security提供的`UserDetailService`接口，该接口只有一个抽象方法`loadUserByUsername`，源码如下：

```
public interface UserDetailsService {
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
}
```

`loadUserByUsername`方法返回一个`UserDetail`对象，该对象也是一个接口，包含一些用于描述用户信息的方法，源码如下：

```
public interface UserDetails extends Serializable {

    Collection<? extends GrantedAuthority> getAuthorities();

    String getPassword();

    String getUsername();

    boolean isAccountNonExpired();

    boolean isAccountNonLocked();

    boolean isCredentialsNonExpired();

    boolean isEnabled();
}
```



这些方法的含义如下：

- `getAuthorities`获取用户包含的权限，返回权限集合，权限是一个继承了`GrantedAuthority`的对象；
- `getPassword`和`getUsername`用于获取密码和用户名；
- `isAccountNonExpired`方法返回boolean类型，用于判断账户是否未过期，未过期返回true反之返回false；
- `isAccountNonLocked`方法用于判断账户是否未锁定；
- `isCredentialsNonExpired`用于判断用户凭证是否没过期，即密码是否未过期；
- `isEnabled`方法用于判断用户是否可用。

实际中我们可以自定义`UserDetails`接口的实现类，也可以直接使用Spring Security提供的`UserDetails`接口实现类`org.springframework.security.core.userdetails.Use`





