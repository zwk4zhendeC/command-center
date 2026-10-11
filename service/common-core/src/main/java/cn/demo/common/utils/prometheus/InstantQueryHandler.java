package cn.demo.common.utils.prometheus;

import cn.demo.common.utils.StringUtils;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class InstantQueryHandler extends QueryHandler {
    private Map<Method, HandlerContext> contextMap = new HashMap<>();

    @Override
    public HttpRequest buildHttpQuery(HandlerContext context, Object[] args) {
        return null;
    }

    @Override
    public HandlerContext buildContext(Class<?> mapperType, Method method) {
        HandlerContext context = contextMap.get(method);
        if (context == null) {
            synchronized (this) {
                if (!contextMap.containsKey(method)) {
                    context = new HandlerContext();
                    InstantQuery instant = AnnotatedElementUtils.findMergedAnnotation(method, InstantQuery.class);
                    context.setAnnotation(instant);
                    context.setMethod(method);
                    context.setResConstructor(extractResConstructor(mapperType, method));
                    context.setFieldInfos(extractFieldInfo(mapperType, method));
                    contextMap.put(method, context);
                }
            }
        }
        return context;
    }

    @Override
    public List<?> map(HandlerContext context, JSONObject data) {
        try {
            if (!"vector".equals(data.get("resultType"))) {
                throw new IllegalArgumentException("Instant mapping requires resultType=vector, got " + data.get("resultType"), null);
            }
            JSONArray series = data.getJSONArray("result");
            List<Object> result = new ArrayList<>(series.size());
            for (int index = 0; index < series.size(); index++) {
                try {
                    result.add(mapSeries(context, series.getJSONObject(index)));
                } catch (RuntimeException ex) {
                    throw new IllegalArgumentException("Cannot map result[" + index + "]: " + ex.getMessage(), ex);
                }
            }
            return result;
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid response: " + ex.getMessage(), ex);
        }
    }


    private Object mapSeries(HandlerContext context, JSONObject series) {
        JSONObject labels = series.getJSONObject("metric");
        if (!(series.get("value") instanceof List<?> sample) || sample.size() != 2) {
            throw new IllegalArgumentException("sample value must contain exactly [timestamp, value]", null);
        }
        // 时间戳是 JSON 数字；Prometheus 值是字符串也可能返回 NaN、+Inf、-Inf。
        if (!(sample.get(0) instanceof Number) || !(sample.get(1) instanceof String)) {
            throw new IllegalArgumentException("Sample requires a numeric timestamp and a Number value", null);
        }
        double timestamp = ((Number) sample.get(0)).doubleValue();
        if (!Double.isFinite(timestamp)) {
            throw new IllegalArgumentException("Sample timestamp must be finite", null);
        }
        double value;
        try {
            value = sampleNumber((String) sample.get(1));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid sample value: " + sample.get(1), ex);
        }
        Object dto;
        try {
            dto = context.getResConstructor().newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException("Cannot construct DTO: " + context.getResConstructor().getDeclaringClass().getName(), ex);
        }
        List<HandlerContext.FieldInfo> fieldInfos = context.getFieldInfos();
        for (HandlerContext.FieldInfo fieldInfo : fieldInfos) {
            Object fieldValue;
            Field field = fieldInfo.getField();
            try {
                switch (fieldInfo.getValueType()) {
                    case LABEL -> {
                        /*没有使用PromValue标签的情况*/
                        Object labelValue = null;
                        if (StringUtils.isEmpty(fieldInfo.getKey())) {
                            String label = field.getName();
                            labelValue = labels.getOrDefault(label, labels.get(snakeCase(label)));
                        } else {
                            labelValue = labels.get(fieldInfo.getKey());
                        }
                        if (labelValue == null) continue;
                        if (!(labelValue instanceof String)) {
                            throw fieldError(field, "Label value must be a string: " + field.getName(), null);
                        }
                        fieldValue = convert(labelValue, field.getType());
                    }
                    case TIMESTAMP -> fieldValue = convert(sample.get(0), field.getType());
                    case VALUE -> fieldValue = convert(value, field.getType());
                    case MATRICES -> {
                        continue;
                    }
                    case SAMPLE -> {
                        SampleValue point = new SampleValue();
                        point.setTimestamp(timestamp);
                        point.setValue(value);
                        fieldValue = point;
                    }
                    default -> throw fieldError(field, "Unsupported instant field source", null);
                }
                //todo：这里需要改为获取set方法，而不是执行反射操作
                field.setAccessible(true);
                field.set(dto, fieldValue);
            } catch (RuntimeException | IllegalAccessException ex) {
                throw fieldError(field, "Cannot fill field: " + ex.getMessage(), ex);
            }
        }
        return dto;
    }

}
