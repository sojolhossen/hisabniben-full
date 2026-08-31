/**
 * Master Enterprise Payment Requests & Verification Center Module
 * HisabNiben Admin Panel
 */

const PaymentRequestsModule = {
    paymentsData: [],
    filteredPayments: [],
    selectedPaymentId: null,
    filters: {
        search: '',
        status: 'all',
        method: 'all',
        type: 'all'
    },

    /**
     * Initialize Module
     */
    async init() {
        this.renderTable();
        await this.loadPayments();
        this.initEventListeners();
    },

    /**
     * Helper to safely get timestamp in milliseconds
     */
    getTimestampMs(item) {
        if (!item) return 0;
        const ts = item.createdAt || item.timestamp || item.submittedAt;
        if (typeof Utils !== 'undefined' && typeof Utils.parseTimestamp === 'function') {
            return Utils.parseTimestamp(ts);
        }
        if (typeof ts === 'number') return ts;
        if (ts && ts.seconds) return ts.seconds * 1000;
        if (ts && typeof ts.toDate === 'function') return ts.toDate().getTime();
        return 0;
    },

    /**
     * Load Payment Requests from both payment_requests and purchases collections
     */
    async loadPayments() {
        try {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.show();

            const records = [];

            // 1. Read payment_requests collection
            const reqRef = getCollection('payment_requests');
            if (reqRef) {
                try {
                    const snap = await reqRef.get();
                    snap.docs.forEach(doc => {
                        records.push({
                            id: doc.id,
                            ...doc.data()
                        });
                    });
                } catch (e) {
                    console.warn('payment_requests collection read info:', e);
                }
            }

            // 2. Read purchases collection (fallback for completed purchases)
            const purchRef = getCollection('purchases');
            if (purchRef) {
                try {
                    const pSnap = await purchRef.get();
                    pSnap.docs.forEach(doc => {
                        if (!records.some(r => r.id === doc.id)) {
                            records.push({
                                id: doc.id,
                                ...doc.data()
                            });
                        }
                    });
                } catch (e) {
                    console.warn('purchases collection read info:', e);
                }
            }

            // Memory sort latest first
            records.sort((a, b) => this.getTimestampMs(b) - this.getTimestampMs(a));
            this.paymentsData = records;

            this.updateKPIs();
            this.applyFilters();

            const subtitle = document.getElementById('sync-timestamp-subtitle');
            if (subtitle) {
                subtitle.textContent = `Review, verify and manage manual bKash, Nagad, Rocket & Bank payment requests. • Synced ${new Date().toLocaleTimeString()}`;
            }
        } catch (e) {
            console.error('Error loading payment requests:', e);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.error('Failed to load payment requests');
            this.applyFilters();
        } finally {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.hide();
        }
    },

    /**
     * Refresh Data
     */
    async refresh() {
        await this.loadPayments();
        if (typeof App !== 'undefined' && App.Toast) App.Toast.success('Payment requests refreshed');
    },

    /**
     * Create a Sample Test Payment Request in Firestore
     */
    async createTestPaymentRequest() {
        try {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.show();

            const testReq = {
                userName: 'আব্দুর রহিম (টেস্ট ইউজার)',
                businessName: 'মেসার্স রহিম চালের আড়ত',
                userPhone: '01712345678',
                packageName: 'Monthly Premium Rice Pass',
                packageType: 'subscription',
                durationDays: 30,
                amount: 299,
                paymentMethod: 'bKash',
                senderNumber: '01712345678',
                transactionId: 'TRX' + Math.floor(10000000 + Math.random() * 90000000),
                status: 'pending',
                createdAt: Date.now()
            };

            await getCollection('payment_requests').add(testReq);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.success('Sample test payment request created!');
            await this.loadPayments();
        } catch (e) {
            console.error('Error creating test payment:', e);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.error('Failed to create test payment request.');
        } finally {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.hide();
        }
    },

    /**
     * Update 8 Bento KPI Summary Cards & Urgent Alert Banner
     */
    updateKPIs() {
        const total = this.paymentsData.length;
        const pending = this.paymentsData.filter(p => (p.status || 'pending').toLowerCase() === 'pending').length;
        const approved = this.paymentsData.filter(p => (p.status || '').toLowerCase() === 'approved' || (p.status || '').toLowerCase() === 'completed').length;
        const rejected = this.paymentsData.filter(p => (p.status || '').toLowerCase() === 'rejected').length;

        let approvedRev = 0;
        let pendingRev = 0;
        let todayRev = 0;
        let monthRev = 0;

        const todayStart = new Date(); todayStart.setHours(0,0,0,0);
        const monthStart = new Date(); monthStart.setDate(1); monthStart.setHours(0,0,0,0);

        this.paymentsData.forEach(p => {
            const amount = parseFloat(p.amount) || 0;
            const ts = this.getTimestampMs(p);
            const status = (p.status || 'pending').toLowerCase();

            if (status === 'approved' || status === 'completed') {
                approvedRev += amount;
                if (ts >= todayStart.getTime()) todayRev += amount;
                if (ts >= monthStart.getTime()) monthRev += amount;
            } else if (status === 'pending') {
                pendingRev += amount;
            }
        });

        this.setElemText('total-requests-count', total);
        this.setElemText('pending-requests-count', pending);
        this.setElemText('approved-requests-count', approved);
        this.setElemText('rejected-requests-count', rejected);

        this.setElemText('approved-revenue-val', `৳${approvedRev.toLocaleString()}`);
        this.setElemText('pending-revenue-val', `৳${pendingRev.toLocaleString()}`);
        this.setElemText('today-revenue-val', `৳${todayRev.toLocaleString()}`);
        this.setElemText('month-revenue-val', `৳${monthRev.toLocaleString()}`);

        // Urgent Payment Alert Box
        const alertBox = document.getElementById('urgent-payment-alert-box');
        const alertTitle = document.getElementById('urgent-alert-title');
        const alertDesc = document.getElementById('urgent-alert-desc');
        if (alertBox) {
            if (pending > 0) {
                alertBox.style.display = 'flex';
                if (alertTitle) alertTitle.textContent = `${pending} Payment Request${pending > 1 ? 's' : ''} Pending Verification`;
                if (alertDesc) alertDesc.textContent = `Manual payment requests worth ৳${pendingRev.toLocaleString()} are waiting for Super Admin review and verification.`;
            } else {
                alertBox.style.display = 'none';
            }
        }
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

        this.filteredPayments = this.paymentsData.filter(p => {
            // Search
            if (searchVal) {
                const name = (p.userName || '').toLowerCase();
                const phone = (p.userPhone || p.phone || '').toLowerCase();
                const store = (p.storeName || p.businessName || '').toLowerCase();
                const trx = (p.transactionId || p.trxId || '').toLowerCase();
                const pkg = (p.packageName || '').toLowerCase();
                const sender = (p.senderNumber || '').toLowerCase();

                if (!name.includes(searchVal) && !phone.includes(searchVal) && !store.includes(searchVal) && !trx.includes(searchVal) && !pkg.includes(searchVal) && !sender.includes(searchVal)) {
                    return false;
                }
            }

            // Status Filter
            if (this.filters.status !== 'all') {
                const st = (p.status || 'pending').toLowerCase();
                if (this.filters.status === 'approved' && st !== 'approved' && st !== 'completed') return false;
                if (this.filters.status !== 'approved' && st !== this.filters.status) return false;
            }

            // Payment Method Filter
            if (this.filters.method !== 'all') {
                const method = (p.paymentMethod || p.paymentMethodName || '').toLowerCase();
                if (!method.includes(this.filters.method)) return false;
            }

            // Purchase Type Filter
            if (this.filters.type !== 'all') {
                const type = (p.packageType || (p.smsCount ? 'sms' : 'subscription')).toLowerCase();
                if (!type.includes(this.filters.type)) return false;
            }

            return true;
        });

        this.renderTable();
        this.renderFilterChips();
    },

    applyStatusFilter(status) {
        this.filters.status = status;
        const select = document.getElementById('filter-status');
        if (select) select.value = status;
        this.applyFilters();
    },

    resetFilters() {
        this.filters = { search: '', status: 'all', method: 'all', type: 'all' };
        const searchInput = document.getElementById('search-payments');
        if (searchInput) searchInput.value = '';
        ['filter-status', 'filter-method', 'filter-type'].forEach(id => {
            const el = document.getElementById(id);
            if (el) el.value = el.options[0].value;
        });
        this.applyFilters();
    },

    renderFilterChips() {
        const container = document.getElementById('active-filter-chips');
        if (!container) return;

        const chips = [];
        if (this.filters.status !== 'all') chips.push(`Status: ${this.filters.status.toUpperCase()}`);
        if (this.filters.method !== 'all') chips.push(`Method: ${this.filters.method.toUpperCase()}`);
        if (this.filters.type !== 'all') chips.push(`Type: ${this.filters.type.toUpperCase()}`);

        container.innerHTML = chips.map(c => `
            <span class="badge" style="background:var(--primary-light); color:var(--primary); padding:4px 10px; border-radius:12px; font-weight:700; font-size:11px;">
                ${c}
            </span>
        `).join('');
    },

    /**
     * Check if a Transaction ID is duplicated across other payment requests
     */
    checkDuplicateTrxId(trxId, currentId) {
        if (!trxId || trxId === '-') return false;
        return this.paymentsData.some(p => p.id !== currentId && (p.transactionId === trxId || p.trxId === trxId));
    },

    /**
     * Render Payment Table
     */
    renderTable() {
        const tbody = document.getElementById('payment-requests-tbody');
        const countSubtitle = document.getElementById('showing-count');
        if (!tbody) return;

        if (countSubtitle) {
            countSubtitle.textContent = `Showing ${this.filteredPayments.length} of ${this.paymentsData.length} payment requests`;
        }

        if (this.filteredPayments.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" style="text-align:center; padding:40px 20px; color:var(--gray-500);">
                        <i class="fas fa-inbox" style="font-size:32px; color:var(--gray-300); margin-bottom:12px; display:block;"></i>
                        <div style="font-weight:700; font-size:14px; margin-bottom:4px; color:var(--dark);">No Payment Requests Found</div>
                        <div style="font-size:12px; color:var(--gray-400); margin-bottom:16px;">No manual payment requests match the active filter.</div>
                        <button type="button" class="btn btn-sm users-btn-primary" onclick="PaymentRequestsModule.createTestPaymentRequest()">
                            <i class="fas fa-plus"></i> Create Sample Test Request
                        </button>
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = this.filteredPayments.map((p, idx) => {
            const trxId = p.transactionId || p.trxId || '-';
            const isDuplicateTrx = this.checkDuplicateTrxId(trxId, p.id);
            const status = (p.status || 'pending').toLowerCase();

            let statusBadge = '<span class="status-badge pending">PENDING</span>';
            if (status === 'approved' || status === 'completed') statusBadge = '<span class="status-badge approved">APPROVED</span>';
            else if (status === 'rejected') statusBadge = '<span class="status-badge rejected">REJECTED</span>';
            else if (status === 'on_hold') statusBadge = '<span class="status-badge expired">ON HOLD</span>';

            const method = p.paymentMethod || p.paymentMethodName || 'Manual';
            const ts = this.getTimestampMs(p);
            const timeStr = ts > 0 ? Utils.formatDate(ts) : '-';
            const userName = p.userName || 'Unknown User';
            const storeName = p.storeName || p.businessName || '';
            const phone = p.userPhone || p.phone || '';

            return `
                <tr>
                    <td style="text-align:center;">${idx + 1}</td>
                    <td>
                        <div style="font-weight:800; color:var(--dark); font-size:13.5px;">${Utils.escapeHtml(userName)}</div>
                        ${storeName ? `<div style="font-size:11.5px; color:var(--primary); font-weight:600;"><i class="fas fa-store"></i> ${Utils.escapeHtml(storeName)}</div>` : ''}
                        <div style="font-size:11px; color:var(--gray-400);">${Utils.escapeHtml(phone)}</div>
                    </td>
                    <td>
                        <strong style="color:var(--dark);">${Utils.escapeHtml(p.packageName || 'Package')}</strong>
                        <div style="font-size:11px; color:var(--gray-400); font-weight:600;">${(p.packageType || 'subscription').toUpperCase()} • ${p.smsCount ? p.smsCount + ' SMS' : (p.durationDays ? p.durationDays + ' Days' : '')}</div>
                    </td>
                    <td style="text-align:center;"><strong style="color:var(--primary); font-size:15px;">৳${(parseFloat(p.amount) || 0).toLocaleString()}</strong></td>
                    <td style="text-align:center;">
                        <span class="badge" style="background:var(--gray-100); color:var(--dark); font-weight:800; padding:4px 10px; font-size:11.5px;">${Utils.escapeHtml(method)}</span>
                        ${p.senderNumber ? `<div style="font-size:11px; color:var(--gray-400); margin-top:2px;">From: ${Utils.escapeHtml(p.senderNumber)}</div>` : ''}
                    </td>
                    <td style="text-align:center;">
                        <div class="trx-badge">
                            <span>${Utils.escapeHtml(trxId)}</span>
                            <button type="button" class="trx-copy-btn" onclick="PaymentRequestsModule.copyText('${Utils.escapeHtml(trxId)}')" title="Copy TrxID"><i class="fas fa-copy"></i></button>
                        </div>
                        ${isDuplicateTrx ? `<div style="font-size:10px; color:#EF4444; font-weight:800; margin-top:2px;"><i class="fas fa-exclamation-circle"></i> DUPLICATE TRX</div>` : ''}
                    </td>
                    <td style="text-align:center;">${statusBadge}</td>
                    <td style="text-align:center;">
                        <div style="display:flex; justify-content:center; gap:6px;">
                            <button class="btn btn-sm users-btn-outline" onclick="PaymentRequestsModule.openDrawer('${p.id}')" title="View Details" style="padding:4px 8px; font-size:11px;">
                                <i class="fas fa-eye"></i>
                            </button>
                            ${status === 'pending' ? `
                                <button class="btn btn-sm users-btn-primary" onclick="PaymentRequestsModule.approvePayment('${p.id}')" title="Approve" style="padding:4px 8px; font-size:11px; background:#10B981; border-color:#10B981;">
                                    <i class="fas fa-check"></i>
                                </button>
                                <button class="btn btn-sm users-btn-outline" onclick="PaymentRequestsModule.rejectPayment('${p.id}')" title="Reject" style="padding:4px 8px; font-size:11px; color:var(--danger);">
                                    <i class="fas fa-times"></i>
                                </button>
                            ` : ''}
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    },

    copyText(text) {
        if (!text || text === '-') return;
        navigator.clipboard.writeText(text);
        if (typeof App !== 'undefined' && App.Toast) App.Toast.success(`Copied "${text}" to clipboard`);
    },

    /**
     * 600px Right-Side Payment Detail Drawer Controls
     */
    openDrawer(id) {
        const p = this.paymentsData.find(item => item.id === id);
        if (!p) return;

        this.selectedPaymentId = id;
        const trxId = p.transactionId || p.trxId || '-';
        const isDuplicateTrx = this.checkDuplicateTrxId(trxId, p.id);
        const status = (p.status || 'pending').toLowerCase();
        const ts = this.getTimestampMs(p);

        this.setElemText('drawer-request-id', `Payment Request #${p.id.substring(0, 8)}`);
        this.setElemText('drawer-submitted-time', `Submitted: ${ts > 0 ? Utils.formatDate(ts) : '-'}`);

        const content = document.getElementById('drawer-content-body');
        const footer = document.getElementById('drawer-footer-actions');

        if (content) {
            content.innerHTML = `
                ${isDuplicateTrx ? `
                    <div class="high-risk-box">
                        <i class="fas fa-exclamation-triangle" style="font-size:18px;"></i>
                        <div>
                            HIGH RISK: Duplicate Transaction ID Detected!
                            <div style="font-size:11px; font-weight:500; margin-top:2px;">This TrxID (${trxId}) has been used in another payment request. Verify bank statement carefully.</div>
                        </div>
                    </div>
                ` : ''}

                <div class="drawer-section">
                    <div class="drawer-section-title"><i class="fas fa-file-invoice"></i> Payment Summary</div>
                    <div class="drawer-field-grid">
                        <div class="drawer-field-item">
                            <div class="drawer-field-label">Status</div>
                            <div class="drawer-field-val">${p.status ? p.status.toUpperCase() : 'PENDING'}</div>
                        </div>
                        <div class="drawer-field-item">
                            <div class="drawer-field-label">Amount Paid</div>
                            <div class="drawer-field-val" style="color:var(--primary); font-size:16px;">৳${(parseFloat(p.amount) || 0).toLocaleString()}</div>
                        </div>
                        <div class="drawer-field-item">
                            <div class="drawer-field-label">Payment Method</div>
                            <div class="drawer-field-val">${p.paymentMethod || p.paymentMethodName || 'Manual'}</div>
                        </div>
                        <div class="drawer-field-item">
                            <div class="drawer-field-label">Transaction ID (TrxID)</div>
                            <div class="drawer-field-val"><code>${Utils.escapeHtml(trxId)}</code></div>
                        </div>
                    </div>
                </div>

                <div class="drawer-section">
                    <div class="drawer-section-title"><i class="fas fa-user"></i> User & Business Information</div>
                    <div class="drawer-field-grid">
                        <div class="drawer-field-item">
                            <div class="drawer-field-label">User Name</div>
                            <div class="drawer-field-val">${Utils.escapeHtml(p.userName || 'N/A')}</div>
                        </div>
                        <div class="drawer-field-item">
                            <div class="drawer-field-label">Store / Business</div>
                            <div class="drawer-field-val">${Utils.escapeHtml(p.storeName || p.businessName || 'N/A')}</div>
                        </div>
                        <div class="drawer-field-item">
                            <div class="drawer-field-label">Phone Number</div>
                            <div class="drawer-field-val">${Utils.escapeHtml(p.userPhone || p.phone || 'N/A')}</div>
                        </div>
                        <div class="drawer-field-item">
                            <div class="drawer-field-label">Sender Number</div>
                            <div class="drawer-field-val">${Utils.escapeHtml(p.senderNumber || 'N/A')}</div>
                        </div>
                    </div>
                </div>

                <div class="drawer-section">
                    <div class="drawer-section-title"><i class="fas fa-box"></i> Package & Benefits</div>
                    <div class="drawer-field-grid">
                        <div class="drawer-field-item">
                            <div class="drawer-field-label">Package Name</div>
                            <div class="drawer-field-val">${Utils.escapeHtml(p.packageName || 'Package')}</div>
                        </div>
                        <div class="drawer-field-item">
                            <div class="drawer-field-label">Type</div>
                            <div class="drawer-field-val">${(p.packageType || 'subscription').toUpperCase()}</div>
                        </div>
                        <div class="drawer-field-item">
                            <div class="drawer-field-label">Benefit</div>
                            <div class="drawer-field-val" style="color:#10B981;">${p.smsCount ? p.smsCount + ' SMS Credits' : (p.durationDays ? p.durationDays + ' Days Access' : 'Package Access')}</div>
                        </div>
                    </div>
                </div>
            `;
        }

        if (footer) {
            if (status === 'pending') {
                footer.innerHTML = `
                    <button class="btn btn-outline" style="color:var(--danger);" onclick="PaymentRequestsModule.rejectPayment('${p.id}')"><i class="fas fa-times"></i> Reject</button>
                    <button class="btn btn-primary" style="flex:1; background:#10B981; border-color:#10B981;" onclick="PaymentRequestsModule.approvePayment('${p.id}')"><i class="fas fa-check"></i> Verify & Approve Payment</button>
                `;
            } else {
                footer.innerHTML = `
                    <div style="font-size:12px; color:var(--gray-500); width:100%; text-align:center;">This payment request is already <strong>${status.toUpperCase()}</strong>.</div>
                `;
            }
        }

        const backdrop = document.getElementById('drawer-backdrop');
        const drawer = document.getElementById('payment-drawer');
        if (backdrop) backdrop.classList.add('active');
        if (drawer) drawer.classList.add('active');
    },

    closeDrawer() {
        const backdrop = document.getElementById('drawer-backdrop');
        const drawer = document.getElementById('payment-drawer');
        if (backdrop) backdrop.classList.remove('active');
        if (drawer) drawer.classList.remove('active');
    },

    /**
     * 100% IDEMPOTENT APPROVAL ENGINE
     */
    async approvePayment(id) {
        const p = this.paymentsData.find(item => item.id === id);
        if (!p) return;

        // Idempotency check: block double approval
        if ((p.status || '').toLowerCase() === 'approved' || (p.status || '').toLowerCase() === 'completed') {
            alert('This payment request has already been approved.');
            return;
        }

        const amount = parseFloat(p.amount) || 0;
        const trxId = p.transactionId || p.trxId || '-';
        const isDuplicateTrx = this.checkDuplicateTrxId(trxId, p.id);

        let warning = '';
        if (isDuplicateTrx) {
            warning = '\n\n⚠️ HIGH RISK WARNING: Duplicate Transaction ID detected!';
        }

        if (!confirm(`Approve payment request of ৳${amount} from "${p.userName || 'User'}"?${warning}`)) return;

        try {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.show();

            const now = Date.now();
            const isSmsPkg = p.packageType === 'sms' || !!p.smsCount;

            // 1. Update User in Firestore
            if (p.userId) {
                const userRef = getDocument(APP_CONFIG.collections.users, p.userId);
                const userSnap = await userRef.get();

                if (userSnap.exists) {
                    const userData = userSnap.data();

                    if (isSmsPkg) {
                        const smsAdd = parseInt(p.smsCount) || 0;
                        const currentSms = parseInt(userData.smsCount || userData.smsLimit) || 0;
                        const newSms = currentSms + smsAdd;

                        await userRef.update({
                            smsCount: newSms,
                            smsLimit: newSms,
                            updatedAt: now
                        });
                    } else {
                        // Subscription
                        const durationDays = parseInt(p.durationDays) || 30;
                        let currentExpiry = 0;

                        if (userData.subscriptionExpiryDate) {
                            if (typeof userData.subscriptionExpiryDate === 'number') currentExpiry = userData.subscriptionExpiryDate;
                            else if (userData.subscriptionExpiryDate.seconds) currentExpiry = userData.subscriptionExpiryDate.seconds * 1000;
                        }

                        let newExpiry;
                        if (durationDays >= 999) {
                            newExpiry = now + (3650 * 24 * 60 * 60 * 1000); // 10 years
                        } else if (currentExpiry > now) {
                            newExpiry = currentExpiry + (durationDays * 24 * 60 * 60 * 1000);
                        } else {
                            newExpiry = now + (durationDays * 24 * 60 * 60 * 1000);
                        }

                        await userRef.update({
                            subscriptionStatus: 'ACTIVE',
                            subscriptionExpiryDate: newExpiry,
                            subscriptionPackageName: p.packageName || 'Premium',
                            updatedAt: now
                        });
                    }
                }
            }

            // 2. Update Payment Request status
            await getDocument('payment_requests', id).update({
                status: 'approved',
                approvedAt: now,
                updatedAt: now
            });

            // 3. Create purchase record
            await getCollection('purchases').add({
                userId: p.userId || '',
                userName: p.userName || '',
                packageName: p.packageName || 'Package',
                amount: amount,
                paymentMethod: p.paymentMethod || p.paymentMethodName || 'Manual',
                transactionId: trxId,
                status: 'completed',
                createdAt: now
            });

            if (typeof App !== 'undefined' && App.Toast) App.Toast.success('Payment approved & benefits applied successfully!');
            this.closeDrawer();
            await this.loadPayments();
        } catch (e) {
            console.error('Error approving payment:', e);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.error('Failed to approve payment.');
        } finally {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.hide();
        }
    },

    /**
     * Reject Payment Request
     */
    async rejectPayment(id) {
        const p = this.paymentsData.find(item => item.id === id);
        if (!p) return;

        const reason = prompt('Reason for rejection (e.g. Invalid Transaction ID, Payment Not Received):', 'Invalid Transaction ID');
        if (reason === null) return;

        try {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.show();

            const now = Date.now();
            await getDocument('payment_requests', id).update({
                status: 'rejected',
                rejectionReason: reason,
                rejectedAt: now,
                updatedAt: now
            });

            if (typeof App !== 'undefined' && App.Toast) App.Toast.success('Payment request rejected');
            this.closeDrawer();
            await this.loadPayments();
        } catch (e) {
            console.error('Error rejecting payment:', e);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.error('Failed to reject payment.');
        } finally {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.hide();
        }
    },

    /**
     * Export CSV Generator
     */
    exportCSV() {
        if (this.filteredPayments.length === 0) {
            alert('No payment requests to export.');
            return;
        }

        let csv = 'Request ID,User Name,Phone,Package,Amount,Payment Method,Transaction ID,Status,Submitted Date\n';
        this.filteredPayments.forEach(p => {
            const ts = this.getTimestampMs(p);
            const timeStr = ts > 0 ? Utils.formatDate(ts) : '-';
            csv += `"${p.id}","${p.userName || ''}","${p.userPhone || p.phone || ''}","${p.packageName || ''}",${p.amount || 0},"${p.paymentMethod || p.paymentMethodName || ''}","${p.transactionId || p.trxId || ''}","${p.status || 'pending'}","${timeStr}"\n`;
        });

        const blob = new Blob([csv], { type: 'type/csv' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `HisabNiben_Payment_Requests_${new Date().toISOString().slice(0,10)}.csv`;
        a.click();
    },

    initEventListeners() {
        const searchInput = document.getElementById('search-payments');
        if (searchInput) {
            searchInput.addEventListener('input', Utils.debounce((e) => {
                this.filters.search = e.target.value;
                this.applyFilters();
            }, 300));
        }
    }
};
