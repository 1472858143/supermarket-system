package com.supermarket.inventory.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.inventory.auth.security.CurrentUser;
import com.supermarket.inventory.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JwtTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final String secret;
    private final long expirationHours;

    public JwtTokenService(
            ObjectMapper objectMapper,
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-hours}") long expirationHours
    ) {
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.expirationHours = expirationHours;
    }

    public String generateToken(CurrentUser user) {
        try {
            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", user.getUserId());
            payload.put("username", user.getUsername());
            payload.put("roles", user.getRoles());
            payload.put("exp", Instant.now().plusSeconds(expirationHours * 3600).getEpochSecond());

            String encodedHeader = base64Url(objectMapper.writeValueAsBytes(header));
            String encodedPayload = base64Url(objectMapper.writeValueAsBytes(payload));
            String signingInput = encodedHeader + "." + encodedPayload;
            String signature = sign(signingInput);
            return signingInput + "." + signature;
        } catch (Exception exception) {
            throw new BusinessException(500, "Token 生成失败");
        }
    }

    public CurrentUser parseToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new BusinessException(401, "Token 格式错误");
            }
            String signingInput = parts[0] + "." + parts[1];
            String expectedSignature = sign(signingInput);
            if (!constantTimeEquals(expectedSignature, parts[2])) {
                throw new BusinessException(401, "Token 签名无效");
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            Map<String, Object> payload = objectMapper.readValue(payloadBytes, new TypeReference<>() {});
            long exp = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() > exp) {
                throw new BusinessException(401, "Token 已过期");
            }

            Long userId = ((Number) payload.get("userId")).longValue();
            String username = (String) payload.get("username");
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) payload.get("roles");
            return new CurrentUser(userId, username, roles);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(401, "未登录或认证失败");
        }
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sign(String content) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
        return base64Url(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
    }

    private boolean constantTimeEquals(String left, String right) {
        return java.security.MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }
}
