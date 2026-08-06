package site.zzrbk.plugin.captcha;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class GeetestVerifierTest {

    @Test
    void doesNotLogCompleteProviderResponse(CapturedOutput output) {
        GeetestVerifier verifier = new GeetestVerifier();
        String sensitiveValue = "sensitive-lot-number";
        String response = "{\"result\":\"success\",\"captcha_args\":{\"lot_number\":\""
            + sensitiveValue + "\"}}";

        assertThat(verifier.parseResponse(response)).isTrue();
        assertThat(output.getAll()).doesNotContain(sensitiveValue);
    }
}
