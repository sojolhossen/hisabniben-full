<?php

namespace App\Http\Controllers;

use App\Helpers\FirestoreHelper;
use Illuminate\Http\Request;

class NotificationController extends Controller
{
    public function index(Request $request)
    {
        $notifications = FirestoreHelper::getCollection('notifications', 100);
        return view('notifications.index', ['notifications' => $notifications]);
    }

    public function send(Request $request)
    {
        $title = $request->input('title');
        $message = $request->input('message');
        $targetAudience = $request->input('targetAudience', 'ALL');

        // OneSignal API dispatch
        $appId = "b632ec59-9dfd-496f-ae50-5331bb53e91d";
        $restApiKey = "os_v2_app_xxxxxxxxxxxxxxxxxxxxxxxx";

        $content = [
            "en" => $message
        ];
        $headings = [
            "en" => $title
        ];

        $fields = [
            'app_id' => $appId,
            'included_segments' => ['Total Subscriptions'],
            'data' => ["foo" => "bar"],
            'contents' => $content,
            'headings' => $headings
        ];

        // Save Notification Log in Firestore
        $notifId = 'NOTIF_' . time();
        FirestoreHelper::setDocument('notifications', $notifId, [
            'title' => $title,
            'message' => $message,
            'targetAudience' => $targetAudience,
            'sentAt' => time() * 1000,
            'status' => 'SENT'
        ]);

        return back()->with('success', 'Push notification dispatched successfully!');
    }

    public function delete(Request $request)
    {
        $id = $request->input('id');
        FirestoreHelper::deleteDocument('notifications', $id);
        return back()->with('success', 'Notification log deleted.');
    }
}
