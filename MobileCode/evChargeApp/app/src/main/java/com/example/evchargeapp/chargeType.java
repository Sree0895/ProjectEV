package com.example.evchargeapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class chargeType extends AppCompatActivity {

    Button mNextButton;
    Spinner mSpinner;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charge_type);


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

        mNextButton = findViewById(R.id.next_button);
        mNextButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), chargeStatus.class);
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
}