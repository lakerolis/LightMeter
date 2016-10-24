package com.falch.lightmeter;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    TextView tw;
    Calendar c;
    private ArrayList<Integer> sensorValues = new ArrayList<>();
    private SensorManager mSensorManager;
    private SensorEventListener mEventListenerLight;
    private int lastLightValue;
    private boolean isSensorStarted = false;

    private void updateUI(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tw.setText("Latest 10 values");
                if (sensorValues.size() > 10) {
                    for (int i = sensorValues.size() - 10; i < sensorValues.size(); i++) {
                        tw.append(System.getProperty("line.separator"));
                        tw.append(Float.toString(sensorValues.get(i)));
                    }
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button startButton = (Button) findViewById(R.id.startB);
        tw = (TextView) findViewById(R.id.textArea);

        final ToggleButton onOff = (ToggleButton) findViewById(R.id.toggleButton);
        Button redButton = (Button) findViewById(R.id.button2);
        Button blueButton = (Button) findViewById(R.id.button3);
        Button resetButton = (Button) findViewById(R.id.button4);
        SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar4);

        onOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(!isChecked){
                    onOff.setText("off");
                    sendRawPostRequest("{\"on\":true}");
                }else{
                    onOff.setText("on");
                    sendRawPostRequest("{\"on\":false}");
                }
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendRawPostRequest("{\"on\":true,\"xy\":[0.3227,0.329],\"bri\":255}");
            }
        });

        redButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendRawPostRequest("{\"on\":true,\"xy\":[0.7,0.2986]}");
            }
        });

        blueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendRawPostRequest("{\"on\":true,\"xy\":[0.139,0.081]}");
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                this.progress = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                sendRawPostRequest("{\"bri\":"+progress+"}");
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            TextView sensorID = (TextView) findViewById(R.id.sensorID);
            @Override
            public void onClick(View v) {
                addToast("sensor started");
                isSensorStarted = true;
                new Timer().scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        if (sensorValues.size() > 10) {
                            int avgLxValue = 0;
                            for (int i = sensorValues.size() - 10; i < sensorValues.size(); i++) {
                                avgLxValue = avgLxValue + sensorValues.get(i);
                            }
                            Log.i("avgLxValue", Float.toString(avgLxValue / 10));
                            sendPostRequest((avgLxValue / 10), sensorID.getText().toString());
                        }
                    }
                }, 0, 5000);
            }
        });

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mEventListenerLight = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float[] values = event.values;
                lastLightValue = (int)values[0];
                sensorValues.add(lastLightValue);
                Log.i("SENSOR_CHANGED",Float.toString(lastLightValue));
                if(isSensorStarted) {
                    updateUI();
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
    }


    @Override
    protected void onResume() {
        mSensorManager.registerListener(mEventListenerLight, mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_UI);
        super.onResume();
    }

    private void sendGetRequest(){
        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="http://www.google.com";
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        Log.i("REQUEST_RESPONSE","Response is: "+ response.substring(0,500));
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i("ERROR","That didn't work!");
            }
        });
// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void sendRawPostRequest(String input){
        RequestQueue queue = Volley.newRequestQueue(this);
        final String URL = "http://192.168.1.100:3002/sensorinput";
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("id","1337");
        params.put("value",input);

        JsonObjectRequest req = new JsonObjectRequest(URL, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            VolleyLog.v("Response:%n %s", response.toString(4));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.e("Error: ", error.getMessage());
            }
        });
        queue.add(req);
    }

    private void sendPostRequest(float value, String id){
        RequestQueue queue = Volley.newRequestQueue(this);
//        final String URL = "http://192.168.0.28:3002/sensorinput"; Home Wifi
        final String URL = "http://192.168.1.100:3002" +
                "/sensorinput"; //Linksys router

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("id", id);
        params.put("value", Float.toString(value));

        JsonObjectRequest req = new JsonObjectRequest(URL, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            VolleyLog.v("Response:%n %s", response.toString(4));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.e("Error: ", error.getMessage());
            }
        });
        queue.add(req);
    }

    private void addToast(String text){
        Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
        toast.show();
    }
}
