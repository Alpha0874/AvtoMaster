package com.avtoforward.automaster;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OrderCreationActivity extends AppCompatActivity {

    private Spinner spinnerCity, spinnerVehicleType, spinnerMasterType, spinnerPaymentMethod;
    private EditText editClientName, editClientPhone, editAddress, editDistanceMkad;
    private EditText editVehicleBrand, editVehicleModel, editVehicleYear;
    private EditText editProblemDescription, editComment;
    private CheckBox checkPriceByAgreement;
    private Button buttonCreateOrder;
    private LinearLayout layoutMkadDistance;

    private Map<String, Map<String, Integer>> priceTable = new HashMap<>();
    private String currentUserId;
    private String selectedService; // из extra

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_creation);

        currentUserId = PocketBaseClient.getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Получаем выбранную услугу
        selectedService = getIntent().getStringExtra("selected_service");
        if (selectedService == null) selectedService = "Автоэлектрик"; // по умолчанию

        // Инициализация виджетов
        spinnerCity = findViewById(R.id.spinnerCity);
        spinnerVehicleType = findViewById(R.id.spinnerVehicleType);
        spinnerMasterType = findViewById(R.id.spinnerMasterType);
        spinnerPaymentMethod = findViewById(R.id.spinnerPaymentMethod);
        editClientName = findViewById(R.id.editClientName);
        editClientPhone = findViewById(R.id.editClientPhone);
        editAddress = findViewById(R.id.editAddress);
        editDistanceMkad = findViewById(R.id.editDistanceMkad);
        editVehicleBrand = findViewById(R.id.editVehicleBrand);
        editVehicleModel = findViewById(R.id.editVehicleModel);
        editVehicleYear = findViewById(R.id.editVehicleYear);
        editProblemDescription = findViewById(R.id.editProblemDescription);
        editComment = findViewById(R.id.editComment);
        checkPriceByAgreement = findViewById(R.id.checkPriceByAgreement);
        buttonCreateOrder = findViewById(R.id.buttonCreateOrder);
        layoutMkadDistance = findViewById(R.id.layoutMkadDistance);

        setupSpinners();
        preSelectMasterType(); // устанавливаем выбранную услугу в спиннер

        spinnerCity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String city = spinnerCity.getSelectedItem().toString();
                if ("Москва".equals(city)) {
                    layoutMkadDistance.setVisibility(View.VISIBLE);
                } else {
                    layoutMkadDistance.setVisibility(View.GONE);
                    editDistanceMkad.setText("0");
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        buttonCreateOrder.setOnClickListener(v -> createOrder());

        initPriceTable();
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> cityAdapter = ArrayAdapter.createFromResource(this,
                R.array.cities_array, android.R.layout.simple_spinner_item);
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCity.setAdapter(cityAdapter);

        ArrayAdapter<CharSequence> vehicleAdapter = ArrayAdapter.createFromResource(this,
                R.array.vehicle_types_array, android.R.layout.simple_spinner_item);
        vehicleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVehicleType.setAdapter(vehicleAdapter);

        ArrayAdapter<CharSequence> masterAdapter = ArrayAdapter.createFromResource(this,
                R.array.master_types_array, android.R.layout.simple_spinner_item);
        masterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMasterType.setAdapter(masterAdapter);

        ArrayAdapter<CharSequence> paymentAdapter = ArrayAdapter.createFromResource(this,
                R.array.payment_methods_array, android.R.layout.simple_spinner_item);
        paymentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPaymentMethod.setAdapter(paymentAdapter);
    }

    private void preSelectMasterType() {
        // Устанавливаем выбранную услугу в спиннер masterType
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerMasterType.getAdapter();
        int position = adapter.getPosition(selectedService);
        if (position >= 0) {
            spinnerMasterType.setSelection(position);
        }
    }

    private void initPriceTable() {
        Map<String, Integer> moscow = new HashMap<>();
        moscow.put("Легковой", 6000);
        moscow.put("Лёгкий коммерческий", 8000);
        moscow.put("Грузовой от 5т", 15000);
        moscow.put("Спецтехника", 17000);
        priceTable.put("Москва", moscow);

        Map<String, Integer> krasnodar = new HashMap<>();
        krasnodar.put("Легковой", 4000);
        krasnodar.put("Лёгкий коммерческий", 5000);
        krasnodar.put("Грузовой от 5т", 8000);
        krasnodar.put("Спецтехника", 10000);
        priceTable.put("Краснодар", krasnodar);
    }

    private void createOrder() {
        String clientName = editClientName.getText().toString().trim();
        String clientPhone = editClientPhone.getText().toString().trim();
        String address = editAddress.getText().toString().trim();
        String description = editProblemDescription.getText().toString().trim();

        if (clientName.isEmpty() || clientPhone.isEmpty() || address.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Заполните имя, телефон, адрес и описание проблемы", Toast.LENGTH_SHORT).show();
            return;
        }

        String city = spinnerCity.getSelectedItem().toString();
        String vehicleType = spinnerVehicleType.getSelectedItem().toString();
        String masterType = spinnerMasterType.getSelectedItem().toString();
        String paymentMethod = spinnerPaymentMethod.getSelectedItem().toString();
        boolean priceByAgreement = checkPriceByAgreement.isChecked();

        int estimatedPrice = 0;
        if (!priceByAgreement) {
            int basePrice = priceTable.get(city).getOrDefault(vehicleType, 0);
            int extraKm = 0;
            if ("Москва".equals(city)) {
                try {
                    extraKm = Integer.parseInt(editDistanceMkad.getText().toString());
                } catch (NumberFormatException ignored) {}
            }
            estimatedPrice = basePrice + (extraKm * 100);
        }

        String orderId = UUID.randomUUID().toString();
        com.avtoforward.automaster.Order order = new com.avtoforward.automaster.Order(
                orderId,
                masterType,
                address,
                description,
                editComment.getText().toString().trim(),
                vehicleType,
                editVehicleBrand.getText().toString().trim(),
                editVehicleModel.getText().toString().trim(),
                editVehicleYear.getText().toString().trim(),
                estimatedPrice,
                "new",
                currentUserId,
                false,
                null,
                System.currentTimeMillis(),
                clientName,
                clientPhone,
                city,
                Integer.parseInt(editDistanceMkad.getText().toString()),
                masterType,
                paymentMethod,
                priceByAgreement,
                0
        );

        new Thread(() -> {
            boolean success = PocketBaseClient.createOrder(order);
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "Заказ создан", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Ошибка создания заказа", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (PocketBaseClient.isLoggedIn()) {
            new Thread(PocketBaseClient::updateLastOnline).start();
        }
    }
}