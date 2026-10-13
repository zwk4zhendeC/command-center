package cn.demo.common.utils.prometheus;

import cn.demo.common.utils.StringUtils;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.net.http.HttpRequest;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static cn.demo.common.utils.prometheus.ValueType.MATRICES;

/**
 * 将 Prometheus 范围查询返回的 matrix 数据映射为 Mapper 方法声明的 DTO 列表。
 */
@Component
public class RangeQueryHandler extends QueryHandler {

    private final Map<Method, HandlerContext> contextMap = new ConcurrentHashMap<>();
    private static final String API_PREFIX = "/query_range";

    public RangeQueryHandler(PrometheusClient prometheusClient) {
        super(prometheusClient);
    }

    @Override
    public HttpRequest buildHttpQuery(HandlerContext context, Object[] args) {
        Map<String, Object> methodParameters = parameters(context.getMethod(), args);
        RangeQuery query = (RangeQuery) context.getAnnotation();

        Map<String, String> queryParameters = new LinkedHashMap<>();
        queryParameters.put("query", render(query.promql(), methodParameters));
        putRenderedIfPresent(queryParameters, "start", query.start(), methodParameters);
        putRenderedIfPresent(queryParameters, "end", query.end(), methodParameters);
        putRenderedIfPresent(queryParameters, "step", query.step(), methodParameters);
        return buildGetRequest(API_PREFIX, queryParameters);
    }

    @Override
    public HandlerContext buildContext(Class<?> mapperType, Method method) {
        return contextMap.computeIfAbsent(method, ignored -> createContext(mapperType, method));
    }

    private HandlerContext createContext(Class<?> mapperType, Method method) {
        RangeQuery range = AnnotatedElementUtils.findMergedAnnotation(method, RangeQuery.class);
        if (range == null) {
            throw new IllegalArgumentException("Range query handler requires @RangeQuery: " + method);
        }

        List<HandlerContext.FieldInfo> fieldInfos = extractFieldInfo(mapperType, method);
        for (HandlerContext.FieldInfo fieldInfo : fieldInfos) {
            Field field = fieldInfo.getField();
            if (fieldInfo.getValueType() == ValueType.MATRICES) {
                validateMatricesField(field);
            }
        }

        HandlerContext context = new HandlerContext();
        context.setAnnotation(range);
        context.setMethod(method);
        context.setResConstructor(extractResConstructor(mapperType, method));
        context.setFieldInfos(fieldInfos);
        return context;
    }

    private void validateMatricesField(Field field) {
        Type genericType = field.getGenericType();
        if (field.getType() != List.class
                || !(genericType instanceof ParameterizedType parameterizedType)
                || parameterizedType.getActualTypeArguments().length != 1
                || parameterizedType.getActualTypeArguments()[0] != SampleValue.class) {
            throw fieldError(field, "MATRICES field type must be List<SampleValue>", null);
        }
    }

    @Override
    public List<?> map(HandlerContext context, JSONObject data) {
        try {
            if (data == null) {
                throw new IllegalArgumentException("response data must not be null");
            }
            if (!"matrix".equals(data.getString("resultType"))) {
                throw new IllegalArgumentException(
                        "Range mapping requires resultType=matrix, got " + data.get("resultType"));
            }

            JSONArray series = data.getJSONArray("result");
            if (series == null) {
                throw new IllegalArgumentException("result must be an array");
            }

            List<Object> result = new ArrayList<>(series.size());
            for (int index = 0; index < series.size(); index++) {
                try {
                    JSONObject item = series.getJSONObject(index);
                    if (item == null) {
                        throw new IllegalArgumentException("series must be an object");
                    }
                    result.add(mapSeries(context, item));
                } catch (RuntimeException ex) {
                    throw new IllegalArgumentException(
                            "Cannot map result[" + index + "]: " + ex.getMessage(), ex);
                }
            }
            return result;
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid response: " + ex.getMessage(), ex);
        }
    }

    private Object mapSeries(HandlerContext context, JSONObject series) {
        JSONObject labels = series.getJSONObject("metric");
        if (labels == null) {
            throw new IllegalArgumentException("metric must be an object");
        }

        JSONArray samples = series.getJSONArray("values");
        if (samples == null) {
            throw new IllegalArgumentException("values must be an array");
        }
        List<SampleValue> matrices = mapSamples(samples);

        Object dto;
        try {
            dto = context.getResConstructor().newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException(
                    "Cannot construct DTO: " + context.getResConstructor().getDeclaringClass().getName(), ex);
        }

        for (HandlerContext.FieldInfo fieldInfo : context.getFieldInfos()) {
            Field field = fieldInfo.getField();
            try {
                Object fieldValue = null;
                switch (fieldInfo.getValueType()) {
                    case LABEL -> {
                        Object labelValue = labelValue(fieldInfo, labels);
                        if (labelValue == null) {
                            continue;
                        }
                        if (!(labelValue instanceof String)) {
                            throw fieldError(field, "Label value must be a string", null);
                        }
                        fieldValue = convert(labelValue, field.getType());
                    }
                    case MATRICES -> fieldValue = matrices;
                }
                if (fieldValue != null) field.set(dto, fieldValue);
            } catch (RuntimeException | IllegalAccessException ex) {
                throw fieldError(field, "Cannot fill field: " + ex.getMessage(), ex);
            }
        }
        return dto;
    }

    private Object labelValue(HandlerContext.FieldInfo fieldInfo, JSONObject labels) {
        if (!StringUtils.isEmpty(fieldInfo.getKey())) {
            return labels.get(fieldInfo.getKey());
        }
        String fieldName = fieldInfo.getField().getName();
        return labels.getOrDefault(fieldName, labels.get(snakeCase(fieldName)));
    }

    private List<SampleValue> mapSamples(JSONArray samples) {
        List<SampleValue> result = new ArrayList<>(samples.size());
        for (int index = 0; index < samples.size(); index++) {
            Object rawSample = samples.get(index);
            if (!(rawSample instanceof List<?> sample) || sample.size() != 2) {
                throw new IllegalArgumentException(
                        "values[" + index + "] must contain exactly [timestamp, value]");
            }
            if (!(sample.get(0) instanceof Number timestampValue)
                    || !(sample.get(1) instanceof String sampleValue)) {
                throw new IllegalArgumentException(
                        "values[" + index + "] requires a numeric timestamp and a string value");
            }

            double timestamp = timestampValue.doubleValue();
            if (!Double.isFinite(timestamp)) {
                throw new IllegalArgumentException("values[" + index + "] timestamp must be finite");
            }

            double value;
            try {
                value = sampleNumber(sampleValue);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(
                        "Invalid sample value at values[" + index + "]: " + sampleValue, ex);
            }

            SampleValue point = new SampleValue();
            point.setTimestamp(timestamp);
            point.setValue(value);
            result.add(point);
        }
        return result;
    }
}
