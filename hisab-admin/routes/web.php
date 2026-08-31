<?php

use Illuminate\Support\Facades\Route;
use App\Http\Controllers\AuthController;
use App\Http\Controllers\DashboardController;
use App\Http\Controllers\UserController;
use App\Http\Controllers\PaymentRequestController;
use App\Http\Controllers\PurchaseHistoryController;
use App\Http\Controllers\PackageController;
use App\Http\Controllers\PaymentMethodController;
use App\Http\Controllers\NotificationController;
use App\Http\Controllers\SmsController;
use App\Http\Controllers\TutorialController;
use App\Http\Controllers\SettingsController;

// Auth Routes
Route::get('/', [AuthController::class, 'showLogin'])->name('login');
Route::get('/login', [AuthController::class, 'showLogin']);
Route::post('/login', [AuthController::class, 'login'])->name('login.post');
Route::post('/logout', [AuthController::class, 'logout'])->name('logout');

// Protected Admin Routes
Route::middleware([\App\Http\Middleware\AdminAuth::class])->group(function () {
    Route::get('/dashboard', [DashboardController::class, 'index'])->name('dashboard');

    // Users & CRM
    Route::get('/users', [UserController::class, 'index'])->name('users.index');
    Route::get('/users/{id}/crm', [UserController::class, 'crm'])->name('users.crm');
    Route::post('/users/update', [UserController::class, 'update'])->name('users.update');
    Route::post('/users/subscription', [UserController::class, 'updateSubscription'])->name('users.subscription');
    Route::post('/users/sms', [UserController::class, 'updateSms'])->name('users.sms');
    Route::post('/users/ban', [UserController::class, 'toggleBan'])->name('users.ban');
    Route::post('/users/delete', [UserController::class, 'deleteUserAndData'])->name('users.delete');

    // Payments & Revenue
    Route::get('/payments/requests', [PaymentRequestController::class, 'index'])->name('payments.requests');
    Route::post('/payments/requests/approve', [PaymentRequestController::class, 'approve'])->name('payments.requests.approve');
    Route::post('/payments/requests/reject', [PaymentRequestController::class, 'reject'])->name('payments.requests.reject');
    Route::get('/payments/history', [PurchaseHistoryController::class, 'index'])->name('payments.history');

    // Packages & Payment Methods
    Route::get('/packages', [PackageController::class, 'index'])->name('packages.index');
    Route::post('/packages/save', [PackageController::class, 'save'])->name('packages.save');
    Route::post('/packages/delete', [PackageController::class, 'delete'])->name('packages.delete');

    Route::get('/payment-methods', [PaymentMethodController::class, 'index'])->name('payment-methods.index');
    Route::post('/payment-methods/save', [PaymentMethodController::class, 'save'])->name('payment-methods.save');
    Route::post('/payment-methods/toggle', [PaymentMethodController::class, 'toggle'])->name('payment-methods.toggle');

    // Notifications & SMS
    Route::get('/notifications', [NotificationController::class, 'index'])->name('notifications.index');
    Route::post('/notifications/send', [NotificationController::class, 'send'])->name('notifications.send');
    Route::post('/notifications/delete', [NotificationController::class, 'delete'])->name('notifications.delete');

    Route::get('/sms-settings', [SmsController::class, 'settings'])->name('sms.settings');
    Route::post('/sms-settings/save', [SmsController::class, 'saveSettings'])->name('sms.settings.save');
    Route::post('/sms-settings/test', [SmsController::class, 'testConnection'])->name('sms.test');

    Route::get('/sms-packages', [SmsController::class, 'packages'])->name('sms.packages');
    Route::post('/sms-packages/save', [SmsController::class, 'savePackage'])->name('sms.packages.save');

    // Tutorials & System Settings
    Route::get('/tutorials', [TutorialController::class, 'index'])->name('tutorials.index');
    Route::post('/tutorials/save', [TutorialController::class, 'save'])->name('tutorials.save');

    Route::get('/settings', [SettingsController::class, 'index'])->name('settings.index');
    Route::post('/settings/save', [SettingsController::class, 'save'])->name('settings.save');
    Route::get('/settings/export-json', [SettingsController::class, 'exportJson'])->name('settings.export-json');
});
