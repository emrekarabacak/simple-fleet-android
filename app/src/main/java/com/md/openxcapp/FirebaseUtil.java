package com.md.openxcapp;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

/**
 * Created by apple on 4.11.2017.
 */

public class FirebaseUtil {


    public static Query notificationsQuery(String vehicleId){
        return FirebaseDatabase.getInstance().getReference().child("notification").child("message");
    }

    public static Query maxSpeedQuery(){
        return FirebaseDatabase.getInstance().getReference().child("maxSpeed").child("max");
    }


}
