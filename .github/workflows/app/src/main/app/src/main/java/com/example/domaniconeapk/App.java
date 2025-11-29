package com.example.domaniconeapk;

import android.app.Application;
import android.content.Context;

public class App extends Application {
    static Context ctx;
    @Override public void onCreate() { super.onCreate(); ctx = getApplicationContext(); }
    public static Context getCtx() { return ctx; }
}
