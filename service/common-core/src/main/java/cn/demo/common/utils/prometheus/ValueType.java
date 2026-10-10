package cn.demo.common.utils.prometheus;


/** 结果映射类型：标签、时间戳、数值、单个采样点和采样点列表。 */
public enum ValueType {
    /** 时间序列标签。 */
    LABEL,
    /** Prometheus 样本时间戳。 */
    TIMESTAMP,
    /** Prometheus 样本值。 */
    VALUE,
    /** 单个样本。 */
    SAMPLE,
    /** 多个样本组成的矩阵。 */
    MATRICES
}
