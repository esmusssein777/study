# Hexo博客部署

## 准备Hexo网站

1. 在本地建立一个Hexo站点，可以参考[官方快速开始文档](https://hexo.io/zh-cn/docs/)。
2. 建立两个GitHub仓库，分别叫`blog`（私有的）和`guangzhengli.github.io`（共有的）。前者用来存储博客源文件，后者用于挂载GitHub Pages。
3. 将本地的博客源文件推送到`blog`仓库。

## 准备秘钥

为了方便运行GitHub Actions时登录GitHub账号，我们使用SSH方式登录。

使用ssh-keygen生成一组公私秘钥对

```
ssh-keygen -t rsa -b 4096 -f ~/.ssh/github-actions-deploy
复制代码
```

在`Settings`->`SSH and GPG keys`添加刚刚生成的公钥，名称随意。 在`blog`仓库的`Settings`->`Secrets`里添加刚刚生成的私钥，名称为 `ACTION_DEPLOY_KEY`。

## 配置 GitHub Actions

添加部署配置

```
name: Deploy Blog

on: [push] # 当有新push时运行

jobs:
  build: # 一项叫做build的任务

    runs-on: ubuntu-latest # 在最新版的Ubuntu系统下运行
    
    steps:
    - name: Checkout # 将仓库内master分支的内容下载到工作目录
      uses: actions/checkout@v1 # 脚本来自 https://github.com/actions/checkout
      
    - name: Use Node.js 10.x # 配置Node环境
      uses: actions/setup-node@v1 # 配置脚本来自 https://github.com/actions/setup-node
      with:
        node-version: "10.x"
    
    - name: Setup Hexo env
      env:
        ACTION_DEPLOY_KEY: ${{ secrets.ACTION_DEPLOY_KEY }}
      run: |
        # set up private key for deploy
        mkdir -p ~/.ssh/
        echo "$ACTION_DEPLOY_KEY" | tr -d '\r' > ~/.ssh/id_rsa
        chmod 600 ~/.ssh/id_rsa
        ssh-keyscan github.com >> ~/.ssh/known_hosts
        # set git infomation
        git config --global user.name 'liguangzheng'
        git config --global user.email 'guangzheng.li@thoughtworks.com'
        # install dependencies
        npm i -g hexo-cli # 安装hexo
        npm install --save hexo-render-pug
        npm install hexo-deployer-git --save
        npm install hexo-wordcount --save
        npm uninstall hexo-generator-index --save
        npm install hexo-generator-index-pin-top --save
        npm install hexo-filter-github-emojis --save
        npm i
  
    - name: Deploy
      run: |
        hexo generate && hexo deploy
```

# 配置Hexo的_config.yml

添加部署配置

```
# Site
title: Ligz's Blog
subtitle: ''
description: ''
keywords:
author: GuangZheng Li
language: zh-CN
timezone: 'Asia/Shanghai'

# Deployment
## Docs: https://hexo.io/docs/deployment.html
deploy:
- type: git
  repo: git@github.com:guangzhengli/guangzhengli.github.io.git
  branch: master
```

