package run.halo.captcha;

public enum CaptchaProvider {
    CLOUDFLARE,
    GEETEST,
    LOCAL;

    public static CaptchaProvider from(String name) {
        if ("geetest".equalsIgnoreCase(name)) {
            return GEETEST;
        }
        if ("cloudflare".equalsIgnoreCase(name)) {
            return CLOUDFLARE;
        }
        if ("local".equalsIgnoreCase(name)) {
            return LOCAL;
        }
        return LOCAL;
    }
}
