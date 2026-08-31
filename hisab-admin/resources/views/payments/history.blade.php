@extends('layouts.app')

@section('title', 'Purchase History & Revenue Logs')

@section('content')
<div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:24px;">
    <div>
        <h1 style="font-size:24px; font-weight:800; color:var(--dark);">Purchase History & Revenue Control</h1>
        <p style="font-size:13.5px; color:var(--text-muted); margin-top:2px;">Complete audit log of all completed subscription purchases and transactions</p>
    </div>

    <div style="display:flex; gap:10px;">
        <div class="card-panel" style="padding:10px 18px; margin-bottom:0; display:flex; align-items:center; gap:10px;">
            <div style="font-size:11px; font-weight:700; color:var(--text-muted);">TOTAL REVENUE</div>
            <div style="font-size:18px; font-weight:800; color:var(--primary);">৳{{ number_format($totalRevenue, 2) }}</div>
        </div>
    </div>
</div>

<div class="card-panel">
    <div class="table-responsive">
        <table class="data-table">
            <thead>
                <tr>
                    <th>Merchant Phone</th>
                    <th>Package Name</th>
                    <th>Duration</th>
                    <th>Method</th>
                    <th>Transaction ID</th>
                    <th>Amount</th>
                    <th>Date</th>
                </tr>
            </thead>
            <tbody>
                @forelse($purchases as $p)
                    <tr>
                        <td><code>{{ $p['phone'] ?? $p['userId'] ?? '-' }}</code></td>
                        <td><strong>{{ $p['packageName'] ?? 'Premium' }}</strong></td>
                        <td>{{ $p['durationDays'] ?? 30 }} Days</td>
                        <td><span class="badge badge-info">{{ strtoupper($p['paymentMethod'] ?? 'Bkash') }}</span></td>
                        <td><strong style="color:var(--primary);">{{ $p['transactionId'] ?? '-' }}</strong></td>
                        <td><strong style="font-size:14px; color:var(--dark);">৳{{ number_format($p['amount'] ?? 0, 2) }}</strong></td>
                        <td>
                            @if(!empty($p['createdAt']))
                                {{ date('d M, Y h:i A', is_numeric($p['createdAt']) ? $p['createdAt']/1000 : time()) }}
                            @else
                                -
                            @endif
                        </td>
                    </tr>
                @empty
                    <tr>
                        <td colspan="7" style="text-align:center; padding:30px; color:var(--text-muted);">No completed purchase records found in history.</td>
                    </tr>
                @endforelse
            </tbody>
        </table>
    </div>
</div>
@endsection
