/**
 * Master Enterprise Users & CRM Module
 * HisabNiben Admin Panel
 */

const UsersModule = {
    usersData: [],
    filteredUsers: [],
    paymentRequests: [],
    transactionsData: [],
    selectedUser: null,
    isGridView: false,
    activeTab: 'overview',
    filters: {
        search: '',
        status: 'all',
        sms: 'all',
        joined: 'all',
        expiry: 'all'
    },

    /**
     * Initialize Module
     */
    async init() {
        await this.loadUsers();
        this.initEventListeners();
        this.parseUrlParams();
    },

    /**
     * Parse URL Query Parameters (e.g. users.html?status=premium)
     */
    parseUrlParams() {
        const urlParams = new URLSearchParams(window.location.search);
        const statusParam = urlParams.get('status');
        if (statusParam) {
            this.filters.status = statusParam;
            const select = document.getElementById('filter-status');
            if (select) select.value = statusParam;
            this.applyFilters();
        }
    },

    /**
     * Load Users from Firestore (with Instant Session Caching)
     */
    async loadUsers(forceRefresh = false) {
        try {
            if (forceRefresh && typeof AppCache !== 'undefined') {
                AppCache.clear('users');
            }

            // 1. Try instant load from AppCache
            if (!forceRefresh && typeof AppCache !== 'undefined') {
                const cached = AppCache.get('users');
                if (cached && cached.usersData) {
                    console.log('⚡ Loading users data from AppCache...');
                    this.usersData = cached.usersData || [];
                    this.paymentRequests = cached.paymentRequests || [];
                    this.updateStats();
                    this.applyFilters();
                    // Fetch fresh in background
                    this.fetchFreshUsersData();
                    return;
                }
            }

            if (typeof App !== 'undefined' && App.Loader) App.Loader.show();
            await this.fetchFreshUsersData();

        } catch (error) {
            console.error('Error loading users:', error);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.error('Failed to load users');
        } finally {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.hide();
        }
    },

    async fetchFreshUsersData() {
        const usersRef = getCollection(APP_CONFIG.collections.users);
        let snapshot;
        try {
            snapshot = await usersRef.orderBy('createdAt', 'desc').get();
        } catch (orderErr) {
            console.warn('orderBy createdAt failed, fetching without order:', orderErr);
            snapshot = await usersRef.get();
        }

        this.usersData = snapshot.docs.map(doc => ({
            id: doc.id,
            ...doc.data()
        }));

        // Fetch payment requests for CRM context safely
        const prColName = APP_CONFIG.collections.payment_requests || APP_CONFIG.collections.paymentRequests || 'payment_requests';
        const prRef = getCollection(prColName);
        if (prRef) {
            try {
                const prSnap = await prRef.get();
                this.paymentRequests = prSnap.docs.map(doc => ({ id: doc.id, ...doc.data() }));
            } catch (prErr) {
                console.warn('Failed to load payment_requests for CRM:', prErr);
                this.paymentRequests = [];
            }
        } else {
            this.paymentRequests = [];
        }

        // Save to AppCache
        if (typeof AppCache !== 'undefined') {
            AppCache.set('users', {
                usersData: this.usersData,
                paymentRequests: this.paymentRequests
            });
        }

        this.updateStats();
        this.applyFilters();

        const subtitle = document.getElementById('sync-timestamp-subtitle');
        if (subtitle) {
            subtitle.textContent = `Manage all HisabNiben users, subscriptions, SMS balance and account activity. • Synced ${new Date().toLocaleTimeString()}`;
        }
    },

    /**
     * Refresh Users
     */
    async refresh() {
        await this.loadUsers(true);
        if (typeof App !== 'undefined' && App.Toast) App.Toast.success('Users list refreshed');
    },

    /**
     * Helper for Timestamp extraction
     */
    getDocTimestamp(field) {
        if (!field) return 0;
        if (typeof field === 'number') return field;
        if (field.seconds) return field.seconds * 1000;
        if (field.toDate) return field.toDate().getTime();
        return 0;
    },

    /**
     * Update 6 KPI Summary Cards
     */
    updateStats() {
        const now = Date.now();
        const total = this.usersData.length;
        const premium = this.usersData.filter(u => u.isPremium).length;
        const trial = this.usersData.filter(u => !u.isPremium && u.trialEnd && u.trialEnd > now).length;
        const expired = this.usersData.filter(u => !u.isPremium && (!u.trialEnd || u.trialEnd <= now)).length;
        const blocked = this.usersData.filter(u => u.isBlocked || u.disabled).length;
        const totalSMS = this.usersData.reduce((sum, u) => sum + (u.smsLimit || 0), 0);

        this.setElemText('total-users-count', total);
        this.setElemText('premium-users-count', premium);
        this.setElemText('trial-users-count', trial);
        this.setElemText('expired-users-count', expired);
        this.setElemText('blocked-users-count', blocked);
        this.setElemText('total-sms-count', totalSMS.toLocaleString());
    },

    setElemText(id, val) {
        const el = document.getElementById(id);
        if (el) el.textContent = val;
    },

    /**
     * Filter Engine
     */
    applyFilters() {
        const searchVal = this.filters.search.toLowerCase().trim();
        const now = Date.now();
        const startOfToday = new Date().setHours(0, 0, 0, 0);
        const sevenDaysMs = 7 * 24 * 60 * 60 * 1000;
        const threeDaysMs = 3 * 24 * 60 * 60 * 1000;

        this.filteredUsers = this.usersData.filter(u => {
            // Search
            if (searchVal) {
                const name = (u.name || '').toLowerCase();
                const phone = (u.phone || '').toLowerCase();
                const email = (u.email || '').toLowerCase();
                const store = (u.storeName || u.shopName || '').toLowerCase();
                const uid = (u.id || u.uid || '').toLowerCase();
                if (!name.includes(searchVal) && !phone.includes(searchVal) && !email.includes(searchVal) && !store.includes(searchVal) && !uid.includes(searchVal)) {
                    return false;
                }
            }

            // Subscription Status
            if (this.filters.status !== 'all') {
                if (this.filters.status === 'premium' && !u.isPremium) return false;
                if (this.filters.status === 'trial' && (u.isPremium || !u.trialEnd || u.trialEnd <= now)) return false;
                if (this.filters.status === 'expired' && (u.isPremium || (u.trialEnd && u.trialEnd > now))) return false;
                if (this.filters.status === 'blocked' && (!u.isBlocked && !u.disabled)) return false;
            }

            // SMS Balance
            if (this.filters.sms !== 'all') {
                const sms = u.smsLimit || 0;
                if (this.filters.sms === 'has_sms' && sms <= 0) return false;
                if (this.filters.sms === 'zero_sms' && sms !== 0) return false;
                if (this.filters.sms === 'low_sms' && sms >= 50) return false;
            }

            // Joined Date
            if (this.filters.joined !== 'all') {
                const created = this.getDocTimestamp(u.createdAt);
                if (this.filters.joined === 'today' && created < startOfToday) return false;
                if (this.filters.joined === '7d' && created < now - sevenDaysMs) return false;
                if (this.filters.joined === '30d' && created < now - 30*24*60*60*1000) return false;
            }

            // Expiry Window
            if (this.filters.expiry !== 'all') {
                const exp = u.subscriptionExpiryDate || u.trialEnd || 0;
                if (this.filters.expiry === 'exp_today' && (exp < startOfToday || exp > startOfToday + 86400000)) return false;
                if (this.filters.expiry === 'exp_3d' && (exp < now || exp > now + threeDaysMs)) return false;
                if (this.filters.expiry === 'exp_7d' && (exp < now || exp > now + sevenDaysMs)) return false;
            }

            return true;
        });

        this.renderView();
        this.renderFilterChips();
    },

    applyStatusFilter(status) {
        this.filters.status = status;
        const select = document.getElementById('filter-status');
        if (select) select.value = status;
        this.applyFilters();
    },

    applySmsFilter(smsStatus) {
        this.filters.sms = smsStatus;
        const select = document.getElementById('filter-sms');
        if (select) select.value = smsStatus;
        this.applyFilters();
    },

    resetFilters() {
        this.filters = { search: '', status: 'all', sms: 'all', joined: 'all', expiry: 'all' };
        const searchInput = document.getElementById('search-users');
        if (searchInput) searchInput.value = '';
        ['filter-status', 'filter-sms', 'filter-joined', 'filter-expiry'].forEach(id => {
            const el = document.getElementById(id);
            if (el) el.value = 'all';
        });
        this.applyFilters();
    },

    toggleAdvancedFilters() {
        const panel = document.getElementById('advanced-filters-panel');
        const btnText = document.getElementById('btn-toggle-filters-text');
        if (!panel) return;

        if (panel.style.display === 'none') {
            panel.style.display = 'block';
            if (btnText) btnText.textContent = 'Hide Filters';
        } else {
            panel.style.display = 'none';
            if (btnText) btnText.textContent = 'Advanced Filters';
        }
    },

    renderFilterChips() {
        const container = document.getElementById('active-filter-chips');
        if (!container) return;

        const chips = [];
        if (this.filters.status !== 'all') chips.push(`Status: ${this.filters.status.toUpperCase()}`);
        if (this.filters.sms !== 'all') chips.push(`SMS: ${this.filters.sms}`);
        if (this.filters.joined !== 'all') chips.push(`Joined: ${this.filters.joined}`);
        if (this.filters.expiry !== 'all') chips.push(`Expiry: ${this.filters.expiry}`);

        container.innerHTML = chips.map(c => `
            <span class="badge" style="background:var(--primary-light); color:var(--primary); padding:4px 10px; border-radius:12px; font-weight:700; font-size:11px;">
                ${c}
            </span>
        `).join('');
    },

    /**
     * Render View (Table vs Grid)
     */
    renderView() {
        this.setElemText('showing-count', `Showing ${this.filteredUsers.length} of ${this.usersData.length} users`);

        if (this.isGridView) {
            this.renderGrid();
        } else {
            this.renderTable();
        }
    },

    toggleView() {
        this.isGridView = !this.isGridView;
        const text = document.getElementById('view-toggle-text');
        const icon = document.getElementById('view-toggle-icon');
        const tableCont = document.getElementById('table-view-container');
        const gridCont = document.getElementById('grid-view-container');

        if (this.isGridView) {
            if (text) text.textContent = 'Table view';
            if (icon) icon.className = 'fas fa-list';
            if (tableCont) tableCont.style.display = 'none';
            if (gridCont) gridCont.style.display = 'grid';
        } else {
            if (text) text.textContent = 'Grid view';
            if (icon) icon.className = 'fas fa-th';
            if (tableCont) tableCont.style.display = 'block';
            if (gridCont) gridCont.style.display = 'none';
        }
        this.renderView();
    },

    /**
     * Render Data Table
     */
    renderTable() {
        const tbody = document.getElementById('users-tbody');
        if (!tbody) return;

        if (this.filteredUsers.length === 0) {
            tbody.innerHTML = `<tr><td colspan="9" style="text-align:center; padding:30px; color:var(--gray-400);">No users match the current search/filter.</td></tr>`;
            return;
        }

        const now = Date.now();
        tbody.innerHTML = this.filteredUsers.map(u => {
            const initial = (u.name || u.phone || 'U').charAt(0).toUpperCase();
            const createdDate = u.createdAt ? new Date(this.getDocTimestamp(u.createdAt)).toLocaleDateString() : 'N/A';
            
            let statusBadge = '<span class="status-badge" style="background:var(--gray-100); color:var(--gray-500);">FREE</span>';
            let remainingText = '-';

            if (u.isBlocked || u.disabled) {
                statusBadge = '<span class="status-badge rejected">BLOCKED</span>';
            } else if (u.isPremium) {
                statusBadge = '<span class="status-badge approved">PREMIUM</span>';
                if (u.subscriptionExpiryDate) {
                    const days = Math.ceil((u.subscriptionExpiryDate - now) / (1000*60*60*24));
                    remainingText = days >= 999 ? 'Life-Time' : (days > 0 ? `${days} days left` : 'Expired today');
                } else {
                    remainingText = 'Life-Time';
                }
            } else if (u.trialEnd && u.trialEnd > now) {
                statusBadge = '<span class="status-badge pending">TRIAL</span>';
                const days = Math.ceil((u.trialEnd - now) / (1000*60*60*24));
                remainingText = `${days} trial day(s)`;
            } else {
                statusBadge = '<span class="status-badge rejected">EXPIRED</span>';
                remainingText = 'Trial expired';
            }

            return `
                <tr>
                    <td><input type="checkbox" class="user-select-cb" value="${u.id}"></td>
                    <td>
                        <div style="display:flex; align-items:center; gap:10px; cursor:pointer;" onclick="UsersModule.openCrmDrawer('${u.id}')">
                            <div style="width:34px; height:34px; border-radius:50%; background:var(--primary); color:white; font-weight:700; display:flex; align-items:center; justify-content:center; font-size:13px;">${initial}</div>
                            <div>
                                <div style="font-weight:700; color:var(--dark);">${Utils.escapeHtml(u.name || 'User')}</div>
                                <div style="font-size:11px; color:var(--gray-400);">${Utils.escapeHtml(u.email || u.id)}</div>
                            </div>
                        </div>
                    </td>
                    <td><strong>${Utils.escapeHtml(u.storeName || u.shopName || '-')}</strong></td>
                    <td><code>${Utils.escapeHtml(u.phone || '-')}</code></td>
                    <td>${statusBadge}</td>
                    <td><strong style="color:#3B82F6;">${u.smsLimit || 0} SMS</strong></td>
                    <td>${remainingText}</td>
                    <td>${createdDate}</td>
                    <td>
                        <div style="display:flex; gap:4px; align-items:center;">
                            <button class="btn btn-sm users-btn-primary" onclick="UsersModule.openCrmDrawer('${u.id}')" style="padding:4px 8px; font-size:11px;" title="View CRM">
                                <i class="fas fa-eye"></i> CRM
                            </button>
                            <button class="btn btn-sm" onclick="UsersModule.openDeleteUserModal('${u.id}')" style="padding:4px 8px; font-size:11px; background:#EF4444; color:white; border:none;" title="Delete User & All Data">
                                <i class="fas fa-trash-alt"></i>
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    },

    /**
     * Render Grid Card View
     */
    renderGrid() {
        const container = document.getElementById('grid-view-container');
        if (!container) return;

        if (this.filteredUsers.length === 0) {
            container.innerHTML = `<div style="grid-column: 1 / -1; text-align:center; padding:40px; color:var(--gray-400);">No users match the current search/filter.</div>`;
            return;
        }

        const now = Date.now();
        container.innerHTML = this.filteredUsers.map(u => {
            const initial = (u.name || u.phone || 'U').charAt(0).toUpperCase();
            return `
                <div class="user-grid-card">
                    <div class="user-grid-header">
                        <div style="width:44px; height:44px; border-radius:50%; background:var(--primary); color:white; font-weight:800; display:flex; align-items:center; justify-content:center; font-size:16px;">${initial}</div>
                        <div>
                            <div style="font-weight:800; font-size:16px; color:var(--dark);">${Utils.escapeHtml(u.name || 'User')}</div>
                            <div style="font-size:12px; color:var(--gray-500);">${Utils.escapeHtml(u.storeName || u.shopName || 'No store name')}</div>
                        </div>
                    </div>
                    <div style="font-size:13px; margin-bottom:12px;">
                        <div><i class="fas fa-phone" style="color:var(--gray-400); width:18px;"></i> <code>${Utils.escapeHtml(u.phone || '-')}</code></div>
                        <div><i class="fas fa-comment-dots" style="color:#3B82F6; width:18px;"></i> <strong>${u.smsLimit || 0} SMS credits</strong></div>
                    </div>
                    <div class="user-grid-footer">
                        <span class="status-badge ${u.isPremium ? 'approved' : 'pending'}">${u.isPremium ? 'PREMIUM' : 'TRIAL'}</span>
                        <div style="display:flex; gap:4px;">
                            <button class="btn btn-sm users-btn-primary" onclick="UsersModule.openCrmDrawer('${u.id}')">View CRM</button>
                            <button class="btn btn-sm" onclick="UsersModule.openDeleteUserModal('${u.id}')" style="background:#EF4444; color:white; border:none;" title="Delete User & Data"><i class="fas fa-trash-alt"></i></button>
                        </div>
                    </div>
                </div>
            `;
        }).join('');
    },

    /**
     * Right-Side CRM Drawer Controls
     */
    async openCrmDrawer(userId) {
        this.selectedUser = this.usersData.find(u => u.id === userId);
        if (!this.selectedUser) return;

        const overlay = document.getElementById('crm-drawer-overlay');
        if (overlay) overlay.classList.add('active');

        const avatar = document.getElementById('drawer-user-avatar');
        if (avatar) avatar.textContent = (this.selectedUser.name || 'U').charAt(0).toUpperCase();

        this.setElemText('drawer-user-name', this.selectedUser.name || 'User Profile');
        this.setElemText('drawer-user-shop', `${this.selectedUser.storeName || this.selectedUser.shopName || 'Store'} • ${this.selectedUser.phone || ''}`);

        // Populate Overview Tab
        this.setElemText('crm-info-name', this.selectedUser.name || '-');
        this.setElemText('crm-info-phone', this.selectedUser.phone || '-');
        this.setElemText('crm-info-email', this.selectedUser.email || '-');
        this.setElemText('crm-info-store', this.selectedUser.storeName || this.selectedUser.shopName || '-');
        this.setElemText('crm-info-uid', this.selectedUser.id);
        this.setElemText('crm-info-joined', this.selectedUser.createdAt ? new Date(this.getDocTimestamp(this.selectedUser.createdAt)).toLocaleDateString() : '-');

        // Populate Subscription Tab
        const now = Date.now();
        const isPrem = this.selectedUser.isPremium;
        this.setElemText('crm-sub-package-name', this.selectedUser.subscriptionPackageName || (isPrem ? 'Premium Package' : 'Free Trial'));
        const expDate = this.selectedUser.subscriptionExpiryDate ? new Date(this.selectedUser.subscriptionExpiryDate).toLocaleDateString() : (this.selectedUser.trialEnd ? new Date(this.selectedUser.trialEnd).toLocaleDateString() : '-');
        this.setElemText('crm-sub-expiry-date', expDate);

        const days = isPrem && this.selectedUser.subscriptionExpiryDate ? Math.ceil((this.selectedUser.subscriptionExpiryDate - now) / (1000*60*60*24)) : (this.selectedUser.trialEnd ? Math.ceil((this.selectedUser.trialEnd - now) / (1000*60*60*24)) : 0);
        this.setElemText('crm-sub-remaining-days', days >= 999 ? 'Life-Time' : `${days} day(s)`);

        this.setElemText('crm-sms-balance-display', `${this.selectedUser.smsLimit || 0} SMS`);

        // Activate Overview Tab IMMEDIATELY
        this.switchCrmTab('overview');

        // Load CRM Insights & Payment History in background
        this.loadCrmBusinessInsights(userId);
        this.loadUserPaymentHistory(userId);
    },

    closeCrmDrawer() {
        const overlay = document.getElementById('crm-drawer-overlay');
        if (overlay) overlay.classList.remove('active');
    },

    switchCrmTab(tabName, btnElem) {
        this.activeTab = tabName;
        document.querySelectorAll('.crm-tab-btn').forEach(btn => btn.classList.remove('active'));
        if (btnElem) btnElem.classList.add('active');

        document.querySelectorAll('.crm-tab-content').forEach(content => content.classList.remove('active'));
        const activeContent = document.getElementById(`crm-tab-${tabName}`);
        if (activeContent) activeContent.classList.add('active');
    },

    async loadCrmBusinessInsights(userId) {
        try {
            // 1. Fetch Customers for this user
            const custRef = getCollection('customers');
            let custCount = 0;
            let totalDues = 0;
            let customersList = [];

            if (custRef) {
                const custSnap = await custRef.where('userId', '==', userId).get();
                custCount = custSnap.size;
                custSnap.forEach(doc => {
                    const c = doc.data();
                    const baki = c.baki || c.currentDue || 0;
                    if (baki > 0) totalDues += baki;
                    customersList.push({ id: doc.id, ...c });
                });
            }

            this.setElemText('crm-stat-cust-count', `${custCount} জন কাস্টমার`);
            this.setElemText('crm-badge-cust-count', custCount);
            this.setElemText('crm-stat-dues', `৳${totalDues.toLocaleString()}`);

            // 2. Fetch Suppliers / Mahajon for this user
            const suppRef = getCollection('suppliers');
            let suppCount = 0;
            let totalPayables = 0;
            let suppliersList = [];

            if (suppRef) {
                const suppSnap = await suppRef.where('userId', '==', userId).get();
                suppCount = suppSnap.size;
                suppSnap.forEach(doc => {
                    const s = doc.data();
                    const payable = s.currentPayable || s.payable || 0;
                    if (payable > 0) totalPayables += payable;
                    suppliersList.push({ id: doc.id, ...s });
                });
            }

            this.setElemText('crm-stat-supp-count', `${suppCount} জন মহাজন`);
            this.setElemText('crm-badge-supp-count', suppCount);
            this.setElemText('crm-stat-payables', `৳${totalPayables.toLocaleString()}`);

            // 3. Fetch Products / Rice Stock for this user
            const prodRef = getCollection('products');
            let prodCount = 0;
            if (prodRef) {
                const prodSnap = await prodRef.where('userId', '==', userId).get();
                prodCount = prodSnap.size;
            }
            this.setElemText('crm-stat-stock', `${prodCount} প্রকার চাল`);

            // 4. Fetch Transactions for this user
            const txRef = getCollection('transactions');
            let totalSales = 0;
            let totalPurchases = 0;
            let txCount = 0;

            if (txRef) {
                const txSnap = await txRef.where('userId', '==', userId).get();
                txCount = txSnap.size;
                txSnap.forEach(doc => {
                    const t = doc.data();
                    const amount = t.amount || t.totalAmount || 0;
                    const typeStr = (t.type || t.transactionType || '').toUpperCase();
                    if (typeStr.includes('SALE') || typeStr.includes('SELL')) {
                        totalSales += amount;
                    } else if (typeStr.includes('PURCHASE') || typeStr.includes('BUY')) {
                        totalPurchases += amount;
                    }
                });
            }

            this.setElemText('crm-stat-sales', `৳${totalSales.toLocaleString()}`);
            this.setElemText('crm-stat-purchases', `৳${totalPurchases.toLocaleString()}`);
            this.setElemText('crm-stat-tx-count', `${txCount} টি লেনদেন`);

            // Render Customers & Suppliers lists in CRM drawer
            this.renderCrmCustomersList(customersList);
            this.renderCrmSuppliersList(suppliersList);

        } catch (e) {
            console.error('Error loading CRM insights:', e);
        }
    },

    renderCrmCustomersList(customers) {
        const container = document.getElementById('crm-customers-list');
        if (!container) return;

        if (!customers || customers.length === 0) {
            container.innerHTML = `<div style="padding:24px; text-align:center; color:var(--gray-400);"><i class="fas fa-user-slash fa-2x" style="margin-bottom:8px;"></i><p>এই ইউজারের কোনো কাস্টমার এন্টি নেই</p></div>`;
            return;
        }

        container.innerHTML = customers.map(c => {
            const baki = c.baki || c.currentDue || 0;
            const bakiBadge = baki > 0
                ? `<span style="color:var(--danger); font-weight:800;">৳${baki.toLocaleString()} বকেয়া</span>`
                : `<span style="color:#10B981; font-weight:700;">পরিশোধিত</span>`;

            return `
                <div style="background:var(--bg-card); border:1px solid var(--border-color); padding:12px 14px; border-radius:var(--radius); margin-bottom:10px; display:flex; justify-content:space-between; align-items:center;">
                    <div>
                        <div style="font-weight:700; color:var(--dark); font-size:13.5px;">${Utils.escapeHtml(c.name || 'Customer')}</div>
                        <div style="font-size:12px; color:var(--gray-500);"><i class="fas fa-phone" style="font-size:11px;"></i> ${Utils.escapeHtml(c.phone || c.mobile || 'N/A')}</div>
                    </div>
                    <div style="text-align:right;">
                        ${bakiBadge}
                    </div>
                </div>
            `;
        }).join('');
    },

    renderCrmSuppliersList(suppliers) {
        const container = document.getElementById('crm-suppliers-list');
        if (!container) return;

        if (!suppliers || suppliers.length === 0) {
            container.innerHTML = `<div style="padding:24px; text-align:center; color:var(--gray-400);"><i class="fas fa-truck-slash fa-2x" style="margin-bottom:8px;"></i><p>এই ইউজারের কোনো মহাজন / সাপ্লায়ার এন্টি নেই</p></div>`;
            return;
        }

        container.innerHTML = suppliers.map(s => {
            const payable = s.currentPayable || s.payable || 0;
            const payBadge = payable > 0
                ? `<span style="color:var(--warning); font-weight:800;">৳${payable.toLocaleString()} পাওনা</span>`
                : `<span style="color:#10B981; font-weight:700;">পরিশোধিত</span>`;

            return `
                <div style="background:var(--bg-card); border:1px solid var(--border-color); padding:12px 14px; border-radius:var(--radius); margin-bottom:10px; display:flex; justify-content:space-between; align-items:center;">
                    <div>
                        <div style="font-weight:700; color:var(--dark); font-size:13.5px;">${Utils.escapeHtml(s.name || s.companyName || 'Supplier')}</div>
                        <div style="font-size:12px; color:var(--gray-500);"><i class="fas fa-store" style="font-size:11px;"></i> ${Utils.escapeHtml(s.millName || s.storeName || 'Mill')} • <i class="fas fa-phone" style="font-size:11px;"></i> ${Utils.escapeHtml(s.phone || 'N/A')}</div>
                    </div>
                    <div style="text-align:right;">
                        ${payBadge}
                    </div>
                </div>
            `;
        }).join('');
    },

    loadUserPaymentHistory(userId) {
        const container = document.getElementById('crm-payments-list');
        if (!container) return;

        const userPr = this.paymentRequests.filter(pr => pr.userId === userId);
        if (userPr.length === 0) {
            container.innerHTML = `<div style="padding:16px; text-align:center; color:var(--gray-400);">No payment history for this user.</div>`;
            return;
        }

        container.innerHTML = userPr.map(pr => `
            <div style="background:var(--gray-50); border:1px solid var(--border-color); padding:12px; border-radius:var(--radius); margin-bottom:10px;">
                <div style="display:flex; justify-content:space-between; font-weight:700;">
                    <span>${Utils.escapeHtml(pr.packageName || 'Premium')}</span>
                    <span class="status-badge ${pr.status === 'approved' ? 'approved' : 'pending'}">${pr.status ? pr.status.toUpperCase() : 'PENDING'}</span>
                </div>
                <div style="margin-top:6px; color:var(--gray-500); font-size:12px;">
                    Amount: ৳${pr.amount || 0} • Method: ${pr.paymentMethod || 'Manual'} • TxID: <code>${pr.transactionId || 'N/A'}</code>
                </div>
            </div>
        `).join('');
    },

    /**
     * Subscription Modal Controls
     */
    openSubscriptionModal() {
        if (!this.selectedUser) return;
        const modal = document.getElementById('subscription-modal');
        const userIdElem = document.getElementById('sub-user-id');
        if (userIdElem) userIdElem.value = this.selectedUser.id;
        if (modal) {
            modal.classList.add('active');
            modal.classList.add('show');
        }
    },

    closeSubscriptionModal() {
        const modal = document.getElementById('subscription-modal');
        if (modal) {
            modal.classList.remove('active');
            modal.classList.remove('show');
        }
    },

    async saveSubscription() {
        const userId = document.getElementById('sub-user-id').value;
        const days = parseInt(document.getElementById('sub-package-select').value) || 30;
        const reason = document.getElementById('sub-reason').value;

        if (!userId) return;

        try {
            App.Loader.show();
            const now = Date.now();
            const currentExpiry = this.selectedUser.subscriptionExpiryDate || now;
            const baseTime = currentExpiry > now ? currentExpiry : now;
            const newExpiry = days >= 999 ? now + (36500 * 24 * 60 * 60 * 1000) : baseTime + (days * 24 * 60 * 60 * 1000);

            const pkgName = days >= 999 ? 'Life-Time Membership' : `${days} Days Premium`;

            const usersRef = getCollection(APP_CONFIG.collections.users);
            if (usersRef) {
                await usersRef.doc(userId).update({
                    isPremium: true,
                    premium: true,
                    subscriptionStatus: 'ACTIVE',
                    subscriptionExpiryDate: newExpiry,
                    subscriptionPackageName: pkgName,
                    packageName: pkgName,
                    updatedAt: Date.now()
                });
            }

            App.Toast.success('Subscription granted successfully!');
            this.closeSubscriptionModal();
            this.closeCrmDrawer();
            await this.loadUsers();
        } catch (e) {
            console.error('Error saving subscription:', e);
            App.Toast.error('Failed to grant subscription');
        } finally {
            App.Loader.hide();
        }
    },

    /**
     * SMS Modal Controls
     */
    openSmsModalFromDrawer() {
        if (!this.selectedUser) return;
        const modal = document.getElementById('update-sms-modal');
        const userIdElem = document.getElementById('sms-user-id');
        if (userIdElem) userIdElem.value = this.selectedUser.id;
        if (modal) {
            modal.classList.add('active');
            modal.classList.add('show');
        }
    },

    closeSmsModal() {
        const modal = document.getElementById('update-sms-modal');
        if (modal) {
            modal.classList.remove('active');
            modal.classList.remove('show');
        }
    },

    updateSmsLabels() {},

    async saveSmsBalance() {
        const userId = document.getElementById('sms-user-id').value;
        const actionType = document.getElementById('sms-action-type').value;
        const amount = parseInt(document.getElementById('sms-amount-input').value) || 0;

        if (!userId || amount <= 0) return;

        try {
            App.Loader.show();
            const currentBalance = this.selectedUser.smsLimit || 0;
            let newBalance = currentBalance;

            if (actionType === 'add') newBalance += amount;
            else if (actionType === 'set') newBalance = amount;
            else if (actionType === 'remove') newBalance = Math.max(0, currentBalance - amount);

            const usersRef = getCollection(APP_CONFIG.collections.users);
            if (usersRef) {
                await usersRef.doc(userId).update({
                    smsLimit: newBalance,
                    updatedAt: Date.now()
                });
            }

            App.Toast.success('SMS balance updated!');
            this.closeSmsModal();
            this.closeCrmDrawer();
            await this.loadUsers();
        } catch (e) {
            console.error('Error updating SMS:', e);
            App.Toast.error('Failed to update SMS');
        } finally {
            App.Loader.hide();
        }
    },

    /**
     * Edit User Modal Controls
     */
    openEditModalFromDrawer() {
        if (!this.selectedUser) return;
        const modal = document.getElementById('edit-user-modal');
        document.getElementById('edit-user-id').value = this.selectedUser.id;
        document.getElementById('edit-user-name').value = this.selectedUser.name || '';
        document.getElementById('edit-user-phone').value = this.selectedUser.phone || '';
        document.getElementById('edit-user-email').value = this.selectedUser.email || '';
        document.getElementById('edit-user-store').value = this.selectedUser.storeName || this.selectedUser.shopName || '';
        if (modal) {
            modal.classList.add('active');
            modal.classList.add('show');
        }
    },

    closeEditModal() {
        const modal = document.getElementById('edit-user-modal');
        if (modal) {
            modal.classList.remove('active');
            modal.classList.remove('show');
        }
    },

    /**
     * Ban / Unban User
     */
    async toggleBanFromDrawer() {
        if (!this.selectedUser) return;
        const isBanned = this.selectedUser.isBlocked || this.selectedUser.disabled;
        const confirmMsg = isBanned ? 'Are you sure you want to unban this user?' : 'Are you sure you want to ban this user?';
        
        if (!confirm(confirmMsg)) return;

        try {
            App.Loader.show();
            const usersRef = getCollection(APP_CONFIG.collections.users);
            if (usersRef) {
                await usersRef.doc(this.selectedUser.id).update({
                    isBlocked: !isBanned,
                    disabled: !isBanned,
                    updatedAt: Date.now()
                });
            }

            App.Toast.success(`User ${!isBanned ? 'banned' : 'unbanned'} successfully!`);
            this.closeCrmDrawer();
            await this.loadUsers();
        } catch (e) {
            console.error('Error updating ban status:', e);
            App.Toast.error('Failed to update user status');
        } finally {
            App.Loader.hide();
        }
    },

    /**
     * CSV Export Generator
     */
    exportCSV() {
        if (this.filteredUsers.length === 0) {
            alert('No users to export.');
            return;
        }

        let csv = 'User Name,Phone,Email,Store Name,Status,SMS Balance,Joined Date\n';
        this.filteredUsers.forEach(u => {
            const dateStr = u.createdAt ? new Date(this.getDocTimestamp(u.createdAt)).toISOString().slice(0,10) : '';
            const status = u.isPremium ? 'PREMIUM' : (u.trialEnd > Date.now() ? 'TRIAL' : 'EXPIRED');
            csv += `"${u.name || ''}","${u.phone || ''}","${u.email || ''}","${u.storeName || u.shopName || ''}","${status}",${u.smsLimit || 0},"${dateStr}"\n`;
        });

        const blob = new Blob([csv], { type: 'text/csv' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `HisabNiben_Users_Export_${new Date().toISOString().slice(0,10)}.csv`;
        a.click();
    },

    toggleSelectAll(cb) {
        document.querySelectorAll('.user-select-cb').forEach(c => c.checked = cb.checked);
    },

    initEventListeners() {
        const searchInput = document.getElementById('search-users');
        if (searchInput) {
            searchInput.addEventListener('input', Utils.debounce((e) => {
                this.filters.search = e.target.value;
                this.applyFilters();
            }, 300));
        }

        const editForm = document.getElementById('edit-user-form');
        if (editForm) {
            editForm.addEventListener('submit', async (e) => {
                e.preventDefault();
                const userId = document.getElementById('edit-user-id').value;
                const name = document.getElementById('edit-user-name').value;
                const phone = document.getElementById('edit-user-phone').value;
                const email = document.getElementById('edit-user-email').value;
                const storeName = document.getElementById('edit-user-store').value;

                try {
                    App.Loader.show();
                    const usersRef = getCollection(APP_CONFIG.collections.users);
                    if (usersRef) {
                        await usersRef.doc(userId).update({
                            name, phone, email, storeName, shopName: storeName, updatedAt: Date.now()
                        });
                    }
                    App.Toast.success('User updated successfully!');
                    this.closeEditModal();
                    this.closeCrmDrawer();
                    await this.loadUsers();
                } catch (err) {
                    console.error('Error updating user:', err);
                    App.Toast.error('Failed to update user');
                } finally {
                    App.Loader.hide();
                }
            });
        }
    },

    /**
     * Delete User Modal Controls
     */
    openDeleteUserModal(userId) {
        const user = this.usersData.find(u => u.id === userId);
        if (!user) return;
        this.selectedUser = user;

        const modal = document.getElementById('delete-user-modal');
        const inputId = document.getElementById('delete-user-id');
        const nameDisp = document.getElementById('delete-user-name-display');
        const phoneDisp = document.getElementById('delete-user-phone-display');
        const confirmInput = document.getElementById('delete-user-confirm-input');

        if (inputId) inputId.value = user.id;
        if (nameDisp) nameDisp.textContent = user.name || 'User Profile';
        if (phoneDisp) phoneDisp.textContent = user.phone || user.email || user.id;
        if (confirmInput) confirmInput.value = '';

        if (modal) {
            modal.classList.add('active');
            modal.classList.add('show');
        }
    },

    openDeleteUserModalFromDrawer() {
        if (!this.selectedUser) return;
        this.openDeleteUserModal(this.selectedUser.id);
    },

    closeDeleteUserModal() {
        const modal = document.getElementById('delete-user-modal');
        if (modal) {
            modal.classList.remove('active');
            modal.classList.remove('show');
        }
    },

    /**
     * Cascading Purge: Delete User Document & All Associated Collections Data
     */
    async confirmDeleteUserAndAllData() {
        const userId = document.getElementById('delete-user-id').value;
        const confirmInput = document.getElementById('delete-user-confirm-input').value.trim().toUpperCase();

        if (!userId) return;

        if (confirmInput !== 'DELETE') {
            if (typeof App !== 'undefined' && App.Toast) App.Toast.error('Please type DELETE in capital letters to confirm deletion.');
            return;
        }

        try {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.show();

            const collectionsToPurge = [
                'customers',
                'suppliers',
                'transactions',
                'products',
                'payment_requests',
                'purchases',
                'sms_history',
                'wallet_accounts'
            ];

            // 1. Purge matching documents in associated collections
            for (const colName of collectionsToPurge) {
                try {
                    const colRef = getCollection(colName);
                    if (colRef) {
                        const snap = await colRef.where('userId', '==', userId).get();
                        const deletePromises = snap.docs.map(doc => doc.ref.delete());
                        await Promise.all(deletePromises);
                    }
                } catch (colErr) {
                    console.warn(`Error purging collection ${colName} for user ${userId}:`, colErr);
                }
            }

            // 2. Delete main User document
            const usersRef = getCollection(APP_CONFIG.collections.users);
            if (usersRef) {
                await usersRef.doc(userId).delete();
            }

            // 3. Clear AppCache
            if (typeof AppCache !== 'undefined') {
                AppCache.clear('users');
                AppCache.clear('dashboard');
            }

            if (typeof App !== 'undefined' && App.Toast) App.Toast.success('User account and all associated data permanently deleted!');
            this.closeDeleteUserModal();
            this.closeCrmDrawer();
            await this.loadUsers(true);

        } catch (e) {
            console.error('Error deleting user and data:', e);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.error('Failed to delete user account');
        } finally {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.hide();
        }
    }
};
