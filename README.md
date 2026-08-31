# HisabNiben - Baki Management App

A professional Android app for Bangladesh users to manage their customers, transactions, and baki/due amounts with subscription-based premium features.

## Features

### User Features
- **OTP Login/Signup** - Phone-based authentication with SMS verification
- **Dashboard** - Overview of customers, transactions, total baki, and payments
- **Customer Management** - Add, edit, delete customers with contact details
- **Transaction Management** - Record payments and baki (due) transactions
- **7-Day Free Trial** - All new users get 7 days free trial
- **Premium Subscription** - Upgrade via Google Play Billing for unlimited access
- **Push Notifications** - Firebase Cloud Messaging for updates
- **Reports & Analytics** - Visual charts showing transaction trends

### Admin Panel Features
- **User Management** - View all users, trial status, subscription status
- **Manual Payment Verification** - Verify bKash/Nagad/Rocket payments
- **Package Management** - Create and manage subscription plans
- **Transaction History** - View all transactions across users
- **Push Notifications** - Send notifications to specific or all users
- **Analytics Dashboard** - User growth, revenue, subscription stats

## Project Structure

```
HisabNiben/
├── app/
│   ├── src/main/
│   │   ├── java/com/sajoldev/hisabniben/
│   │   │   ├── activity/       # All activities
│   │   │   ├── adapter/         # RecyclerView adapters
│   │   │   ├── model/          # Data models
│   │   │   ├── service/        # FCM and background services
│   │   │   └── util/           # Utility classes
│   │   ├── res/
│   │   │   ├── layout/         # XML layouts
│   │   │   ├── drawable/       # Icons and graphics
│   │   │   └── values/         # Colors, strings, themes
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── admin-panel/
│   └── index.html              # Admin panel (HTML/CSS/JS)
├── firebase_rules.json         # Firestore security rules
└── README.md
```

## Setup Instructions

### 1. Firebase Setup

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project "HisabNiben"
3. Enable **Authentication** - Phone provider
4. Enable **Cloud Firestore** - Create database (start in test mode)
5. Enable **Firebase Cloud Messaging**
6. Download `google-services.json` and place in `app/`
7. Copy your Firebase config to admin-panel/index.html

### 2. Firestore Security Rules

Copy the rules from `firebase_rules.json` to Firebase Console > Firestore > Rules.

### 3. Create Admin User

1. Build and run the app
2. Sign up as a new user
3. In Firebase Console, manually set `isAdmin: true` for your user document

### 4. Google Play Billing Setup

1. Create Google Play Developer account
2. Create subscription products in Google Play Console:
   - `hisabniben_premium_monthly` (monthly)
   - `hisabniben_premium_yearly` (yearly)
3. Add your licensing key to the app

### 5. SMS API Configuration

Update API credentials in `SmsApiService.java`:
```java
private static final String API_KEY = "your_api_key";
private static final String SENDER_ID = "your_sender_id";
```

### 6. Admin Panel Deployment

1. Update Firebase config in `admin-panel/index.html`
2. Host on any web server (Firebase Hosting recommended)
3. Set up admin login credentials in Firebase Auth

## Building the App

```bash
# Using Gradle
./gradlew assembleDebug

# Or open in Android Studio
# File > Open > HisabNiben
# Build > Build APK
```

## Default Packages (Create in Firestore)

```json
{
  "name": "Monthly Premium",
  "price": 99,
  "durationDays": 30,
  "description": "Premium features for 1 month",
  "playStoreProductId": "hisabniben_premium_monthly",
  "status": "active"
}
```

```json
{
  "name": "Yearly Premium",
  "price": 999,
  "durationDays": 365,
  "description": "Premium features for 1 year",
  "playStoreProductId": "hisabniben_premium_yearly",
  "status": "active"
}
```

## Technology Stack

- **Android**: Java
- **Backend**: Firebase Firestore
- **Auth**: Firebase Authentication (Phone)
- **Payments**: Google Play Billing
- **Notifications**: Firebase Cloud Messaging
- **Charts**: MPAndroidChart
- **Admin Panel**: HTML/CSS/JS with Firebase SDK

## License

This project is for educational purposes. Modify as needed for your requirements.
