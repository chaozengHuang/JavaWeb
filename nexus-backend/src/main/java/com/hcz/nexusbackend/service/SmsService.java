package com.hcz.nexusbackend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SmsService {

    // 互亿无线短信平台配置
    private static final String API_ID = "C06660428";
    private static final String API_KEY = "9d82c2d907f1b293aff5afba65293221";
    private static final String API_URL = "https://api.ihuyi.com/sms/Submit.json";
    private static final int CODE_EXPIRE_MINUTES = 10;

    private final Map<String, CodeInfo> codeStore = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private static class CodeInfo {
        String code;
        long expireTime;
        CodeInfo(String code, long expireTime) { this.code = code; this.expireTime = expireTime; }
    }

    public boolean sendVerifyCode(String phone) {
        String code = String.format("%06d", random.nextInt(1000000));
        long expireTime = System.currentTimeMillis() + CODE_EXPIRE_MINUTES * 60 * 1000;
        codeStore.put(phone, new CodeInfo(code, expireTime));

        try {
            String content = "您的验证码是：" + code + "。请不要把验证码泄露给其他人。";

            // 构建 POST 参数
            String postData = "account=" + URLEncoder.encode(API_ID, StandardCharsets.UTF_8)
                    + "&password=" + URLEncoder.encode(API_KEY, StandardCharsets.UTF_8)
                    + "&mobile=" + URLEncoder.encode(phone, StandardCharsets.UTF_8)
                    + "&content=" + URLEncoder.encode(content, StandardCharsets.UTF_8);

            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(postData.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            String resp = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            log.info("互亿无线短信发送: phone={}, httpStatus={}, response={}", phone, status, resp);

            // code=2 表示提交成功
            if (status == 200 && resp.contains("\"code\":2")) {
                return true;
            }
            // 即使 API 返回失败，验证码仍可用（日志可查）
            log.warn("短信API返回非成功状态，验证码仍保存在内存中: phone={}, code={}", phone, code);
            return false;
        } catch (Exception e) {
            log.error("短信发送异常: phone={}", phone, e);
            // 异常时验证码仍可用
            log.info("=== 验证码(手机={}): {} , 通过日志查看 ===", phone, code);
            return false;
        }
    }

    public boolean verifyCode(String phone, String code) {
        CodeInfo info = codeStore.get(phone);
        if (info == null) return false;
        if (System.currentTimeMillis() > info.expireTime) {
            codeStore.remove(phone);
            return false;
        }
        if (info.code.equals(code)) {
            codeStore.remove(phone);
            return true;
        }
        return false;
    }
}
