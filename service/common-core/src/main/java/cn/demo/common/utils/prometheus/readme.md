prometheus-query要做什么：
1. 让外部查询Prometheus时，像使用Mybatis一样，使用注解在接口方法上标注promql语句,调用该接口后动态代理会调用Prometheus接口，并且将返回结果中的对象序列化成接口返回值。


### 当前实现：输出 PromQL 并映射模拟即时查询响应

- 在配置类上添加 `@EnablePrometheusMappers(basePackages = "...")`，接口标记 `@PrometheusMapper`，即可按接口类型注入或使用默认 Bean 名注入。
- `@InstantQuery` 和 `@RangeQuery` 的 `value` / `promql` 互为别名。
- `@Param("query")` 对应模板中的 `query`，支持 JavaBean 属性和 Map 键的点号路径。未声明 `@Param` 时，需要编译时启用 `-parameters`。
- `#{query.hostName}` 将值转为带双引号、已转义的 PromQL 字符串；`${...}` 原样插入，标签字符串建议使用 `#{...}`。占位符不需要再加引号。
- 空值转换为空字符串；不存在的参数、属性或 Map 键会报错。
- 即时查询向标准输出打印 PromQL，再将 `RES` 中的 vector 响应转换为 `List<T>`。不发送 HTTP 请求；范围查询结果映射暂不支持并明确报错。
- 返回类型必须为具有具体 DTO 类型的 `List<T>`，DTO 必须有无参构造器。空结果返回空列表。
- 默认从标签填充实例字段（包含父类字段），先按原字段名匹配，再匹配下划线名称；缺失标签保留字段初始值。
- `@PromValue(value = "host_name")` 显式指定 LABEL 标签名；TIMESTAMP 取样本时间戳，VALUE 取样本数值，SAMPLE 字段必须为 `SampleValue`。即时查询不允许 MATRICES。
- `SampleValue.timestamp` 使用 `Double`，单位为秒并保留小数秒。整数目标字段遇到小数值会报错，不静默截断。
- `time`、`start`、`end`、`step` 为后续请求预留，当前不处理。

例如 `hostName = "node-1"`、`hostIp = "172.16.0.3"` 时，调用示例的 `up` 方法输出：

```promql
up{host_name="node-1",host_ip="172.16.0.3"}
```

先不实际查询,我在PromMapperFactory中提供了一个RES变量作为返回值
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
