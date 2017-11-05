package com.md.openxcapp;

import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.openxc.measurements.EngineSpeed;
import com.openxc.measurements.Measurement;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class FirebaseDataSender {

    private static final String TAG = "FirebaseDataSender";
    private static final int SENDER_PERIOD = 1000;
    private static final int SENDER_DELAY = 0;

    private String mVehicleId;
    private DatabaseReference mFirebaseDatabaseRef;

    private Map<String, Measurement> variables = new HashMap<>();


    private FirebaseDataSender(String vehicleId){
        this.mVehicleId = vehicleId;
        mFirebaseDatabaseRef = FirebaseDatabase.getInstance().getReference();
        Log.i(TAG," FirebaseDataSender initialized with vehicleId : " + vehicleId);
        if (mVehicleId != null){
            initializeTimer();
        } else {
            Log.e(TAG,"Vehicle Id cannot be resolved. No data will send to firebase");
        }
    }

    private void initializeTimer(){
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                sendMessage();
            }
        },SENDER_DELAY,SENDER_PERIOD);
    }

    public static FirebaseDataSender getInstance(String vehicleId){
        return new FirebaseDataSender(vehicleId);
    }

    public Map<String, Measurement> getVariables() {
        return variables;
    }

    private void sendMessage(){

        for (String key: variables.keySet()){
            Measurement measurement ;
            if ((measurement = variables.get(key)) != null){
                //Log.d(TAG, "Sending "+ measurement.getGenericName() +" data : " + measurement);
                mFirebaseDatabaseRef.child(mVehicleId).child(measurement.getGenericName()).child(String.valueOf(measurement.getBirthtime())).setValue(measurement);
            }
        }
    }

}
