package com.avtoforward.automaster.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.avtoforward.automaster.DocumentsListFragment;
import com.avtoforward.automaster.PocketBaseClient;
import com.avtoforward.automaster.R;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DocumentModelsFragment extends Fragment {
    private static final String ARG_CATEGORY_ID = "category_id";
    private String categoryId;

    public static DocumentModelsFragment newInstance(String categoryId) {
        DocumentModelsFragment fragment = new DocumentModelsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY_ID, categoryId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categoryId = getArguments().getString(ARG_CATEGORY_ID);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_forum_topics, container, false);
        ListView listView = view.findViewById(R.id.listTopics);
        TextView title = view.findViewById(R.id.textCategoryTitle);
        title.setText("Выберите модель");

        new Thread(() -> {
            try {
                String filter = "category='" + categoryId + "'";
                String encodedFilter = URLEncoder.encode(filter, StandardCharsets.UTF_8.name());
                String url = PocketBaseClient.getBaseUrl() + "/api/collections/document_models/records?filter=" + encodedFilter + "&sort=name";
                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(url)
                        .header("Authorization", PocketBaseClient.getAuthToken())
                        .get()
                        .build();
                okhttp3.Response response = PocketBaseClient.getClient().newCall(request).execute();
                if (response.isSuccessful()) {
                    JsonObject result = PocketBaseClient.getGson().fromJson(response.body().string(), JsonObject.class);
                    JsonArray items = result.getAsJsonArray("items");
                    List<String> modelNames = new ArrayList<>();
                    List<String> modelIds = new ArrayList<>();
                    for (int i = 0; i < items.size(); i++) {
                        JsonObject item = items.get(i).getAsJsonObject();
                        modelIds.add(item.get("id").getAsString());
                        modelNames.add(item.get("name").getAsString());
                    }
                    requireActivity().runOnUiThread(() -> {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, modelNames);
                        listView.setAdapter(adapter);
                        listView.setOnItemClickListener((parent, view1, position, id) -> {
                            String selectedModelId = modelIds.get(position);
                            String selectedModelName = modelNames.get(position);
                            DocumentsListFragment fragment = new DocumentsListFragment(selectedModelId, selectedModelName);
                            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
                            transaction.replace(R.id.container, fragment);
                            transaction.addToBackStack(null);
                            transaction.commit();
                        });
                    });
                } else {
                    requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Нет доступных моделей", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Ошибка загрузки моделей", Toast.LENGTH_SHORT).show());
                e.printStackTrace();
            }
        }).start();
        return view;
    }
}