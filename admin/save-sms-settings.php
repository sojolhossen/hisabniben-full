<?php
header('Content-Type: application/json');

$apiKey = "VWalg9MQxTojswdnbkrv";
$senderId = "8809648907415";
$defaultSmsLimit = 10;

$config = [
    'apiKey' => $apiKey,
    'senderId' => $senderId,
    'defaultSmsLimit' => $defaultSmsLimit,
    'updatedAt' => time()
];

$jsonFile = __DIR__ . '/../firebase/sms_settings.json';
file_put_contents($jsonFile, json_encode($config, JSON_PRETTY_PRINT));

echo json_encode(['success' => true, 'message' => 'Settings saved', 'data' => $config]);
?>