package net.techcn.solarricerakeapp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import net.techcn.solarricerakeapp.Adapter.DryingHistoryTableAdapter;
import net.techcn.solarricerakeapp.Model.DryingHistoryModel;
import net.techcn.solarricerakeapp.databinding.FragmentDryingHistoryBinding;
import net.techcn.solarricerakeapp.databinding.FragmentReportsBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class DryingHistoryFragment extends Fragment {

    FragmentDryingHistoryBinding binding;
    public static final String LOG_TAG = DryingHistoryFragment.class.getSimpleName();

    GlobalObject globalObject;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        globalObject = new GlobalObject(context.getApplicationContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentDryingHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Initialize UI components and set click listeners
        binding.backImageButton.setOnClickListener(v ->
                getParentFragmentManager().popBackStack());

        if (globalObject.isInternetAvailable()) {
            populate_table();
        } else {
            negative_dialog("Data retrival failed", "Please check your internet connection and try again.");
        }
    }

    private void populate_table() {
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Fetching the data...");
        progressDialog.show();
        try {
            List<DryingHistoryModel> dataObjectList = new ArrayList<>();
            globalObject.dryingHistoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Log.d(LOG_TAG, "snapshot.getChildrenCount(): " + snapshot.getChildrenCount());
                        List<DataSnapshot> childSnapshots = new ArrayList<>();
                        for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                            childSnapshots.add(childSnapshot);
                        }
                        //Reverse
                        Collections.reverse(childSnapshots);

                        for (DataSnapshot childSnapshot : childSnapshots) {
                            String startTime = childSnapshot.child("start_timestamp").getValue(String.class);
                            String endTime = childSnapshot.child("end_timestamp").getValue(String.class);
                            String duration = childSnapshot.child("duration").getValue(String.class);
                            String mode = childSnapshot.child("mode").getValue(String.class);
                            dataObjectList.add(new DryingHistoryModel(startTime, endTime, duration, mode));
                        }

                        RecyclerView recyclerView = binding.tableRecyclerView;
                        DryingHistoryTableAdapter adapter = new DryingHistoryTableAdapter(dataObjectList);
                        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
                        recyclerView.setLayoutManager(linearLayoutManager);
                        recyclerView.setAdapter(adapter);

                        positive_dialog("Data retrival success", "Data retrieved successfully.");
                        progressDialog.dismiss();

                    } else {
                        negative_dialog("Data retrival failed", "No data found.");
                        binding.tableRecyclerView.setVisibility(View.GONE);
                        progressDialog.dismiss();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        } catch (Exception e) {
            Log.d(LOG_TAG, "Error: " + e.getMessage());
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
        }
    }

    public void negative_dialog(String title, String text) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(title);
        alertDialog.setMessage(text);
        alertDialog.setIcon(R.drawable.error_solid);
        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog_create = alertDialog.create();
        dialog_create.show();
    }

    public void positive_dialog(String title, String text) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(title);
        alertDialog.setMessage(text);
        alertDialog.setIcon(R.drawable.success_filled);
        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog_create = alertDialog.create();
        dialog_create.show();
    }

}