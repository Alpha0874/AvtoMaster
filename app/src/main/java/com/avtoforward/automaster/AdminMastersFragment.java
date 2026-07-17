package com.avtoforward.automaster;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class AdminMastersFragment extends Fragment {

    private static final String TAG = "AdminMasters";

    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> userIds = new ArrayList<>();
    private List<String> userDisplayNames = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_masters, container, false);
        listView = view.findViewById(R.id.listMasters);
        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, userDisplayNames);
        listView.setAdapter(adapter);
        loadAllUsers();

        // Короткое нажатие — просмотр информации о мастере
        listView.setOnItemClickListener((parent, view1, position, id) -> {
            String userId = userIds.get(position);
            showMasterDetails(userId);
        });

        // Долгое нажатие — удаление (как было)
        listView.setOnItemLongClickListener((parent, view1, position, id) -> {
            String userId = userIds.get(position);
            showDeleteConfirmDialog(userId);
            return true;
        });

        return view;
    }

    private void loadAllUsers() {
        new Thread(() -> {
            try {
                JsonObject result = PocketBaseClient.getAllUsers();
                Log.d(TAG, "loadAllUsers: result = " + (result != null ? result.toString() : "null"));

                if (result != null && result.has("items")) {
                    JsonArray items = result.getAsJsonArray("items");
                    Log.d(TAG, "Всего пользователей (из ответа): " + items.size());

                    List<String> ids = new ArrayList<>();
                    List<String> displayNames = new ArrayList<>();

                    for (int i = 0; i < items.size(); i++) {
                        JsonObject item = items.get(i).getAsJsonObject();
                        String id = item.get("id").getAsString();
                        String email = item.has("email") ? item.get("email").getAsString() : "без email";
                        String role = item.has("role") && !item.get("role").isJsonNull()
                                ? item.get("role").getAsString()
                                : "не задана";
                        String fullName = item.has("full_name") && !item.get("full_name").isJsonNull()
                                ? item.get("full_name").getAsString()
                                : "";

                        // Показываем только мастеров (роль "master")
                        if (!"master".equals(role)) continue;

                        String display = fullName.isEmpty() ? email : fullName + " (" + email + ")";
                        ids.add(id);
                        displayNames.add(display);
                    }

                    requireActivity().runOnUiThread(() -> {
                        userIds.clear();
                        userDisplayNames.clear();
                        userIds.addAll(ids);
                        userDisplayNames.addAll(displayNames);
                        adapter.notifyDataSetChanged();

                        if (ids.isEmpty()) {
                            Toast.makeText(getContext(), "Мастеров с ролью 'master' не найдено. Проверьте базу.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getContext(), "Загружено мастеров: " + ids.size(), Toast.LENGTH_SHORT).show();
                        }
                    });

                } else {
                    Log.e(TAG, "Ответ сервера пустой или нет поля items");
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Ошибка загрузки: сервер вернул пустой ответ", Toast.LENGTH_LONG).show()
                    );
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка загрузки", e);
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void showMasterDetails(String userId) {
        new Thread(() -> {
            JsonObject user = PocketBaseClient.getUserInfo(userId);
            if (user == null) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Не удалось загрузить данные мастера", Toast.LENGTH_SHORT).show()
                );
                return;
            }

            requireActivity().runOnUiThread(() -> {
                // Собираем информацию
                String email = user.has("email") ? user.get("email").getAsString() : "не указан";
                String fullName = user.has("full_name") && !user.get("full_name").isJsonNull()
                        ? user.get("full_name").getAsString() : "не указано";
                String nickname = user.has("nickname") && !user.get("nickname").isJsonNull()
                        ? user.get("nickname").getAsString() : "не указан";
                String phone = user.has("phone") && !user.get("phone").isJsonNull()
                        ? user.get("phone").getAsString() : "не указан";
                String city = user.has("city") && !user.get("city").isJsonNull()
                        ? user.get("city").getAsString() : "не указан";
                String masterStatus = user.has("master_status") && !user.get("master_status").isJsonNull()
                        ? user.get("master_status").getAsString() : "не указан";
                String hasScanner = user.has("has_scanner") && !user.get("has_scanner").isJsonNull()
                        ? user.get("has_scanner").getAsString() : "нет";
                String corporateReady = user.has("corporate_ready") && !user.get("corporate_ready").isJsonNull()
                        ? user.get("corporate_ready").getAsString() : "нет";
                String verificationStatus = user.has("verification_status") && !user.get("verification_status").isJsonNull()
                        ? user.get("verification_status").getAsString() : "pending";

                String statusText;
                int color;
                if ("verified".equals(verificationStatus)) {
                    statusText = "✅ Подтверждён";
                    color = ContextCompat.getColor(requireContext(), R.color.switch_thumb_on);
                } else if ("pending".equals(verificationStatus)) {
                    statusText = "⏳ На проверке";
                    color = ContextCompat.getColor(requireContext(), R.color.text_secondary);
                } else {
                    statusText = "❌ Отклонён";
                    color = ContextCompat.getColor(requireContext(), R.color.switch_thumb_off);
                }

                String message = "Email: " + email + "\n"
                        + "ФИО: " + fullName + "\n"
                        + "Никнейм: " + nickname + "\n"
                        + "Телефон: " + phone + "\n"
                        + "Город: " + city + "\n"
                        + "Статус мастера: " + masterStatus + "\n"
                        + "Сканер: " + ("yes".equals(hasScanner) ? "✅ есть" : "❌ нет") + "\n"
                        + "Корпоративные клиенты: " + ("yes".equals(corporateReady) ? "✅ готов" : "❌ не готов") + "\n"
                        + "Верификация: " + statusText;

                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("Информация о мастере")
                        .setMessage(message)
                        .setPositiveButton("Закрыть", null);

                // Кнопки для управления верификацией (если не подтверждён)
                if ("pending".equals(verificationStatus)) {
                    builder.setNeutralButton("Подтвердить", (dialog, which) -> {
                        updateVerification(userId, "verified");
                    });
                    builder.setNegativeButton("Отклонить", (dialog, which) -> {
                        updateVerification(userId, "rejected");
                    });
                } else if ("rejected".equals(verificationStatus)) {
                    builder.setNeutralButton("Подтвердить", (dialog, which) -> {
                        updateVerification(userId, "verified");
                    });
                } else if ("verified".equals(verificationStatus)) {
                    builder.setNegativeButton("Отклонить", (dialog, which) -> {
                        updateVerification(userId, "rejected");
                    });
                }

                builder.show();
            });
        }).start();
    }

    private void updateVerification(String userId, String newStatus) {
        new Thread(() -> {
            boolean success = PocketBaseClient.updateVerificationStatus(userId, newStatus);
            requireActivity().runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(getContext(), "Статус изменён на: " + newStatus, Toast.LENGTH_SHORT).show();
                    // Обновляем список (перезагружаем)
                    loadAllUsers();
                } else {
                    Toast.makeText(getContext(), "Ошибка изменения статуса", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void showDeleteConfirmDialog(String userId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Удалить мастера?")
                .setMessage("Все данные мастера будут удалены без возможности восстановления.")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    new Thread(() -> {
                        boolean success = PocketBaseClient.deleteUser(userId);
                        requireActivity().runOnUiThread(() -> {
                            if (success) {
                                Toast.makeText(getContext(), "Мастер удалён", Toast.LENGTH_SHORT).show();
                                loadAllUsers();
                            } else {
                                Toast.makeText(getContext(), "Ошибка удаления", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }).start();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAllUsers();
    }
}