package comp4342.totp_manager;


import static comp4342.totp_manager.HomeFragment.INIT_BEFORE_KEY;
import static comp4342.totp_manager.SyncCodeFragment.FIRST_TIME_SYNC_KEY;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.journeyapps.barcodescanner.CaptureActivity;

import comp4342.totp_manager.sync.CredentialManagement;
import comp4342.totp_manager.sync.SyncServer;

public class ScanCodeFragment extends Fragment {
    private static final String PREFS_NAME = "AppPrefs";

    private static final String FIRST_LAUNCH_KEY = "firstLaunch";

    private static final int SCAN_QR_REQUEST_CODE = 1001;
    private final String SCANSYNC_LOG_PREFIX = "======::=====ScanQRCode";
    private String global_password = null;
    private String uid = null;
    private String skk = null;

    public static View root;

    public static SharedPreferences sharedPreferences;


    @SuppressLint("SetTextI18n")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_scansync, container, false);
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean firstLaunch = sharedPreferences.getBoolean(FIRST_LAUNCH_KEY, true);

        TextView myTextView = root.findViewById(R.id.my_textview);

        Button scanButton = root.findViewById(R.id.scan_qr_button);
        if (firstLaunch) {
            // Set up the scan button
            scanButton.setOnClickListener(v -> {
                // Start the QR code scanning activity
                Intent intent = new Intent(getActivity(), CaptureActivity.class);
                startActivityForResult(intent, SCAN_QR_REQUEST_CODE);
            });
            myTextView.setVisibility(View.VISIBLE); // Show the TextView
        }else {
            myTextView.setText("Synchronization to other devices is disabled as the system is not empty or has been in synchronized mode");
            scanButton.setVisibility(View.VISIBLE);
            scanButton.setVisibility(View.INVISIBLE);
        }

        return root;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SCAN_QR_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Extract the scanned QR code data
            String scannedData = data.getStringExtra("SCAN_RESULT");
            assert scannedData != null;
            try {
                String[] separated = scannedData.split("\\|");
                uid = separated[0];
                skk = separated[1];
//                Log.i(SCANSYNC_LOG_PREFIX, "From QRCode: uid="+uid+";skk="+skk);
                showPasswordPrompt();
            }catch (Exception e) {
                Toast.makeText(requireContext(), "Invalid QR code", Toast.LENGTH_SHORT).show();
            }

        }
    }


    private void switchToHomeFragment() {
        NavController navController = NavHostFragment.findNavController(this); // Get NavController
        navController.navigate(R.id.nav_home); // Navigate to HomeFragment
    }

    /**
     * Displays an AlertDialog to prompt the user for their password.
     */
    private void showPasswordPrompt() {
        Activity activity = getActivity();
        if (activity == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Enter Password");

        // Create an input field for the password
        final EditText input = new EditText(activity);
        input.setHint("Enter your password");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        // Set up Confirm and Cancel buttons
        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String password = input.getText().toString();
            if (!password.isEmpty()) {
//                Log.i(SCANSYNC_LOG_PREFIX, "Password entered: " + password);
                global_password = password;
                try {
                    SyncServer.global_server = new SyncServer(requireContext(), uid, skk, global_password);
                } catch (Exception e) {
                    Log.e(SCANSYNC_LOG_PREFIX, "new sync server failed.", e);
                    Toast.makeText(requireContext(), "New sync server failed.", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    HomeFragment.SyncInitialData(requireActivity(), requireContext());
                    HomeFragment.sharedPreferences.edit().putBoolean(INIT_BEFORE_KEY, true).apply();
                    HomeFragment.sharedPreferences.edit().putBoolean(FIRST_LAUNCH_KEY, false).apply();
                    HomeFragment.sharedPreferences.edit().putBoolean(FIRST_TIME_SYNC_KEY, false).apply();

                    // 成功了
                    Log.i(SCANSYNC_LOG_PREFIX, "Initial Data Download Success!");
                    Toast.makeText(requireContext(), "Data Download Success!", Toast.LENGTH_SHORT).show();
                    switchToHomeFragment();
                } catch (Exception e) {
                    Log.e(SCANSYNC_LOG_PREFIX, "Cannot decrypt the data");
                    HomeFragment.sharedPreferences.edit().putBoolean(INIT_BEFORE_KEY, false).apply();
                    HomeFragment.sharedPreferences.edit().putBoolean(FIRST_LAUNCH_KEY, true).apply();
                    HomeFragment.sharedPreferences.edit().putBoolean(FIRST_TIME_SYNC_KEY, true).apply();

                    CredentialManagement.clearCredentials(requireContext());
                    Toast.makeText(requireContext(), "Sync Data Failed! Password or Network Error", Toast.LENGTH_LONG).show();
                }
            } else {
                Log.e(SCANSYNC_LOG_PREFIX, "Password is empty.");
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
            Log.i(SCANSYNC_LOG_PREFIX, "User canceled password input.");
        });

        builder.show();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}