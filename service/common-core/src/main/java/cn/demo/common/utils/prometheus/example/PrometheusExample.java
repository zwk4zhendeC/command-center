package cn.demo.common.utils.prometheus.example;

import cn.demo.common.utils.prometheus.InstantQuery;
import cn.demo.common.utils.prometheus.Param;
import cn.demo.common.utils.prometheus.PrometheusMapper;
import cn.demo.common.utils.prometheus.RangeQuery;

import java.util.List;

/**
 * 调用方期望的声明式查询接口，方法注解定义 PromQL 模板。
 */
@PrometheusMapper
public interface PrometheusExample {

    @InstantQuery(value = "up{host_name=#{query.hostName},host_ip=#{query.hostIp}}", time = "${time}")
    List<SingleRes> up(@Param("query") Query query, @Param("time") String time);


    @InstantQuery(value = "up{host_name=#{query.hostName},host_ip=#{query.hostIp}}", time = "${time}")
    List<SingleRes2> up2(@Param("query") Query query, @Param("time") String time);

    @RangeQuery(value = """
              ( 1 - (
                node_memory_MemAvailable_bytes{host_name=#{query.hostName}}
                / node_memory_MemTotal_bytes{host_name=#{query.hostName}}
              )) * 100
            """, start = "${query.start}", end = "${query.end}")
    List<RangeRes> memory(@Param("query") ExampleRangeQuery query);

//
//    @RangeQuery("""
//              ( 1 - (
//              )) * 100
//            """)
//    RangeRes test();
}
