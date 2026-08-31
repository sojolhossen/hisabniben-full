package com.sajoldev.hisabniben.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class TutorialVideo implements Serializable {

    public static final String TYPE_YOUTUBE = "youtube";
    public static final String TYPE_FACEBOOK = "facebook";
    public static final String TYPE_DIRECT = "direct";
    public static final String TYPE_EXTERNAL = "external";

    public static final String CAT_ALL = "all";
    public static final String CAT_GETTING_STARTED = "getting_started";
    public static final String CAT_SALES = "sales";
    public static final String CAT_PURCHASE = "purchase";
    public static final String CAT_STOCK = "stock";
    public static final String CAT_CUSTOMER = "customer";
    public static final String CAT_SUPPLIER = "supplier";
    public static final String CAT_WALLET = "wallet";
    public static final String CAT_EXPENSE = "expense";
    public static final String CAT_REPORTS = "reports";
    public static final String CAT_SMS = "sms";
    public static final String CAT_SUBSCRIPTION = "subscription";
    public static final String CAT_SETTINGS = "settings";
    public static final String CAT_OTHER = "other";

    private String id;
    private String title;
    private String description;
    private String category;
    private String videoType;
    private String videoUrl;
    private String thumbnailUrl;
    private String duration;
    private boolean isPublished;
    private int sortOrder;
    private long createdAt;
    private long updatedAt;
    private String createdBy;

    public TutorialVideo() {
        this.isPublished = true;
        this.sortOrder = 1;
        this.category = CAT_GETTING_STARTED;
        this.videoType = TYPE_YOUTUBE;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public TutorialVideo(String id, String title, String description, String category, String videoType, String videoUrl, String thumbnailUrl, String duration, boolean isPublished, int sortOrder) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.videoType = videoType;
        this.videoUrl = videoUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.duration = duration;
        this.isPublished = isPublished;
        this.sortOrder = sortOrder;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getVideoType() { return videoType; }
    public void setVideoType(String videoType) { this.videoType = videoType; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    @PropertyName("isPublished")
    public boolean isPublished() { return isPublished; }

    @PropertyName("isPublished")
    public void setPublished(boolean published) { isPublished = published; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Object createdAt) {
        if (createdAt instanceof com.google.firebase.Timestamp) {
            this.createdAt = ((com.google.firebase.Timestamp) createdAt).toDate().getTime();
        } else if (createdAt instanceof Number) {
            this.createdAt = ((Number) createdAt).longValue();
        }
    }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Object updatedAt) {
        if (updatedAt instanceof com.google.firebase.Timestamp) {
            this.updatedAt = ((com.google.firebase.Timestamp) updatedAt).toDate().getTime();
        } else if (updatedAt instanceof Number) {
            this.updatedAt = ((Number) updatedAt).longValue();
        }
    }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    @Exclude
    public String getCategoryLabelBangla() {
        if (category == null) return "অন্যান্য";
        switch (category.toLowerCase()) {
            case CAT_GETTING_STARTED: return "শুরু করুন";
            case CAT_SALES: return "চাল বিক্রি";
            case CAT_PURCHASE: return "চাল ক্রয়";
            case CAT_STOCK: return "স্টক";
            case CAT_CUSTOMER: return "কাস্টমার";
            case CAT_SUPPLIER: return "মহাজন";
            case CAT_WALLET: return "ওয়ালেট";
            case CAT_EXPENSE: return "খরচ";
            case CAT_REPORTS: return "রিপোর্ট";
            case CAT_SMS: return "SMS";
            case CAT_SUBSCRIPTION: return "সাবস্ক্রিপশন";
            case CAT_SETTINGS: return "সেটিংস";
            case CAT_OTHER: return "অন্যান্য";
            default: return category;
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("description", description);
        map.put("category", category);
        map.put("videoType", videoType);
        map.put("videoUrl", videoUrl);
        map.put("thumbnailUrl", thumbnailUrl);
        map.put("duration", duration);
        map.put("isPublished", isPublished);
        map.put("sortOrder", sortOrder);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        map.put("createdBy", createdBy);
        return map;
    }
}
