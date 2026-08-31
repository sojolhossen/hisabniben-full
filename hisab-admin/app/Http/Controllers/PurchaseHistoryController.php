<?php

namespace App\Http\Controllers;

use App\Helpers\FirestoreHelper;
use Illuminate\Http\Request;

class PurchaseHistoryController extends Controller
{
    public function index(Request $request)
    {
        $purchases = FirestoreHelper::getCollection('purchases', 500);

        $totalRevenue = 0;
        foreach ($purchases as $p) {
            $totalRevenue += (float)($p['amount'] ?? 0);
        }

        return view('payments.history', [
            'purchases' => $purchases,
            'totalRevenue' => $totalRevenue,
            'totalCount' => count($purchases)
        ]);
    }
}
