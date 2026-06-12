package com.avtoforward.automaster;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.fragment.app.Fragment;

public class DocumentCategoriesFragment extends Fragment {

    private final String specialization;
    private final String[] categories = {"Легковые", "Грузовые", "Спецтехника", "Мото", "Катера"};

    public DocumentCategoriesFragment(String specialization) {
        this.specialization = specialization;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_document_categories, container, false);
        ListView listView = view.findViewById(R.id.listDocumentCategories);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, categories);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view1, position, id) -> {
            String selectedCategory = categories[position];
            Fragment modelsFragment = new DocumentModelsFragment(specialization, selectedCategory);
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, modelsFragment)
                    .addToBackStack(null)
                    .commit();
        });
        return view;
    }
}