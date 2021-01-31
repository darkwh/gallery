package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class DemoAdapter extends RecyclerView.Adapter<DemoAdapter.ViewHolder> {

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_demo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.item_id.setText(position+"");
    }

    @Override
    public int getItemCount() {
        return 120;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView item_id;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            item_id = itemView.findViewById(R.id.item_id);
        }
    }
}
