package com.avtoforward.automaster;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class ForumTopicsFragment extends Fragment {

    private final String subcategoryId;
    private final String subcategoryName;
    private ListView listView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TopicAdapter adapter;
    private List<TopicItem> topics = new ArrayList<>();
    private SharedPreferences lastReadPrefs;

    // Класс для хранения темы и количества непрочитанных
    private static class TopicItem {
        String id;
        String title;
        int unreadCount;

        TopicItem(String id, String title) {
            this.id = id;
            this.title = title;
            this.unreadCount = 0;
        }
    }

    // Адаптер для отображения названия темы и количества непрочитанных
    private class TopicAdapter extends ArrayAdapter<TopicItem> {
        TopicAdapter(Context context, List<TopicItem> items) {
            super(context, android.R.layout.simple_list_item_2, items);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            }
            TextView text1 = convertView.findViewById(android.R.id.text1);
            TextView text2 = convertView.findViewById(android.R.id.text2);
            TopicItem item = getItem(position);
            text1.setText(item.title);
            if (item.unreadCount > 0) {
                text2.setText(String.valueOf(item.unreadCount));
                text2.setVisibility(View.VISIBLE);
            } else {
                text2.setVisibility(View.GONE);
            }
            return convertView;
        }
    }

    public ForumTopicsFragment(String subcategoryId, String subcategoryName) {
        this.subcategoryId = subcategoryId;
        this.subcategoryName = subcategoryName;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_forum_topics, container, false);

        listView = view.findViewById(R.id.listTopics);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        Button buttonCreateTopic = view.findViewById(R.id.buttonCreateTopic);
        TextView title = view.findViewById(R.id.textCategoryTitle);
        title.setText("Темы: " + subcategoryName);

        if (getActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Темы: " + subcategoryName);
        }

        lastReadPrefs = requireContext().getSharedPreferences("forum_last_read", Context.MODE_PRIVATE);

        adapter = new TopicAdapter(requireContext(), topics);
        listView.setAdapter(adapter);

        loadTopics();

        swipeRefreshLayout.setOnRefreshListener(() -> loadTopics());

        buttonCreateTopic.setOnClickListener(v -> showCreateTopicDialog());

        listView.setOnItemClickListener((parent, view1, position, id) -> {
            TopicItem item = adapter.getItem(position);
            if (item != null && getActivity() instanceof ForumActivity) {
                ((ForumActivity) getActivity()).openChat(item.id, item.title);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTopics();
    }

    private void loadTopics() {
        new Thread(() -> {
            JsonObject result = PocketBaseClient.getTopicsBySubcategory(subcategoryId);
            if (result != null && result.has("items")) {
                JsonArray items = result.getAsJsonArray("items");
                List<TopicItem> newTopics = new ArrayList<>();
                for (int i = 0; i < items.size(); i++) {
                    JsonObject obj = items.get(i).getAsJsonObject();
                    String id = obj.get("id").getAsString();
                    String topicTitle = obj.get("title").getAsString();
                    newTopics.add(new TopicItem(id, topicTitle));
                }
                requireActivity().runOnUiThread(() -> {
                    topics.clear();
                    topics.addAll(newTopics);
                    adapter.notifyDataSetChanged();
                    loadUnreadCounts();
                });
            } else {
                requireActivity().runOnUiThread(() -> {
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(getContext(), "Ошибка загрузки тем", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void loadUnreadCounts() {
        String userId = PocketBaseClient.getCurrentUserId();
        if (userId == null) return;
        for (TopicItem topic : topics) {
            new Thread(() -> {
                long lastRead = lastReadPrefs.getLong("topic_" + topic.id, 0);
                int unread = PocketBaseClient.getUnreadCountForTopic(topic.id, lastRead);
                requireActivity().runOnUiThread(() -> {
                    topic.unreadCount = unread;
                    adapter.notifyDataSetChanged();
                });
            }).start();
        }
        requireActivity().runOnUiThread(() -> {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void showCreateTopicDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Новая тема");
        EditText input = new EditText(requireContext());
        input.setHint("Введите название темы");
        builder.setView(input);
        builder.setPositiveButton("Создать", (dialog, which) -> {
            String title = input.getText().toString().trim();
            if (!title.isEmpty()) {
                createNewTopic(title);
            } else {
                Toast.makeText(getContext(), "Введите название", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void createNewTopic(String title) {
        String userId = PocketBaseClient.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(getContext(), "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            boolean success = PocketBaseClient.createTopic(title, subcategoryId, userId);
            requireActivity().runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(getContext(), "Тема создана", Toast.LENGTH_SHORT).show();
                    loadTopics();
                } else {
                    Toast.makeText(getContext(), "Ошибка создания темы", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
}