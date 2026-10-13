package cn.demo.common.utils.prometheus;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties("prometheus")
public class PrometheusConfig {
    private String url;
    private Duration timeout = Duration.ofSeconds(20);
}
