/**
 * Master Enterprise SMS Packages Control Center Module
 * HisabNiben Admin Panel
 */

const SmsPackagesModule = {
    packagesData: [],
    filteredPackages: [],
    editingId: null,
    isGridView: false,
    filters: {
        search: '',
        status: 'all',
        smsRange: 'all',
        sort: 'price_asc'
    },

    /**
     * Initialize Module
     */
    async init() {
        await this.loadPackages();
        this.initEventListeners();
    },

    /**
     * Load SMS Packages from Firestore
     */
    async loadPackages() {
        try {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.show();

            const pkgRef = getCollection('sms_packages');
            if (pkgRef) {
                const snap = await pkgRef.get();
                this.packagesData = snap.docs.map(doc => ({
                    id: doc.id,
                    ...doc.data()
                }));
            }

            this.updateStats();
            this.updatePerformanceHighlights();
            this.applyFilters();

            const subtitle = document.getElementById('sync-timestamp-subtitle');
            if (subtitle) {
                subtitle.textContent = `Manage SMS bundles, pricing, per-SMS unit cost, and live Firestore sync. • Synced ${new Date().toLocaleTimeString()}`;
            }
        } catch (e) {
            console.error('Error loading SMS packages:', e);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.error('Failed to load SMS packages');
        } finally {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.hide();
        }
    },

    /**
     * Refresh
     */
    async refresh() {
        await this.loadPackages();
        if (typeof App !== 'undefined' && App.Toast) App.Toast.success('SMS packages refreshed');
    },

    /**
     * Update 4 Bento KPI Summary Cards
     */
    updateStats() {
        const nonArchived = this.packagesData.filter(p => !p.isArchived);
        const total = nonArchived.length;
        const active = nonArchived.filter(p => p.status === 'active' || p.isActive === true).length;
        const inactive = nonArchived.filter(p => p.status !== 'active' && !p.isActive).length;
        const totalSms = nonArchived.filter(p => p.status === 'active' || p.isActive).reduce((sum, p) => sum + (p.smsCount || 0), 0);

        this.setElemText('total-packages-count', total);
        this.setElemText('active-packages-count', active);
        this.setElemText('inactive-packages-count', inactive);
        this.setElemText('total-sms-count', totalSms.toLocaleString());
    },

    /**
     * Performance Highlights Calculator
     */
    updatePerformanceHighlights() {
        const activePkgs = this.packagesData.filter(p => (p.status === 'active' || p.isActive) && !p.isArchived);
        if (activePkgs.length === 0) return;

        // 1. Best Value (lowest price / smsCount)
        const sortedByValue = [...activePkgs].sort((a, b) => {
            const costA = (a.price || 0) / (a.smsCount || 1);
            const costB = (b.price || 0) / (b.smsCount || 1);
            return costA - costB;
        });
        const bv = sortedByValue[0];
        if (bv) {
            const unitCost = ((bv.price || 0) / (bv.smsCount || 1)).toFixed(2);
            this.setElemText('bv-name', bv.name || 'N/A');
            this.setElemText('bv-sub', `৳${unitCost} / SMS • ${bv.smsCount} SMS`);
        }

        // 2. Cheapest Option
        const sortedByPrice = [...activePkgs].sort((a, b) => (a.price || 0) - (b.price || 0));
        const cheap = sortedByPrice[0];
        if (cheap) {
            this.setElemText('cheap-name', cheap.name || 'N/A');
            this.setElemText('cheap-sub', `Only ৳${cheap.price} (${cheap.smsCount} SMS)`);
        }

        // 3. Highest SMS Volume
        const sortedBySms = [...activePkgs].sort((a, b) => (b.smsCount || 0) - (a.smsCount || 0));
        const hv = sortedBySms[0];
        if (hv) {
            this.setElemText('hv-name', hv.name || 'N/A');
            this.setElemText('hv-sub', `${(hv.smsCount || 0).toLocaleString()} SMS Bundle`);
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

        this.filteredPackages = this.packagesData.filter(p => {
            // Search
            if (searchVal) {
                const name = (p.name || '').toLowerCase();
                const pid = (p.productId || p.playStoreProductId || '').toLowerCase();
                const desc = (p.description || '').toLowerCase();
                const sms = (p.smsCount || '').toString();
                const price = (p.price || '').toString();
                if (!name.includes(searchVal) && !pid.includes(searchVal) && !desc.includes(searchVal) && !sms.includes(searchVal) && !price.includes(searchVal)) {
                    return false;
                }
            }

            // Status Filter
            if (this.filters.status !== 'all') {
                if (this.filters.status === 'archived' && !p.isArchived) return false;
                if (this.filters.status !== 'archived') {
                    if (p.isArchived) return false;
                    const isActive = p.status === 'active' || p.isActive === true;
                    if (this.filters.status === 'active' && !isActive) return false;
                    if (this.filters.status === 'inactive' && isActive) return false;
                }
            } else {
                // Default hide archived unless explicitly asked
                if (p.isArchived) return false;
            }

            // SMS Range Filter
            if (this.filters.smsRange !== 'all') {
                const minSms = parseInt(this.filters.smsRange) || 0;
                if ((p.smsCount || 0) < minSms) return false;
            }

            return true;
        });

        // Sort
        this.filteredPackages.sort((a, b) => {
            if (this.filters.sort === 'price_asc') return (a.price || 0) - (b.price || 0);
            if (this.filters.sort === 'price_desc') return (b.price || 0) - (a.price || 0);
            if (this.filters.sort === 'sms_desc') return (b.smsCount || 0) - (a.smsCount || 0);
            return (a.sortOrder || 99) - (b.sortOrder || 99);
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

    resetFilters() {
        this.filters = { search: '', status: 'all', smsRange: 'all', sort: 'price_asc' };
        const searchInput = document.getElementById('search-packages');
        if (searchInput) searchInput.value = '';
        ['filter-status', 'filter-sms-range', 'filter-sort'].forEach(id => {
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
        if (this.filters.smsRange !== 'all') chips.push(`SMS: ${this.filters.smsRange}+`);

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
        this.setElemText('showing-count', `Showing ${this.filteredPackages.length} of ${this.packagesData.length} packages`);

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
     * Render Table View
     */
    renderTable() {
        const tbody = document.getElementById('sms-packages-tbody');
        if (!tbody) return;

        if (this.filteredPackages.length === 0) {
            tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; padding:30px; color:var(--gray-400);">No SMS packages match current filter.</td></tr>`;
            return;
        }

        tbody.innerHTML = this.filteredPackages.map((p, idx) => {
            const isActive = p.status === 'active' || p.isActive === true;
            const isArchived = p.isArchived === true;
            
            let statusBadge = '<span class="status-badge pending">INACTIVE</span>';
            if (isArchived) statusBadge = '<span class="status-badge rejected">ARCHIVED</span>';
            else if (isActive) statusBadge = '<span class="status-badge approved">ACTIVE</span>';

            const unitCost = (p.smsCount > 0) ? `৳${((p.price || 0) / p.smsCount).toFixed(2)} / SMS` : '—';
            const rawPid = p.productId || p.playStoreProductId;
            const pidDisplay = rawPid ? `<code>${Utils.escapeHtml(rawPid)}</code>` : `<span style="color:var(--gray-400); font-style:italic; font-size:12px;">Not Configured</span>`;

            return `
                <tr>
                    <td>${idx + 1}</td>
                    <td>
                        <strong style="color:var(--dark); font-size:14px;">${Utils.escapeHtml(p.name || 'Unnamed')}</strong>
                        ${p.description ? `<div style="font-size:11px; color:var(--gray-400); margin-top:2px;">${Utils.escapeHtml(p.description)}</div>` : ''}
                    </td>
                    <td><strong>${(p.smsCount || 0).toLocaleString()} SMS</strong></td>
                    <td><strong style="color:var(--primary); font-size:14px;">৳${(p.price || 0).toLocaleString()}</strong></td>
                    <td><span class="badge" style="background:rgba(16,185,129,0.12); color:#10B981; font-weight:800; padding:4px 10px; border-radius:12px; font-size:11.5px;"><i class="fas fa-circle" style="font-size:7px; vertical-align:middle; margin-right:4px;"></i>${unitCost}</span></td>
                    <td>${pidDisplay}</td>
                    <td>${statusBadge}</td>
                    <td>
                        <div style="display:flex; justify-content:center; gap:6px;">
                            <button class="btn btn-sm users-btn-outline" onclick="SmsPackagesModule.openEditModal('${p.id}')" title="Edit" style="padding:4px 8px; font-size:11px;">
                                <i class="fas fa-edit"></i>
                            </button>
                            <button class="btn btn-sm users-btn-outline" onclick="SmsPackagesModule.duplicatePackage('${p.id}')" title="Duplicate" style="padding:4px 8px; font-size:11px;">
                                <i class="fas fa-copy"></i>
                            </button>
                            <button class="btn btn-sm users-btn-outline" onclick="SmsPackagesModule.togglePackageStatus('${p.id}')" title="Toggle Status" style="padding:4px 8px; font-size:11px;">
                                <i class="fas fa-power-off"></i>
                            </button>
                            <button class="btn btn-sm users-btn-outline" onclick="SmsPackagesModule.archivePackage('${p.id}')" title="Safe Archive" style="padding:4px 8px; font-size:11px; color:var(--danger);">
                                <i class="fas fa-archive"></i>
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

        if (this.filteredPackages.length === 0) {
            container.innerHTML = `<div style="grid-column:1/-1; text-align:center; padding:40px; color:var(--gray-400);">No SMS packages match current filter.</div>`;
            return;
        }

        container.innerHTML = this.filteredPackages.map(p => {
            const isActive = p.status === 'active' || p.isActive === true;
            const unitCost = (p.smsCount > 0) ? `৳${((p.price || 0) / p.smsCount).toFixed(2)} / SMS` : '—';
            return `
                <div class="user-grid-card" style="border-top:3px solid var(--primary);">
                    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:10px;">
                        <h4 style="font-size:16px; font-weight:800; color:var(--dark); margin:0;">${Utils.escapeHtml(p.name)}</h4>
                        <span class="status-badge ${isActive ? 'approved' : 'pending'}">${isActive ? 'ACTIVE' : 'INACTIVE'}</span>
                    </div>
                    <div style="font-size:24px; font-weight:900; color:var(--primary); margin-bottom:4px;">৳${(p.price || 0).toLocaleString()}</div>
                    <div style="font-size:13px; font-weight:700; color:var(--gray-600); margin-bottom:4px;">${(p.smsCount || 0).toLocaleString()} SMS</div>
                    <div style="font-size:12px; color:#10B981; font-weight:700; margin-bottom:14px;">${unitCost}</div>
                    <div class="user-grid-footer">
                        <button class="btn btn-sm users-btn-outline" onclick="SmsPackagesModule.duplicatePackage('${p.id}')"><i class="fas fa-copy"></i> Clone</button>
                        <button class="btn btn-sm users-btn-primary" onclick="SmsPackagesModule.openEditModal('${p.id}')"><i class="fas fa-edit"></i> Edit</button>
                    </div>
                </div>
            `;
        }).join('');
    },

    /**
     * Modal Controls
     */
    openAddModal() {
        this.editingId = null;
        document.getElementById('sms-package-modal-title').innerHTML = '<i class="fas fa-plus-circle"></i> Create SMS Package';
        document.getElementById('sms-package-form').reset();
        document.getElementById('sms-package-id').value = '';

        const modal = document.getElementById('sms-package-modal');
        if (modal) {
            modal.classList.add('active');
            modal.classList.add('show');
        }
        this.updateLiveUnitCost();
    },

    openEditModal(id) {
        const p = this.packagesData.find(item => item.id === id);
        if (!p) return;

        this.editingId = id;
        document.getElementById('sms-package-modal-title').innerHTML = '<i class="fas fa-edit"></i> Edit SMS Package';

        document.getElementById('sms-package-id').value = p.id;
        document.getElementById('sms-package-name').value = p.name || '';
        document.getElementById('sms-package-count').value = p.smsCount || 500;
        document.getElementById('sms-package-price').value = p.price || 200;
        document.getElementById('sms-package-product-id').value = p.productId || p.playStoreProductId || '';
        document.getElementById('sms-package-description').value = p.description || '';
        document.getElementById('sms-package-sort').value = p.sortOrder || 1;
        document.getElementById('sms-package-status').value = (p.status === 'active' || p.isActive) ? 'active' : 'inactive';

        const modal = document.getElementById('sms-package-modal');
        if (modal) {
            modal.classList.add('active');
            modal.classList.add('show');
        }
        this.updateLiveUnitCost();
    },

    closeModal() {
        const modal = document.getElementById('sms-package-modal');
        if (modal) {
            modal.classList.remove('active');
            modal.classList.remove('show');
        }
    },

    /**
     * Live Unit Cost Calculator
     */
    updateLiveUnitCost() {
        const count = parseInt(document.getElementById('sms-package-count').value) || 0;
        const price = parseFloat(document.getElementById('sms-package-price').value) || 0;

        const breakdownElem = document.getElementById('calc-breakdown');
        const unitPriceElem = document.getElementById('calc-unit-price');

        if (breakdownElem) breakdownElem.textContent = `${count.toLocaleString()} SMS • ৳${price.toLocaleString()}`;

        if (count > 0 && price >= 0) {
            const unitCost = (price / count).toFixed(2);
            if (unitPriceElem) unitPriceElem.textContent = `৳${unitCost} / SMS`;
        } else {
            if (unitPriceElem) unitPriceElem.textContent = '৳0.00 / SMS';
        }
    },

    /**
     * Save Package to Firestore
     */
    async savePackage(event) {
        event.preventDefault();

        const name = document.getElementById('sms-package-name').value.trim();
        const smsCount = parseInt(document.getElementById('sms-package-count').value) || 0;
        const price = parseFloat(document.getElementById('sms-package-price').value) || 0;
        const productId = document.getElementById('sms-package-product-id').value.trim();
        const description = document.getElementById('sms-package-description').value.trim();
        const sortOrder = parseInt(document.getElementById('sms-package-sort').value) || 1;
        const status = document.getElementById('sms-package-status').value;

        if (!name || smsCount <= 0 || price < 0) {
            alert('Please fill out all required fields with valid values.');
            return;
        }

        // Duplicate Product ID Check
        if (productId) {
            const duplicate = this.packagesData.find(p => p.id !== this.editingId && (p.productId === productId || p.playStoreProductId === productId));
            if (duplicate) {
                alert(`এই Play Store Product ID (${productId}) ইতিমধ্যে অন্য একটি প্যাকেজে ব্যবহার করা হয়েছে।`);
                return;
            }
        }

        try {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.show();

            const pkgData = {
                name,
                smsCount,
                price,
                productId,
                playStoreProductId: productId,
                description,
                sortOrder,
                status,
                isActive: status === 'active',
                isPublished: status === 'active',
                updatedAt: Date.now()
            };

            const pkgRef = getCollection('sms_packages');

            if (this.editingId) {
                await pkgRef.doc(this.editingId).update(pkgData);
                if (typeof App !== 'undefined' && App.Toast) App.Toast.success('SMS Package updated!');
            } else {
                pkgData.createdAt = Date.now();
                await pkgRef.add(pkgData);
                if (typeof App !== 'undefined' && App.Toast) App.Toast.success('New SMS Package created!');
            }

            this.closeModal();
            await this.loadPackages();
        } catch (e) {
            console.error('Error saving SMS package:', e);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.error('Failed to save SMS package');
        } finally {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.hide();
        }
    },

    /**
     * Duplicate Package
     */
    async duplicatePackage(id) {
        const p = this.packagesData.find(item => item.id === id);
        if (!p) return;

        if (!confirm(`Duplicate package "${p.name}"?`)) return;

        try {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.show();

            const copyData = {
                ...p,
                name: `Copy of ${p.name}`,
                createdAt: Date.now(),
                updatedAt: Date.now()
            };
            delete copyData.id;

            const pkgRef = getCollection('sms_packages');
            await pkgRef.add(copyData);

            if (typeof App !== 'undefined' && App.Toast) App.Toast.success('SMS Package duplicated!');
            await this.loadPackages();
        } catch (e) {
            console.error('Error duplicating package:', e);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.error('Failed to duplicate SMS package');
        } finally {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.hide();
        }
    },

    /**
     * Toggle Status
     */
    async togglePackageStatus(id) {
        const p = this.packagesData.find(item => item.id === id);
        if (!p) return;

        const newStatus = (p.status === 'active' || p.isActive) ? 'inactive' : 'active';

        try {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.show();

            const pkgRef = getCollection('sms_packages');
            await pkgRef.doc(id).update({
                status: newStatus,
                isActive: newStatus === 'active',
                updatedAt: Date.now()
            });

            if (typeof App !== 'undefined' && App.Toast) App.Toast.success(`Status updated to ${newStatus.toUpperCase()}`);
            await this.loadPackages();
        } catch (e) {
            console.error('Error toggling status:', e);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.error('Failed to update status');
        } finally {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.hide();
        }
    },

    /**
     * Safe Archive (Replaces hard deletion)
     */
    async archivePackage(id) {
        const p = this.packagesData.find(item => item.id === id);
        if (!p) return;

        if (!confirm(`Are you sure you want to archive "${p.name}"? Archived packages will no longer be purchasable, but historical records remain safe.`)) return;

        try {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.show();

            const pkgRef = getCollection('sms_packages');
            await pkgRef.doc(id).update({
                isArchived: true,
                status: 'inactive',
                isActive: false,
                updatedAt: Date.now()
            });

            if (typeof App !== 'undefined' && App.Toast) App.Toast.success('SMS package safely archived');
            await this.loadPackages();
        } catch (e) {
            console.error('Error archiving package:', e);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.error('Failed to archive package');
        } finally {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.hide();
        }
    },

    /**
     * Export CSV Generator
     */
    exportCSV() {
        if (this.filteredPackages.length === 0) {
            alert('No SMS packages to export.');
            return;
        }

        let csv = 'Package Name,SMS Count,Price,Price Per SMS,Product ID,Status,Sort Order\n';
        this.filteredPackages.forEach(p => {
            const unitCost = (p.smsCount > 0) ? (p.price / p.smsCount).toFixed(2) : '0';
            csv += `"${p.name}",${p.smsCount},${p.price},"${unitCost}","${p.productId || ''}","${p.status || 'active'}",${p.sortOrder || 1}\n`;
        });

        const blob = new Blob([csv], { type: 'text/csv' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `HisabNiben_SMS_Packages_Export_${new Date().toISOString().slice(0,10)}.csv`;
        a.click();
    },

    initEventListeners() {
        const searchInput = document.getElementById('search-packages');
        if (searchInput) {
            searchInput.addEventListener('input', Utils.debounce((e) => {
                this.filters.search = e.target.value;
                this.applyFilters();
            }, 300));
        }
    }
};
