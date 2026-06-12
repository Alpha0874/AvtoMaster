package com.avtoforward.automaster;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class TopicAdapter extends BaseAdapter {
    private Context context;
    private List<String> titles;
    private List<Integer> unreadCounts;

    public TopicAdapter(Context context, List<String> titles, List<Integer> unreadCounts) {
        this.context = context;
        this.titles = titles;
        this.unreadCounts = unreadCounts;
    }

    @Override
    public int getCount() { return titles.size(); }

    @Override
    public Object getItem(int position) { return titles.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_forum_topic, parent, false);
        }
        TextView titleView = convertView.findViewById(R.id.textTopicTitle);
        TextView badgeView = convertView.findViewById(R.id.textUnreadBadge);
        String title = titles.get(position);
        int unread = unreadCounts.get(position);
        titleView.setText(title);
        if (unread > 0) {
            badgeView.setText(String.valueOf(unread));
            badgeView.setVisibility(View.VISIBLE);
        } else {
            badgeView.setVisibility(View.GONE);
        }
        return convertView;
    }
}