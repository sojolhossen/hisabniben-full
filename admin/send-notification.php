<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(['error' => 'Only POST method allowed']);
    http_response_code(405);
    exit;
}

$input = json_decode(file_get_contents('php://input'), true);

if (!$input || !isset($input['title']) || !isset($input['message'])) {
    echo json_encode(['error' => 'Invalid input']);
    http_response_code(400);
    exit;
}

$appId = 'b632ec59-9dfd-496f-ae50-5331bb53e91d';
$apiKey = 'YOUR_ONESIGNAL_REST_API_KEY';

$title = $input['title'];
$message = $input['message'];
$target = $input['target'] ?? 'all';
$userId = $input['userId'] ?? null;
$imageUrl = $input['imageUrl'] ?? null;

$data = [
    'app_id' => $appId,
    'headings' => ['en' => $title],
    'contents' => ['en' => $message],
    'target_channel' => 'push'
];

// Add image if provided
if ($imageUrl) {
    $data['big_picture'] = $imageUrl;
}

if ($target === 'specific' && $userId) {
    $data['include_external_user_ids'] = [$userId];
    $data['include_aliases'] = ['external_id' => [$userId]];
} else {
    $data['included_segments'] = ['Subscribed Users', 'All'];
}

// Debug log
error_log('OneSignal Request: ' . json_encode($data));

$ch = curl_init('https://onesignal.com/api/v1/notifications');
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($data));
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'Content-Type: application/json',
    'Authorization: Basic ' . $apiKey
]);
curl_setopt($ch, CURLOPT_TIMEOUT, 30);
curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, 0);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$curlError = curl_error($ch);
curl_close($ch);

if ($curlError) {
    echo json_encode(['error' => 'cURL Error: ' . $curlError]);
    http_response_code(500);
    exit;
}

echo $response;
