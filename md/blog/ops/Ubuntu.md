# Ubuntu初始化

## root 密码

```
sudo passwd root //配置密码
su root
```

## CD-ROM

```
sudo nano /etc/apt/sources.list
```

注释掉

```
deb cdrom:[Ubuntu-Server 16.04 LTS _Xenial Xerus_ - Release amd64 (20160420.3)]/ xenial main restricted
```

执行 cmd+O 然后 enter，之后 cmd+X

## ssh

```
sudo apt-get install openssh-server //安装ssh服务
```

修改 `/etc/ssh/sshd_config` 文件，将某个（不同版本不同）

```
PermitRootLogin without-password
PermitRootLogin prohibit-password
PermitRootLogin no
```

修改为

```
PermitRootLogin no
```

重启 `sudo service ssh restart`







