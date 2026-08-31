<?php

namespace App\Http\Controllers;

use App\Helpers\FirestoreHelper;
use Illuminate\Http\Request;

class SmsController extends Controller
{
    public function settings(Request $request)
    {
        $smsApiDoc = FirestoreHelper::getDocument('settings', 'sms_api');
        $history = FirestoreHelper::getCollection('sms_history', 200);

        return view('sms.settings', [
            'smsApiDoc' => $smsApiDoc,
            'history' => $history
        ]);
    }

    public function saveSettings(Request $request)
    {
        $apiKey = $request->input('apiKey');
        $senderId = $request->input('senderId');

        $data = [
            'apiKey' => $apiKey,
            'senderId' => $senderId,
            'updatedAt' => time() * 1000
        ];

        FirestoreHelper::setDocument('settings', 'sms_api', $data);
        return back()->with('success', 'BulkSMSBD API Credentials saved successfully!');
    }

    public function testConnection(Request $request)
    {
        $smsApiDoc = FirestoreHelper::getDocument('settings', 'sms_api');
        $apiKey = $smsApiDoc['apiKey'] ?? '';

        if (empty($apiKey)) {
            return back()->with('error', 'Please configure your BulkSMSBD API Key first!');
        }

        $url = "http://bulksmsbd.net/api/getBalanceApi?api_key=" . urlencode($apiKey);
        $ch = curl_init($url);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_TIMEOUT, 15);
        $response = curl_exec($ch);
        curl_close($ch);

        return back()->with('success', 'BulkSMSBD Gateway Response: ' . ($response ?: 'Connection verified!'));
    }

    public function packages(Request $request)
    {
        $smsPackages = FirestoreHelper::getCollection('sms_packages', 100);
        return view('sms.packages', ['packages' => $smsPackages]);
    }

    public function savePackage(Request $request)
    {
        $id = $request->input('id') ?: ('SMS_PKG_' . time());
        $data = [
            'name' => $request->input('name'),
            'price' => (float)$request->input('price'),
            'smsCount' => (int)$request->input('smsCount'),
            'updatedAt' => time() * 1000
        ];

        FirestoreHelper::setDocument('sms_packages', $id, $data);
        return back()->with('success', 'SMS Package saved successfully!');
    }
}
