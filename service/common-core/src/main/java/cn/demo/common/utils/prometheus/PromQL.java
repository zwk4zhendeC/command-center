package cn.demo.common.utils.prometheus;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PromQL {
    /** PromQL 模板，支持 #{参数} 和 ${参数} 占位符。 */
    @AliasFor("promql")
    String value() default "";

    @AliasFor("value")
    String promql() default "";
}
