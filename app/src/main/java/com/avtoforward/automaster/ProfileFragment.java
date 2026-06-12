package com.avtoforward.automaster;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.gson.JsonObject;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private TextView textName, textServiceName, textSpecialty, textRating;
    private ImageView imageAvatar;
    private Button buttonChangeRole, buttonForum, buttonEditProfile;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        textName = view.findViewById(R.id.textMasterName);
        textServiceName = view.findViewById(R.id.textServiceName);
        textSpecialty = view.findViewById(R.id.textMasterSpecialty);
        textRating = view.findViewById(R.id.textRating);
        imageAvatar = view.findViewById(R.id.imageProfile);
        buttonChangeRole = view.findViewById(R.id.buttonChangeRole);
        buttonForum = view.findViewById(R.id.buttonForum);
        buttonEditProfile = view.findViewById(R.id.buttonEditProfile);

        loadProfileData();

        buttonChangeRole.setOnClickListener(v -> {
            if (getActivity() != null) {
                Intent serviceIntent = new Intent(getActivity(), ForumNotificationService.class);
                getActivity().stopService(serviceIntent);
            }
            PocketBaseClient.logout();
            SharedPreferences prefs = requireActivity().getSharedPreferences("AutoMasterPrefs", getContext().MODE_PRIVATE);
            prefs.edit().remove("user_role").apply();
            Intent intent = new Intent(getActivity(), RoleSelectionActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });

        buttonForum.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), ForumActivity.class));
        });

        buttonEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), EditMasterProfileActivity.class));
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfileData();
    }

    private void loadProfileData() {
        String userId = PocketBaseClient.getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "Не удалось получить ID пользователя");
            return;
        }

        new Thread(() -> {
            JsonObject user = PocketBaseClient.getUserInfo(userId);
            if (user != null && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    String fullName = getStringOrEmpty(user, "full_name");
                    String nickname = getStringOrEmpty(user, "nickname");
                    String serviceName = getStringOrEmpty(user, "service_name");
                    String email = getStringOrEmpty(user, "email");

                    String displayName = fullName;
                    if (displayName.isEmpty()) displayName = nickname;
                    if (displayName.isEmpty()) displayName = serviceName;
                    if (displayName.isEmpty()) displayName = email;
                    if (displayName.isEmpty()) displayName = "Мастер";
                    textName.setText(displayName);

                    if (!serviceName.isEmpty() && !serviceName.equals(displayName)) {
                        textServiceName.setText(serviceName);
                        textServiceName.setVisibility(View.VISIBLE);
                    } else {
                        textServiceName.setVisibility(View.GONE);
                    }

                    String specialty = getStringOrEmpty(user, "master_status");
                    if (specialty.isEmpty()) specialty = getStringOrEmpty(user, "specialty");
                    if (specialty.isEmpty()) specialty = "Специализация не указана";
                    textSpecialty.setText(specialty);

                    // Аватар через Glide
                    if (user.has("avatar") && !user.get("avatar").isJsonNull()) {
                        String avatarUrl = PocketBaseClient.getBaseUrl() + "/api/files/users/" + userId + "/" + user.get("avatar").getAsString();
                        Log.d(TAG, "Loading avatar URL: " + avatarUrl);
                        Glide.with(this)
                                .load(avatarUrl)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .skipMemoryCache(true)
                                .placeholder(R.drawable.ic_role_master)
                                .error(R.drawable.ic_role_master)
                                .into(imageAvatar);
                    } else {
                        imageAvatar.setImageResource(R.drawable.ic_role_master);
                    }
                });
            } else {
                Log.e(TAG, "Не удалось загрузить данные пользователя");
            }
        }).start();
    }

    private String getStringOrEmpty(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return "";
    }
}