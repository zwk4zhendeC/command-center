package cn.demo.common.utils.prometheus;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 范围查询模板；start/end/step 预留为请求参数，当前只输出 PromQL。 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RangeQuery {
    /** PromQL 模板，支持 #{参数} 和 ${参数} 占位符。 */
    @AliasFor("promql")
    String value() default "";

    @AliasFor("value")
    String promql() default "";

    /** 范围查询起始时间表达式。 */
    String start() default "";

    /** 范围查询结束时间表达式。 */
    String end() default "";

    /** 查询步长表达式。 */
    String step() default "";
}
