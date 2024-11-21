package net.techcn.solarricerakeapp;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.techcn.solarricerakeapp.databinding.FragmentDevicesBinding;

public class DevicesFragment extends Fragment implements FragmentManager.OnBackStackChangedListener {

//    FragmentDevicesBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_devices, container, false);

        getParentFragmentManager().addOnBackStackChangedListener(this);
        if (savedInstanceState == null)
            getParentFragmentManager().beginTransaction().add(R.id.fragment1, new DeviceListFragment(), "devices").commit();
        else
            onBackStackChanged();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onBackStackChanged() {
//        if (getActivity() != null) {
//            ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(getChildFragmentManager().getBackStackEntryCount() > 0);
//        }
    }

//    @Override
//    public boolean onSupportNavigateUp() {
//        if (getActivity() != null) {
//            getActivity().onBackPressed();
//        }
//        return true;
//    }
}
