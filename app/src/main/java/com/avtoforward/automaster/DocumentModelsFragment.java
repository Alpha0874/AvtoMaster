package com.avtoforward.automaster;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class DocumentModelsFragment extends Fragment {

    private static final String TAG = "DocumentModels";
    private final String specialization;
    private final String category;
    private ListView listView;
    private List<String> modelNames = new ArrayList<>();
    private List<String> modelIds = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    public DocumentModelsFragment(String specialization, String category) {
        this.specialization = specialization;
        this.category = category;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_document_models, container, false);
        listView = view.findViewById(R.id.listModels);
        TextView title = view.findViewById(R.id.textModelsTitle);
        title.setText("Модели: " + category);

        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, modelNames);
        listView.setAdapter(adapter);

        loadModels();

        listView.setOnItemClickListener((parent, view1, position, id) -> {
            String modelId = modelIds.get(position);
            String modelName = modelNames.get(position);
            Fragment docsFragment = new DocumentsListFragment(modelId, modelName);
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, docsFragment)
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private void loadModels() {
        new Thread(() -> {
            try {
                // Экранируем кавычки внутри строк
                String safeCategory = category.replace("'", "''");
                String safeSpec = specialization.replace("'", "''");
                String filter = "category='" + safeCategory + "'";
                Log.d(TAG, "Filter: " + filter);
                JsonObject result = PocketBaseClient.getModelsByFilter(filter);
                Log.d(TAG, "Result: " + (result == null ? "null" : result.toString()));
                if (result != null && result.has("items")) {
                    JsonArray items = result.getAsJsonArray("items");
                    List<String> names = new ArrayList<>();
                    List<String> ids = new ArrayList<>();
                    for (int i = 0; i < items.size(); i++) {
                        JsonObject item = items.get(i).getAsJsonObject();
                        names.add(item.get("name").getAsString());
                        ids.add(item.get("id").getAsString());
                        Log.d(TAG, "Model: " + item.get("name").getAsString());
                    }
                    requireActivity().runOnUiThread(() -> {
                        modelNames.clear();
                        modelIds.clear();
                        modelNames.addAll(names);
                        modelIds.addAll(ids);
                        adapter.notifyDataSetChanged();
                    });
                } else {
                    Log.e(TAG, "No 'items' in result or result is null");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading models", e);
            }
        }).start();
    }
}