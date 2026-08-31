/**
 * HisabNiben Admin Panel Configuration
 * Replace these values with your Firebase project credentials
 */

const APP_CONFIG = {
    // Firebase Configuration
    firebase: {
        apiKey: "AIzaSyCknq4ArQmWsLgGDNm0uH4fqPs4I1eQE4A",
        authDomain: "eduprep-9c5b3.firebaseapp.com",
        projectId: "eduprep-9c5b3",
        storageBucket: "eduprep-9c5b3.firebasestorage.app",
        messagingSenderId: "455453552762",
        appId: "1:455453552762:web:96bb8a76582d1b3a948bc3",
        measurementId: "G-DT9CFDZ075"
    },

    // App Settings
    app: {
        name: "HisabNiben",
        title: "HisabNiben Admin Panel",
        version: "1.0.0",
        currency: "৳",
        dateFormat: "dd/mm/yyyy"
    },

    // Collection Names (Firestore)
    collections: {
        users: "users",
        customers: "customers",
        transactions: "transactions",
        packages: "packages",
        notifications: "notifications",
        products: "products",
        settings: "settings",
        sms_packages: "sms_packages",
        payment_methods: "payment_methods",
        payment_requests: "payment_requests"
    },

    // Pagination
    pagination: {
        usersPerPage: 20,
        transactionsPerPage: 50,
        customersPerPage: 20
    },

    // Trial Settings
    trial: {
        durationDays: 7
    },

    // Toast Duration (ms)
    toast: {
        success: 3000,
        error: 5000,
        warning: 4000
    },

    // Chart Colors - Matching App Colors
    chartColors: {
        primary: "#F54927",
        success: "#10B981",
        warning: "#F59E0B",
        danger: "#EF4444",
        info: "#6366F1",
        purple: "#8B5CF6"
    },

    // OneSignal Configuration
    onesignal: {
        appId: "b632ec59-9dfd-496f-ae50-5331bb53e91d",
        restApiKey: "YOUR_ONESIGNAL_REST_API_KEY"
    }
};

// Freeze config to prevent modifications
Object.freeze(APP_CONFIG);
