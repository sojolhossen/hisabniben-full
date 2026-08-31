/**
 * Main Application Module
 * HisabNiben Admin Panel
 */

/**
 * Toast Notification System
 */
const Toast = {
    container: null,

    init() {
        this.container = document.getElementById('toast-container');
        if (!this.container) {
            this.container = document.createElement('div');
            this.container.id = 'toast-container';
            this.container.className = 'toast-container';
            document.body.appendChild(this.container);
        }
    },

    /**
     * Show toast notification
     * @param {string} message
     * @param {string} type - success, error, warning, info
     * @param {number} duration
     */
    show(message, type = 'success', duration = null) {
        if (!this.container) this.init();

        const icons = {
            success: 'fa-check-circle',
            error: 'fa-times-circle',
            warning: 'fa-exclamation-triangle',
            info: 'fa-info-circle'
        };

        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.innerHTML = `
            <i class="fas ${icons[type] || icons.info}"></i>
            <span class="toast-message">${Utils.escapeHtml(message)}</span>
            <button class="toast-close" onclick="this.parentElement.remove()">
                <i class="fas fa-times"></i>
            </button>
        `;

        this.container.appendChild(toast);

        // Auto remove
        const time = duration || APP_CONFIG?.toast?.[type] || 3000;
        setTimeout(() => {
            toast.style.animation = 'slideIn 0.3s ease reverse';
            setTimeout(() => toast.remove(), 300);
        }, time);
    },

    success(message, duration) {
        this.show(message, 'success', duration);
    },

    error(message, duration) {
        this.show(message, 'error', duration);
    },

    warning(message, duration) {
        this.show(message, 'warning', duration);
    },

    info(message, duration) {
        this.show(message, 'info', duration);
    }
};

/**
 * Loader/Spinner System
 */
const Loader = {
    overlay: null,

    init() {
        this.overlay = document.getElementById('loader-overlay');
        if (!this.overlay) {
            this.overlay = document.createElement('div');
            this.overlay.id = 'loader-overlay';
            this.overlay.className = 'loader-overlay';
            this.overlay.innerHTML = '<div class="loader"></div>';
            document.body.appendChild(this.overlay);
        }
    },

    show() {
        if (!this.overlay) this.init();
        this.overlay.classList.add('show');
    },

    hide() {
        if (this.overlay) {
            this.overlay.classList.remove('show');
        }
    }
};

/**
 * Modal System
 */
const Modal = {
    /**
     * Show modal by ID
     * @param {string} modalId
     */
    show(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.classList.add('show');
            document.body.style.overflow = 'hidden';
        }
    },

    /**
     * Hide modal by ID
     * @param {string} modalId
     */
    hide(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.classList.remove('show');
            document.body.style.overflow = '';
        }
    },

    /**
     * Hide all modals
     */
    hideAll() {
        document.querySelectorAll('.modal-overlay.show').forEach(modal => {
            modal.classList.remove('show');
        });
        document.body.style.overflow = '';
    }
};

/**
 * Sidebar Management
 */
const Sidebar = {
    sidebar: null,
    overlay: null,
    toggleBtn: null,

    init() {
        this.sidebar = document.getElementById('sidebar');
        this.overlay = document.getElementById('sidebar-overlay');
        this.toggleBtn = document.getElementById('menu-toggle');

        if (this.toggleBtn) {
            this.toggleBtn.addEventListener('click', () => this.toggle());
        }

        if (this.overlay) {
            this.overlay.addEventListener('click', () => this.close());
        }

        // Close sidebar on nav link click (mobile)
        if (this.sidebar) {
            this.sidebar.querySelectorAll('.nav-link').forEach(link => {
                link.addEventListener('click', () => {
                    if (window.innerWidth < 992) {
                        this.close();
                    }
                });
            });
        }

        // Set active nav link
        this.setActiveLink();
    },

    toggle() {
        if (this.sidebar) {
            this.sidebar.classList.toggle('show');
        }
        if (this.overlay) {
            this.overlay.classList.toggle('show');
        }
    },

    open() {
        if (this.sidebar) {
            this.sidebar.classList.add('show');
        }
        if (this.overlay) {
            this.overlay.classList.add('show');
        }
    },

    close() {
        if (this.sidebar) {
            this.sidebar.classList.remove('show');
        }
        if (this.overlay) {
            this.overlay.classList.remove('show');
        }
    },

    setActiveLink() {
        const currentPage = window.location.pathname.split('/').pop() || 'index.html';
        
        document.querySelectorAll('.sidebar-nav .nav-link').forEach(link => {
            link.classList.remove('active');
            const href = link.getAttribute('href');
            if (href === currentPage || (currentPage === '' && href === 'index.html')) {
                link.classList.add('active');
            }
        });
    }
};

/**
 * Load Component (HTML Include)
 */
async function loadComponent(elementId, componentPath) {
    try {
        const response = await fetch(componentPath);
        if (!response.ok) throw new Error('Component not found');
        
        const html = await response.text();
        const element = document.getElementById(elementId);
        
        if (element) {
            element.innerHTML = html;
            
            // Re-initialize sidebar after loading
            if (componentPath.includes('sidebar')) {
                Sidebar.init();
            }
        }
        
        return true;
    } catch (error) {
        console.error('Error loading component:', error);
        return false;
    }
}

/**
 * Initialize common modules
 */
function initCommonModules() {
    // Initialize Toast
    Toast.init();
    
    // Initialize Loader
    Loader.init();
    
    // Initialize Sidebar
    Sidebar.init();
    
    // Close modals on overlay click
    document.querySelectorAll('.modal-overlay').forEach(overlay => {
        overlay.addEventListener('click', (e) => {
            if (e.target === overlay) {
                overlay.classList.remove('show');
                document.body.style.overflow = '';
            }
        });
    });
    
    // Close modals on escape key
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            Modal.hideAll();
        }
    });
}

/**
 * Update user info in sidebar
 */
function updateUserInfo(userData) {
    const adminName = document.getElementById('admin-name');
    const adminAvatar = document.getElementById('admin-avatar');
    
    if (adminName && userData) {
        adminName.textContent = userData.name || userData.email || 'Admin';
    }
    
    if (adminAvatar && userData) {
        const initials = Utils.getInitials(userData.name || userData.email || 'A');
        adminAvatar.textContent = initials;
    }
}

/**
 * Initialize page
 */
document.addEventListener('DOMContentLoaded', () => {
    initCommonModules();
});

// Export for use in other modules
window.App = {
    Toast,
    Loader,
    Modal,
    Sidebar,
    loadComponent,
    updateUserInfo
};
