<?php

namespace App\Http\Controllers;

use App\Helpers\FirestoreHelper;
use Illuminate\Http\Request;

class TutorialController extends Controller
{
    public function index(Request $request)
    {
        $tutorials = FirestoreHelper::getCollection('tutorials', 100);
        return view('tutorials.index', ['tutorials' => $tutorials]);
    }

    public function save(Request $request)
    {
        $id = $request->input('id') ?: ('TUTORIAL_' . time());
        $data = [
            'title' => $request->input('title'),
            'youtubeUrl' => $request->input('youtubeUrl'),
            'youtubeId' => $request->input('youtubeId', ''),
            'description' => $request->input('description', ''),
            'isPublished' => $request->has('isPublished'),
            'updatedAt' => time() * 1000
        ];

        FirestoreHelper::setDocument('tutorials', $id, $data);
        return back()->with('success', 'Video tutorial saved and synced with Android app!');
    }
}
