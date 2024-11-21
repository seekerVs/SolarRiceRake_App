package net.techcn.solarricerakeapp;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import net.techcn.solarricerakeapp.databinding.FragmentNotificationBinding;
import net.techcn.solarricerakeapp.databinding.FragmentReportsBinding;

import java.util.Objects;

public class NotificationFragment extends Fragment {

    FragmentNotificationBinding binding;

    GlobalObject globalObject;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        globalObject.notifRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                populateNotificationLayout();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        globalObject = new GlobalObject(context.getApplicationContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentNotificationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Initialize UI components and set click listeners
        populateNotificationLayout();

        binding.backImageButton.setOnClickListener(v ->
                getParentFragmentManager().popBackStack());
    }

    public void populateNotificationLayout() {
        globalObject.notifRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    binding.notifAvailableTextview.setVisibility(View.GONE);

                    binding.notificationLayout.removeAllViews();

                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                        String notif_title = childSnapshot.child("title").getValue(String.class);
                        String notif_date = childSnapshot.child("timestamp").getValue(String.class);
                        String notif_message = childSnapshot.child("message").getValue(String.class);

                        LayoutInflater inflater = getActivity().getLayoutInflater();
                        View view = inflater.inflate(R.layout.notif_block, null);

                        TextView notifTitleTextview = view.findViewById(R.id.notifTitle);
                        TextView notifDateTextview = view.findViewById(R.id.notifDate);
                        TextView notifMessageTextview = view.findViewById(R.id.notifMessage);

                        notifTitleTextview.setText(notif_title);
                        notifDateTextview.setText(notif_date);
                        notifMessageTextview.setText(notif_message);

                        binding.notificationLayout.addView(view, 0);
                    }

                    ScrollView scrollView = binding.scrollView;
                    scrollView.post(() -> scrollView.fullScroll(View.FOCUS_FORWARD));
                } else {
                    binding.notifAvailableTextview.setVisibility(View.VISIBLE);
                    Toast.makeText(getActivity(), "No notifications available", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                binding.notifAvailableTextview.setVisibility(View.VISIBLE);
                Toast.makeText(getActivity(), "Failed to fetch notifications" + databaseError, Toast.LENGTH_SHORT).show();
            }
        });
    }
}