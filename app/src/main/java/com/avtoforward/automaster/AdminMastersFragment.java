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

public class AdminMastersFragment extends Fragment {

    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> userIds = new ArrayList<>();
    private List<String> userEmails = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_masters, container, false);
        listView = view.findViewById(R.id.listMasters);
        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, userEmails);
        listView.setAdapter(adapter);
        loadMasters();
        listView.setOnItemLongClickListener((parent, view1, position, id) -> {
            String userId = userIds.get(position);
            showDeleteConfirmDialog(userId);
            return true;
        });
        return view;
    }

    private void loadMasters() {
        new Thread(() -> {
            JsonObject result = PocketBaseClient.getUsersByRole("master");
            if (result != null && result.has("items")) {
                JsonArray items = result.getAsJsonArray("items");
                List<String> ids = new ArrayList<>();
                List<String> emails = new ArrayList<>();
                for (int i = 0; i < items.size(); i++) {
                    JsonObject item = items.get(i).getAsJsonObject();
                    ids.add(item.get("id").getAsString());
                    emails.add(item.get("email").getAsString());
                }
                requireActivity().runOnUiThread(() -> {
                    userIds.clear();
                    userEmails.clear();
                    userIds.addAll(ids);
                    userEmails.addAll(emails);
                    adapter.notifyDataSetChanged();
                });
            } else {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Ошибка загрузки мастеров", Toast.LENGTH_SHORT).show()
                );
            }
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
                                loadMasters();
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
        loadMasters();
    }
}