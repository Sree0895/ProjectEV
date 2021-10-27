package com.example.evchargeapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
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
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class bookSlot extends AppCompatActivity {

        private Button mBackButton,mBookButton,mUnBookButton;
        private Spinner mSpinnerState ;
        private Spinner mSpinnerDistricts;
        private static Spinner mSpinnerEvsList;
        private static TextView mBookTextView;


        private String host;
        private String clientId;
        private static mqttClass mqttInstance = null;
        private String userName;
        private String passWord = "";
        private static String mMqttRxdata;

        private static String mState = "";
        private static String mDistrict = "";
        private static String mEvcs = "";
        private String mEv = "";

        private static String mBookingSlotServiceTopic = "";
        private static String mBookingSlotRequestTopic = "";


        private static ListView mSlotListView;

        private static ArrayList<String> mDistrictList;
        private static ArrayList<String> mEvcsList;
        private static ArrayList<String> mEvcsInfoList;
        private static ArrayList<String> mSlotList;

        private static ArrayAdapter adapterEvs;
        private static ArrayAdapter adapterDistrict;
        private static ArrayAdapter adapterSlot;

        private static int mTimeSlotSelected = -1;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_book_slot);

            mDistrictList = new ArrayList<>();
            mDistrictList.clear();

            mEvcsList = new ArrayList<>();
            mEvcsList.clear();

            mEvcsInfoList = new ArrayList<>();
            mEvcsInfoList.clear();

            mSlotList = new ArrayList<>();
            mSlotList.clear();

            host = "tcp://" + config.getHost() + ":" + config.getPort();
            clientId = config.getClientId();


            mBookTextView = findViewById(R.id.bookText);
            mSpinnerState = (Spinner) findViewById(R.id.spinnerStates);
            mSpinnerDistricts = (Spinner) findViewById(R.id.spinnerDistricts);
            mSpinnerEvsList = findViewById(R.id.spinnerEvList);
            mSpinnerEvsList = (Spinner) findViewById(R.id.spinnerEvList);
            mSlotListView = (ListView) findViewById(R.id.timeslotListView);

            mSpinnerState.setBackgroundColor(Color.GRAY);
            mSpinnerDistricts.setBackgroundColor(Color.GRAY);
            mSpinnerEvsList.setBackgroundColor(Color.GRAY);
            mSlotListView.setBackgroundColor(Color.GRAY);

            readXml("slot.xml", mSlotList);
            adapterSlot = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_item, mSlotList);
            adapterSlot.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mSlotListView.setAdapter(adapterSlot);

            // Create an ArrayAdapter using the string array and a default spinner layout
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                    R.array.states_list, android.R.layout.simple_spinner_item);
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            // Apply the adapter to the spinner
            mSpinnerState.setAdapter(adapter);
            mSpinnerState.setSelection(0);

            mState = mSpinnerState.getSelectedItem().toString();
            mState.toLowerCase();
            readXml(mState.toLowerCase()+".xml", mDistrictList);

            mSpinnerState.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                    mState = mSpinnerState.getSelectedItem().toString();
                    mState.toLowerCase();
                    mDistrictList.clear();
                    mEvcsList.clear();
                    mEvcsInfoList.clear();
                    if(!mState.isEmpty()) {
                        readXml(mState.toLowerCase() + ".xml", mDistrictList);
                        adapterDistrict = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_item, mDistrictList);
                        adapterDistrict.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        mSpinnerDistricts.setAdapter(adapterDistrict);

                        adapterEvs = new ArrayAdapter<>(getApplicationContext(),android.R.layout.simple_spinner_item, mEvcsInfoList);
                        adapterEvs.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        // Apply the adapter to the spinner
                        mSpinnerEvsList.setAdapter(adapterEvs);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                }
            });


            mSpinnerDistricts.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                    mEvcsList.clear();
                    mEvcsInfoList.clear();
                    adapterEvs = new ArrayAdapter<>(getApplicationContext(),android.R.layout.simple_spinner_item, mEvcsInfoList);
                    adapterEvs.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    mSpinnerEvsList.setAdapter(adapterEvs);
                    mDistrict = mSpinnerDistricts.getSelectedItem().toString();

                    mqttInstance.unsubscribeTopic(mBookingSlotServiceTopic);
                    if (!mDistrict.isEmpty()) {
                        mBookingSlotServiceTopic = "topic/bookingSlotService/" + config.getEvVendor() + "/" + mState + "/" + mDistrict;
                        Log.i("Test1", mBookingSlotServiceTopic);
                        mqttInstance.subscribeTopic(mBookingSlotServiceTopic);
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                }
            });


        mSpinnerEvsList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {

                mEvcs = mSpinnerEvsList.getSelectedItem().toString();
                mqttInstance.unsubscribeTopic(mBookingSlotServiceTopic);

                if (!mDistrict.isEmpty()){
                    mBookingSlotServiceTopic = "topic/bookingSlotServiceEvcs/" + config.getEvVendor() + "/" + mState + "/" + mDistrict + "/" +  mEvcs;
                    mqttInstance.subscribeTopic(mBookingSlotServiceTopic);
                    Log.i("Test", "onItemSelected: " + mBookingSlotServiceTopic);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        mSlotListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    mTimeSlotSelected = 0;
                    BitSet bitSet = new BitSet();
                    bitSet.clear();
                    bitSet.set(position);
                    for (int bit = 0; bit < bitSet.length(); bit++) {
                        if (bitSet.get(bit)) {
                            mTimeSlotSelected |= (1 << bit);
                        }
                    }
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

            mBookButton = findViewById(R.id.book_button);
            mBookButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    JSONObject obj1 = new JSONObject();
                    String topic = "";
                    if( (mState.compareTo("") != 0)  && (mDistrict.compareTo("") != 0) &&  (mEvcs.compareTo("") != 0))
                    {
                        try {
                            topic = "topic/bookingSlotRequest/" +
                                    config.getEvVendor() + "/" + mState + "/" + mDistrict + "/" + mEvcs;
                            obj1.put("code", "9002");
                            obj1.put("user", config.getUserId());
                            obj1.put("evNumber", config.getVehicleId());
                            obj1.put("evChargerType", config.getChargerType());
                            obj1.put("evVehicleType", config.getVehicleType());
                            obj1.put("slotReq", Integer.toString(mTimeSlotSelected));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (mqttInstance != null) {
                            mqttInstance.publishMessage(topic, obj1.toString());
                        }
                    }
                    else
                    {
                        Toast.makeText(getBaseContext(), "Parameters incomplete", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            mUnBookButton = findViewById(R.id.unbook_button);
            mUnBookButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    JSONObject obj1 = new JSONObject();
                    String topic = "";
                    if( (mState.compareTo("") != 0)  && (mDistrict.compareTo("") != 0) &&  (mEvcs.compareTo("") != 0))
                    {
                        try {
                            topic = "topic/bookingSlotRequest/" +
                                    config.getEvVendor() + "/" + mState + "/" + mDistrict + "/" + mEvcs;
                            obj1.put("code", "9004");
                            obj1.put("user", config.getUserId());
                            obj1.put("evNumber", config.getVehicleId());
                            obj1.put("evChargerType", config.getChargerType());
                            obj1.put("evVehicleType", config.getVehicleType());
                            obj1.put("slotReq", Integer.toString(mTimeSlotSelected));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (mqttInstance != null) {
                            mqttInstance.publishMessage(topic, obj1.toString());
                        }
                    }
                    else
                    {
                        Toast.makeText(getBaseContext(), "Parameters incomplete", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            mqttInstance = new mqttClass(getApplicationContext(), host, clientId, "pi", passWord, mHandler);
            mqttInstance.mqttConnect("");
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
                        mqttInstance.subscribeTopic(mBookingSlotServiceTopic);
                    }
                }
                if (msg.what == 9)
                {
                    if(!bundle.getBoolean("subscribeFlag"))
                    {
                        mqttInstance.subscribeTopic(mBookingSlotServiceTopic);
                    }
                }
                if(msg.what == 8)
                {
                    mMqttRxdata = bundle.getString("mqttRxData");
                    extractData(mMqttRxdata);
                    //Log.i("Test", "Mqtt Data: " + mMqttRxdata );
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
                    case 9000:
                        if((object.getString("evcsDistrict").compareTo(mDistrict) == 0) && (object.getString("evcsState").compareTo(mState) == 0)  && (object.getString("evcsName").compareTo(mEvcs) == 0)) {
                            //Log.i("Test", object.getString("freeSlots"));
                        }
                        break;
                    case 9001:
                           if((object.getString("evcsDistrict").compareTo(mDistrict) == 0) && (object.getString("evcsState").compareTo(mState) == 0) ) {
                               if (!mEvcsList.contains(object.getString("evcsId"))) {
                                   mEvcsList.add(object.getString("evcsId"));
                                   mEvcsInfoList.add(object.getString("evcsName") );
                                   adapterEvs.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                   mSpinnerEvsList.setAdapter(adapterEvs);
                               }
                           }
                        break;
                    case 9003:
                        if((object.getString("evcsDistrict").compareTo(mDistrict) == 0) && (object.getString("evcsState").compareTo(mState) == 0)  && (object.getString("evcsName").compareTo(mEvcs) == 0)) {
                            if(object.getString("user").compareTo(config.getUserId()) == 0) {
                                Log.i("Test", "Booking Successful ");
                                mBookTextView.setText(object.getString("response"));
                            }
                        }
                        break;
                    case 9005:
                        if((object.getString("evcsDistrict").compareTo(mDistrict) == 0) && (object.getString("evcsState").compareTo(mState) == 0)  && (object.getString("evcsName").compareTo(mEvcs) == 0)) {
                            if(object.getString("user").compareTo(config.getUserId()) == 0) {
                                Log.i("Test", "Unbooking Successful");
                                mBookTextView.setText(object.getString("response"));
                            }
                        }
                        break;
                    default:
                        break;
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }