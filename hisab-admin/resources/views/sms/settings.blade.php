@extends('layouts.app')

@section('title', 'SMS Gateway Settings')

@section('content')
<div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:24px;">
    <div>
        <h1 style="font-size:24px; font-weight:800; color:var(--dark);">BulkSMSBD API & Gateway Settings</h1>
        <p style="font-size:13.5px; color:var(--text-muted); margin-top:2px;">Manage BulkSMSBD API keys, Sender ID, live connectivity, and SMS dispatch history logs</p>
    </div>

    <form method="POST" action="{{ route('sms.test') }}">
        @csrf
        <button type="submit" class="btn btn-outline" style="color:var(--info); border-color:var(--info);"><i class="fas fa-plug-circle-check"></i> Test API Connection</button>
    </form>
</div>

<div style="display:grid; grid-template-columns: 1fr 2fr; gap:24px;">
    <!-- CREDENTIALS FORM -->
    <div class="card-panel">
        <h3 style="font-size:16px; font-weight:800; margin-bottom:16px;"><i class="fas fa-key" style="color:var(--primary);"></i> API Credentials</h3>

        <form method="POST" action="{{ route('sms.settings.save') }}">
            @csrf
            <div style="margin-bottom:14px;">
                <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">BulkSMSBD API Key</label>
                <input type="password" name="apiKey" value="{{ $smsApiDoc['apiKey'] ?? '' }}" required class="btn btn-outline" style="width:100%; text-align:left;" placeholder="API Key...">
            </div>

            <div style="margin-bottom:18px;">
                <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">Sender ID (Approved Masking / Non-Masking)</label>
                <input type="text" name="senderId" value="{{ $smsApiDoc['senderId'] ?? '' }}" required class="btn btn-outline" style="width:100%; text-align:left;" placeholder="8809612... or 8801...">
            </div>

            <button type="submit" class="btn btn-primary" style="width:100%; justify-content:center; padding:12px;"><i class="fas fa-save"></i> Save SMS Gateway API</button>
        </form>
    </div>

    <!-- RECENT SMS DISPATCH LOGS -->
    <div class="card-panel">
        <h3 style="font-size:16px; font-weight:800; margin-bottom:16px;"><i class="fas fa-list-check" style="color:var(--info);"></i> Transactional SMS Dispatch Logs</h3>

        <div class="table-responsive">
            <table class="data-table">
                <thead>
                    <tr>
                        <th>Recipient / Merchant</th>
                        <th>SMS Content</th>
                        <th>Sender ID Used</th>
                        <th>Sent Date</th>
                    </tr>
                </thead>
                <tbody>
                    @forelse($history as $h)
                        <tr>
                            <td><code>{{ $h['recipientPhone'] ?? $h['phone'] ?? '-' }}</code></td>
                            <td>
                                <div style="font-size:12px; color:var(--text-main); line-height:1.4;">{{ Str::limit($h['message'] ?? $h['smsBody'] ?? '', 50) }}</div>
                            </td>
                            <td><span class="badge badge-info">{{ $h['businessNameUsed'] ?? $h['senderId'] ?? 'HisabNiben' }}</span></td>
                            <td>
                                @if(!empty($h['createdAt']))
                                    {{ date('d M, h:i A', is_numeric($h['createdAt']) ? $h['createdAt']/1000 : time()) }}
                                @else
                                    -
                                @endif
                            </td>
                        </tr>
                    @empty
                        <tr>
                            <td colspan="4" style="text-align:center; padding:20px; color:var(--text-muted);">No SMS dispatch history found.</td>
                        </tr>
                    @endforelse
                </tbody>
            </table>
        </div>
    </div>
</div>
@endsection
