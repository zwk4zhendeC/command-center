package cn.demo.common.utils.prometheus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 指定结果字段的数据来源；未标注的实例字段默认从标签中映射。 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PromValue {
    /** LABEL 的标签名；为空时先匹配 Java 字段名，再匹配下划线名称。其他类型不使用此属性。 */
    String value() default "";

    /** 字段对应的 Prometheus 数据类型。 */
    ValueType type() default ValueType.LABEL;

}
