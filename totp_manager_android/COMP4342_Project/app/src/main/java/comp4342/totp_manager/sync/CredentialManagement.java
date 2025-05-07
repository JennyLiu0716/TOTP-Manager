package comp4342.totp_manager.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class CredentialManagement {
    private static final String KEY_ALIAS = "CredentialManagement_KEY";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String SHARED_PREFS_NAME = "CredentialPrefs";
    private static final String ENCRYPTED_DATA_KEY = "CredentialManagement_KEY";

    // 初始化 AES 加密密钥
    public static void generateEncryptionKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            keyGenerator.init(new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build());
            keyGenerator.generateKey();
            Log.i("CredentialManagement", "AES Encryption Key Generated");
        }
    }

    // 存储加密的用户信息
    public static void storeKeys(Context context, String username, String skk, String userPassword) throws Exception {
        generateEncryptionKey();

        // 将 skk 和 userPassword 用 "|" 拼接
        String combinedPrivateKey = skk + "|" + userPassword;

        // 加密组合密钥
        String encryptedData = encryptData(username + "|" + combinedPrivateKey);

        // 将密文存储到 SharedPreferences 中
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(ENCRYPTED_DATA_KEY, encryptedData).apply();

        Log.i("CredentialManagement", "Sync Key Stored!");
    }

    // 加载并解密用户信息
    public static String[] loadKeys(Context context) throws Exception {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        String encryptedData = sharedPreferences.getString(ENCRYPTED_DATA_KEY, null);

        if (encryptedData == null) {
            throw new Exception("No key for Sync");
        }

        // 解密数据
        String decryptedData = decryptData(encryptedData);

        // 拆分解密后的数据
        String[] parts = decryptedData.split("\\|", 3); // 限制为 3 部分，防止误分隔
        if (parts.length != 3) {
            throw new Exception("Wrong Data Format for Key Storage");
        }

        String username = parts[0];
        String skk = parts[1];
        String userPassword = parts[2];

        return new String[]{username, skk, userPassword};
    }

    // 使用密钥加密数据
    private static String encryptData(String data) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        SecretKey secretKey = (SecretKey) keyStore.getKey(KEY_ALIAS, null);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        // 获取 GCM 初始化向量 (IV)
        byte[] iv = cipher.getIV();

        // 执行加密
        byte[] encryptedData = cipher.doFinal(data.getBytes());

        // 将 IV 和加密后的数据拼接在一起
        byte[] combined = new byte[iv.length + encryptedData.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedData, 0, combined, iv.length, encryptedData.length);

        return Base64.encodeToString(combined, Base64.DEFAULT);
    }

    // 使用密钥解密数据
    private static String decryptData(String encryptedData) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        SecretKey secretKey = (SecretKey) keyStore.getKey(KEY_ALIAS, null);

        byte[] combined = Base64.decode(encryptedData, Base64.DEFAULT);

        // 提取 IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);

        // 提取加密数据
        byte[] encryptedBytes = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, GCM_IV_LENGTH, encryptedBytes, 0, encryptedBytes.length);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

        // 解密数据
        byte[] decryptedData = cipher.doFinal(encryptedBytes);
        return new String(decryptedData);
    }

    // 清空 KeyStore 和 SharedPreferences
    public static void clearCredentials(Context context) {
        try {
            // 清空 KeyStore 中的密钥
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS);
                Log.i("CredentialManagement", "KeyStore has been deleted");
            }

            // 清空 SharedPreferences 中的数据
            SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(ENCRYPTED_DATA_KEY);
            editor.apply();

            Log.i("CredentialManagement", "SharedPreferences has been cleared");
        } catch (Exception e) {
            Log.e("CredentialManagement", "Error when clear the key stored", e);
        }
    }
}
