package comp4342.totp_manager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.DisplayMetrics;
import android.util.Log;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;


import android.content.Intent;
import android.view.View;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import android.content.SharedPreferences;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import android.text.TextUtils;

import java.util.Random;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import comp4342.totp_manager.databinding.ActivityMainBinding;
import comp4342.totp_manager.utils.ChartData;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    private static final String PREFS_NAME = "AppPrefs";
    private static final String FIRST_LAUNCH_KEY = "firstLaunch";
    private static final String ACTIVITY_LOG_PREFIX = "======::=====MainActivity";

    private SharedPreferences sharedPreferences;

    private float dX, dY;
    private int lastAction;
    private long touchStartTime;
    private static final int CLICK_THRESHOLD = 200; // 判断为点击的时间阈值（毫秒）
    private static final int DRAG_THRESHOLD = 10;  // 判断为拖动的距离阈值（像素）
    private int screenWidth, screenHeight;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Disable screenshots
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        bioFirstAuth();
    }

    public void bioFirstAuth() {
        // Initialize the executor
        Executor executor = ContextCompat.getMainExecutor(this);

        // Initialize BiometricPrompt
        BiometricPrompt biometricPrompt = new BiometricPrompt(MainActivity.this,
                executor, new BiometricPrompt.AuthenticationCallback() {

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                // User has authenticated successfully
                Toast.makeText(MainActivity.this, "Authentication succeeded!", Toast.LENGTH_SHORT).show();
                // Proceed with your app's functionality here
                setSupportActionBar(binding.appBarMain.toolbar);
                setContentView(binding.getRoot());
                binding.appBarMain.fab.setOnClickListener(v -> {
                    IntentIntegrator intentIntegrator = new IntentIntegrator(MainActivity.this);
                    intentIntegrator.setPrompt("Scan a barcode or QR Code");
                    intentIntegrator.setOrientationLocked(true);
                    intentIntegrator.initiateScan();
                });

                DrawerLayout drawer = binding.drawerLayout;
                NavigationView navigationView = binding.navView;

                // Passing each menu ID as a set of Ids because each
                // menu should be considered as top level destinations.
                mAppBarConfiguration = new AppBarConfiguration.Builder(
                        R.id.nav_home, R.id.nav_showSyncCode, R.id.nav_slideshow)
                        .setOpenableLayout(drawer)
                        .build();
                NavController navController = Navigation.findNavController(MainActivity.this, R.id.nav_host_fragment_content_main);
                NavigationUI.setupActionBarWithNavController(MainActivity.this, navController, mAppBarConfiguration);
                NavigationUI.setupWithNavController(navigationView, navController);


                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                screenWidth = displayMetrics.widthPixels;
                screenHeight = displayMetrics.heightPixels;


                FloatingActionButton fab = findViewById(R.id.fab);
                // 设置拖动事件监听器

                // 设置拖动和点击事件
                fab.setOnTouchListener(new View.OnTouchListener() {
                    private float startX, startY; // 记录触摸开始位置

                    @Override
                    public boolean onTouch(View view, MotionEvent event) {
                        switch (event.getActionMasked()) {
                            case MotionEvent.ACTION_DOWN:
                                // 记录触摸起点和时间
                                dX = view.getX() - event.getRawX();
                                dY = view.getY() - event.getRawY();
                                startX = event.getRawX();
                                startY = event.getRawY();
                                touchStartTime = System.currentTimeMillis();
                                lastAction = MotionEvent.ACTION_DOWN;
                                return true;

                            case MotionEvent.ACTION_MOVE:
                                // 更新 FloatingActionButton 的位置
                                float newX = event.getRawX() + dX;
                                float newY = event.getRawY() + dY;

                                // 应用边界限制
                                newX = Math.max(0, Math.min(newX, screenWidth - view.getWidth()));
                                newY = Math.max(0, Math.min(newY, screenHeight - view.getHeight()));

                                view.setX(newX);
                                view.setY(newY);
                                lastAction = MotionEvent.ACTION_MOVE;
                                return true;

                            case MotionEvent.ACTION_UP:
                                // 判断是否是点击事件
                                long touchDuration = System.currentTimeMillis() - touchStartTime;
                                float deltaX = Math.abs(event.getRawX() - startX);
                                float deltaY = Math.abs(event.getRawY() - startY);

                                if (touchDuration < CLICK_THRESHOLD && deltaX < DRAG_THRESHOLD && deltaY < DRAG_THRESHOLD) {
                                    // 触发点击事件
                                    view.performClick();
                                }
                                return true;

                            default:
                                return false;
                        }
                    }
                });
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                // User failed the authentication
                Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                finish();
                System.exit(0);
            }

            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                // Check if the user canceled the authentication
                if (errorCode == BiometricPrompt.ERROR_CANCELED) {
                    // Handle the cancellation
                    Toast.makeText(MainActivity.this, "Authentication cancelled.", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    // Handle other types of authentication errors if needed
                    Toast.makeText(MainActivity.this, "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });

        BiometricPrompt.PromptInfo promptInfo;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) { // Android 11 or higher
            promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Authentication Required")
                    .setSubtitle("Log in using biometric or device credentials")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG |
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build();
        } else {
            // Fallback for Android 10 or lower
            promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Authentication Required")
                    .setSubtitle("Log in using biometric credentials")
                    .setNegativeButtonText("Cancel")
                    .build();
        }

        // Show the authentication prompt
        biometricPrompt.authenticate(promptInfo);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    // qr code
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
            } else {
                String scannedData = result.getContents(); // The scanned string
                String secret = extractSecret(scannedData);
                if (secret==null){
                    Toast.makeText(this, "Invalid qr code!", Toast.LENGTH_SHORT).show();
                    return;
                }
                promptUserForName(scannedData); // Handle the scanned data
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public static String extractSecret(String otpAuthUrl) {
        // Define the regular expression pattern to match the secret key
        String regex = "secret=([A-Z0-9]+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(otpAuthUrl);

        // Check if the pattern matches and extract the secret key
        if (matcher.find()) {
            return matcher.group(1); // Extracts the first group (the secret key)
        }
        return null; // If no match found, return null
    }


    private void promptUserForName(String scannedData) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Name the Information");

        // Input field for the name
        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = input.getText().toString();
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(this, "Name cannot be empty!", Toast.LENGTH_SHORT).show();
            } else {
                Random random = new Random();
                long randomLong = random.nextLong();
                String secret = extractSecret(scannedData);
                if (secret==null){
                    Toast.makeText(this, "Invalid qr code!", Toast.LENGTH_SHORT).show();
                    return;
                }
                ChartData newChartData = new ChartData(name, secret, System.currentTimeMillis(), randomLong, "Sync Information");
                //             Add the new ChartData to the list and update the adapter
                HomeFragment.chartDataList.add(newChartData);
                HomeFragment.current_view.add(newChartData);

                try {
                    HomeFragment.saveDataToDatabase(this, this);
                    sharedPreferences.edit().putBoolean(FIRST_LAUNCH_KEY, false).apply();
                } catch (Exception e) {
                    Log.e(ACTIVITY_LOG_PREFIX, "During adding a new element: FAILED SAVE DATA TO DATABASE",e);
                    throw new RuntimeException(e);
                }

            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Apply blur effect using a custom view or library
        View decorView = getWindow().getDecorView();
        decorView.setAlpha(0.5f); // Simulate a blur-like effect by reducing alpha
    }

    @Override
    protected void onResume() {
        super.onResume();
        View decorView = getWindow().getDecorView();
        decorView.setAlpha(1f); // Restore alpha
    }


}