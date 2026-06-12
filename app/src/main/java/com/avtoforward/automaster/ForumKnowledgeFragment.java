package com.avtoforward.automaster;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.card.MaterialCardView;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class ForumKnowledgeFragment extends Fragment {

    private List<String> categoryIds = new ArrayList<>();
    private List<String> categoryNames = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_knowledge_list, container, false);
        LinearLayout containerLayout = view.findViewById(R.id.containerKnowledge);
        containerLayout.removeAllViews();

        new Thread(() -> {
            JsonObject result = PocketBaseClient.getForumCategories("knowledge");
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
                    categoryIds.clear();
                    categoryNames.clear();
                    categoryIds.addAll(ids);
                    categoryNames.addAll(names);
                    buildMenu(containerLayout);
                });
            } else {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Ошибка загрузки категорий", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();

        return view;
    }

    private void buildMenu(LinearLayout container) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (int i = 0; i < categoryNames.size(); i++) {
            String categoryId = categoryIds.get(i);
            String categoryName = categoryNames.get(i);
            MaterialCardView card = (MaterialCardView) inflater.inflate(R.layout.item_forum_category, container, false);
            TextView textView = card.findViewById(R.id.textCategory);
            textView.setText(categoryName);
            card.findViewById(R.id.iconCategory).setVisibility(View.GONE);

            card.setOnClickListener(v -> {
                SubcategoriesFragment fragment = SubcategoriesFragment.newInstance(categoryId);
                FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.container, fragment);
                transaction.addToBackStack(null);
                transaction.commit();
            });

            container.addView(card);
        }
    }
}