package com.avtoforward.automaster.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.avtoforward.automaster.ClientProfileEditActivity;
import com.avtoforward.automaster.ForegroundNotificationService;
import com.avtoforward.automaster.LoginActivity;
import com.avtoforward.automaster.PocketBaseClient;
import com.avtoforward.automaster.R;
import com.avtoforward.automaster.RoleSelectionActivity;
import com.avtoforward.automaster.utils.SessionManager;
import com.google.gson.JsonObject;

public class ClientProfileFragment extends Fragment {

    private TextView textEmail, textName, textPhone;
    private Button buttonEditProfile, buttonLogout;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_client_profile, container, false);

        textEmail = view.findViewById(R.id.textClientEmail);
        textName = view.findViewById(R.id.textClientName);
        textPhone = view.findViewById(R.id.textClientPhone);
        buttonEditProfile = view.findViewById(R.id.buttonEditProfile);
        buttonLogout = view.findViewById(R.id.buttonLogout);

        loadProfile();

        buttonEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), ClientProfileEditActivity.class));
        });

        buttonLogout.setOnClickListener(v -> {
            // Очищаем сессию через SessionManager
            SessionManager sessionManager = new SessionManager(requireContext());
            sessionManager.logout();

            // Останавливаем сервис уведомлений
            Intent serviceIntent = new Intent(getActivity(), ForegroundNotificationService.class);
            getActivity().stopService(serviceIntent);

            // Выходим из PocketBase (очищаем токен и т.д.)
            PocketBaseClient.logout();

            Toast.makeText(getContext(), "Вы вышли", Toast.LENGTH_SHORT).show();

            // Переходим на экран выбора роли (он сам проверит, что сессии нет, и покажет выбор)
            Intent intent = new Intent(getActivity(), RoleSelectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        return view;
    }

    private void loadProfile() {
        String userId = PocketBaseClient.getCurrentUserId();
        if (userId == null) return;
        new Thread(() -> {
            JsonObject user = PocketBaseClient.getUserInfo(userId);
            if (user != null) {
                requireActivity().runOnUiThread(() -> {
                    String email = user.has("email") ? user.get("email").getAsString() : "не указан";
                    String fullName = user.has("full_name") && !user.get("full_name").isJsonNull()
                            ? user.get("full_name").getAsString() : "не указано";
                    String phone = user.has("phone") && !user.get("phone").isJsonNull()
                            ? user.get("phone").getAsString() : "не указан";

                    textEmail.setText("Email: " + email);
                    textName.setText("Имя: " + fullName);
                    textPhone.setText("Телефон: " + phone);
                });
            }
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
    }
}