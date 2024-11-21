package net.techcn.solarricerakeapp.Adapter;


import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.techcn.solarricerakeapp.Model.DryingHistoryModel;
import net.techcn.solarricerakeapp.R;

import java.util.List;

public class DryingHistoryTableAdapter extends RecyclerView.Adapter {
    List<DryingHistoryModel> dryingHistoryModels;
    Context context;
    public static final String LOG_TAG = DryingHistoryTableAdapter.class.getSimpleName();
//    String currentUser;


    public DryingHistoryTableAdapter(List<DryingHistoryModel> dryingHistoryModels) {
        this.dryingHistoryModels = dryingHistoryModels;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.
                from(parent.getContext()).
                inflate(R.layout.drying_table_item, parent, false);
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
            rowViewHolder.startTimeTextView.setBackgroundResource(R.drawable.table_header_cell_bg);
            rowViewHolder.endTimeTextView.setBackgroundResource(R.drawable.table_header_cell_bg);
            rowViewHolder.durationTextView.setBackgroundResource(R.drawable.table_header_cell_bg);
            rowViewHolder.modeTextView.setBackgroundResource(R.drawable.table_header_cell_bg);

            // Set header row text color
            rowViewHolder.startTimeTextView.setTextColor(ContextCompat.getColor(context, R.color.text_color));
            rowViewHolder.endTimeTextView.setTextColor(ContextCompat.getColor(context, R.color.text_color));
            rowViewHolder.durationTextView.setTextColor(ContextCompat.getColor(context, R.color.text_color));
            rowViewHolder.modeTextView.setTextColor(ContextCompat.getColor(context, R.color.text_color));

            rowViewHolder.startTimeTextView.setText("Start Time");
            rowViewHolder.endTimeTextView.setText("End Time");
            rowViewHolder.durationTextView.setText("Duration");
            rowViewHolder.modeTextView.setText("Mode");
        } else {
            DryingHistoryModel modal = dryingHistoryModels.get(rowPos-1);

            rowViewHolder.startTimeTextView.setBackgroundResource(R.drawable.table_content_cell_bg);
            rowViewHolder.endTimeTextView.setBackgroundResource(R.drawable.table_content_cell_bg);
            rowViewHolder.durationTextView.setBackgroundResource(R.drawable.table_content_cell_bg);
            rowViewHolder.modeTextView.setBackgroundResource(R.drawable.table_content_cell_bg);;

            rowViewHolder.startTimeTextView.setText(modal.getStartTime());
            rowViewHolder.endTimeTextView.setText(modal.getEndTime());
            rowViewHolder.durationTextView.setText(modal.getDuration());
            rowViewHolder.modeTextView.setText(modal.getMode());
        }
    }

    @Override
    public int getItemCount() {
        return dryingHistoryModels.size()+1;
    }

    public class RowViewHolder extends RecyclerView.ViewHolder {
        protected TextView startTimeTextView, endTimeTextView, durationTextView, modeTextView;

        public RowViewHolder(View itemView) {
            super(itemView);
            context = itemView.getContext();
            startTimeTextView = itemView.findViewById(R.id.startTimeTextview);
            endTimeTextView = itemView.findViewById(R.id.endTimeTextview);
            durationTextView = itemView.findViewById(R.id.durationTextview);
            modeTextView = itemView.findViewById(R.id.modeTextview);
        }

//        public void setOnLongClickListener(View.OnLongClickListener onLongClickListener, String id) {
//            HistoryId = id;
//            String strDate = startTimeTextView.getText().toString();
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
