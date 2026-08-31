/**
 * Master Enterprise Payment Methods Center Module
 * HisabNiben Admin Panel
 */

const PaymentMethodsModule = {
    methodsData: [],
    filteredMethods: [],
    editingId: null,
    filters: {
        search: '',
        status: 'all',
        type: 'all'
    },

    /**
     * Initialize Module
     */
    async init() {
        await this.loadMethods();
        this.initEventListeners();
    },

    /**
     * Load Payment Methods from Firestore (payment_methods collection)
     */
    async loadMethods() {
        try {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.show();

            const pmRef = getCollection('payment_methods');
            if (pmRef) {
                const snap = await pmRef.get();
                this.methodsData = snap.docs.map(doc => ({
                    id: doc.id,
                    ...doc.data()
                }));
            }

            // Sort: sortOrder ASC, then createdAt DESC
            this.methodsData.sort((a, b) => {
                const orderA = parseInt(a.sortOrder) || 99;
                const orderB = parseInt(b.sortOrder) || 99;
                if (orderA !== orderB) return orderA - orderB;

                const tsA = Utils.parseTimestamp(a.createdAt || a.timestamp);
                const tsB = Utils.parseTimestamp(b.createdAt || b.timestamp);
                return tsB - tsA;
            });

            this.updateKPIs();
            this.applyFilters();

            const subtitle = document.getElementById('sync-timestamp-subtitle');
            if (subtitle) {
                subtitle.textContent = `Configure merchant/personal receiver accounts, payment instructions, and active gateway routes for the Android mobile app. • Synced ${new Date().toLocaleTimeString()}`;
            }
        } catch (e) {
            console.error('Error loading payment methods:', e);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.error('Failed to load payment methods');
            this.applyFilters();
        } finally {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.hide();
        }
    },

    /**
     * Refresh
     */
    async refresh() {
        await this.loadMethods();
        if (typeof App !== 'undefined' && App.Toast) App.Toast.success('Payment methods refreshed.');
    },

    /**
     * Update 4 Bento KPI Cards
     */
    updateKPIs() {
        const total = this.methodsData.length;
        const active = this.methodsData.filter(m => m.isActive !== false && m.status !== 'inactive').length;
        const inactive = total - active;

        const types = new Set(this.methodsData.map(m => m.type || 'Personal')).size;

        this.setElemText('kpi-total-methods', total);
        this.setElemText('kpi-active-methods', active);
        this.setElemText('kpi-inactive-methods', inactive);
        this.setElemText('kpi-types-count', `${types} Type${types > 1 ? 's' : ''}`);
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

        this.filteredMethods = this.methodsData.filter(m => {
            // Search
            if (searchVal) {
                const name = (m.name || '').toLowerCase();
                const num = (m.accountNumber || '').toLowerCase();
                const holder = (m.accountName || '').toLowerCase();
                const inst = (m.instructions || '').toLowerCase();

                if (!name.includes(searchVal) && !num.includes(searchVal) && !holder.includes(searchVal) && !inst.includes(searchVal)) {
                    return false;
                }
            }

            // Status Filter
            const isActive = m.isActive !== false && m.status !== 'inactive';
            if (this.filters.status === 'active' && !isActive) return false;
            if (this.filters.status === 'inactive' && isActive) return false;

            // Type Filter
            if (this.filters.type !== 'all' && (m.type || 'Personal').toLowerCase() !== this.filters.type.toLowerCase()) return false;

            return true;
        });

        this.renderCardsGrid();
        this.renderFilterChips();
    },

    applyStatusFilter(status) {
        this.filters.status = status;
        const select = document.getElementById('filter-status');
        if (select) select.value = status;
        this.applyFilters();
    },

    resetFilters() {
        this.filters = { search: '', status: 'all', type: 'all' };
        const searchInput = document.getElementById('search-methods');
        if (searchInput) searchInput.value = '';
        ['filter-status', 'filter-type'].forEach(id => {
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
        if (this.filters.type !== 'all') chips.push(`Type: ${this.filters.type.toUpperCase()}`);

        container.innerHTML = chips.map(c => `
            <span class="badge" style="background:var(--primary-light); color:var(--primary); padding:4px 10px; border-radius:12px; font-weight:700; font-size:11px;">
                ${c}
            </span>
        `).join('');
    },

    /**
     * Render Payment Method Cards Grid
     */
    renderCardsGrid() {
        const container = document.getElementById('methods-grid-container');
        const countSubtitle = document.getElementById('showing-count');
        if (!container) return;

        if (countSubtitle) {
            countSubtitle.textContent = `Showing ${this.filteredMethods.length} of ${this.methodsData.length} payment methods`;
        }

        if (this.filteredMethods.length === 0) {
            container.innerHTML = `
                <div style="grid-column: 1 / -1; text-align: center; padding: 40px; color: var(--gray-400);">
                    <i class="fas fa-wallet" style="font-size: 36px; margin-bottom: 10px; display: block; color: var(--gray-300);"></i>
                    No payment methods found matching active filters.
                </div>
            `;
            return;
        }

        container.innerHTML = this.filteredMethods.map(m => {
            const isActive = m.isActive !== false && m.status !== 'inactive';
            const nameLower = (m.name || '').toLowerCase();

            let iconClass = 'bkash';
            let iconFont = 'fa-wallet';
            if (nameLower.includes('nagad')) { iconClass = 'nagad'; iconFont = 'fa-mobile-screen'; }
            else if (nameLower.includes('rocket')) { iconClass = 'rocket'; iconFont = 'fa-rocket'; }
            else if (nameLower.includes('bank')) { iconClass = 'bank'; iconFont = 'fa-building-columns'; }

            const accNum = m.accountNumber || 'Not Set';

            return `
                <div class="pm-card ${isActive ? 'active-method' : 'inactive-method'}">
                    <div class="pm-card-header">
                        <div class="pm-icon-title">
                            <div class="pm-icon-badge ${iconClass}">
                                <i class="fas ${iconFont}"></i>
                            </div>
                            <div class="pm-title-box">
                                <h6>${Utils.escapeHtml(m.name || 'Payment Method')}</h6>
                                <div class="pm-type-tag">${Utils.escapeHtml(m.type || 'Personal')}</div>
                            </div>
                        </div>
                        <label class="switch" title="${isActive ? 'Deactivate' : 'Activate'}">
                            <input type="checkbox" ${isActive ? 'checked' : ''} onchange="PaymentMethodsModule.toggleStatus('${m.id}', ${isActive})">
                            <span class="slider"></span>
                        </label>
                    </div>

                    <div class="pm-detail-list">
                        <div class="pm-detail-item">
                            <span class="pm-detail-label">Account / Receiver:</span>
                            <div class="pm-detail-val" style="display:flex; align-items:center; gap:6px;">
                                <code>${Utils.escapeHtml(accNum)}</code>
                                <button type="button" class="trx-copy-btn" onclick="PaymentMethodsModule.copyText('${Utils.escapeHtml(accNum)}')" title="Copy Number"><i class="fas fa-copy"></i></button>
                            </div>
                        </div>
                        ${m.accountName ? `
                            <div class="pm-detail-item">
                                <span class="pm-detail-label">Holder Name:</span>
                                <span class="pm-detail-val">${Utils.escapeHtml(m.accountName)}</span>
                            </div>
                        ` : ''}
                        <div class="pm-detail-item">
                            <span class="pm-detail-label">Sort Order:</span>
                            <span class="pm-detail-val">#${m.sortOrder || 1}</span>
                        </div>
                    </div>

                    <div class="pm-instructions-box">
                        <strong><i class="fas fa-info-circle"></i> Note:</strong> ${Utils.escapeHtml(m.instructions || 'No specific instructions set.')}
                    </div>

                    <div class="pm-card-actions">
                        <button type="button" class="btn btn-sm users-btn-outline" style="flex:1;" onclick="PaymentMethodsModule.openEditModal('${m.id}')">
                            <i class="fas fa-edit"></i> Edit
                        </button>
                        <button type="button" class="btn btn-sm users-btn-outline" style="color:var(--danger);" onclick="PaymentMethodsModule.deleteMethod('${m.id}', '${Utils.escapeHtml(m.name)}')">
                            <i class="fas fa-trash-alt"></i> Delete
                        </button>
                    </div>
                </div>
            `;
        }).join('');
    },

    copyText(text) {
        if (!text || text === 'Not Set') return;
        navigator.clipboard.writeText(text);
        if (typeof App !== 'undefined' && App.Toast) App.Toast.success(`Copied "${text}" to clipboard`);
    },

    /**
     * 1-Click Active / Inactive Status Toggle
     */
    async toggleStatus(id, currentActiveState) {
        try {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.show();

            const newStatus = !currentActiveState;
            await getDocument('payment_methods', id).update({
                isActive: newStatus,
                status: newStatus ? 'active' : 'inactive',
                updatedAt: Date.now()
            });

            if (typeof App !== 'undefined' && App.Toast) {
                App.Toast.success(`Payment method ${newStatus ? 'activated' : 'deactivated'} successfully!`);
            }
            await this.loadMethods();
        } catch (e) {
            console.error('Error toggling status:', e);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.error('Failed to toggle status.');
        } finally {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.hide();
        }
    },

    /**
     * Modal Controls (Add / Edit)
     */
    openAddModal() {
        this.editingId = null;
        this.setElemText('pm-modal-title', 'Add New Payment Method');
        
        document.getElementById('pm-form').reset();
        document.getElementById('pm-sort').value = (this.methodsData.length + 1);
        document.getElementById('pm-active').checked = true;

        this.updateLivePreview();
        const modal = document.getElementById('pm-modal');
        if (modal) { modal.classList.add('active'); modal.classList.add('show'); }
    },

    openEditModal(id) {
        const m = this.methodsData.find(item => item.id === id);
        if (!m) return;

        this.editingId = id;
        const titleElem = document.getElementById('pm-modal-title');
        if (titleElem) titleElem.innerHTML = `<i class="fas fa-edit"></i> Edit Payment Method (${Utils.escapeHtml(m.name)})`;

        document.getElementById('pm-name').value = m.name || '';
        document.getElementById('pm-type').value = m.type || 'Personal';
        document.getElementById('pm-account-number').value = m.accountNumber || '';
        document.getElementById('pm-account-name').value = m.accountName || '';
        document.getElementById('pm-instructions').value = m.instructions || '';
        document.getElementById('pm-sort').value = m.sortOrder || 1;
        document.getElementById('pm-active').checked = (m.isActive !== false && m.status !== 'inactive');

        this.updateLivePreview();
        const modal = document.getElementById('pm-modal');
        if (modal) { modal.classList.add('active'); modal.classList.add('show'); }
    },

    closeModal() {
        const modal = document.getElementById('pm-modal');
        if (modal) { modal.classList.remove('active'); modal.classList.remove('show'); }
    },

    /**
     * Real-time Live Mobile App Preview Renderer
     */
    updateLivePreview() {
        const name = document.getElementById('pm-name').value.trim() || 'Payment Method Name';
        const type = document.getElementById('pm-type').value || 'Personal';
        const number = document.getElementById('pm-account-number').value.trim() || '01700000000';
        const holder = document.getElementById('pm-account-name').value.trim() || '';
        const inst = document.getElementById('pm-instructions').value.trim() || 'Make payment and enter Transaction ID (TrxID).';
        const isActive = document.getElementById('pm-active').checked;

        this.setElemText('preview-name', name);
        this.setElemText('preview-type', type.toUpperCase());
        this.setElemText('preview-number', number);
        this.setElemText('preview-holder', holder ? `Holder: ${holder}` : '');
        this.setElemText('preview-instructions', inst);

        const badge = document.getElementById('preview-status');
        if (badge) {
            if (isActive) {
                badge.style.background = 'rgba(16,185,129,0.2)';
                badge.style.color = '#34D399';
                badge.textContent = 'ACTIVE';
            } else {
                badge.style.background = 'rgba(239,68,68,0.2)';
                badge.style.color = '#F87171';
                badge.textContent = 'INACTIVE';
            }
        }

        // Icon badge style
        const iconBadge = document.getElementById('preview-icon-badge');
        const iconElem = document.getElementById('preview-icon');
        const nameLower = name.toLowerCase();

        if (iconBadge && iconElem) {
            iconBadge.className = 'pm-icon-badge';
            if (nameLower.includes('nagad')) {
                iconBadge.classList.add('nagad');
                iconElem.className = 'fas fa-mobile-screen';
            } else if (nameLower.includes('rocket')) {
                iconBadge.classList.add('rocket');
                iconElem.className = 'fas fa-rocket';
            } else if (nameLower.includes('bank')) {
                iconBadge.classList.add('bank');
                iconElem.className = 'fas fa-building-columns';
            } else {
                iconBadge.classList.add('bkash');
                iconElem.className = 'fas fa-wallet';
            }
        }
    },

    /**
     * Save Payment Method (Create / Update in Firestore)
     */
    async savePaymentMethod(event) {
        event.preventDefault();

        const name = document.getElementById('pm-name').value.trim();
        const type = document.getElementById('pm-type').value;
        const accountNumber = document.getElementById('pm-account-number').value.trim();
        const accountName = document.getElementById('pm-account-name').value.trim();
        const instructions = document.getElementById('pm-instructions').value.trim();
        const sortOrder = parseInt(document.getElementById('pm-sort').value) || 1;
        const isActive = document.getElementById('pm-active').checked;

        if (!name || !accountNumber) {
            alert('Please fill out required fields: Name and Account Number.');
            return;
        }

        try {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.show();

            const now = Date.now();
            const payload = {
                name,
                type,
                accountNumber,
                accountName,
                instructions,
                sortOrder,
                isActive,
                status: isActive ? 'active' : 'inactive',
                updatedAt: now
            };

            if (this.editingId) {
                await getDocument('payment_methods', this.editingId).set(payload, { merge: true });
                if (typeof App !== 'undefined' && App.Toast) App.Toast.success('Payment method updated successfully!');
            } else {
                // Generate custom ID or let Firestore generate
                const slugId = name.toLowerCase().replace(/[^a-z0-9]/g, '_') + '_' + accountNumber.slice(-4);
                payload.createdAt = now;
                await getDocument('payment_methods', slugId).set(payload, { merge: true });
                if (typeof App !== 'undefined' && App.Toast) App.Toast.success('Payment method created successfully!');
            }

            this.closeModal();
            await this.loadMethods();
        } catch (e) {
            console.error('Error saving payment method:', e);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.error('Failed to save payment method.');
        } finally {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.hide();
        }
    },

    /**
     * Delete Payment Method
     */
    async deleteMethod(id, name) {
        if (!confirm(`Are you sure you want to delete payment method "${name}"?`)) return;

        try {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.show();

            await getDocument('payment_methods', id).delete();

            if (typeof App !== 'undefined' && App.Toast) App.Toast.success(`Payment method "${name}" deleted.`);
            await this.loadMethods();
        } catch (e) {
            console.error('Error deleting payment method:', e);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.error('Failed to delete payment method.');
        } finally {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.hide();
        }
    },

    initEventListeners() {
        const searchInput = document.getElementById('search-methods');
        if (searchInput) {
            searchInput.addEventListener('input', Utils.debounce((e) => {
                this.filters.search = e.target.value;
                this.applyFilters();
            }, 300));
        }
    }
};
