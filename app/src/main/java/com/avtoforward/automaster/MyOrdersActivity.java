package com.avtoforward.automaster;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.avtoforward.automaster.Order;
import com.avtoforward.automaster.PocketBaseClient;
import com.avtoforward.automaster.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MyOrdersActivity extends AppCompatActivity {

    private ListView listView;
    private OrderAdapter adapter;
    private List<Order> orderList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Мои заказы");
        }

        listView = findViewById(R.id.listOrders);
        loadOrders();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Order order = adapter.getItem(position);
            if (order != null) {
                showOrderDetailsDialog(order);
            }
        });
    }

    private void loadOrders() {
        String masterId = PocketBaseClient.getCurrentUserId();
        if (masterId == null) {
            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        new Thread(() -> {
            List<Order> orders = PocketBaseClient.getMyOrders(masterId);
            runOnUiThread(() -> {
                orderList.clear();
                if (orders != null) {
                    orderList.addAll(orders);
                }
                if (adapter == null) {
                    adapter = new OrderAdapter(this, orderList);
                    listView.setAdapter(adapter);
                } else {
                    adapter.notifyDataSetChanged();
                }
                if (orderList.isEmpty()) {
                    Toast.makeText(this, "У вас нет принятых заказов", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private class OrderAdapter extends ArrayAdapter<Order> {
        private final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

        OrderAdapter(MyOrdersActivity context, List<Order> orders) {
            super(context, android.R.layout.simple_list_item_2, orders);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            }
            TextView text1 = convertView.findViewById(android.R.id.text1);
            TextView text2 = convertView.findViewById(android.R.id.text2);

            Order order = getItem(position);
            if (order == null) return convertView;

            String service = order.getService();
            String status = order.getStatus();
            String date = sdf.format(new java.util.Date(order.getCreatedAt()));

            String displayStatus;
            int color;
            if ("accepted".equals(status)) {
                displayStatus = "🛠️ В РАБОТЕ";
                color = ContextCompat.getColor(getContext(), R.color.orange_accent);
            } else if ("completed".equals(status)) {
                displayStatus = "✅ Выполнен";
                color = ContextCompat.getColor(getContext(), R.color.switch_thumb_on);
            } else if ("cancelled".equals(status)) {
                displayStatus = "❌ Отменён";
                color = ContextCompat.getColor(getContext(), R.color.switch_thumb_off);
            } else {
                displayStatus = status;
                color = ContextCompat.getColor(getContext(), R.color.text_secondary);
            }

            text1.setText(service);
            text2.setText(displayStatus + " | " + date);
            text2.setTextColor(color);
            return convertView;
        }
    }

    private void showOrderDetailsDialog(Order order) {
        String status = order.getStatus();
        String displayStatus;
        if ("accepted".equals(status)) {
            displayStatus = "🛠️ В РАБОТЕ";
        } else if ("completed".equals(status)) {
            displayStatus = "✅ Выполнен";
        } else if ("cancelled".equals(status)) {
            displayStatus = "❌ Отменён";
        } else {
            displayStatus = status;
        }

        String message = "Услуга: " + order.getService() + "\n"
                + "Адрес: " + order.getAddress() + "\n"
                + "Описание: " + order.getDescription() + "\n"
                + "Авто: " + order.getVehicleBrand() + " " + order.getVehicleModel() + "\n"
                + "Город: " + order.getCity() + "\n"
                + "Заказчик: " + order.getClientName() + "\n"
                + "Телефон заказчика: " + order.getClientPhone() + "\n"
                + "Статус: " + displayStatus + "\n"
                + "Создан: " + new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new java.util.Date(order.getCreatedAt()));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Детали заказа")
                .setMessage(message)
                .setPositiveButton("Закрыть", null);

        if ("accepted".equals(status)) {
            builder.setNegativeButton("✅ Завершить", (dialog, which) -> {
                confirmCompleteOrder(order.getId());
            });
        }

        builder.setNeutralButton("📞 Позвонить", (dialog, which) -> {
            String phone = order.getClientPhone();
            if (phone != null && !phone.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + phone));
                startActivity(intent);
            } else {
                Toast.makeText(this, "Номер телефона не указан", Toast.LENGTH_SHORT).show();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void confirmCompleteOrder(String orderId) {
        new AlertDialog.Builder(this)
                .setTitle("Завершить заказ?")
                .setMessage("Подтвердите, что работа выполнена.")
                .setPositiveButton("Да", (dialog, which) -> {
                    new Thread(() -> {
                        boolean success = PocketBaseClient.completeOrder(orderId);
                        runOnUiThread(() -> {
                            if (success) {
                                Toast.makeText(this, "Заказ завершён", Toast.LENGTH_SHORT).show();
                                loadOrders();
                            } else {
                                Toast.makeText(this, "Ошибка завершения заказа", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }).start();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
}