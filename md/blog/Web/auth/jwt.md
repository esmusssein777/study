### JWT

> JWT是JSON WEB TOKEN的缩写，它是基于 RFC 7519 标准定义的一种可以安全传输的的JSON对象，由于使用了数字签名，所以是可信任和安全的。



#### JWT的组成

- JWT token的格式：header.payload.signature

- header中用于存放签名的生成算法

  ```json
  {"alg": "HS512"}Copy to clipboardErrorCopied
  ```

- payload中用于存放用户名、token的生成时间和过期时间

  ```json
  {"sub":"admin","created":1489079981393,"exp":1489684781}Copy to clipboardErrorCopied
  ```

- signature为以header和payload生成的签名，一旦header和payload被篡改，验证将失败

  ```java
  //secret为加密算法的密钥
  String signature = HMACSHA512(base64UrlEncode(header) + "." +base64UrlEncode(payload),secret)Copy to clipboardErrorCopied
  ```



#### JWT实例

这是一个JWT的字符串

```
eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsImNyZWF0ZWQiOjE1NTY3NzkxMjUzMDksImV4cCI6MTU1NzM4MzkyNX0.d-iki0193X0bBOETf2UN3r3PotNIEAV7mzIxxeI5IxFyzzkOZxS0PGfF_SK6wxCv2K8S0cZjMkv6b5bCqc0VBwCopy to clipboardErrorCopied
```

可以在该网站上获得解析结果：https://jwt.io/ ![img](https://macrozheng.github.io/mall-learning/images/arch_screen_13.png)



#### JWT实现认证和授权的原理

- 用户调用登录接口，登录成功后获取到JWT的token；
- 之后用户每次调用接口都在http的header中添加一个叫Authorization的头，值为JWT的token；
- 后台程序通过对Authorization头中信息的解码及数字签名校验来获取其中的用户信息，从而实现认证和授权。