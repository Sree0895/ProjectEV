package com.example.evchargeapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class config extends AppCompatActivity {

    TextView mSettingsTextView,mUserIdText,mVehicleIdText,mManufacturerIdText,mChargerTypeText,mVehicleTypeText;
    Button mNextButton;

    EditText mUserIdEditText,mVehicleIdEditText,mManufacturerEditText,mChargerTypeEditText,mVehicleTypeEditText;

    private static String TAG = "ConfigActivity";
    private static String mHost = "192.168.1.21"; //""au2f39iu6fuus-ats.iot.us-east-2.amazonaws.com";
    private static String mPort = "1883";
    private static String mClientId = "awsMobile1";
    private static String mUserId = null;
    private static String mVehicleId = null;
    private static String mEvVendor = null;
    private static String mChargerType = null;
    private static String mVehicleType = null;


    public static String getHost() {
        return mHost;
    }

    public static String getPort() {
        return mPort;
    }

    public static String getClientId() {
        return mClientId;
    }

    public static String getUserId() {
        return mUserId;
    }

    public static String getChargerType() {
        return mChargerType;
    }

    public static String getVehicleType() {
        return mVehicleType;
    }

    public static String getVehicleId() {
        return mVehicleId;
    }

    public static String getEvVendor() {
        return mEvVendor;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        mSettingsTextView = findViewById(R.id.settingsTextView);

        mUserIdText = findViewById(R.id.userIdText);
        mUserIdEditText = findViewById(R.id.userIdEditText);

        mVehicleIdText = findViewById(R.id.vehicleIdText);
        mVehicleIdEditText = findViewById(R.id.vehicleIdEditText);

        mManufacturerIdText = findViewById(R.id.manufacturerIdText);
        mManufacturerEditText = findViewById(R.id.manufacturerEditText);

        mChargerTypeText = findViewById(R.id.chargerTypeText);
        mChargerTypeEditText = findViewById(R.id.chargerTypeEditText);

        mVehicleTypeText = findViewById(R.id.vehicleTypeText);
        mVehicleTypeEditText = findViewById(R.id.vehicleTypeEditText);

        mNextButton = findViewById(R.id.next_button);
        mNextButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                mUserId = mUserIdEditText.getText().toString();
                mVehicleId = mVehicleIdEditText.getText().toString();
                mEvVendor = mManufacturerEditText.getText().toString();
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }


    private void showToast(String msg)
    {
        Toast.makeText( this,msg,Toast.LENGTH_SHORT).show();
    }

}