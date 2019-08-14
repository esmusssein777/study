如果服务强制关闭导致了项目连接redis出现

Please check the Redis logs for details about the RDB error.

进入redis，写上

```
config set stop-writes-on-bgsave-error no
```

![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)