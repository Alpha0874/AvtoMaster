package com.avtoforward.automaster;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

public class ActiveOrdersFragment extends Fragment implements OrderAdapter.OnOrderActionListener {

    private ListView listView;
    private OrderAdapter adapter;
    private List<com.avtoforward.automaster.Order> orders = new ArrayList<>();
    private SwipeRefreshLayout swipeRefresh;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_active_orders, container, false);
        listView = view.findViewById(R.id.listActiveOrders);
        swipeRefresh = view.findViewById(R.id.swipeRefreshActive);
        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(this::loadOrders);
        }
        loadOrders();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadOrders();
    }

    public void loadOrders() {
        new Thread(() -> {
            if (!PocketBaseClient.isLoggedIn()) {
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Не авторизован", Toast.LENGTH_SHORT).show());
                return;
            }
            String userId = PocketBaseClient.getCurrentUserId();
            String verificationStatus = PocketBaseClient.getVerificationStatus(userId);
            if (!"verified".equals(verificationStatus)) {
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Доступ только после верификации", Toast.LENGTH_LONG).show());
                return;
            }
            List<com.avtoforward.automaster.Order> newOrders = PocketBaseClient.getNewOrders();
            requireActivity().runOnUiThread(() -> {
                orders.clear();
                orders.addAll(newOrders);
                if (adapter == null) {
                    adapter = new OrderAdapter(requireContext(), orders, true, this);
                    listView.setAdapter(adapter);
                } else {
                    adapter.notifyDataSetChanged();
                }
                if (swipeRefresh != null && swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
            });
        }).start();
    }

    @Override
    public void onOrderAction(boolean success) {
        if (success) {
            loadOrders();
        }
    }
}