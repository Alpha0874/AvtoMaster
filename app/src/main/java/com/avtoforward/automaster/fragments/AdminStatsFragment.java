package com.avtoforward.automaster.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.avtoforward.automaster.PocketBaseClient;
import com.avtoforward.automaster.R;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class AdminStatsFragment extends Fragment {

    private TextView textStats;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_stats, container, false);
        textStats = view.findViewById(R.id.textStats);
        loadStats();
        return view;
    }

    private void loadStats() {
        new Thread(() -> {
            try {
                JsonObject stats = PocketBaseClient.getStats();
                JsonObject users = PocketBaseClient.getAllUsers();

                int totalUsers = 0;
                int totalMasters = 0;
                int totalClients = 0;
                if (users != null && users.has("totalItems")) {
                    totalUsers = users.get("totalItems").getAsInt();
                    JsonArray items = users.getAsJsonArray("items");
                    for (int i = 0; i < items.size(); i++) {
                        JsonObject user = items.get(i).getAsJsonObject();
                        String role = user.has("role") && !user.get("role").isJsonNull()
                                ? user.get("role").getAsString() : "";
                        if ("master".equals(role)) totalMasters++;
                        else if ("user".equals(role)) totalClients++;
                    }
                }

                StringBuilder sb = new StringBuilder();
                sb.append("📊 СТАТИСТИКА\n\n");
                sb.append("Всего пользователей: ").append(totalUsers).append("\n");
                sb.append("  - Мастеров: ").append(totalMasters).append("\n");
                sb.append("  - Клиентов: ").append(totalClients).append("\n\n");

                if (stats != null) {
                    int onlineMasters = stats.has("online_masters") ? stats.get("online_masters").getAsInt() : 0;
                    int onlineClients = stats.has("online_clients") ? stats.get("online_clients").getAsInt() : 0;
                    int newOrders = stats.has("new_orders") ? stats.get("new_orders").getAsInt() : 0;
                    int completedOrders = stats.has("completed_orders") ? stats.get("completed_orders").getAsInt() : 0;

                    sb.append("🟢 Онлайн:\n");
                    sb.append("  - Мастеров: ").append(onlineMasters).append("\n");
                    sb.append("  - Клиентов: ").append(onlineClients).append("\n\n");
                    sb.append("📦 Заказы:\n");
                    sb.append("  - Новых: ").append(newOrders).append("\n");
                    sb.append("  - Выполнено: ").append(completedOrders).append("\n");
                } else {
                    sb.append("⚠️ Не удалось загрузить статистику заказов\n");
                }

                String finalText = sb.toString();
                requireActivity().runOnUiThread(() -> {
                    textStats.setText(finalText);
                });

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    textStats.setText("Ошибка загрузки данных\n" + e.getMessage());
                    Toast.makeText(getContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}