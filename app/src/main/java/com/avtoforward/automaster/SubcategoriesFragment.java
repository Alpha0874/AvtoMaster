package com.avtoforward.automaster;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class SubcategoriesFragment extends Fragment {

    private String categoryId;
    private List<String> subcategoryIds = new ArrayList<>();
    private List<String> subcategoryNames = new ArrayList<>();

    public static SubcategoriesFragment newInstance(String categoryId) {
        SubcategoriesFragment fragment = new SubcategoriesFragment();
        Bundle args = new Bundle();
        args.putString("categoryId", categoryId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categoryId = getArguments().getString("categoryId");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_subcategories, container, false);
        TextView title = view.findViewById(R.id.textSubcategoryTitle);
        title.setText("Выберите тип техники");
        ListView listView = view.findViewById(R.id.listSubcategories);

        new Thread(() -> {
            JsonObject result = PocketBaseClient.getSubcategories(categoryId);
            if (result != null && result.has("items")) {
                JsonArray items = result.getAsJsonArray("items");
                List<String> ids = new ArrayList<>();
                List<String> names = new ArrayList<>();
                for (int i = 0; i < items.size(); i++) {
                    JsonObject item = items.get(i).getAsJsonObject();
                    ids.add(item.get("id").getAsString());
                    names.add(item.get("name").getAsString());
                }
                requireActivity().runOnUiThread(() -> {
                    subcategoryIds.clear();
                    subcategoryNames.clear();
                    subcategoryIds.addAll(ids);
                    subcategoryNames.addAll(names);
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                            android.R.layout.simple_list_item_1, subcategoryNames);
                    listView.setAdapter(adapter);
                    listView.setOnItemClickListener((parent, view1, position, id) -> {
                        String subcategoryId = subcategoryIds.get(position);
                        String subcategoryName = subcategoryNames.get(position);
                        com.avtoforward.automaster.fragments.ForumTopicsFragment topicsFragment = new com.avtoforward.automaster.fragments.ForumTopicsFragment(subcategoryId, subcategoryName);
                        requireActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.container, topicsFragment)
                                .addToBackStack(null)
                                .commit();
                    });
                });
            } else {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Нет подкатегорий", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();

        return view;
    }
}