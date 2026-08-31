/**
 * Master Enterprise Purchase History & Revenue Control Center Module
 * HisabNiben Admin Panel
 */

const PurchaseHistoryModule = {
    purchasesData: [],
    filteredPurchases: [],
    usersData: [],
    selectedPurchase: null,
    isGridView: false,
    charts: {},
    filters: {
        search: '',
        status: 'all',
        type: 'all',
        method: 'all',
        date: 'all'
    },

    /**
     * Initialize Module
     */
    async init() {
        await Promise.all([
            this.loadPurchases(),
            this.loadUsers()
        ]);
        this.initEventListeners();
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
     * Load Purchases from both payment_requests and purchases collections
     */
    async loadPurchases() {
        try {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.show();

            const records = [];

            // 1. Load payment_requests
            const prRef = getCollection(APP_CONFIG.collections.payment_requests || 'payment_requests');
            if (prRef) {
                try {
                    const prSnap = await prRef.get();
                    prSnap.docs.forEach(doc => {
                        const d = doc.data();
                        records.push(this.normalizeRecord(doc.id, d, 'payment_request'));
                    });
                } catch (e) {
                    console.warn('Error reading payment_requests:', e);
                }
            }

            // 2. Load purchases collection if present
            const purchRef = getCollection('purchases');
            if (purchRef) {
                try {
                    const pSnap = await purchRef.get();
                    pSnap.docs.forEach(doc => {
                        const d = doc.data();
                        // Avoid duplicates if same ID exists
                        if (!records.some(r => r.id === doc.id)) {
                            records.push(this.normalizeRecord(doc.id, d, 'purchase'));
                        }
                    });
                } catch (e) {
                    console.warn('Error reading purchases collection:', e);
                }
            }

            // Sort newest first
            records.sort((a, b) => b.createdAt - a.createdAt);
            this.purchasesData = records;

            this.updateStats();
            this.initAnalyticsCharts();
            this.applyFilters();

            const subtitle = document.getElementById('sync-timestamp-subtitle');
            if (subtitle) {
                subtitle.textContent = `Monitor subscriptions, SMS purchases, payments and revenue. • Synced ${new Date().toLocaleTimeString()}`;
            }

        } catch (error) {
            console.error('Error loading purchases:', error);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.error('Failed to load purchases');
        } finally {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.hide();
        }
    },

    /**
     * Normalize Purchase Record
     */
    normalizeRecord(id, d, source) {
        const rawStatus = (d.status || 'pending').toLowerCase();
        let status = 'pending';
        if (rawStatus === 'approved' || rawStatus === 'completed' || rawStatus === 'success') status = 'completed';
        else if (rawStatus === 'rejected' || rawStatus === 'failed') status = 'failed';
        else if (rawStatus === 'cancelled') status = 'cancelled';

        const amount = d.amount || d.price || 0;
        const method = d.paymentMethod || d.method || 'Manual';
        const type = d.smsCount ? 'sms' : (d.packageId && d.packageId.includes('sms') ? 'sms' : 'subscription');
        const packageName = d.packageName || d.name || (type === 'sms' ? 'SMS Package' : 'Premium Package');

        return {
            id,
            raw: d,
            source,
            userId: d.userId || d.uid || '',
            userName: d.userName || d.name || 'User',
            phone: d.phone || d.phoneNumber || '',
            email: d.email || '',
            storeName: d.storeName || d.shopName || '',
            packageName,
            type,
            amount,
            paymentMethod: method,
            transactionId: d.transactionId || d.txId || 'N/A',
            status,
            createdAt: this.getDocTimestamp(d.createdAt || d.approvedAt || d.date),
            approvedAt: this.getDocTimestamp(d.approvedAt),
            expiryDate: d.subscriptionExpiryDate ? this.getDocTimestamp(d.subscriptionExpiryDate) : 0,
            duration: d.durationDays || d.duration || (packageName.includes('Life') ? 'Life-Time' : '30 Days')
        };
    },

    /**
     * Load Users for Lookup
     */
    async loadUsers() {
        try {
            const usersRef = getCollection(APP_CONFIG.collections.users);
            if (usersRef) {
                const snap = await usersRef.get();
                this.usersData = snap.docs.map(doc => ({ id: doc.id, ...doc.data() }));
            }
        } catch (e) {
            console.warn('Failed to load users for lookup:', e);
        }
    },

    /**
     * Refresh
     */
    async refresh() {
        await this.loadPurchases();
        if (typeof App !== 'undefined' && App.Toast) App.Toast.success('Purchase records refreshed');
    },

    /**
     * Update 6 KPI Summary Cards
     */
    updateStats() {
        const total = this.purchasesData.length;
        const completed = this.purchasesData.filter(p => p.status === 'completed');
        const pending = this.purchasesData.filter(p => p.status === 'pending');
        const failed = this.purchasesData.filter(p => p.status === 'failed');

        const totalRevenue = completed.reduce((sum, p) => sum + p.amount, 0);
        const subRevenue = completed.filter(p => p.type === 'subscription').reduce((sum, p) => sum + p.amount, 0);
        const smsRevenue = completed.filter(p => p.type === 'sms').reduce((sum, p) => sum + p.amount, 0);

        this.setElemText('total-purchases-count', total);
        this.setElemText('completed-count', completed.length);
        this.setElemText('pending-count', pending.length);
        this.setElemText('failed-count', failed.length);

        this.setElemText('sub-revenue-val', `৳${subRevenue.toLocaleString()}`);
        this.setElemText('sms-revenue-val', `৳${smsRevenue.toLocaleString()}`);
    },

    setElemText(id, val) {
        const el = document.getElementById(id);
        if (el) el.textContent = val;
    },

    /**
     * Render Revenue Analytics Charts (Chart.js)
     */
    initAnalyticsCharts() {
        const ctxTrend = document.getElementById('purchaseRevenueChart');
        if (ctxTrend) {
            if (this.charts.trend) this.charts.trend.destroy();

            const completed = this.purchasesData.filter(p => p.status === 'completed');
            const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
            const monthlyRev = new Array(12).fill(0);

            completed.forEach(p => {
                if (p.createdAt > 0) {
                    const m = new Date(p.createdAt).getMonth();
                    if (!isNaN(m)) monthlyRev[m] += p.amount;
                }
            });

            const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
            const textColor = isDark ? '#94A3B8' : '#64748B';
            const gridColor = isDark ? '#1F2937' : '#E2E8F0';

            this.charts.trend = new Chart(ctxTrend, {
                type: 'line',
                data: {
                    labels: months,
                    datasets: [{
                        label: 'Approved Revenue (৳)',
                        data: monthlyRev,
                        borderColor: '#F54927',
                        backgroundColor: 'rgba(245, 73, 39, 0.12)',
                        fill: true,
                        tension: 0.4,
                        borderWidth: 3
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: { legend: { display: false } },
                    scales: {
                        x: { ticks: { color: textColor }, grid: { color: gridColor } },
                        y: { ticks: { color: textColor }, grid: { color: gridColor } }
                    }
                }
            });
        }

        const ctxMethod = document.getElementById('paymentMethodChart');
        if (ctxMethod) {
            if (this.charts.method) this.charts.method.destroy();

            const completed = this.purchasesData.filter(p => p.status === 'completed');
            const bkash = completed.filter(p => p.paymentMethod.toLowerCase().includes('bkash')).reduce((s, p) => s + p.amount, 0);
            const nagad = completed.filter(p => p.paymentMethod.toLowerCase().includes('nagad')).reduce((s, p) => s + p.amount, 0);
            const rocket = completed.filter(p => p.paymentMethod.toLowerCase().includes('rocket')).reduce((s, p) => s + p.amount, 0);
            const bank = completed.filter(p => p.paymentMethod.toLowerCase().includes('bank')).reduce((s, p) => s + p.amount, 0);
            const gplay = completed.filter(p => p.paymentMethod.toLowerCase().includes('google') || p.paymentMethod.toLowerCase().includes('play')).reduce((s, p) => s + p.amount, 0);

            const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
            const textColor = isDark ? '#F1F5F9' : '#0F172A';

            this.charts.method = new Chart(ctxMethod, {
                type: 'doughnut',
                data: {
                    labels: ['bKash', 'Nagad', 'Rocket', 'Bank', 'Google Play'],
                    datasets: [{
                        data: [bkash, nagad, rocket, bank, gplay],
                        backgroundColor: ['#E2136E', '#F7931E', '#8C3494', '#10B981', '#4285F4'],
                        borderWidth: 0
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { position: 'bottom', labels: { color: textColor, font: { weight: '600', size: 11 } } }
                    },
                    cutout: '65%'
                }
            });
        }
    },

    /**
     * Filter Engine
     */
    applyFilters() {
        const searchVal = this.filters.search.toLowerCase().trim();
        const now = Date.now();
        const startOfToday = new Date().setHours(0, 0, 0, 0);
        const sevenDaysMs = 7 * 24 * 60 * 60 * 1000;

        this.filteredPurchases = this.purchasesData.filter(p => {
            // Search
            if (searchVal) {
                const id = (p.id || '').toLowerCase();
                const name = (p.userName || '').toLowerCase();
                const phone = (p.phone || '').toLowerCase();
                const txid = (p.transactionId || '').toLowerCase();
                const pkg = (p.packageName || '').toLowerCase();
                if (!id.includes(searchVal) && !name.includes(searchVal) && !phone.includes(searchVal) && !txid.includes(searchVal) && !pkg.includes(searchVal)) {
                    return false;
                }
            }

            // Status Filter
            if (this.filters.status !== 'all' && p.status !== this.filters.status) return false;

            // Type Filter
            if (this.filters.type !== 'all' && p.type !== this.filters.type) return false;

            // Method Filter
            if (this.filters.method !== 'all') {
                const m = p.paymentMethod.toLowerCase();
                if (this.filters.method === 'bkash' && !m.includes('bkash')) return false;
                if (this.filters.method === 'nagad' && !m.includes('nagad')) return false;
                if (this.filters.method === 'rocket' && !m.includes('rocket')) return false;
                if (this.filters.method === 'bank' && !m.includes('bank')) return false;
                if (this.filters.method === 'google_play' && !m.includes('google') && !m.includes('play')) return false;
            }

            // Date Filter
            if (this.filters.date !== 'all') {
                if (this.filters.date === 'today' && p.createdAt < startOfToday) return false;
                if (this.filters.date === '7d' && p.createdAt < now - sevenDaysMs) return false;
                if (this.filters.date === '30d' && p.createdAt < now - 30*24*60*60*1000) return false;
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

    applyTypeFilter(type) {
        this.filters.type = type;
        const select = document.getElementById('filter-type');
        if (select) select.value = type;
        this.applyFilters();
    },

    resetFilters() {
        this.filters = { search: '', status: 'all', type: 'all', method: 'all', date: 'all' };
        const searchInput = document.getElementById('search-purchases');
        if (searchInput) searchInput.value = '';
        ['filter-status', 'filter-type', 'filter-method', 'filter-date'].forEach(id => {
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
        if (this.filters.type !== 'all') chips.push(`Type: ${this.filters.type.toUpperCase()}`);
        if (this.filters.method !== 'all') chips.push(`Method: ${this.filters.method}`);

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
        this.setElemText('showing-count', `Showing ${this.filteredPurchases.length} of ${this.purchasesData.length} purchases`);

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
        const tbody = document.getElementById('purchases-tbody');
        if (!tbody) return;

        if (this.filteredPurchases.length === 0) {
            tbody.innerHTML = `<tr><td colspan="9" style="text-align:center; padding:30px; color:var(--gray-400);">No purchase records match the current filter.</td></tr>`;
            return;
        }

        tbody.innerHTML = this.filteredPurchases.map(p => {
            const initial = (p.userName || 'U').charAt(0).toUpperCase();
            const dateStr = p.createdAt > 0 ? new Date(p.createdAt).toLocaleDateString() : 'N/A';
            const statusBadge = p.status === 'completed' ? '<span class="status-badge approved">APPROVED</span>' : (p.status === 'failed' ? '<span class="status-badge rejected">REJECTED</span>' : '<span class="status-badge pending">PENDING</span>');
            
            const methodLower = p.paymentMethod.toLowerCase();
            let methodBadgeClass = 'bank';
            if (methodLower.includes('bkash')) methodBadgeClass = 'bkash';
            else if (methodLower.includes('nagad')) methodBadgeClass = 'nagad';
            else if (methodLower.includes('rocket')) methodBadgeClass = 'rocket';
            else if (methodLower.includes('google') || methodLower.includes('play')) methodBadgeClass = 'gplay';

            return `
                <tr>
                    <td><input type="checkbox" class="purchase-select-cb" value="${p.id}"></td>
                    <td><code>${Utils.escapeHtml(p.transactionId !== 'N/A' ? p.transactionId : p.id)}</code></td>
                    <td>
                        <div style="display:flex; align-items:center; gap:10px; cursor:pointer;" onclick="PurchaseHistoryModule.openDrawer('${p.id}')">
                            <div style="width:32px; height:32px; border-radius:50%; background:var(--primary); color:white; font-weight:700; display:flex; align-items:center; justify-content:center; font-size:12px;">${initial}</div>
                            <div>
                                <div style="font-weight:700; color:var(--dark);">${Utils.escapeHtml(p.userName)}</div>
                                <div style="font-size:11px; color:var(--gray-400);">${Utils.escapeHtml(p.phone || '-')}</div>
                            </div>
                        </div>
                    </td>
                    <td><strong>${Utils.escapeHtml(p.packageName)}</strong></td>
                    <td><strong style="color:var(--primary);">৳${p.amount.toLocaleString()}</strong></td>
                    <td><span class="method-badge ${methodBadgeClass}">${Utils.escapeHtml(p.paymentMethod)}</span></td>
                    <td>${dateStr}</td>
                    <td>${statusBadge}</td>
                    <td>
                        <button class="btn btn-sm users-btn-outline" onclick="PurchaseHistoryModule.openDrawer('${p.id}')" style="padding:4px 8px; font-size:11px;">
                            <i class="fas fa-eye"></i> Details
                        </button>
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

        if (this.filteredPurchases.length === 0) {
            container.innerHTML = `<div style="grid-column: 1 / -1; text-align:center; padding:40px; color:var(--gray-400);">No purchase records match the current filter.</div>`;
            return;
        }

        container.innerHTML = this.filteredPurchases.map(p => {
            const initial = (p.userName || 'U').charAt(0).toUpperCase();
            return `
                <div class="user-grid-card">
                    <div class="user-grid-header">
                        <div style="width:40px; height:40px; border-radius:50%; background:var(--primary); color:white; font-weight:800; display:flex; align-items:center; justify-content:center; font-size:15px;">${initial}</div>
                        <div>
                            <div style="font-weight:800; font-size:15px; color:var(--dark);">${Utils.escapeHtml(p.userName)}</div>
                            <div style="font-size:12px; color:var(--gray-500);">${Utils.escapeHtml(p.packageName)}</div>
                        </div>
                    </div>
                    <div style="font-size:13px; margin-bottom:12px;">
                        <div>Amount: <strong style="color:var(--primary);">৳${p.amount.toLocaleString()}</strong></div>
                        <div>Method: <strong>${Utils.escapeHtml(p.paymentMethod)}</strong></div>
                        <div>TxID: <code>${Utils.escapeHtml(p.transactionId)}</code></div>
                    </div>
                    <div class="user-grid-footer">
                        <span class="status-badge ${p.status === 'completed' ? 'approved' : 'pending'}">${p.status.toUpperCase()}</span>
                        <button class="btn btn-sm users-btn-primary" onclick="PurchaseHistoryModule.openDrawer('${p.id}')">Details</button>
                    </div>
                </div>
            `;
        }).join('');
    },

    /**
     * Open 600px Right-Side Purchase Detail Drawer
     */
    openDrawer(purchaseId) {
        this.selectedPurchase = this.purchasesData.find(p => p.id === purchaseId);
        if (!this.selectedPurchase) return;

        const overlay = document.getElementById('purchase-drawer-overlay');
        if (overlay) overlay.classList.add('active');

        const p = this.selectedPurchase;

        this.setElemText('drawer-purchase-id', `Purchase #${p.transactionId !== 'N/A' ? p.transactionId : p.id.slice(0,8)}`);
        this.setElemText('dp-amount', `৳${p.amount.toLocaleString()}`);
        
        const statusBadge = document.getElementById('dp-status-badge');
        if (statusBadge) {
            statusBadge.className = `status-badge ${p.status === 'completed' ? 'approved' : (p.status === 'failed' ? 'rejected' : 'pending')}`;
            statusBadge.textContent = p.status.toUpperCase();
        }

        this.setElemText('dp-method', p.paymentMethod);
        this.setElemText('dp-txid', p.transactionId);
        this.setElemText('dp-date', p.createdAt > 0 ? new Date(p.createdAt).toLocaleString() : 'N/A');
        this.setElemText('dp-duration', p.duration);

        this.setElemText('dp-user-name', p.userName);
        this.setElemText('dp-user-phone', p.phone || '-');
        this.setElemText('dp-user-email', p.email || '-');
        this.setElemText('dp-user-store', p.storeName || '-');

        // Toggle Pending Approval Section
        const approvalSec = document.getElementById('dp-approval-section');
        if (approvalSec) {
            approvalSec.style.display = p.status === 'pending' ? 'block' : 'none';
        }

        // Timeline updates
        const tlVerify = document.getElementById('tl-step-verify');
        const tlActive = document.getElementById('tl-step-active');
        if (tlVerify && tlActive) {
            if (p.status === 'completed') {
                tlVerify.className = 'timeline-step completed';
                tlActive.className = 'timeline-step completed';
            } else if (p.status === 'pending') {
                tlVerify.className = 'timeline-step active';
                tlActive.className = 'timeline-step';
            } else {
                tlVerify.className = 'timeline-step';
                tlActive.className = 'timeline-step';
            }
        }
    },

    closeDrawer() {
        const overlay = document.getElementById('purchase-drawer-overlay');
        if (overlay) overlay.classList.remove('active');
    },

    /**
     * Idempotent Payment Approval Engine
     */
    async approvePendingPayment() {
        if (!this.selectedPurchase || this.selectedPurchase.status !== 'pending') return;

        const p = this.selectedPurchase;
        if (!confirm(`Are you sure you want to approve this payment request of ৳${p.amount} from ${p.userName}?`)) return;

        try {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.show();

            const now = Date.now();
            const days = parseInt(p.duration) || 30;

            // Update Payment Request Document in Firestore
            const prColName = APP_CONFIG.collections.payment_requests || 'payment_requests';
            const prRef = getCollection(prColName);
            if (prRef) {
                await prRef.doc(p.id).update({
                    status: 'approved',
                    approvedAt: now,
                    updatedAt: now
                });
            }

            // Update User Subscription or SMS Credits safely
            if (p.userId) {
                const userRef = getCollection(APP_CONFIG.collections.users).doc(p.userId);
                const userSnap = await userRef.get();
                if (userSnap.exists) {
                    const uData = userSnap.data();
                    if (p.type === 'sms') {
                        const currentSms = uData.smsLimit || 0;
                        const addedSms = parseInt(p.packageName.replace(/[^0-9]/g, '')) || 500;
                        await userRef.update({
                            smsLimit: currentSms + addedSms,
                            updatedAt: now
                        });
                    } else {
                        const currentExp = uData.subscriptionExpiryDate || now;
                        const baseTime = currentExp > now ? currentExp : now;
                        const newExp = p.duration.toLowerCase().includes('life') ? now + (36500 * 24 * 60 * 60 * 1000) : baseTime + (days * 24 * 60 * 60 * 1000);

                        await userRef.update({
                            isPremium: true,
                            premium: true,
                            subscriptionStatus: 'ACTIVE',
                            subscriptionExpiryDate: newExp,
                            subscriptionPackageName: p.packageName,
                            packageName: p.packageName,
                            updatedAt: now
                        });
                    }
                }
            }

            if (typeof App !== 'undefined' && App.Toast) App.Toast.success('Payment approved and subscription activated successfully!');
            this.closeDrawer();
            await this.loadPurchases();
        } catch (e) {
            console.error('Error approving payment:', e);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.error('Failed to approve payment');
        } finally {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.hide();
        }
    },

    /**
     * Reject Pending Payment Request
     */
    async rejectPendingPayment() {
        if (!this.selectedPurchase || this.selectedPurchase.status !== 'pending') return;

        const p = this.selectedPurchase;
        const reason = prompt('Enter rejection reason for this payment request:', 'Invalid Transaction ID / Amount mismatch');
        if (!reason) return;

        try {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.show();

            const now = Date.now();
            const prColName = APP_CONFIG.collections.payment_requests || 'payment_requests';
            const prRef = getCollection(prColName);
            if (prRef) {
                await prRef.doc(p.id).update({
                    status: 'rejected',
                    rejectionReason: reason,
                    rejectedAt: now,
                    updatedAt: now
                });
            }

            if (typeof App !== 'undefined' && App.Toast) App.Toast.success('Payment request rejected.');
            this.closeDrawer();
            await this.loadPurchases();
        } catch (e) {
            console.error('Error rejecting payment:', e);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.error('Failed to reject payment');
        } finally {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.hide();
        }
    },

    /**
     * Export CSV Generator
     */
    exportCSV() {
        if (this.filteredPurchases.length === 0) {
            alert('No purchase records to export.');
            return;
        }

        let csv = 'Purchase ID,User Name,Phone,Package,Type,Amount,Payment Method,Transaction ID,Status,Date\n';
        this.filteredPurchases.forEach(p => {
            const dateStr = p.createdAt > 0 ? new Date(p.createdAt).toISOString().slice(0,10) : '';
            csv += `"${p.id}","${p.userName}","${p.phone}","${p.packageName}","${p.type}",${p.amount},"${p.paymentMethod}","${p.transactionId}","${p.status}","${dateStr}"\n`;
        });

        const blob = new Blob([csv], { type: 'text/csv' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `HisabNiben_Purchases_Export_${new Date().toISOString().slice(0,10)}.csv`;
        a.click();
    },

    initEventListeners() {
        const searchInput = document.getElementById('search-purchases');
        if (searchInput) {
            searchInput.addEventListener('input', Utils.debounce((e) => {
                this.filters.search = e.target.value;
                this.applyFilters();
            }, 300));
        }
    }
};
