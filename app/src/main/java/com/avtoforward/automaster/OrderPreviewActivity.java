package com.avtoforward.automaster;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.UUID;

public class OrderPreviewActivity extends AppCompatActivity {

    private TextView previewClientName, previewClientPhone, previewCity, previewAddress;
    private TextView previewVehicle, previewService, previewDescription, previewPaymentMethod;
    private TextView previewPrice, previewDistance, previewDistanceLabel;
    private TextView infoText;
    private CheckBox checkPriceByAgreement;
    private Button buttonConfirmOrder, buttonBack;
    private LinearLayout infoLayout;

    private String selectedService, city, vehicleType, paymentMethod;
    private String clientName, clientPhone, address, description, comment;
    private String vehicleBrand, vehicleModel, vehicleYear;
    private int distance, price;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_preview);

        Bundle bundle = getIntent().getExtras();
        if (bundle == null) {
            Toast.makeText(this, "Ошибка данных заказа", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        clientName = bundle.getString("clientName");
        clientPhone = bundle.getString("clientPhone");
        city = bundle.getString("city");
        address = bundle.getString("address");
        vehicleBrand = bundle.getString("vehicleBrand");
        vehicleModel = bundle.getString("vehicleModel");
        vehicleYear = bundle.getString("vehicleYear");
        vehicleType = bundle.getString("vehicleType");
        selectedService = bundle.getString("service");
        description = bundle.getString("description");
        comment = bundle.getString("comment");
        paymentMethod = bundle.getString("paymentMethod");
        distance = bundle.getInt("distance", 0);
        price = bundle.getInt("price", 0);

        // Инициализация виджетов
        previewClientName = findViewById(R.id.previewClientName);
        previewClientPhone = findViewById(R.id.previewClientPhone);
        previewCity = findViewById(R.id.previewCity);
        previewAddress = findViewById(R.id.previewAddress);
        previewVehicle = findViewById(R.id.previewVehicle);
        previewService = findViewById(R.id.previewService);
        previewDescription = findViewById(R.id.previewDescription);
        previewPaymentMethod = findViewById(R.id.previewPaymentMethod);
        previewPrice = findViewById(R.id.previewPrice);
        previewDistance = findViewById(R.id.previewDistance);
        previewDistanceLabel = findViewById(R.id.previewDistanceLabel);
        infoText = findViewById(R.id.infoText);
        infoLayout = findViewById(R.id.infoLayout);
        checkPriceByAgreement = findViewById(R.id.checkPriceByAgreement);
        buttonConfirmOrder = findViewById(R.id.buttonConfirmOrder);
        buttonBack = findViewById(R.id.buttonBack);

        // Заполняем данные
        previewClientName.setText("Заказчик: " + clientName);
        previewClientPhone.setText("Телефон: " + clientPhone);
        previewCity.setText("Город: " + city);
        previewAddress.setText("Адрес: " + address);
        String vehicle = vehicleBrand + " " + vehicleModel + " (" + vehicleYear + ")";
        previewVehicle.setText("ТС: " + vehicle);
        previewService.setText("Услуга: " + selectedService);
        previewDescription.setText("Описание: " + description);
        previewPaymentMethod.setText("Оплата: " + paymentMethod);

        if (distance > 0) {
            previewDistanceLabel.setVisibility(View.VISIBLE);
            previewDistance.setVisibility(View.VISIBLE);
            previewDistance.setText("Расстояние за МКАД: " + distance + " км");
        }

        previewPrice.setText(String.valueOf(price));

        String infoMessage = "Уважаемый заказчик!\n\n" +
                "Благодарим вас за обращение в сервис «АвтоТехПомощь».\n\n" +
                "Ваша заявка будет активна в течение 3 часов. Если за это время ни один мастер не откликнется, " +
                "она автоматически переместится в архив, но останется в истории ваших заказов.\n\n" +
                "Если ваша заявка всё ещё актуальна и вы готовы подождать, вы сможете продлить её через 3 часа " +
                "в разделе «История заказов» — продление снова сделает заявку видимой для мастеров.\n\n" +
                "Мы ценим ваше время и делаем всё возможное, чтобы найти мастера под вашу задачу.\n" +
                "Спасибо, что выбираете наш сервис!";
        infoText.setText(infoMessage);

        checkPriceByAgreement.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                previewPrice.setText("По договорённости");
                previewPrice.setTextColor(getColor(android.R.color.darker_gray));
            } else {
                previewPrice.setText(String.valueOf(price));
                previewPrice.setTextColor(getColor(R.color.switch_thumb_on));
            }
        });

        buttonConfirmOrder.setOnClickListener(v -> createOrder());
        buttonBack.setOnClickListener(v -> finish());
    }

    private void createOrder() {
        String userId = PocketBaseClient.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean priceByAgreement = checkPriceByAgreement.isChecked();
        int finalPrice = priceByAgreement ? 0 : price;

        String orderId = UUID.randomUUID().toString();
        com.avtoforward.automaster.Order order = new com.avtoforward.automaster.Order(
                orderId,
                selectedService,
                address,
                description,
                comment,
                vehicleType,
                vehicleBrand,
                vehicleModel,
                vehicleYear,
                finalPrice,
                "new",
                userId,
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
                    Toast.makeText(this, "Заказ создан!", Toast.LENGTH_SHORT).show();
                    // Переход на главную страницу клиента
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Ошибка создания заказа", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
}