package cn.demo.common.utils.prometheus;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/** 启用 Mapper 扫描，通过 {@link #basePackages()} 指定接口所在的包（包含子包）。 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(PrometheusMapperRegistrar.class)
public @interface EnablePrometheusMappers {

    /** 要扫描的、包含 {@link PrometheusMapper} 接口的基础包。 */
    String[] basePackages();
}
