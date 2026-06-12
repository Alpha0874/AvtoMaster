package com.avtoforward.automaster;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.util.ArrayList;
import java.util.List;

public class ActiveOrdersActivity extends AppCompatActivity {
    private ListView listView;
    private OrderAdapter adapter;
    private List<com.avtoforward.automaster.Order> orderList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Активные заказы");
        }
        listView = findViewById(R.id.listOrders);
        loadOrders();
    }

    private void loadOrders() {
        String masterId = PocketBaseClient.getCurrentUserId();
        if (masterId == null) {
            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        new Thread(() -> {
            List<com.avtoforward.automaster.Order> allOrders = PocketBaseClient.getMyOrders(masterId);
            List<com.avtoforward.automaster.Order> activeOrders = new ArrayList<>();
            for (com.avtoforward.automaster.Order order : allOrders) {
                if ("accepted".equals(order.getStatus())) {
                    activeOrders.add(order);
                }
            }
            runOnUiThread(() -> {
                orderList.clear();
                orderList.addAll(activeOrders);
                adapter = new OrderAdapter(this, orderList, false, null);
                listView.setAdapter(adapter);
            });
        }).start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}