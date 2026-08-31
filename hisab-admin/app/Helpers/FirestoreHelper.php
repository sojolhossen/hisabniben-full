<?php

namespace App\Helpers;

class FirestoreHelper
{
    private static $projectId = 'eduprep-9c5b3';

    public static function getProjectId()
    {
        return env('FIREBASE_PROJECT_ID', self::$projectId);
    }

    /**
     * Convert Firestore Document REST JSON into a clean PHP Associative Array
     */
    public static function formatDocument($doc)
    {
        if (!$doc || !is_array($doc)) return null;

        $id = '';
        if (isset($doc['name'])) {
            $parts = explode('/', $doc['name']);
            $id = end($parts);
        }

        $result = ['id' => $id];

        if (isset($doc['fields']) && is_array($doc['fields'])) {
            foreach ($doc['fields'] as $key => $valObj) {
                $result[$key] = self::parseValue($valObj);
            }
        }

        return $result;
    }

    /**
     * Convert list of Firestore REST documents into an array of clean PHP arrays
     */
    public static function formatDocuments($documents)
    {
        if (!is_array($documents)) return [];
        $list = [];
        foreach ($documents as $doc) {
            $formatted = self::formatDocument($doc);
            if ($formatted) {
                $list[] = $formatted;
            }
        }
        return $list;
    }

    private static function parseValue($valObj)
    {
        if (!is_array($valObj)) return null;

        if (isset($valObj['stringValue'])) return $valObj['stringValue'];
        if (isset($valObj['integerValue'])) return (int) $valObj['integerValue'];
        if (isset($valObj['doubleValue'])) return (float) $valObj['doubleValue'];
        if (isset($valObj['booleanValue'])) return (bool) $valObj['booleanValue'];
        if (isset($valObj['timestampValue'])) return $valObj['timestampValue'];
        if (isset($valObj['nullValue'])) return null;

        if (isset($valObj['arrayValue']['values'])) {
            $arr = [];
            foreach ($valObj['arrayValue']['values'] as $item) {
                $arr[] = self::parseValue($item);
            }
            return $arr;
        }

        if (isset($valObj['mapValue']['fields'])) {
            $map = [];
            foreach ($valObj['mapValue']['fields'] as $k => $v) {
                $map[$k] = self::parseValue($v);
            }
            return $map;
        }

        return null;
    }

    /**
     * Convert PHP array into Firestore Fields JSON object
     */
    public static function encodeFields($data)
    {
        $fields = [];
        foreach ($data as $key => $val) {
            if ($key === 'id') continue;
            $fields[$key] = self::encodeValue($val);
        }
        return ['fields' => $fields];
    }

    private static function encodeValue($val)
    {
        if (is_null($val)) return ['nullValue' => null];
        if (is_bool($val)) return ['booleanValue' => $val];
        if (is_int($val)) return ['integerValue' => (string)$val];
        if (is_float($val)) return ['doubleValue' => $val];
        if (is_string($val)) return ['stringValue' => $val];

        if (is_array($val)) {
            // Check if associative or indexed
            if (array_keys($val) === range(0, count($val) - 1)) {
                $values = [];
                foreach ($val as $item) {
                    $values[] = self::encodeValue($item);
                }
                return ['arrayValue' => ['values' => $values]];
            } else {
                $fields = [];
                foreach ($val as $k => $v) {
                    $fields[$k] = self::encodeValue($v);
                }
                return ['mapValue' => ['fields' => $fields]];
            }
        }

        return ['stringValue' => (string)$val];
    }

    /**
     * GET request to Firestore REST API
     */
    public static function getCollection($collectionName, $pageSize = 300)
    {
        $projectId = self::getProjectId();
        $url = "https://firestore.googleapis.com/v1/projects/{$projectId}/databases/(default)/documents/{$collectionName}?pageSize={$pageSize}";

        $ch = curl_init($url);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        $response = curl_exec($ch);
        curl_close($ch);

        $json = json_decode($response, true);
        return isset($json['documents']) ? self::formatDocuments($json['documents']) : [];
    }

    /**
     * Query collection by field (e.g. userId == 'abc')
     */
    public static function queryCollection($collectionName, $field, $operator, $value)
    {
        $projectId = self::getProjectId();
        $url = "https://firestore.googleapis.com/v1/projects/{$projectId}/databases/(default)/documents:runQuery";

        $opMap = [
            '==' => 'EQUAL',
            '!=' => 'NOT_EQUAL',
            '>'  => 'GREATER_THAN',
            '>=' => 'GREATER_THAN_OR_EQUAL',
            '<'  => 'LESS_THAN',
            '<=' => 'LESS_THAN_OR_EQUAL'
        ];

        $op = isset($opMap[$operator]) ? $opMap[$operator] : 'EQUAL';

        $query = [
            'structuredQuery' => [
                'from' => [['collectionId' => $collectionName]],
                'where' => [
                    'fieldFilter' => [
                        'field' => ['fieldPath' => $field],
                        'op' => $op,
                        'value' => self::encodeValue($value)
                    ]
                ]
            ]
        ];

        $ch = curl_init($url);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($query));
        curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        $response = curl_exec($ch);
        curl_close($ch);

        $results = json_decode($response, true);
        $documents = [];
        if (is_array($results)) {
            foreach ($results as $item) {
                if (isset($item['document'])) {
                    $documents[] = self::formatDocument($item['document']);
                }
            }
        }

        return $documents;
    }

    /**
     * GET Single Document
     */
    public static function getDocument($collectionName, $docId)
    {
        $projectId = self::getProjectId();
        $url = "https://firestore.googleapis.com/v1/projects/{$projectId}/databases/(default)/documents/{$collectionName}/{$docId}";

        $ch = curl_init($url);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        $response = curl_exec($ch);
        curl_close($ch);

        $json = json_decode($response, true);
        return isset($json['name']) ? self::formatDocument($json) : null;
    }

    /**
     * CREATE or UPDATE Document
     */
    public static function setDocument($collectionName, $docId, $data)
    {
        $projectId = self::getProjectId();
        $apiKey = env('FIREBASE_API_KEY', 'AIzaSyCknq4ArQmWsLgGDNm0uH4fqPs4I1eQE4A');
        $url = "https://firestore.googleapis.com/v1/projects/{$projectId}/databases/(default)/documents/{$collectionName}/{$docId}?key={$apiKey}";

        $payload = json_encode(self::encodeFields($data));

        $ch = curl_init($url);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'PATCH');
        curl_setopt($ch, CURLOPT_POSTFIELDS, $payload);
        curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        $response = curl_exec($ch);
        curl_close($ch);

        $json = json_decode($response, true);
        return isset($json['name']) ? self::formatDocument($json) : null;
    }

    /**
     * DELETE Document
     */
    public static function deleteDocument($collectionName, $docId)
    {
        $projectId = self::getProjectId();
        $apiKey = env('FIREBASE_API_KEY', 'AIzaSyCknq4ArQmWsLgGDNm0uH4fqPs4I1eQE4A');
        $url = "https://firestore.googleapis.com/v1/projects/{$projectId}/databases/(default)/documents/{$collectionName}/{$docId}?key={$apiKey}";

        $ch = curl_init($url);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'DELETE');
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        $response = curl_exec($ch);
        curl_close($ch);

        return true;
    }
}
