package comp4342.totp_manager;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.widget.Toast;

import comp4342.totp_manager.databinding.FragmentHomeBinding;
import comp4342.totp_manager.sync.SyncServer;
import comp4342.totp_manager.utils.ChartData;
import comp4342.totp_manager.utils.DiskEncryption;
import comp4342.totp_manager.utils.TotpAdapter;

public class HomeFragment extends Fragment {

    public static FragmentHomeBinding binding = null;
    public static View root = null;
    private static RecyclerView recyclerView;
    public static TotpAdapter adapter = null;  // Your adapter class
    public static List<ChartData> chartDataList;  // Replace with your data model

    public static List<ChartData> current_view = new ArrayList<>();

    private RecyclerView.LayoutManager layoutManager;

    public static SharedPreferences sharedPreferences;

    private static final String PREFS_NAME = "AppPrefs";
    private static final String FIRST_LAUNCH_KEY = "firstLaunch";
    public static final String INIT_BEFORE_KEY = "initBefore";
    private static final String HOME_LOG_PREFIX = "======::=====HOME_LOGGING";
    public static final String FIRST_TIME_SYNC_KEY = "firstTimeSync";


    public static boolean init_before;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        root = binding.getRoot();

        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean firstLaunch = sharedPreferences.getBoolean(FIRST_LAUNCH_KEY, true);
        init_before = sharedPreferences.getBoolean(INIT_BEFORE_KEY, false);
        boolean isFirstTime = sharedPreferences.getBoolean(FIRST_TIME_SYNC_KEY, true);

        if (firstLaunch) {
            try {
                handleFirstLaunch();
                Log.e(HOME_LOG_PREFIX, "First Launch Success");
            } catch (Exception e) {
                Log.e(HOME_LOG_PREFIX, "First Launch Failed", e);
                throw new RuntimeException(e);
            }
        } else {
            if (init_before){
                decryptAndLoadData();
                if (!isFirstTime) {
                    try {
                        SyncServer.global_server = new SyncServer(requireContext());
                        saveDataToDatabase(requireActivity(), requireContext());
                        initializeRecyclerView(chartDataList);
                        Log.i(HOME_LOG_PREFIX, "Home Launch with sync");

                    } catch (Exception e) {
                        Log.e(HOME_LOG_PREFIX, "HOME SYNC DATA ERROR", e);
                    }
                }
            }else{
                decryptAndLoadData();
                Log.i(HOME_LOG_PREFIX, "Home Launch without sync");
            }
        }

        return root;
    }

    private void handleFirstLaunch() throws Exception {
        DiskEncryption aesKeystore = new DiskEncryption(requireActivity());
        aesKeystore.generateKey();
        chartDataList = new ArrayList<>();
        initializeRecyclerView(chartDataList);
    }

    private void decryptAndLoadData() {
        try {
            DiskEncryption aesKeystore = new DiskEncryption(requireActivity());
            byte[] plaintextBytes = aesKeystore.decryptData(requireActivity());
            String plaintext =  new String(plaintextBytes, StandardCharsets.UTF_8);

//            Log.i(HOME_LOG_PREFIX, "Get Decrypted Data: "+plaintext);

            chartDataList = new Gson().fromJson(plaintext, new TypeToken<List<ChartData>>() {}.getType());
            initializeRecyclerView(chartDataList);
        } catch (Exception e) {
            Log.e(HOME_LOG_PREFIX, "Load data from disk: Decryption failed", e);
            chartDataList = new ArrayList<>();
            current_view = new ArrayList<>();
            initializeRecyclerView(chartDataList);
        }
    }

    private void initializeRecyclerView(List<ChartData> chartDataList) {
        recyclerView = binding.recyclerView;
        layoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(layoutManager);

        if (current_view == null) current_view = new ArrayList<>();

        current_view.clear();
        for (ChartData d : chartDataList) {
            if (!d.is_deleted()) current_view.add(d);
        }

        if (adapter == null) {
            adapter = new TotpAdapter(current_view, requireContext(), new TotpAdapter.OnItemClickListener() {
                @Override
                public void onEditClick(ChartData chartData) {
                    // Handle edit
                    showEditDialog(chartData);
                }

                @Override
                public void onDeleteClick(int position, ChartData chartData) {
                    // Handle delete
                    deleteRecord(position, chartData);
                }
            });
        }
        recyclerView.setAdapter(adapter);
    }

    private void showEditDialog(ChartData chartData) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit Record");

        // Create EditText for input
        final EditText input = new EditText(requireContext());
        input.setText(chartData.getName());
        builder.setView(input);

        // Set up buttons
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                chartData.setName(newName); // Update the data model
                onResume(); // Notify the adapter about changes
                try {
                    saveDataToDatabase(requireActivity(), requireContext()); // Save updated data back to the database
                } catch (Exception e) {
                    Log.e(HOME_LOG_PREFIX, "Data cannot be saved in ShowEditDialog", e);
                }
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void deleteRecord(int position, ChartData chartData) {
        try {
            current_view.remove(chartData);
            for (ChartData e : chartDataList) {
                if (e == null || chartData == null) continue;
                if (e.getCdid() == chartData.getCdid()) {
                    e.delete();
                }
            }
//            adapter.notifyItemRemoved(position); // Notify adapter about item removal
            adapter.notifyDataSetChanged();
            saveDataToDatabase(requireActivity(), requireContext()); // Save updated data back to the database
        } catch (Exception e) {
            Log.e(HOME_LOG_PREFIX, "Delete element failed. position=" + position + ";addr=" + chartData, e);
        }

    }

    public static void saveDataToDatabase(FragmentActivity activity, Context context) throws Exception {
        boolean isFirstTime = sharedPreferences.getBoolean(FIRST_TIME_SYNC_KEY, true);

        if (SyncServer.global_server != null && !isFirstTime){
            try {
                String remote_data = SyncServer.global_server.sync();
                List<ChartData> remote_chartDataList =
                        new Gson().fromJson(remote_data, new TypeToken<List<ChartData>>() {}.getType());
//                Log.i(HOME_LOG_PREFIX, "[saveDataToDatabase] Get Data from remote: "+remote_data);
                chartDataList = ChartData.mergeLists(chartDataList,remote_chartDataList);
            } catch (Exception e) {
                Toast.makeText(context, "Cannot fetch the remote data", Toast.LENGTH_SHORT).show();
                Log.e(HOME_LOG_PREFIX, "Data cannot be sync in saveDataToDatabase", e);
            }
        }

        Gson gson = new Gson();
        String marshalledData = gson.toJson(chartDataList);
        DiskEncryption aesKeystore = new DiskEncryption(activity);
        aesKeystore.encryptData(marshalledData, activity);

        if (SyncServer.global_server != null && !isFirstTime) {
            try {
                SyncServer.global_server.upload(marshalledData);
            } catch (Exception e) {
                Toast.makeText(context, "Cannot upload the latest data", Toast.LENGTH_SHORT).show();
                Log.e(HOME_LOG_PREFIX, "Data cannot be uploaded in saveDataToDatabase", e);
            }
        }

    }

    public static void SyncInitialData(FragmentActivity activity, Context context) throws Exception {
        String remote_data = SyncServer.global_server.sync();
        List<ChartData> remote_chartDataList =
                new Gson().fromJson(remote_data, new TypeToken<List<ChartData>>() {}.getType());
        chartDataList = ChartData.mergeLists(chartDataList,remote_chartDataList);

//        Log.i(HOME_LOG_PREFIX, "[SyncInitialData] Get Data from remote: "+remote_data);

        Gson gson = new Gson();
        String marshalledData = gson.toJson(chartDataList);
        DiskEncryption aesKeystore = new DiskEncryption(activity);
        aesKeystore.encryptData(marshalledData, activity);
    }



    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}