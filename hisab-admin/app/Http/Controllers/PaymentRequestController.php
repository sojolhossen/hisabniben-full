<?php

namespace App\Http\Controllers;

use App\Helpers\FirestoreHelper;
use Illuminate\Http\Request;

class PaymentRequestController extends Controller
{
    public function index(Request $request)
    {
        $requests = FirestoreHelper::getCollection('payment_requests', 300);

        $status = strtoupper($request->input('status', 'ALL'));
        $search = strtolower($request->input('search', ''));

        $filtered = array_filter($requests, function($p) use ($status, $search) {
            $pStatus = strtoupper($p['status'] ?? 'PENDING');
            $trxId = strtolower($p['transactionId'] ?? '');
            $phone = strtolower($p['phone'] ?? '');

            if ($status !== 'ALL' && $pStatus !== $status) return false;
            if ($search !== '' && strpos($trxId, $search) === false && strpos($phone, $search) === false) return false;

            return true;
        });

        // Detect duplicate TrxIDs
        $trxCounts = [];
        foreach ($requests as $r) {
            $tid = strtoupper(trim($r['transactionId'] ?? ''));
            if ($tid) {
                $trxCounts[$tid] = ($trxCounts[$tid] ?? 0) + 1;
            }
        }

        return view('payments.requests', [
            'requests' => array_values($filtered),
            'status' => $status,
            'search' => $search,
            'trxCounts' => $trxCounts
        ]);
    }

    public function approve(Request $request)
    {
        $reqId = $request->input('id');
        $pReq = FirestoreHelper::getDocument('payment_requests', $reqId);

        if (!$pReq) return back()->with('error', 'Payment request not found.');

        $userId = $pReq['userId'] ?? null;
        $durationDays = (int)($pReq['durationDays'] ?? 30);
        $packageName = $pReq['packageName'] ?? 'Premium Package';

        $now = time() * 1000;
        $expiryTimestamp = $durationDays >= 999 
            ? ($now + (36500 * 24 * 60 * 60 * 1000)) 
            : ($now + ($durationDays * 24 * 60 * 60 * 1000));

        // 1. Update Payment Request status
        FirestoreHelper::setDocument('payment_requests', $reqId, [
            'status' => 'APPROVED',
            'approvedAt' => $now
        ]);

        // 2. Upgrade User Subscription
        if ($userId) {
            FirestoreHelper::setDocument('users', $userId, [
                'isPremium' => true,
                'subscriptionStatus' => 'ACTIVE',
                'subscriptionPackageName' => $packageName,
                'subscriptionExpiryDate' => $expiryTimestamp,
                'updatedAt' => $now
            ]);
        }

        // 3. Log to Purchase History
        $purchaseId = 'PURCHASE_' . time() . '_' . rand(1000, 9999);
        FirestoreHelper::setDocument('purchases', $purchaseId, [
            'userId' => $userId,
            'phone' => $pReq['phone'] ?? '',
            'amount' => $pReq['amount'] ?? 0,
            'packageName' => $packageName,
            'durationDays' => $durationDays,
            'transactionId' => $pReq['transactionId'] ?? '',
            'paymentMethod' => $pReq['paymentMethod'] ?? 'Bkash',
            'status' => 'APPROVED',
            'createdAt' => $now
        ]);

        return back()->with('success', 'Payment request approved and user subscription upgraded!');
    }

    public function reject(Request $request)
    {
        $reqId = $request->input('id');
        FirestoreHelper::setDocument('payment_requests', $reqId, [
            'status' => 'REJECTED',
            'rejectedAt' => time() * 1000
        ]);

        return back()->with('success', 'Payment request rejected.');
    }
}
