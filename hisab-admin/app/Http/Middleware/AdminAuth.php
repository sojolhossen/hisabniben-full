<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Session;

class AdminAuth
{
    public function handle(Request $request, Closure $next)
    {
        if (!Session::has('admin_authenticated')) {
            return redirect()->route('login')->with('error', 'Please login to access the admin panel.');
        }

        return $next($request);
    }
}
