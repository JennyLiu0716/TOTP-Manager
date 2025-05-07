package comp4342.totp_manager;


import static comp4342.totp_manager.HomeFragment.INIT_BEFORE_KEY;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import android.util.Log;
import android.widget.Toast;

import comp4342.totp_manager.sync.SyncServer;


public class SyncCodeFragment extends Fragment {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String FIRST_LAUNCH_KEY = "firstLaunch";

    public static final String FIRST_TIME_SYNC_KEY = "firstTimeSync";
    private static final String SYNC_STRING = "======::====ScanCode"; // Replace with your actual sync code logic

    public static SharedPreferences sharedPreferences;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_showsyncode, container, false);
        ImageView qrCodeImageView = root.findViewById(R.id.qr_code_image_view);

        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isFirstTime = sharedPreferences.getBoolean(FIRST_TIME_SYNC_KEY, true);
        Log.i(SYNC_STRING,"Sync, isFirstTime="+isFirstTime);

        if (isFirstTime) {
            try {
                Log.i(SYNC_STRING, "Try create sync server");
                SyncServer.global_server = new SyncServer(requireContext(), () -> {
                    sharedPreferences.edit().putBoolean(FIRST_TIME_SYNC_KEY, false).apply();
                    HomeFragment.sharedPreferences.edit().putBoolean(INIT_BEFORE_KEY, true).apply();
                    Gson gson = new Gson();
                    String marshalledData = gson.toJson(HomeFragment.chartDataList);
                    try {
                        SyncServer.global_server.upload(marshalledData);
                        Log.i(SYNC_STRING,"server first call callback - upload success");

                        String username = SyncServer.global_server.username;
                        String skk = SyncServer.global_server.skk;
                        String info = username + "|" + skk;

                        // Generate and display QR code
                        Log.i(SYNC_STRING, "First Round QR Code Generate");
                        generateQRCode(qrCodeImageView, info);
                    } catch (Exception e) {
                        Log.e(SYNC_STRING,"server first call callback - upload failed",e);
                        Toast.makeText(requireContext(), "Cannot upload data", Toast.LENGTH_SHORT).show();
                    }
                });
                Log.i(SYNC_STRING, "After Try create sync server");
            } catch (Exception e) {
                Log.e(SYNC_STRING, "Create QR Failed", e);
                Toast.makeText(requireContext(), "Cannot Connect to the server! Sync Code Create Failed",
                        Toast.LENGTH_LONG).show();
            }
            return root;
        }

        try {
            if (SyncServer.global_server == null) {
                SyncServer.global_server = new SyncServer(requireContext());
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Cannot Connect to Sync Server", Toast.LENGTH_SHORT).show();
            return root;
        }

        // After handling, mark as not first time
        HomeFragment.sharedPreferences.edit().putBoolean(FIRST_TIME_SYNC_KEY, false).apply();
        sharedPreferences.edit().putBoolean(FIRST_LAUNCH_KEY, false).apply();

        Gson gson = new Gson();
        String marshalledData = gson.toJson(HomeFragment.chartDataList);
        try {
            SyncServer.global_server.upload(marshalledData);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Cannot upload data", Toast.LENGTH_SHORT).show();
            return root;
        }
        String username = SyncServer.global_server.username;
        String skk = SyncServer.global_server.skk;
        String info = username + "|" + skk;

        // Generate and display QR code
        Log.i(SYNC_STRING, "Normal QR Code Generate");
        generateQRCode(qrCodeImageView, info);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private void generateQRCode(ImageView qrCodeImageView, String data) {
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(data, BarcodeFormat.QR_CODE, 800, 800);
            qrCodeImageView.setImageBitmap(bitmap);
        } catch (WriterException e) {
            Log.e("SyncCodeFragment", "QR Code generation failed", e);
        }
    }
}