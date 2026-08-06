package site.zzrbk.plugin.captcha;

public class CaptchaSetting {

    public static final String GROUP = "basic";
    public static final int LOCAL_CAPTCHA_LENGTH = 6;

    private String provider = "local";
    private Boolean enabled = true;
    private String geetestId = "";
    private String geetestKey = "";
    private String cloudflareSiteKey = "";
    private String cloudflareSecretKey = "";
    private Integer localCaptchaLength = LOCAL_CAPTCHA_LENGTH;
    private Integer localCaptchaWidth = 130;
    private Integer localCaptchaHeight = 48;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public String getGeetestId() { return geetestId; }
    public void setGeetestId(String geetestId) { this.geetestId = geetestId; }

    public String getGeetestKey() { return geetestKey; }
    public void setGeetestKey(String geetestKey) { this.geetestKey = geetestKey; }

    public String getCloudflareSiteKey() { return cloudflareSiteKey; }
    public void setCloudflareSiteKey(String cloudflareSiteKey) { this.cloudflareSiteKey = cloudflareSiteKey; }

    public String getCloudflareSecretKey() { return cloudflareSecretKey; }
    public void setCloudflareSecretKey(String cloudflareSecretKey) { this.cloudflareSecretKey = cloudflareSecretKey; }

    public Integer getLocalCaptchaLength() { return localCaptchaLength; }
    public void setLocalCaptchaLength(Integer localCaptchaLength) { this.localCaptchaLength = localCaptchaLength; }

    public int getValidatedLocalCaptchaLength() {
        return LOCAL_CAPTCHA_LENGTH;
    }

    public Integer getLocalCaptchaWidth() { return localCaptchaWidth; }
    public void setLocalCaptchaWidth(Integer localCaptchaWidth) { this.localCaptchaWidth = localCaptchaWidth; }

    public Integer getLocalCaptchaHeight() { return localCaptchaHeight; }
    public void setLocalCaptchaHeight(Integer localCaptchaHeight) { this.localCaptchaHeight = localCaptchaHeight; }

    public CaptchaProvider getProviderEnum() {
        return CaptchaProvider.from(provider);
    }
}
