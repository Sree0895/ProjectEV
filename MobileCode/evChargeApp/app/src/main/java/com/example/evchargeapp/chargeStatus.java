package com.example.evchargeapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class chargeStatus extends AppCompatActivity {

    static ProgressBar mProgessBar;
    ImageButton mScanBtn;
    static ImageButton mControlBtn;
    static TextView mChargeStatusText;
    static TextView mUserText,mUserTextVal;
    static TextView mVehicleNumberText,mVehicleNumberTextVal;
    static TextView mStartTimeText,mStartTimeTextVal;
    static TextView mEndTimeText,mEndTimeTextVal;
    private static Group mGroup;
    static String SubChargeControlTopic = "";
    static String PubChargeControlTopic = "";
    private String host;
    private String clientId;
    private static mqttClass mqttInstance = null;
    private String userName;
    private String passWord = "";
    private static String mMqttRxdata;
    private static boolean mControlToggleState = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charge_status);

//        host = "tcp://" + "192.168.1.21" + ":" + "1883";
        clientId = "";

        host = "tcp://" + config.getHost() + ":" + config.getPort();
        //clientId = config.getClientId();

        mqttInstance = new mqttClass(getApplicationContext(), host, clientId, "pi", passWord, mHandler);
        mqttInstance.mqttConnect("");
        mGroup= findViewById(R.id.group);
        mGroup.setVisibility(View.INVISIBLE);
        mProgessBar = findViewById(R.id.progressBar);
        mProgessBar.setProgress(0);
        mProgessBar.setVisibility(View.INVISIBLE);


        mChargeStatusText= findViewById(R.id.chargeValText);
        mChargeStatusText.setText("----");
        mUserText= findViewById(R.id.userNameText);
        mVehicleNumberText= findViewById(R.id.vehicleNumberText);
        mStartTimeText= findViewById(R.id.startTimeText);
        mEndTimeText= findViewById(R.id.endTimeText);

        mUserTextVal= findViewById(R.id.userNameTextVal);
        mVehicleNumberTextVal= findViewById(R.id.vehicleNumberTextVal);
        mStartTimeTextVal= findViewById(R.id.startTimeTextVal);
        mEndTimeTextVal= findViewById(R.id.endTimeTextVal);

        mControlBtn = findViewById(R.id.controlbtn);
        mControlBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( true == mControlToggleState )
                {
                    mScanBtn.setVisibility(View.INVISIBLE);
                    mControlToggleState = false;
                    mControlBtn.setImageResource(R.drawable.charge_off);
                    mqttInstance.subscribeTopic(SubChargeControlTopic);
                    mProgessBar.setVisibility(View.VISIBLE);
                    requestChargeOn();
                    Toast.makeText(getBaseContext(), "Turned On", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    mScanBtn.setVisibility(View.VISIBLE);
                    mqttInstance.unsubscribeTopic(SubChargeControlTopic);
                    mControlToggleState = true;
                    mControlBtn.setImageResource(R.drawable.charge_on);
                    requestChargeOff();
                    Toast.makeText(getBaseContext(), "Turned Off", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mScanBtn = findViewById(R.id.scanBtn);
        mScanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // we need to create the object
                // of IntentIntegrator class
                // which is the class of QR library
                IntentIntegrator intentIntegrator = new IntentIntegrator(chargeStatus.this);
                intentIntegrator.setPrompt("Scan QR Code");
                //intentIntegrator.setCaptureActivity(CaptureActivityPortrait.class);
                //intentIntegrator.setScanningRectangle(450, 450);

                //intentIntegrator.setOrientationLocked(true);
                intentIntegrator.initiateScan();
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        // if the intentResult is null then
        // toast a message as "cancelled"
        if (intentResult != null) {
            if (intentResult.getContents() == null)
            {
                mGroup.setVisibility(View.INVISIBLE);
            }
            else
            {
                extractScanData(intentResult.getContents());
                //mGroup.setVisibility(View.VISIBLE);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private static final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage (Message msg){
            Bundle bundle = msg.getData();
            if (msg.what == 7)
            {
                if(!bundle.getBoolean("publishFlag"))
                {
                    mqttInstance.subscribeTopic(SubChargeControlTopic);
                }
            }
            if (msg.what == 9)
            {
                if(!bundle.getBoolean("subscribeFlag"))
                {
                    mqttInstance.subscribeTopic(SubChargeControlTopic);
                }
            }
            if(msg.what == 8)
            {
                mMqttRxdata = bundle.getString("mqttRxData");
                extractData(mMqttRxdata);
                Log.i("Test", "Mqtt Data: " + mMqttRxdata );
            }
        }
    };

    private static void extractData(String str) {
        try {
            JSONArray array = new JSONArray( "["+ str + "]");
            JSONObject object = array.getJSONObject(0);
            int code = object.getInt("code");
            switch (code)
            {
                case 3003:
                    mUserTextVal.setText(object.getString("user"));
                    mVehicleNumberTextVal.setText(object.getString("evNumber"));
                    mStartTimeTextVal.setText(object.getString("startTime"));
                    mEndTimeTextVal.setText(object.getString("endTime"));
                    double chargeLevel = object.getDouble("currCharge") * 100;
                    if(chargeLevel >= 100) {
                        mControlBtn.setImageResource(R.drawable.charge_on);
                        mControlToggleState = true;
                    }
                    mChargeStatusText.setText(Integer.toString((int)chargeLevel) + "%") ;
                    mProgessBar.setProgress((int)chargeLevel);
                    break;
                case 3002:
                    mGroup.setVisibility(View.VISIBLE);
                    break;
                default:
                    break;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private static void extractScanData(String str) {
        try {
            JSONArray array = new JSONArray( "["+ str + "]");
            JSONObject object = array.getJSONObject(0);
            //SubChargeControlTopic = "topic/"  + object.getString("evcsManufacturer") + "/" + object.getString("evcsId") +  "/userData/" + "ssmr";
            SubChargeControlTopic = "topic/userData/"  + object.getString("evcsManufacturer") + "/" +
                     object.getString("evcsState") +  "/" + object.getString("evcsDistrict") +  "/" +
                     object.getString("evcsName") +  "/" + object.getString("evcsId") +  "/" +
                    config.getUserId();
            Log.i("TAG", "extractScanData: " + SubChargeControlTopic);
            requestEVCSservice(object);
            mqttInstance.subscribeTopic(SubChargeControlTopic);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private static void requestEVCSservice(JSONObject object)
    {
        JSONObject obj1 = new JSONObject();
        try {
            PubChargeControlTopic = "topic/userService/"  + object.getString("evcsManufacturer") + "/" +
                    object.getString("evcsState") + "/" + object.getString("evcsDistrict") + "/" + object.getString("evcsName") +  "/" + object.getString("evcsId") +  "/" +
                    config.getUserId() +  "/" + config.getVehicleId();
            obj1.put("code", "3001");
            obj1.put("authReq", "true");
            obj1.put("user", config.getUserId() );
            obj1.put("evNumber", config.getVehicleId());
            obj1.put("evChargerType", config.getChargerType());
            obj1.put("evVehicleType", config.getVehicleType());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if(mqttInstance != null)
            mqttInstance.publishMessage(PubChargeControlTopic, obj1.toString());
    }

    private static void requestChargeOn()
    {
        JSONObject obj1 = new JSONObject();
        try {
            obj1.put("code", "101");
            obj1.put("user", config.getUserId() );
            obj1.put("evNumber", config.getVehicleId());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if(mqttInstance != null)
           mqttInstance.publishMessage(PubChargeControlTopic, obj1.toString());
    }

    private static void requestChargeOff()
    {
        JSONObject obj1 = new JSONObject();
        try {
            obj1.put("code", "102");
            obj1.put("user", config.getUserId() );
            obj1.put("evNumber", config.getVehicleId());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(mqttInstance != null)
            mqttInstance.publishMessage(PubChargeControlTopic, obj1.toString());
    }
}