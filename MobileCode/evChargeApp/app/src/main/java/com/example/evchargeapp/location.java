package com.example.evchargeapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class location extends AppCompatActivity {

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
    private static String mLocationTopic = "";

    private GpsTracker gpsTracker;

    double mCurrLatitude ;
    double mCurrLongitude;
    double mDestLatitude ;
    double mDestLongitude;

    ListView mLocationListView;
    List<String> tutorials = new ArrayList<>();

    private static ArrayList<String> mDistrictList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        mDistrictList = new ArrayList<>();
        mDistrictList.clear();

        try {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        gpsTracker = new GpsTracker(location.this);
        if(gpsTracker.canGetLocation()){
            mCurrLatitude = gpsTracker.getLatitude();
            mCurrLongitude = gpsTracker.getLongitude();
            Log.d("Test", mCurrLatitude + " " + mCurrLongitude);
        }else{
            gpsTracker.showSettingsAlert();
        }

        host = "tcp://" + config.getHost() + ":" + config.getPort();
        clientId = config.getClientId();

        mqttInstance = new mqttClass(getApplicationContext(), host, clientId, "pi", passWord, mHandler);

        mSpinnerState = (Spinner) findViewById(R.id.spinnerStates);
        mSpinnerDistricts = (Spinner) findViewById(R.id.spinnerDistricts);
        mSpinnerDistricts.setBackgroundColor(Color.WHITE);
        mSpinnerState = (Spinner) findViewById(R.id.spinnerStates);
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
        ArrayAdapter<CharSequence> adapterDistrict = ArrayAdapter.createFromResource(this,
                R.array.Telangana, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapterDistrict.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mSpinnerDistricts.setAdapter(adapterDistrict);

        mSpinnerDistricts.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                mDistrict=mSpinnerDistricts.getSelectedItem().toString();
                mLocationTopic = "topic/locations/" + config.getEvVendor() + "/" + mState + "/" + mDistrict  ;
                mqttInstance.mqttConnect(mLocationTopic);
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

/*        Bundle bundle = getIntent().getExtras();
        ArrayList<String> arraylist = (ArrayList<String>)getIntent().getSerializableExtra("key");
        ArrayList<JSONObject> jsonlist = new ArrayList<JSONObject>();

        int i =0;
        for (String temp : arraylist) {
            try {

                    JSONArray array = new JSONArray( "["+ temp + "]");
                    JSONObject object = array.getJSONObject(0);
                    String evcsName = object.getString("evcsName");
                    double lattitude = object.getDouble("evcsLat");
                    double longitude = object.getDouble("evcsLon");
                    double distance = getDistance(mCurrLatitude,lattitude,mCurrLongitude,longitude);
                    String display = evcsName + " located at " +  distance;
                    tutorials.add(display);
                    jsonlist.add(object);
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }*/

        mLocationListView = findViewById(R.id.locationListView);
        ArrayAdapter<String> arr;
        arr = new ArrayAdapter<String>(
                this,
                R.layout.support_simple_spinner_dropdown_item,
                tutorials);
        mLocationListView.setAdapter(arr);

        mLocationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
/*                try {
                    JSONObject object = jsonlist.get(position);
                    mDestLatitude = object.getDouble("evcsLat");
                    mDestLongitude = object.getDouble("evcsLon");
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }*/

                final Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://maps.google.com/maps?"
                                + "saddr="+ mCurrLatitude+","+mCurrLongitude + "&daddr="+ mDestLatitude+","+mDestLongitude  ));

                intent.setClassName("com.google.android.apps.maps","com.google.android.maps.MapsActivity");

                startActivity(intent);
            }
        });
    }

    public double getDistance(double currlat,
                                     double accidlat, double currlon,
                                     double accidlon)
    {
        // The math module contains a function
        // named toRadians which converts from
        // degrees to radians.
        currlon = Math.toRadians(currlon);
        accidlon = Math.toRadians(accidlon);
        currlat = Math.toRadians(currlat);
        accidlat = Math.toRadians(accidlat);

        // Haversine formula
        double dlon = accidlon - currlon;
        double dlat = accidlat - currlat;
        double a = Math.pow(Math.sin(dlat / 2), 2)
                + Math.cos(currlat) * Math.cos(accidlat)
                * Math.pow(Math.sin(dlon / 2),2);

        double c = 2 * Math.asin(Math.sqrt(a));

        // Radius of earth in kilometers. Use 3956
        // for miles
        double r = 6371;

        // calculate the result
        return(c * r );
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
                    mqttInstance.subscribeTopic(mLocationTopic);
                }
            }
            if (msg.what == 9)
            {
                if(!bundle.getBoolean("subscribeFlag"))
                {
                    mqttInstance.subscribeTopic(mLocationTopic);
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