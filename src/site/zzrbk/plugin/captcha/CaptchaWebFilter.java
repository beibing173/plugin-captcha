package site.zzrbk.plugin.captcha;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.app.security.AdditionalWebFilter;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Semaphore;

public class CaptchaWebFilter implements AdditionalWebFilter {

    private static final Logger log = LoggerFactory.getLogger(CaptchaWebFilter.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ReactiveSettingFetcher settingFetcher;
    private final List<CaptchaVerifier> verifiers;
    private final CaptchaCodeStore codeStore;

    // Rate limiting for captcha image: max requests per window
    private static final int MAX_IMAGE_PER_WINDOW = 60;
    private static final Duration RATE_WINDOW = Duration.ofMinutes(1);
    private final ConcurrentHashMap<String, AtomicInteger> imageRateMap = new ConcurrentHashMap<>();
    private final Semaphore imageConcurrencyLimit = new Semaphore(10);
    private Instant rateWindowStart = Instant.now();

    public CaptchaWebFilter(ReactiveSettingFetcher settingFetcher,
                            List<CaptchaVerifier> verifiers,
                            CaptchaCodeStore codeStore) {
        this.settingFetcher = settingFetcher;
        this.verifiers = verifiers;
        this.codeStore = codeStore;
        log.info("PluginCaptcha v1.0.0 filter loaded, verifiers={}", verifiers.size());
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE + 10;
    }

    private static boolean isCaptchaPage(String path) {
        return "/login".equals(path) || "/registration".equals(path)
                || "/register".equals(path) || "/signup".equals(path);
    }

    private synchronized boolean checkImageRate(String clientIp) {
        if (Duration.between(rateWindowStart, Instant.now()).compareTo(RATE_WINDOW) > 0) {
            imageRateMap.clear();
            rateWindowStart = Instant.now();
        }
        AtomicInteger count = imageRateMap.computeIfAbsent(clientIp, k -> new AtomicInteger(0));
        return count.incrementAndGet() <= MAX_IMAGE_PER_WINDOW;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();

        if (method == HttpMethod.POST && isCaptchaPage(path)) {
            return settingFetcher.fetch("basic", CaptchaSetting.class)
                    .timeout(Duration.ofSeconds(3))
                    .defaultIfEmpty(new CaptchaSetting())
                    .flatMap(setting -> {
                        if (!Boolean.TRUE.equals(setting.getEnabled())) {
                            return chain.filter(exchange);
                        }
                        return verifyCaptcha(exchange, setting)
                                .flatMap(valid -> {
                                    if (valid) {
                                        return chain.filter(exchange);
                                    }
                                    return reject(exchange, path);
                                });
                    })
                    .onErrorResume(e -> {
                        log.error("Captcha POST error - rejecting: {}", e.getMessage());
                        return reject(exchange, path);
                    });
        }

        if (method == HttpMethod.GET) {
            if ("/captcha_image".equals(path)) {
                return settingFetcher.fetch("basic", CaptchaSetting.class)
                        .timeout(Duration.ofSeconds(3))
                        .defaultIfEmpty(new CaptchaSetting())
                        .flatMap(setting -> {
                            if (!Boolean.TRUE.equals(setting.getEnabled())
                                    || setting.getProviderEnum() != CaptchaProvider.LOCAL) {
                                return rejectImage(exchange);
                            }
                            String ip = exchange.getRequest().getRemoteAddress() != null
                                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                                    : "unknown";
                            if (!checkImageRate(ip)) {
                                return rejectImage(exchange);
                            }
                            if (!imageConcurrencyLimit.tryAcquire()) {
                                return rejectImage(exchange);
                            }
                            return serveCaptchaImage(exchange, setting)
                                    .doFinally(s -> imageConcurrencyLimit.release());
                        })
                        .onErrorResume(e -> {
                            log.error("Captcha image error: {}", e.getMessage());
                            return rejectImage(exchange);
                        });
            }

            if (isCaptchaPage(path)) {
                return settingFetcher.fetch("basic", CaptchaSetting.class)
                        .timeout(Duration.ofSeconds(3))
                        .defaultIfEmpty(new CaptchaSetting())
                        .flatMap(setting -> {
                            if (!Boolean.TRUE.equals(setting.getEnabled())) {
                                return chain.filter(exchange);
                            }
                            return injectCaptcha(exchange, chain, setting);
                        })
                        .onErrorResume(e -> {
                            log.error("Captcha GET error - rejecting: {}", e.getMessage());
                            return reject(exchange, path);
                        });
            }
        }

        return chain.filter(exchange);
    }

    private Mono<Void> serveCaptchaImage(ServerWebExchange exchange, CaptchaSetting setting) {
        int length = setting.getLocalCaptchaLength() != null ? setting.getLocalCaptchaLength() : 4;
        int width = setting.getLocalCaptchaWidth() != null ? setting.getLocalCaptchaWidth() : 130;
        int height = setting.getLocalCaptchaHeight() != null ? setting.getLocalCaptchaHeight() : 48;

        if (length < 1 || length > 10) length = 4;
        if (width < 40 || width > 600) width = 130;
        if (height < 20 || height > 300) height = 48;

        String code = codeStore.generateCode(length);
        String token = CaptchaCodeStore.generateToken();
        if (!codeStore.store(token, code)) {
            return rejectImage(exchange);
        }

        byte[] imageBytes = codeStore.generateImage(code, width, height);

        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().setContentType(MediaType.IMAGE_PNG);
        response.getHeaders().set("Cache-Control", "no-store, no-cache, must-revalidate");
        response.getHeaders().set("Pragma", "no-cache");
        response.getHeaders().set("Expires", "0");

        response.addCookie(org.springframework.http.ResponseCookie.from("captcha_token", token)
                .path("/")
                .httpOnly(true)
                .sameSite("Lax")
                .maxAge(Duration.ofMinutes(5))
                .build());

        return response.writeWith(Mono.just(response.bufferFactory().wrap(imageBytes)));
    }

    private Mono<Void> rejectImage(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        return response.setComplete();
    }

    private Mono<Void> injectCaptcha(ServerWebExchange exchange, WebFilterChain chain,
                                     CaptchaSetting setting) {
        String configJson = buildConfigJson(setting);
        String captchaJsInline = getCaptchaJs();
        String injection = "<script>window.__captchaConfig=" + configJson
                + ";</script><script>" + captchaJsInline + "</script>";

        ServerHttpResponse originalResponse = exchange.getResponse();
        ServerHttpResponse decoratedResponse =
                new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                return DataBufferUtils.join(body).flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);

                    byte[] resultBytes;
                    try {
                        MediaType ct = getHeaders().getContentType();
                        boolean isHtml = ct != null
                                && ct.toString().toLowerCase().contains("text/html");

                        if (isHtml) {
                            String html = new String(bytes, StandardCharsets.UTF_8);
                            html = cleanPreviousInjections(html);
                            html = insertBeforeBody(html, injection);
                            resultBytes = html.getBytes(StandardCharsets.UTF_8);
                            getDelegate().getHeaders()
                                    .setContentLength(resultBytes.length);
                        } else {
                            resultBytes = bytes;
                        }
                    } catch (Exception e) {
                        log.error("Captcha inject error: {}", e.getMessage());
                        getDelegate().getHeaders().setContentLength(bytes.length);
                        resultBytes = bytes;
                    }

                    return getDelegate().writeWith(Mono.just(
                            getDelegate().bufferFactory().wrap(resultBytes)));
                });
            }

            @Override
            public Mono<Void> writeAndFlushWith(
                    Publisher<? extends Publisher<? extends DataBuffer>> body) {
                return writeWith(Flux.from(body).flatMapSequential(p -> p));
            }
        };

        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    private static String cleanPreviousInjections(String html) {
        html = html.replaceAll(
                "<script>window\\.__captchaConfig[^<]*</script>", "");
        html = html.replaceAll(
                "<script>[\\s\\S]*?\\[Captcha\\][\\s\\S]*?</script>", "");
        return html;
    }

    private static String insertBeforeBody(String html, String injection) {
        if (html.contains("</body>")) {
            return html.replace("</body>", injection + "\n</body>");
        }
        return html + injection;
    }

    private Mono<Boolean> verifyCaptcha(ServerWebExchange exchange, CaptchaSetting setting) {
        CaptchaProvider provider = setting.getProviderEnum();
        return verifiers.stream()
                .filter(v -> v.supports(provider))
                .findFirst()
                .map(v -> v.verify(exchange, setting))
                .orElse(Mono.just(false));
    }

    private String buildConfigJson(CaptchaSetting setting) {
        try {
            Map<String, Object> config = new LinkedHashMap<>();
            config.put("provider", setting.getProviderEnum().name().toLowerCase());
            config.put("enabled", setting.getEnabled());
            config.put("geetestId", setting.getGeetestId());
            config.put("cloudflareSiteKey", setting.getCloudflareSiteKey());
            config.put("captchaImageUrl", "/captcha_image");
            return OBJECT_MAPPER.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String getCaptchaJs() {
        try {
            var is = getClass().getClassLoader()
                    .getResourceAsStream("console/captcha.js");
            if (is != null) {
                String loaded = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                if (loaded.length() > 50) {
                    log.info("Loaded captcha.js from classpath ({} chars)", loaded.length());
                    return loaded;
                }
                log.warn("captcha.js too short: {} chars", loaded.length());
            } else {
                log.error("captcha.js NOT FOUND in classpath!");
            }
        } catch (Exception e) {
            log.error("Failed to load captcha.js: {}", e.getMessage());
        }
        return "console.error('[Captcha] captcha.js failed to load');";
    }

    private Mono<Void> reject(ServerWebExchange exchange, String pagePath) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FOUND);
        response.getHeaders().setLocation(URI.create(pagePath + "?captcha_error=1"));
        return response.setComplete();
    }
}
