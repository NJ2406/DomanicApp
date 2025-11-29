package com.example.domaniconeapk;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.VH> {
    List<LogStore.Action> items;
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public void setItems(List<LogStore.Action> list) {
        items = list;
        notifyDataSetChanged();
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false));
    }

    @Override
    public void onBindViewHolder(VH h, int pos) {
        LogStore.Action a = items.get(pos);
        h.t1.setText(a.type + " — " + a.device);
        h.t2.setText(df.format(new Date(a.ts)) + " • " + a.message);
    }

    @Override
    public int getItemCount() { return items == null ? 0 : items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView t1, t2;
        VH(View v) {
            super(v);
            t1 = v.findViewById(android.R.id.text1);
            t2 = v.findViewById(android.R.id.text2);
        }
    }
}
