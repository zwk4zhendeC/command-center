package cn.demo.common.utils.prometheus;

import jakarta.annotation.Resource;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 创建 Prometheus Mapper 接口的 JDK 动态代理。
 *
 * <p>代理输出 PromQL，并将模拟响应 RES 映射为即时查询结果；后续再接入 HTTP 客户端。</p>
 */
@Component
public class PromMapperFactory {

    private static final Pattern PLACEHOLDER = Pattern.compile("([#$])\\{([^{}]+)}");

    @Autowired
    private InstantQueryHandler instantQueryHandler;

    /**
     * 为指定接口创建代理实例。
     *
     * @param mapperType 使用 {@link PrometheusMapper} 标记的接口类型
     * @param <T>        Mapper 接口类型
     * @return Mapper 代理
     */
    public <T> T createMapper(Class<T> mapperType) {
        if (mapperType == null || !mapperType.isInterface()) {
            throw new IllegalArgumentException("Prometheus mapper must be an interface");
        }
        Object proxy = Proxy.newProxyInstance(mapperType.getClassLoader(),
                new Class<?>[]{mapperType},
                (instance, method, args) -> {
                    // 容器和调试器可能调用 Object 方法，它们不属于 PromQL 查询。
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> "PrometheusMapper[" + mapperType.getName() + "]";
                            case "hashCode" -> System.identityHashCode(instance);
                            case "equals" -> instance == args[0];
                            default -> throw new IllegalStateException("Unexpected Object method: " + method);
                        };
                    }
                    if (method.isDefault()) {
                        return InvocationHandler.invokeDefault(instance, method, args);
                    }
                    Class<? extends Annotation> clazz = queryAnnotation(method);
                    QueryHandler queryHandler = null;
                    if (clazz == InstantQuery.class) {
                        queryHandler = instantQueryHandler;
                    }  else if (clazz == RangeQuery.class) {
                        throw new UnsupportedOperationException("RangeQuery is not implemented yet");
                    } else {
                        throw new IllegalArgumentException("Unsupported query annotation: " + clazz);
                    }
                    return queryHandler.invocation(mapperType, method, args);
                });
        return mapperType.cast(proxy);
    }

    Map<Method, Class<? extends Annotation>> annotationCache = new HashMap<>();

    private Class<? extends Annotation> queryAnnotation(Method method) {
        Class<? extends Annotation> clazz = annotationCache.get(method);
        if (clazz == null)
            synchronized (this) {
                if (!annotationCache.containsKey(method)) {
                    InstantQuery instant = AnnotatedElementUtils.findMergedAnnotation(method, InstantQuery.class);
                    RangeQuery range = AnnotatedElementUtils.findMergedAnnotation(method, RangeQuery.class);
                    int count = (instant != null ? 1 : 0) + (range != null ? 1 : 0);
                    if (count != 1) {
                        throw new IllegalArgumentException("Mapper method must declare exactly one query annotation: " + method);
                    }
                    clazz = instant != null ? InstantQuery.class : RangeQuery.class;
                    annotationCache.put(method, clazz);
                }
            }
        return clazz;
    }
}
