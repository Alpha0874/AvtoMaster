package com.avtoforward.automaster.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.avtoforward.automaster.ActiveOrdersActivity;
import com.avtoforward.automaster.AdminActivity;
import com.avtoforward.automaster.EditMasterProfileActivity;
import com.avtoforward.automaster.ForumActivity;
import com.avtoforward.automaster.MyOrdersActivity;
import com.avtoforward.automaster.PocketBaseClient;
import com.avtoforward.automaster.R;
import com.avtoforward.automaster.StatisticsActivity;
import com.google.gson.JsonObject;

import java.util.List;

public class MenuFragment extends Fragment {

    private TextView textWelcome, textNewOrdersCount, textCompletedOrders;
    private SwitchCompat switchAcceptingOrders;
    private Button btnNewOrders, btnMyOrders, btnProfile, btnForum, btnStatistics;
    private Button btnAdmin;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_master, container, false);

        textWelcome = view.findViewById(R.id.textWelcome);
        textNewOrdersCount = view.findViewById(R.id.textNewOrdersCount);
        textCompletedOrders = view.findViewById(R.id.textCompletedOrders);
        switchAcceptingOrders = view.findViewById(R.id.switchAcceptingOrders);
        btnNewOrders = view.findViewById(R.id.btnNewOrders);
        btnMyOrders = view.findViewById(R.id.btnMyOrders);
        btnProfile = view.findViewById(R.id.btnProfile);
        btnForum = view.findViewById(R.id.btnForum);
        btnStatistics = view.findViewById(R.id.btnStatistics);
        btnAdmin = view.findViewById(R.id.btnAdmin);

        // Загружаем данные (включая проверку роли)
        loadMasterData();

        // Переключатель приёма заказов
        switchAcceptingOrders.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String userId = PocketBaseClient.getCurrentUserId();
            if (userId != null) {
                new Thread(() -> {
                    boolean success = PocketBaseClient.setAcceptingOrders(userId, isChecked);
                    if (!success) {
                        requireActivity().runOnUiThread(() -> {
                            switchAcceptingOrders.setChecked(!isChecked);
                        });
                    }
                }).start();
            }
        });

        // Обработчики кнопок
        btnNewOrders.setOnClickListener(v -> startActivity(new Intent(getActivity(), ActiveOrdersActivity.class)));
        btnMyOrders.setOnClickListener(v -> startActivity(new Intent(getActivity(), MyOrdersActivity.class)));
        btnProfile.setOnClickListener(v -> startActivity(new Intent(getActivity(), EditMasterProfileActivity.class)));
        btnForum.setOnClickListener(v -> startActivity(new Intent(getActivity(), ForumActivity.class)));
        btnStatistics.setOnClickListener(v -> startActivity(new Intent(getActivity(), StatisticsActivity.class)));

        // Кнопка администрирования (показывается только для админа)
        btnAdmin.setOnClickListener(v -> startActivity(new Intent(getActivity(), AdminActivity.class)));

        return view;
    }

    private void loadMasterData() {
        new Thread(() -> {
            String userId = PocketBaseClient.getCurrentUserId();
            if (userId == null) {
                requireActivity().runOnUiThread(() -> {
                    btnAdmin.setVisibility(View.GONE);
                });
                return;
            }

            JsonObject user = PocketBaseClient.getUserInfo(userId);
            if (user == null) {
                requireActivity().runOnUiThread(() -> {
                    btnAdmin.setVisibility(View.GONE);
                });
                return;
            }

            // Получаем роль
            String role = user.has("role") && !user.get("role").isJsonNull()
                    ? user.get("role").getAsString()
                    : "user";

            String fullName = user.has("full_name") && !user.get("full_name").isJsonNull()
                    ? user.get("full_name").getAsString()
                    : "Мастер";
            String nickname = user.has("nickname") && !user.get("nickname").isJsonNull()
                    ? user.get("nickname").getAsString()
                    : "";

            boolean accepting = "yes".equals(user.get("accepting_orders").getAsString());

            List<com.avtoforward.automaster.Order> newOrders = PocketBaseClient.getNewOrders();
            int newOrdersCount = newOrders.size();

            List<com.avtoforward.automaster.Order> completedOrders = PocketBaseClient.getCompletedOrders(userId);
            int completedCount = completedOrders.size();

            boolean isAdmin = "admin".equals(role);

            requireActivity().runOnUiThread(() -> {
                String displayName = fullName.isEmpty() ? nickname : fullName;
                textWelcome.setText("Здравствуйте, " + displayName + "!");
                textNewOrdersCount.setText("Новых заказов: " + newOrdersCount);
                textCompletedOrders.setText("✅ Выполнено заказов: " + completedCount);
                switchAcceptingOrders.setChecked(accepting);

                // Показываем кнопку администрирования только если роль admin
                if (isAdmin) {
                    btnAdmin.setVisibility(View.VISIBLE);
                } else {
                    btnAdmin.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMasterData(); // Обновляем данные при возврате на фрагмент
    }
}