@extends('layouts.app')

@section('title', 'System Settings')

@section('content')
<div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:24px;">
    <div>
        <h1 style="font-size:24px; font-weight:800; color:var(--dark);">System Settings & Configuration</h1>
        <p style="font-size:13.5px; color:var(--text-muted); margin-top:2px;">Support helpline, trial duration, system maintenance, and JSON database backup</p>
    </div>

    <a href="{{ route('settings.export-json') }}" class="btn btn-outline" style="color:var(--success); border-color:var(--success);"><i class="fas fa-download"></i> Export JSON Backup</a>
</div>

<div style="display:grid; grid-template-columns: 1fr 1fr; gap:24px;">
    <!-- SUPPORT HELPLINE -->
    <div class="card-panel">
        <h3 style="font-size:16px; font-weight:800; margin-bottom:16px;"><i class="fas fa-headset" style="color:var(--primary);"></i> Customer Support Helpline (`settings/support`)</h3>

        <form method="POST" action="{{ route('settings.save') }}">
            @csrf
            <input type="hidden" name="type" value="support">

            <div style="margin-bottom:14px;">
                <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">Support Phone Number</label>
                <input type="text" name="phone" value="{{ $support['phone'] ?? '01310997904' }}" required class="btn btn-outline" style="width:100%; text-align:left;">
            </div>

            <div style="margin-bottom:14px;">
                <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">WhatsApp Support Number</label>
                <input type="text" name="whatsapp" value="{{ $support['whatsapp'] ?? '01310997904' }}" required class="btn btn-outline" style="width:100%; text-align:left;">
            </div>

            <div style="margin-bottom:14px;">
                <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">Support Email</label>
                <input type="email" name="email" value="{{ $support['email'] ?? 'support@hisabniben.com' }}" required class="btn btn-outline" style="width:100%; text-align:left;">
            </div>

            <div style="margin-bottom:18px;">
                <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">Available Hours</label>
                <input type="text" name="availableHours" value="{{ $support['availableHours'] ?? 'Sat - Thu: 9 AM - 9 PM' }}" class="btn btn-outline" style="width:100%; text-align:left;">
            </div>

            <button type="submit" class="btn btn-primary" style="width:100%; justify-content:center; padding:11px;"><i class="fas fa-save"></i> Save Helpline Settings</button>
        </form>
    </div>

    <!-- FREE TRIAL & SYSTEM CONFIG -->
    <div class="card-panel">
        <h3 style="font-size:16px; font-weight:800; margin-bottom:16px;"><i class="fas fa-sliders" style="color:var(--info);"></i> Free Trial & System Options (`settings/system`)</h3>

        <form method="POST" action="{{ route('settings.save') }}">
            @csrf
            <input type="hidden" name="type" value="system">

            <div style="margin-bottom:16px;">
                <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">Free Trial Duration (Days on Signup)</label>
                <input type="number" name="trialDurationDays" value="{{ $system['trialDurationDays'] ?? 7 }}" required class="btn btn-outline" style="width:100%; text-align:left;">
            </div>

            <div style="margin-bottom:16px; display:flex; align-items:center; gap:8px;">
                <input type="checkbox" name="autoTrialOnSignup" id="st-trial" style="width:16px; height:16px;" {{ !isset($system['autoTrialOnSignup']) || !empty($system['autoTrialOnSignup']) ? 'checked' : '' }}>
                <label for="st-trial" style="font-size:13px; font-weight:700;">Auto-Start Free Trial on New Signup</label>
            </div>

            <div style="margin-bottom:20px; display:flex; align-items:center; gap:8px;">
                <input type="checkbox" name="maintenanceMode" id="st-maint" style="width:16px; height:16px;" {{ !empty($system['maintenanceMode']) ? 'checked' : '' }}>
                <label for="st-maint" style="font-size:13px; font-weight:700; color:var(--danger);">Enable System Maintenance Mode</label>
            </div>

            <button type="submit" class="btn btn-primary" style="width:100%; justify-content:center; padding:11px;"><i class="fas fa-save"></i> Save System Options</button>
        </form>
    </div>
</div>
@endsection
