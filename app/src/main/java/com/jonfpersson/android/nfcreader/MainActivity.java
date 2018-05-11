package com.jonfpersson.android.nfcreader;

import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Build;
import android.os.Parcelable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    // alias key = nfc reader
    NfcAdapter nfcAdapter;
    com.github.florent37.materialtextfield.MaterialTextField textField;
    EditText txtTagContent;
    TextView textLabel;
    TextView infoLabel;
    TextView tagDataText;
    HorizontalScrollView scrollView;
    CheckBox formatTagBox;
    boolean boxIsChecked;

    int mode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        iniVariables();
        setColors();

        mode = getIntent().getIntExtra("messageMode", 0);

        if (mode == 1){
            formatTagBox.setVisibility(View.INVISIBLE);
            textField.setVisibility(View.INVISIBLE);
            tagDataText.setVisibility(View.VISIBLE);
            textLabel.setText("The tag says:");
            infoLabel.setText("Place the tag on the phone to read");
        } else {
            textField.setVisibility(View.VISIBLE);
            formatTagBox.setVisibility(View.VISIBLE);
            tagDataText.setVisibility(View.INVISIBLE);
            textLabel.setText("Write to NFC tag here:");
            infoLabel.setText("Write to tag");
        }

        formatTagBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (formatTagBox.isChecked()){
                    textField.setEnabled(false);
                    textField.setFocusable(false);
                    textField.setVisibility(View.INVISIBLE);
                    boxIsChecked = true;
                    textLabel.setText("Place the tag to format");
                } else {
                    textField.setEnabled(true);
                    textField.setFocusable(true);
                    textField.setVisibility(View.VISIBLE);
                    boxIsChecked = false;
                    textLabel.setText("Write to NFC tag here:");


                }

            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        enableTagWriteMode();

    }

    @Override
    protected void onPause() {
        super.onPause();
        disableTagWriteMode();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent.hasExtra(NfcAdapter.EXTRA_TAG)) {
            //Toast.makeText(this, "NfcIntent!", Toast.LENGTH_SHORT).show();

            if(mode == 1)
            {
                Parcelable[] parcelables = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

                if(parcelables != null && parcelables.length > 0)
                {
                    readTextFromMessage((NdefMessage) parcelables[0]);
                }else{
                    Toast.makeText(this, "No NDEF messages found!", Toast.LENGTH_SHORT).show();
                }

            }else{

                try {
                    Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                    NdefMessage ndefMessage = createNdefMessage(txtTagContent.getText()+"");

                    writeNdefMessage(tag, ndefMessage);
                }
                catch (NullPointerException E){
                    Toast.makeText(this, "Text to large!", Toast.LENGTH_SHORT).show();
                    System.out.println("Exeption: "+E);
                }

            }

        }
    }

    private void readTextFromMessage(NdefMessage ndefMessage) {

        NdefRecord[] ndefRecords = ndefMessage.getRecords();

        if(ndefRecords != null && ndefRecords.length>0){

            NdefRecord ndefRecord = ndefRecords[0];

            String tagContent = getTextFromNdefRecord(ndefRecord);

            tagDataText.setText(tagContent + "\r\n" );

            boolean isContain = containsURL(tagContent);
            System.out.println("contain: " + isContain);
            if (isContain)
                displayUrlDialog(tagContent);

        }else
        {
            Toast.makeText(this, "No NDEF records found!", Toast.LENGTH_SHORT).show();
        }

    }

    private void enableTagWriteMode() {

        Intent intent = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        IntentFilter[] intentFilters = new IntentFilter[]{};

        if(nfcAdapter==null)
        {
            nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        } else{
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null);
        }

    }

    private void disableTagWriteMode() {
        try {
            nfcAdapter.disableForegroundDispatch(this);

        } catch (Exception E) {
            System.out.println("\nSomething got fucked up");
        }
    }

    private void formatTag(Tag tag, NdefMessage ndefMessage) {
        try {

            NdefFormatable ndefFormatable = NdefFormatable.get(tag);

            if (ndefFormatable == null) {
                Toast.makeText(this, "This tag is not ndef formatable!", Toast.LENGTH_SHORT).show();
                return;
            }

            ndefFormatable.connect();
            ndefFormatable.format(ndefMessage);
            ndefFormatable.close();

            Toast.makeText(this, "Tag writen!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e("formatTag", e.getMessage());
        }

    }

    private void writeNdefMessage(Tag tag, NdefMessage ndefMessage) {
        try {

            if (tag == null) {
                Toast.makeText(this, "Tag object cannot be null", Toast.LENGTH_SHORT).show();
                return;
            }
            Ndef ndef = Ndef.get(tag);

            if (ndef == null || boxIsChecked) {
                // format tag with the ndef format and writes the message.
                formatTag(tag, ndefMessage);
                System.out.println("TAG FORMATED, I THINK LOL");
            } else {
                ndef.connect();

                if (!ndef.isWritable()) {
                    Toast.makeText(this, "Tag is not writable!", Toast.LENGTH_SHORT).show();

                    ndef.close();
                    return;
                }

                ndef.writeNdefMessage(ndefMessage);
                ndef.close();

                Toast.makeText(this, "Tag writen!", Toast.LENGTH_SHORT).show();

            }

       } catch (Exception e) {
            Toast.makeText(this, "Error: Hold tag still and try again", Toast.LENGTH_SHORT).show();
            // Log.e("writeNdefMessage", e.getMessage());
            System.out.println("Exeption: " + e);
        }

    }

    private NdefRecord createTextRecord(String content) {
        try {
            byte[] language;
            language = Locale.getDefault().getLanguage().getBytes("UTF-8");

            final byte[] text = content.getBytes("UTF-8");
            final int languageSize = language.length;
            final int textLength = text.length;
            final ByteArrayOutputStream payload = new ByteArrayOutputStream(1 + languageSize + textLength);

            payload.write((byte) (languageSize & 0x1F));
            payload.write(language, 0, languageSize);
            payload.write(text, 0, textLength);

            return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload.toByteArray());

        } catch (UnsupportedEncodingException e) {
            Log.e("createTextRecord", e.getMessage());
        }
        return null;
    }

    private NdefMessage createNdefMessage(String content) {

        NdefRecord ndefRecord = createTextRecord(content);
        //NdefRecord ndefRecord2 = createTextRecord(content);

        NdefMessage ndefMessage = new NdefMessage(new NdefRecord[]{ndefRecord});

        return ndefMessage;
    }

    public String getTextFromNdefRecord(NdefRecord ndefRecord)
    {
        String tagContent = null;
        try {
            byte[] payload = ndefRecord.getPayload();
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
            int languageSize = payload[0] & 0063;
            tagContent = new String(payload, languageSize + 1,
                    payload.length - languageSize - 1, textEncoding);
        } catch (UnsupportedEncodingException e) {
            Log.e("getTextFromNdefRecord", e.getMessage(), e);
        }
        return tagContent;
    }
    private void setColors(){

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#6b6aba")));


        Window window = MainActivity.this.getWindow();

// clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

// add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

// finally change the color
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            window.setStatusBarColor(ContextCompat.getColor(MainActivity.this,R.color.notiBar));
        } else{
            // Cant set notificationbar color
        }
    }

    void iniVariables(){
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        txtTagContent = findViewById(R.id.txtTagContent);
        textLabel = findViewById(R.id.informationText);
        infoLabel = findViewById(R.id.infoText);
        textField = findViewById(R.id.editTextContainer);
        tagDataText = findViewById(R.id.tagInformationView);
        scrollView = findViewById(R.id.scrollView);
        formatTagBox = findViewById(R.id.formatTagBox);
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(View.FOCUS_RIGHT);
            }
        });



    }

    void displayUrlDialog (final String url){
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Would you like to open this url?");
        alertDialog.setMessage(url);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Go to page",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();

                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        System.out.println("URL: "+url);
                        startActivity(browserIntent);

                    }
                });

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        alertDialog.show();


    }

    private boolean containsURL(String content){
        String REGEX = "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
        Pattern p = Pattern.compile(REGEX,Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(content);
        if(m.find()) {
            return true;
        }

        return false;
    }
}
