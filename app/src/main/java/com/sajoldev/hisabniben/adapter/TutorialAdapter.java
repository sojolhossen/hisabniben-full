package com.sajoldev.hisabniben.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.TutorialVideo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TutorialAdapter extends RecyclerView.Adapter<TutorialAdapter.TutorialViewHolder> {

    public interface OnTutorialClickListener {
        void onTutorialClick(TutorialVideo video);
    }

    private final Context context;
    private final List<TutorialVideo> tutorialList;
    private final OnTutorialClickListener listener;

    public TutorialAdapter(Context context, List<TutorialVideo> tutorialList, OnTutorialClickListener listener) {
        this.context = context;
        this.tutorialList = tutorialList != null ? tutorialList : new ArrayList<>();
        this.listener = listener;
    }

    public void updateList(List<TutorialVideo> newList) {
        this.tutorialList.clear();
        if (newList != null) {
            this.tutorialList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TutorialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tutorial_video, parent, false);
        return new TutorialViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TutorialViewHolder holder, int position) {
        TutorialVideo video = tutorialList.get(position);
        holder.bind(video);
    }

    @Override
    public int getItemCount() {
        return tutorialList.size();
    }

    class TutorialViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivThumbnail;
        private final TextView tvCategoryBadge;
        private final TextView tvDurationBadge;
        private final TextView tvTitle;
        private final TextView tvDescription;

        public TutorialViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            tvCategoryBadge = itemView.findViewById(R.id.tvCategoryBadge);
            tvDurationBadge = itemView.findViewById(R.id.tvDurationBadge);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
        }

        public void bind(TutorialVideo video) {
            tvTitle.setText(video.getTitle() != null ? video.getTitle() : "");
            tvDescription.setText(video.getDescription() != null ? video.getDescription() : "");
            tvCategoryBadge.setText(video.getCategoryLabelBangla());

            if (video.getDuration() != null && !video.getDuration().trim().isEmpty()) {
                tvDurationBadge.setText(video.getDuration().trim());
                tvDurationBadge.setVisibility(View.VISIBLE);
            } else {
                tvDurationBadge.setVisibility(View.GONE);
            }

            String thumbUrl = video.getThumbnailUrl();
            if (thumbUrl == null || thumbUrl.trim().isEmpty()) {
                if (TutorialVideo.TYPE_YOUTUBE.equalsIgnoreCase(video.getVideoType())) {
                    String ytId = extractYouTubeId(video.getVideoUrl());
                    if (ytId != null) {
                        thumbUrl = "https://img.youtube.com/vi/" + ytId + "/hqdefault.jpg";
                    }
                }
            }

            if (thumbUrl != null && !thumbUrl.trim().isEmpty()) {
                Glide.with(context)
                        .load(thumbUrl)
                        .placeholder(R.drawable.bg_rounded_card)
                        .error(R.drawable.bg_rounded_card)
                        .into(ivThumbnail);
            } else {
                ivThumbnail.setImageResource(R.drawable.bg_rounded_card);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTutorialClick(video);
                }
            });
        }
    }

    public static String extractYouTubeId(String url) {
        if (url == null || url.trim().isEmpty()) return null;
        String pattern = "(?<=watch\\?v=|/videos/|embed\\/|shorts\\/|youtu\\.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed$%2F|youtu\\.be%2F|%2Fv%2F)[^#\\&\\?\\n]*";
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(url);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
}
