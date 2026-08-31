@extends('layouts.app')

@section('title', 'Users & CRM')

@section('content')
<div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:24px;">
    <div>
        <h1 style="font-size:24px; font-weight:800; color:var(--dark);">Merchant Directory & A-to-Z CRM</h1>
        <p style="font-size:13.5px; color:var(--text-muted); margin-top:2px;">Manage user accounts, business insights, customer dues, suppliers, subscriptions & account deletion</p>
    </div>
</div>

<!-- SEARCH & FILTER BAR -->
<div class="card-panel" style="padding:16px 20px; margin-bottom:20px;">
    <form method="GET" action="{{ route('users.index') }}" style="display:flex; gap:12px; flex-wrap:wrap; align-items:center;">
        <div style="flex:1; min-width:240px; position:relative;">
            <input type="text" name="search" value="{{ $search }}" placeholder="Search by merchant name, phone, or store..." class="btn btn-outline" style="width:100%; text-align:left; background:var(--bg-body); cursor:text;">
        </div>

        <select name="status" class="btn btn-outline" style="font-size:12.5px; font-weight:700;" onchange="this.form.submit()">
            <option value="ALL" {{ $status == 'ALL' ? 'selected' : '' }}>All Status</option>
            <option value="PREMIUM" {{ $status == 'PREMIUM' ? 'selected' : '' }}>Premium Only</option>
            <option value="TRIAL" {{ $status == 'TRIAL' ? 'selected' : '' }}>Trial Only</option>
            <option value="EXPIRED" {{ $status == 'EXPIRED' ? 'selected' : '' }}>Expired Only</option>
            <option value="BLOCKED" {{ $status == 'BLOCKED' ? 'selected' : '' }}>Blocked Only</option>
        </select>

        <button type="submit" class="btn btn-primary"><i class="fas fa-filter"></i> Apply Filter</button>
        @if($search || $status != 'ALL')
            <a href="{{ route('users.index') }}" class="btn btn-outline" style="color:var(--danger); border-color:var(--danger);"><i class="fas fa-xmark"></i> Clear</a>
        @endif
    </form>
</div>

<!-- USERS DATA TABLE -->
<div class="card-panel">
    <div class="table-responsive">
        <table class="data-table">
            <thead>
                <tr>
                    <th>Merchant Name</th>
                    <th>Store / Mill Name</th>
                    <th>Phone</th>
                    <th>Status</th>
                    <th>SMS Balance</th>
                    <th>Expiry / Trial</th>
                    <th style="text-align:right;">Actions</th>
                </tr>
            </thead>
            <tbody>
                @forelse($users as $u)
                    @php
                        $now = time() * 1000;
                        $initial = strtoupper(substr($u['name'] ?? $u['phone'] ?? 'M', 0, 1));
                    @endphp
                    <tr>
                        <td>
                            <div style="display:flex; align-items:center; gap:12px; cursor:pointer;" onclick="openCrmModal('{{ $u['id'] }}')">
                                <div style="width:36px; height:36px; border-radius:50%; background:var(--primary); color:white; font-weight:800; display:flex; align-items:center; justify-content:center; font-size:14px;">{{ $initial }}</div>
                                <div>
                                    <strong style="color:var(--dark);">{{ $u['name'] ?? 'Merchant' }}</strong>
                                    <div style="font-size:11px; color:var(--text-muted);">{{ $u['email'] ?? $u['id'] }}</div>
                                </div>
                            </div>
                        </td>
                        <td><strong>{{ $u['storeName'] ?? $u['shopName'] ?? '-' }}</strong></td>
                        <td><code>{{ $u['phone'] ?? '-' }}</code></td>
                        <td>
                            @if(!empty($u['isBlocked']) || !empty($u['disabled']))
                                <span class="badge badge-danger">BLOCKED</span>
                            @elseif(!empty($u['isPremium']))
                                <span class="badge badge-success">PREMIUM</span>
                            @elseif(!empty($u['trialEnd']) && $u['trialEnd'] > $now)
                                <span class="badge badge-warning">TRIAL</span>
                            @else
                                <span class="badge badge-danger">EXPIRED</span>
                            @endif
                        </td>
                        <td><strong style="color:#3B82F6;">{{ $u['smsLimit'] ?? 0 }} SMS</strong></td>
                        <td>
                            @if(!empty($u['isPremium']) && !empty($u['subscriptionExpiryDate']))
                                @php $days = ceil(($u['subscriptionExpiryDate'] - $now) / (1000*60*60*24)); @endphp
                                {{ $days >= 999 ? 'Life-Time' : ($days > 0 ? $days.' days left' : 'Expired today') }}
                            @elseif(!empty($u['trialEnd']) && $u['trialEnd'] > $now)
                                @php $trialDays = ceil(($u['trialEnd'] - $now) / (1000*60*60*24)); @endphp
                                {{ $trialDays }} trial day(s)
                            @else
                                Expired
                            @endif
                        </td>
                        <td style="text-align:right;">
                            <div style="display:inline-flex; gap:6px;">
                                <button class="btn btn-primary" style="padding:4px 8px; font-size:11px;" onclick="openCrmModal('{{ $u['id'] }}')">
                                    <i class="fas fa-eye"></i> CRM
                                </button>
                                <button class="btn btn-outline" style="padding:4px 8px; font-size:11px;" onclick="openEditModal('{{ $u['id'] }}', '{{ addslashes($u['name'] ?? '') }}', '{{ addslashes($u['phone'] ?? '') }}', '{{ addslashes($u['email'] ?? '') }}', '{{ addslashes($u['storeName'] ?? '') }}')">
                                    <i class="fas fa-edit"></i>
                                </button>
                                <button class="btn btn-danger" style="padding:4px 8px; font-size:11px;" onclick="openDeleteModal('{{ $u['id'] }}', '{{ addslashes($u['name'] ?? 'Merchant') }}', '{{ addslashes($u['phone'] ?? '') }}')">
                                    <i class="fas fa-trash-alt"></i>
                                </button>
                            </div>
                        </td>
                    </tr>
                @empty
                    <tr>
                        <td colspan="7" style="text-align:center; padding:30px; color:var(--text-muted);">No users match the current search or status filter.</td>
                    </tr>
                @endforelse
            </tbody>
        </table>
    </div>
</div>

<!-- CRM MODAL OVERLAY -->
<div id="crm-modal" style="display:none; position:fixed; inset:0; background:rgba(15,23,42,0.6); backdrop-filter:blur(4px); z-index:1000; align-items:center; justify-content:center; padding:20px;">
    <div style="background:white; border-radius:16px; width:100%; max-width:900px; max-height:90vh; display:flex; flex-direction:column; overflow:hidden; box-shadow:0 25px 50px rgba(0,0,0,0.25);">
        <!-- Header -->
        <div style="padding:20px 24px; background:var(--dark); color:white; display:flex; align-items:center; justify-content:space-between;">
            <div>
                <h3 id="crm-user-name" style="font-size:18px; font-weight:800; margin:0;">Merchant CRM Profile</h3>
                <p id="crm-user-store" style="font-size:12px; color:#94A3B8; margin-top:2px; margin-bottom:0;">Loading business insights...</p>
            </div>
            <button onclick="closeCrmModal()" style="background:none; border:none; color:white; font-size:20px; cursor:pointer;"><i class="fas fa-times"></i></button>
        </div>

        <!-- Body -->
        <div style="padding:24px; overflow-y:auto; flex:1;">
            <!-- 6 Bento Insight Cards Grid -->
            <div style="display:grid; grid-template-columns:repeat(3, 1fr); gap:14px; margin-bottom:24px;">
                <div style="background:var(--bg-body); padding:14px; border-radius:10px; border:1px solid var(--border-color);">
                    <div style="font-size:11px; font-weight:700; color:var(--text-muted);"><i class="fas fa-users" style="color:#3B82F6;"></i> CUSTOMERS & DUES</div>
                    <div id="crm-stat-customers" style="font-size:18px; font-weight:800; color:var(--dark); margin-top:4px;">0 Customers</div>
                    <div id="crm-stat-dues" style="font-size:12px; font-weight:700; color:var(--danger); margin-top:2px;">৳0 Due</div>
                </div>

                <div style="background:var(--bg-body); padding:14px; border-radius:10px; border:1px solid var(--border-color);">
                    <div style="font-size:11px; font-weight:700; color:var(--text-muted);"><i class="fas fa-store" style="color:#F59E0B;"></i> MAHAJON & PAYABLES</div>
                    <div id="crm-stat-suppliers" style="font-size:18px; font-weight:800; color:var(--dark); margin-top:4px;">0 Mahajon</div>
                    <div id="crm-stat-payables" style="font-size:12px; font-weight:700; color:var(--warning); margin-top:2px;">৳0 Payable</div>
                </div>

                <div style="background:var(--bg-body); padding:14px; border-radius:10px; border:1px solid var(--border-color);">
                    <div style="font-size:11px; font-weight:700; color:var(--text-muted);"><i class="fas fa-boxes-stacked" style="color:#10B981;"></i> PRODUCT VARIETIES</div>
                    <div id="crm-stat-products" style="font-size:18px; font-weight:800; color:var(--dark); margin-top:4px;">0 Varieties</div>
                    <div style="font-size:11.5px; color:var(--text-muted); margin-top:2px;">Inventory Items</div>
                </div>

                <div style="background:var(--bg-body); padding:14px; border-radius:10px; border:1px solid var(--border-color);">
                    <div style="font-size:11px; font-weight:700; color:var(--text-muted);"><i class="fas fa-cart-shopping" style="color:#10B981;"></i> TOTAL SALES</div>
                    <div id="crm-stat-sales" style="font-size:18px; font-weight:800; color:#10B981; margin-top:4px;">৳0</div>
                    <div style="font-size:11.5px; color:var(--text-muted); margin-top:2px;">Lifetime Sales Sum</div>
                </div>

                <div style="background:var(--bg-body); padding:14px; border-radius:10px; border:1px solid var(--border-color);">
                    <div style="font-size:11px; font-weight:700; color:var(--text-muted);"><i class="fas fa-truck-ramp-box" style="color:#0EA5E9;"></i> TOTAL PURCHASES</div>
                    <div id="crm-stat-purchases" style="font-size:18px; font-weight:800; color:#0EA5E9; margin-top:4px;">৳0</div>
                    <div style="font-size:11.5px; color:var(--text-muted); margin-top:2px;">Stock Purchases Sum</div>
                </div>

                <div style="background:var(--bg-body); padding:14px; border-radius:10px; border:1px solid var(--border-color);">
                    <div style="font-size:11px; font-weight:700; color:var(--text-muted);"><i class="fas fa-list-check" style="color:var(--dark);"></i> TRANSACTIONS</div>
                    <div id="crm-stat-tx" style="font-size:18px; font-weight:800; color:var(--dark); margin-top:4px;">0 Entries</div>
                    <div style="font-size:11.5px; color:var(--text-muted); margin-top:2px;">Ledger Records</div>
                </div>
            </div>

            <!-- Customer & Transaction Directories Grid -->
            <div style="display:grid; grid-template-columns:1fr 1fr; gap:16px; margin-top:20px;">
                <div>
                    <h4 style="font-size:14px; font-weight:800; margin-bottom:10px; color:var(--dark);"><i class="fas fa-address-book" style="color:var(--primary);"></i> Customers Directory (কাস্টমার তালিকা)</h4>
                    <div id="crm-customers-container" style="background:var(--bg-body); border:1px solid var(--border-color); border-radius:10px; padding:12px; font-size:13px; max-height:220px; overflow-y:auto;">
                        Loading customers...
                    </div>
                </div>

                <div>
                    <h4 style="font-size:14px; font-weight:800; margin-bottom:10px; color:var(--dark);"><i class="fas fa-receipt" style="color:#3B82F6;"></i> Recent Transactions (সাম্প্রতিক ট্রানজিশন)</h4>
                    <div id="crm-tx-container" style="background:var(--bg-body); border:1px solid var(--border-color); border-radius:10px; padding:12px; font-size:13px; max-height:220px; overflow-y:auto;">
                        Loading transactions...
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<!-- EDIT USER MODAL -->
<div id="edit-user-modal" style="display:none; position:fixed; inset:0; background:rgba(15,23,42,0.6); backdrop-filter:blur(4px); z-index:1000; align-items:center; justify-content:center; padding:20px;">
    <div style="background:white; border-radius:16px; width:100%; max-width:460px; padding:24px; box-shadow:0 25px 50px rgba(0,0,0,0.25);">
        <h3 style="font-size:18px; font-weight:800; margin-bottom:16px;">Edit Merchant Profile</h3>

        <form method="POST" action="{{ route('users.update') }}">
            @csrf
            <input type="hidden" name="id" id="edit-user-id">

            <div style="margin-bottom:14px;">
                <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">Merchant Name</label>
                <input type="text" name="name" id="edit-name" required class="btn btn-outline" style="width:100%; text-align:left;">
            </div>

            <div style="margin-bottom:14px;">
                <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">Phone Number</label>
                <input type="text" name="phone" id="edit-phone" required class="btn btn-outline" style="width:100%; text-align:left;">
            </div>

            <div style="margin-bottom:14px;">
                <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">Email Address</label>
                <input type="email" name="email" id="edit-email" class="btn btn-outline" style="width:100%; text-align:left;">
            </div>

            <div style="margin-bottom:20px;">
                <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">Store / Mill Name</label>
                <input type="text" name="storeName" id="edit-store" class="btn btn-outline" style="width:100%; text-align:left;">
            </div>

            <div style="display:flex; justify-content:flex-end; gap:10px;">
                <button type="button" class="btn btn-outline" onclick="closeEditModal()">Cancel</button>
                <button type="submit" class="btn btn-primary"><i class="fas fa-save"></i> Save Changes</button>
            </div>
        </form>
    </div>
</div>

<!-- DELETE CONFIRMATION MODAL -->
<div id="delete-modal" style="display:none; position:fixed; inset:0; background:rgba(15,23,42,0.6); backdrop-filter:blur(4px); z-index:1000; align-items:center; justify-content:center; padding:20px;">
    <div style="background:white; border-radius:16px; width:100%; max-width:460px; padding:24px; box-shadow:0 25px 50px rgba(0,0,0,0.25);">
        <h3 style="color:var(--danger); font-size:18px; font-weight:800; margin-bottom:12px;"><i class="fas fa-exclamation-triangle"></i> Permanent Account & Data Purge</h3>
        <p style="font-size:13px; color:var(--text-main); margin-bottom:14px;">
            You are about to permanently delete <strong id="del-name">Merchant</strong> (<code id="del-phone">Phone</code>).
        </p>

        <div style="background:rgba(239,68,68,0.08); border-left:4px solid var(--danger); padding:12px; border-radius:8px; font-size:12px; color:var(--dark); margin-bottom:16px; line-height:1.5;">
            <strong>This will permanently purge:</strong> User Profile, Customers & Dues, Mahajon & Payables, Ledger Transactions, Products & Stock, Payment Requests, SMS History, Wallet Accounts.
        </div>

        <form method="POST" action="{{ route('users.delete') }}">
            @csrf
            <input type="hidden" name="id" id="del-user-id">
            <div style="margin-bottom:16px;">
                <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">Type <code style="color:var(--danger); font-weight:800;">DELETE</code> to confirm:</label>
                <input type="text" name="confirm_text" placeholder="DELETE" required class="btn btn-outline" style="width:100%; text-transform:uppercase; text-align:left;">
            </div>

            <div style="display:flex; justify-content:flex-end; gap:10px;">
                <button type="button" class="btn btn-outline" onclick="closeDeleteModal()">Cancel</button>
                <button type="submit" class="btn btn-danger"><i class="fas fa-trash-alt"></i> Delete Account & All Data</button>
            </div>
        </form>
    </div>
</div>
@endsection

@section('scripts')
<script>
const crmBaseUrl = "{{ url('/users') }}";

function openCrmModal(userId) {
    document.getElementById('crm-modal').style.display = 'flex';
    document.getElementById('crm-user-name').textContent = 'Merchant CRM Profile';
    document.getElementById('crm-user-store').textContent = 'Loading business insights from Firebase...';
    document.getElementById('crm-customers-container').innerHTML = '<div style="color:var(--text-muted); font-size:12px;">Loading customers...</div>';
    document.getElementById('crm-tx-container').innerHTML = '<div style="color:var(--text-muted); font-size:12px;">Loading transactions...</div>';

    fetch(crmBaseUrl + '/' + userId + '/crm')
        .then(res => {
            if (!res.ok) throw new Error('HTTP status ' + res.status);
            return res.json();
        })
        .then(data => {
            if (data.error) {
                alert(data.error);
                return;
            }
            document.getElementById('crm-user-name').textContent = data.user.name || 'Merchant Profile';
            document.getElementById('crm-user-store').textContent = (data.user.storeName || data.user.shopName || 'Rice Store') + ' • Phone: ' + (data.user.phone || '-') + ' • Email: ' + (data.user.email || '-');

            document.getElementById('crm-stat-customers').textContent = data.stats.totalCustomers + ' Customers';
            document.getElementById('crm-stat-dues').textContent = '৳' + Number(data.stats.totalCustomerDues || 0).toLocaleString() + ' Due';

            document.getElementById('crm-stat-suppliers').textContent = data.stats.totalSuppliers + ' Mahajon';
            document.getElementById('crm-stat-payables').textContent = '৳' + Number(data.stats.totalSupplierPayables || 0).toLocaleString() + ' Payable';

            document.getElementById('crm-stat-products').textContent = data.stats.totalProducts + ' Varieties';
            document.getElementById('crm-stat-sales').textContent = '৳' + Number(data.stats.totalSalesSum || 0).toLocaleString();
            document.getElementById('crm-stat-purchases').textContent = '৳' + Number(data.stats.totalPurchasesSum || 0).toLocaleString();
            document.getElementById('crm-stat-tx').textContent = data.stats.totalTransactions + ' Entries';

            // Customers List
            let custList = (!data.customers || data.customers.length === 0) 
                ? '<div style="color:var(--text-muted); font-size:12px;">No customers added yet.</div>' 
                : data.customers.map(c => `
                    <div style="display:flex; justify-content:space-between; align-items:center; padding:8px 0; border-bottom:1px dashed var(--border-color);">
                        <div>
                            <strong>${c.name || 'Customer'}</strong> 
                            <span style="font-size:11px; color:var(--text-muted);">(${c.phone || '-'})</span>
                        </div>
                        <span style="font-weight:800; font-size:12.5px; color:${(c.baki || c.dueAmount || 0) > 0 ? 'var(--danger)' : 'var(--success)'};">
                            ৳${Number(c.baki || c.dueAmount || 0).toLocaleString()}
                        </span>
                    </div>
                `).join('');
            document.getElementById('crm-customers-container').innerHTML = custList;

            // Transactions List
            let txList = (!data.transactions || data.transactions.length === 0) 
                ? '<div style="color:var(--text-muted); font-size:12px;">No transactions recorded yet.</div>' 
                : data.transactions.map(t => {
                    const amt = Number(t.amount || 0).toLocaleString();
                    const typeUpper = (t.type || 'PAYMENT').toUpperCase();
                    return `
                        <div style="display:flex; justify-content:space-between; align-items:center; padding:6px 0; border-bottom:1px dashed var(--border-color); font-size:12px;">
                            <div>
                                <span class="badge ${typeUpper === 'PAYMENT' ? 'badge-success' : 'badge-warning'}" style="font-size:9px;">${typeUpper}</span>
                                <strong>${t.customerName || 'Ledger Entry'}</strong>
                                <div style="font-size:10.5px; color:var(--text-muted);">${t.note || ''}</div>
                            </div>
                            <strong style="font-size:13px; color:var(--dark);">৳${amt}</strong>
                        </div>
                    `;
                }).join('');
            document.getElementById('crm-tx-container').innerHTML = txList;
        })
        .catch(err => {
            console.error('CRM fetch error:', err);
            document.getElementById('crm-user-store').textContent = 'Failed to load merchant CRM insights.';
            document.getElementById('crm-customers-container').innerHTML = '<div style="color:var(--danger); font-size:12px;">Error fetching merchant details.</div>';
            document.getElementById('crm-tx-container').innerHTML = '<div style="color:var(--danger); font-size:12px;">Error fetching transactions.</div>';
        });
}

function closeCrmModal() {
    document.getElementById('crm-modal').style.display = 'none';
}

function openEditModal(userId, name, phone, email, store) {
    document.getElementById('edit-user-id').value = userId;
    document.getElementById('edit-name').value = name;
    document.getElementById('edit-phone').value = phone;
    document.getElementById('edit-email').value = email;
    document.getElementById('edit-store').value = store;
    document.getElementById('edit-user-modal').style.display = 'flex';
}

function closeEditModal() {
    document.getElementById('edit-user-modal').style.display = 'none';
}

function openDeleteModal(userId, name, phone) {
    document.getElementById('del-user-id').value = userId;
    document.getElementById('del-name').textContent = name;
    document.getElementById('del-phone').textContent = phone;
    document.getElementById('delete-modal').style.display = 'flex';
}

function closeDeleteModal() {
    document.getElementById('delete-modal').style.display = 'none';
}
</script>
@endsection
