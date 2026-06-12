package com.avtoforward.automaster;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.JsonObject;

public class MenuFragment extends Fragment {

    private SwitchCompat switchAcceptingOrders;
    private TextView textMasterName, textMasterSpecialty;
    private ImageView imageAvatar;
    private View cardAdmin;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);

        switchAcceptingOrders = view.findViewById(R.id.switchAcceptingOrders);
        textMasterName = view.findViewById(R.id.textMasterName);
        textMasterSpecialty = view.findViewById(R.id.textMasterSpecialty);
        imageAvatar = view.findViewById(R.id.imageAvatar);
        cardAdmin = view.findViewById(R.id.cardAdmin);

        loadProfileInfo();      // загрузит имя, специализацию и аватар
        loadSwitchState();
        checkAdminRole();

        switchAcceptingOrders.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateSwitchColors(isChecked);
            String userId = PocketBaseClient.getCurrentUserId();
            if (userId == null) {
                Toast.makeText(getContext(), "Ошибка авторизации", Toast.LENGTH_SHORT).show();
                return;
            }
            new Thread(() -> {
                boolean success = PocketBaseClient.setAcceptingOrders(userId, isChecked);
                requireActivity().runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(getContext(), isChecked ? "Приём заказов включён" : "Приём заказов выключен", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Ошибка изменения статуса", Toast.LENGTH_SHORT).show();
                        switchAcceptingOrders.setOnCheckedChangeListener(null);
                        switchAcceptingOrders.setChecked(!isChecked);
                        updateSwitchColors(!isChecked);
                        switchAcceptingOrders.setOnCheckedChangeListener((btn, ch) -> onCheckedChanged(btn, ch));
                    }
                });
            }).start();
        });

        // Обработчики карточек
        view.findViewById(R.id.cardActiveOrders).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), ActiveOrdersActivity.class)));
        view.findViewById(R.id.cardOrders).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), MyOrdersActivity.class)));
        view.findViewById(R.id.cardStatistics).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), StatisticsActivity.class)));
        view.findViewById(R.id.cardForum).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), ForumActivity.class)));
        view.findViewById(R.id.cardSettings).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), EditMasterProfileActivity.class)));
        view.findViewById(R.id.cardSupport).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), SupportActivity.class)));
        view.findViewById(R.id.cardAppInfo).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), InfoActivity.class)));
        view.findViewById(R.id.cardAppHelp).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), ContributeActivity.class)));

        return view;
    }

    private void updateSwitchColors(boolean isChecked) {
        if (isChecked) {
            switchAcceptingOrders.setThumbTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.holo_green_light));
            switchAcceptingOrders.setTrackTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.holo_green_dark));
        } else {
            switchAcceptingOrders.setThumbTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.holo_red_light));
            switchAcceptingOrders.setTrackTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.holo_red_dark));
        }
    }

    private void loadProfileInfo() {
        String userId = PocketBaseClient.getCurrentUserId();
        if (userId == null) return;
        new Thread(() -> {
            JsonObject user = PocketBaseClient.getUserInfo(userId);
            if (user != null) {
                String name = user.has("full_name") ? user.get("full_name").getAsString() : "Мастер";
                String specialty = user.has("master_status") ? user.get("master_status").getAsString() : "Автомастер";
                String avatarFile = user.has("avatar") && !user.get("avatar").isJsonNull() ? user.get("avatar").getAsString() : null;

                requireActivity().runOnUiThread(() -> {
                    textMasterName.setText(name);
                    textMasterSpecialty.setText(specialty);
                    if (avatarFile != null && !avatarFile.isEmpty()) {
                        String avatarUrl = PocketBaseClient.getBaseUrl() + "/api/files/users/" + userId + "/" + avatarFile;
                        Glide.with(requireContext())
                                .load(avatarUrl)
                                .circleCrop()
                                .placeholder(R.drawable.ic_role_master)
                                .error(R.drawable.ic_role_master)
                                .into(imageAvatar);
                    } else {
                        imageAvatar.setImageResource(R.drawable.ic_role_master);
                    }
                });
            }
        }).start();
    }

    private void loadSwitchState() {
        String userId = PocketBaseClient.getCurrentUserId();
        if (userId == null) return;
        new Thread(() -> {
            boolean accepting = PocketBaseClient.isAcceptingOrders(userId);
            requireActivity().runOnUiThread(() -> {
                switchAcceptingOrders.setOnCheckedChangeListener(null);
                switchAcceptingOrders.setChecked(accepting);
                updateSwitchColors(accepting);
                switchAcceptingOrders.setOnCheckedChangeListener((btn, ch) -> onCheckedChanged(btn, ch));
            });
        }).start();
    }

    private void checkAdminRole() {
        new Thread(() -> {
            String role = PocketBaseClient.getUserRole();
            requireActivity().runOnUiThread(() -> {
                if ("admin".equals(role)) {
                    cardAdmin.setVisibility(View.VISIBLE);
                    cardAdmin.setOnClickListener(v ->
                            startActivity(new Intent(getActivity(), AdminActivity.class)));
                } else {
                    cardAdmin.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    private void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
        String userId = PocketBaseClient.getCurrentUserId();
        if (userId == null) return;
        new Thread(() -> {
            boolean success = PocketBaseClient.setAcceptingOrders(userId, isChecked);
            requireActivity().runOnUiThread(() -> {
                if (success) {
                    updateSwitchColors(isChecked);
                } else {
                    Toast.makeText(getContext(), "Ошибка изменения статуса", Toast.LENGTH_SHORT).show();
                    buttonView.setOnCheckedChangeListener(null);
                    buttonView.setChecked(!isChecked);
                    updateSwitchColors(!isChecked);
                    buttonView.setOnCheckedChangeListener(this::onCheckedChanged);
                }
            });
        }).start();
    }
}