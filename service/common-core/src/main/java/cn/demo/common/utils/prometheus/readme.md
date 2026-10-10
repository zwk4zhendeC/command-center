prometheus-query要做什么：
1. 让外部查询Prometheus时，像使用Mybatis一样，使用注解在接口方法上标注promql语句,调用该接口后动态代理会调用Prometheus接口，并且将返回结果中的对象序列化成接口返回值。


### 当前实现：仅预览 PromQL

- 在配置类上添加 `@EnablePrometheusMappers(basePackages = "...")`，接口标记 `@PrometheusMapper`，即可按接口类型注入或使用默认 Bean 名注入。
- `@InstantQuery` 和 `@RangeQuery` 的 `value` / `promql` 互为别名；兼容早期的 `@PromQLInstant`。
- `@Param("query")` 对应模板中的 `query`，支持 JavaBean 属性和 Map 键的点号路径。未声明 `@Param` 时，需要编译时启用 `-parameters`。
- `#{query.hostName}` 将值转为带双引号、已转义的 PromQL 字符串；`${...}` 原样插入，标签字符串建议使用 `#{...}`。占位符不需要再加引号。
- 空值转换为空字符串；不存在的参数、属性或 Map 键会报错。
- 调用查询方法只向标准输出打印渲染后的 PromQL。引用类型返回 `null`，基本类型返回零值，`void` 无返回值。
- `time`、`start`、`end`、`step` 和 `@PromValue` 为后续请求、响应映射预留，当前不处理。

例如 `hostName = "node-1"`、`hostIp = "172.16.0.3"` 时，调用示例的 `up` 方法输出：

```promql
up{host_name="node-1",host_ip="172.16.0.3"}
```

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