package com.avtoforward.automaster.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.avtoforward.automaster.Order;
import com.avtoforward.automaster.PocketBaseClient;
import com.avtoforward.automaster.R;

import java.util.UUID;

public class AdminCreateOrderFragment extends Fragment {

    private Spinner spinnerService, spinnerVehicleType, spinnerPaymentMethod;
    private EditText editAddress, editDescription, editVehicleBrand, editVehicleModel, editVehicleYear;
    private EditText editPrice, editClientName, editClientPhone, editCity, editDistance;
    private Button buttonCreateOrder;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_create_order, container, false);

        spinnerService = view.findViewById(R.id.spinnerService);
        spinnerVehicleType = view.findViewById(R.id.spinnerVehicleType);
        spinnerPaymentMethod = view.findViewById(R.id.spinnerPaymentMethod);
        editAddress = view.findViewById(R.id.editAddress);
        editDescription = view.findViewById(R.id.editDescription);
        editVehicleBrand = view.findViewById(R.id.editVehicleBrand);
        editVehicleModel = view.findViewById(R.id.editVehicleModel);
        editVehicleYear = view.findViewById(R.id.editVehicleYear);
        editPrice = view.findViewById(R.id.editPrice);
        editClientName = view.findViewById(R.id.editClientName);
        editClientPhone = view.findViewById(R.id.editClientPhone);
        editCity = view.findViewById(R.id.editCity);
        editDistance = view.findViewById(R.id.editDistance);
        buttonCreateOrder = view.findViewById(R.id.buttonCreateOrder);

        ArrayAdapter<CharSequence> serviceAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.master_types_array, android.R.layout.simple_spinner_item);
        serviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerService.setAdapter(serviceAdapter);

        ArrayAdapter<CharSequence> vehicleAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.vehicle_types_array, android.R.layout.simple_spinner_item);
        vehicleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVehicleType.setAdapter(vehicleAdapter);

        ArrayAdapter<CharSequence> paymentAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.payment_methods_array, android.R.layout.simple_spinner_item);
        paymentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPaymentMethod.setAdapter(paymentAdapter);

        buttonCreateOrder.setOnClickListener(v -> createOrder());

        return view;
    }

    private void createOrder() {
        String service = spinnerService.getSelectedItem().toString();
        String address = editAddress.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String vehicleType = spinnerVehicleType.getSelectedItem().toString();
        String vehicleBrand = editVehicleBrand.getText().toString().trim();
        String vehicleModel = editVehicleModel.getText().toString().trim();
        String vehicleYear = editVehicleYear.getText().toString().trim();
        String priceStr = editPrice.getText().toString().trim();
        String clientName = editClientName.getText().toString().trim();
        String clientPhone = editClientPhone.getText().toString().trim();
        String city = editCity.getText().toString().trim();
        String paymentMethod = spinnerPaymentMethod.getSelectedItem().toString();
        String distanceStr = editDistance.getText().toString().trim();

        if (address.isEmpty() || clientName.isEmpty() || clientPhone.isEmpty()) {
            Toast.makeText(getContext(), "Заполните адрес, имя и телефон", Toast.LENGTH_SHORT).show();
            return;
        }

        int price = 0;
        if (!priceStr.isEmpty()) {
            try {
                price = Integer.parseInt(priceStr);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Некорректная цена", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        int distance = 0;
        if (!distanceStr.isEmpty()) {
            try {
                distance = Integer.parseInt(distanceStr);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Некорректное расстояние", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String userId = PocketBaseClient.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(getContext(), "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            return;
        }

        Order order = new Order(
                UUID.randomUUID().toString(),
                service,
                address,
                description,
                "",
                vehicleType,
                vehicleBrand,
                vehicleModel,
                vehicleYear,
                price,
                "new",
                userId,
                false,
                null,
                System.currentTimeMillis(),
                clientName,
                clientPhone,
                city,
                distance,
                service,
                paymentMethod,
                false,
                0
        );

        new Thread(() -> {
            boolean success = PocketBaseClient.createOrder(order);
            requireActivity().runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(getContext(), "Заказ создан", Toast.LENGTH_SHORT).show();
                    clearFields();
                } else {
                    Toast.makeText(getContext(), "Ошибка создания заказа", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void clearFields() {
        editAddress.setText("");
        editDescription.setText("");
        editVehicleBrand.setText("");
        editVehicleModel.setText("");
        editVehicleYear.setText("");
        editPrice.setText("");
        editClientName.setText("");
        editClientPhone.setText("");
        editCity.setText("");
        editDistance.setText("");
    }
}