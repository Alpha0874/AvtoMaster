package com.avtoforward.automaster;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

public class UserOrdersFragment extends Fragment {

    private ListView listView;
    private OrderAdapter adapter;
    private List<com.avtoforward.automaster.Order> orderList = new ArrayList<>();
    private SwipeRefreshLayout swipeRefresh;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_orders, container, false);
        listView = view.findViewById(R.id.listUserOrders);
        swipeRefresh = view.findViewById(R.id.swipeRefreshUserOrders);
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

    private void loadOrders() {
        if (PocketBaseClient.getAuthToken() == null) {
            Toast.makeText(getContext(), "Не авторизован", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = PocketBaseClient.getCurrentUserId();
        if (userId == null) return;

        new Thread(() -> {
            List<com.avtoforward.automaster.Order> orders = PocketBaseClient.getUserOrders(userId);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    orderList.clear();
                    orderList.addAll(orders);
                    if (adapter == null) {
                        // Для клиента кнопки действий не нужны (false)
                        adapter = new OrderAdapter(getContext(), orderList, false, null);
                        listView.setAdapter(adapter);
                    } else {
                        adapter.notifyDataSetChanged();
                    }
                    if (swipeRefresh != null && swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
                });
            }
        }).start();
    }
}