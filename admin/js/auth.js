/**
 * Authentication Module
 * HisabNiben Admin Panel
 */

const Auth = {
    currentUser: null,
    isAdmin: false,

    /**
     * Initialize authentication state listener
     */
    init() {
        return new Promise((resolve) => {
            // Check sessionStorage first
            const storedUser = sessionStorage.getItem('adminUser');
            if (storedUser) {
                try {
                    const userData = JSON.parse(storedUser);
                    if (userData.isAdmin) {
                        this.currentUser = userData;
                        this.isAdmin = true;
                        resolve({ user: userData, isAdmin: true });
                        return;
                    }
                } catch (e) {
                    sessionStorage.removeItem('adminUser');
                }
            }
            
            const auth = getAuth();
            
            auth.onAuthStateChanged(async (user) => {
                if (user) {
                    const adminStatus = await this.checkAdminStatus(user.uid);
                    
                    if (adminStatus) {
                        this.currentUser = user;
                        this.isAdmin = true;
                        this.onAuthSuccess(user);
                        resolve({ user, isAdmin: true });
                    } else {
                        this.signOut();
                        resolve({ user: null, isAdmin: false, error: 'Not an admin' });
                    }
                } else {
                    this.currentUser = null;
                    this.isAdmin = false;
                    this.onAuthLogout();
                    resolve({ user: null, isAdmin: false });
                }
            });
        });
    },

    /**
     * Check if user has admin privileges
     * @param {string} userId
     * @returns {Promise<boolean>}
     */
    async checkAdminStatus(userId) {
        try {
            const userData = await getUserData(userId);
            return userData && userData.isAdmin === true;
        } catch (error) {
            console.error('Error checking admin status:', error);
            return false;
        }
    },

    /**
     * Login with email/phone and password
     * @param {string} identifier
     * @param {string} password
     * @returns {Promise<Object>}
     */
    async login(identifier, password) {
        try {
            Loader.show();
            
            const auth = getAuth();
            const db = getFirestore();
            
            let userRecord = null;
            
            console.log('Login attempt with:', identifier);
            
            // Check if input is phone number (flexible matching)
            const cleanInput = identifier.replace(/\s/g, '');
            
            // Simple phone check - contains only digits (with optional + at start)
            const isPhoneInput = /^\+?\d+$/.test(cleanInput) && cleanInput.length >= 11;
            
            if (isPhoneInput) {
                // Normalize phone number to 880 format (as stored in Firestore)
                let phone = cleanInput.replace(/^\+88/, '').replace(/^88/, '');
                // Remove leading 0 if present
                if (phone.startsWith('0')) {
                    phone = phone.substring(1);
                }
                if (!phone.startsWith('880')) {
                    phone = '880' + phone;
                }
                
                console.log('Searching for phone:', phone);
                
                // Try different phone formats
                const searchFormats = [
                    phone,
                    phone.replace(/^880/, ''),
                    '0' + phone.replace(/^880/, '')
                ];
                
                console.log('Search formats:', searchFormats);
                
                for (const format of searchFormats) {
                    try {
                        console.log('Querying for:', format);
                        const userQuery = await db.collection(APP_CONFIG.collections.users)
                            .where('phone', '==', format)
                            .limit(1)
                            .get();
                        
                        console.log('Query result size:', userQuery.size);
                        
                        if (!userQuery.empty) {
                            const userDoc = userQuery.docs[0];
                            userRecord = { id: userDoc.id, ...userDoc.data() };
                            console.log('Found user:', userRecord.name, 'isAdmin:', userRecord.isAdmin);
                            break;
                        }
                    } catch (queryError) {
                        console.log('Query error:', queryError.message);
                        if (queryError.code === 'permission-denied') {
                            throw new Error('Permission denied. Please deploy firestore rules.');
                        }
                    }
                }
                
                if (!userRecord) {
                    // Last try - get all users and filter manually
                    console.log('Trying to get all users...');
                    try {
                        const allUsers = await db.collection(APP_CONFIG.collections.users).limit(50).get();
                        console.log('Total users found:', allUsers.size);
                        
                        for (const doc of allUsers.docs) {
                            const data = doc.data();
                            console.log('User phone:', data.phone);
                        }
                    } catch (e) {
                        console.log('Error getting all users:', e.message);
                    }
                    
                    throw new Error('User not found with this phone number');
                }
            } else {
                // Email login - use Firebase Auth
                try {
                    const result = await auth.signInWithEmailAndPassword(identifier, password);
                    userRecord = await getUserData(result.user.uid);
                } catch (authError) {
                    if (authError.code === 'auth/invalid-email') {
                        throw new Error('Invalid input. Please enter a valid phone number or email address');
                    }
                    throw authError;
                }
            }
            
            // Check if we found user via phone (need to verify the isPhone status)
            const isPhoneLogin = userRecord && userRecord.phone && userRecord.phone.startsWith('880');
            
            console.log('=== USER RECORD ===');
            console.log(JSON.stringify(userRecord, null, 2));
            console.log('isPhoneLogin:', isPhoneLogin);
            
            if (!userRecord) {
                throw new Error('User data not found');
            }
            
            // Check admin status - handle both boolean and string
            let isAdmin = false;
            if (typeof userRecord.isAdmin !== 'undefined') {
                isAdmin = userRecord.isAdmin === true || userRecord.isAdmin === 'true' || userRecord.isAdmin === '1';
            }
            console.log('=== ADMIN CHECK ===');
            console.log('isAdmin value:', userRecord.isAdmin);
            console.log('isAdmin type:', typeof userRecord.isAdmin);
            console.log('isAdmin result:', isAdmin);
            
            if (!isAdmin) {
                if (auth.currentUser) {
                    await auth.signOut();
                }
                throw new Error('Access denied. Admin privileges required.');
            }
            
            // For phone login, verify password from Firestore
            if (isPhoneLogin) {
                // Check password
                if (userRecord.password !== password) {
                    throw new Error('Wrong password');
                }
                
                // Store in session
                sessionStorage.setItem('adminUser', JSON.stringify({
                    uid: userRecord.id,
                    email: userRecord.email,
                    phone: userRecord.phone,
                    isAdmin: true
                }));
                
                this.currentUser = { uid: userRecord.id, email: userRecord.email, phone: userRecord.phone };
                this.isAdmin = true;
                
                Toast.show('Login successful!', 'success');
                return { success: true, user: this.currentUser };
            } else {
                // Store in session
                sessionStorage.setItem('adminUser', JSON.stringify({
                    uid: auth.currentUser.uid,
                    email: auth.currentUser.email,
                    isAdmin: true
                }));
                
                this.currentUser = auth.currentUser;
                this.isAdmin = true;
                
                Toast.show('Login successful!', 'success');
                return { success: true, user: auth.currentUser };
            }
        } catch (error) {
            console.error('Login error:', error);
            console.error('Error message:', error.message);
            
            let message = 'Login failed';
            if (error.code === 'auth/user-not-found') {
                message = 'User not found';
            } else if (error.code === 'auth/wrong-password') {
                message = 'Wrong password';
            } else if (error.code === 'auth/invalid-email') {
                message = 'Invalid input. Please enter phone number or valid email';
            } else if (error.code === 'auth/too-many-requests') {
                message = 'Too many attempts. Please try again later.';
            } else if (error.code === 'permission-denied' || error.message.includes('permission')) {
                message = 'Permission denied. Rules may need updating.';
            } else if (error.message.includes('Access denied')) {
                message = error.message;
            } else if (error.message.includes('not found') || error.message.includes('User not found')) {
                message = 'User not found with this phone number';
            } else if (error.message.includes('Wrong password')) {
                message = 'Wrong password';
            } else if (error.message.includes('Invalid input')) {
                message = error.message;
            }
            
            Toast.show(message, 'error');
            return { success: false, error: message };
        } finally {
            Loader.hide();
        }
    },

    /**
     * Sign out current user
     * @returns {Promise<void>}
     */
    async signOut() {
        try {
            // Clear session storage first
            sessionStorage.removeItem('adminUser');
            
            const auth = getAuth();
            try {
                await auth.signOut();
            } catch (e) {
                // Auth might not be signed in, ignore error
            }
            
            this.currentUser = null;
            this.isAdmin = false;
            
            Toast.show('Logged out successfully', 'info');
        } catch (error) {
            console.error('Sign out error:', error);
            Toast.show('Error signing out', 'error');
        }
    },

    /**
     * Get current user
     * @returns {Object|null}
     */
    getCurrentUser() {
        return this.currentUser;
    },

    /**
     * Get current user ID
     * @returns {string|null}
     */
    getUserId() {
        return this.currentUser?.uid || null;
    },

    /**
     * Check if user is logged in
     * @returns {boolean}
     */
    isLoggedIn() {
        return this.currentUser !== null;
    },

    /**
     * Check if current user is admin
     * @returns {boolean}
     */
    isCurrentUserAdmin() {
        return this.isAdmin;
    },

    /**
     * Called when authentication succeeds
     * Override this to customize behavior
     * @param {Object} user
     */
    onAuthSuccess(user) {
        // Override in main.js or specific pages
        console.log('User authenticated:', user.email);
    },

    /**
     * Called when user logs out
     * Override this to customize behavior
     */
    onAuthLogout() {
        // Override in main.js or specific pages
        console.log('User logged out');
    },

    /**
     * Require authentication for a page
     * Redirects to login if not authenticated
     */
    async requireAuth() {
        // Check sessionStorage first
        const storedUser = sessionStorage.getItem('adminUser');
        if (storedUser) {
            try {
                const userData = JSON.parse(storedUser);
                if (userData.isAdmin) {
                    this.currentUser = userData;
                    this.isAdmin = true;
                    return true;
                }
            } catch (e) {
                // Invalid stored data
            }
        }
        
        const auth = getAuth();
        
        return new Promise((resolve) => {
            const unsubscribe = auth.onAuthStateChanged(async (user) => {
                unsubscribe();
                
                if (!user) {
                    window.location.href = 'login.html';
                    resolve(false);
                    return;
                }
                
                const isAdmin = await this.checkAdminStatus(user.uid);
                
                if (!isAdmin) {
                    await this.signOut();
                    window.location.href = 'login.html';
                    resolve(false);
                    return;
                }
                
                this.currentUser = user;
                this.isAdmin = true;
                resolve(true);
            });
        });
    },

    /**
     * Require login page to redirect if already logged in
     */
    async redirectIfLoggedIn() {
        // Check sessionStorage first
        const storedUser = sessionStorage.getItem('adminUser');
        if (storedUser) {
            try {
                const userData = JSON.parse(storedUser);
                if (userData.isAdmin) {
                    window.location.href = 'index.html';
                    return true;
                }
            } catch (e) {
                // Invalid stored data
            }
        }
        
        const auth = getAuth();
        
        return new Promise((resolve) => {
            const unsubscribe = auth.onAuthStateChanged(async (user) => {
                unsubscribe();
                
                if (user) {
                    const isAdmin = await this.checkAdminStatus(user.uid);
                    
                    if (isAdmin) {
                        window.location.href = 'index.html';
                        resolve(true);
                        return;
                    }
                }
                
                resolve(false);
            });
        });
    },

    /**
     * Get user profile data
     * @returns {Promise<Object|null>}
     */
    async getProfile() {
        if (!this.currentUser) return null;
        return await getUserData(this.currentUser.uid);
    },

    /**
     * Update user profile
     * @param {Object} data
     * @returns {Promise<boolean>}
     */
    async updateProfile(data) {
        if (!this.currentUser) return false;
        
        try {
            await getDocument(APP_CONFIG.collections.users, this.currentUser.uid).update({
                ...data,
                updatedAt: Date.now()
            });
            return true;
        } catch (error) {
            console.error('Error updating profile:', error);
            return false;
        }
    }
};
