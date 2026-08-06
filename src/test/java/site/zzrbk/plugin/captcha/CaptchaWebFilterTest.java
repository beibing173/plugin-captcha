package site.zzrbk.plugin.captcha;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.halo.app.plugin.ReactiveSettingFetcher;

class CaptchaWebFilterTest {

    @ParameterizedTest
    @ValueSource(strings = {"/login", "/registration"})
    void acceptsSixCharacterCaptchaOnAuthenticationPaths(String path) {
        ReactiveSettingFetcher settingFetcher = mock(ReactiveSettingFetcher.class);
        CaptchaSetting setting = localSetting(6);
        when(settingFetcher.fetch(eq(CaptchaSetting.GROUP), eq(CaptchaSetting.class)))
            .thenReturn(Mono.just(setting));

        CaptchaCodeStore codeStore = new CaptchaCodeStore();
        try {
            String token = CaptchaCodeStore.generateToken();
            String code = "ABCDEF";
            codeStore.store(token, code);

            CaptchaWebFilter filter = new CaptchaWebFilter(
                settingFetcher,
                List.of(new LocalCaptchaVerifier(codeStore)),
                codeStore
            );
            MockServerHttpRequest request = MockServerHttpRequest.post(path)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .cookie(new HttpCookie("captcha_token", token))
                .body("captcha_code=" + code);
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            AtomicBoolean authenticationReached = new AtomicBoolean();
            WebFilterChain chain = filteredExchange -> {
                authenticationReached.set(true);
                filteredExchange.getResponse().setStatusCode(HttpStatus.OK);
                return filteredExchange.getResponse().setComplete();
            };

            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

            assertThat(authenticationReached).isTrue();
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
        } finally {
            codeStore.destroy();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/login", "/registration"})
    void injectsConfiguredInputLengthOnAuthenticationPages(String path) {
        ReactiveSettingFetcher settingFetcher = mock(ReactiveSettingFetcher.class);
        CaptchaSetting setting = localSetting(6);
        when(settingFetcher.fetch(eq(CaptchaSetting.GROUP), eq(CaptchaSetting.class)))
            .thenReturn(Mono.just(setting));

        CaptchaWebFilter filter = new CaptchaWebFilter(
            settingFetcher,
            List.of(),
            mock(CaptchaCodeStore.class)
        );
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get(path).build()
        );
        WebFilterChain chain = filteredExchange -> {
            filteredExchange.getResponse().getHeaders().setContentType(MediaType.TEXT_HTML);
            byte[] html = "<html><body><form><input type=\"password\"></form></body></html>"
                .getBytes();
            return filteredExchange.getResponse().writeWith(Mono.just(
                filteredExchange.getResponse().bufferFactory().wrap(html)
            ));
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        String body = exchange.getResponse().getBodyAsString().block();
        assertThat(body)
            .contains("\"localCaptchaLength\":6")
            .contains("input.maxLength=codeLength")
            .contains("input.minLength=codeLength");
    }

    private static CaptchaSetting localSetting(int length) {
        CaptchaSetting setting = new CaptchaSetting();
        setting.setEnabled(true);
        setting.setProvider("local");
        setting.setLocalCaptchaLength(length);
        return setting;
    }
}
