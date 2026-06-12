package com.avtoforward.automaster;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.util.ArrayList;
import java.util.List;

public class CompletedOrdersActivity extends AppCompatActivity {
    private ListView listView;
    private OrderAdapter adapter;
    private List<com.avtoforward.automaster.Order> orderList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completed_orders);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("История заказов");
        }
        listView = findViewById(R.id.listCompletedOrders);
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
            List<com.avtoforward.automaster.Order> completedOrders = PocketBaseClient.getCompletedOrders(masterId);
            runOnUiThread(() -> {
                orderList.clear();
                orderList.addAll(completedOrders);
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