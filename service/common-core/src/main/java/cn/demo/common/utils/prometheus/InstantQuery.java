package cn.demo.common.utils.prometheus;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 即时查询模板；调用时向 Prometheus {@code /query} 端点发送 GET 请求。 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InstantQuery {
    /** Pebble PromQL 模板；使用 {{ 参数 }}，字符串外层双引号需在模板中添加。 */
    @AliasFor("promql")
    String value() default "";

    @AliasFor("value")
    String promql() default "";

    /** Prometheus 即时查询的 time 参数的 Pebble 模板；为空时不发送该参数。 */
    String time() default "";
}
