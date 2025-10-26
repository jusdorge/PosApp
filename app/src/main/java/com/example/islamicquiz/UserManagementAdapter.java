package com.example.islamicquiz;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.islamicquiz.model.User;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class UserManagementAdapter extends BaseAdapter {
    private Context context;
    private List<User> users;
    private LayoutInflater inflater;
    
    public UserManagementAdapter(Context context, List<User> users) {
        this.context = context;
        this.users = users;
        this.inflater = LayoutInflater.from(context);
    }
    
    @Override
    public int getCount() {
        return users.size();
    }
    
    @Override
    public Object getItem(int position) {
        return users.get(position);
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_user, parent, false);
            holder = new ViewHolder();
            holder.fullNameText = convertView.findViewById(R.id.fullNameText);
            holder.emailText = convertView.findViewById(R.id.emailText);
            holder.roleText = convertView.findViewById(R.id.roleText);
            holder.statusText = convertView.findViewById(R.id.statusText);
            holder.lastLoginText = convertView.findViewById(R.id.lastLoginText);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        User user = users.get(position);
        
        holder.fullNameText.setText(user.getFullName());
        holder.emailText.setText(user.getEmail());
        holder.roleText.setText(user.getRole() != null ? user.getRole().getDisplayName() : context.getString(R.string.not_specified));
        
        // حالة المستخدم
        if (user.isActive()) {
            holder.statusText.setText(context.getString(R.string.user_status_active));
            holder.statusText.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
        } else {
            holder.statusText.setText(context.getString(R.string.user_status_disabled));
            holder.statusText.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
        }
        
        // آخر تسجيل دخول
        if (user.getLastLogin() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            holder.lastLoginText.setText(context.getString(R.string.last_login_label, sdf.format(user.getLastLogin().toDate())));
        } else {
            holder.lastLoginText.setText(context.getString(R.string.never_logged_in));
        }
        
        return convertView;
    }
    
    private static class ViewHolder {
        TextView fullNameText;
        TextView emailText;
        TextView roleText;
        TextView statusText;
        TextView lastLoginText;
    }
} 