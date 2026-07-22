package run.halo.captcha;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class LocalCaptchaVerifier implements CaptchaVerifier {

    private static final Logger log = LoggerFactory.getLogger(LocalCaptchaVerifier.class);
    private static final String CODE_PARAM = "captcha_code";

    private final CaptchaCodeStore codeStore;

    public LocalCaptchaVerifier(CaptchaCodeStore codeStore) {
        this.codeStore = codeStore;
    }

    @Override
    public boolean supports(CaptchaProvider provider) {
        return provider == CaptchaProvider.LOCAL;
    }

    @Override
    public Mono<Boolean> verify(ServerWebExchange exchange, CaptchaSetting setting) {
        return exchange.getFormData().flatMap(formData -> {
            String token = getCookieToken(exchange);
            if (token == null || token.isBlank()) {
                log.warn("Local captcha: no token cookie found");
                return Mono.just(false);
            }

            String userCode = formData.getFirst(CODE_PARAM);
            if (userCode == null || userCode.isBlank()) {
                log.warn("Local captcha: no captcha_code in form");
                return Mono.just(false);
            }

            String storedCode = codeStore.getAndRemove(token);
            if (storedCode == null) {
                log.warn("Local captcha: no stored code for token {}", token);
                return Mono.just(false);
            }

            boolean valid = storedCode.equalsIgnoreCase(userCode.trim());
            log.info("Local captcha verification: {} for token {}", valid, token);
            return Mono.just(valid);
        });
    }

    private String getCookieToken(ServerWebExchange exchange) {
        var cookies = exchange.getRequest().getCookies().getFirst("captcha_token");
        if (cookies != null) {
            return cookies.getValue();
        }
        return null;
    }
}
