package com.example.satunetra_v3.helper;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseHelper {
    FirebaseDatabase database;
    DatabaseReference reference;

    public FirebaseHelper(){
        database = FirebaseDatabase.getInstance();
    }

    public DatabaseReference getReference() {
        return reference;
    }
}
