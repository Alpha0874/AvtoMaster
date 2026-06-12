package com.avtoforward.automaster;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class AdminOrdersFragment extends Fragment {

    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> orderTitles = new ArrayList<>();
    private List<String> orderIds = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_orders, container, false);
        listView = view.findViewById(R.id.listAllOrders);
        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, orderTitles);
        listView.setAdapter(adapter);
        loadOrders();
        return view;
    }

    private void loadOrders() {
        new Thread(() -> {
            JsonObject result = PocketBaseClient.getAllOrders();
            if (result != null && result.has("items")) {
                JsonArray items = result.getAsJsonArray("items");
                List<String> titles = new ArrayList<>();
                List<String> ids = new ArrayList<>();
                for (int i = 0; i < items.size(); i++) {
                    JsonObject item = items.get(i).getAsJsonObject();
                    ids.add(item.get("id").getAsString());
                    String service = item.has("service") ? item.get("service").getAsString() : "";
                    String address = item.has("address") ? item.get("address").getAsString() : "";
                    titles.add(service + " / " + address);
                }
                requireActivity().runOnUiThread(() -> {
                    orderTitles.clear();
                    orderIds.clear();
                    orderTitles.addAll(titles);
                    orderIds.addAll(ids);
                    adapter.notifyDataSetChanged();
                });
            }
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadOrders();
    }
}