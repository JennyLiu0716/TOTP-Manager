package comp4342.totp_manager.utils;

import java.security.Key;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base32;

public class TOTPGenerator {
    private static final String HMAC_SHA1 = "HmacSHA1";
    private static final int OTP_LENGTH = 6;

    public static String generateTOTP(String secretKey) throws Exception {
        // 获取当前时间（秒）
        long currentTimeSeconds = System.currentTimeMillis() / 1000;
        long timeStep = 30L;  // TOTP 标准时间步长（30秒）
        long counter = currentTimeSeconds / timeStep;

        // 使用 Base32 解码密钥
        Base32 base32 = new Base32();
        byte[] decodedKey = base32.decode(secretKey);

        // 准备 HMAC 密钥并执行 HMAC 计算
        SecretKeySpec signKey = new SecretKeySpec(decodedKey, HMAC_SHA1);
        Mac mac = Mac.getInstance(HMAC_SHA1);
        mac.init(signKey);
        byte[] hash = mac.doFinal(longToBytes(counter));

        // 从哈希值中提取 OTP
        int otp = extractOTP(hash);
        return String.format("%0" + OTP_LENGTH + "d", otp);
    }

    private static byte[] longToBytes(long value) {
        return new byte[] {
                (byte) (value >> 56),
                (byte) (value >> 48),
                (byte) (value >> 40),
                (byte) (value >> 32),
                (byte) (value >> 24),
                (byte) (value >> 16),
                (byte) (value >> 8),
                (byte) value
        };
    }

    private static int extractOTP(byte[] hash) {
        int offset = hash[hash.length - 1] & 0xf;
        int binaryCode = ((hash[offset] & 0x7f) << 24) |
                ((hash[offset + 1] & 0xff) << 16) |
                ((hash[offset + 2] & 0xff) << 8) |
                (hash[offset + 3] & 0xff);
        return binaryCode % 1000000;  // 生成6位数的OTP
    }
}