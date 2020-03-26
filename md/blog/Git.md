### 常用的命令

#### 创建一个新的项目后

使用 

`git add .` 

`git commit -m ''`

`git remote add origin ...`

`git push -u origin master`

#### 使用分支

`git chenkout -b dev`创建 dev 分支

`git push origin dev` 创建远程的 dev 分支

#### 克隆他人的项目使用分支

`git clone ... `

`git checkout -b dev origin/dev`

`git branch` 可查看

`git pull origin dev` 拉到本地的 dev 分支



$ git pull <远程主机名> <远程分支名>:<本地分支名>

#### stash 和 pop

```
Git stash
git stash pop
```



#### Rebase的用法

如果我们需要从远程仓库中获取某个分支到本地的某个分支。

可以使用命令

```
git checkout branchname

git pull origin/branchname
```

不过这样会出现merge的commit信息，会污染commit的提交树。

可以使用`git pull --rebase`来合并commit信息

但是代码多了rebase会很难受，没有IDEA的可视化

在IDEA中，

```
git branch --set-upstream-to=origin/release_issus release_issus
```

可以将远程的release_issus分支连接到本地的release_issus分支

这样我们可以使用`Useing Stash + rebase `的方式合并，并且有可视化的merge。



在master上merge分支也会出现多余的commit信息，我们照样使用 `git rebase release_issus`的方式合并，不会污染commit树。











强制使用git最新的代码覆盖本地

``git fetch --all && git reset --hard origin/master && git pull``



Git clone 慢

`git config --global http.postBuffer 524288000`

```
$ git clone http://github.com/large-repository --depth 1

$ cd large-repository

$ git fetch --unshallow
```