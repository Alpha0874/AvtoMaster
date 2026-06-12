package com.avtoforward.automaster;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class AdminVerificationFragment extends Fragment {

    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> userIds = new ArrayList<>();
    private List<String> userEmails = new ArrayList<>();
    private List<String> userNames = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_verification, container, false);
        listView = view.findViewById(R.id.listPendingVerifications);
        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, userEmails);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view1, position, id) -> {
            String userId = userIds.get(position);
            String email = userEmails.get(position);
            String name = userNames.get(position);
            showVerificationDialog(userId, name, email);
        });

        loadPendingVerifications();
        return view;
    }

    private void loadPendingVerifications() {
        new Thread(() -> {
            JsonObject result = PocketBaseClient.getPendingVerifications();
            if (result != null && result.has("items")) {
                JsonArray items = result.getAsJsonArray("items");
                List<String> ids = new ArrayList<>();
                List<String> emails = new ArrayList<>();
                List<String> names = new ArrayList<>();
                for (int i = 0; i < items.size(); i++) {
                    JsonObject item = items.get(i).getAsJsonObject();
                    ids.add(item.get("id").getAsString());
                    emails.add(item.get("email").getAsString());
                    String fullName = item.has("full_name") ? item.get("full_name").getAsString() : "";
                    names.add(fullName);
                }
                requireActivity().runOnUiThread(() -> {
                    userIds.clear();
                    userEmails.clear();
                    userNames.clear();
                    userIds.addAll(ids);
                    userEmails.addAll(emails);
                    userNames.addAll(names);
                    adapter.notifyDataSetChanged();
                });
            }
        }).start();
    }

    private void showVerificationDialog(String userId, String name, String email) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Верификация мастера")
                .setMessage("Мастер: " + name + "\nEmail: " + email + "\n\nПодтвердить регистрацию?")
                .setPositiveButton("Подтвердить", (d, w) -> updateVerification(userId, "verified"))
                .setNegativeButton("Отклонить", (d, w) -> updateVerification(userId, "rejected"))
                .setNeutralButton("Отмена", null)
                .show();
    }

    private void updateVerification(String userId, String status) {
        new Thread(() -> {
            boolean success = PocketBaseClient.updateVerificationStatus(userId, status);
            requireActivity().runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(getContext(), "Статус обновлён", Toast.LENGTH_SHORT).show();
                    loadPendingVerifications();
                } else {
                    Toast.makeText(getContext(), "Ошибка", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPendingVerifications();
    }
}