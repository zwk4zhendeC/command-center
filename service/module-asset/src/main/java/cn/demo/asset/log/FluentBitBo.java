package cn.demo.asset.log;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class FluentBitBo {
    private Service service;
    private List<Parse> parses;
    private Pipeline pipeline;

    @Data
    public static class Service {

    }

    @Data
    public static class Parse {

    }

    @Data
    public static class Pipeline {
        private Map<String, Map<String, String>> inputs = new LinkedHashMap<>();
        private Map<String, Map<String, String>> filters = new LinkedHashMap<>();
        private Map<String, Map<String, String>> outputs = new LinkedHashMap<>();
    }
}
