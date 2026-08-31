<?php

namespace App\Http\Controllers;

use App\Helpers\FirestoreHelper;
use Illuminate\Http\Request;

class PackageController extends Controller
{
    public function index(Request $request)
    {
        $rawPackages = FirestoreHelper::getCollection('packages', 100);

        $packages = array_map(function($p) {
            $features = [];
            if (isset($p['features'])) {
                if (is_array($p['features'])) {
                    $features = $p['features'];
                } else if (is_string($p['features'])) {
                    $features = array_filter(array_map('trim', explode("\n", $p['features'])));
                }
            }

            return [
                'id' => $p['id'] ?? ('PKG_' . time()),
                'name' => $p['name'] ?? 'Package',
                'price' => (float)($p['price'] ?? 0),
                'originalPrice' => (float)($p['originalPrice'] ?? $p['price'] ?? 0),
                'durationDays' => (int)($p['durationDays'] ?? 30),
                'playStoreProductId' => $p['playStoreProductId'] ?? '',
                'description' => $p['description'] ?? '',
                'status' => $p['status'] ?? 'active',
                'isActive' => ($p['status'] ?? 'active') === 'active',
                'isPopular' => !empty($p['isPopular']),
                'badgeTag' => $p['badgeTag'] ?? ($p['popularTag'] ?? (!empty($p['isPopular']) ? 'MOST POPULAR' : '')),
                'smsCount' => (int)($p['smsCount'] ?? $p['smsBonus'] ?? 0),
                'features' => $features,
                'featuresText' => implode("\n", $features),
                'updatedAt' => $p['updatedAt'] ?? time() * 1000
            ];
        }, $rawPackages);

        return view('packages.index', ['packages' => $packages]);
    }

    public function save(Request $request)
    {
        $id = $request->input('id') ?: ('PKG_' . time());

        $featuresInput = $request->input('features', '');
        $featuresList = array_values(array_filter(array_map('trim', explode("\n", $featuresInput))));

        $status = $request->input('status', 'active');
        $isPopular = $request->has('isPopular');
        $price = (float)$request->input('price');
        $origPrice = $request->input('originalPrice') !== null ? (float)$request->input('originalPrice') : $price;

        $data = [
            'name' => $request->input('name'),
            'price' => $price,
            'originalPrice' => $origPrice,
            'durationDays' => (int)$request->input('durationDays'),
            'playStoreProductId' => trim($request->input('playStoreProductId', '')),
            'description' => $request->input('description', ''),
            'status' => $status,
            'isPopular' => $isPopular,
            'badgeTag' => $request->input('badgeTag', $isPopular ? 'MOST POPULAR' : ''),
            'smsCount' => (int)$request->input('smsCount', 0),
            'features' => $featuresList,
            'type' => 'subscription',
            'packageType' => 'subscription',
            'updatedAt' => time() * 1000
        ];

        FirestoreHelper::setDocument('packages', $id, $data);
        return back()->with('success', 'Subscription package updated successfully in Firebase!');
    }

    public function delete(Request $request)
    {
        $id = $request->input('id');
        FirestoreHelper::deleteDocument('packages', $id);
        return back()->with('success', 'Subscription package deleted.');
    }
}
