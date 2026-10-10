package cn.demo.common.utils.prometheus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 描述结果字段的数据来源，预留给后续响应映射；当前代理不读取此注解。 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PromValue {
    /** Prometheus 返回字段名，未指定时使用 Java 字段名。 */
    String value() default "";

    /** 字段对应的 Prometheus 数据类型。 */
    ValueType type() default ValueType.LABEL;

}
