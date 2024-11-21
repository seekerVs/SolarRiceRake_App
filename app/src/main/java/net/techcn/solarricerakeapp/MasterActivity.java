package net.techcn.solarricerakeapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import net.techcn.solarricerakeapp.databinding.ActivityMasterBinding;

public class MasterActivity extends AppCompatActivity {
    ActivityMasterBinding binding;
    public static final String LOG_TAG = MasterActivity.class.getSimpleName();
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    String currrentFragment = "DashboardFragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMasterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        sharedPreferences = getSharedPreferences("PREFS_DATA", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        // Default fragment
        openDashboardFragment();

        // Dashboard navigation
        binding.dashboardBtn.setOnClickListener(v -> {
            onClickResponseManager("DashboardFragment");
        });

        // Controls navigation
        binding.controlsBtn.setOnClickListener(v -> {
            onClickResponseManager("ControlsFragment");
        });

        // Reports navigation
        binding.reportsBtn.setOnClickListener(v -> {
            onClickResponseManager("ReportsFragment");
        });

        // Settings navigation
        binding.settingsBtn.setOnClickListener(v -> {
            onClickResponseManager("SettingsFragment");
        });
    }

    private void openDashboardFragment() {
        getSupportFragmentManager().beginTransaction().replace(R.id.framelayout, new DashboardFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
        binding.dashboardBtn.setImageResource(R.drawable.dashboardicon2);
        binding.controlsBtn.setImageResource(R.drawable.controlicon);
        binding.reportsBtn.setImageResource(R.drawable.reportsicon);
        binding.settingsBtn.setImageResource(R.drawable.gearicon);
    }

    private void openControlsFragment() {
        getSupportFragmentManager().beginTransaction().replace(R.id.framelayout, new ControlsFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
        binding.dashboardBtn.setImageResource(R.drawable.dashboardicon);
        binding.controlsBtn.setImageResource(R.drawable.controlicon2);
        binding.reportsBtn.setImageResource(R.drawable.reportsicon);
        binding.settingsBtn.setImageResource(R.drawable.gearicon);
    }

    private void openReportsFragment() {
        getSupportFragmentManager().beginTransaction().replace(R.id.framelayout, new ReportsFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
        binding.dashboardBtn.setImageResource(R.drawable.dashboardicon);
        binding.controlsBtn.setImageResource(R.drawable.controlicon);
        binding.reportsBtn.setImageResource(R.drawable.reportsicon2);
        binding.settingsBtn.setImageResource(R.drawable.gearicon);
    }

    private void openSettingsFragment() {
        getSupportFragmentManager().beginTransaction().replace(R.id.framelayout, new SettingsFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
        binding.dashboardBtn.setImageResource(R.drawable.dashboardicon);
        binding.controlsBtn.setImageResource(R.drawable.controlicon);
        binding.reportsBtn.setImageResource(R.drawable.reportsicon);
        binding.settingsBtn.setImageResource(R.drawable.gearicon2);
    }

    private void onClickResponseManager(String className) {
        if (currrentFragment.equals(className)) {
            return;
        }
        currrentFragment = className;
        String isAutomaticMode = sharedPreferences.getString("is_automatic_mode", "false");
        Log.d(LOG_TAG, "MasterActivity isAutomaticMode: " + isAutomaticMode);
        if (sharedPreferences.getString("is_automatic_mode", "false").equals("true")) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
            alertDialog.setTitle("Warning");
            alertDialog.setMessage("It looks like you're in automatic mode. Do you want to exit?");
            alertDialog.setIcon(R.drawable.information);
            alertDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                    editor.putString("is_automatic_mode", "false");
                    editor.commit();
                    onClickActionChooser(className);
                }
            });
            alertDialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog_create = alertDialog.create();
            dialog_create.show();
        } else {
            onClickActionChooser(className);
        }
    }

    private void onClickActionChooser(String className) {
        switch (className) {
            case "DashboardFragment":
                openDashboardFragment();
                break;
            case "ControlsFragment":
                openControlsFragment();
                break;
            case "ReportsFragment":
                openReportsFragment();
                break;
            case "SettingsFragment":
                openSettingsFragment();
                break;
        }
        currrentFragment = className;
    }
}