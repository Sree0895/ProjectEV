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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class chargeStatus extends AppCompatActivity {

    private static Spinner mSpinner;
    private RadioGroup radioGroup;

    private static ArrayList<String> mOptionList;

    private static int mOption = 0;
    private static int mOptionParam = 0;

    public static int getOption() {
        return mOption;
    }

    public static int getOptionParam() {
        return mOptionParam;
    }

    static ProgressBar mProgessBar;
    static ImageButton mScanBtn;
    static ImageButton mControlBtn;
    static TextView mChargeStatusText;
    static TextView mUserText,mUserTextVal;
    static TextView mVehicleNumberText,mVehicleNumberTextVal;
    static TextView mStartTimeText,mStartTimeTextVal;
    static TextView mEndTimeText,mEndTimeTextVal;
    static TextView mChargeConText,mChargeConTextVal;
    static TextView mCostText,mCostTextVal;
    static ImageView mBatImageView;
    private static Group mGroup;
    private static Group mGroup2;
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

        clientId = "";
        host = "tcp://" + config.getHost() + ":" + config.getPort();

        mqttInstance = new mqttClass(getApplicationContext(), host, clientId, "pi", passWord, mHandler);
        mqttInstance.mqttConnect("");
        mGroup= findViewById(R.id.group);
        mGroup2= findViewById(R.id.group1);
        mGroup.setVisibility(View.INVISIBLE);
        mProgessBar = findViewById(R.id.progressBar);
        mProgessBar.setProgress(0);
        mProgessBar.setVisibility(View.INVISIBLE);


        mBatImageView = findViewById(R.id.imageView2);
        mChargeStatusText= findViewById(R.id.chargeValText);
        mChargeStatusText.setText("----");
        mUserText= findViewById(R.id.userNameText);
        mVehicleNumberText= findViewById(R.id.vehicleNumberText);
        mStartTimeText= findViewById(R.id.startTimeText);
        mEndTimeText= findViewById(R.id.endTimeText);
        mChargeConText= findViewById(R.id.chargeConText);
        mCostText= findViewById(R.id.costText);

        mUserTextVal= findViewById(R.id.userNameTextVal);
        mVehicleNumberTextVal= findViewById(R.id.vehicleNumberTextVal);
        mStartTimeTextVal= findViewById(R.id.startTimeTextVal);
        mEndTimeTextVal= findViewById(R.id.endTimeTextVal);
        mChargeConTextVal= findViewById(R.id.chargeConTextVal);
        mCostTextVal= findViewById(R.id.costTextVal);

        mControlBtn = findViewById(R.id.controlbtn);
        mControlBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( true == mControlToggleState )
                {
                    mScanBtn.setVisibility(View.INVISIBLE);
                    mGroup2.setVisibility(View.INVISIBLE);
                    mSpinner.setVisibility(View.INVISIBLE);
                    mControlToggleState = false;
                    mControlBtn.setImageResource(R.drawable.charge_off);
                    mqttInstance.subscribeTopic(SubChargeControlTopic);
                    mProgessBar.setVisibility(View.VISIBLE);
                    requestChargeOn();
                    Toast.makeText(getBaseContext(), "Turned On", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    //mqttInstance.unsubscribeTopic(SubChargeControlTopic);
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

        mOptionList = new ArrayList<>();
        mOptionList.clear();

        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton rb = (RadioButton) group.findViewById(checkedId);
                if (null != rb ) {
                    String text = (String) rb.getText();
                    if( text.compareTo("User control charging") == 0)
                    {
                        mSpinner.setVisibility(View.INVISIBLE);
                        mOption = 0;
                    }
                    if( text.compareTo("Time-based charging") == 0 )
                    {
                        mOption = 1;
                        mOptionList.clear();
                        // Create an ArrayAdapter using the string array and a default spinner layout
                        readXml("time.xml", mOptionList);
                        ArrayAdapter adapterTime = new ArrayAdapter<>(getApplicationContext(),android.R.layout.simple_spinner_item, mOptionList);
                        adapterTime.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        // Apply the adapter to the spinner
                        mSpinner.setAdapter(adapterTime);
                        mSpinner.setVisibility(View.VISIBLE);
                    }
                    if( text.compareTo("Percentage-based charging") == 0)
                    {
                        mOption = 2;
                        mOptionList.clear();
                        // Create an ArrayAdapter using the string array and a default spinner layout
                        readXml("percent.xml", mOptionList);
                        ArrayAdapter adapterTime = new ArrayAdapter<>(getApplicationContext(),android.R.layout.simple_spinner_item, mOptionList);
                        adapterTime.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        // Apply the adapter to the spinner
                        mSpinner.setAdapter(adapterTime);
                        mSpinner.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        mSpinner = (Spinner) findViewById(R.id.spinnerTime);
        mSpinner.setVisibility(View.INVISIBLE);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                String[] tempList = mSpinner.getSelectedItem().toString().split(" ");
                mOptionParam = Integer.parseInt(tempList[0]);  ;
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

    }

    private void readXml(String fileName,ArrayList<String> arrlist )
    {
        try {
            InputStream is = getApplicationContext().getAssets().open(fileName);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(is);

            Element element=doc.getDocumentElement();
            element.normalize();
            NodeList nList = doc.getElementsByTagName("item");
            for (int i=0; i<nList.getLength(); i++) {
                Node node = nList.item(i);
                arrlist.add(node.getTextContent());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
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
                Log.i("TAG", "handleMessage: " + mMqttRxdata);
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
                     if( (object.getString("user").compareTo(config.getUserId()) == 0)  && (object.getString("evNumber").compareTo(config.getVehicleId()) == 0)) {
                         mUserTextVal.setText(object.getString("user"));
                         mVehicleNumberTextVal.setText(object.getString("evNumber"));
                         mStartTimeTextVal.setText(object.getString("startTime"));
                         mEndTimeTextVal.setText(object.getString("endTime"));
                         String temp = object.getString("evChargeOption");
                         if (temp.compareTo("0") == 0)
                             mChargeConTextVal.setText("UserControl");
                         else if (temp.compareTo("1") == 0)
                             mChargeConTextVal.setText("Time-Based");
                         else if (temp.compareTo("2") == 0)
                             mChargeConTextVal.setText("Percent-Based");
                         mCostTextVal.setText("TBD");
                         double chargeLevel = object.getDouble("currCharge") * 100;
                         mChargeStatusText.setText(Integer.toString((int) chargeLevel) + "%");
                         mProgessBar.setProgress((int) chargeLevel);
                     }
                    break;
                case 3002:
                    if( (object.getString("user").compareTo(config.getUserId()) == 0)  && (object.getString("evNumber").compareTo(config.getVehicleId()) == 0)) {

                        mGroup.setVisibility(View.VISIBLE);
                        mScanBtn.setVisibility(View.INVISIBLE);
                        mGroup2.setVisibility(View.INVISIBLE);
                        mSpinner.setVisibility(View.INVISIBLE);
                    }
                    break;
                case 3005:
                    if( (object.getString("user").compareTo(config.getUserId()) == 0)  && (object.getString("evNumber").compareTo(config.getVehicleId()) == 0)) {
                        mControlBtn.setImageResource(R.drawable.charge_on);
                        mControlToggleState = true;
                        mGroup.setVisibility(View.INVISIBLE);
                        mScanBtn.setVisibility(View.VISIBLE);
                        mGroup2.setVisibility(View.VISIBLE);
                        if(object.getString("evChargeOption").compareTo("0") != 0)
                            mSpinner.setVisibility(View.VISIBLE);
                        mqttInstance.unsubscribeTopic(SubChargeControlTopic);
                    }
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
            SubChargeControlTopic = "topic/userData/"  + object.getString("evcsManufacturer") + "/" +
                     object.getString("evcsState") +  "/" + object.getString("evcsDistrict") +  "/" +
                     object.getString("evcsName") +  "/" + object.getString("evcsId") +  "/" +
                    config.getUserId();
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
            obj1.put("evChargeOption", Integer.toString(getOption()));
            obj1.put("evChargeOptionParam", Integer.toString(getOptionParam()));
            //Log.i("TestReq", "requestEVCSservice: " + obj1.toString());
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

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        finish();
    }
}