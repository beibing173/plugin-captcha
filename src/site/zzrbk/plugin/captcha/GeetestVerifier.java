package site.zzrbk.plugin.captcha;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class GeetestVerifier implements CaptchaVerifier {

    private static final Logger log = LoggerFactory.getLogger(GeetestVerifier.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(CaptchaProvider provider) {
        return provider == CaptchaProvider.GEETEST;
    }

    @Override
    public Mono<Boolean> verify(ServerWebExchange exchange, CaptchaSetting setting) {
        return exchange.getFormData().flatMap(formData -> {
            String lotNumber = formData.getFirst("lot_number");
            String captchaOutput = formData.getFirst("captcha_output");
            String passToken = formData.getFirst("pass_token");
            String genTime = formData.getFirst("gen_time");

            log.info("Geetest verify: lot={} captcha_output={} pass_token={} gen_time={}",
                    lotNumber, captchaOutput, passToken, genTime);

            if (lotNumber == null || captchaOutput == null || passToken == null || genTime == null) {
                log.warn("Geetest: missing form fields");
                return Mono.just(false);
            }

            String captchaId = setting.getGeetestId();
            String captchaKey = setting.getGeetestKey();

            if (captchaId == null || captchaKey == null
                    || captchaId.isBlank() || captchaKey.isBlank()) {
                log.warn("Geetest: missing captcha id or key");
                return Mono.just(false);
            }

            String signToken = hmacSha256(lotNumber, captchaKey);
            log.info("Geetest sign_token={}", signToken);

            String url = "https://gcaptcha4.geetest.com/validate?captcha_id=" + enc(captchaId);
            String body = "lot_number=" + enc(lotNumber)
                    + "&captcha_output=" + enc(captchaOutput)
                    + "&pass_token=" + enc(passToken)
                    + "&gen_time=" + enc(genTime)
                    + "&sign_token=" + enc(signToken)
                    + "&captcha_id=" + enc(captchaId);

            return WebClient.create()
                    .post()
                    .uri(url)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .map(this::parseResponse)
                    .onErrorResume(e -> {
                        log.error("Geetest API error", e);
                        return Mono.just(false);
                    });
        });
    }

    private Boolean parseResponse(String body) {
        log.info("Geetest API resp: {}", body);
        try {
            JsonNode node = objectMapper.readTree(body);
            String result = node.path("result").asText("");
            String reason = node.path("reason").asText("");
            log.info("Geetest result={} reason={}", result, reason);
            return "success".equals(result);
        } catch (Exception e) {
            log.error("Geetest parse error", e);
            return false;
        }
    }

    private String enc(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    private static String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 failed", e);
        }
    }
}
