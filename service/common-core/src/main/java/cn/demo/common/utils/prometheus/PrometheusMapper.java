package cn.demo.common.utils.prometheus;

import java.lang.annotation.*;

/** 标记需要由注册器扫描并生成动态代理的 Prometheus 查询接口。 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PrometheusMapper {
}
