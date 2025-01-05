package com.example.annotatex_mobile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.RadioButton;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.ViewHolder> {
    private final List<Language> languages;
    private final OnLanguageSelectedListener listener;

    public interface OnLanguageSelectedListener {
        void onLanguageSelected(String language);
    }

    public LanguageAdapter(List<Language> languages, OnLanguageSelectedListener listener) {
        this.languages = languages;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_language, parent, false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Language language = languages.get(position);
        holder.flagImage.setImageResource(language.getFlagResource());
        holder.languageName.setText(language.getName());
        
        holder.itemView.setOnClickListener(v -> 
            listener.onLanguageSelected(language.getName()));
    }

    @Override
    public int getItemCount() {
        return languages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView flagImage;
        TextView languageName;
        RadioButton radioButton;

        ViewHolder(View view) {
            super(view);
            flagImage = view.findViewById(R.id.flagImage);
            languageName = view.findViewById(R.id.languageName);
            radioButton = view.findViewById(R.id.radioButton);
        }
    }
} 