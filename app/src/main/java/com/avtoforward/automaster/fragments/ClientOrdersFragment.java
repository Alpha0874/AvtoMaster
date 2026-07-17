package com.avtoforward.automaster.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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

public class ClientOrdersFragment extends Fragment {

    private ListView listView;
    private SwipeRefreshLayout swipeRefreshLayout;
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
            String service = order.getService();
            String status = order.getStatus();
            String date = sdf.format(new java.util.Date(order.getCreatedAt()));

            // Преобразуем статус для клиента
            String displayStatus;
            int color;
            if ("new".equals(status)) {
                displayStatus = "Поиск мастера";
                color = ContextCompat.getColor(getContext(), R.color.switch_thumb_on); // зеленый
            } else if ("accepted".equals(status)) {
                displayStatus = "Мастер принял заказ";
                color = ContextCompat.getColor(getContext(), R.color.orange_accent);
            } else if ("completed".equals(status)) {
                displayStatus = "Выполнено";
                color = ContextCompat.getColor(getContext(), R.color.text_secondary);
            } else if ("cancelled".equals(status)) {
                displayStatus = "Отменён";
                color = ContextCompat.getColor(getContext(), R.color.switch_thumb_off); // красный
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
        View view = inflater.inflate(R.layout.fragment_client_orders, container, false);
        listView = view.findViewById(R.id.listClientOrders);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshOrders);
        adapter = new OrderAdapter(requireContext(), orders);
        listView.setAdapter(adapter);

        loadOrders();

        swipeRefreshLayout.setOnRefreshListener(this::loadOrders);

        listView.setOnItemClickListener((parent, view1, position, id) -> {
            Order order = adapter.getItem(position);
            if (order != null) {
                showOrderDetails(order);
            }
        });

        return view;
    }

    private void loadOrders() {
        String userId = PocketBaseClient.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(getContext(), "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            return;
        }
        new Thread(() -> {
            List<Order> userOrders = PocketBaseClient.getUserOrders(userId);
            requireActivity().runOnUiThread(() -> {
                orders.clear();
                if (userOrders != null) {
                    orders.addAll(userOrders);
                }
                adapter.notifyDataSetChanged();
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                if (orders.isEmpty()) {
                    Toast.makeText(getContext(), "У вас пока нет заказов", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void showOrderDetails(Order order) {
        String status = order.getStatus();
        String displayStatus;
        int color;
        if ("new".equals(status)) {
            displayStatus = "Поиск мастера (ожидайте ответа)";
            color = ContextCompat.getColor(getContext(), R.color.switch_thumb_on);
        } else if ("accepted".equals(status)) {
            displayStatus = "Мастер принял заказ";
            color = ContextCompat.getColor(getContext(), R.color.orange_accent);
        } else if ("completed".equals(status)) {
            displayStatus = "Выполнено";
            color = ContextCompat.getColor(getContext(), R.color.text_secondary);
        } else if ("cancelled".equals(status)) {
            displayStatus = "Отменён";
            color = ContextCompat.getColor(getContext(), R.color.switch_thumb_off);
        } else {
            displayStatus = status;
            color = ContextCompat.getColor(getContext(), R.color.text_secondary);
        }

        String message = "Услуга: " + order.getService() + "\n"
                + "Адрес: " + order.getAddress() + "\n"
                + "Описание: " + order.getDescription() + "\n"
                + "Авто: " + order.getVehicleBrand() + " " + order.getVehicleModel() + "\n"
                + "Статус: " + displayStatus + "\n"
                + "Создан: " + new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new java.util.Date(order.getCreatedAt()));

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Детали заказа")
                .setMessage(message)
                .setPositiveButton("Закрыть", null);

        // Отмена доступна только для "new"
        if ("new".equals(status)) {
            builder.setNegativeButton("Отменить заказ", (dialog, which) -> {
                cancelOrder(order.getId());
            });
        }

        AlertDialog dialog = builder.create();
        dialog.show();

        // Меняем цвет статуса в диалоге (необязательно, но можно попробовать)
        // Но проще просто оставить как есть, так как диалог показывает текст с цветом? Нет, в диалоге текст не раскрашен.
        // Мы просто выводим текст, цвет не важен.
    }

    private void cancelOrder(String orderId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Отменить заказ?")
                .setMessage("Вы уверены, что хотите отменить заказ?")
                .setPositiveButton("Да", (dialog, which) -> {
                    new Thread(() -> {
                        boolean success = PocketBaseClient.updateOrderStatus(orderId, "cancelled", null);
                        requireActivity().runOnUiThread(() -> {
                            if (success) {
                                Toast.makeText(getContext(), "Заказ отменён", Toast.LENGTH_SHORT).show();
                                loadOrders();
                            } else {
                                Toast.makeText(getContext(), "Ошибка отмены", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }).start();
                })
                .setNegativeButton("Нет", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadOrders();
    }
}