package cn.demo.common.utils.prometheus;

import io.pebbletemplates.pebble.extension.escaper.EscapingStrategy;

/** 转义 PromQL 字符串内容；外层双引号由模板作者添加。 */
public class PromqlEscapingStrategy implements EscapingStrategy {

    @Override
    public String escape(String input) {
        return input.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
