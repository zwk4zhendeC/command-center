package cn.demo.common.utils.prometheus;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/** 构建并发送 Prometheus HTTP 请求。 */
@Component
public class PrometheusClient {

    private final PrometheusConfig config;
    private final HttpClient httpClient;

    public PrometheusClient(PrometheusConfig config) {
        this.config = config;
        Duration timeout = requireTimeout(config.getTimeout());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * 创建带 URL 编码查询参数的 GET 请求。
     */
    public HttpRequest buildGet(String apiPath, Map<String, String> queryParameters) {
        if (config.getUrl() == null || config.getUrl().isBlank()) {
            throw new IllegalStateException("prometheus.url must not be blank");
        }

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUriString(config.getUrl())
                .path(apiPath);
        queryParameters.forEach(uriBuilder::queryParam);
        // 先构建组件再编码，避免 PromQL 中的 {label="value"} 被当成 URI 模板。
        URI uri = uriBuilder.build().encode().toUri();

        return HttpRequest.newBuilder(uri)
                .timeout(requireTimeout(config.getTimeout()))
                .header("Accept", "application/json")
                .GET()
                .build();
    }

    /**
     * 同步发送请求。只有 2xx 响应会交给上层解析。
     */
    public String send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Prometheus HTTP " + response.statusCode()
                        + ": " + response.body());
            }
            return response.body();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Prometheus request was interrupted: " + request.uri(), ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot request Prometheus: " + request.uri(), ex);
        }
    }

    private Duration requireTimeout(Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalStateException("prometheus.timeout must be greater than zero");
        }
        return timeout;
    }
}
