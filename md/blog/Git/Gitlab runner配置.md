

```
# 查看虚拟机系统信息
cat /proc/version

#查看 yum 的Java版本
yum -y list java*

#下载Java11
sudo yum install -y java-11-openjdk-devel.x86_64

#查看 git 版本。如果太低 fetch 会失败
git --version

# 移除 git 
yum remove git*

# 重新下载最新 git
sudo yum -y install  https://centos7.iuscommunity.org/ius-release.rpm

sudo yum -y install  git2u-all

#给docker 权限，否则会docker build失败
sudo chmod 666 /var/run/docker.sock

#下载 gitlab-runner
curl -L https://packages.gitlab.com/install/repositories/runner/gitlab-runner/script.rpm.sh | sudo bash

sudo yum install gitlab-runner

# 注册一个 gitlab-runner
sudo gitlab-runner register
	1.输入gitlab的地址，默认http://www.gitlab.com
	2.输入项目的 token 
	3.输入描述
	4.输入tag，不写
	
# 查看启动的gitlab-runner
gitlab-runner list

# 查看一些失效的runner
gitlab-runner verify
# 去除失效的runner
gitlab-runner verify --delete


```

docker 

```
sudo docker run -d --name gitlab-runner --restart always \
       -v /srv/gitlab-runner/config:/etc/gitlab-runner \
       -v /var/run/docker.sock:/var/run/docker.sock \
       gitlab/gitlab-runner:latest
       
sudo docker exec gitlab-runner gitlab-runner register -n \
       --url https://gitlab.com/ \
       --registration-token bUS15HCxvcGqHMzV1PzU \
       --executor docker \
       --docker-image docker \
       --docker-volumes /root/.m2:/root/.m2 \
       --docker-volumes /root/.npm:/root/.npm \
       --docker-volumes /var/run/docker.sock:/var/run/docker.sock \
       --description "group-runner-docker-dev-1"
```

