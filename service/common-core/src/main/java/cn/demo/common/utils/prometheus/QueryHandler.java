package cn.demo.common.utils.prometheus;

import com.alibaba.fastjson2.JSONObject;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.http.HttpRequest;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class QueryHandler {

    //    public abstract List<?>
    private final static Pattern PLACEHOLDER = Pattern.compile("([#$])\\{([^{}]+)}");
    private final static DefaultConversionService conversion = new DefaultConversionService();

    private static final String RES = """
{
    "status": "success",
    "data": {
        "resultType": "matrix",
        "result": [
            {
                "metric": {
                    "__name__": "up",
                    "asset_type": "host",
                    "host_ip": "172.16.0.3",
                    "host_name": "lavm-3tm34xa6s4",
                    "instance": "node-exporter:9100",
                    "job": "host",
                    "version": "v4"
                },
                "values": [
                    [
                        1791793763,
                        "1"
                    ],
                    [
                        1791794063,
                        "1"
                    ]
                ]
            }
        ]
    },
    "stats": {
        "seriesFetched": "1",
        "executionTimeMsec": 11
    }
}
            """;

    /**
     * 构建请求体，不过还没有想清楚返回什么
     */
    public abstract HttpRequest buildHttpQuery(HandlerContext context, Object[] args);

    /**
     * 外部入口
     *
     * @param method
     * @param args
     */
    public List<?> invocation(Class<?> mapperType, Method method, Object[] args) {
        HandlerContext context = buildContext(mapperType, method);
        HttpRequest httpRequest = buildHttpQuery(context, args);
        /**
         * 1. 发送请求
         * 2. 处理响应的status
         * 3. 提取data，调用map将响应中data的数据映射成返回结果
         */
        JSONObject jsonObject = JSONObject.parseObject(RES);
        return map(context, jsonObject.getJSONObject("data"));
    }

    public abstract HandlerContext buildContext(Class<?> mapperType, Method method);

    /**
     * 将返回结果映射成方法返回值
     *
     * @return
     */
    public abstract List<?> map(HandlerContext context, JSONObject data);

    /**
     * 提取属性及其注解
     * @param mapperType
     * @param method
     * @return
     */
    protected List<HandlerContext.FieldInfo> extractFieldInfo(Class<?> mapperType, Method method) {
        Class<?> dtoType = extractResType(mapperType, method);
        List<HandlerContext.FieldInfo> res = new ArrayList<>();
        Set<String> names = new HashSet<>();
        // 子类的同名字段优先，跳过静态字段和编译器生成的字段。
        for (Class<?> type = dtoType; type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()
                        || !names.add(field.getName())) {
                    continue;
                }
                field.setAccessible(true);
                HandlerContext.FieldInfo fieldInfo = new HandlerContext.FieldInfo();
                fieldInfo.setField(field);
                PromValue annotation = field.getAnnotation(PromValue.class);
                if (annotation != null) {
                    fieldInfo.setKey(annotation.value());
                    fieldInfo.setValueType(annotation.type());
                } else {
                    fieldInfo.setKey(null);
                    fieldInfo.setValueType(ValueType.LABEL);
                }
                res.add(fieldInfo);
            }
        }
        return res;
    }

    private Class<?> extractResType(Class<?> mapperType, Method method) {
        ResolvableType returnType = ResolvableType.forMethodReturnType(method, mapperType);
        /**
         * 1. 检查返回值是否是List类型
         * 2. 检查返回值是否为List<具体类型>
         * 3. 检查泛型参数是否为具体类型，而非List<?>,List<? extends XX> 等类型
         * 4. 检查泛型是否是List<T>
         */
        if (method.getReturnType() != List.class
                || !(method.getGenericReturnType() instanceof ParameterizedType declared)
                || declared.getActualTypeArguments()[0] instanceof WildcardType
                || returnType.hasUnresolvableGenerics()) {
            throw new IllegalArgumentException(method.toGenericString() + ": " + "Return type must be List<T> with a concrete DTO type");
        }
        /*拿到返回值的第一层泛型*/
        Class<?> dtoType = returnType.getGeneric(0).resolve();
        /*排除接口、抽象类、数组、枚举、或者第一层泛型中又包了一层泛型*/
        if (dtoType == null || dtoType == Object.class || dtoType.isInterface()
                || Modifier.isAbstract(dtoType.getModifiers()) || dtoType.isPrimitive()
                || dtoType.isArray() || dtoType.isEnum()
                || returnType.getGeneric(0).hasGenerics()   //我感觉这个判断可以去掉
        ) {
            throw new IllegalArgumentException(method.toGenericString() + ": " + "List element must be a concrete DTO class");
        }
        return dtoType;
    }

    protected Constructor<?> extractResConstructor(Class<?> mapperType, Method method) {
        Class<?> resType = extractResType(mapperType, method);
        Constructor<?> constructor = null;
        try {
            constructor = resType.getDeclaredConstructor();
            if (!constructor.trySetAccessible()) {
                throw new IllegalArgumentException(method.toGenericString() + ": " + "DTO no-argument constructor is inaccessible", null);
            }
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException(method.toGenericString() + ": " + "DTO requires a no-argument constructor: " + resType.getName(), ex);
        }
        return constructor;
    }

    /**
     * 从方法上读取唯一的查询注解，并取得 PromQL 模板。
     */
    protected String queryTemplate(Method method) {
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
    protected Map<String, Object> parameters(Method method, Object[] args) {
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
    protected String render(String template, Map<String, Object> parameters) {
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
    protected Object resolve(String path, Map<String, Object> parameters) {
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
    protected String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
    }

    /**
     * 数值先转成字符串再转换，整数目标遇到小数会报错，避免静默截断。
     */
    protected Object convert(Object value, Class<?> targetType) {
        if (value instanceof Number && targetType != Double.class && targetType != double.class
                && targetType != Float.class && targetType != float.class
                && targetType != String.class) {
            String number = value.toString();
            if (Double.isFinite(((Number) value).doubleValue())) {
                number = new java.math.BigDecimal(number).stripTrailingZeros().toPlainString();
            }
            return conversion.convert(number, targetType);
        }
        return conversion.convert(value, targetType);
    }

    protected double sampleNumber(String value) {
        return switch (value) {
            case "+Inf", "Inf" -> Double.POSITIVE_INFINITY;
            case "-Inf" -> Double.NEGATIVE_INFINITY;
            default -> Double.parseDouble(value);
        };
    }

    protected String snakeCase(String name) {
        return name.replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
    }


    protected IllegalArgumentException fieldError(Field field, String message, Throwable cause) {
        return new IllegalArgumentException("Field " + field.getDeclaringClass().getName() + "." + field.getName() + ": " + message, cause);
    }

}
