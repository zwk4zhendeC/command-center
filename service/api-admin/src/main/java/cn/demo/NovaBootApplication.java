package cn.demo;

import cn.demo.common.utils.prometheus.EnablePrometheusMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * 启动程序
 *
 * @author ruoyi
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnablePrometheusMappers(basePackages = "cn.demo.common.utils.prometheus.example")
public class NovaBootApplication {

    public static void main(String[] args) {
        // System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(NovaBootApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  服务启动成功   ლ(´ڡ`ლ)ﾞ  ");
    }
}
