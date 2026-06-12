package com.avtoforward.automaster;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.fragment.app.Fragment;

public class DocumentSpecializationsFragment extends Fragment {

    private final String[] specializations = {"Автоэлектрик", "Автомеханик", "Гидравлик", "Пневматик"};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_document_specializations, container, false);
        ListView listView = view.findViewById(R.id.listSpecializations);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, specializations);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view1, position, id) -> {
            String spec = specializations[position];
            // Открыть выбор категории техники, передав специализацию
            Fragment catFragment = new DocumentCategoriesFragment(spec);
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, catFragment)
                    .addToBackStack(null)
                    .commit();
        });
        return view;
    }
}