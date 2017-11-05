package com.md.openxcapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.MainThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.progresviews.ProgressLine;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.openxc.VehicleManager;
import com.openxc.measurements.AcceleratorPedalPosition;
import com.openxc.measurements.EngineSpeed;
import com.openxc.measurements.FuelConsumed;
import com.openxc.measurements.FuelLevel;
import com.openxc.measurements.Latitude;
import com.openxc.measurements.Longitude;
import com.openxc.measurements.Measurement;
import com.openxc.measurements.Odometer;
import com.openxc.measurements.VehicleSpeed;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "StarterActivity";

    private VehicleManager mVehicleManager;
    private ServiceConnection mServiceConnection;
    private EngineSpeed.Listener mListener;
    private VehicleSpeed.Listener  mVehicleSpeedListener;
    private Latitude.Listener mVehicleLatitudeListener;
    private Longitude.Listener mVehicleLongitudeListener;
    private FuelLevel.Listener mFuelLevelListener;
    private FuelConsumed.Listener mFuelConsumedListener;
    private AcceleratorPedalPosition.Listener mPedalPositionListener;
    private Odometer.Listener mOdometerListener;
    private FirebaseDataSender mFirebaseDataSender;
    private ValueEventListener mValueEventListener;
    private Query mNotificationsQuery;
    private ValueEventListener mMaxSpeedListener;
    private Query mMaxSpeedQuery;


    private TextView mVehicleSpeedView;
    private TextView mNotificationText;
    private ImageView mHudModeButtonImage;

    private boolean IsHudeModeEnabled = false;

    private TextView mFuelConsumed;
    private TextView mAverageFuel;
    private TextView mNotification;
    private ProgressLine mFuelProgress;


    private Boolean isStartingDistanceCreated = false;
    private Double startingKm;
    private Double fuelConsumedValue;
    private int mMaxSpeed = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG,"oncreate");
        IsHudeModeEnabled = getIntent().getBooleanExtra("IsHudModeEnabled",false);

        if(IsHudeModeEnabled) {
            setContentView(R.layout.activity_hud);
        }
        else {
            setContentView(R.layout.activity_main);

        }

        mHudModeButtonImage = findViewById(R.id.imgHeadUp);
        mHudModeButtonImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this,MainActivity.class);
                i.putExtra("IsHudModeEnabled",true);
                startActivity(i);
            }
        });



        mVehicleSpeedView = findViewById(R.id.txtVehicleSpeed);
        mNotificationText = findViewById(R.id.txtNotificationText);
        mFuelConsumed = findViewById(R.id.tv_fuel_consumed);
        mAverageFuel = findViewById(R.id.tv_average_fuel);
        mNotification = findViewById(R.id.txtNotificationText);
        mFuelProgress = findViewById(R.id.fuel_progress);

        Typeface myTypeface = Typeface.createFromAsset(getAssets(), "headlines.ttf");
        mVehicleSpeedView.setTypeface(myTypeface);
        //
        // mNotificationText.setTypeface(myTypeface);

        mNotificationText.setSelected(true);  // Set focus to the textview

        //mEngineSpeedView = (TextView) findViewById(R.id.speed_tv);
        initializeListeners();
        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.i(TAG, "Bound to VehicleManager");

                mVehicleManager = ((VehicleManager.VehicleBinder) service)
                        .getService();
                String vehicleId = getVehicleId();
                mFirebaseDataSender = FirebaseDataSender.getInstance(vehicleId);
                mVehicleManager.addListener(EngineSpeed.class, mListener);
                mVehicleManager.addListener(VehicleSpeed.class, mVehicleSpeedListener);
                mVehicleManager.addListener(Latitude.class, mVehicleLatitudeListener);
                mVehicleManager.addListener(Longitude.class, mVehicleLongitudeListener);
                mVehicleManager.addListener(FuelLevel.class, mFuelLevelListener);
                mVehicleManager.addListener(FuelConsumed.class, mFuelConsumedListener);
                mVehicleManager.addListener(Odometer.class, mOdometerListener);
                mVehicleManager.addListener(AcceleratorPedalPosition.class, mPedalPositionListener);
                //mVehicleManager.addListener(SteeringWheelAngle.class,mSteeringWheelAngleListener);
                //Log.i(TAG, "Vehicle id : " + mVehicleManager.getVehicleInterfaceDeviceId());

            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.w(TAG, "VehicleManager Service  disconnected unexpectedly");
                mVehicleManager = null;
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mVehicleManager == null) {
            Intent intent = new Intent(this, VehicleManager.class);
            bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void initializeListeners(){
        mListener = new EngineSpeed.Listener() {
            public void receive(final Measurement measurement) {
                sendValueToFirebase(measurement);
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        final EngineSpeed speed = (EngineSpeed) measurement;
                        int engineSpeed = speed.getValue().intValue();
                        if(engineSpeed > 10000)
                            engineSpeed = 10000;

                    }
                });
            }
        };

        mVehicleSpeedListener = new VehicleSpeed.Listener() {
            @Override
            public void receive(final Measurement measurement) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        sendValueToFirebase(measurement);
                        final VehicleSpeed speed = (VehicleSpeed) measurement;
                        int vehicleSpeed = speed.getValue().intValue();
                        if(vehicleSpeed > mMaxSpeed)
                            mVehicleSpeedView.setTextColor( getResources().getColor( R.color.md_red_900));
                        else
                            mVehicleSpeedView.setTextColor( getVehicleSpeedColor());
                        mVehicleSpeedView.setText(String.valueOf(vehicleSpeed));

                    }
                });
            }
        };

        mVehicleLatitudeListener = new Latitude.Listener() {
            @Override
            public void receive(Measurement measurement) {
                sendValueToFirebase(measurement);
            }
        };

        mVehicleLongitudeListener = new Longitude.Listener() {
            @Override
            public void receive(Measurement measurement) {
                sendValueToFirebase(measurement);
            }
        };

        mFuelConsumedListener = new FuelConsumed.Listener() {
            @Override
            public void receive(final Measurement measurement) {

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        FuelConsumed fuelConsumed = (FuelConsumed) measurement;
                        fuelConsumedValue = fuelConsumed.getValue().doubleValue();
                        //Log.d(TAG,"fuel consumed : " + fuelConsumedValue);
                        mFuelConsumed.setText(String.format("%.3f",fuelConsumedValue));
                    }
                });
            }
        };

        mFuelLevelListener = new FuelLevel.Listener() {
            @Override
            public void receive(final Measurement measurement) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String value = String.valueOf(measurement.getSerializedValue());
                        Double doubleValue = Double.valueOf(value);
                        FuelLevel fuelLevel = (FuelLevel) measurement;
                        mFuelProgress.setmPercentage(doubleValue.intValue());
                        mFuelProgress.setmValueText(String.valueOf(measurement.getValue()));
                    }
                });
            }
        };

        mOdometerListener = new Odometer.Listener() {
            @Override
            public void receive(final Measurement measurement) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Odometer odometer = (Odometer) measurement;
                        if (!isStartingDistanceCreated){
                            startingKm = odometer.getValue().doubleValue();
                            isStartingDistanceCreated = true;
                            //Log.d(TAG, "Starting km : " + startingKm);
                        }
                        if (fuelConsumedValue != null){
                            Double currentKm = odometer.getValue().doubleValue();
                            Double average = ( fuelConsumedValue / (currentKm  - startingKm)) * 100L;
                            //Log.d(TAG,"Average fuel consumed : " + average);
                            if (!average.equals(new Double("0.0")) && !Double.isInfinite(average)){
                                mAverageFuel.setText(String.format("%.3f",average));
                            }else {
                                mAverageFuel.setText("Calculating...");
                            }
                        }
                    }
                });
            }
        };

        mMaxSpeedQuery = FirebaseUtil.maxSpeedQuery();
        mMaxSpeedListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null){
                    Integer value = 50;
                    if (dataSnapshot.getValue() instanceof String){
                        String stringValue = (String) dataSnapshot.getValue();
                        mMaxSpeed = Integer.valueOf(stringValue);
                    } else if (dataSnapshot.getValue() instanceof Long){
                        Long longValue = (Long) dataSnapshot.getValue();
                        mMaxSpeed = longValue.intValue();
                    }
                    Log.d(TAG, "max speed : " + dataSnapshot.getValue());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        mMaxSpeedQuery.addValueEventListener(mMaxSpeedListener);

        mNotificationsQuery = FirebaseUtil.notificationsQuery(getVehicleId());
        mValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                Log.d(TAG,"ondatachange");
                    Log.d(TAG,"received " + dataSnapshot.getValue());
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (dataSnapshot.getValue() != null){
                                mNotification.setText(String.valueOf(dataSnapshot.getValue()));
                                playNotificationSound();
                            }
                        }
                    });

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        mNotificationsQuery.addValueEventListener(mValueEventListener);

        mPedalPositionListener = new AcceleratorPedalPosition.Listener() {
            @Override
            public void receive(Measurement measurement) {
                sendValueToFirebase(measurement);
            }
        };

    }

    private String getVehicleId(){
        //String vehicleId = mVehicleManager.getVehicleInterfaceDeviceId();
        return "5000";
        //return vehicleId == null ? "testVehicle" : vehicleId;
    }

    private void sendValueToFirebase(Measurement measurement){
        if (mFirebaseDataSender != null){
            mFirebaseDataSender.getVariables().put(measurement.getGenericName(), measurement);
        }
    }

     private void playNotificationSound(){
         try {
             Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
             Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
             r.play();
         } catch (Exception e) {
             Log.e(TAG,"exception occured while playing sound " + e);
         }
     }

    private int getVehicleSpeedColor(){
        if(IsHudeModeEnabled){
            return getResources().getColor(R.color.md_white_1000);
        } else {
            return getResources().getColor(R.color.md_teal_600);
        }
    }
}
