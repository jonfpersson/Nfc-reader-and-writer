package com.jonfpersson.android.nfcreader;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toolbar;

import com.kobakei.ratethisapp.RateThisApp;

import static android.provider.AlarmClock.EXTRA_MESSAGE;


public class MainMenu extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        setColors();

        // Monitor launch times and interval from installation
        RateThisApp.onCreate(this);
        // If the condition is satisfied, "Rate this app" dialog will be shown
        RateThisApp.showRateDialogIfNeeded(this);

        NfcManager manager = (NfcManager) MainMenu.this.getSystemService(Context.NFC_SERVICE);
        NfcAdapter adapter = manager.getDefaultAdapter();
        if (!(adapter != null && adapter.isEnabled())) {
            // adapter doesnt exists and isnt enabled.

            AlertDialog alertDialog = new AlertDialog.Builder(MainMenu.this).create();
            alertDialog.setTitle("Please Enable NFC");
            alertDialog.setMessage("Go to your phones system setting and turn on NFC, or else the application won't work properly");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Ok",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
                        }
                    });
            alertDialog.show();

        }

    }

    public void startReadActivity(View v) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("messageMode", 1);
        startActivity(intent);
    }

    public void startWriteActivity(View v) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("messageMode", 2);
        startActivity(intent);
    }
//    String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);

    private void setColors() {

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#6b6aba")));

        Window window = MainMenu.this.getWindow();

        // clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        // finally change the color

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(ContextCompat.getColor(MainMenu.this, R.color.notiBar));
        } else {
            // Cant set notificationbar color
        }

    }

}
