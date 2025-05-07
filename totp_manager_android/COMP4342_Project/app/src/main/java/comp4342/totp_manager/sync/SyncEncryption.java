package comp4342.totp_manager.sync;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.util.Base64;

import android.util.Log;

public class SyncEncryption {
    public static byte[] hexStringToByteArray(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    public static String encrypt(String iv, String userSecret, String database) throws Exception {
//        Log.i("Encryption", "iv="+iv+";user_secret="+userSecret+";data="+database);

        // Generate EKey using SHA3-256(IV || userSecret)
        String concatenatedInput = iv + userSecret;
        byte[] eKeyBytes = sha_256(concatenatedInput);

        // Initialize AES cipher in CBC mode with the EKey and IV
        SecretKeySpec secretKeySpec = new SecretKeySpec(eKeyBytes, "AES");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(hexStringToByteArray(iv));

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

        byte[] encryptedBytes = cipher.doFinal(database.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
    }

    public static String decrypt(String iv, String userSecret, String encryptedData) throws Exception {
        // Generate EKey using SHA3-256(IV || userSecret)
        String concatenatedInput = iv + userSecret;
        byte[] eKeyBytes = sha_256(concatenatedInput);

        // Initialize AES cipher in CBC mode with the EKey and IV
        SecretKeySpec secretKeySpec = new SecretKeySpec(eKeyBytes, "AES");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(hexStringToByteArray(iv));

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

        byte[] decryptedBytes = cipher.doFinal(Base64.decode(encryptedData, Base64.DEFAULT));
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    private static byte[] sha_256(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(input.getBytes(StandardCharsets.UTF_8));
    }

}
