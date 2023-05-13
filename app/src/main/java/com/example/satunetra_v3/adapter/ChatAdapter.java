package com.example.satunetra_v3.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.satunetra_v3.R;
import com.example.satunetra_v3.model.Message;
import com.google.firebase.database.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
    private final List<Message> messageList;
    private final int SELF = 100;

    public ChatAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }

    @NonNull
    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View v;

        if(viewType == SELF){
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item_user, parent,false);
        }else{
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item_bot, parent,false);
        }

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull ViewHolder holder, int position) {
        Message message = messageList.get(position);
        holder.message.setText(message.getMessage());
        String time = new SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime());
        message.setTime(time);
        holder.currentTime.setText(message.getTime());
    }


    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        if(message.getId() != null && message.getId().equals("1")){
            return SELF;
        }
        return position;
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView message;
        TextView currentTime;
        public ViewHolder(View v) {
            super(v);
            message = v.findViewById(R.id.tv_message);
            currentTime = v.findViewById(R.id.tv_current_time);
        }
    }
}
