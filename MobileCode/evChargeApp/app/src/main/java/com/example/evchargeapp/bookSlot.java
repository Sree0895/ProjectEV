package com.example.evchargeapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class bookSlot extends AppCompatActivity {
    Button mBackButton;
    Spinner mSpinnerState ;
    Spinner mSpinnerDistricts;

    private String host;
    private String clientId;
    private static mqttClass mqttInstance = null;
    private String userName;
    private String passWord = "";
    private static String mMqttRxdata;
    private String mState = "";
    private String mDistrict = "";
    private String mEv = "";
    private static String mBookingSlotTopic = "";
    private static ArrayList<String> mDistrictList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_slot);
        mDistrictList = new ArrayList<>();
        mDistrictList.clear();


        host = "tcp://" + config.getHost() + ":" + config.getPort();
        clientId = config.getClientId();

        mqttInstance = new mqttClass(getApplicationContext(), host, clientId, "pi", passWord, mHandler);

        mSpinnerState = (Spinner) findViewById(R.id.spinnerStates);
        mSpinnerDistricts = (Spinner) findViewById(R.id.spinnerDistricts);
        mSpinnerDistricts.setBackgroundColor(Color.WHITE);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.states_list, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mSpinnerState.setAdapter(adapter);

        mSpinnerState.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                mState = mSpinnerState.getSelectedItem().toString();
                mState.toLowerCase();
                mDistrictList.clear();
                readXml(mState.toLowerCase()+".xml", mDistrictList);
                ArrayAdapter adapterDistrict = new ArrayAdapter<>(getApplicationContext(),android.R.layout.simple_spinner_item, mDistrictList);
                adapterDistrict.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                mSpinnerDistricts.setAdapter(adapterDistrict);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        mState = mSpinnerState.getSelectedItem().toString();
        mState.toLowerCase();
        readXml(mState.toLowerCase()+".xml", mDistrictList);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter adapterDistrict = new ArrayAdapter<>(getApplicationContext(),android.R.layout.simple_spinner_item, mDistrictList);
        // Specify the layout to use when the list of choices appears
        adapterDistrict.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mSpinnerDistricts.setAdapter(adapterDistrict);

        mSpinnerDistricts.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                mDistrict=mSpinnerDistricts.getSelectedItem().toString();
                mBookingSlotTopic = "topic/bookingSlot/" + config.getEvVendor() + "/" + mState + "/" + mDistrict + "/" + mEv + "/" ;
                mqttInstance.mqttConnect(mBookingSlotTopic);
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        mBackButton = findViewById(R.id.back_button);
        mBackButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mqttInstance.mqttDisconnect();
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
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

    private static final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage (Message msg){
            Bundle bundle = msg.getData();
            if (msg.what == 7)
            {
                if(!bundle.getBoolean("publishFlag"))
                {
                    mqttInstance.subscribeTopic(mBookingSlotTopic);
                }
            }
            if (msg.what == 9)
            {
                if(!bundle.getBoolean("subscribeFlag"))
                {
                    mqttInstance.subscribeTopic(mBookingSlotTopic);
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
                    //
                    break;
                case 3002:
                    break;
                default:
                    break;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}