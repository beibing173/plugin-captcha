package site.zzrbk.plugin.captcha;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

@Component
public class CaptchaPlugin extends BasePlugin {

    private static final Logger log = LoggerFactory.getLogger(CaptchaPlugin.class);

    public CaptchaPlugin(PluginContext context) {
        super(context);
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        log.info("PluginCaptcha stopped");
    }
}
