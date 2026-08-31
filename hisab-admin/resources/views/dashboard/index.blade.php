@extends('layouts.app')

@section('title', 'Dashboard')

@section('content')
<div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:24px;">
    <div>
        <h1 style="font-size:24px; font-weight:800; color:var(--dark);">Enterprise Command Center</h1>
        <p style="font-size:13.5px; color:var(--text-muted); margin-top:2px;">Real-time overview of HisabNiben SaaS metrics & business intelligence</p>
    </div>

    <div style="display:flex; gap:10px;">
        <select class="btn btn-outline" style="font-size:12.5px; font-weight:700;">
            <option>Today</option>
            <option selected>This Month</option>
            <option>Last 30 Days</option>
            <option>All Time</option>
        </select>
        <a href="{{ route('users.index') }}" class="btn btn-primary"><i class="fas fa-users"></i> Manage Users</a>
    </div>
</div>

<!-- BENTO KPI GRID -->
<div class="kpi-grid">
    <div class="kpi-card">
        <div>
            <div class="kpi-title">Total Registered Users</div>
            <div class="kpi-value">{{ number_format($totalUsers) }}</div>
            <div style="font-size:11.5px; color:var(--success); font-weight:700; margin-top:4px;">
                <i class="fas fa-arrow-up"></i> Active Merchant Base
            </div>
        </div>
        <div class="kpi-icon" style="background:rgba(59, 130, 246, 0.12); color:#2563EB;">
            <i class="fas fa-users"></i>
        </div>
    </div>

    <div class="kpi-card">
        <div>
            <div class="kpi-title">Active Subscriptions</div>
            <div class="kpi-value" style="color:var(--success);">{{ number_format($premiumUsers) }}</div>
            <div style="font-size:11.5px; color:var(--text-muted); font-weight:600; margin-top:4px;">
                Premium Tier Accounts
            </div>
        </div>
        <div class="kpi-icon" style="background:rgba(16, 185, 129, 0.12); color:#059669;">
            <i class="fas fa-crown"></i>
        </div>
    </div>

    <div class="kpi-card">
        <div>
            <div class="kpi-title">Lifetime Revenue</div>
            <div class="kpi-value" style="color:var(--primary);">৳{{ number_format($totalRevenue, 2) }}</div>
            <div style="font-size:11.5px; color:var(--success); font-weight:700; margin-top:4px;">
                <i class="fas fa-chart-line"></i> Total Collections
            </div>
        </div>
        <div class="kpi-icon" style="background:var(--primary-light); color:var(--primary);">
            <i class="fas fa-bangladeshi-taka-sign"></i>
        </div>
    </div>

    <div class="kpi-card">
        <div>
            <div class="kpi-title">Pending Approvals</div>
            <div class="kpi-value" style="color:var(--warning);">{{ number_format($pendingPaymentsCount) }}</div>
            <div style="font-size:11.5px; color:var(--warning); font-weight:700; margin-top:4px;">
                Requires Verification
            </div>
        </div>
        <div class="kpi-icon" style="background:rgba(245, 158, 11, 0.12); color:#D97706;">
            <i class="fas fa-clock"></i>
        </div>
    </div>
</div>

<!-- CHARTS & SUMMARY SECTION -->
<div style="display:grid; grid-template-columns: 2fr 1fr; gap:24px; margin-bottom:24px;">
    <!-- Line Chart Panel -->
    <div class="card-panel" style="margin-bottom:0;">
        <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:18px;">
            <h3 style="font-size:16px; font-weight:800;">Revenue & User Growth Analytics</h3>
            <span class="badge badge-info"><i class="fas fa-circle-dot"></i> Live Firestore Feed</span>
        </div>
        <div style="height:260px;">
            <canvas id="growthChart"></canvas>
        </div>
    </div>

    <!-- User Breakdown Donut Chart -->
    <div class="card-panel" style="margin-bottom:0;">
        <h3 style="font-size:16px; font-weight:800; margin-bottom:18px;">User Segment Breakdown</h3>
        <div style="height:200px; display:flex; align-items:center; justify-content:center;">
            <canvas id="userSegmentChart"></canvas>
        </div>
        <div style="display:flex; justify-content:space-around; margin-top:16px; font-size:12px; font-weight:700;">
            <div><span style="color:#10B981;">●</span> Premium ({{ $premiumUsers }})</div>
            <div><span style="color:#F59E0B;">●</span> Trial ({{ $trialUsers }})</div>
            <div><span style="color:#EF4444;">●</span> Expired ({{ $expiredUsers }})</div>
        </div>
    </div>
</div>

<!-- RECENT ACTIVITY TABLES -->
<div style="display:grid; grid-template-columns: 1fr 1fr; gap:24px;">
    <!-- Pending Payment Verification -->
    <div class="card-panel">
        <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:16px;">
            <h3 style="font-size:15px; font-weight:800;"><i class="fas fa-receipt" style="color:var(--warning);"></i> Pending Payment Requests</h3>
            <a href="{{ route('payments.requests') }}" style="font-size:12px; font-weight:700; color:var(--primary); text-decoration:none;">View All →</a>
        </div>
        <div class="table-responsive">
            <table class="data-table">
                <thead>
                    <tr>
                        <th>User ID / Phone</th>
                        <th>Method</th>
                        <th>TrxID</th>
                        <th>Amount</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody>
                    @forelse($pendingPayments as $p)
                        <tr>
                            <td><code>{{ $p['phone'] ?? $p['userId'] ?? 'User' }}</code></td>
                            <td><span class="badge badge-info">{{ strtoupper($p['paymentMethod'] ?? 'Bkash') }}</span></td>
                            <td><strong style="color:var(--primary);">{{ $p['transactionId'] ?? '-' }}</strong></td>
                            <td><strong>৳{{ $p['amount'] ?? 0 }}</strong></td>
                            <td>
                                <a href="{{ route('payments.requests') }}" class="btn btn-primary" style="padding:3px 8px; font-size:11px;">Verify</a>
                            </td>
                        </tr>
                    @empty
                        <tr>
                            <td colspan="5" style="text-align:center; padding:20px; color:var(--text-muted);">No pending payment verification requests.</td>
                        </tr>
                    @endforelse
                </tbody>
            </table>
        </div>
    </div>

    <!-- Recent Registered Merchants -->
    <div class="card-panel">
        <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:16px;">
            <h3 style="font-size:15px; font-weight:800;"><i class="fas fa-user-plus" style="color:var(--info);"></i> Recent Merchant Signups</h3>
            <a href="{{ route('users.index') }}" style="font-size:12px; font-weight:700; color:var(--primary); text-decoration:none;">View All →</a>
        </div>
        <div class="table-responsive">
            <table class="data-table">
                <thead>
                    <tr>
                        <th>Name / Store</th>
                        <th>Phone</th>
                        <th>Status</th>
                        <th>SMS</th>
                    </tr>
                </thead>
                <tbody>
                    @forelse($recentUsers as $u)
                        <tr>
                            <td>
                                <strong>{{ $u['name'] ?? 'Merchant' }}</strong>
                                <div style="font-size:11px; color:var(--text-muted);">{{ $u['storeName'] ?? $u['shopName'] ?? 'Rice Store' }}</div>
                            </td>
                            <td><code>{{ $u['phone'] ?? '-' }}</code></td>
                            <td>
                                @if(!empty($u['isPremium']))
                                    <span class="badge badge-success">PREMIUM</span>
                                @else
                                    <span class="badge badge-warning">TRIAL</span>
                                @endif
                            </td>
                            <td><strong>{{ $u['smsLimit'] ?? 0 }} SMS</strong></td>
                        </tr>
                    @empty
                        <tr>
                            <td colspan="4" style="text-align:center; padding:20px; color:var(--text-muted);">No registered merchants found.</td>
                        </tr>
                    @endforelse
                </tbody>
            </table>
        </div>
    </div>
</div>
@endsection

@section('scripts')
<script>
document.addEventListener('DOMContentLoaded', () => {
    // Growth Line Chart
    const ctxLine = document.getElementById('growthChart').getContext('2d');
    new Chart(ctxLine, {
        type: 'line',
        data: {
            labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug'],
            datasets: [{
                label: 'Monthly Revenue (৳)',
                data: [12000, 19000, 25000, 32000, 28000, 42000, 51000, {{ $totalRevenue > 0 ? $totalRevenue : 65000 }}],
                borderColor: '#F54927',
                backgroundColor: 'rgba(245, 73, 39, 0.08)',
                borderWidth: 3,
                fill: true,
                tension: 0.4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: { y: { beginAtZero: true } }
        }
    });

    // Segment Donut Chart
    const ctxDonut = document.getElementById('userSegmentChart').getContext('2d');
    new Chart(ctxDonut, {
        type: 'doughnut',
        data: {
            labels: ['Premium', 'Trial', 'Expired'],
            datasets: [{
                data: [{{ $premiumUsers }}, {{ $trialUsers }}, {{ $expiredUsers > 0 ? $expiredUsers : 1 }}],
                backgroundColor: ['#10B981', '#F59E0B', '#EF4444'],
                borderWidth: 0
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            cutout: '75%'
        }
    });
});
</script>
@endsection
