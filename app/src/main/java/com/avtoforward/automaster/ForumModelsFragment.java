package com.avtoforward.automaster;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;

public class ForumModelsFragment extends Fragment {

    private String category;

    public ForumModelsFragment(String category) {
        this.category = category;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_forum_models, container, false);

        TextView title = view.findViewById(R.id.textCategoryTitle);
        title.setText(category + " – выберите модель");

        ListView listView = view.findViewById(R.id.listModels);
        ArrayList<String> models = new ArrayList<>();

        // Заполняем модели в зависимости от категории
        switch (category) {
            case "Легковые":
                models.add("Toyota Camry");
                models.add("Volkswagen Passat");
                models.add("Lada Vesta");
                models.add("Kia Rio");
                break;
            case "Грузовые":
                models.add("ГАЗель");
                models.add("MAN TGS");
                models.add("Volvo FH");
                models.add("Scania R-Series");
                break;
            case "Спецтехника":
                models.add("Экскаватор JCB");
                models.add("Погрузчик Bobcat");
                models.add("Автокран КС-35714");
                break;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_list_item_1,
                models
        );
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, v, position, id) -> {
            String model = models.get(position);
            ((ForumActivity) getActivity()).openChat(model);
        });

        return view;
    }
}