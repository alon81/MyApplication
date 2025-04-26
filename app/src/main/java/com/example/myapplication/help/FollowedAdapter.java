package com.example.myapplication.help;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;

import java.util.List;
public class FollowedAdapter extends RecyclerView.Adapter<FollowedAdapter.ViewHolder> {

    private Context context;
    private List<String> sources;
    private FirestoreHelper firestoreHelper;

    public FollowedAdapter(Context context, FirestoreHelper firestoreHelper) {
        this.context = context;
        this.firestoreHelper = firestoreHelper;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_followed_source, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String source = sources.get(position);
        holder.sourceName.setText(source);
        holder.unfollowButton.setOnClickListener(v -> unfollowSource(source));
    }

    @Override
    public int getItemCount() {
        return sources != null ? sources.size() : 0;
    }

    public void setSources(List<String> sources) {
        this.sources = sources;
        notifyDataSetChanged();
    }

    public void unfollowSource(String source) {
        firestoreHelper.removeFollowedSource(source);
        sources.remove(source);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView sourceName;
        Button unfollowButton;

        public ViewHolder(View itemView) {
            super(itemView);
            sourceName = itemView.findViewById(R.id.source_name);
            unfollowButton = itemView.findViewById(R.id.unfollow_button);
        }
    }
}
