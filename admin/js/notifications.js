/**
 * Enterprise Notifications Module
 * HisabNiben Admin Panel
 */

const TYPE_TAG_MAP = {
    'announcement': '📢 সাধারণ ঘোষণা',
    'feature_update': '⭐ ফিচার আপডেট',
    'subscription': '💳 সাবস্ক্রিপশন',
    'promotional': '🎉 অফার / প্রোমোশন',
    'tutorial': '🎥 ভিডিও টিউটোরিয়াল',
    'maintenance': '🛠️ মেইনটেন্যান্স',
    'security_alert': '🔒 নিরাপত্তা এলার্ট'
};

const NotificationsModule = {
    usersData: [],
    notificationsHistory: [],

    async init() {
        await this.loadUsers();
        await this.loadHistory();
        this.initEventListeners();
        this.populateUserSelect();
        this.updateAudiencePreview();
        this.updatePreview();
    },

    async refresh() {
        await this.loadUsers();
        await this.loadHistory();
        if (typeof App !== 'undefined' && App.Toast) App.Toast.success('Notifications history refreshed.');
    },

    async loadUsers() {
        try {
            const snap = await getCollection(APP_CONFIG.collections.users || 'users').get();
            this.usersData = snap.docs.map(doc => ({
                id: doc.id,
                ...doc.data()
            }));
            this.updateKPIs();
        } catch (error) {
            console.error('Error loading users:', error);
        }
    },

    async loadHistory() {
        const container = document.getElementById('notification-history');
        if (!container) return;

        try {
            const historyRef = getCollection('notification_history');
            if (historyRef) {
                historyRef.orderBy('createdAt', 'desc').limit(50).onSnapshot((snapshot) => {
                    this.notificationsHistory = snapshot.docs.map(doc => ({
                        id: doc.id,
                        ...doc.data()
                    }));

                    this.updateKPIs();
                    this.renderHistory();

                    const subtitle = document.getElementById('sync-timestamp-subtitle');
                    if (subtitle) {
                        subtitle.textContent = `Broadcast push notifications, announcements, in-app alerts, and promotional banners to rice business app users. • Synced ${new Date().toLocaleTimeString()}`;
                    }
                });
            }
        } catch (error) {
            console.error('Error loading notification history:', error);
        }
    },

    updateKPIs() {
        const totalSent = this.notificationsHistory.length;

        const todayStart = new Date(); todayStart.setHours(0,0,0,0);
        const sentToday = this.notificationsHistory.filter(n => {
            const ts = Utils.parseTimestamp(n.createdAt);
            return ts >= todayStart.getTime();
        }).length;

        const reach = this.usersData.length;

        this.setElemText('kpi-total-sent', totalSent);
        this.setElemText('kpi-sent-today', sentToday);
        this.setElemText('kpi-total-reach', reach);
        this.setElemText('total-sent', totalSent);
    },

    setElemText(id, val) {
        const el = document.getElementById(id);
        if (el) el.textContent = val;
    },

    renderHistory() {
        const container = document.getElementById('notification-history');
        if (!container) return;

        if (this.notificationsHistory.length === 0) {
            container.innerHTML = `
                <div class="history-empty" style="text-align: center; padding: 40px; color: var(--gray-400);">
                    <i class="fas fa-bell-slash fa-2x" style="margin-bottom: 10px; color: var(--gray-300);"></i>
                    <h5 style="font-size: 15px; font-weight: 700; color: var(--dark); margin-bottom: 4px;">কোনো নোটিফিকেশন হিস্ট্রি নেই</h5>
                    <p style="font-size: 13px; color: var(--gray-500);">পূর্বে পাঠানো নোটিফিকেশনসমূহ এখানে রিয়েল-টাইমে প্রদর্শিত হবে।</p>
                </div>
            `;
            return;
        }

        container.innerHTML = this.notificationsHistory.map(item => {
            const ts = Utils.parseTimestamp(item.createdAt);
            const dateFormatted = ts > 0 ? Utils.formatDate(ts) : '-';
            const typeLabel = TYPE_TAG_MAP[item.type] || item.type || 'ঘোষণা';
            const countText = item.userCount ? `${item.userCount} recipients` : 'All users';
            const imgHtml = item.imageUrl ? `<div style="margin-top: 8px;"><img src="${item.imageUrl}" style="max-height: 100px; border-radius: 8px; object-fit: cover;"></div>` : '';

            return `
                <div class="history-item">
                    <div class="history-item-header">
                        <span class="badge" style="background:rgba(245,73,39,0.12); color:var(--primary); font-size:11px; font-weight:700; padding:3px 8px;">${Utils.escapeHtml(typeLabel)}</span>
                        <span style="font-size: 11px; color: var(--gray-400);"><i class="fas fa-clock"></i> ${dateFormatted}</span>
                    </div>
                    <div class="history-item-title">${Utils.escapeHtml(item.title || 'Untitled')}</div>
                    <div class="history-item-msg">${Utils.escapeHtml(item.message || '')}</div>
                    ${imgHtml}
                    <div class="history-item-footer">
                        <span><i class="fas fa-users" style="color: var(--primary);"></i> ${countText}</span>
                        <div style="display:flex; gap:6px; align-items:center;">
                            <span style="color: #10B981; font-weight: 700;"><i class="fas fa-check-circle"></i> Sent</span>
                            <button type="button" class="btn btn-sm users-btn-outline" style="padding:2px 6px; font-size:10px;" onclick="NotificationsModule.duplicateCampaign('${item.id}')" title="Duplicate Campaign">
                                <i class="fas fa-copy"></i> Duplicate
                            </button>
                            <button type="button" class="btn btn-sm users-btn-outline" style="padding:2px 6px; font-size:10px; color:var(--danger);" onclick="NotificationsModule.deleteNotification('${item.id}')" title="Delete Notification">
                                <i class="fas fa-trash-alt"></i> Delete
                            </button>
                        </div>
                    </div>
                </div>
            `;
        }).join('');
    },

    /**
     * Delete a sent notification from history and Firestore
     */
    async deleteNotification(id) {
        if (!confirm('Are you sure you want to delete this sent notification from history?')) return;

        try {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.show();

            // 1. Delete from notification_history
            await getDocument('notification_history', id).delete();

            // 2. Delete from notifications collection if document exists
            try {
                await getDocument('notifications', id).delete();
            } catch (err) { }

            if (typeof App !== 'undefined' && App.Toast) App.Toast.success('Notification deleted from history.');
        } catch (error) {
            console.error('Error deleting notification:', error);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.error('Failed to delete notification.');
        } finally {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.hide();
        }
    },

    /**
     * Copy an existing campaign into composer fields without sending
     */
    duplicateCampaign(id) {
        const item = this.notificationsHistory.find(n => n.id === id);
        if (!item) return;

        document.getElementById('notification-title').value = item.title || '';
        document.getElementById('notification-message').value = item.message || '';
        document.getElementById('notification-type').value = item.type || 'announcement';
        document.getElementById('notification-priority').value = item.priority || 'normal';
        document.getElementById('notification-action').value = item.actionType || 'NONE';
        document.getElementById('notification-cta-text').value = item.ctaText || '';
        document.getElementById('notification-deeplink').value = item.deepLink || item.actionTarget || '';
        document.getElementById('notification-image-url').value = item.imageUrl || '';

        this.updatePreview();
        if (typeof App !== 'undefined' && App.Toast) App.Toast.info(`Copied campaign "${item.title}" into composer.`);
        document.getElementById('notification-title').focus();
    },

    populateUserSelect() {
        const select = document.getElementById('specific-user');
        if (!select) return;

        select.innerHTML = '<option value="">গ্রাহক নির্বাচন করুন (Select User)</option>';
        this.usersData.forEach(user => {
            const option = document.createElement('option');
            option.value = user.id;
            option.textContent = `${user.name || 'Unknown User'} - ${user.storeName || user.businessName || 'Store'} (${user.phone || user.email || 'N/A'})`;
            select.appendChild(option);
        });
    },

    updateAudiencePreview() {
        const targetRadio = document.querySelector('input[name="target"]:checked');
        if (!targetRadio) return;
        const target = targetRadio.value;

        const specDiv = document.getElementById('specific-user-div');
        if (specDiv) specDiv.style.display = (target === 'specific') ? 'block' : 'none';

        let count = 0;
        const now = Date.now();

        switch (target) {
            case 'all':
                count = this.usersData.length;
                break;
            case 'premium':
                count = this.usersData.filter(u => u.isPremium || u.premium || u.subscriptionStatus === 'ACTIVE').length;
                break;
            case 'trial':
                count = this.usersData.filter(u => u.onTrial || (u.trialEnd && u.trialEnd > now) || u.subscriptionStatus === 'TRIAL').length;
                break;
            case 'expired':
                count = this.usersData.filter(u => !u.isPremium && u.subscriptionStatus !== 'ACTIVE' && (!u.trialEnd || u.trialEnd <= now)).length;
                break;
            case 'specific':
                count = 1;
                break;
        }

        const countEl = document.getElementById('target-count');
        if (countEl) countEl.textContent = count;
    },

    updatePreview() {
        const titleInput = document.getElementById('notification-title');
        const msgInput = document.getElementById('notification-message');
        const imgInput = document.getElementById('notification-image-url');
        const ctaInput = document.getElementById('notification-cta-text');
        const typeSelect = document.getElementById('notification-type');

        const titlePrev = document.getElementById('preview-title');
        const msgPrev = document.getElementById('preview-message');
        const imgPrev = document.getElementById('preview-image');
        const ctaPrev = document.getElementById('preview-cta-btn');
        const charCurrent = document.getElementById('char-current');
        const categoryTag = document.getElementById('preview-category-tag');

        if (titlePrev) titlePrev.textContent = (titleInput && titleInput.value.trim()) ? titleInput.value.trim() : 'নোটিফিকেশন শিরোনাম';
        if (msgPrev) msgPrev.textContent = (msgInput && msgInput.value.trim()) ? msgInput.value.trim() : 'আপনার নোটিফিকেশনের লেখাগুলো অ্যান্ড্রয়েড ফোনে যেভাবে দেখাবে...';
        if (charCurrent && msgInput) charCurrent.textContent = msgInput.value.length;

        if (categoryTag && typeSelect) {
            categoryTag.textContent = TYPE_TAG_MAP[typeSelect.value] || 'সাধারণ ঘোষণা';
        }

        if (imgPrev && imgInput) {
            if (imgInput.value.trim()) {
                imgPrev.src = imgInput.value.trim();
                imgPrev.style.display = 'block';
            } else {
                imgPrev.style.display = 'none';
            }
        }

        if (ctaPrev && ctaInput) {
            if (ctaInput.value.trim()) {
                ctaPrev.textContent = ctaInput.value.trim();
                ctaPrev.style.display = 'inline-block';
            } else {
                ctaPrev.style.display = 'none';
            }
        }
    },

    initEventListeners() {
        const form = document.getElementById('notification-form');
        if (form) {
            form.addEventListener('submit', (e) => {
                e.preventDefault();
                this.sendNotification(e);
            });
        }
    },

    /**
     * Send Push Notification (With Confirmation Warning & Double-Click Protection)
     */
    async sendNotification(event) {
        if (event) event.preventDefault();

        const title = document.getElementById('notification-title').value.trim();
        const message = document.getElementById('notification-message').value.trim();
        const type = document.getElementById('notification-type').value;
        const priority = document.getElementById('notification-priority').value;
        const actionType = document.getElementById('notification-action').value;
        const ctaText = document.getElementById('notification-cta-text').value.trim();
        const deepLink = document.getElementById('notification-deeplink').value.trim();
        const imageUrl = document.getElementById('notification-image-url').value.trim();
        
        const targetRadio = document.querySelector('input[name="target"]:checked');
        const target = targetRadio ? targetRadio.value : 'all';

        if (!title || !message) {
            alert('Please enter both Title and Message.');
            return;
        }

        let targetUsers = [];
        if (target === 'specific') {
            const userId = document.getElementById('specific-user').value;
            if (!userId) {
                alert('Please select a target user.');
                return;
            }
            targetUsers = [userId];
        }

        const countEl = document.getElementById('target-count');
        const estCount = countEl ? parseInt(countEl.textContent) || 0 : 0;

        let warning = '';
        if (priority === 'urgent') warning += '\n⚠️ Warning: Urgent Priority badge attached!';
        if (estCount > 50) warning += `\n📢 Broadcast size: ${estCount} users will receive this push notification.`;

        if (!confirm(`Send push notification "${title}" to target audience?${warning}`)) return;

        try {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.show();

            const now = Date.now();
            const notifData = {
                title,
                message,
                type,
                priority,
                actionType: actionType !== 'NONE' ? actionType : null,
                ctaText: ctaText || null,
                deepLink: deepLink || null,
                actionTarget: deepLink || null,
                imageUrl: imageUrl || null,
                target,
                targetUserIds: targetUsers,
                createdAt: now,
                sentBy: 'Super Admin'
            };

            // 1. Write to main Firestore `notifications` collection
            await getCollection('notifications').add(notifData);

            // 2. Write to `notification_history`
            await getCollection('notification_history').add({
                ...notifData,
                userCount: target === 'all' ? this.usersData.length : (targetUsers.length || estCount)
            });

            // 3. Optional PHP FCM Webhook dispatch
            try {
                await fetch('send-notification.php', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        title,
                        message,
                        target,
                        imageUrl,
                        userId: targetUsers[0] || null
                    })
                });
            } catch (err) { }

            if (typeof App !== 'undefined' && App.Toast) App.Toast.success('Push notification broadcasted successfully!');

            document.getElementById('notification-form').reset();
            document.getElementById('target-all').checked = true;
            this.updateAudiencePreview();
            this.updatePreview();
        } catch (error) {
            console.error('Error sending notification:', error);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.error('Failed to send notification: ' + error.message);
        } finally {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.hide();
        }
    }
};

function updatePreview() {
    NotificationsModule.updatePreview();
}

function updateAudiencePreview() {
    NotificationsModule.updateAudiencePreview();
}
