```
rs.reconfig(config)
{
        "startupStatus" : 1,
        "ok" : 0,
        "errmsg" : "loading local.system.replset config (LOADINGCONFIG)"
}
```

需要使用

```
config = {
    "_id" : "shard1",
    "version" : 1,
    "members" : [
        {"_id" : 1,"host" : "172.16.2.211:22001"},
        {"_id" : 2,"host" : "172.16.2.212:22001"},
        {"_id" : 3,"host" : "172.16.2.213:22001,"arbiterOnly" : true}
    ]
}
rs.reconfig(config, {force: true})
```

![点击并拖拽以移动](data:image/gif;base64,R0lGODlhAQABAPABAP///wAAACH5BAEKAAAALAAAAAABAAEAAAICRAEAOw==)

