整个过程

www.cas.client.com 为cas客户端，也就是用户要访问的资源所在，www.cas.server.com 为cas服务端，是单点登录的认证中心。

![](https://user-gold-cdn.xitu.io/2018/8/4/1650313f3bd30829?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

#### ①：首先用户访问www.cas.client.com，cas客户端收到请求判断用户是否登录。判断过程在AuthenticationFilter过滤器中进行。AuthenticationFilter主要判断用户是否登录，未登录则重定向到登录页面。

那么是如何验证用户是否登录过呢？

如果session中包含“*const_cas_assertion*”属性，说明已经登录，跳过此过滤器执行配置的其他过滤器；

如果ticket参数不为空（可能是登陆后跳转回来的），跳过此过滤器，执行TicketValidationFilter 验证ticket；

如果前两个条件都不满足，重定向到cas服务端，返回登录页面进行登录操作。

#### ②：①中发现用户未登录，将浏览器重定向到www.cas.server.com，并携带一个参数service，参数值为①中的请求地址。

#### ③：cas服务端收到请求将登录页面返回给浏览器。

#### ④：用户输入用户名、密码，提交到cas服务端验证。

#### ⑤：cas服务端验证用户名、密码有效。

当cas服务端验证用户名、密码有效后，将浏览器重定向回①中service值对应的url并携带一个ticket参数，同时会在Cookie中设置一个CASTGC，该cookie是网站www.cas.server.com的cookie，只有访问这个网站才会携带这个cookie过去。

Cookie中的CASTGC：向cookie中添加该值的目的是当下次访问www.cas.server.com时，浏览器将Cookie中的TGC携带到服务器，服务器根据这个TGC，查找与之对应的TGT。从而判断用户是否登录过了，是否需要展示登录页面。TGT与TGC的关系就像SESSION与Cookie中SESSIONID的关系。

TGT：Ticket Granted Ticket（俗称大令牌，或者说票根，他可以签发ST）。

TGC：Ticket Granted Cookie（cookie中的value），存在Cookie中，根据他可以找到TGT。

ST：Service Ticket （小令牌），是TGT生成的，默认是用一次就生效了。也就是上面的ticket值。

#### ⑥：www.cas.client.com取得ticket后进入TicketValidationFilter过滤器，该过滤器主要验证ticket是否有效。

#### ⑦：www.cas.server.com接收到ticket之后，验证，验证通过返回结果告诉www.cas.client.com该ticket有效。

#### ⑧: www.cas.client.com将请求的资源返回给浏览器。

