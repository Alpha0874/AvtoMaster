package com.avtoforward.automaster;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.gson.JsonObject;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

public class UserProfileInfoFragment extends Fragment {

    private EditText editFullName, editPhone, editEmail;
    private Button buttonSave, buttonLogout;
    private ImageView imageAvatar;
    private String userId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_profile_info, container, false);

        editFullName = view.findViewById(R.id.editFullName);
        editPhone = view.findViewById(R.id.editPhone);
        editEmail = view.findViewById(R.id.editEmail);
        buttonSave = view.findViewById(R.id.buttonSave);
        buttonLogout = view.findViewById(R.id.buttonLogout);
        imageAvatar = view.findViewById(R.id.imageAvatar);

        userId = PocketBaseClient.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(getContext(), "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            return view;
        }

        loadUserData();

        buttonSave.setOnClickListener(v -> saveUserData());
        buttonLogout.setOnClickListener(v -> confirmLogout());

        return view;
    }

    private void loadUserData() {
        new Thread(() -> {
            JsonObject user = PocketBaseClient.getUserInfo(userId);
            if (user != null && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    String fullName = user.has("full_name") && !user.get("full_name").isJsonNull()
                            ? user.get("full_name").getAsString() : "";
                    String phone = user.has("phone") && !user.get("phone").isJsonNull()
                            ? user.get("phone").getAsString() : "";
                    String email = user.has("email") && !user.get("email").isJsonNull()
                            ? user.get("email").getAsString() : "";

                    editFullName.setText(fullName);
                    editPhone.setText(phone);
                    editEmail.setText(email);

                    if (user.has("avatar") && !user.get("avatar").isJsonNull()) {
                        String avatarUrl = PocketBaseClient.getBaseUrl() + "/api/files/users/" + userId + "/" + user.get("avatar").getAsString();
                        Picasso.get().load(avatarUrl).placeholder(R.drawable.ic_role_user).into(imageAvatar);
                    } else {
                        imageAvatar.setImageResource(R.drawable.ic_role_user);
                    }
                });
            }
        }).start();
    }

    private void saveUserData() {
        String fullName = editFullName.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        // email не меняем (обычно не редактируется, но можно разрешить)
        String email = editEmail.getText().toString().trim();

        Map<String, Object> data = new HashMap<>();
        data.put("full_name", fullName);
        data.put("phone", phone);
        data.put("email", email);

        new Thread(() -> {
            boolean success = PocketBaseClient.updateUser(userId, data);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(getContext(), "Данные сохранены", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Ошибка сохранения", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Выход")
                .setMessage("Вы уверены, что хотите выйти из аккаунта?")
                .setPositiveButton("Да", (dialog, which) -> {
                    PocketBaseClient.logout();
                    Intent intent = new Intent(getActivity(), RoleSelectionActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
}