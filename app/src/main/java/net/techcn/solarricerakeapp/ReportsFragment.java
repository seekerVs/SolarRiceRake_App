package net.techcn.solarricerakeapp;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.techcn.solarricerakeapp.databinding.FragmentReportsBinding;

public class ReportsFragment extends Fragment {

    FragmentReportsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentReportsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Initialize UI components and set click listeners

        binding.DryingHistoryLayout.setOnClickListener(v -> {
            getFragmentManager().beginTransaction().replace(R.id.framelayout, new DryingHistoryFragment())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .addToBackStack(null)
                    .commit();
        });

        binding.MoistureHistoryLayout.setOnClickListener(v -> {
            getFragmentManager().beginTransaction().replace(R.id.framelayout, new MoistureHistoryFragment())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .addToBackStack(null)
                    .commit();
        });

        binding.BatteryHistoryLayout.setOnClickListener(v -> {
            getFragmentManager().beginTransaction().replace(R.id.framelayout, new BatteryHistoryFragment())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .addToBackStack(null)
                    .commit();
        });

        binding.ActivityLogLayout.setOnClickListener(v -> {
            getFragmentManager().beginTransaction().replace(R.id.framelayout, new ActivityLogFragment())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .addToBackStack(null)
                    .commit();
        });
    }
}