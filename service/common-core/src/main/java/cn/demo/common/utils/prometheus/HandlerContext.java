package cn.demo.common.utils.prometheus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HandlerContext {
    private Method method;
    protected Annotation annotation;
    /*返回值的构造器*/
    protected Constructor<?> resConstructor;
    protected List<FieldInfo> fieldInfos;

    @Data
    public static class FieldInfo {
        private Field field;
        private String key;
        private ValueType valueType;
    }
}
