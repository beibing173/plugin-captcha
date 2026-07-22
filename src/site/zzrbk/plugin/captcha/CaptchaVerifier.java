package site.zzrbk.plugin.captcha;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public interface CaptchaVerifier {
    boolean supports(CaptchaProvider provider);

    Mono<Boolean> verify(ServerWebExchange exchange, CaptchaSetting setting);
}
