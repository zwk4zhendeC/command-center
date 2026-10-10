package cn.demo.common.utils.prometheus.example;

import cn.demo.common.utils.prometheus.PromValue;
import cn.demo.common.utils.prometheus.SampleValue;
import cn.demo.common.utils.prometheus.ValueType;
import lombok.Data;

import java.util.List;

/** 范围查询期望的返回结构，data 对应采样点列表。 */
@Data
public class RangeRes {
    private String hostName;
    private String hostIp;

    @PromValue(type = ValueType.MATRICES)
    List<SampleValue> data;
}
