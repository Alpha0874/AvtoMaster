package com.avtoforward.automaster.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.avtoforward.automaster.PocketBaseClient;
import com.avtoforward.automaster.R;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminUsersFragment extends Fragment {

    private static final String TAG = "AdminUsers";
    private LinearLayout container;
    private List<JsonObject> userList = new ArrayList<>();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_users, container, false);
        this.container = view.findViewById(R.id.usersContainer);
        loadUsers();
        return view;
    }

    private void loadUsers() {
        new Thread(() -> {
            try {
                JsonObject result = PocketBaseClient.getAllUsers();
                if (result == null || !result.has("items")) {
                    requireActivity().runOnUiThread(() -> {
                        TextView empty = new TextView(getContext());
                        empty.setText("Нет пользователей");
                        empty.setTextColor(getResources().getColor(android.R.color.white));
                        container.addView(empty);
                    });
                    return;
                }
                JsonArray items = result.getAsJsonArray("items");
                Log.d(TAG, "Получено пользователей: " + items.size());
                userList.clear();
                for (int i = 0; i < items.size(); i++) {
                    JsonObject user = items.get(i).getAsJsonObject();
                    userList.add(user);
                }
                requireActivity().runOnUiThread(() -> displayUsers());
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void displayUsers() {
        container.removeAllViews();
        if (userList.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("Нет пользователей");
            empty.setTextColor(getResources().getColor(android.R.color.white));
            container.addView(empty);
            return;
        }
        // Для каждого пользователя создаём карточку и загружаем данные асинхронно
        for (JsonObject user : userList) {
            View card = getLayoutInflater().inflate(R.layout.item_user, container, false);
            container.addView(card);
            // Загружаем данные в фоне
            new Thread(() -> populateUserCard(card, user)).start();
        }
    }

    private void populateUserCard(View card, JsonObject user) {
        try {
            TextView nameView = card.findViewById(R.id.userName);
            TextView emailView = card.findViewById(R.id.userEmail);
            TextView roleView = card.findViewById(R.id.userRole);
            TextView statusView = card.findViewById(R.id.userStatus);
            TextView cityView = card.findViewById(R.id.userCity);
            TextView registeredView = card.findViewById(R.id.userRegistered);
            Button btnVerify = card.findViewById(R.id.btnVerify);
            Button btnBan = card.findViewById(R.id.btnBan);
            Button btnUnban = card.findViewById(R.id.btnUnban);

            LinearLayout masterInfoLayout = card.findViewById(R.id.masterInfoLayout);
            TextView masterNumberView = card.findViewById(R.id.masterNumber);
            TextView masterTimeView = card.findViewById(R.id.masterTime);
            TextView completedOrdersView = card.findViewById(R.id.completedOrders);
            TextView failedOrdersView = card.findViewById(R.id.failedOrders);

            String userId = safeGetString(user, "id");
            String fullName = safeGetString(user, "full_name");
            String nickname = safeGetString(user, "nickname");
            String userEmail = safeGetString(user, "email");
            String userRole = safeGetString(user, "role");
            String city = safeGetString(user, "city");
            String created = safeGetString(user, "created");
            boolean banned = user.has("banned") && user.get("banned").getAsBoolean();
            boolean verified = user.has("verified") && user.get("verified").getAsBoolean();

            // Отображаем имя
            String displayName = fullName.isEmpty() ? (nickname.isEmpty() ? "Без имени" : nickname) : fullName;
            mainHandler.post(() -> nameView.setText(displayName));

            // Email
            mainHandler.post(() -> emailView.setText(userEmail.isEmpty() ? "Нет email" : userEmail));

            // Роль
            mainHandler.post(() -> roleView.setText("Роль: " + userRole));

            // Статус
            String statusText = banned ? "🔴 Заблокирован" : (verified ? "🟢 Активен" : "🟡 Ожидает подтверждения");
            int color = banned ? android.R.color.holo_red_dark :
                    verified ? android.R.color.holo_green_dark : android.R.color.holo_orange_dark;
            mainHandler.post(() -> {
                statusView.setText(statusText);
                statusView.setTextColor(getResources().getColor(color));
            });

            // Город
            mainHandler.post(() -> cityView.setText(city.isEmpty() ? "Город не указан" : "🏙️ " + city));

            // Дата регистрации
            String regDate = "Дата неизвестна";
            if (!created.isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'Z'", Locale.US);
                    Date date = sdf.parse(created);
                    SimpleDateFormat displayFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                    regDate = displayFormat.format(date);
                } catch (Exception e) {
                    regDate = created;
                }
            }
            String finalRegDate = regDate;
            mainHandler.post(() -> registeredView.setText("📅 " + finalRegDate));

            // Кнопки
            mainHandler.post(() -> {
                btnVerify.setVisibility((!banned && !verified) ? View.VISIBLE : View.GONE);
                btnBan.setVisibility((!banned && verified) ? View.VISIBLE : View.GONE);
                btnUnban.setVisibility(banned ? View.VISIBLE : View.GONE);
            });

            // Если роль = master, загружаем дополнительную статистику
            if ("master".equals(userRole)) {
                mainHandler.post(() -> masterInfoLayout.setVisibility(View.VISIBLE));

                // Номер мастера
                int masterNumber = PocketBaseClient.getMasterNumber(userId);
                mainHandler.post(() -> masterNumberView.setText("№ мастера: " + masterNumber));

                // Общее время в приложении
                long totalSeconds = PocketBaseClient.getTotalAppTime(userId);
                String timeStr = formatTime(totalSeconds);
                mainHandler.post(() -> masterTimeView.setText("⏱️ " + timeStr));

                // Количество выполненных заказов (completed)
                int completed = PocketBaseClient.getOrderCountByStatus(userId, "completed");
                mainHandler.post(() -> completedOrdersView.setText("✅ Выполнено: " + completed));

                // Количество проваленных заказов (rejected или canceled)
                int rejected = PocketBaseClient.getOrderCountByStatus(userId, "rejected");
                int canceled = PocketBaseClient.getOrderCountByStatus(userId, "canceled");
                int failed = rejected + canceled;
                mainHandler.post(() -> failedOrdersView.setText("❌ Провалено: " + failed));
            } else {
                mainHandler.post(() -> masterInfoLayout.setVisibility(View.GONE));
            }

            // Обработчики кнопок
            btnVerify.setOnClickListener(v -> updateStatus(userId, "verify"));
            btnBan.setOnClickListener(v -> updateStatus(userId, "ban"));
            btnUnban.setOnClickListener(v -> updateStatus(userId, "unban"));

        } catch (Exception e) {
            Log.e(TAG, "Error populating card", e);
        }
    }

    private String formatTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        if (hours > 0) {
            return hours + "ч " + minutes + "м";
        } else {
            return minutes + "м";
        }
    }

    private String safeGetString(JsonObject obj, String key) {
        if (obj == null) return "";
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            try {
                return obj.get(key).getAsString();
            } catch (Exception e) {
                return "";
            }
        }
        return "";
    }

    private void updateStatus(String userId, String action) {
        new Thread(() -> {
            try {
                boolean success = false;
                switch (action) {
                    case "verify": success = PocketBaseClient.verifyUser(userId); break;
                    case "ban": success = PocketBaseClient.banUser(userId); break;
                    case "unban": success = PocketBaseClient.unbanUser(userId); break;
                }
                boolean finalSuccess = success;
                requireActivity().runOnUiThread(() -> {
                    if (finalSuccess) {
                        String msg = action.equals("verify") ? "Аккаунт подтверждён" :
                                action.equals("ban") ? "Пользователь заблокирован" : "Пользователь разблокирован";
                        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                        loadUsers(); // обновить список
                    } else {
                        Toast.makeText(getContext(), "Ошибка обновления статуса", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}