## RBAC的权限管理

[TOC]

#### 前言

在用shiro或者spring security时总是会很迷惑底层到底是怎么实现的，所以这次不用任何的权限框架实现RBAC(Role-Based Access Control基于角色的权限访问控制)。

为了更好的理解，没有使用springboot，这里是代码的地址，用mvn clean package打成war包放到tomcat里面跑就行

代码的地址[<https://github.com/esmusssein777/springbootlearning/tree/master/permission/rbac>](<https://github.com/esmusssein777/springbootlearning/tree/master/permission/rbac>)

#### 框架

框架用的是spring+mybatis+mysql+tomcat，这样的一套还是比较好的能理解整个的体系

#### 数据库设计

数据库是我们常见的表，user(用户)，role(角色)，permission(权限)，user_role(用户角色关系表)，role_permission(角色权限关系表)。这样的表是非常常见的权限管理表，每个用户可以对应不同的角色，也可以有多个角色，每个角色又对应不同的权限，一个角色可以有多个权限。

用户角色关系表、角色权限关系表都是多对多的表

![1555319817559](C:\Users\phy\AppData\Roaming\Typora\typora-user-images\1555319817559.png)

```
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for permission
-- ----------------------------
DROP TABLE IF EXISTS `permission`;
CREATE TABLE `permission`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '权限名称',
  `description` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '权限描述表',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '权限表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for role
-- ----------------------------
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '权限名称',
  `description` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '权限描述',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for role_permission
-- ----------------------------
DROP TABLE IF EXISTS `role_permission`;
CREATE TABLE `role_permission`  (
  `role_id` int(11) NULL DEFAULT NULL,
  `permission_id` int(11) NULL DEFAULT NULL,
  INDEX `role_permission_uid_fk`(`role_id`) USING BTREE,
  INDEX `role_permission_pid_fk`(`permission_id`) USING BTREE,
  CONSTRAINT `role_permission_pid_fk` FOREIGN KEY (`permission_id`) REFERENCES `permission` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `role_permission_uid_fk` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户名',
  `password` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '密码',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_role
-- ----------------------------
DROP TABLE IF EXISTS `user_role`;
CREATE TABLE `user_role`  (
  `user_id` int(11) NULL DEFAULT NULL,
  `role_id` int(11) NULL DEFAULT NULL,
  INDEX `user_role_uid_fk`(`user_id`) USING BTREE,
  INDEX `user_role_rid_fk`(`role_id`) USING BTREE,
  CONSTRAINT `user_role_rid_fk` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `user_role_uid_fk` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '用户角色表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
```



#### 代码

代码的整体如下面

![1555319894089](C:\Users\phy\AppData\Roaming\Typora\typora-user-images\1555319894089.png)

我们首先不管常见的实体类和Dao层之类的，如果有不懂的下面也贴了代码，github中也有完整的代码

##### 注解控制权限

在APIController.java中我们用注解来控制权限，RequiredPermission控制权限，RequiredRole控制角色，如下代码

```
@RestController
@RequestMapping("/api")
public class APIController {

    @RequiredPermission("add")
    @RequestMapping("/add")
    public String add() {
        return "添加数据成功";
    }

    @RequiredPermission("delete")
    @RequestMapping("/delete")
    public String delete() {
        return "删除数据成功";
    }

    @RequiredPermission("get")
    @RequestMapping("/get")
    public String select() {
        return "查询数据成功";
    }

    @RequiredRole("boss")
    @RequestMapping("/boss")
    public String boss() {
        return "此数据为 Boss 专用数据, 你是 boss, 你可以查看";
    }

    @RequiredRole("employee")
    @RequestMapping("/employee")
    public String employee() {
        return "此数据为员工专用数据, 你是员工, 可以查看";
    }
}
```

我们是如何做的呢，我们首先来看这两个注解

```
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiredPermission {
    String value();
}
```

```
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiredRole {
    String value();
}
```

对注解不熟悉的同学可以看effectiveJava这本书对注解的解释，也可以看我的另一篇博客[effectiveJava学习笔记：注解](<https://blog.csdn.net/qq_39071530/article/details/83306460>)

我们利用注解来给方法设置value值。来给方法设置权限或者权限，这两者其实一样，给方法设置角色的时候还是遍历权限来判断是否有该权限。我们来看最关键的拦截代码

PermissionHandlerInterceptor.java

```
/**
 * author:ligz
 */
public class PermissionHandlerInterceptor implements HandlerInterceptor {

    @Resource
    private UserService userService;

    @Resource
    private RoleService roleService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        response.setHeader("Content-type", "text/html;charset=UTF-8");

        Method method = ((HandlerMethod) handler).getMethod();
        RequiredRole requiredRole = method.getAnnotation(RequiredRole.class);
        RequiredPermission requiredPermission = method.getAnnotation(RequiredPermission.class);
        User user = (User) request.getSession().getAttribute("user");

        if (user == null) {
            response.getWriter().write("未登录");
            return false;
        }


        List<Role> userRoles = userService.getUserRoles(user.getId());


        if (requiredRole != null) {
            for (Role role : userRoles) {
                if (role.getName().equals(requiredRole.value())) {
                    return true;
                }
            }
        }

        if (requiredPermission != null) {
            for (Role role : userRoles) {
                List<Permission> permissions = roleService.getPermissions(role.getId());
                for (Permission persission : permissions) {
                    if (requiredPermission.value().equals(persission.getName())) {
                        return true;
                    }
                }
            }
        }
        response.getWriter().println("你的权限不足");
        return false;
    }
}
```

我们拦截api/*下面的每一个请求，获取该方法的value值来对比当前在session中存储的用户的权限对比，如果有权限则成功，如果一个权限都不匹配那么就失败。

下面的是一些增删改查

##### 实体类

```
public class User {
    private Integer id;

    private String username;

    private String password;

    // getter setter 略
}
```

```
public class Role {
    private Integer id;

    private String name;

    private String description;

    // getter setter 略
}
```

```
public class Permission {
    private Integer id;

    private String name;

    private String description;

    // getter setter 略
}
```

##### Dao层

防止贴代码过多，我们将代码上传到github中，这里只列出user相关的Dao层和service层和controller层

UserMapper.java

```
package com.ligz.mapper;

import com.ligz.model.Role;
import com.ligz.model.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * author:ligz
 */
public interface UserMapper {
    int insert(User record);

    User selectByPrimaryKey(Integer id);

    int updateByPrimaryKey(User record);

    List<User> selectALL();

    /**
     * 查询用户拥有的角色列表
     * @param id 用户 id
     * @return 角色列表
     */
    List<Role> selectRolesByPrimaryKey(Integer id);

    /**
     * 删除用户所有角色
     * @param id 用户id
     * @return 删除成功的条数
     */
    int deleteRoles(Integer id);

    /**
     * 为用户赋予角色
     * @param userId userId 用户 id
     * @param id roleId 授予的角色 id
     * @return 插入成功的条数
     */
    int insertUserRole(@Param("user_id") Integer userId, @Param("role_id") Integer id);

    /**
     * 根据用户名密码查询账号是否存在
     * @param username 用户名
     * @param password 密码
     * @return 查询到的账号
     */
    User selectUserByUsernameAndPassword(@Param("username") String username, @Param("password") String password);
}

```



UserMapper.xml

```
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ligz.mapper.UserMapper">
    <resultMap id="BaseResultMap" type="com.ligz.model.User">
        <id column="id" jdbcType="INTEGER"  property="id"/>
        <result column="username" jdbcType="VARCHAR" property="username"/>
        <result column="password" jdbcType="VARCHAR" property="password"/>
    </resultMap>

    <sql id="Base_Column_List">
        id, username, password
    </sql>
    
    <select id="selectByPrimaryKey" parameterType="java.lang.Integer" resultMap="BaseResultMap">
        select
        <include refid="Base_Column_List"/>
        from user
        where id = #{id,jdbcType=INTEGER}
    </select>

    <select id="selectALL" resultType="com.ligz.model.User">
        select <include refid="Base_Column_List"/> from user
    </select>

    <insert id="insert" keyColumn="id" keyProperty="id" parameterType="com.ligz.model.User" useGeneratedKeys="true">
        insert into user (username, password) values (#{username,jdbcType=VARCHAR}, #{password,jdbcType=VARCHAR})
    </insert>

    <update id="updateByPrimary" parameterType="com.ligz.model.User">
        update user set username = #{username,jdbcType=VARCHAR},password = #{password,jdbcType=VARCHAR} where id = #{id,jdbcType=INTEGER}
    </update>

    <select id="selectRolesByPrimaryKey" parameterType="java.lang.Integer" resultType="com.ligz.model.Role">
        select role.* from role, user_role where user_role.role_id = role.id and user_role.user_id = #{id,jdbcType=INTEGER}
    </select>

    <delete id="deleteRoles" parameterType="java.lang.Integer">
        delete from user_role where user_id = #{id,jdbcType=INTEGER}
    </delete>

    <insert id="insertUserRole">
        insert into user_role (user_id, role_id) values (#{user_id}, #{role_id})
    </insert>

    <select id="selectUserByUsernameAndPassword" resultMap="BaseResultMap">
        select <include refid="Base_Column_List"/> from user where username=#{username,jdbcType=VARCHAR} and password=#{password,jdbcType=VARCHAR}
    </select>

</mapper>
```



UserService.java

```
package com.ligz.service;

import com.ligz.mapper.UserMapper;
import com.ligz.model.Role;
import com.ligz.model.User;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * author:ligz
 */
@Service
public class UserService {

    @Resource
    private UserMapper userMapper;

    public int add(User user) {
        return userMapper.insert(user);
    }

    public User get(Integer id) {
        return userMapper.selectByPrimaryKey(id);
    }

    public List<User> getAllUser() {
        return userMapper.selectALL();
    }

    public List<Role> getUserRoles(Integer id) {
        return userMapper.selectRolesByPrimaryKey(id);
    }

    public void updateRoles(Integer id, int[] roleIds) {
        userMapper.deleteRoles(id);
        if (roleIds != null) {
            for (int roleId : roleIds) {
                userMapper.insertUserRole(id, roleId);
            }
        }
    }

    public User selectUserByUsernameAndPassword(String username, String password) {
        return userMapper.selectUserByUsernameAndPassword(username, password);
    }

}
```

UserController.java

```
package com.ligz.controller;

import com.ligz.model.User;
import com.ligz.service.RoleService;
import com.ligz.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;

/**
 * author:ligz
 */
@Controller
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private RoleService roleService;

    @RequestMapping("listUser")
    public ModelAndView listUser() {
        return new ModelAndView("user.jsp").addObject("users", userService.getAllUser());
    }

    @RequestMapping("grantRoleView")
    public ModelAndView grantRoleView(int id) {
        ModelAndView modelAndView = new ModelAndView("grant-role.jsp");
        modelAndView.addObject("user", userService.get(id));
        modelAndView.addObject("roles", roleService.getAll());
        modelAndView.addObject("grantRole", userService.getUserRoles(id));
        return modelAndView;
    }

    @RequestMapping("grantRole")
    @ResponseBody
    public String grantRole(int id, int[] roleId) {
        userService.updateRoles(id, roleId);
        return "success";
    }

    @RequestMapping("addUser")
    @ResponseBody
    public String addUserSubmit(User user) {
        return userService.add(user) > 0 ? "success" : "error";
    }

}

```

##### 思考

代码本身不复杂，我们也很容易的看出缺点来。

* 密码明文展示
* 每一次请求都会获取对应的权限和角色，这种访问频率高的我们很显然需要缓存