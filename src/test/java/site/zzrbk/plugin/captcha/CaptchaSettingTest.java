package site.zzrbk.plugin.captcha;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CaptchaSettingTest {

    @Test
    void alwaysUsesSixCharacterCaptcha() {
        CaptchaSetting setting = new CaptchaSetting();

        assertThat(setting.getValidatedLocalCaptchaLength()).isEqualTo(6);

        setting.setLocalCaptchaLength(4);
        assertThat(setting.getValidatedLocalCaptchaLength()).isEqualTo(6);

        setting.setLocalCaptchaLength(10);
        assertThat(setting.getValidatedLocalCaptchaLength()).isEqualTo(6);
    }

    @Test
    void fallsBackWhenConfiguredLengthIsInvalid() {
        CaptchaSetting setting = new CaptchaSetting();

        setting.setLocalCaptchaLength(null);
        assertThat(setting.getValidatedLocalCaptchaLength()).isEqualTo(6);

        setting.setLocalCaptchaLength(0);
        assertThat(setting.getValidatedLocalCaptchaLength()).isEqualTo(6);

        setting.setLocalCaptchaLength(11);
        assertThat(setting.getValidatedLocalCaptchaLength()).isEqualTo(6);
    }
}
