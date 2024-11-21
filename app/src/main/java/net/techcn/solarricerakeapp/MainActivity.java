package net.techcn.solarricerakeapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.SignInMethodQueryResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import net.techcn.solarricerakeapp.DBHelper.AccountDBHelper;
import net.techcn.solarricerakeapp.databinding.ActivityMainBinding;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String LOG_TAG = MainActivity.class.getSimpleName();
    ActivityMainBinding binding;
    FirebaseAuth auth;
    GoogleSignInClient googleSignInClient;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    GlobalObject globalObject;
    AccountDBHelper DB;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        globalObject = new GlobalObject(this);
        sharedPreferences = getSharedPreferences("PREFS_DATA", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        DB = new AccountDBHelper(MainActivity.this);

        FirebaseApp.initializeApp(this);
        auth = FirebaseAuth.getInstance();

        binding.signInBtn.setOnClickListener(v -> {
            launch_authentication();
        });

        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(MainActivity.this, options);

        String email = sharedPreferences.getString("current_email", null);
        Log.d(LOG_TAG, LOG_TAG + "update_active_user: " + email);

        checkAuthenticatedUser();

    }

    private void checkAuthenticatedUser() {
        try {
            String user_account = sharedPreferences.getString("current_email",null);
            Log.d(LOG_TAG, "current_email: " + user_account + ">>>>>>>>>>>>>>>>>>>>>>>>>>>>");
            // Check if there is saved account
            if (user_account != null && !user_account.isEmpty()) {
                user_account = user_account.replace(".", "");
                checkDevice(user_account);
                openMasterActivity();
                Toast.makeText(this, "Account detected! Signing in... ", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(LOG_TAG, "JUST A NORMAL DAY CAPTAIN!");
            }
        } catch (Exception e) {
            Log.d(LOG_TAG, "Catch Error in MainActivity OnCreate: " + e);
        }
    }

    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            Log.d(LOG_TAG, "onActivityResult TRIGGERED ");
            if (result.getResultCode() == RESULT_OK) {
                Task<GoogleSignInAccount> accountTask = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount signInAccount = accountTask.getResult(ApiException.class);
                    AuthCredential authCredential = GoogleAuthProvider.getCredential(signInAccount.getIdToken(), null);

                    Log.d(LOG_TAG, "signInAccount.getEmail(): " + signInAccount.getEmail());
                    auth.signInWithCredential(authCredential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
//                                AuthResult authResult = task.getResult();
//                                boolean isNewUser = authResult.getAdditionalUserInfo().isNewUser();
//
//                                if (isNewUser) {
//                                    Log.d(LOG_TAG, "This is a new user.");
//                                    // Handle new user logic here
//                                } else {
//                                    Log.d(LOG_TAG, "This is an existing user.");
//                                    // Handle existing user logic here
//                                }

                                String email = auth.getCurrentUser().getEmail();
                                // save data in db
                                email = email.replace(".", "");
                                save_data(email);
                                checkDevice(email);
                            } else {
                                Toast.makeText(getApplicationContext(), "Failed to sign in: " + task.getException(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (ApiException e) {
                    e.printStackTrace();
                }
            }
        }
    });


//    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
//        @Override
//        public void onActivityResult(ActivityResult result) {
//            Log.d(LOG_TAG, "onActivityResult TRIGGERED ");
//            if (result.getResultCode() == RESULT_OK) {
//                Task<GoogleSignInAccount> accountTask = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
//                try {
//                    GoogleSignInAccount signInAccount = accountTask.getResult(ApiException.class);
//                    AuthCredential authCredential = GoogleAuthProvider.getCredential(signInAccount.getIdToken(), null);
//
//                    Log.d(LOG_TAG, "signInAccount.getEmail(): " + signInAccount.getEmail());
//                    auth.signInWithCredential(authCredential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
//                        @Override
//                        public void onComplete(@NonNull Task<AuthResult> task) {
//                            if (task.isSuccessful()) {
//                                String email = auth.getCurrentUser().getEmail();
//                                // save data in db
//                                save_data(email);
//                            } else {
//                                Toast.makeText(MainActivity.this, "Failed to sign in: " + task.getException(), Toast.LENGTH_SHORT).show();
//                            }
//                        }
//                    });
//                } catch (ApiException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    });

    public void save_data(String email) {
        Log.d(LOG_TAG, "Data saved: " + email + ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Loading...");
        progressDialog.show();
        if (progressDialog != null && progressDialog.isShowing()) {
            Log.d(LOG_TAG, "Data saved: " + email + ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

            boolean isAccountExist = DB.checkdata(email);
            Boolean isSaved = null;
            if (isAccountExist) {
                Toast.makeText(this, "Account detected! Signing in... ", Toast.LENGTH_SHORT).show();
                editor.putString("current_email", email);
                editor.commit();
                progressDialog.dismiss();
                openMasterActivity();
            } else {
                editor.putString("current_email", email);
                editor.commit();
                isSaved = DB.save_account(email);

                Log.d(LOG_TAG, "Data savedd: " + String.valueOf(isSaved) + ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                if (isSaved) {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Signed in successfully!", Toast.LENGTH_SHORT).show();
                    Log.d(LOG_TAG, "Data savedd: " + String.valueOf(isSaved) + ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                    openMasterActivity();
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to sign in!", Toast.LENGTH_SHORT).show();
                }
            }

        }
    }

    private void checkDevice(String email) {
        if (globalObject.isInternetAvailable()) {
            globalObject.releasedDeviceRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshots) {
                    Log.d(LOG_TAG, "snapshots.getChildrenCount(): " + snapshots.getChildrenCount());

                    for (DataSnapshot snapshot : snapshots.getChildren()) {
                        String key = snapshot.getKey();
                        String value = snapshot.getValue(String.class);

                        if (value.equals(email)) {
                            editor.putString("current_device",key);
                            editor.commit();
                            break;
                        }
                        Log.d(LOG_TAG, "key: " + key + ", value: " + value);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

    public void launch_authentication() {
        // Check if there is saved account
        ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Loading...");
        progressDialog.show();
        if (globalObject.isInternetAvailable()) {
            progressDialog.setMessage("Requesting Firebase Authentication...");
            String message = (auth.getCurrentUser() == null) ? "User is not authenticated" : "User is authenticated";
            Log.d(LOG_TAG,message);
            Intent intent = googleSignInClient.getSignInIntent();
            activityResultLauncher.launch(intent);
            progressDialog.setMessage("Success!");
            progressDialog.dismiss();
        } else {
            Toast.makeText(this, "No Internet Connection", Toast.LENGTH_SHORT).show();
            progressDialog.setMessage("Failed: There's an error in accessing Firebase functions");
            progressDialog.dismiss();
        }
    }

    private void checkIfEmailExists(String email) {
        auth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(new OnCompleteListener<SignInMethodQueryResult>() {
                    @Override
                    public void onComplete(@NonNull Task<SignInMethodQueryResult> task) {
                        if (task.isSuccessful()) {
                            SignInMethodQueryResult result = task.getResult();
                            List<String> signInMethods = result.getSignInMethods();
                            Log.d(LOG_TAG, "checkIfEmailExists: signInMethods: " + signInMethods);
                            if (signInMethods != null && !signInMethods.isEmpty()) {
                                // Email is already registered
                                Log.d(LOG_TAG, "checkIfEmailExists: Email is already registered.");
                            } else {
                                // Email is not registered
                                Log.d(LOG_TAG, "checkIfEmailExists: Email is not registered.");
                            }
                        } else {
                            // Error occurred
                            Log.e(LOG_TAG, "checkIfEmailExists: Error checking email: " + task.getException().getMessage());
                        }
                    }
                });
    }

    public void openMasterActivity() {
        Intent intent = new Intent(MainActivity.this, MasterActivity.class);
        startActivity(intent);
        finish();
    }
}