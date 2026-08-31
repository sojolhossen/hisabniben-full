<?php

namespace App\Http\Controllers;

use App\Helpers\FirestoreHelper;
use Illuminate\Http\Request;

class UserController extends Controller
{
    public function index(Request $request)
    {
        $allUsers = FirestoreHelper::getCollection('users', 500);

        $search = strtolower($request->input('search', ''));
        $status = strtoupper($request->input('status', 'ALL'));

        $now = time() * 1000;

        $filtered = array_filter($allUsers, function($u) use ($search, $status, $now) {
            $name = strtolower($u['name'] ?? '');
            $phone = strtolower($u['phone'] ?? '');
            $store = strtolower($u['storeName'] ?? $u['shopName'] ?? '');

            if ($search !== '' && strpos($name, $search) === false && strpos($phone, $search) === false && strpos($store, $search) === false) {
                return false;
            }

            if ($status === 'PREMIUM') {
                return !empty($u['isPremium']);
            } elseif ($status === 'TRIAL') {
                return empty($u['isPremium']) && !empty($u['trialEnd']) && $u['trialEnd'] > $now;
            } elseif ($status === 'EXPIRED') {
                return empty($u['isPremium']) && (empty($u['trialEnd']) || $u['trialEnd'] <= $now);
            } elseif ($status === 'BLOCKED') {
                return !empty($u['isBlocked']) || !empty($u['disabled']);
            }

            return true;
        });

        return view('users.index', [
            'users' => array_values($filtered),
            'totalCount' => count($allUsers),
            'search' => $request->input('search', ''),
            'status' => $status
        ]);
    }

    public function crm(Request $request, $id)
    {
        $user = FirestoreHelper::getDocument('users', $id);
        if (!$user) {
            return response()->json(['error' => 'User not found'], 404);
        }

        $customers = FirestoreHelper::queryCollection('customers', 'userId', '==', $id);
        $suppliers = FirestoreHelper::queryCollection('suppliers', 'userId', '==', $id);
        $transactions = FirestoreHelper::queryCollection('transactions', 'userId', '==', $id);
        $products = FirestoreHelper::queryCollection('products', 'userId', '==', $id);
        $purchases = FirestoreHelper::queryCollection('purchases', 'userId', '==', $id);
        $smsHistory = FirestoreHelper::queryCollection('sms_history', 'userId', '==', $id);

        $totalCustomerDues = 0;
        foreach ($customers as $c) {
            if (isset($c['dueAmount'])) $totalCustomerDues += (float)$c['dueAmount'];
        }

        $totalSupplierPayables = 0;
        foreach ($suppliers as $s) {
            if (isset($s['payableAmount'])) $totalSupplierPayables += (float)$s['payableAmount'];
        }

        $totalSalesSum = 0;
        $totalPurchasesSum = 0;
        foreach ($transactions as $t) {
            $type = strtoupper($t['type'] ?? '');
            $amount = (float)($t['amount'] ?? 0);
            if ($type === 'SALE' || $type === 'SELL') $totalSalesSum += $amount;
            if ($type === 'PURCHASE' || $type === 'BUY') $totalPurchasesSum += $amount;
        }

        return response()->json([
            'user' => $user,
            'stats' => [
                'totalCustomers' => count($customers),
                'totalCustomerDues' => $totalCustomerDues,
                'totalSuppliers' => count($suppliers),
                'totalSupplierPayables' => $totalSupplierPayables,
                'totalProducts' => count($products),
                'totalSalesSum' => $totalSalesSum,
                'totalPurchasesSum' => $totalPurchasesSum,
                'totalTransactions' => count($transactions),
            ],
            'customers' => array_slice($customers, 0, 50),
            'suppliers' => array_slice($suppliers, 0, 50),
            'transactions' => array_slice($transactions, 0, 20),
            'smsHistory' => array_slice($smsHistory, 0, 20),
        ]);
    }

    public function update(Request $request)
    {
        $userId = $request->input('id');
        $data = [
            'name' => $request->input('name'),
            'phone' => $request->input('phone'),
            'email' => $request->input('email'),
            'storeName' => $request->input('storeName'),
            'shopName' => $request->input('storeName'),
            'updatedAt' => time() * 1000
        ];

        FirestoreHelper::setDocument('users', $userId, $data);
        return back()->with('success', 'User profile updated successfully!');
    }

    public function updateSubscription(Request $request)
    {
        $userId = $request->input('id');
        $durationDays = (int)$request->input('durationDays', 30);
        $packageName = $request->input('packageName', 'Premium Plan');

        $now = time() * 1000;
        $expiryTimestamp = $durationDays >= 999 
            ? ($now + (36500 * 24 * 60 * 60 * 1000)) 
            : ($now + ($durationDays * 24 * 60 * 60 * 1000));

        $data = [
            'isPremium' => true,
            'subscriptionStatus' => 'ACTIVE',
            'subscriptionPackageName' => $packageName,
            'subscriptionExpiryDate' => $expiryTimestamp,
            'updatedAt' => $now
        ];

        FirestoreHelper::setDocument('users', $userId, $data);
        return back()->with('success', "Subscription updated to {$packageName} for {$durationDays} days!");
    }

    public function updateSms(Request $request)
    {
        $userId = $request->input('id');
        $smsLimit = (int)$request->input('smsLimit', 0);

        FirestoreHelper::setDocument('users', $userId, ['smsLimit' => $smsLimit, 'updatedAt' => time() * 1000]);
        return back()->with('success', "SMS balance updated to {$smsLimit} SMS!");
    }

    public function toggleBan(Request $request)
    {
        $userId = $request->input('id');
        $user = FirestoreHelper::getDocument('users', $userId);
        if (!$user) return back()->with('error', 'User not found');

        $isBlocked = !empty($user['isBlocked']);
        FirestoreHelper::setDocument('users', $userId, ['isBlocked' => !$isBlocked, 'disabled' => !$isBlocked, 'updatedAt' => time() * 1000]);

        $statusText = !$isBlocked ? 'banned' : 'unbanned';
        return back()->with('success', "User account has been {$statusText}!");
    }

    public function deleteUserAndData(Request $request)
    {
        $userId = $request->input('id');
        $confirmText = strtoupper(trim($request->input('confirm_text', '')));

        if ($confirmText !== 'DELETE') {
            return back()->with('error', 'Please type DELETE in capital letters to confirm deletion.');
        }

        $collectionsToPurge = [
            'customers',
            'suppliers',
            'transactions',
            'products',
            'payment_requests',
            'purchases',
            'sms_history',
            'wallet_accounts'
        ];

        foreach ($collectionsToPurge as $colName) {
            $docs = FirestoreHelper::queryCollection($colName, 'userId', '==', $userId);
            foreach ($docs as $d) {
                if (isset($d['id'])) {
                    FirestoreHelper::deleteDocument($colName, $d['id']);
                }
            }
        }

        // Delete Main User Document
        FirestoreHelper::deleteDocument('users', $userId);

        return redirect()->route('users.index')->with('success', 'User account and all associated data permanently purged!');
    }
}
