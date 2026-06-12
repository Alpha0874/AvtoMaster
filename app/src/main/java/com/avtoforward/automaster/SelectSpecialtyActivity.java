package com.avtoforward.automaster;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.HashMap;
import java.util.Map;

public class SelectSpecialtyActivity extends AppCompatActivity {

    private RadioGroup radioGroup;
    private Button buttonSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_specialty);

        radioGroup = findViewById(R.id.radioGroupSpecialty);
        buttonSave = findViewById(R.id.buttonSaveSpecialty);

        buttonSave.setOnClickListener(v -> saveSpecialty());
    }

    private void saveSpecialty() {
        int selectedId = radioGroup.getCheckedRadioButtonId();
        String specialty = "";
        if (selectedId == R.id.radioAutoElectrician) {
            specialty = "Автоэлектрик";
        } else if (selectedId == R.id.radioAutoMechanic) {
            specialty = "Автомеханик";
        } else if (selectedId == R.id.radioHydraulic) {
            specialty = "Гидравлик";
        }

        if (specialty.isEmpty()) {
            Toast.makeText(this, "Выберите специализацию", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = PocketBaseClient.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        buttonSave.setEnabled(false);

        // СОХРАНЯЕМ В ПОЛЕ master_status (уже существует)
        Map<String, Object> data = new HashMap<>();
        data.put("master_status", specialty);

        new Thread(() -> {
            boolean success = PocketBaseClient.updateUser(userId, data);
            runOnUiThread(() -> {
                buttonSave.setEnabled(true);
                if (success) {
                    Toast.makeText(this, "Специализация сохранена", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SelectSpecialtyActivity.this, MasterActivity.class));
                    finish();
                } else {
                    Toast.makeText(this, "Ошибка сохранения. Проверьте интернет.", Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }
}