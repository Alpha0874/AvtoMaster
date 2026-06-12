package com.avtoforward.automaster;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AdminCreateOrderFragment extends Fragment {

    // Поля ввода
    private EditText editClientName, editClientPhone, editAddress, editVehicleBrand, editVehicleModel, editVehicleYear;
    private EditText editProblemDescription, editComment;
    private Spinner spinnerCity, spinnerVehicleType, spinnerMasterType, spinnerPaymentMethod;
    private LinearLayout layoutMkadDistance;
    private EditText editDistanceMkad;
    private CheckBox checkPriceByAgreement;
    private TextView textCalculatedPrice;
    private Button buttonCreateOrder;

    // Данные для расчёта
    private Map<String, Map<String, Integer>> priceTable = new HashMap<>();
    private int currentPrice = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_create_order, container, false);

        // Инициализация виджетов
        editClientName = view.findViewById(R.id.editClientName);
        editClientPhone = view.findViewById(R.id.editClientPhone);
        editAddress = view.findViewById(R.id.editAddress);
        editVehicleBrand = view.findViewById(R.id.editVehicleBrand);
        editVehicleModel = view.findViewById(R.id.editVehicleModel);
        editVehicleYear = view.findViewById(R.id.editVehicleYear);
        editProblemDescription = view.findViewById(R.id.editProblemDescription);
        editComment = view.findViewById(R.id.editComment);
        spinnerCity = view.findViewById(R.id.spinnerCity);
        spinnerVehicleType = view.findViewById(R.id.spinnerVehicleType);
        spinnerMasterType = view.findViewById(R.id.spinnerMasterType);
        spinnerPaymentMethod = view.findViewById(R.id.spinnerPaymentMethod);
        layoutMkadDistance = view.findViewById(R.id.layoutMkadDistance);
        editDistanceMkad = view.findViewById(R.id.editDistanceMkad);
        checkPriceByAgreement = view.findViewById(R.id.checkPriceByAgreement);
        textCalculatedPrice = view.findViewById(R.id.textCalculatedPrice);
        buttonCreateOrder = view.findViewById(R.id.buttonCreateOrder);

        // Заполнение спиннеров
        ArrayAdapter<CharSequence> cityAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.cities_array, android.R.layout.simple_spinner_item);
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCity.setAdapter(cityAdapter);

        ArrayAdapter<CharSequence> vehicleTypeAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.vehicle_types_array, android.R.layout.simple_spinner_item);
        vehicleTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVehicleType.setAdapter(vehicleTypeAdapter);

        ArrayAdapter<CharSequence> masterTypeAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.master_types_array, android.R.layout.simple_spinner_item);
        masterTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMasterType.setAdapter(masterTypeAdapter);

        ArrayAdapter<CharSequence> paymentAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.payment_methods_array, android.R.layout.simple_spinner_item);
        paymentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPaymentMethod.setAdapter(paymentAdapter);

        // Настройка таблицы цен (город -> тип ТС -> цена)
        initPriceTable();

        // Слушатели
        spinnerCity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateMkadVisibility();
                calculatePrice();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerVehicleType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                calculatePrice();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        editDistanceMkad.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) calculatePrice();
        });

        checkPriceByAgreement.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                textCalculatedPrice.setText("Цена: по договорённости");
                buttonCreateOrder.setEnabled(true);
            } else {
                calculatePrice();
            }
        });

        buttonCreateOrder.setOnClickListener(v -> createOrder());

        // Изначально скрыть поле км за МКАД, если город не Москва
        updateMkadVisibility();
        calculatePrice();

        return view;
    }

    private void initPriceTable() {
        // Цены выезда + диагностики (руб)
        Map<String, Integer> moscowPrices = new HashMap<>();
        moscowPrices.put("Легковой", 6000);
        moscowPrices.put("Лёгкий коммерческий", 8000);
        moscowPrices.put("Грузовой от 5т", 15000);
        moscowPrices.put("Спецтехника", 17000);
        priceTable.put("Москва", moscowPrices);

        Map<String, Integer> krasnodarPrices = new HashMap<>();
        krasnodarPrices.put("Легковой", 4000);
        krasnodarPrices.put("Лёгкий коммерческий", 5000);
        krasnodarPrices.put("Грузовой от 5т", 8000);
        krasnodarPrices.put("Спецтехника", 10000);
        priceTable.put("Краснодар", krasnodarPrices);
    }

    private void updateMkadVisibility() {
        String selectedCity = spinnerCity.getSelectedItem().toString();
        if ("Москва".equals(selectedCity)) {
            layoutMkadDistance.setVisibility(View.VISIBLE);
        } else {
            layoutMkadDistance.setVisibility(View.GONE);
            editDistanceMkad.setText("0");
        }
    }

    private void calculatePrice() {
        if (checkPriceByAgreement.isChecked()) return;

        String city = spinnerCity.getSelectedItem().toString();
        String vehicleType = spinnerVehicleType.getSelectedItem().toString();

        int basePrice = 0;
        if (priceTable.containsKey(city) && priceTable.get(city).containsKey(vehicleType)) {
            basePrice = priceTable.get(city).get(vehicleType);
        }

        int extraKm = 0;
        if ("Москва".equals(city)) {
            try {
                extraKm = Integer.parseInt(editDistanceMkad.getText().toString());
            } catch (NumberFormatException e) {
                extraKm = 0;
            }
        }
        int total = basePrice + (extraKm * 100); // 100 руб/км за МКАД
        currentPrice = total;
        textCalculatedPrice.setText("Цена: " + currentPrice + " ₽");
        buttonCreateOrder.setEnabled(true);
    }

    private void createOrder() {
        // Проверка обязательных полей
        String clientName = editClientName.getText().toString().trim();
        String clientPhone = editClientPhone.getText().toString().trim();
        String address = editAddress.getText().toString().trim();
        String description = editProblemDescription.getText().toString().trim();

        if (clientName.isEmpty() || clientPhone.isEmpty() || address.isEmpty() || description.isEmpty()) {
            Toast.makeText(getContext(), "Заполните имя, телефон, адрес и описание проблемы", Toast.LENGTH_SHORT).show();
            return;
        }

        String city = spinnerCity.getSelectedItem().toString();
        String vehicleType = spinnerVehicleType.getSelectedItem().toString();
        String masterType = spinnerMasterType.getSelectedItem().toString();
        String paymentMethod = spinnerPaymentMethod.getSelectedItem().toString();
        boolean priceByAgreement = checkPriceByAgreement.isChecked();

        int estimatedPrice = priceByAgreement ? 0 : currentPrice;
        int distanceMkad = 0;
        try {
            distanceMkad = Integer.parseInt(editDistanceMkad.getText().toString());
        } catch (NumberFormatException ignored) {}

        String userId = PocketBaseClient.getCurrentUserId(); // админ создаёт заказ, можно привязать к админу или к фиктивному клиенту
        // Для теста используем ID админа. Позже клиент будет авторизован, тогда подставится его ID.

        String orderId = UUID.randomUUID().toString();
        Order order = new Order(
                orderId,
                masterType,      // service
                address,
                description,
                editComment.getText().toString().trim(),
                vehicleType,
                editVehicleBrand.getText().toString().trim(),
                editVehicleModel.getText().toString().trim(),
                editVehicleYear.getText().toString().trim(),
                estimatedPrice,
                "new",
                userId,
                false,
                null,
                System.currentTimeMillis(),
                clientName,
                clientPhone,
                city,
                distanceMkad,
                masterType,
                paymentMethod,
                priceByAgreement,
                0   // finalPrice пока 0
        );

        new Thread(() -> {
            boolean success = PocketBaseClient.createOrder(order);
            requireActivity().runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(getContext(), "Заказ создан", Toast.LENGTH_SHORT).show();
                    clearForm();
                } else {
                    Toast.makeText(getContext(), "Ошибка создания заказа", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void clearForm() {
        editClientName.setText("");
        editClientPhone.setText("");
        editAddress.setText("");
        editVehicleBrand.setText("");
        editVehicleModel.setText("");
        editVehicleYear.setText("");
        editProblemDescription.setText("");
        editComment.setText("");
        editDistanceMkad.setText("0");
        checkPriceByAgreement.setChecked(false);
        spinnerCity.setSelection(0);
        spinnerVehicleType.setSelection(0);
        spinnerMasterType.setSelection(0);
        spinnerPaymentMethod.setSelection(0);
        textCalculatedPrice.setText("Цена: 0 ₽");
    }
}