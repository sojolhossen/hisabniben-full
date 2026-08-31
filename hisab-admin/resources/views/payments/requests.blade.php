@extends('layouts.app')

@section('title', 'Payment Verification Requests')

@section('content')
<div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:24px;">
    <div>
        <h1 style="font-size:24px; font-weight:800; color:var(--dark);">Payment Requests & Idempotent Verification</h1>
        <p style="font-size:13.5px; color:var(--text-muted); margin-top:2px;">Approve manual subscription payments, verify TrxIDs, and auto-upgrade merchant accounts</p>
    </div>
</div>

<!-- SEARCH & FILTER BAR -->
<div class="card-panel" style="padding:16px 20px; margin-bottom:20px;">
    <form method="GET" action="{{ route('payments.requests') }}" style="display:flex; gap:12px; flex-wrap:wrap; align-items:center;">
        <div style="flex:1; min-width:240px;">
            <input type="text" name="search" value="{{ $search }}" placeholder="Search by TrxID or phone number..." class="btn btn-outline" style="width:100%; text-align:left; background:var(--bg-body); cursor:text;">
        </div>

        <select name="status" class="btn btn-outline" style="font-size:12.5px; font-weight:700;" onchange="this.form.submit()">
            <option value="ALL" {{ $status == 'ALL' ? 'selected' : '' }}>All Status</option>
            <option value="PENDING" {{ $status == 'PENDING' ? 'selected' : '' }}>Pending Only</option>
            <option value="APPROVED" {{ $status == 'APPROVED' ? 'selected' : '' }}>Approved Only</option>
            <option value="REJECTED" {{ $status == 'REJECTED' ? 'selected' : '' }}>Rejected Only</option>
        </select>

        <button type="submit" class="btn btn-primary"><i class="fas fa-filter"></i> Filter</button>
    </form>
</div>

<!-- REQUESTS TABLE -->
<div class="card-panel">
    <div class="table-responsive">
        <table class="data-table">
            <thead>
                <tr>
                    <th>Merchant / Phone</th>
                    <th>Package Details</th>
                    <th>Payment Method</th>
                    <th>Transaction ID</th>
                    <th>Amount</th>
                    <th>Status</th>
                    <th style="text-align:right;">Actions</th>
                </tr>
            </thead>
            <tbody>
                @forelse($requests as $r)
                    @php
                        $pStatus = strtoupper($r['status'] ?? 'PENDING');
                        $trxId = strtoupper(trim($r['transactionId'] ?? ''));
                        $isDuplicate = ($trxCounts[$trxId] ?? 0) > 1;
                    @endphp
                    <tr>
                        <td>
                            <strong>{{ $r['phone'] ?? $r['userId'] ?? 'Merchant' }}</strong>
                            <div style="font-size:11px; color:var(--text-muted);">UID: {{ substr($r['userId'] ?? '', 0, 12) }}...</div>
                        </td>
                        <td>
                            <strong>{{ $r['packageName'] ?? 'Premium Package' }}</strong>
                            <div style="font-size:11px; color:var(--text-muted);">{{ $r['durationDays'] ?? 30 }} Days Duration</div>
                        </td>
                        <td><span class="badge badge-info">{{ strtoupper($r['paymentMethod'] ?? 'Bkash') }}</span></td>
                        <td>
                            <strong style="color:var(--primary); font-size:14px;">{{ $trxId ?: '-' }}</strong>
                            @if($isDuplicate)
                                <span class="badge badge-danger" title="Duplicate TrxID detected! High Risk!">⚠️ HIGH RISK DUPLICATE</span>
                            @endif
                        </td>
                        <td><strong style="font-size:15px; color:var(--dark);">৳{{ number_format($r['amount'] ?? 0, 2) }}</strong></td>
                        <td>
                            @if($pStatus === 'APPROVED')
                                <span class="badge badge-success">APPROVED</span>
                            @elseif($pStatus === 'REJECTED')
                                <span class="badge badge-danger">REJECTED</span>
                            @else
                                <span class="badge badge-warning">PENDING</span>
                            @endif
                        </td>
                        <td style="text-align:right;">
                            @if($pStatus === 'PENDING')
                                <div style="display:inline-flex; gap:6px;">
                                    <form method="POST" action="{{ route('payments.requests.approve') }}" style="display:inline;">
                                        @csrf
                                        <input type="hidden" name="id" value="{{ $r['id'] }}">
                                        <button type="submit" class="btn btn-primary" style="padding:4px 10px; font-size:11.5px; background:var(--success);">
                                            <i class="fas fa-check"></i> Approve
                                        </button>
                                    </form>

                                    <form method="POST" action="{{ route('payments.requests.reject') }}" style="display:inline;">
                                        @csrf
                                        <input type="hidden" name="id" value="{{ $r['id'] }}">
                                        <button type="submit" class="btn btn-danger" style="padding:4px 10px; font-size:11.5px;">
                                            <i class="fas fa-xmark"></i> Reject
                                        </button>
                                    </form>
                                </div>
                            @else
                                <span style="font-size:12px; color:var(--text-muted); font-weight:600;">Completed</span>
                            @endif
                        </td>
                    </tr>
                @empty
                    <tr>
                        <td colspan="7" style="text-align:center; padding:30px; color:var(--text-muted);">No payment requests match the current search or status filter.</td>
                    </tr>
                @endforelse
            </tbody>
        </table>
    </div>
</div>
@endsection
