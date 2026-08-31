/**
 * Firebase Configuration & Initialization
 * HisabNiben Admin Panel
 */

// Firebase configuration is loaded from config/config.js
// Make sure config.js is loaded before this file

let db = null;
let auth = null;
let messaging = null;

/**
 * Initialize Firebase services
 */
function initFirebase() {
    try {
        // Check if config exists
        if (typeof APP_CONFIG === 'undefined') {
            console.error('APP_CONFIG not found. Make sure config.js is loaded.');
            return false;
        }

        // Initialize Firebase if not already initialized
        if (!firebase.apps.length) {
            firebase.initializeApp(APP_CONFIG.firebase);
        }

        // Initialize services
        db = firebase.firestore();
        try {
            db.enablePersistence({ synchronizeTabs: true }).catch(err => {
                if (err.code === 'failed-precondition') {
                    console.warn('Firestore persistence failed: Multiple tabs open');
                } else if (err.code === 'unimplemented') {
                    console.warn('Firestore persistence unsupported in browser');
                }
            });
        } catch (e) {
            console.warn('Firestore persistence init error:', e);
        }
        auth = firebase.auth();
        
        // Initialize messaging (with error handling for browsers that don't support it)
        try {
            if (typeof firebase.messaging === 'function') {
                messaging = firebase.messaging();
            } else {
                console.warn('Firebase Messaging not available in this environment');
            }
        } catch (e) {
            console.warn('Firebase Messaging initialization failed:', e.message);
        }

        console.log('Firebase initialized successfully');
        return true;
    } catch (error) {
        console.error('Firebase initialization error:', error);
        return false;
    }
}

/**
 * Get Firestore instance
 * @returns {FirebaseFirestore|null}
 */
function getFirestore() {
    if (!db) {
        const initialized = initFirebase();
        if (!initialized) {
            console.error('Firebase initialization failed');
            return null;
        }
    }
    return db;
}

/**
 * Get Firebase Auth instance
 * @returns {FirebaseAuth}
 */
function getAuth() {
    if (!auth) {
        initFirebase();
    }
    return auth;
}

/**
 * Get Firebase Messaging instance
 * @returns {FirebaseMessaging|null}
 */
function getMessaging() {
    return messaging;
}

/**
 * Get collection reference
 * @param {string} collectionName
 * @returns {CollectionReference|null}
 */
function getCollection(collectionName) {
    if (!collectionName || typeof collectionName !== 'string' || collectionName.trim() === '') {
        console.error('Invalid collection name provided:', collectionName);
        return null;
    }
    
    const firestore = getFirestore();
    if (!firestore) {
        console.error('Firestore not initialized');
        return null;
    }
    
    try {
        return firestore.collection(collectionName);
    } catch (error) {
        console.error('Error getting collection:', collectionName, error);
        return null;
    }
}

/**
 * Get document reference
 * @param {string} collectionName
 * @param {string} docId
 * @returns {DocumentReference}
 */
function getDocument(collectionName, docId) {
    return getCollection(collectionName).doc(docId);
}

/**
 * Check if user is admin
 * @param {string} userId
 * @returns {Promise<boolean>}
 */
async function isUserAdmin(userId) {
    try {
        const doc = await getDocument(APP_CONFIG.collections.users, userId).get();
        return doc.exists && doc.data().isAdmin === true;
    } catch (error) {
        console.error('Error checking admin status:', error);
        return false;
    }
}

/**
 * Get user data
 * @param {string} userId
 * @returns {Promise<Object|null>}
 */
async function getUserData(userId) {
    try {
        const doc = await getDocument(APP_CONFIG.collections.users, userId).get();
        return doc.exists ? { id: doc.id, ...doc.data() } : null;
    } catch (error) {
        console.error('Error getting user data:', error);
        return null;
    }
}

// Initialize Firebase on script load
document.addEventListener('DOMContentLoaded', () => {
    initFirebase();
});
