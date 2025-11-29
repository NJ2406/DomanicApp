package com.example.domaniconeapk;

import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MonitorActivity extends AppCompatActivity {
    RecyclerView rv;
    LogAdapter adapter;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_monitor);

        rv = findViewById(R.id.rvLogs);
        adapter = new LogAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        Button refresh = findViewById(R.id.btnRefresh);
        Button clear = findViewById(R.id.btnClear);

        refresh.setOnClickListener(v -> load());
        clear.setOnClickListener(v -> {
            LogStore.clear();
            load();
        });

        load();
    }

    void load() {
        List<LogStore.Action> list = LogStore.getAll();
        adapter.setItems(list);
    }
}
