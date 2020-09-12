## Gradle 使用

[TOC]

### 基础介绍

基于 Gradle 的实质就是 Groovy 脚本，执行一种类型的配置脚本时就会创建一个关联的对象，譬如执行 Build script 脚本就会创建一个 Project 对象，这个对象其实就是 Gradle 的代理对象。下面给出来各种类型 Gradle 对应的对象类型：

表 1. Gradle 构建对象表

| 脚本类型        | 关联对象类型          |
| :-------------- | :-------------------- |
| Init script     | Gradle Initialization |
| Settings script | Settings              |
| Build script    | Project               |



Gradle 的三种主要对象解释如下：

- Gradle 对象：构建初始化时创建，整个构建执行过程中只有这么一个对象，一般不推荐修改这个默认配置。
- Settings 对象：每个 settings.gradle（Gradle 所定义的一个约定名称的设置文件 settings.gradle）会转换成一个 Settings 对象，该文件在初始化阶段被执行，对于多项目构建必须保证在根目录下有 settings.gradle 文件，对于单项目构建设置文件是可选的，不过建议还是需要配置上。
- Project 对象：每个 build.gradle 会转换成一个 Project 对象。



Gradle 的三种不同时期脚本用途如下：

1. 初始化脚本 Init script（Gradle）

Gradle 对象：初始化脚本 Init script（Gradle）类似于 Gradle 的其他类型脚本，这种脚本在构建开始之前运行，主要的用途是为接下来的 Build script 做一些准备工作。我们如果需要编写初始化脚本 Init script，则可以把它按规则放置在 USER_HOME/.gradle/相关目录下。

初始化脚本的Gradle 对象代表了 Gradle 的调运，我们可以通过调用 Project 对象的 getGradle()方法获得 Gradle 实例对象。



2. 设置脚本 Settings script（Settings）

Settings 对象：在对工程进行配置（譬如多项目构建）时 Settings 实例与 settings.gradle 文件一一对应，它用来进行一些项目设置的配置。这个文件一般放置在工程的根目录。



3. 构建脚本 Build script（Project）

在 Gradle 中每个待编译的工程都是一个 Project（每个工程的 build.gradle 对应一个 Project 对象），每个 Project 在构建的时候都包含一系列 Task，这些 Task 中很多又是 Gradle 的插件默认支持的。所谓的我们编写 Gradle 脚本，实质大多数时候都是在编写构建脚本 Build script，所以说 Project 和 Script 对象的属性和方法等 API 非常重要。

每一个 Project 对象和 build.gradle 一一对应，一个项目在构建时都具备如下流程：

1. 为当前项目创建一个 Settings 类型的实例。如果当前项目存在 settings.gradle 文件，则通过该文件配置刚才创建的 Settings 实例。
2. 通过 Settings 实例的配置创建项目层级结构的 Project 对象实例。
3. 最后通过上面创建的项目层级结构 Project 对象实例去执行每个 Project 对应的 build.gradle 脚本。



### 生命周期

作为一个构建脚本的开发者，不应只局限于编写任务动作或者配置逻辑，有时候想在指定的生命周期事件发生的时候执行一段代码，这里就需要了解生命周期事件。生命周期事件可以在指定的生命周期之前、之中或者之后发生，在执行阶段之后发生的生命周期事件就该是构建的完成了。具体执行周期见下图：

图 1. Gradle 生命周期图

![img](https://www.ibm.com/developerworks/cn/opensource/os-using-gradle-to-build-docker-image/image005.jpg)

- Initialization phase：Gradle 支持单项目和多项目构建，在初始化阶段 Gradle 决定那些项目需要加入构建，并为这些需要加入构建的项目分别创建 Project 实例，实质为执行 settings.gradple 脚本。

- Configuration phase：配置阶段将整个 build 的 Project 及 Task 关系确定，它会建立一个有向图来描述 Task 之间的相互依赖关系，解析每个被加入构建项目的 build.gradle 脚本。

- Execution phase：执行阶段就是 Gradle 执行 root-project 及每个 sub-project 下面自定义的 Task 任务及其所依赖的 Task 以达到进行最终构建目标的生成。

可以看见，生命周期其实和上面构建脚本 Build script 的执行流程是可以关联上的。



### 依赖配置

在 Gradle 中依赖可以组合成 configurations（配置），一个配置简单地说就是一系列的依赖，通俗理解就是依赖配置，可以使用它们声明项目的外部依赖，也可以被用来声明项目的发布。下面我们给出几种 Java 插件中常见的配置，如下：

- compile

用来编译项目源代码的依赖；

- runtime

在运行时被生成的类需要的依赖，默认项，包含编译时的依赖；

- testCompile

编译测试代码依赖，默认项，包含生成的类运行所需的依赖和编译源代码的依赖；

- testRuntime

运行测试所需要的依赖，默认项，包含上面三个依赖；