package cn.demo.common.utils.prometheus;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PromQL {
    /** Pebble PromQL 模板；使用 {{ 参数 }}，字符串外层双引号需在模板中添加。 */
    @AliasFor("promql")
    String value() default "";

    @AliasFor("value")
    String promql() default "";
}
