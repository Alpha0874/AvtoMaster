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

public class MyOrdersFragment extends Fragment implements OrderAdapter.OnOrderActionListener {

    private ListView listView;
    private OrderAdapter adapter;
    private List<com.avtoforward.automaster.Order> orderList = new ArrayList<>();
    private SwipeRefreshLayout swipeRefresh;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_orders, container, false);
        listView = view.findViewById(R.id.listMyOrders);
        swipeRefresh = view.findViewById(R.id.swipeRefreshMy);
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
        if (PocketBaseClient.getAuthToken() == null) {
            Toast.makeText(getContext(), "Не авторизован", Toast.LENGTH_SHORT).show();
            return;
        }
        String masterId = PocketBaseClient.getCurrentUserId();
        if (masterId == null) return;

        new Thread(() -> {
            List<com.avtoforward.automaster.Order> orders = PocketBaseClient.getMyOrders(masterId);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    orderList.clear();
                    orderList.addAll(orders);
                    if (adapter == null) {
                        adapter = new OrderAdapter(getContext(), orderList, false, this);
                        listView.setAdapter(adapter);
                    } else {
                        adapter.notifyDataSetChanged();
                    }
                    if (swipeRefresh != null && swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
                });
            }
        }).start();
    }

    @Override
    public void onOrderAction(boolean success) {
        if (success) {
            loadOrders();
        }
    }
}