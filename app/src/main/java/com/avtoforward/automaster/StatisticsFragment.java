package com.avtoforward.automaster;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class StatisticsFragment extends Fragment {

    private TextView textCompletedOrders;
    private TextView textTotalEarnings;
    private TextView textRating;
    private ListView listRecentCompleted;
    private OrderAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        textCompletedOrders = view.findViewById(R.id.textCompletedOrders);
        textTotalEarnings = view.findViewById(R.id.textTotalEarnings);
        textRating = view.findViewById(R.id.textRating);
        listRecentCompleted = view.findViewById(R.id.listRecentCompleted);

        loadStatistics();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStatistics();
    }

    private void loadStatistics() {
        String masterId = PocketBaseClient.getCurrentUserId();
        if (masterId == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        new Thread(() -> {
            List<com.avtoforward.automaster.Order> completedOrders = PocketBaseClient.getCompletedOrders(masterId);
            int completedCount = completedOrders.size();
            int totalEarnings = 0;
            for (com.avtoforward.automaster.Order order : completedOrders) {
                totalEarnings += order.getPrice();
            }

            // Последние 5 заказов
            List<com.avtoforward.automaster.Order> lastFive = new ArrayList<>();
            int start = Math.max(0, completedOrders.size() - 5);
            for (int i = start; i < completedOrders.size(); i++) {
                lastFive.add(completedOrders.get(i));
            }

            final int finalCount = completedCount;
            final int finalTotal = totalEarnings;
            final List<com.avtoforward.automaster.Order> finalLastFive = lastFive;

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    textCompletedOrders.setText(String.valueOf(finalCount));
                    textTotalEarnings.setText(finalTotal + " ₽");
                    textRating.setText("★ 5.0"); // можно оставить так, предупреждение не критично

                    // Обновляем адаптер
                    adapter = new OrderAdapter(getContext(), finalLastFive, false, null);
                    listRecentCompleted.setAdapter(adapter);
                });
            }
        }).start();
    }
}