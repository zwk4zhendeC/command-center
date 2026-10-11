package cn.demo.common.utils.prometheus;

import lombok.Data;

/** 一个采样点：Prometheus 时间戳和数值。 */
@Data
public class SampleValue {
    /** Unix 时间戳，单位为秒，保留小数秒；对应样本中的第一个元素。 */
    @PromValue(type = ValueType.TIMESTAMP)
    private Double timestamp;
    /** 样本数值，Prometheus 返回值中的第二个元素。 */
    @PromValue(type = ValueType.VALUE)
    private Double value;
}
