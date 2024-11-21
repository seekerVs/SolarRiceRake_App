package net.techcn.solarricerakeapp.DBHelper;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import net.techcn.solarricerakeapp.R;

import java.util.Arrays;

public class AccountDBHelper extends SQLiteOpenHelper {

    String LOG_TAG = "AccountHelper";
    Context app_context;
    SharedPreferences sharedPreferences;
    FirebaseAuth auth;
    SharedPreferences.Editor editor;

    public AccountDBHelper(Context context) {
        super(context, "accounts.db", null, 2);
        app_context = context;
        sharedPreferences = app_context.getSharedPreferences("PREFS_DATA", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        auth = FirebaseAuth.getInstance();
    }

    @Override
    public void onCreate(SQLiteDatabase DB) {
        DB.execSQL("create Table Accounts(account_email TEXT primary key)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase DB, int i, int i1) {
        DB.execSQL("drop Table if exists Accounts");
    }


//    public Boolean save_account(String user, String email, String phone,
//                                 String password, String name) {
    public Boolean save_account(String email) {
        SQLiteDatabase DB;
        Cursor cursor;
        ProgressDialog progressDialog = new ProgressDialog(app_context);
        progressDialog.setMessage("Saving account...");
        progressDialog.show();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }
        }, 3000);
        try {
            DB = this.getWritableDatabase();
            cursor = DB.rawQuery("Select * from Accounts where account_email = ?", new String[]{email});
        } catch (Exception e) {
            Toast.makeText(app_context, "Unable to process: Database initialization error!", Toast.LENGTH_LONG).show();
            return false;
        }
        if(cursor.getCount() == 0) {
            DB = this.getWritableDatabase();
            ContentValues contentValues = new ContentValues();

            if(email != null || !email.isEmpty()) {
                contentValues.put("account_email", email);
                Log.d(LOG_TAG,"email->>>>>>>");
            }

            long result = DB.insert("Accounts", null, contentValues);

            if (result == -1) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(app_context);
                alertDialog.setTitle("Sign Up");
                alertDialog.setMessage("Account registration failed");
                alertDialog.setIcon(R.drawable.error_solid);
                alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
                AlertDialog dialog_create = alertDialog.create();
                Log.d(LOG_TAG, "Insert Error...");
                progressDialog.dismiss();
                dialog_create.show();
                return false;
            } else {
//                update_active_user(user,password);
                progressDialog.dismiss();
                cursor.close();
                return true;
            }
        } else {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(app_context);
            alertDialog.setTitle("Sign Up");
            alertDialog.setMessage("Account already exists! Sign in instead");
            alertDialog.setIcon(R.drawable.error_solid);
            alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog_create = alertDialog.create();
            progressDialog.dismiss();
            dialog_create.show();

            cursor.close();
            progressDialog.dismiss();
            return false;
        }
    }

    public Boolean update_account(String email) {
        Log.d(LOG_TAG,"After data DBHELPER: " + " email:" + email + ">>>>>>>>>>>>>>>>>>>>>>>>>");
        ProgressDialog progressDialog = new ProgressDialog(app_context);
        progressDialog.setMessage("Updating account information...");
        progressDialog.show();
        SQLiteDatabase DB = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        if (email != null && !email.isEmpty()) {
            contentValues.put("account_email", email);
        }

        Cursor cursor = DB.rawQuery("Select * from Accounts where account_email = ?", new String[]{email});
        //if has match, update data
        if (cursor.getCount() > 0 || auth.getCurrentUser() != null) {
            long result = DB.update("Accounts", contentValues, "account_email=?", new String[]{email});
            cursor.close();
            update_active_user(email);
            if (result == -1) {
                progressDialog.dismiss();
                negative_dialog("","Failed!");
                return false;
            } else {
                progressDialog.dismiss();
//                positive_dialog("","Success");
                return true;
            }
        }
        progressDialog.dismiss();
        return false;
    }

    public Boolean delete_account(String email) {
        SQLiteDatabase DB = this.getWritableDatabase();
        Cursor cursor = DB.rawQuery("Select * from Accounts where account_email = ?", new String[]{email});
        if (cursor.getCount() > 0) {
            long result = DB.delete("Accounts", "account_email=?", new String[]{email});
            if (result == -1) {
                Log.d(LOG_TAG, "Delete Error...");
                return false;
            } else {
                cursor.close();
                return true;
            }
        } else {
            return false;
        }
    }

    public Cursor get_all_data() {
        SQLiteDatabase DB = this.getWritableDatabase();
        Cursor cursor = DB.rawQuery("Select * from Accounts", null);
        return cursor;
    }

    public Cursor get_one_data(String email) {
        SQLiteDatabase DB = this.getWritableDatabase();
        Cursor cursor = DB.rawQuery("Select * from Accounts where account_email = ?", new String[]{email});
        cursor.getCount();
        return cursor;
    }

    //checks if the data is existing using given string
    public Boolean checkdata(String email) {
        SQLiteDatabase DB = this.getWritableDatabase();
        Cursor cursor = DB.rawQuery("Select * from Accounts where account_email = ?", new String[]{email});
        if (cursor.getCount() > 0) {
            return true;
        } else {
            return false;
        }
    }

    private void update_active_user(String email) {
        // Save in sharedpreferences
        editor.putString("current_email", email);
        editor.commit();
    }

    public void positive_dialog(String title, String text) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(app_context);
        alertDialog.setTitle(title);
        alertDialog.setMessage(text);
        alertDialog.setIcon(R.drawable.success_filled);
//        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int id) {
//                dialog.dismiss();
//            }
//        });
        AlertDialog dialog_create = alertDialog.create();
        dialog_create.show();
    }

    public void negative_dialog(String title, String text) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(app_context);
        alertDialog.setTitle(title);
        alertDialog.setMessage(text);
        alertDialog.setIcon(R.drawable.error_solid);
//        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int id) {
//                dialog.dismiss();
//            }
//        });
        AlertDialog dialog_create = alertDialog.create();
        dialog_create.show();
    }
}