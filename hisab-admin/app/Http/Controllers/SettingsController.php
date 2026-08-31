<?php

namespace App\Http\Controllers;

use App\Helpers\FirestoreHelper;
use Illuminate\Http\Request;

class SettingsController extends Controller
{
    public function index(Request $request)
    {
        $supportDoc = FirestoreHelper::getDocument('settings', 'support');
        $systemDoc = FirestoreHelper::getDocument('settings', 'system');
        $landingDoc = FirestoreHelper::getDocument('settings', 'landingPage');

        return view('settings.index', [
            'support' => $supportDoc,
            'system' => $systemDoc,
            'landing' => $landingDoc
        ]);
    }

    public function save(Request $request)
    {
        $type = $request->input('type');

        if ($type === 'support') {
            FirestoreHelper::setDocument('settings', 'support', [
                'phone' => $request->input('phone'),
                'whatsapp' => $request->input('whatsapp'),
                'email' => $request->input('email'),
                'availableHours' => $request->input('availableHours', 'Sat - Thu: 9 AM - 9 PM'),
                'updatedAt' => time() * 1000
            ]);
        } elseif ($type === 'system') {
            FirestoreHelper::setDocument('settings', 'system', [
                'trialDurationDays' => (int)$request->input('trialDurationDays', 7),
                'autoTrialOnSignup' => $request->has('autoTrialOnSignup'),
                'maintenanceMode' => $request->has('maintenanceMode'),
                'updatedAt' => time() * 1000
            ]);
        }

        return back()->with('success', 'System settings saved successfully!');
    }

    public function exportJson(Request $request)
    {
        $users = FirestoreHelper::getCollection('users', 500);
        $purchases = FirestoreHelper::getCollection('purchases', 500);
        $packages = FirestoreHelper::getCollection('packages', 100);

        $exportData = [
            'app' => 'HisabNiben Enterprise',
            'exportedAt' => date('Y-m-d H:i:s'),
            'users' => $users,
            'purchases' => $purchases,
            'packages' => $packages
        ];

        $jsonStr = json_encode($exportData, JSON_PRETTY_PRINT);
        $fileName = 'HisabNiben_Database_Backup_' . date('Y-m-d') . '.json';

        return response($jsonStr, 200, [
            'Content-Type' => 'application/json',
            'Content-Disposition' => 'attachment; filename="' . $fileName . '"'
        ]);
    }
}
