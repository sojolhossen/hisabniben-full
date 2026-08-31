package com.sajoldev.hisabniben.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.io.Serializable;

@IgnoreExtraProperties
public class Notification implements Serializable {
    private String id;
    private String title;
    private String message;
    private long createdAt;
    private String type; // announcement, feature_update, system, subscription, offer, business_tip
    private String priority; // normal, important, urgent
    private String actionType; // OPEN_SUBSCRIPTION, OPEN_SMS, OPEN_REPORTS, OPEN_SALES, OPEN_PURCHASE
    private String actionTarget;
    private String deepLink;
    private String imageUrl;
    private boolean read;
    private String ctaText;
    private boolean isPinned;
    private long expiresAt;
    private String target;

    public Notification() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Object createdAt) {
        if (createdAt instanceof Timestamp) {
            this.createdAt = ((Timestamp) createdAt).toDate().getTime();
        } else if (createdAt instanceof Long) {
            this.createdAt = (Long) createdAt;
        } else if (createdAt instanceof Number) {
            this.createdAt = ((Number) createdAt).longValue();
        }
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getActionTarget() { return actionTarget; }
    public void setActionTarget(String actionTarget) { this.actionTarget = actionTarget; }

    public String getDeepLink() { return deepLink; }
    public void setDeepLink(String deepLink) { this.deepLink = deepLink; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getCtaText() { return ctaText; }
    public void setCtaText(String ctaText) { this.ctaText = ctaText; }

    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Object expiresAt) {
        if (expiresAt instanceof Timestamp) {
            this.expiresAt = ((Timestamp) expiresAt).toDate().getTime();
        } else if (expiresAt instanceof Long) {
            this.expiresAt = (Long) expiresAt;
        } else if (expiresAt instanceof Number) {
            this.expiresAt = ((Number) expiresAt).longValue();
        }
    }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
}