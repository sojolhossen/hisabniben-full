<?php

namespace App\Http\Controllers;

use App\Helpers\FirestoreHelper;
use Illuminate\Http\Request;

class PaymentMethodController extends Controller
{
    public function index(Request $request)
    {
        $rawMethods = FirestoreHelper::getCollection('payment_methods', 100);

        $methods = array_map(function($m) {
            $name = $m['name'] ?? (isset($m['id']) ? ucfirst($m['id']) : 'Payment Method');
            $number = $m['accountNumber'] ?? $m['number'] ?? $m['phone'] ?? '-';
            $type = $m['accountType'] ?? $m['type'] ?? 'PERSONAL';
            $instructions = $m['instructions'] ?? 'Send money to the account number above and submit Transaction ID.';
            $isActive = isset($m['active']) ? (bool)$m['active'] : (isset($m['isActive']) ? (bool)$m['isActive'] : true);

            return [
                'id' => $m['id'] ?? ('METHOD_' . time()),
                'name' => $name,
                'number' => $number,
                'accountNumber' => $number,
                'type' => strtoupper($type),
                'accountType' => strtoupper($type),
                'instructions' => $instructions,
                'isActive' => $isActive,
                'active' => $isActive,
                'icon' => $m['icon'] ?? strtolower($name),
                'updatedAt' => $m['updatedAt'] ?? time() * 1000
            ];
        }, $rawMethods);

        return view('payment-methods.index', ['methods' => $methods]);
    }

    public function save(Request $request)
    {
        $name = $request->input('name');
        $id = $request->input('id') ?: (strtolower(str_replace([' ', '-'], '_', $name)) ?: ('METHOD_' . time()));
        $type = strtoupper($request->input('type', 'PERSONAL'));
        $number = $request->input('number');
        $instructions = $request->input('instructions', '');
        $isActive = $request->has('isActive');

        $data = [
            'name' => $name,
            'number' => $number,
            'accountNumber' => $number,
            'type' => $type,
            'accountType' => $type,
            'instructions' => $instructions,
            'active' => $isActive,
            'isActive' => $isActive,
            'icon' => strtolower($name),
            'updatedAt' => time() * 1000
        ];

        FirestoreHelper::setDocument('payment_methods', $id, $data);
        return back()->with('success', 'Payment method saved in Firebase successfully!');
    }

    public function toggle(Request $request)
    {
        $id = $request->input('id');
        $method = FirestoreHelper::getDocument('payment_methods', $id);
        if ($method) {
            $currentActive = isset($method['active']) ? (bool)$method['active'] : (!empty($method['isActive']));
            $newActive = !$currentActive;

            FirestoreHelper::setDocument('payment_methods', $id, [
                'active' => $newActive,
                'isActive' => $newActive,
                'updatedAt' => time() * 1000
            ]);
        }

        return back()->with('success', 'Payment method status toggled in Firebase.');
    }
}
