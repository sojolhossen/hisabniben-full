/**
 * Master Enterprise Subscription & Package Control Center Module
 * HisabNiben Admin Panel
 */

const PackagesModule = {
    packagesData: [],
    filteredPackages: [],
    editingId: null,
    isGridView: false,
    filters: {
        search: '',
        type: 'all',
        status: 'all',
        duration: 'all'
    },

    /**
     * Initialize Package Module
     */
    async init() {
        await this.loadPackages();
        this.initEventListeners();
    },

    /**
     * Load Packages from Firestore
     */
    async loadPackages() {
        try {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.show();

            const pkgRef = getCollection(APP_CONFIG.collections.packages || 'subscription_packages');
            if (pkgRef) {
                const snap = await pkgRef.get();
                this.packagesData = snap.docs.map(doc => ({
                    id: doc.id,
                    ...doc.data()
                }));
            }

            // Sort by sortOrder or createdAt
            this.packagesData.sort((a, b) => (a.sortOrder || 99) - (b.sortOrder || 99));

            this.updateStats();
            this.applyFilters();

            const subtitle = document.getElementById('sync-timestamp-subtitle');
            if (subtitle) {
                subtitle.textContent = `Control app subscription plans, SMS bundles, pricing, and live Firestore sync. • Synced ${new Date().toLocaleTimeString()}`;
            }
        } catch (e) {
            console.error('Error loading packages:', e);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.error('Failed to load packages');
        } finally {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.hide();
        }
    },

    /**
     * Refresh Packages
     */
    async refresh() {
        await this.loadPackages();
        if (typeof App !== 'undefined' && App.Toast) App.Toast.success('Packages refreshed');
    },

    /**
     * Update 6 KPI Summary Cards
     */
    updateStats() {
        const total = this.packagesData.length;
        const active = this.packagesData.filter(p => p.status === 'active' || p.isActive === true).length;
        const subCount = this.packagesData.filter(p => p.type === 'subscription' || !p.type).length;
        const smsCount = this.packagesData.filter(p => p.type === 'sms').length;
        const featured = this.packagesData.filter(p => p.badgeText || p.isFeatured).length;

        this.setElemText('total-packages-count', total);
        this.setElemText('active-packages-count', active);
        this.setElemText('subscription-count', subCount);
        this.setElemText('sms-count', smsCount);
        this.setElemText('featured-count', featured);
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
            if (searchVal) {
                const name = (p.name || '').toLowerCase();
                const pid = (p.productId || p.playStoreProductId || '').toLowerCase();
                const desc = (p.description || '').toLowerCase();
                const badge = (p.badgeText || '').toLowerCase();
                const price = (p.price || '').toString();
                if (!name.includes(searchVal) && !pid.includes(searchVal) && !desc.includes(searchVal) && !badge.includes(searchVal) && !price.includes(searchVal)) return false;
            }

            if (this.filters.type !== 'all') {
                const pType = p.type || 'subscription';
                if (pType !== this.filters.type) return false;
            }

            if (this.filters.status !== 'all') {
                const isActive = p.status === 'active' || p.isActive === true;
                if (this.filters.status === 'active' && !isActive) return false;
                if (this.filters.status === 'inactive' && isActive) return false;
            }

            if (this.filters.duration !== 'all') {
                const dur = p.durationDays || 30;
                if (this.filters.duration === 'lifetime' && dur < 999) return false;
                if (this.filters.duration !== 'lifetime' && dur !== parseInt(this.filters.duration)) return false;
            }

            return true;
        });

        this.renderView();
        this.renderFilterChips();
    },

    applyTypeFilter(type) {
        this.filters.type = type;
        const select = document.getElementById('filter-type');
        if (select) select.value = type;
        this.applyFilters();
    },

    applyStatusFilter(status) {
        this.filters.status = status;
        const select = document.getElementById('filter-status');
        if (select) select.value = status;
        this.applyFilters();
    },

    resetFilters() {
        this.filters = { search: '', type: 'all', status: 'all', duration: 'all' };
        const searchInput = document.getElementById('search-packages');
        if (searchInput) searchInput.value = '';
        ['filter-type', 'filter-status', 'filter-duration'].forEach(id => {
            const el = document.getElementById(id);
            if (el) el.value = 'all';
        });
        this.applyFilters();
    },

    renderFilterChips() {
        const container = document.getElementById('active-filter-chips');
        if (!container) return;

        const chips = [];
        if (this.filters.type !== 'all') chips.push(`Type: ${this.filters.type.toUpperCase()}`);
        if (this.filters.status !== 'all') chips.push(`Status: ${this.filters.status.toUpperCase()}`);
        if (this.filters.duration !== 'all') chips.push(`Duration: ${this.filters.duration}`);

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
        const tbody = document.getElementById('packages-tbody');
        if (!tbody) return;

        if (this.filteredPackages.length === 0) {
            tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; padding:30px; color:var(--gray-400);">No packages match the current filter.</td></tr>`;
            return;
        }

        tbody.innerHTML = this.filteredPackages.map((p, idx) => {
            const isActive = p.status === 'active' || p.isActive === true;
            const statusBadge = isActive ? '<span class="status-badge approved">ACTIVE</span>' : '<span class="status-badge pending">INACTIVE</span>';
            const badgeText = p.badgeText ? `<span class="badge" style="background:var(--primary-light); color:var(--primary); font-size:10px; padding:2px 6px; border-radius:4px; margin-left:6px;">${Utils.escapeHtml(p.badgeText)}</span>` : '';
            const durationText = p.durationDays >= 999 ? 'Life-Time' : `${p.durationDays || 30} Days`;
            const priceText = `৳${(p.price || 0).toLocaleString()}`;
            const feats = Array.isArray(p.features) ? p.features.slice(0, 3).join(', ') + (p.features.length > 3 ? '...' : '') : 'Default features';

            return `
                <tr>
                    <td>${idx + 1}</td>
                    <td>
                        <strong style="color:var(--dark);">${Utils.escapeHtml(p.name || 'Unnamed')}</strong> ${badgeText}
                    </td>
                    <td><strong style="color:var(--primary);">${priceText}</strong></td>
                    <td>${durationText}</td>
                    <td><span style="font-size:12px; color:var(--gray-500);">${Utils.escapeHtml(feats)}</span></td>
                    <td><code>${Utils.escapeHtml(p.productId || p.playStoreProductId || '-')}</code></td>
                    <td>${statusBadge}</td>
                    <td>
                        <div style="display:flex; gap:6px;">
                            <button class="btn btn-sm users-btn-outline" onclick="PackagesModule.openEditModal('${p.id}')" title="Edit" style="padding:4px 8px; font-size:11px;">
                                <i class="fas fa-edit"></i>
                            </button>
                            <button class="btn btn-sm users-btn-outline" onclick="PackagesModule.duplicatePackage('${p.id}')" title="Duplicate" style="padding:4px 8px; font-size:11px;">
                                <i class="fas fa-copy"></i>
                            </button>
                            <button class="btn btn-sm users-btn-outline" onclick="PackagesModule.togglePackageStatus('${p.id}')" title="Toggle Status" style="padding:4px 8px; font-size:11px;">
                                <i class="fas fa-power-off"></i>
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
            container.innerHTML = `<div style="grid-column:1/-1; text-align:center; padding:40px; color:var(--gray-400);">No packages match current filter.</div>`;
            return;
        }

        container.innerHTML = this.filteredPackages.map(p => {
            const isActive = p.status === 'active' || p.isActive === true;
            const durationText = p.durationDays >= 999 ? 'Life-Time' : `${p.durationDays || 30} Days`;
            return `
                <div class="user-grid-card" style="border-top:3px solid var(--primary);">
                    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:10px;">
                        <h4 style="font-size:16px; font-weight:800; color:var(--dark); margin:0;">${Utils.escapeHtml(p.name)}</h4>
                        <span class="status-badge ${isActive ? 'approved' : 'pending'}">${isActive ? 'ACTIVE' : 'INACTIVE'}</span>
                    </div>
                    <div style="font-size:24px; font-weight:900; color:var(--primary); margin-bottom:6px;">৳${(p.price || 0).toLocaleString()}</div>
                    <div style="font-size:12px; color:var(--gray-500); margin-bottom:14px;">${durationText} Access</div>
                    <div class="user-grid-footer">
                        <button class="btn btn-sm users-btn-outline" onclick="PackagesModule.duplicatePackage('${p.id}')"><i class="fas fa-copy"></i> Clone</button>
                        <button class="btn btn-sm users-btn-primary" onclick="PackagesModule.openEditModal('${p.id}')"><i class="fas fa-edit"></i> Edit</button>
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
        document.getElementById('package-modal-title').innerHTML = '<i class="fas fa-plus-circle"></i> Configure New Subscription Package';
        document.getElementById('package-form').reset();
        document.getElementById('package-id').value = '';

        const modal = document.getElementById('package-modal');
        if (modal) {
            modal.classList.add('active');
            modal.classList.add('show');
        }
        this.onPackageTypeChange();
        this.updateLivePreview();
    },

    openEditModal(id) {
        const p = this.packagesData.find(item => item.id === id);
        if (!p) return;

        this.editingId = id;
        document.getElementById('package-modal-title').innerHTML = '<i class="fas fa-edit"></i> Edit Subscription Package';

        document.getElementById('package-id').value = p.id;
        document.getElementById('package-name').value = p.name || '';
        document.getElementById('package-type').value = p.type || 'subscription';
        document.getElementById('package-price').value = p.price || '';
        document.getElementById('package-original-price').value = p.originalPrice || '';
        document.getElementById('package-duration').value = p.durationDays || 30;
        document.getElementById('package-product-id').value = p.productId || p.playStoreProductId || '';
        document.getElementById('package-badge').value = p.badgeText || '';
        document.getElementById('package-description').value = p.description || '';
        if (document.getElementById('package-sms-credits')) {
            document.getElementById('package-sms-credits').value = p.smsCount || p.smsLimit || p.smsCredits || '';
        }
        document.getElementById('package-sort').value = p.sortOrder || 1;
        document.getElementById('package-status').value = (p.status === 'active' || p.isActive) ? 'active' : 'inactive';

        // Check features
        const featList = p.features || [];
        document.querySelectorAll('input[name="features"]').forEach(cb => {
            cb.checked = featList.includes(cb.value);
        });

        const modal = document.getElementById('package-modal');
        if (modal) {
            modal.classList.add('active');
            modal.classList.add('show');
        }
        this.onPackageTypeChange();
        this.updateLivePreview();
    },

    closeModal() {
        const modal = document.getElementById('package-modal');
        if (modal) {
            modal.classList.remove('active');
            modal.classList.remove('show');
        }
    },

    setDurationPreset(days) {
        const input = document.getElementById('package-duration');
        if (input) {
            input.value = days;
            this.updateLivePreview();
        }
    },

    onPackageTypeChange() {
        const typeSelect = document.getElementById('package-type');
        const smsContainer = document.getElementById('sms-credits-container');
        if (!typeSelect || !smsContainer) return;

        if (typeSelect.value === 'sms') {
            smsContainer.style.display = 'block';
        } else {
            smsContainer.style.display = 'none';
        }
    },

    /**
     * Live Preview Updater for Android App Card
     */
    updateLivePreview() {
        const name = document.getElementById('package-name').value || 'Package Name';
        const price = parseFloat(document.getElementById('package-price').value) || 0;
        const origPrice = parseFloat(document.getElementById('package-original-price').value) || 0;
        const days = parseInt(document.getElementById('package-duration').value) || 30;
        const badge = document.getElementById('package-badge').value || 'POPULAR';

        this.setElemText('preview-name', name);
        this.setElemText('preview-duration', days >= 999 ? 'Life-Time Access' : `${days} Days Access`);
        this.setElemText('preview-price', `৳${price.toLocaleString()}`);

        const origElem = document.getElementById('preview-original');
        const discElem = document.getElementById('preview-discount');
        if (origPrice > price) {
            if (origElem) { origElem.style.display = 'inline'; origElem.textContent = `৳${origPrice.toLocaleString()}`; }
            const discountPct = Math.round(((origPrice - price) / origPrice) * 100);
            if (discElem) { discElem.style.display = 'inline-block'; discElem.textContent = `SAVE ${discountPct}%`; }
        } else {
            if (origElem) origElem.style.display = 'none';
            if (discElem) discElem.style.display = 'none';
        }

        const badgeElem = document.getElementById('preview-badge');
        if (badgeElem) badgeElem.textContent = badge.toUpperCase();

        // Features preview
        const selectedFeats = [];
        document.querySelectorAll('input[name="features"]:checked').forEach(cb => selectedFeats.push(cb.value));

        const featsListElem = document.getElementById('preview-features-list');
        if (featsListElem) {
            featsListElem.innerHTML = selectedFeats.map(f => `<div class="android-sub-feature-item"><i class="fas fa-check-circle"></i> ${Utils.escapeHtml(f)}</div>`).join('');
        }
    },

    /**
     * Save Package to Firestore
     */
    async savePackage(event) {
        event.preventDefault();

        const name = document.getElementById('package-name').value.trim();
        const type = document.getElementById('package-type').value;
        const price = parseFloat(document.getElementById('package-price').value) || 0;
        const originalPrice = parseFloat(document.getElementById('package-original-price').value) || 0;
        const durationDays = parseInt(document.getElementById('package-duration').value) || 30;
        const productId = document.getElementById('package-product-id').value.trim();
        const badgeText = document.getElementById('package-badge').value.trim();
        const description = document.getElementById('package-description').value.trim();
        const smsCount = parseInt(document.getElementById('package-sms-credits') ? document.getElementById('package-sms-credits').value : 0) || 0;
        const sortOrder = parseInt(document.getElementById('package-sort').value) || 1;
        const status = document.getElementById('package-status').value;

        const features = [];
        document.querySelectorAll('input[name="features"]:checked').forEach(cb => features.push(cb.value));

        if (!name || price < 0) {
            alert('Please provide valid package name and price.');
            return;
        }

        try {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.show();

            const pkgData = {
                name,
                type,
                price,
                originalPrice,
                durationDays,
                productId,
                playStoreProductId: productId,
                badgeText,
                description,
                smsCount,
                smsLimit: smsCount,
                sortOrder,
                status,
                isActive: status === 'active',
                isPublished: status === 'active',
                features,
                updatedAt: Date.now()
            };

            const pkgRef = getCollection(APP_CONFIG.collections.packages || 'subscription_packages');

            if (this.editingId) {
                await pkgRef.doc(this.editingId).update(pkgData);
                if (typeof App !== 'undefined' && App.Toast) App.Toast.success('Package updated successfully!');
            } else {
                pkgData.createdAt = Date.now();
                await pkgRef.add(pkgData);
                if (typeof App !== 'undefined' && App.Toast) App.Toast.success('New package created!');
            }

            this.closeModal();
            await this.loadPackages();
        } catch (e) {
            console.error('Error saving package:', e);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.error('Failed to save package');
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
                name: `${p.name} (Copy)`,
                createdAt: Date.now(),
                updatedAt: Date.now()
            };
            delete copyData.id;

            const pkgRef = getCollection(APP_CONFIG.collections.packages || 'subscription_packages');
            await pkgRef.add(copyData);

            if (typeof App !== 'undefined' && App.Toast) App.Toast.success('Package duplicated successfully!');
            await this.loadPackages();
        } catch (e) {
            console.error('Error duplicating package:', e);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.error('Failed to duplicate package');
        } finally {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.hide();
        }
    },

    /**
     * Toggle Package Status
     */
    async togglePackageStatus(id) {
        const p = this.packagesData.find(item => item.id === id);
        if (!p) return;

        const newStatus = (p.status === 'active' || p.isActive) ? 'inactive' : 'active';

        try {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.show();

            const pkgRef = getCollection(APP_CONFIG.collections.packages || 'subscription_packages');
            await pkgRef.doc(id).update({
                status: newStatus,
                isActive: newStatus === 'active',
                updatedAt: Date.now()
            });

            if (typeof App !== 'undefined' && App.Toast) App.Toast.success(`Package status set to ${newStatus.toUpperCase()}`);
            await this.loadPackages();
        } catch (e) {
            console.error('Error toggling package status:', e);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.error('Failed to update status');
        } finally {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.hide();
        }
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
