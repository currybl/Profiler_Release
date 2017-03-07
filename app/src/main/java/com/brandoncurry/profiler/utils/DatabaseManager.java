package com.brandoncurry.profiler.utils;

import com.brandoncurry.profiler.global.ProfilerApplication;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Created by BrandonCurry on 3/6/17.
 */

public class DatabaseManager {

    public static DatabaseManager databaseManager;

    public DatabaseManager(){
        databaseManager = new DatabaseManager();
    }

    public static DatabaseManager getInstance(){
        return databaseManager;
    }

    public static DatabaseReference getDatabase(){
        return ProfilerApplication.globalDatabase.getReference();
    }
}
