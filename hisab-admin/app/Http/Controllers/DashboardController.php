<?php

namespace App\Http\Controllers;

use App\Helpers\FirestoreHelper;
use Illuminate\Http\Request;

class DashboardController extends Controller
{
    public function index(Request $request)
    {
        $users = FirestoreHelper::getCollection('users', 300);
        $paymentRequests = FirestoreHelper::getCollection('payment_requests', 200);
        $purchases = FirestoreHelper::getCollection('purchases', 200);
        $notifications = FirestoreHelper::getCollection('notifications', 50);

        $now = time() * 1000;
        $totalUsers = count($users);
        $premiumUsers = 0;
        $trialUsers = 0;
        $expiredUsers = 0;

        foreach ($users as $u) {
            if (!empty($u['isPremium'])) {
                $premiumUsers++;
            } elseif (!empty($u['trialEnd']) && $u['trialEnd'] > $now) {
                $trialUsers++;
            } else {
                $expiredUsers++;
            }
        }

        $pendingPayments = array_filter($paymentRequests, function($p) {
            return isset($p['status']) && strtoupper($p['status']) === 'PENDING';
        });

        $totalRevenue = 0;
        foreach ($purchases as $p) {
            if (isset($p['amount'])) {
                $totalRevenue += (float)$p['amount'];
            }
        }

        return view('dashboard.index', [
            'totalUsers' => $totalUsers,
            'premiumUsers' => $premiumUsers,
            'trialUsers' => $trialUsers,
            'expiredUsers' => $expiredUsers,
            'pendingPaymentsCount' => count($pendingPayments),
            'pendingPayments' => array_slice(array_values($pendingPayments), 0, 5),
            'totalRevenue' => $totalRevenue,
            'recentUsers' => array_slice($users, 0, 6),
            'purchasesCount' => count($purchases),
            'notificationsCount' => count($notifications)
        ]);
    }
}
