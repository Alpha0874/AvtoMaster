package com.avtoforward.automaster.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.avtoforward.automaster.Order;
import com.avtoforward.automaster.PocketBaseClient;
import com.avtoforward.automaster.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MyOrdersFragment extends Fragment {

    private static final String TAG = "MyOrders";
    private ListView listView;
    private SwipeRefreshLayout swipeRefresh;
    private OrderAdapter adapter;
    private List<Order> orders = new ArrayList<>();

    private class OrderAdapter extends ArrayAdapter<Order> {
        private final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

        OrderAdapter(@NonNull android.content.Context context, List<Order> orders) {
            super(context, android.R.layout.simple_list_item_2, orders);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
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
                displayStatus = "✅ Мастер принял";
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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_orders, container, false);
        listView = view.findViewById(R.id.listMyOrders);
        swipeRefresh = view.findViewById(R.id.swipeRefreshMyOrders);

        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(this::loadOrders);
        }

        adapter = new OrderAdapter(requireContext(), orders);
        listView.setAdapter(adapter);

        loadOrders();

        listView.setOnItemClickListener((parent, view1, position, id) -> {
            Order order = adapter.getItem(position);
            if (order != null) {
                showOrderDetailsDialog(order);
            } else {
                Toast.makeText(getContext(), "Ошибка: заказ не найден", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void loadOrders() {
        String masterId = PocketBaseClient.getCurrentUserId();
        if (masterId == null) {
            Toast.makeText(getContext(), "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            return;
        }
        new Thread(() -> {
            List<Order> myOrders = PocketBaseClient.getMyOrders(masterId);
            Log.d(TAG, "Загружено заказов: " + (myOrders != null ? myOrders.size() : 0));
            requireActivity().runOnUiThread(() -> {
                orders.clear();
                if (myOrders != null) {
                    orders.addAll(myOrders);
                }
                adapter.notifyDataSetChanged();
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                if (orders.isEmpty()) {
                    Toast.makeText(getContext(), "У вас нет принятых заказов", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void showOrderDetailsDialog(Order order) {
        // Создаём кастомный диалог с кнопками
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_order_details, null);
        builder.setView(dialogView);

        TextView tvService = dialogView.findViewById(R.id.dialogService);
        TextView tvAddress = dialogView.findViewById(R.id.dialogAddress);
        TextView tvDescription = dialogView.findViewById(R.id.dialogDescription);
        TextView tvVehicle = dialogView.findViewById(R.id.dialogVehicle);
        TextView tvCity = dialogView.findViewById(R.id.dialogCity);
        TextView tvClientName = dialogView.findViewById(R.id.dialogClientName);
        TextView tvClientPhone = dialogView.findViewById(R.id.dialogClientPhone);
        TextView tvStatus = dialogView.findViewById(R.id.dialogStatus);
        TextView tvCreated = dialogView.findViewById(R.id.dialogCreated);
        Button btnCall = dialogView.findViewById(R.id.btnCallClient);
        Button btnComplete = dialogView.findViewById(R.id.btnCompleteOrder);
        Button btnClose = dialogView.findViewById(R.id.btnCloseDialog);

        // Заполняем данные
        tvService.setText("Услуга: " + order.getService());
        tvAddress.setText("Адрес: " + order.getAddress());
        tvDescription.setText("Описание: " + order.getDescription());
        tvVehicle.setText("Авто: " + order.getVehicleBrand() + " " + order.getVehicleModel());
        tvCity.setText("Город: " + order.getCity());
        tvClientName.setText("Заказчик: " + order.getClientName());
        tvClientPhone.setText("Телефон: " + order.getClientPhone());

        String status = order.getStatus();
        String displayStatus;
        int color;
        if ("accepted".equals(status)) {
            displayStatus = "✅ Мастер принял";
            color = ContextCompat.getColor(requireContext(), R.color.orange_accent);
        } else if ("completed".equals(status)) {
            displayStatus = "✅ Выполнен";
            color = ContextCompat.getColor(requireContext(), R.color.switch_thumb_on);
        } else if ("cancelled".equals(status)) {
            displayStatus = "❌ Отменён";
            color = ContextCompat.getColor(requireContext(), R.color.switch_thumb_off);
        } else {
            displayStatus = status;
            color = ContextCompat.getColor(requireContext(), R.color.text_secondary);
        }
        tvStatus.setText("Статус: " + displayStatus);
        tvStatus.setTextColor(color);

        tvCreated.setText("Создан: " + new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                .format(new java.util.Date(order.getCreatedAt())));

        // Кнопка звонка
        btnCall.setOnClickListener(v -> {
            String phone = order.getClientPhone();
            if (phone != null && !phone.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + phone));
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "Номер телефона не указан", Toast.LENGTH_SHORT).show();
            }
        });

        // Кнопка "Завершить заказ" (показываем только для accepted)
        if ("accepted".equals(status)) {
            btnComplete.setVisibility(View.VISIBLE);
            btnComplete.setOnClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Завершить заказ?")
                        .setMessage("Вы уверены, что хотите завершить этот заказ?")
                        .setPositiveButton("Да", (dialog, which) -> {
                            new Thread(() -> {
                                boolean success = PocketBaseClient.completeOrder(order.getId());
                                requireActivity().runOnUiThread(() -> {
                                    if (success) {
                                        Toast.makeText(getContext(), "Заказ завершён", Toast.LENGTH_SHORT).show();
                                        loadOrders();
                                        // Закрываем диалог
                                        AlertDialog currentDialog = (AlertDialog) dialogView.getParent();
                                        if (currentDialog != null) currentDialog.dismiss();
                                    } else {
                                        Toast.makeText(getContext(), "Ошибка завершения заказа", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }).start();
                        })
                        .setNegativeButton("Отмена", null)
                        .show();
            });
        } else {
            btnComplete.setVisibility(View.GONE);
        }

        // Кнопка закрытия
        btnClose.setOnClickListener(v -> {
            AlertDialog dialog = (AlertDialog) dialogView.getParent();
            if (dialog != null) dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadOrders();
    }
}