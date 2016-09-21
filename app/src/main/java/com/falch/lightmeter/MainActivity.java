package com.falch.lightmeter;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    TextView tw;
    Calendar c;
    private ArrayList<Float> sensorValues = new ArrayList<>();
    private SensorManager mSensorManager;
    private SensorEventListener mEventListenerLight;
    private float lastLightValue;

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

        tw = (TextView) findViewById(R.id.textArea);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mEventListenerLight = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float[] values = event.values;
                lastLightValue = values[0];
                sensorValues.add(lastLightValue);
                Log.i("SENSOR_CHANGED",Float.toString(lastLightValue));
                updateUI();
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                c = Calendar.getInstance();
                Log.i("SEND_DATA",Integer.toString(c.get(Calendar.SECOND)));
                sendRequest();
            }
        }, 0, 3000);
    }

    @Override
    protected void onResume() {
        mSensorManager.registerListener(mEventListenerLight, mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_UI);
        super.onResume();
    }

    private void sendRequest(){
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
}
