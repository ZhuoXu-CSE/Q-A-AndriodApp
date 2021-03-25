package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class ChatAdapter extends BaseAdapter {
    private Context context;
    private List<ChatEntity> chatEntityList;

    public ChatAdapter(List<ChatEntity> chatEntityList, Context context) {
        this.chatEntityList = chatEntityList;
        this.context = context;
    }

    @Override
    public int getCount() {
        return chatEntityList.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        ChatEntity entity = chatEntityList.get(position);
        return entity.getRole();
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if(convertView == null){

            //left chat or right chat
            if(chatEntityList.get(position).getRole()==0){
                convertView = LayoutInflater.from(context).inflate(R.layout.chat_left,parent,false);
            }
            else{
                convertView = LayoutInflater.from(context).inflate(R.layout.chat_right,parent,false);
            }

            holder = new ViewHolder();
            holder.txt = (TextView) convertView.findViewById(R.id.text);
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder) convertView.getTag();
        }
        holder.txt.setText(chatEntityList.get(position).getMessage());
        return convertView;
    }

    private class ViewHolder{
        TextView txt;
    }

    //main call add to add ChatEntity and update
    public void add(ChatEntity sent) {
        chatEntityList.add(sent);
        notifyDataSetChanged();
    }

}