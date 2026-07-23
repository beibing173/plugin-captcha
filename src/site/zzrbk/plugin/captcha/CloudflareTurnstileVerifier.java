package site.zzrbk.plugin.captcha;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import java.time.Duration;
import reactor.core.publisher.Mono;

public class CloudflareTurnstileVerifier implements CaptchaVerifier {

    private static final Logger log = LoggerFactory.getLogger(CloudflareTurnstileVerifier.class);
    private static final String VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    private static final String TOKEN_PARAM = "cf-turnstile-response";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(CaptchaProvider provider) {
        return provider == CaptchaProvider.CLOUDFLARE;
    }

    @Override
    public Mono<Boolean> verify(ServerWebExchange exchange, CaptchaSetting setting) {
        return exchange.getFormData().flatMap(formData -> {
            String token = formData.getFirst(TOKEN_PARAM);
            if (token == null || token.isBlank()) {
                return Mono.just(false);
            }
            return verifyToken(token, setting.getCloudflareSecretKey());
        });
    }

    private Mono<Boolean> verifyToken(String token, String secretKey) {
        if (secretKey == null || secretKey.isBlank()) {
            return Mono.just(false);
        }

        String body = "secret=" + encode(secretKey) + "&response=" + encode(token);

        return WebClient.create()
                .post()
                .uri(VERIFY_URL)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue(body)
                .retrieve()
.bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .map(this::parseResponse)
                .onErrorResume(e -> {
                    log.error("Cloudflare Turnstile verify error", e);
                    return Mono.just(false);
                });
    }

    private boolean parseResponse(String body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            boolean success = node.path("success").asBoolean(false);
            log.info("Cloudflare Turnstile result: success={}", success);
            return success;
        } catch (Exception e) {
            log.error("Cloudflare Turnstile parse error", e);
            return false;
        }
    }

    private static String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
}
