/**
 * Enterprise Settings Center Module
 * HisabNiben Admin Panel
 */

const SettingsModule = {
    async init() {
        await this.loadAllSettings();
    },

    async refresh() {
        await this.loadAllSettings();
        if (typeof App !== 'undefined' && App.Toast) App.Toast.success('Settings reloaded from Firestore.');
    },

    /**
     * Load All Firestore Settings Docs
     */
    async loadAllSettings() {
        try {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.show();

            await Promise.all([
                this.loadSupportSettings(),
                this.loadSystemSettings(),
                this.loadLandingPageSettings()
            ]);

            const profile = await Auth.getProfile();
            if (profile) {
                const emailElem = document.getElementById('admin-email-display');
                if (emailElem) emailElem.textContent = profile.email || Auth.getCurrentUser()?.email || '-';

                const loginElem = document.getElementById('last-login-display');
                if (loginElem && profile.lastLogin) {
                    const ts = Utils.parseTimestamp(profile.lastLogin);
                    loginElem.textContent = ts > 0 ? Utils.formatDate(ts) : '-';
                }
            }

            const subtitle = document.getElementById('sync-timestamp-subtitle');
            if (subtitle) {
                subtitle.textContent = `Configure global app options, customer support helpline, trial periods, landing page stats, and database sync options. • Synced ${new Date().toLocaleTimeString()}`;
            }
        } catch (e) {
            console.error('Error loading settings:', e);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.error('Failed to load settings.');
        } finally {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.hide();
        }
    },

    async loadSupportSettings() {
        try {
            const doc = await getDocument('settings', 'support').get();
            if (doc.exists) {
                const d = doc.data();
                this.setVal('setting-support-phone', d.phone || '');
                this.setVal('setting-support-whatsapp', d.whatsapp || '');
                this.setVal('setting-support-email', d.email || '');
                this.setVal('setting-support-hours', d.supportHours || '');
            }
        } catch (e) { console.warn('Error reading settings/support:', e); }
    },

    async loadSystemSettings() {
        try {
            const doc = await getDocument('settings', 'system').get();
            if (doc.exists) {
                const d = doc.data();
                this.setVal('setting-app-name', d.appName || 'HisabNiben');
                this.setVal('setting-currency', d.currency || '৳');
                this.setVal('setting-trial-days', d.trialDurationDays || d.trialDays || 7);
                this.setText('kpi-trial-days', `${d.trialDurationDays || 7} Days`);
            }
        } catch (e) { console.warn('Error reading settings/system:', e); }
    },

    async loadLandingPageSettings() {
        try {
            const doc = await getDocument('settings', 'landingPage').get();
            if (doc.exists) {
                const d = doc.data();
                this.setVal('lp-hero-title', d.heroTitle || '');
                this.setVal('lp-hero-subtitle', d.heroSubtitle || '');
                this.setVal('lp-stats-users', d.statsUsers || '10K+');
                this.setVal('lp-stats-customers', d.statsCustomers || '50K+');
                this.setVal('lp-stats-trans', d.statsTransactions || '1M+');
                this.setVal('lp-stats-rating', d.statsRating || '4.8');
            }
        } catch (e) { console.warn('Error reading settings/landingPage:', e); }
    },

    setVal(id, val) {
        const el = document.getElementById(id);
        if (el) el.value = val;
    },

    setText(id, val) {
        const el = document.getElementById(id);
        if (el) el.textContent = val;
    },

    /**
     * Save Customer Support Settings into Firestore (settings/support)
     */
    async saveSupportSettings(event) {
        if (event) event.preventDefault();

        const phone = document.getElementById('setting-support-phone').value.trim();
        const whatsapp = document.getElementById('setting-support-whatsapp').value.trim();
        const email = document.getElementById('setting-support-email').value.trim();
        const supportHours = document.getElementById('setting-support-hours').value.trim();

        try {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.show();

            const payload = {
                phone,
                whatsapp,
                email,
                supportHours,
                updatedAt: Date.now()
            };

            await getDocument('settings', 'support').set(payload, { merge: true });
            if (typeof App !== 'undefined' && App.Toast) App.Toast.success('Customer Support & Helpline settings saved to Firestore!');
        } catch (e) {
            console.error('Error saving support settings:', e);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.error('Failed to save support settings.');
        } finally {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.hide();
        }
    },

    /**
     * Save System Settings into Firestore (settings/system)
     */
    async saveSystemSettings(event) {
        if (event) event.preventDefault();

        const appName = document.getElementById('setting-app-name').value.trim();
        const currency = document.getElementById('setting-currency').value.trim();
        const trialDurationDays = parseInt(document.getElementById('setting-trial-days').value) || 7;

        try {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.show();

            const payload = {
                appName,
                currency,
                trialDurationDays,
                trialDays: trialDurationDays,
                updatedAt: Date.now()
            };

            await getDocument('settings', 'system').set(payload, { merge: true });
            this.setText('kpi-trial-days', `${trialDurationDays} Days`);

            if (typeof App !== 'undefined' && App.Toast) App.Toast.success('System & Free Trial settings saved to Firestore!');
        } catch (e) {
            console.error('Error saving system settings:', e);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.error('Failed to save system settings.');
        } finally {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.hide();
        }
    },

    /**
     * Save Landing Page Settings into Firestore (settings/landingPage)
     */
    async saveLandingPageSettings(event) {
        if (event) event.preventDefault();

        const heroTitle = document.getElementById('lp-hero-title').value.trim();
        const heroSubtitle = document.getElementById('lp-hero-subtitle').value.trim();
        const statsUsers = document.getElementById('lp-stats-users').value.trim();
        const statsCustomers = document.getElementById('lp-stats-customers').value.trim();
        const statsTransactions = document.getElementById('lp-stats-trans').value.trim();
        const statsRating = document.getElementById('lp-stats-rating').value.trim();

        try {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.show();

            const payload = {
                heroTitle,
                heroSubtitle,
                statsUsers,
                statsCustomers,
                statsTransactions,
                statsRating,
                updatedAt: Date.now()
            };

            await getDocument('settings', 'landingPage').set(payload, { merge: true });
            if (typeof App !== 'undefined' && App.Toast) App.Toast.success('Landing Page settings saved!');
        } catch (e) {
            console.error('Error saving landing page settings:', e);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.error('Failed to save landing page settings.');
        } finally {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.hide();
        }
    },

    /**
     * Send Admin Password Reset Email
     */
    async changePassword() {
        const user = Auth.getCurrentUser();
        const email = user?.email || document.getElementById('admin-email-display').textContent;

        if (!email || email === '-') {
            alert('Unable to retrieve admin email address.');
            return;
        }

        if (!confirm(`Send password reset email to "${email}"?`)) return;

        try {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.show();
            await getAuth().sendPasswordResetEmail(email);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.success(`Password reset email sent to ${email}`);
        } catch (e) {
            console.error('Error sending password reset:', e);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.error('Error sending password reset: ' + e.message);
        } finally {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.hide();
        }
    },

    /**
     * Export Complete Database Backup JSON
     */
    async exportAllData() {
        try {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.show();

            const usersSnap = await getCollection('users').get();
            const users = usersSnap.docs.map(d => ({ id: d.id, ...d.data() }));

            const pkgsSnap = await getCollection('subscription_packages').get();
            const packages = pkgsSnap.docs.map(d => ({ id: d.id, ...d.data() }));

            const backupData = {
                exportTimestamp: new Date().toISOString(),
                usersCount: users.length,
                packagesCount: packages.length,
                users,
                packages
            };

            const blob = new Blob([JSON.stringify(backupData, null, 2)], { type: 'application/json' });
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `HisabNiben_Backup_${new Date().toISOString().slice(0,10)}.json`;
            a.click();

            if (typeof App !== 'undefined' && App.Toast) App.Toast.success('Database backup exported successfully!');
        } catch (e) {
            console.error('Error exporting backup:', e);
            if (typeof App !== 'undefined' && App.Toast) App.Toast.error('Failed to export backup.');
        } finally {
            if (typeof App !== 'undefined' && App.Loader) App.Loader.hide();
        }
    }
};

function saveSupportSettings() { SettingsModule.saveSupportSettings(); }
function saveSystemSettings() { SettingsModule.saveSystemSettings(); }
function saveAppSettings() { SettingsModule.saveSystemSettings(); }
function saveLandingPageSettings() { SettingsModule.saveLandingPageSettings(); }
function changePassword() { SettingsModule.changePassword(); }
function exportAllData() { SettingsModule.exportAllData(); }
