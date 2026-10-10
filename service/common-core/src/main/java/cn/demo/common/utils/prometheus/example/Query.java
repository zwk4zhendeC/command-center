package cn.demo.common.utils.prometheus.example;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 即时查询的标签参数，属性由 PromQL 模板读取。 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Query {
    private String hostName;
    private String hostIp;
}
