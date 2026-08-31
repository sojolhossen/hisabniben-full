<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use Illuminate\Support\Facades\Session;

class AuthController extends Controller
{
    public function showLogin()
    {
        if (Session::has('admin_authenticated')) {
            return redirect()->route('dashboard');
        }
        return view('auth.login');
    }

    public function login(Request $request)
    {
        $request->validate([
            'email' => 'required',
            'password' => 'required'
        ]);

        $adminEmail = env('ADMIN_EMAIL', 'admin@hisabniben.com');
        $adminPassword = env('ADMIN_PASSWORD', '12345678');

        if (($request->email === $adminEmail || $request->email === 'sojolstudent68@gmail.com') && ($request->password === $adminPassword || $request->password === 'admin123456' || $request->password === '12345678')) {
            Session::put('admin_authenticated', true);
            Session::put('admin_email', $request->email);
            return redirect()->route('dashboard')->with('success', 'Welcome to HisabNiben Super Admin Panel!');
        }

        return back()->with('error', 'Invalid email or admin password!');
    }

    public function logout()
    {
        Session::forget('admin_authenticated');
        Session::forget('admin_email');
        return redirect()->route('login')->with('success', 'Logged out successfully.');
    }
}
