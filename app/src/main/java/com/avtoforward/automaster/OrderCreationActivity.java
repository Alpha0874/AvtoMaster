package com.avtoforward.automaster;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;

import java.util.UUID;

public class OrderCreationActivity extends AppCompatActivity {

    private Spinner spinnerCity, spinnerVehicleType, spinnerPaymentMethod;
    private EditText editClientName, editClientPhone, editAddress, editDistanceMkad;
    private EditText editVehicleBrand, editVehicleModel, editVehicleYear;
    private EditText editProblemDescription, editComment, editPrice;
    private CheckBox checkPriceByAgreement;
    private Button buttonContinue;
    private LinearLayout layoutMkadDistance;

    private String currentUserId;
    private String selectedService;
    private int calculatedPrice = 0;

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

        selectedService = getIntent().getStringExtra("selected_service");
        if (selectedService == null) selectedService = "Автоэлектрик";

        spinnerCity = findViewById(R.id.spinnerCity);
        spinnerVehicleType = findViewById(R.id.spinnerVehicleType);
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
        editPrice = findViewById(R.id.editPrice);
        checkPriceByAgreement = findViewById(R.id.checkPriceByAgreement);
        buttonContinue = findViewById(R.id.buttonContinue);
        layoutMkadDistance = findViewById(R.id.layoutMkadDistance);

        setupSpinners();

        // Обновление цены при смене города или типа ТС
        AdapterView.OnItemSelectedListener priceUpdateListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updatePrice();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };
        spinnerCity.setOnItemSelectedListener(priceUpdateListener);
        spinnerVehicleType.setOnItemSelectedListener(priceUpdateListener);

        editDistanceMkad.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) updatePrice();
        });

        checkPriceByAgreement.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                editPrice.setEnabled(false);
                editPrice.setText("0");
            } else {
                editPrice.setEnabled(true);
                updatePrice();
            }
        });

        // Показываем/скрываем поле расстояния для Москвы
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
                updatePrice();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        buttonContinue.setOnClickListener(v -> createOrder());

        // Первоначальный расчёт цены
        updatePrice();
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

        ArrayAdapter<CharSequence> paymentAdapter = ArrayAdapter.createFromResource(this,
                R.array.payment_methods_array, android.R.layout.simple_spinner_item);
        paymentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPaymentMethod.setAdapter(paymentAdapter);
    }

    private void updatePrice() {
        if (checkPriceByAgreement.isChecked()) {
            editPrice.setText("0");
            editPrice.setEnabled(false);
            return;
        }

        String city = spinnerCity.getSelectedItem().toString();
        String vehicleType = spinnerVehicleType.getSelectedItem().toString();
        String distanceStr = editDistanceMkad.getText().toString().trim();

        int distance = 0;
        if (!distanceStr.isEmpty()) {
            try {
                distance = Integer.parseInt(distanceStr);
            } catch (NumberFormatException ignored) {}
        }

        if (city.isEmpty()) {
            editPrice.setText("0");
            editPrice.setEnabled(true);
            return;
        }

        final int finalDistance = distance;
        final String finalCity = city;
        final String finalVehicleType = vehicleType;

        new Thread(() -> {
            JsonObject tariff = PocketBaseClient.getTariff(finalCity, finalVehicleType);
            runOnUiThread(() -> {
                if (tariff != null) {
                    int basePrice = tariff.get("base_price").getAsInt();
                    int pricePerKm = tariff.has("price_per_km") && !tariff.get("price_per_km").isJsonNull()
                            ? tariff.get("price_per_km").getAsInt()
                            : 0;
                    int total = basePrice + finalDistance * pricePerKm;
                    editPrice.setText(String.valueOf(total));
                    editPrice.setEnabled(true);
                } else {
                    editPrice.setText("0");
                    editPrice.setEnabled(true);
                }
            });
        }).start();
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
        String paymentMethod = spinnerPaymentMethod.getSelectedItem().toString();
        boolean priceByAgreement = checkPriceByAgreement.isChecked();

        int estimatedPrice = 0;
        if (!priceByAgreement) {
            try {
                estimatedPrice = Integer.parseInt(editPrice.getText().toString().trim());
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Некорректная цена", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        int distance = 0;
        try {
            distance = Integer.parseInt(editDistanceMkad.getText().toString().trim());
        } catch (NumberFormatException ignored) {}

        String orderId = UUID.randomUUID().toString();
        Order order = new Order(
                orderId,
                selectedService,
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
                distance,
                selectedService,
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