package com.avtoforward.automaster.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.avtoforward.automaster.PocketBaseClient;
import com.avtoforward.automaster.R;
import com.avtoforward.automaster.models.ChatMessage;
import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;

import java.util.List;

public class ChatAdapter extends ArrayAdapter<ChatMessage> {

    private final Context context;
    private final List<ChatMessage> messages;
    private final String currentUserId;
    private final OnMessageActionListener listener;

    public interface OnMessageActionListener {
        void onEditMessage(ChatMessage message);
        void onDeleteMessage(ChatMessage message);
    }

    public ChatAdapter(Context context, List<ChatMessage> messages, String currentUserId, OnMessageActionListener listener) {
        super(context, 0, messages);
        this.context = context;
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_chat_message, parent, false);
        }

        ChatMessage message = messages.get(position);

        TextView textAuthor = convertView.findViewById(R.id.textAuthor);
        TextView textMessage = convertView.findViewById(R.id.textMessage);
        TextView textTime = convertView.findViewById(R.id.textTime);
        PhotoView imageAttachment = convertView.findViewById(R.id.imageAttachment);

        // Имя автора + отметка "Вы"
        String author = message.getAuthorName();
        if (message.getAuthorId().equals(currentUserId)) {
            author = author + " (Вы)";
        }
        textAuthor.setText(author);

        // Время — просто показываем строку, которую передали
        if (message.getCreated() != null && !message.getCreated().isEmpty()) {
            textTime.setText(message.getCreated());
            textTime.setVisibility(View.VISIBLE);
        } else {
            textTime.setVisibility(View.GONE);
        }

        // Текст
        if (message.getMessageText() != null && !message.getMessageText().isEmpty()) {
            textMessage.setVisibility(View.VISIBLE);
            textMessage.setText(message.getMessageText());
        } else {
            textMessage.setVisibility(View.GONE);
        }

        // Фото
        if (message.getImageUrls() != null && !message.getImageUrls().isEmpty()) {
            imageAttachment.setVisibility(View.VISIBLE);
            String imageUrl = PocketBaseClient.getBaseUrl() + "/api/files/forum_messages/" + message.getId() + "/" + message.getImageUrls().get(0);
            Glide.with(context).load(imageUrl).into(imageAttachment);
        } else {
            imageAttachment.setVisibility(View.GONE);
        }

        // Долгое нажатие — показываем диалог редактирования/удаления (только для своих сообщений)
        if (message.getAuthorId().equals(currentUserId)) {
            convertView.setOnLongClickListener(v -> {
                if (listener != null) {
                    new android.app.AlertDialog.Builder(context)
                            .setTitle("Действие с сообщением")
                            .setItems(new String[]{"Редактировать", "Удалить"}, (dialog, which) -> {
                                if (which == 0) {
                                    listener.onEditMessage(message);
                                } else {
                                    listener.onDeleteMessage(message);
                                }
                            })
                            .show();
                }
                return true;
            });
        } else {
            convertView.setOnLongClickListener(null);
        }

        return convertView;
    }
}