package com.avtoforward.automaster;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends BaseAdapter {
    private final Context context;
    private final List<Order> orders;
    private final boolean isActiveOrders;
    private final OnOrderActionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

    public interface OnOrderActionListener {
        void onOrderAction(boolean success);
    }

    public OrderAdapter(Context context, List<Order> orders, boolean isActiveOrders, OnOrderActionListener listener) {
        this.context = context;
        this.orders = orders;
        this.isActiveOrders = isActiveOrders;
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return orders.size();
    }

    @Override
    public Object getItem(int position) {
        return orders.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
        }

        Order order = orders.get(position);

        TextView textService = convertView.findViewById(R.id.textOrderService);
        TextView textVehicle = convertView.findViewById(R.id.textOrderVehicle);
        TextView textDescription = convertView.findViewById(R.id.textOrderDescription);
        TextView textAddress = convertView.findViewById(R.id.textOrderAddress);
        TextView textPrice = convertView.findViewById(R.id.textOrderPrice);
        TextView textDate = convertView.findViewById(R.id.textOrderDate);
        Button buttonAction = convertView.findViewById(R.id.buttonOrderAction);

        textService.setText("Услуга: " + order.getService());
        textVehicle.setText(order.getVehicleBrand() + " " + order.getVehicleModel() + " (" + order.getVehicleYear() + ")");
        textDescription.setText("Проблема: " + order.getDescription());
        textAddress.setText("Адрес: " + order.getAddress());
        textPrice.setText(order.getPrice() + " ₽");
        if (order.getCreatedAt() > 0) {
            textDate.setText(dateFormat.format(new Date(order.getCreatedAt())));
            textDate.setVisibility(View.VISIBLE);
        } else {
            textDate.setVisibility(View.GONE);
        }

        if (isActiveOrders) {
            buttonAction.setText("Взять в работу");
            buttonAction.setVisibility(View.VISIBLE);
            buttonAction.setOnClickListener(v -> {
                buttonAction.setEnabled(false);
                new Thread(() -> {
                    boolean success = PocketBaseClient.acceptOrder(order.getId(), PocketBaseClient.getCurrentUserId());
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        buttonAction.setEnabled(true);
                        if (success) {
                            Toast.makeText(context, "Заказ взят", Toast.LENGTH_SHORT).show();
                            if (listener != null) listener.onOrderAction(true);
                        } else {
                            Toast.makeText(context, "Ошибка", Toast.LENGTH_SHORT).show();
                        }
                    });
                }).start();
            });
        } else {
            if ("accepted".equals(order.getStatus())) {
                buttonAction.setText("Завершить");
                buttonAction.setVisibility(View.VISIBLE);
                buttonAction.setOnClickListener(v -> {
                    buttonAction.setEnabled(false);
                    new Thread(() -> {
                        boolean success = PocketBaseClient.completeOrder(order.getId());
                        ((android.app.Activity) context).runOnUiThread(() -> {
                            buttonAction.setEnabled(true);
                            if (success) {
                                Toast.makeText(context, "Заказ завершён", Toast.LENGTH_SHORT).show();
                                if (listener != null) listener.onOrderAction(true);
                            } else {
                                Toast.makeText(context, "Ошибка", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }).start();
                });
            } else {
                buttonAction.setVisibility(View.GONE);
            }
        }

        return convertView;
    }
}