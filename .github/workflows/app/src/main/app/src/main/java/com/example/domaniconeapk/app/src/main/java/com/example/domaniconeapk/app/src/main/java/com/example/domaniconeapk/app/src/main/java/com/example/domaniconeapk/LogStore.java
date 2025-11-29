package com.example.domaniconeapk;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.reflect.TypeToken;
import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class LogStore {
    static final String PREF = "domanic_log";
    static final String KEY = "actions";

    static Gson gson = new Gson();

    public static class Action {
        public long ts;
        public String device;
        public String type;
        public String message;
        public Action(String device, String type, String message) {
            this.ts = System.currentTimeMillis();
            this.device = device;
            this.type = type;
            this.message = message;
        }
    }

    static SharedPreferences prefs() {
        return App.getCtx().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static void logAction(Action a) {
        try {
            List<Action> all = getAll();
            all.add(0, a);
            prefs().edit().putString(KEY, gson.toJson(all)).apply();
        } catch (Exception ignored) {}
    }

    public static List<Action> getAll() {
        try {
            String s = prefs().getString(KEY, null);
            if (s == null) return new ArrayList<>();
            Type t = new TypeToken<ArrayList<Action>>(){}.getType();
            return gson.fromJson(s, t);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static void clear() {
        prefs().edit().remove(KEY).apply();
    }
}
