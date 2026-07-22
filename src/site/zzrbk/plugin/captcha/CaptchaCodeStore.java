package site.zzrbk.plugin.captcha;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

public class CaptchaCodeStore {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_CAPACITY = 10000;
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final ScheduledExecutorService CLEANER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "captcha-cleaner");
                t.setDaemon(true);
                return t;
            });

    private final ConcurrentMap<String, CodeEntry> store = new ConcurrentHashMap<>();

    public CaptchaCodeStore() {
        CLEANER.scheduleAtFixedRate(this::cleanExpired, 60, 60, TimeUnit.SECONDS);
    }

    public static String generateToken() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(32);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public String generateCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    public boolean store(String token, String code) {
        if (store.size() >= MAX_CAPACITY) {
            return false;
        }
        store.put(token, new CodeEntry(code, System.currentTimeMillis()));
        return true;
    }

    public String getAndRemove(String token) {
        CodeEntry entry = store.remove(token);
        return entry != null ? entry.code : null;
    }

    public byte[] generateImage(String code, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Background gradient
            for (int y = 0; y < height; y++) {
                float ratio = (float) y / height;
                int r = (int) (240 + ratio * 15);
                int green = (int) (240 + ratio * 15);
                int b = (int) (248 + ratio * 7);
                g.setColor(new Color(r, green, b));
                g.drawLine(0, y, width, y);
            }

            // Noise dots
            for (int i = 0; i < 80; i++) {
                int x = RANDOM.nextInt(width);
                int y = RANDOM.nextInt(height);
                g.setColor(new Color(150 + RANDOM.nextInt(80), 150 + RANDOM.nextInt(80), 150 + RANDOM.nextInt(80)));
                g.fillOval(x, y, 2, 2);
            }

            // Interference lines
            for (int i = 0; i < 5; i++) {
                int x1 = RANDOM.nextInt(width);
                int y1 = RANDOM.nextInt(height);
                int x2 = RANDOM.nextInt(width);
                int y2 = RANDOM.nextInt(height);
                g.setColor(new Color(160 + RANDOM.nextInt(60), 160 + RANDOM.nextInt(60), 160 + RANDOM.nextInt(60)));
                g.drawLine(x1, y1, x2, y2);
            }

            // Draw characters
            int charWidth = (width - 20) / code.length();
            for (int i = 0; i < code.length(); i++) {
                String ch = String.valueOf(code.charAt(i));
                int fontSize = 28 + RANDOM.nextInt(10);
                Font font = new Font("Arial", Font.BOLD, fontSize);
                g.setFont(font);

                // Random color per character
                g.setColor(new Color(20 + RANDOM.nextInt(80), 20 + RANDOM.nextInt(120), 120 + RANDOM.nextInt(80)));

                // Position with jitter
                int x = 10 + i * charWidth + RANDOM.nextInt(6);
                int y = height / 2 + fontSize / 3 + RANDOM.nextInt(6);

                // Random rotation
                double angle = (RANDOM.nextDouble() - 0.5) * 0.3;
                g.rotate(angle, x + charWidth / 2.0, y - fontSize / 3.0);
                g.drawString(ch, x, y);
                g.rotate(-angle, x + charWidth / 2.0, y - fontSize / 3.0);
            }

            // Border
            g.setColor(new Color(180, 180, 200));
            g.drawRect(0, 0, width - 1, height - 1);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate captcha image", e);
        } finally {
            g.dispose();
        }
    }

    private void cleanExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> now - e.getValue().createdAt > Duration.ofMinutes(5).toMillis());
    }

    private static class CodeEntry {
        final String code;
        final long createdAt;

        CodeEntry(String code, long createdAt) {
            this.code = code;
            this.createdAt = createdAt;
        }
    }
}
