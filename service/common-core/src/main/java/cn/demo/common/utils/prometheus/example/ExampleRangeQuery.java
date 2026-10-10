package cn.demo.common.utils.prometheus.example;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 范围查询输入；通过 query.hostName 等属性引用参数。 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExampleRangeQuery {
    private String hostName;
    private String start;
    private String end;
}
