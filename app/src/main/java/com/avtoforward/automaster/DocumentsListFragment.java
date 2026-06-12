package com.avtoforward.automaster;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DocumentsListFragment extends Fragment {

    private final String modelId;
    private final String modelName;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private final List<String> docTitles = new ArrayList<>();
    private final List<String> docIds = new ArrayList<>();
    private final List<String> docFileNames = new ArrayList<>();
    private final List<String> docCollectionIds = new ArrayList<>();
    private final List<String> docAuthors = new ArrayList<>(); // для проверки авторства
    private EditText editSearch;
    private Button buttonSearch;

    private ActivityResultLauncher<Intent> pdfPickerLauncher;
    private String pendingTitle = "";
    private String pendingDesc = "";

    public DocumentsListFragment(String modelId, String modelName) {
        this.modelId = modelId;
        this.modelName = modelName;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_documents_list, container, false);
        listView = view.findViewById(R.id.listDocuments);
        TextView title = view.findViewById(R.id.textDocumentsTitle);
        title.setText("Документы: " + modelName);
        Button btnUpload = view.findViewById(R.id.buttonUploadDocument);
        editSearch = view.findViewById(R.id.editSearch);
        buttonSearch = view.findViewById(R.id.buttonSearch);

        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, docTitles);
        listView.setAdapter(adapter);

        loadDocuments();

        btnUpload.setOnClickListener(v -> showUploadDialog());

        buttonSearch.setOnClickListener(v -> filterDocuments());

        listView.setOnItemClickListener((parent, view1, position, id) -> {
            String docId = docIds.get(position);
            openDocument(docId);
        });

        // Долгое нажатие – меню редактирования/удаления (только для своих документов)
        listView.setOnItemLongClickListener((parent, view1, position, id) -> {
            String currentUserId = PocketBaseClient.getCurrentUserId();
            if (currentUserId == null) return false;
            String authorId = docAuthors.get(position);
            if (currentUserId.equals(authorId)) {
                showDocumentActionsDialog(position);
            } else {
                Toast.makeText(getContext(), "Вы не автор этого документа", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        pdfPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            uploadSelectedFile(uri);
                        } else {
                            Toast.makeText(getContext(), "Файл не выбран", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        return view;
    }

    private void loadDocuments() {
        new Thread(() -> {
            try {
                JsonObject result = PocketBaseClient.getDocumentsByModel(modelId);
                Log.d("DocumentsList", "RESULT: " + (result == null ? "null" : result.toString()));

                if (result != null && result.has("items")) {
                    JsonArray items = result.getAsJsonArray("items");
                    List<String> titles = new ArrayList<>();
                    List<String> ids = new ArrayList<>();
                    List<String> fileNames = new ArrayList<>();
                    List<String> collIds = new ArrayList<>();
                    List<String> authors = new ArrayList<>();

                    for (int i = 0; i < items.size(); i++) {
                        JsonObject item = items.get(i).getAsJsonObject();
                        titles.add(item.get("title").getAsString());
                        ids.add(item.get("id").getAsString());
                        collIds.add(item.get("collectionId").getAsString());
                        authors.add(item.get("uploaded_by").getAsString());

                        // Безопасный парсинг поля file
                        String fileName = "";
                        if (item.has("file") && !item.get("file").isJsonNull()) {
                            try {
                                JsonArray files = item.getAsJsonArray("file");
                                if (files != null && files.size() > 0) {
                                    fileName = files.get(0).getAsString();
                                }
                            } catch (ClassCastException e) {
                                fileName = item.get("file").getAsString();
                            }
                        }
                        fileNames.add(fileName);
                    }

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            docTitles.clear();
                            docIds.clear();
                            docFileNames.clear();
                            docCollectionIds.clear();
                            docAuthors.clear();
                            docTitles.addAll(titles);
                            docIds.addAll(ids);
                            docFileNames.addAll(fileNames);
                            docCollectionIds.addAll(collIds);
                            docAuthors.addAll(authors);
                            adapter.notifyDataSetChanged();
                        });
                    }
                } else {
                    Log.e("DocumentsList", "Result has no 'items' or result is null");
                }
            } catch (Exception e) {
                Log.e("DocumentsList", "Error loading documents", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Ошибка загрузки списка", Toast.LENGTH_SHORT).show());
                }
            }
        }).start();
    }

    private void filterDocuments() {
        String query = editSearch.getText().toString().trim().toLowerCase();
        if (query.isEmpty()) {
            adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, docTitles);
            listView.setAdapter(adapter);
            return;
        }
        List<String> filtered = new ArrayList<>();
        for (String title : docTitles) {
            if (title.toLowerCase().contains(query)) {
                filtered.add(title);
            }
        }
        ArrayAdapter<String> filteredAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, filtered);
        listView.setAdapter(filteredAdapter);
    }

    private void showUploadDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Загрузить документ");
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_upload_document, null);
        EditText editTitle = dialogView.findViewById(R.id.editDocTitle);
        EditText editDesc = dialogView.findViewById(R.id.editDocDesc);
        builder.setView(dialogView);
        builder.setPositiveButton("Выбрать файл", (dialog, which) -> {
            pendingTitle = editTitle.getText().toString().trim();
            pendingDesc = editDesc.getText().toString().trim();
            if (pendingTitle.isEmpty()) {
                Toast.makeText(getContext(), "Введите название документа", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            pdfPickerLauncher.launch(Intent.createChooser(intent, "Выберите PDF"));
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void uploadSelectedFile(Uri uri) {
        new Thread(() -> {
            try {
                File tempFile = PocketBaseClient.copyUriToTempFile(uri);
                if (tempFile == null) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Не удалось скопировать файл", Toast.LENGTH_SHORT).show());
                    }
                    return;
                }
                boolean success = PocketBaseClient.uploadDocument(pendingTitle, pendingDesc, modelId, tempFile.getAbsolutePath());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (success) {
                            Toast.makeText(getContext(), "Документ загружен", Toast.LENGTH_SHORT).show();
                            loadDocuments();
                        } else {
                            Toast.makeText(getContext(), "Ошибка загрузки на сервер", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("DocumentsList", "upload error", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        }).start();
    }

    private void showDocumentActionsDialog(int position) {
        String docId = docIds.get(position);
        String oldTitle = docTitles.get(position);
        String oldDesc = ""; // описание не отображается в списке, можно не получать
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Действие")
                .setItems(new String[]{"Редактировать", "Удалить"}, (dialog, which) -> {
                    if (which == 0) {
                        showEditDialog(docId, oldTitle);
                    } else if (which == 1) {
                        confirmDelete(docId);
                    }
                });
        builder.show();
    }

    private void showEditDialog(String docId, String oldTitle) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Редактировать документ");
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_document, null);
        EditText editTitle = dialogView.findViewById(R.id.editDocTitle);
        EditText editDesc = dialogView.findViewById(R.id.editDocDesc);
        editTitle.setText(oldTitle);
        builder.setView(dialogView);
        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String newTitle = editTitle.getText().toString().trim();
            String newDesc = editDesc.getText().toString().trim();
            if (newTitle.isEmpty()) {
                Toast.makeText(getContext(), "Название не может быть пустым", Toast.LENGTH_SHORT).show();
                return;
            }
            new Thread(() -> {
                boolean success = PocketBaseClient.updateDocument(docId, newTitle, newDesc);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (success) {
                            Toast.makeText(getContext(), "Документ обновлён", Toast.LENGTH_SHORT).show();
                            loadDocuments();
                        } else {
                            Toast.makeText(getContext(), "Ошибка обновления", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }).start();
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void confirmDelete(String docId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Удалить документ?")
                .setMessage("Файл будет удалён безвозвратно.")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    new Thread(() -> {
                        boolean success = PocketBaseClient.deleteDocument(docId);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (success) {
                                    Toast.makeText(getContext(), "Документ удалён", Toast.LENGTH_SHORT).show();
                                    loadDocuments();
                                } else {
                                    Toast.makeText(getContext(), "Ошибка удаления", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }).start();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void openDocument(String docId) {
        int index = docIds.indexOf(docId);
        if (index >= 0 && index < docFileNames.size() && index < docCollectionIds.size()) {
            String fileName = docFileNames.get(index);
            String collectionId = docCollectionIds.get(index);
            if (fileName == null || fileName.isEmpty()) {
                Toast.makeText(getContext(), "Имя файла не найдено", Toast.LENGTH_SHORT).show();
                return;
            }
            String baseUrl = PocketBaseClient.getBaseUrl();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            String url = baseUrl + "/api/files/" + collectionId + "/" + docId + "/" + fileName;
            Log.d("DocumentsList", "Opening URL: " + url);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } else {
            Toast.makeText(getContext(), "Документ не найден", Toast.LENGTH_SHORT).show();
        }
    }
}