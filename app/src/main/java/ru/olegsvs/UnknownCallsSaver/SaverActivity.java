package ru.olegsvs.UnknownCallsSaver;

import android.Manifest;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class SaverActivity extends AppCompatActivity {

    ProgressBar pb;
    TextView tv, prefix;
    CheckBox ch1incoming, ch2rejected, ch3missed, ch4blocked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saver);
        pb = findViewById(R.id.contactsProgressBar);
        prefix = findViewById(R.id.prefix);
        tv = findViewById(R.id.tv);
        ch1incoming = findViewById(R.id.ch1incoming);
        ch2rejected = findViewById(R.id.ch2rejected);
        ch3missed = findViewById(R.id.ch3missed);
        ch4blocked = findViewById(R.id.ch4blocked);
    }

    public class MyThread extends Thread {
        public void run() {
            getPerm();
        }
    }


    public void btnClick(View v) {
        pb.setVisibility(View.VISIBLE);
        MyThread myThread = new MyThread();
        myThread.start();
    }

    public void getIncomingCalls(final Context context) {

        final Uri callog = CallLog.Calls.CONTENT_URI;
        Cursor cursor = null;

        try {
            // Query all the columns of the records that matches "type=2"
            // (outgoing) and orders the results by "date"
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.

            }
            int a = -1, b = -1, c = -1, d = -1;
            if(ch1incoming.isChecked()) a = 1;
            if(ch2rejected.isChecked()) b = 6;
            if(ch3missed.isChecked()) c = 3;
            if(ch4blocked.isChecked()) d = 6;

            cursor = getContentResolver().query(callog, null,
                    CallLog.Calls.TYPE + " IN ("+a+", "+b+", "+c+", "+d+")",
                    null, CallLog.Calls.DATE);

            cursor.moveToLast();
            Log.i("IDDQD", "getLastOutgoingCallDuration: " + cursor.getCount());
            if (cursor.getCount() == 0) {
                tv.setText(R.string.nothingToImport);
                return;
            }
            int i = 0;
            do {
                final int numberCall = cursor
                        .getColumnIndex(CallLog.Calls.NUMBER);
                final int dateCell = cursor
                        .getColumnIndex(CallLog.Calls.DATE);
                String number = cursor.getString(numberCall);
                long date = Long.parseLong(cursor.getString(dateCell));
                Date date5 = new java.util.Date(date);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(" yyyy-MM-dd HH:mm:ss");
                String parsedDateTime = String.valueOf(simpleDateFormat.format(date5));
                if (!contactExists(SaverActivity.this, number)) {
                    WritePhoneContact(prefix.getText().toString() + parsedDateTime, number);
                    tv.setText(getString(R.string.importNum) + i++);
                }
                // do what ever you want here
            } while (cursor.moveToPrevious());
            if (i == 0)
                tv.setText(R.string.nothingToImport);
            else
                tv.setText(getString(R.string.importFin) + i + getString(R.string.contacts));
        } finally {
            // Close the resources
            if (cursor != null) {
                cursor.close();
                pb.setVisibility(View.INVISIBLE);
            }
        }
    }

    public void WritePhoneContact(String displayName, String number) {
        Context contetx = SaverActivity.this; //Application's context or Activity's context
        String strDisplayName = displayName; // Name of the Person to add
        String strNumber = number; //number of the person to add with the Contact
        System.out.println("NAME> " + strDisplayName + "    NUMBER ====>  " + strNumber);
        ArrayList<ContentProviderOperation> cntProOper = new ArrayList<ContentProviderOperation>();
        int contactIndex = cntProOper.size();//ContactSize

        //Newly Inserted contact
        // A raw contact will be inserted ContactsContract.RawContacts table in contacts database.
        cntProOper.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)//Step1
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null).build());

        //Display name will be inserted in ContactsContract.Data table
        cntProOper.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)//Step2
                .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID, contactIndex)
                .withValue(ContactsContract.RawContacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, strDisplayName) // Name of the contact
                .build());
        //Mobile number will be inserted in ContactsContract.Data table
        cntProOper.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)//Step 3
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, contactIndex)
                .withValue(ContactsContract.RawContacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, strNumber) // Number to be added
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE).build()); //Type like HOME, MOBILE etc
        try {
            // We will do batch operation to insert all above data
            //Contains the output of the app of a ContentProviderOperation.
            //It is sure to have exactly one of uri or count set
            ContentProviderResult[] contentProresult = null;
            contentProresult = contetx.getContentResolver().applyBatch(ContactsContract.AUTHORITY, cntProOper); //apply above data insertion into contacts list
        } catch (RemoteException exp) {
            //logs;
        } catch (OperationApplicationException exp) {
            //logs
        }
    }

    public boolean contactExists(Context context, String number) {
/// number is the phone number
        Uri lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number));
        String[] mPhoneNumberProjection = {ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.NUMBER, ContactsContract.PhoneLookup.DISPLAY_NAME};
        Cursor cur = context.getContentResolver().query(lookupUri, mPhoneNumberProjection, null, null, null);
        try {
            if (cur.moveToFirst()) {
                return true;
            }
        } finally {
            if (cur != null)
                cur.close();
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 42)
            getPerm();
    }

    public void getPerm() {
        int permissions_code = 42;
        String[] permissions = {Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                getIncomingCalls(SaverActivity.this);
            } else {
                ActivityCompat.requestPermissions(this, permissions, permissions_code);
            }
        } else getIncomingCalls(SaverActivity.this);

    }
}
