package net.techcn.solarricerakeapp.Adapter;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.techcn.solarricerakeapp.Model.BatteryStatusModel;
import net.techcn.solarricerakeapp.R;

import java.util.List;

public class BatteryStatusTableAdapter extends RecyclerView.Adapter {
    List<BatteryStatusModel> batteryStatusModels;
    Context context;
    public static final String LOG_TAG = BatteryStatusTableAdapter.class.getSimpleName();
//    String currentUser;
    
    public BatteryStatusTableAdapter(List<BatteryStatusModel> batteryStatusModels) {
        this.batteryStatusModels = batteryStatusModels;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.
                from(parent.getContext()).
                inflate(R.layout.battery_table_item, parent, false);
//        currentUser = sharedPreferences.getString("current_user", null);
        return new RowViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        RowViewHolder rowViewHolder = (RowViewHolder) holder;

        int rowPos = rowViewHolder.getAdapterPosition();
//        Log.d(LOG_TAG, "getItemCount()" + getItemCount());

        if (rowPos == 0) {
            // set header row background
            rowViewHolder.percentTextview.setBackgroundResource(R.drawable.table_header_cell_bg);
            rowViewHolder.timestampTextView.setBackgroundResource(R.drawable.table_header_cell_bg);
            rowViewHolder.statusTextView.setBackgroundResource(R.drawable.table_header_cell_bg);

            // Set header row text color
            rowViewHolder.percentTextview.setTextColor(ContextCompat.getColor(context, R.color.text_color));
            rowViewHolder.timestampTextView.setTextColor(ContextCompat.getColor(context, R.color.text_color));
            rowViewHolder.statusTextView.setTextColor(ContextCompat.getColor(context, R.color.text_color));

            rowViewHolder.percentTextview.setText("Percentage (%)");
            rowViewHolder.timestampTextView.setText("Timestamp");
            rowViewHolder.statusTextView.setText("Status");
        } else {
            BatteryStatusModel modal = batteryStatusModels.get(rowPos-1);

            rowViewHolder.percentTextview.setBackgroundResource(R.drawable.table_content_cell_bg);
            rowViewHolder.timestampTextView.setBackgroundResource(R.drawable.table_content_cell_bg);
            rowViewHolder.statusTextView.setBackgroundResource(R.drawable.table_content_cell_bg);

            rowViewHolder.percentTextview.setText(modal.getPercentage());
            rowViewHolder.timestampTextView.setText(modal.getTimestamp());
            rowViewHolder.statusTextView.setText(modal.getStatus());
        }
    }

    @Override
    public int getItemCount() {
        return batteryStatusModels.size()+1;
    }

    public class RowViewHolder extends RecyclerView.ViewHolder {
        protected TextView timestampTextView, percentTextview , statusTextView;

        public RowViewHolder(View itemView) {
            super(itemView);
            context = itemView.getContext();
            timestampTextView = itemView.findViewById(R.id.timestampTextview);
            percentTextview = itemView.findViewById(R.id.percentageTextview);
            statusTextView = itemView.findViewById(R.id.statusTextview);

        }

//        public void setOnLongClickListener(View.OnLongClickListener onLongClickListener, String id) {
//            HistoryId = id;
//            String strDate = percentTextview.getText().toString();
//
//            android.app.AlertDialog.Builder alertDialog = new android.app.AlertDialog.Builder(context);
//            alertDialog.setTitle("Delete Date History");
//            alertDialog.setMessage("Are your sure you want to delete the history of this brand in \"" + strDate +"\"?");
//            alertDialog.setIcon(R.drawable.success_filled);
//            alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                public void onClick(DialogInterface dialog, int id) {
//                    DBHistory.delete_history(HistoryId,strDate);
//                    Intent intent = new Intent(context, InventoryActivity.class);
////                                adapter.notifyDataSetChanged();
//                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//                    context.startActivity(intent);
//                    dialog.dismiss();
//                }
//            });
//            alertDialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    dialog.dismiss();
//                }
//            });
//            android.app.AlertDialog dialog_create = alertDialog.create();
//            dialog_create.show();
//            }
    }
}
