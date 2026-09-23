package com.example.ordermanage.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;

@Service
public class CaptchaService {

    private static final long TTL_MS = 5 * 60 * 1000L;
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private record Entry(String code, long expireAt) {
    }

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

    public Map<String, String> create() {
        StringBuilder sb = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            sb.append(CHARS.charAt(ThreadLocalRandom.current().nextInt(CHARS.length())));
        }
        return create(sb.toString());
    }

    public Map<String, String> create(String code) {
        String id = "cap_" + UUID.randomUUID().toString().substring(0, 8);
        store.put(id, new Entry(code.toLowerCase(), System.currentTimeMillis() + TTL_MS));
        purgeExpired();
        return Map.of("captcha_id", id, "image", "data:image/png;base64," + renderPng(code));
    }

    /** Case-insensitive check; consumes the captcha entry. */
    public boolean validate(String captchaId, String code) {
        if (captchaId == null || code == null) {
            return false;
        }
        Entry e = store.remove(captchaId);
        if (e == null) {
            return false;
        }
        if (System.currentTimeMillis() > e.expireAt()) {
            return false;
        }
        return e.code().equalsIgnoreCase(code.trim());
    }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(en -> now > en.getValue().expireAt());
    }

    private String renderPng(String code) {
        BufferedImage image = new BufferedImage(120, 40, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(new Color(245, 245, 250));
            g.fillRect(0, 0, 120, 40);
            g.setFont(new Font("SansSerif", Font.BOLD, 26));
            int x = 12;
            for (int i = 0; i < code.length(); i++) {
                g.setColor(new Color(30 + i * 40, 60 + i * 30, 120 + i * 20));
                g.drawString(String.valueOf(code.charAt(i)), x, 30);
                x += 26;
            }
            g.setColor(new Color(180, 180, 200));
            for (int i = 0; i < 6; i++) {
                g.drawLine(i * 20, 0, i * 20 + 15, 40);
            }
        } finally {
            g.dispose();
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("captcha render failed", e);
        }
    }
}
