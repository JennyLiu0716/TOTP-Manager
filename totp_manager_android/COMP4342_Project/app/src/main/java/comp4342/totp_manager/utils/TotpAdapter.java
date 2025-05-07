package comp4342.totp_manager.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import comp4342.totp_manager.R;

public class TotpAdapter extends RecyclerView.Adapter<TotpAdapter.TotpViewHolder> {

    private List<ChartData> chartDataList;
    private Context context;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onEditClick(ChartData chartData);

        void onDeleteClick(int position, ChartData chartData);
    }

    public TotpAdapter(List<ChartData> chartDataList, Context context, OnItemClickListener onItemClickListener) {
        this.chartDataList = chartDataList;
        this.context = context;
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public TotpViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.item_totp, parent, false);
        return new TotpViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(TotpViewHolder holder, int position) {
        ChartData chartData = chartDataList.get(position);
        holder.name.setText(chartData.getName());
        try {
            holder.code.setText(generateTotpCode(chartData.getKey()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        startUpdating(holder, chartData);

        holder.editButton.setOnClickListener(v -> onItemClickListener.onEditClick(chartData));
        holder.deleteButton.setOnClickListener(v -> {
                onItemClickListener.onDeleteClick(position, chartData);
        });
    }

    @Override
    public int getItemCount() {
        return chartDataList.size();
    }

    public static class TotpViewHolder extends RecyclerView.ViewHolder {
        TextView name, code, comment;
        ProgressBar progressBar;
        ImageView editButton, deleteButton;

        public TotpViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.totp_name);
            code = itemView.findViewById(R.id.totp_code);
//            comment = itemView.findViewById(R.id.totp_comment);
            progressBar = itemView.findViewById(R.id.totp_bar);
            editButton = itemView.findViewById(R.id.edit_button);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }

    private String generateTotpCode(String key) throws Exception {
        // Implement TOTP code generation logic here using the TOTP key
        String token = TOTPGenerator.generateTOTP(key);  // Placeholder for actual TOTP code generation
        return token;
    }

    private void startUpdating(TotpViewHolder holder, ChartData chartData) {
        final long period = 30_000; // 30 seconds in milliseconds
        Handler handler = new Handler();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
//                long startTime = chartData.getTime();
                long elapsedTime = currentTime % period; // Elapsed time in the current 30-second period
                long timeLeft = period - elapsedTime;   // Remaining time in the current 30-second period

                // Update progress bar (from 0 to 100)
                int progress = (int) ((timeLeft * 100) / period);
                holder.progressBar.setProgress(progress);

                // Update TOTP code at the start of each period
                if (elapsedTime < 100) { // Near the start of a new period
                    try {
                        holder.code.setText(generateTotpCode(chartData.getKey()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // Post the update to run again after 100ms
                handler.postDelayed(this, 100);
            }
        };

        handler.post(runnable);
    }




}

