/**
 * Utility Functions
 * HisabNiben Admin Panel
 */

const Utils = {
    /**
     * Format date to readable string
     * @param {number|Date} timestamp
     * @param {string} format
     * @returns {string}
     */
    formatDate(timestamp, format = 'dd/mm/yyyy') {
        if (!timestamp) return '-';

        const date = timestamp instanceof Date ? timestamp : new Date(timestamp);

        if (isNaN(date.getTime())) return '-';

        const day = String(date.getDate()).padStart(2, '0');
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const year = date.getFullYear();
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');

        // Lazy evaluation to prevent infinite recursion
        switch (format) {
            case 'dd/mm/yyyy':
                return `${day}/${month}/${year}`;
            case 'mm/dd/yyyy':
                return `${month}/${day}/${year}`;
            case 'yyyy-mm-dd':
                return `${year}-${month}-${day}`;
            case 'dd mmm yyyy':
                return `${day} ${this.getMonthName(date.getMonth())} ${year}`;
            case 'dd/mm/yyyy hh:mm':
                return `${day}/${month}/${year} ${hours}:${minutes}`;
            case 'relative':
                return this.getRelativeTime(date);
            default:
                return `${day}/${month}/${year}`;
        }
    },

    /**
     * Get month name
     * @param {number} monthIndex
     * @returns {string}
     */
    getMonthName(monthIndex) {
        const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 
                       'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
        return months[monthIndex] || '';
    },

    /**
     * Get relative time (e.g., "2 hours ago")
     * @param {Date} date
     * @returns {string}
     */
    getRelativeTime(date) {
        const now = new Date();
        const diff = now - date;
        const seconds = Math.floor(diff / 1000);
        const minutes = Math.floor(seconds / 60);
        const hours = Math.floor(minutes / 60);
        const days = Math.floor(hours / 24);
        
        if (days > 7) {
            return this.formatDate(date, 'dd mmm yyyy');
        } else if (days > 0) {
            return `${days} day${days > 1 ? 's' : ''} ago`;
        } else if (hours > 0) {
            return `${hours} hour${hours > 1 ? 's' : ''} ago`;
        } else if (minutes > 0) {
            return `${minutes} minute${minutes > 1 ? 's' : ''} ago`;
        } else {
            return 'Just now';
        }
    },

    /**
     * Format currency
     * @param {number} amount
     * @returns {string}
     */
    formatCurrency(amount) {
        if (amount === null || amount === undefined) return '৳0';
        const currency = APP_CONFIG?.app?.currency || '৳';
        return `${currency}${Number(amount).toLocaleString('en-IN')}`;
    },

    /**
     * Format number with commas
     * @param {number} num
     * @returns {string}
     */
    formatNumber(num) {
        if (num === null || num === undefined) return '0';
        return Number(num).toLocaleString('en-IN');
    },

    /**
     * Parse any timestamp, Firestore Timestamp, Date or ISO string into milliseconds
     * @param {any} ts
     * @returns {number}
     */
    parseTimestamp(ts) {
        if (!ts) return 0;
        if (typeof ts === 'number') return ts;
        if (ts.seconds) return ts.seconds * 1000;
        if (typeof ts.toDate === 'function') return ts.toDate().getTime();
        if (ts instanceof Date) return ts.getTime();
        const parsed = new Date(ts).getTime();
        return isNaN(parsed) ? 0 : parsed;
    },

    /**
     * Truncate text
     * @param {string} text
     * @param {number} length
     * @returns {string}
     */
    truncateText(text, length = 50) {
        if (!text) return '';
        if (text.length <= length) return text;
        return text.substring(0, length) + '...';
    },

    /**
     * Get initials from name
     * @param {string} name
     * @returns {string}
     */
    getInitials(name) {
        if (!name) return '?';
        const words = name.trim().split(' ');
        if (words.length === 1) {
            return words[0].charAt(0).toUpperCase();
        }
        return (words[0].charAt(0) + words[words.length - 1].charAt(0)).toUpperCase();
    },

    /**
     * Validate email
     * @param {string} email
     * @returns {boolean}
     */
    isValidEmail(email) {
        const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return re.test(email);
    },

    /**
     * Validate phone (Bangladesh format)
     * @param {string} phone
     * @returns {boolean}
     */
    isValidPhone(phone) {
        const re = /^(\+88)?01[3-9]\d{8}$/;
        return re.test(phone.replace(/\s/g, ''));
    },

    /**
     * Generate random ID
     * @param {number} length
     * @returns {string}
     */
    generateId(length = 10) {
        const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
        let result = '';
        for (let i = 0; i < length; i++) {
            result += chars.charAt(Math.floor(Math.random() * chars.length));
        }
        return result;
    },

    /**
     * Debounce function
     * @param {Function} func
     * @param {number} wait
     * @returns {Function}
     */
    debounce(func, wait = 300) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    },

    /**
     * Deep clone object
     * @param {Object} obj
     * @returns {Object}
     */
    deepClone(obj) {
        return JSON.parse(JSON.stringify(obj));
    },

    /**
     * Check if object is empty
     * @param {Object} obj
     * @returns {boolean}
     */
    isEmpty(obj) {
        if (obj === null || obj === undefined) return true;
        if (Array.isArray(obj)) return obj.length === 0;
        if (typeof obj === 'object') return Object.keys(obj).length === 0;
        if (typeof obj === 'string') return obj.trim() === '';
        return false;
    },

    /**
     * Get URL parameter
     * @param {string} name
     * @returns {string|null}
     */
    getUrlParam(name) {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.get(name);
    },

    /**
     * Copy text to clipboard
     * @param {string} text
     * @returns {Promise<boolean>}
     */
    async copyToClipboard(text) {
        try {
            await navigator.clipboard.writeText(text);
            return true;
        } catch (err) {
            console.error('Failed to copy:', err);
            return false;
        }
    },

    /**
     * Calculate days between dates
     * @param {Date|string|number} date1
     * @param {Date|string|number} date2
     * @returns {number}
     */
    daysBetween(date1, date2) {
        const d1 = new Date(date1);
        const d2 = new Date(date2);
        const diff = Math.abs(d2 - d1);
        return Math.ceil(diff / (1000 * 60 * 60 * 24));
    },

    /**
     * Get status badge HTML
     * @param {string} status
     * @returns {string}
     */
    getStatusBadge(status) {
        const badges = {
            'premium': '<span class="badge badge-premium">Premium</span>',
            'trial': '<span class="badge badge-trial">Trial</span>',
            'expired': '<span class="badge badge-expired">Expired</span>',
            'active': '<span class="badge badge-active">Active</span>',
            'inactive': '<span class="badge badge-inactive">Inactive</span>',
            'payment': '<span class="badge badge-payment">Payment</span>',
            'due': '<span class="badge badge-due">Due</span>'
        };
        return badges[status?.toLowerCase()] || `<span class="badge">${status || 'Unknown'}</span>`;
    },

    /**
     * Escape HTML to prevent XSS
     * @param {string} str
     * @returns {string}
     */
    escapeHtml(str) {
        if (!str) return '';
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    },

    /**
     * Download data as CSV
     * @param {Array} data
     * @param {string} filename
     * @param {Array} headers
     */
    downloadCSV(data, filename, headers = null) {
        if (!data || data.length === 0) {
            Toast.show('No data to export', 'warning');
            return;
        }

        const csvHeaders = headers || Object.keys(data[0]);
        const csvRows = [
            csvHeaders.join(','),
            ...data.map(row => 
                csvHeaders.map(header => {
                    let cell = row[header] ?? '';
                    if (typeof cell === 'string' && (cell.includes(',') || cell.includes('"'))) {
                        cell = `"${cell.replace(/"/g, '""')}"`;
                    }
                    return cell;
                }).join(',')
            )
        ];

        const blob = new Blob([csvRows.join('\n')], { type: 'text/csv' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        a.click();
        window.URL.revokeObjectURL(url);
    }
};

/**
 * Global Admin Session Cache Manager
 * Caches data during tab navigation so pages load instantly without re-fetching from Firestore.
 * Clears cache automatically when the user reloads the page (F5 / Refresh button).
 */
const AppCache = {
    prefix: 'hisabniben_admin_cache_',
    
    get(key) {
        try {
            const data = sessionStorage.getItem(this.prefix + key);
            return data ? JSON.parse(data) : null;
        } catch (e) {
            console.warn('AppCache get error:', e);
            return null;
        }
    },

    set(key, data) {
        try {
            sessionStorage.setItem(this.prefix + key, JSON.stringify(data));
        } catch (e) {
            console.warn('AppCache set error:', e);
        }
    },

    clear(key) {
        try {
            if (key) {
                sessionStorage.removeItem(this.prefix + key);
            } else {
                Object.keys(sessionStorage).forEach(k => {
                    if (k.startsWith(this.prefix)) {
                        sessionStorage.removeItem(k);
                    }
                });
                console.log('AppCache cleared.');
            }
        } catch (e) {
            console.warn('AppCache clear error:', e);
        }
    }
};

// Auto clear cache on manual page reload/F5 so fresh data is fetched!
window.addEventListener('beforeunload', () => {
    try {
        if (performance.getEntriesByType && performance.getEntriesByType('navigation')[0]) {
            const navType = performance.getEntriesByType('navigation')[0].type;
            if (navType === 'reload') {
                AppCache.clear();
            }
        }
    } catch (e) {}
});
