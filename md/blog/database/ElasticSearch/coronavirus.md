# 运用Elastic Stack分析COVID-19数据并进行可视化分析

## 数据来源

我们将使用在地址https://www.datawrapper.de/_/Gnfyw/下载我们想要的数据。我们点击链接Get the data来下载我们想要的数据。

将导入的数据修改名字为 `covid19.csv`

配置`filebeat` 来导入数据

```
filebeat.inputs:
- type: log
  paths:
    - /Users/guangzheng.li/workspace/elasticsearch/covid19.csv
  exclude_lines: ['^Lat']
 
output.elasticsearch:
  hosts: ["http://localhost:9200"]
  index: covid19
 
setup.ilm.enabled: false
setup.template.name: covid19
setup.template.pattern: covid19
```

在上面我们定义了一个type为log的filebeat.inputs。我们定了我们的log的路径。你需要根据自己的实际路径来修改上面的路径。我们定义了数据的index为covid19的索引。值得注意的是，由于csv文件的第一行是数据的header，我们需要去掉这一行。为此，我们采用了exclude_lines: ['^Lat']来去掉第一行。

等我们定义好上面的配置后，我们运行如下的命令：

```
./filebeat -e -c ../filebeat_covid19.yml
```

可以对数据进行查询：

```
GET covid19/_search
```

![FZEp94](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/FZEp94.png)



### 去掉无用的字段

在我们的文档里，我们可以看到有很多我们并不想要的字段，比如ecs， host，log等等。我们想把这些字段去掉，那么我们该如何做呢？我们可以通过定义一个pipleline来帮我们处理。为此，我们定义一个如下的pipeline:

```
PUT _ingest/pipeline/covid19_parser
{
  "processors": [
    {
      "remove": {
        "field": ["log", "input", "ecs", "host", "agent"],
        "if": "ctx.log != null && ctx.input != null && ctx.ecs != null && ctx.host != null && ctx.agent != null"
      }
    }
  ]
}
```


上面的pipeline定义了一个叫做remove的processor。它检查log，input, ecs, host及agent都不为空的情况下，删除字段log, input，ecs, host及agent。我们在Kibana中执行上面的命令。

为了能够使得我们的pipleline起作用，我们通过如下指令来执行：

`POST covid19/_update_by_query?pipeline=covid19_parser`
当我们执行完上面的指令后，我们重新查看我们的文档



![uZf597](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/uZf597.png)



### 替换引号

我们可以看到导入的message数据为：

`"""37.1232245,-78.4927721,"Virginia, US",Virginia,",",US,221,0,0"""`
显然，这里的数据有很多的引号"字符，我们想把这些字符替换为符号'。为此，我们需要gsub processors来帮我们处理。我重新修改我们的pipeline:

```
PUT _ingest/pipeline/covid19_parser
{
  "processors": [
    {
      "remove": {
        "field": ["log", "input", "ecs", "host", "agent"],
        "if": "ctx.log != null && ctx.input != null && ctx.ecs != null && ctx.host != null && ctx.agent != null"
      }
    },
    {
      "gsub": {
        "field": "message",
        "pattern": "\"",
        "replacement": "'"
      }
    }    
  ]
}
```

在Kibana中运行上面的指令，并同时执行：

`POST covid19/_update_by_query?pipeline=covid19_parser`
经过上面的pipeline的处理后

我们的message的信息如下：

```
"37.1232245,-78.4927721,'Virginia, US',Virginia,', ',US,1249,0,0"
```

### 解析信息
在上面我们已经很成功地把我们的信息转换为我们所希望的数据类型。接下来我们来使用grok来解析我们的数据。grok的数据解析，基本上是一种正则解析的方法。我们首先使用Kibana所提供的Grok Debugger来帮助我们分析数据。我们将使用如下的grok pattern来解析我们的message:

`%{NUMBER:lat:float},%{NUMBER:lon:float},'%{DATA:address}',%{DATA:city},', *',%{DATA:country},%{NUMBER:infected:int},%{NUMBER:death:int}`

![3fkqMh](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/3fkqMh.png)

我们点击Grok Debugger，并把我们的相应的文档拷入到相应的输入框中，并用上面的grok pattern来解析数据。上面显示，它可以帮我们成功地解析我们想要的数据。显然这个被解析的信息更适合我们做数据的分析。为此，我们需要重新修改pipeline：

```
PUT _ingest/pipeline/covid19_parser
{
  "processors": [
    {
      "remove": {
        "field": ["log", "input", "ecs", "host", "agent"],
        "if": "ctx.log != null && ctx.input != null && ctx.ecs != null && ctx.host != null && ctx.agent != null"
      }
    },
    {
      "gsub": {
        "field": "message",
        "pattern": "\"",
        "replacement": "'"
      }
    },
    {
     "grok": {
        "field": "message",
        "patterns": [
          "%{NUMBER:lat:float},%{NUMBER:lon:float},'%{DATA:address}',%{DATA:city},', *',%{DATA:country},%{NUMBER:infected:int},%{NUMBER:death:int}"
        ]
      }
    }        
  ]
}
```

我们运行上面的pipeline，并使用如下的命令来重新对数据进行分析：

`POST covid19/_update_by_query?pipeline=covid19_parser`
我们重新来查看文档：
![HlxLpn](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/HlxLpn.png)

在上面我们可以看到新增加的country，infected，address等等的字段。

### 添加location字段

在上面我们可以看到lon及lat字段。这些字段是文档的经纬度信息。这些信息并不能为我们所使用，因为首先他们是分散的，并不处于一个通过叫做location的字段中。为此，我们需要创建一个新的location字段。为此我们更新pipeline为：

```
PUT _ingest/pipeline/covid19_parser
{
  "processors": [
    {
      "remove": {
        "field": ["log", "input", "ecs", "host", "agent"],
        "if": "ctx.log != null && ctx.input != null && ctx.ecs != null && ctx.host != null && ctx.agent != null"
      }
    },
    {
      "gsub": {
        "field": "message",
        "pattern": "\"",
        "replacement": "'"
      }
    },
    {
     "grok": {
        "field": "message",
        "patterns": [
          "%{NUMBER:lat:float},%{NUMBER:lon:float},'%{DATA:address}',%{DATA:city},', *',%{DATA:country},%{NUMBER:infected:int},%{NUMBER:death:int}"
        ]
      }
    },
    {
      "set": {
        "field": "location.lat",
        "value": "{{lat}}"
      }
    },
    {
      "set": {
        "field": "location.lon",
        "value": "{{lon}}"
      }
    }
  ]
}
```

在上面我们设置了一个叫做location.lat及location.lon的两个字段。它们的值分别是{{lat}}及{{lon}}。我们执行上面的命令。

由于location是一个新增加的字段，在默认的情况下，它的两个字段都会被Elasticsearch设置为text的类型。为了能够让我们的数据在地图中进行显示，它必须是一个geo_point的数据类型。为此，我们必须通过如下命令来设置它的数据类型：

```
PUT covid19/_mapping
{
  "properties": {
    "location": {
      "type": "geo_point"
    }
  }
}
```

生效查询

```
POST covid19/_update_by_query?pipeline=covid19_parser

GET covid19/_search
```

![2q5ihq](https://cdn.jsdelivr.net/gh/guangzhengli/ImgURL@master/uPic/2q5ihq.png)

