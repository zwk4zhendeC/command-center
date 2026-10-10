package cn.demo.common.utils.prometheus.example;

import cn.demo.common.utils.prometheus.PromValue;
import cn.demo.common.utils.prometheus.SampleValue;
import cn.demo.common.utils.prometheus.ValueType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 即时查询期望的返回结构，data 对应单个采样点。 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SingleRes {
    private String hostName;
    private String hostIp;

    @PromValue(type = ValueType.SAMPLE)
    SampleValue data;
}
