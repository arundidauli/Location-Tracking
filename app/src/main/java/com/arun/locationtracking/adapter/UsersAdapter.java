package com.arun.locationtracking.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.arun.locationtracking.MainActivity;
import com.arun.locationtracking.R;
import com.arun.locationtracking.model.User;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.ViewHolder> {
    private Context context;
    private List<User> userList=new ArrayList<>();
    private MyClickListener myClickListener;

    public UsersAdapter(Context context, MyClickListener myClickListener) {
        this.context = context;
        this.myClickListener = myClickListener;
    }

    @NonNull
    @Override
    public UsersAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_users_row, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull UsersAdapter.ViewHolder holder, int position) {

        Glide.with(context).load(userList.get(position).getPhoto()).placeholder(R.mipmap.gps).into(holder.userProfile);
        holder.userName.setText(userList.get(position).getName());
        holder.userDetails.setText(userList.get(position).getEmail());
        holder.txtDirection.setOnClickListener(v -> {
            myClickListener.getLatLong(userList.get(position).getDeviceId());
        });
        holder.txtTracking.setOnClickListener(v -> {
            myClickListener.clicked(userList.get(position).getDeviceId());

        });

    }

    public void setUsers(List<User> users){
        if (userList!=null){
            userList.clear();
            userList.addAll(users);
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private CircleImageView userProfile;
        private TextView userName,userDetails,txtTracking,txtDirection;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userProfile=itemView.findViewById(R.id.userProfile);
            userName=itemView.findViewById(R.id.userName);
            userDetails=itemView.findViewById(R.id.userDetails);
            txtTracking=itemView.findViewById(R.id.txtTracking);
            txtDirection=itemView.findViewById(R.id.txtDirection);

        }
    }

   public interface MyClickListener{
       void clicked(String id);
       void getLatLong(String id);
    }
}
