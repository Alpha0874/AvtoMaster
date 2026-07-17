package com.avtoforward.automaster.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.avtoforward.automaster.Order;
import com.avtoforward.automaster.PocketBaseClient;
import com.avtoforward.automaster.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ActiveOrdersFragment extends Fragment {

    private ListView listView;
    private SwipeRefreshLayout swipeRefresh;
    private ArrayAdapter<String> adapter;
    private List<String> displayItems = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_active_orders, container, false);
        listView = view.findViewById(R.id.listActiveOrders);
        swipeRefresh = view.findViewById(R.id.swipeRefreshActive);

        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, displayItems);
        listView.setAdapter(adapter);

        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(this::loadOrders);
        }

        loadOrders();
        return view;
    }

    private void loadOrders() {
        if (!PocketBaseClient.isLoggedIn()) {
            Toast.makeText(getContext(), "Не авторизован", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            List<Order> orders = PocketBaseClient.getNewOrders();

            requireActivity().runOnUiThread(() -> {
                displayItems.clear();
                if (orders != null && !orders.isEmpty()) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
                    for (Order order : orders) {
                        String info = "Услуга: " + order.getService()
                                + "\nАдрес: " + order.getAddress()
                                + "\nСтатус: " + order.getStatus()
                                + "\nСоздан: " + sdf.format(new java.util.Date(order.getCreatedAt()));
                        displayItems.add(info);
                    }
                } else {
                    displayItems.add("Нет активных заказов");
                }
                adapter.notifyDataSetChanged();
                if (swipeRefresh != null && swipeRefresh.isRefreshing()) {
                    swipeRefresh.setRefreshing(false);
                }
            });
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadOrders();
    }
}