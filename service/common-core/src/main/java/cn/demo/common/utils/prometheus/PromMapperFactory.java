package cn.demo.common.utils.prometheus;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 创建 Prometheus Mapper 接口的 JDK 动态代理。
 *
 * <p>代理调用时只完成模板解析和输出 PromQL，后续再接入 HTTP 客户端及结果映射。
 * 这样可以先验证接口声明和查询模板是否符合预期。</p>
 */
@Component
public class PromMapperFactory {

    private static final Pattern PLACEHOLDER = Pattern.compile("([#$])\\{([^{}]+)}");

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
                    //todo:要加缓存
                    String template = queryTemplate(method);
                    String promql = render(template, parameters(method, args));
                    System.out.println(promql);
                    //todo：
                    // 预览阶段引用类型返回 null；基本类型返回零值，避免代理拆箱失败。
                    Class<?> returnType = method.getReturnType();
                    return returnType.isPrimitive() && returnType != void.class
                            ? Array.get(Array.newInstance(returnType, 1), 0) : null;
                });
        return mapperType.cast(proxy);
    }

    /**
     * 从方法上读取唯一的查询注解，并取得 PromQL 模板。
     */
    private String queryTemplate(Method method) {
        // 合并注解会处理 value/promql 的 @AliasFor，支持两种写法。
        InstantQuery instant = AnnotatedElementUtils.findMergedAnnotation(method, InstantQuery.class);
        RangeQuery range = AnnotatedElementUtils.findMergedAnnotation(method, RangeQuery.class);
//        PromQLInstant legacy = AnnotatedElementUtils.findMergedAnnotation(method, PromQLInstant.class);
        int count = (instant != null ? 1 : 0) + (range != null ? 1 : 0);
        if (count != 1) {
            throw new IllegalArgumentException("Mapper method must declare exactly one query annotation: " + method);
        }
        String template = instant != null ? instant.promql() : range.promql();
        if (template.isBlank()) {
            throw new IllegalArgumentException("PromQL must not be blank: " + method);
        }
        return template;
    }

    /**
     * 将 Java 方法参数转换为模板可引用的参数表。
     */
    private Map<String, Object> parameters(Method method, Object[] args) {
        Map<String, Object> values = new LinkedHashMap<>();
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Param annotation = parameters[i].getAnnotation(Param.class);
            String name;
            if (annotation != null) {
                name = annotation.value();
            } else if (parameters[i].isNamePresent()) {
                name = parameters[i].getName();
            } else {
                throw new IllegalArgumentException("Declare @Param or compile with -parameters: " + method);
            }
            if (name.isBlank() || values.containsKey(name)) {
                throw new IllegalArgumentException("Blank or duplicate mapper parameter: " + name);
            }
            values.put(name, args[i]);
        }
        return values;
    }

    /**
     * #{...} 是 PromQL 字符串字面量，${...} 是原始文本；空值渲染为空字符串。
     */
    private String render(String template, Map<String, Object> parameters) {
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            Object value = resolve(matcher.group(2).trim(), parameters);
            String text = value == null ? "" : String.valueOf(value);
            String replacement = "#".equals(matcher.group(1)) ? quote(text) : text;
            // 参数里的 $ 和反斜杠只作为文本，不再被正则替换解释。
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 按点号读取 @Param 根参数及其 JavaBean 属性或 Map 键。
     */
    private Object resolve(String path, Map<String, Object> parameters) {
        String[] segments = path.split("\\.", -1);
        if (!parameters.containsKey(segments[0])) {
            throw new IllegalArgumentException("Unknown PromQL parameter: " + path);
        }
        Object value = parameters.get(segments[0]);
        for (int i = 1; i < segments.length; i++) {
            if (segments[i].isBlank()) {
                throw new IllegalArgumentException("Invalid PromQL property path: " + path);
            }
            if (value == null) {
                return null;
            }
            if (value instanceof Map<?, ?> map) {
                if (!map.containsKey(segments[i])) {
                    throw new IllegalArgumentException("Unknown PromQL map key: " + path);
                }
                value = map.get(segments[i]);
            } else {
                BeanWrapper bean = PropertyAccessorFactory.forBeanPropertyAccess(value);
                if (!bean.isReadableProperty(segments[i])) {
                    throw new IllegalArgumentException("Unreadable PromQL property: " + path);
                }
                value = bean.getPropertyValue(segments[i]);
            }
        }
        return value;
    }

    /**
     * 将参数转成 PromQL 字符串字面量，并转义特殊字符。
     */
    private String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
    }
}
