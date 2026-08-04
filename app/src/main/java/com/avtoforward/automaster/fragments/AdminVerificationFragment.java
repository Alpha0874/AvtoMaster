package com.avtoforward.automaster.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.avtoforward.automaster.PocketBaseClient;
import com.avtoforward.automaster.R;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class AdminVerificationFragment extends Fragment {

    private LinearLayout container;
    private List<JsonObject> pendingList = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_verification, container, false);
        this.container = view.findViewById(R.id.verificationContainer);
        loadPendingVerifications();
        return view;
    }

    private void loadPendingVerifications() {
        new Thread(() -> {
            try {
                JsonObject result = PocketBaseClient.getPendingVerifications();
                if (result == null || !result.has("items")) {
                    requireActivity().runOnUiThread(() -> {
                        TextView empty = new TextView(getContext());
                        empty.setText("Нет заявок на верификацию");
                        empty.setTextColor(getResources().getColor(android.R.color.white));
                        container.addView(empty);
                    });
                    return;
                }
                JsonArray items = result.getAsJsonArray("items");
                pendingList.clear();
                for (int i = 0; i < items.size(); i++) {
                    JsonObject user = items.get(i).getAsJsonObject();
                    pendingList.add(user);
                }
                requireActivity().runOnUiThread(() -> displayPending());
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Ошибка загрузки", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void displayPending() {
        container.removeAllViews();
        if (pendingList.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("Нет заявок на верификацию");
            empty.setTextColor(getResources().getColor(android.R.color.white));
            container.addView(empty);
            return;
        }
        for (JsonObject user : pendingList) {
            View card = getLayoutInflater().inflate(R.layout.item_verification, container, false);
            TextView name = card.findViewById(R.id.userName);
            TextView email = card.findViewById(R.id.userEmail);
            Button approve = card.findViewById(R.id.btnApprove);
            Button reject = card.findViewById(R.id.btnReject);

            String fullName = safeGetString(user, "full_name");
            String userEmail = safeGetString(user, "email");
            String userId = safeGetString(user, "id");

            name.setText(fullName.isEmpty() ? "Без имени" : fullName);
            email.setText(userEmail.isEmpty() ? "Нет email" : userEmail);

            approve.setOnClickListener(v -> updateStatus(userId, "verified"));
            reject.setOnClickListener(v -> updateStatus(userId, "rejected"));

            container.addView(card);
        }
    }

    private String safeGetString(JsonObject obj, String key) {
        if (obj == null) return "";
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            try {
                return obj.get(key).getAsString();
            } catch (Exception e) {
                return "";
            }
        }
        return "";
    }

    private void updateStatus(String userId, String status) {
        new Thread(() -> {
            try {
                boolean success = PocketBaseClient.updateVerificationStatus(userId, status);
                requireActivity().runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(getContext(), "Статус обновлён", Toast.LENGTH_SHORT).show();
                        loadPendingVerifications();
                    } else {
                        Toast.makeText(getContext(), "Ошибка обновления", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}