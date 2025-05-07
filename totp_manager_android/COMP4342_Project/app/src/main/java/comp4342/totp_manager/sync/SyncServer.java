package comp4342.totp_manager.sync;
import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import android.util.Log;

import androidx.core.util.Consumer;

public class SyncServer {
    public String username="";
    public String skk="";
    public String user_secret="";
    public static SyncServer global_server = null;

    private static final String TAG = "======::=====SyncServer";


    public SyncServer(Context context) throws Exception {
        String[] u_uinfo;

        try {
            // 尝试从 CredentialManagement 中加载密钥
            u_uinfo = CredentialManagement.loadKeys(context);
            username = u_uinfo[0];
            skk = u_uinfo[1];
            user_secret = u_uinfo[2];


        } catch (Exception e) {
            // 如果加载失败（例如首次使用），则创建用户并让用户输入密码
            u_uinfo = SyncConnection.createUser();
            String[] finalU_uinfo = u_uinfo;

            username = u_uinfo[0];
            skk = u_uinfo[1];

            getPasswordFromUser(context, new Consumer<String>() {
                @Override
                public void accept(String password) {
                    try {
                        user_secret = password;
                        CredentialManagement.storeKeys(context, finalU_uinfo[0], finalU_uinfo[1], password);

                    } catch (Exception ex) {
                        Log.e(TAG, "Sync Key Store Failed", e);
                        throw new RuntimeException(ex);
                    }
                    Log.i(TAG, "Sync Key Stored! ");
                }
            });
        }
    }

    public SyncServer(Context context, Runnable callback) {
        String[] u_uinfo;

        try {
            // 尝试从 CredentialManagement 中加载密钥
            u_uinfo = CredentialManagement.loadKeys(context);
            username = u_uinfo[0];
            skk = u_uinfo[1];
            user_secret = u_uinfo[2];

        } catch (Exception e) {
            getPasswordFromUser(context, password -> {
                try {
                    user_secret = password;
                    String[] finalU_uinfo = SyncConnection.createUser();
                    username = finalU_uinfo[0];
                    skk = finalU_uinfo[1];
                    CredentialManagement.storeKeys(context, finalU_uinfo[0], finalU_uinfo[1], password);
                    Log.i(TAG, "Sync Key Stored");
                    if (callback != null) {
                        callback.run();
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Create QR Failed", ex);
                    Toast.makeText(context, "Cannot Connect to the server! Sync Code Create Failed",
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }


    public SyncServer(Context context, String uid, String _skk, String _user_secret) throws Exception {
            username = uid;
            skk = _skk;
            user_secret = _user_secret;
            CredentialManagement.storeKeys(context, username, skk, user_secret);
    }

    private void getPasswordFromUser(Context context, Consumer<String> callback) {
        Log.i(TAG, "Wait for user's password");
        final EditText passwordInput = new EditText(context);
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setHint("Enter your password (min 10 characters)");

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Password Required")
                .setMessage("Please enter a password (at least 10 characters):")
                .setView(passwordInput)
                .setPositiveButton("OK", (dialogInterface, i) -> {
                    String input = passwordInput.getText().toString();

                    if (input.length() >= 10) {
                        callback.accept(input); // 返回密码
                    } else {
                        Toast.makeText(context, "Password must be at least 10 characters long", Toast.LENGTH_SHORT).show();
                        getPasswordFromUser(context, callback);
                    }
                })
                .setNegativeButton("Cancel", (dialogInterface, i) -> {
                    dialogInterface.cancel();
                })
                .setCancelable(false)
                .create();

        dialog.show();
    }

    public void upload(String data) throws Exception {
        if (user_secret == null) {
            throw new Exception("User secret is not set");
        }
        String encrypted = SyncEncryption.encrypt(skk, user_secret, data);
        SyncConnection.upload(username, encrypted);
    }

    public String sync() throws Exception {
        if (user_secret == null) {
            throw new Exception("User secret is not set");
        }
        String encrypted = SyncConnection.sync(username);
        return SyncEncryption.decrypt(skk, user_secret, encrypted);
    }
}
