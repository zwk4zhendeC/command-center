package cn.demo.common.utils.prometheus;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 范围查询模板；调用时向 Prometheus {@code /query_range} 端点发送 GET 请求。 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RangeQuery {
    /** Pebble PromQL 模板；使用 {{ 参数 }}，字符串外层双引号需在模板中添加。 */
    @AliasFor("promql")
    String value() default "";

    @AliasFor("value")
    String promql() default "";

    /** 范围查询 start 参数的 Pebble 模板；为空时不发送该参数。 */
    String start() default "";

    /** 范围查询 end 参数的 Pebble 模板；为空时不发送该参数。 */
    String end() default "";

    /** 查询 step 参数的 Pebble 模板；为空时不发送该参数。 */
    String step() default "";
}
