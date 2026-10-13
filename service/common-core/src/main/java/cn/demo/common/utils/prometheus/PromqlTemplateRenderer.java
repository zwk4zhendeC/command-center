package cn.demo.common.utils.prometheus;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.loader.StringLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/** 渲染注解中的 Pebble 字符串模板，支持条件、循环和 PromQL 内容转义。 */
@Component
public class PromqlTemplateRenderer {

    // StringLoader 用于内嵌模板；不提供模板文件的 include/extends/import。
    // 引擎内置缓存按模板文本复用编译结果，不缓存每次调用的参数。
    private final PebbleEngine promqlEngine = new PebbleEngine.Builder()
            .loader(new StringLoader())
            .strictVariables(true)      /*引用不存在的变量或属性时抛出异常*/
            .autoEscaping(true)     /*自动对模板表达式的输出执行转义*/
            .addEscapingStrategy("promql", new PromqlEscapingStrategy())
            .defaultEscapingStrategy("promql")
            .cacheActive(true)
            .build();

    /*渲染基本参数的引擎，引用策略使用不存在则为空*/
    private final PebbleEngine argsEngine = new PebbleEngine.Builder()
            .loader(new StringLoader())
            .strictVariables(false)      /*引用不存在的变量或属性时抛出异常*/
            .autoEscaping(true)     /*自动对模板表达式的输出执行转义*/
            .addEscapingStrategy("promql", new PromqlEscapingStrategy())
            .defaultEscapingStrategy("promql")
            .cacheActive(true)
            .build();

    public String renderPromQL(Method method, String attribute, String source,
                               Map<String, Object> parameters) {
        try {
            StringWriter writer = new StringWriter();
            // 每次调用使用独立上下文和输出缓冲区，允许并发复用已编译模板。
            promqlEngine.getTemplate(source).evaluate(writer, new HashMap<>(parameters));
            return writer.toString();
        } catch (IOException | RuntimeException ex) {
            throw new IllegalArgumentException(method.toGenericString() + " [" + attribute + "]"
                    + ": Cannot render Pebble template: " + ex.getMessage(), ex);
        }
    }

    public String renderPromArgs(Method method, String attribute, String source,
                               Map<String, Object> parameters) {
        try {
            StringWriter writer = new StringWriter();
            // 每次调用使用独立上下文和输出缓冲区，允许并发复用已编译模板。
            argsEngine.getTemplate(source).evaluate(writer, new HashMap<>(parameters));
            return writer.toString();
        } catch (IOException | RuntimeException ex) {
            throw new IllegalArgumentException(method.toGenericString() + " [" + attribute + "]"
                    + ": Cannot render Pebble template: " + ex.getMessage(), ex);
        }
    }
}
