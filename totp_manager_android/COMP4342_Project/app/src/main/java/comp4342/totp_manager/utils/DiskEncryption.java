// Removed references related to "bio" while maintaining functionality and code integrity.
package comp4342.totp_manager.utils;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Utility class for generating AES-256-CBC keys, storing them securely using Android Keystore with
 * biometric authentication, and performing encryption and decryption.
 */
public class DiskEncryption {

    private static final String KEY_ALIAS = "AES256KeyStorage";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String IV_FILE_NAME = "encryption_iv";
    private static final String CIPHERTEXT_FILE_NAME = "encrypted_data";

    private final Context context;

    private static final String AES256_LOG_PREFIX = "======::=====AES256_Storage";

    public DiskEncryption(Context context) {
        this.context = context;
    }

    /**
     * Generates an AES-256 key with biometric authentication required for every use.
     */
    public void generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        keyGenerator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        keyGenerator.generateKey();
    }

    /**
     * Retrieves the SecretKey from the Android Keystore.
     */
    private SecretKey getSecretKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
        } catch (Exception e) {
            Log.e(AES256_LOG_PREFIX, "Get Key Error", e);
            return null;
        }
    }

    /**
     * Encrypts the provided plaintext using the AES key with biometric authentication.
     *
     * @param plaintext The plaintext to encrypt.
     * @param activity  The activity from which this method is called.
     */
    public void encryptData(String plaintext, FragmentActivity activity) throws Exception {
//        Log.i(AES256_LOG_PREFIX, "Try Encrypt Data: "+plaintext);
        SecretKey secretKey = getSecretKey();
        if (secretKey == null) {
            generateKey();
            secretKey = getSecretKey();
        }

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] ciphertext = cipher.doFinal(
                plaintext.getBytes(StandardCharsets.UTF_8));
        byte[] iv = cipher.getIV();
        storeIvToDisk(iv);
        storeCiphertextToDisk(ciphertext);
    }

    public byte[] decryptData(FragmentActivity activity) throws Exception {
        SecretKey secretKey = getSecretKey();
        if (secretKey == null) {
            throw new IllegalStateException("Secret key not found. Generate the key first.");
        }

        byte[] iv = readIvFromDisk();
        byte[] ciphertext = readCiphertextFromDisk();
        if (iv == null || ciphertext == null) {
            throw new IllegalStateException("IV or ciphertext not found on disk.");
        }

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

        return cipher.doFinal(ciphertext);
    }


    /**
     * Stores the IV to local disk.
     *
     * @param iv The initialization vector to store.
     */
    private void storeIvToDisk(byte[] iv) {
        try (FileOutputStream fos = context.openFileOutput(IV_FILE_NAME, Context.MODE_PRIVATE)) {
            fos.write(iv);
        } catch (Exception e) {
            Log.e(AES256_LOG_PREFIX, "Save IV to DISK Failed", e);
        }
    }

    /**
     * Reads the IV from local disk.
     *
     * @return The initialization vector, or null if not found.
     */
    private byte[] readIvFromDisk() {
        try (FileInputStream fis = context.openFileInput(IV_FILE_NAME)) {
            byte[] iv = new byte[12];
            int bytesRead = fis.read(iv);
            if (bytesRead == 12) {
                return iv;
            } else {
                throw new IllegalStateException("Invalid IV size read from disk. size="+bytesRead);
            }
        } catch (Exception e) {
            Log.e(AES256_LOG_PREFIX, "Load IV from DISK Failed", e);
            return null;
        }
    }

    /**
     * Stores the ciphertext to local disk.
     *
     * @param ciphertext The encrypted data to store.
     */
    private void storeCiphertextToDisk(byte[] ciphertext) {
        try (FileOutputStream fos = context.openFileOutput(CIPHERTEXT_FILE_NAME, Context.MODE_PRIVATE)) {
            fos.write(ciphertext);
        } catch (Exception e) {
            Log.e(AES256_LOG_PREFIX, "Save Ciphertext to DISK Failed", e);
        }
    }

    /**
     * Reads the ciphertext from local disk.
     *
     * @return The encrypted data, or null if not found.
     */
    private byte[] readCiphertextFromDisk() {
        try (FileInputStream fis = context.openFileInput(CIPHERTEXT_FILE_NAME)) {
            int available = fis.available();
            byte[] ciphertext = new byte[available];
            int bytesRead = fis.read(ciphertext);
            if (bytesRead == available) {
                return ciphertext;
            } else {
                throw new IllegalStateException("Error reading ciphertext from disk.");
            }
        } catch (Exception e) {
            Log.e(AES256_LOG_PREFIX, "Load CipherText from DISK Failed", e);
            return null;
        }
    }

}