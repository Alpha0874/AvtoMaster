package com.avtoforward.automaster;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.avtoforward.automaster.utils.SessionManager;

public class RoleSelectionActivity extends AppCompatActivity {

    private LinearLayout cardClient, cardMaster, cardTowTruck, cardCorporate;
    private LinearLayout descClient, descMaster;
    private TextView descTowTruck, descCorporate;
    private ImageView arrowClient, arrowMaster, arrowTowTruck, arrowCorporate;
    private Button buttonToLogin;
    private Button buttonClientLogin, buttonMasterLogin;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        if (sessionManager.isLoggedIn()) {
            redirectToMain();
            finish();
            return;
        }

        setContentView(R.layout.activity_role_selection);

        // Находим элементы
        cardClient = findViewById(R.id.cardClient);
        cardMaster = findViewById(R.id.cardMaster);
        cardTowTruck = findViewById(R.id.cardTowTruck);
        cardCorporate = findViewById(R.id.cardCorporate);

        descClient = findViewById(R.id.descClient);
        descMaster = findViewById(R.id.descMaster);
        descTowTruck = findViewById(R.id.descTowTruck);
        descCorporate = findViewById(R.id.descCorporate);

        arrowClient = findViewById(R.id.arrowClient);
        arrowMaster = findViewById(R.id.arrowMaster);
        arrowTowTruck = findViewById(R.id.arrowTowTruck);
        arrowCorporate = findViewById(R.id.arrowCorporate);

        buttonClientLogin = findViewById(R.id.buttonClientLogin);
        buttonMasterLogin = findViewById(R.id.buttonMasterLogin);
        buttonToLogin = findViewById(R.id.buttonToLogin);

        // Обработчики кликов для карточек
        cardClient.setOnClickListener(v -> toggleCard(descClient, arrowClient, "client"));
        cardMaster.setOnClickListener(v -> toggleCard(descMaster, arrowMaster, "master"));
        cardTowTruck.setOnClickListener(v -> {
            // Показываем заглушку "В разработке"
            toggleDescription(descTowTruck, arrowTowTruck);
        });
        cardCorporate.setOnClickListener(v -> {
            toggleDescription(descCorporate, arrowCorporate);
        });

        // Обработчики кнопок "Войти или зарегистрироваться"
        buttonClientLogin.setOnClickListener(v -> goToLogin());
        buttonMasterLogin.setOnClickListener(v -> goToLogin());

        buttonToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RoleSelectionActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void toggleCard(LinearLayout description, ImageView arrow, String role) {
        if (description.getVisibility() == View.GONE) {
            // Показываем описание
            description.setVisibility(View.VISIBLE);
            arrow.setRotation(180);
        } else {
            // Если описание уже открыто, просто скрываем (или ничего не делаем)
            description.setVisibility(View.GONE);
            arrow.setRotation(0);
        }
    }

    private void toggleDescription(TextView description, ImageView arrow) {
        if (description.getVisibility() == View.GONE) {
            description.setVisibility(View.VISIBLE);
            arrow.setRotation(180);
        } else {
            description.setVisibility(View.GONE);
            arrow.setRotation(0);
        }
    }

    private void goToLogin() {
        startActivity(new Intent(RoleSelectionActivity.this, LoginActivity.class));
        finish();
    }

    private void redirectToMain() {
        String role = sessionManager.getUserRole();
        if ("admin".equals(role)) {
            startActivity(new Intent(RoleSelectionActivity.this, AdminActivity.class));
        } else if ("master".equals(role)) {
            startActivity(new Intent(RoleSelectionActivity.this, MasterActivity.class));
        } else {
            startActivity(new Intent(RoleSelectionActivity.this, MainActivity.class));
        }
    }
}