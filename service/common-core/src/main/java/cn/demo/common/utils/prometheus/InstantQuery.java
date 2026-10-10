package cn.demo.common.utils.prometheus;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 即时查询模板；{@link #time()} 预留为查询时间参数，当前预览阶段不执行请求。 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InstantQuery {
    /** PromQL 模板，支持 #{参数} 和 ${参数} 占位符。 */
    @AliasFor("promql")
    String value() default "";

    @AliasFor("value")
    String promql() default "";

    /** Prometheus 即时查询的时间参数，后续请求阶段使用。 */
    String time() default "";
}
