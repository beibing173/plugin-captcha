package site.zzrbk.plugin.captcha;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

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
        try {
            CaptchaCodeStore codeStore = pluginContext.getApplicationContext()
                    .getBean(CaptchaCodeStore.class);
            if (codeStore != null) {
                codeStore.destroy();
                log.info("PluginCaptcha: cleanup task shut down");
            }
        } catch (Exception e) {
            log.warn("PluginCaptcha: failed to shut down cleanup task: {}", e.getMessage());
        }
    }
}
