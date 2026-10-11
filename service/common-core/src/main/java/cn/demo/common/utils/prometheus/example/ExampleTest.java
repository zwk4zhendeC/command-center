package cn.demo.common.utils.prometheus.example;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/** 容器启动后演示注入和调用 Mapper；即时查询返回模拟响应映射后的 DTO 列表。 */
@Component
public class ExampleTest {
    @Resource
    private PrometheusExample prometheusExample;

    @PostConstruct
    public void init() {
        List<SingleRes> res1 = prometheusExample.up(new Query("host1", "1.1.1.1"), "123");
        System.out.println("res:"+res1);
//        RangeRes memory = prometheusExample.memory(new ExampleRangeQuery("host1", "2026-10-03T20:10:51.781Z", "2026-10-04T20:10:51.781Z"));
//        RangeRes test = prometheusExample.test();
    }
}
