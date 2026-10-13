# Prometheus 声明式查询

在接口方法上用注解定义 PromQL，调用 Mapper 时动态代理会渲染模板、发送 Prometheus GET 请求，并将响应映射为 `List<T>`。

## 使用方式

- 配置类添加 `@EnablePrometheusMappers(basePackages = "...")`，接口标记 `@PrometheusMapper`，即可从 Spring 容器注入接口。
- 配置 `prometheus.url`，即时查询使用 `/api/v1/query`，范围查询使用 `/api/v1/query_range`。
- `@InstantQuery` 和 `@RangeQuery` 的 `value` / `promql` 互为别名。
- `@Param("query")` 定义模板根参数 `query`；未声明 `@Param` 时，需要编译时启用 `-parameters`。
- `promql`、`time`、`start`、`end`、`step` 全部使用 Pebble 模板。未配置的可选时间参数不会发送。

## Pebble 模板

旧的 `#{...}` 和 `${...}` 不再支持，使用时会抛出迁移提示。
使用 `{{ query.hostName }}` 读取 JavaBean 属性或 Map 键；未定义的变量、属性或键会报错。
已定义的 null 值渲染为空字符串，可通过条件主动省略对应标签。

字符串的外层双引号由模板作者添加。Java 普通字符串中需要写成 `\"`，文本块中可直接写 `"`：

```java
@InstantQuery(value = """
        up{host_name="{{ query.hostName }}"}
        """, time = "{{ time }}")
List<SingleRes> up(@Param("query") Query query, @Param("time") String time);
```

输出内容默认使用 PromQL 字符串转义：反斜杠、双引号、换行、回车和制表符分别转换为 `\\`、`\"`、`\n`、`\r` 和 `\t`，不会添加外层引号，也不会进行 HTML 转义。
数值或时间参数直接使用 `{{ step }}`、`{{ time }}`。
需要插入可信的完整 PromQL 片段时使用 `{{ fragment | raw }}`，该过滤器会跳过默认转义。

可选标签使用条件判空；`is not empty` 同时排除 null、空字符串和空白字符串：

```pebble
up{
{% if query is not null and query.hostName is not empty %}
host_name="{{ query.hostName }}"
{% endif %}
}
```

如果变量本身可能未定义，先用 `query is defined` 判断，再访问其属性。
循环可以构建多个主机查询，`loop.last` 用于省略最后一个分隔符，空集合使用 `else` 分支：

```pebble
{% for hostName in hostNames %}
up{host_name="{{ hostName }}"}{% if not loop.last %} or {% endif %}
{% else %}vector(0){% endfor %}
```

模板采用内嵌字符串加载器，不支持通过 `include`、`extends`、`import` 加载模板文件。
引擎缓存编译后的模板，每次调用使用独立参数上下文。
渲染异常包含 Mapper 方法、注解属性名以及 Pebble 异常信息，并保留原始异常链以定位模板行号。

## 返回值映射

- 返回类型必须为具有具体 DTO 类型的 `List<T>`，DTO 必须有无参构造器。空结果返回空列表。
- 默认从标签填充实例字段（包含父类字段），先按原字段名匹配，再匹配下划线名称；缺失标签保留字段初始值。
- `@PromValue(value = "host_name")` 显式指定 LABEL 标签名。
- 即时查询支持 TIMESTAMP、VALUE 和 SAMPLE；SAMPLE 字段必须为 `SampleValue`，不允许 MATRICES。
- 范围查询使用 MATRICES，字段必须为 `List<SampleValue>`；不允许即时查询的 TIMESTAMP、VALUE 和 SAMPLE。
- `SampleValue.timestamp` 为秒单位的 `Double`，保留小数秒。整数目标字段遇到小数值会报错，不静默截断。

### 支持的查询
#### 即时查询
**1. 时间序列**
请求：
```bash
curl --location -g --request GET 'https://metrics.zwkzone.cn/api/v1/query?query=up{job="host"}'
```
结果：
```json
{
  "status": "success",
  "data": {
    "resultType": "vector",
    "result": [
      {
        "metric": {
          "__name__": "up",
          "asset_type": "host",
          "host_ip": "172.16.0.3",
          "host_name": "lavm-3tm34xa6s4",
          "instance": "node-exporter:9100",
          "job": "host",
          "version": "v4"
        },
        "value": [
          1791517746,
          "1"
        ]
      },
      {
        "metric": {
          "__name__": "up",
          "asset_type": "host2",
          "host_ip": "172.16.0.4",
          "host_name": "lavm-3tm34xa6s5",
          "instance": "node-exporter:9100",
          "job": "host",
          "version": "v4"
        },
        "value": [
          1791517746,
          "1"
        ]
      }
    ]
  },
  "stats": {
    "seriesFetched": "1",
    "executionTimeMsec": 6
  }
}
```

2. **标量查询**
```bash
curl --location --request GET 'https://metrics.zwkzone.cn/api/v1/query?query=count(up)'
```
结果：
```json
{
    "status": "success",
    "data": {
        "resultType": "vector",
        "result": [
            {
                "metric": {},
                "value": [
                    1791518513,
                    "11"
                ]
            }
        ]
    },
    "stats": {
        "seriesFetched": "11",
        "executionTimeMsec": 2
    }
}
```

### 范围查询
```bash
curl --location --request GET 'https://metrics.zwkzone.cn/api/v1/query_range?query=up{job="host"}'
```
结果
```json
{
    "status": "success",
    "data": {
        "resultType": "matrix",
        "result": [
            {
                "metric": {
                    "__name__": "up",
                    "asset_type": "host",
                    "host_ip": "172.16.0.3",
                    "host_name": "lavm-3tm34xa6s4",
                    "instance": "node-exporter:9100",
                    "job": "host",
                    "version": "v4"
                },
                "values": [
                    [
                        1791518187,
                        "1"
                    ],
                    [
                        1791518487,
                        "1"
                    ]
                ]
            }
        ]
    },
    "stats": {
        "seriesFetched": "1",
        "executionTimeMsec": 8
    }
}
```
