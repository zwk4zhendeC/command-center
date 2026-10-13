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

    @InstantQuery(value = """
            up{
                {% if query.hostName is not empty %}
                    host_name="{{ query.hostName }}",
                {% endif %}
                {% if query.hostIp is not empty %}
                    host_ip="{{ query.hostIp }}",
                {% endif %}
            }
            """
            , time = "{{ time }}")
    List<SingleRes> up(@Param("query") Query query, @Param("time") String time);


    @RangeQuery(value = """
              ( 1 - (
                node_memory_MemAvailable_bytes{host_name="{{ query.hostName }}"}
                / node_memory_MemTotal_bytes{host_name="{{ query.hostName }}"}
              )) * 100
            """, start = "{{ query.start }}", end = "{{ query.end }}")
    List<RangeRes> memory(@Param("query") ExampleRangeQuery query);

    /**
     * 可选主机名：null、空字符串或空白字符串时省略标签条件。
     */
    @InstantQuery("""
            up{
            {% if hostName is not empty %}
                host_name="{{ hostName }}"
            {% endif %}
            }
            """)
    List<SingleRes> upOptional(@Param("hostName") String hostName);

    /**
     * 循环构建多个查询分支，每个主机名按 PromQL 字符串内容转义。
     */
    @InstantQuery("""
            {% for hostName in hostNames %}
            up{host_name="{{ hostName }}"}{% if not loop.last %} or {% endif %}
            {% else %}vector(0){% endfor %}
            """)
    List<SingleRes> upForHosts(@Param("hostNames") List<String> hostNames);
}
