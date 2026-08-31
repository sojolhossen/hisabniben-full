@extends('layouts.app')

@section('title', 'Payment Methods')

@section('content')
<div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:24px;">
    <div>
        <h1 style="font-size:24px; font-weight:800; color:var(--dark);">Payment Methods & Accounts</h1>
        <p style="font-size:13.5px; color:var(--text-muted); margin-top:2px;">Configure bKash, Nagad, Rocket & Bank accounts synced with Firebase & Android mobile app</p>
    </div>

    <button class="btn btn-primary" onclick="openMethodModal()"><i class="fas fa-plus"></i> Add Payment Method</button>
</div>

<div style="display:grid; grid-template-columns:repeat(auto-fit, minmax(280px, 1fr)); gap:20px;">
    @forelse($methods as $m)
        <div class="card-panel" style="position:relative; margin-bottom:0;">
            <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:12px;">
                <h3 style="font-size:18px; font-weight:800; color:var(--dark);">{{ $m['name'] ?? 'Payment Method' }}</h3>
                <span class="badge {{ !empty($m['isActive']) ? 'badge-success' : 'badge-danger' }}">
                    {{ !empty($m['isActive']) ? 'ACTIVE' : 'INACTIVE' }}
                </span>
            </div>

            <div style="font-size:18px; font-weight:800; color:var(--primary); margin-bottom:8px;">
                <code>{{ $m['number'] ?? $m['accountNumber'] ?? '-' }}</code>
            </div>

            <div style="font-size:12px; font-weight:700; color:var(--text-muted); margin-bottom:12px;">
                TYPE: {{ strtoupper($m['type'] ?? $m['accountType'] ?? 'PERSONAL') }}
            </div>

            <p style="font-size:12.5px; color:var(--text-main); line-height:1.5; margin-bottom:20px;">
                {{ $m['instructions'] ?? 'Send Money to this number and submit your TrxID.' }}
            </p>

            <div style="display:flex; gap:10px; border-top:1px solid var(--border-color); padding-top:14px;">
                <button type="button" class="btn btn-outline" style="flex:1; justify-content:center;" data-method="{{ json_encode($m) }}" onclick="editMethodData(this)">
                    <i class="fas fa-edit"></i> Edit
                </button>

                <form method="POST" action="{{ route('payment-methods.toggle') }}" style="display:inline;">
                    @csrf
                    <input type="hidden" name="id" value="{{ $m['id'] }}">
                    <button type="submit" class="btn btn-outline" style="color:var(--warning);"><i class="fas fa-power-off"></i> Toggle</button>
                </form>
            </div>
        </div>
    @empty
        <div class="card-panel" style="grid-column:1 / -1; text-align:center; padding:40px; color:var(--text-muted);">
            No payment methods configured in Firebase. Click <strong>Add Payment Method</strong> to configure bKash / Nagad.
        </div>
    @endforelse
</div>

<!-- MODAL -->
<div id="method-modal" style="display:none; position:fixed; inset:0; background:rgba(15,23,42,0.6); backdrop-filter:blur(4px); z-index:1000; align-items:center; justify-content:center; padding:20px;">
    <div style="background:white; border-radius:16px; width:100%; max-width:460px; padding:24px; box-shadow:0 25px 50px rgba(0,0,0,0.25);">
        <h3 id="method-modal-title" style="font-size:18px; font-weight:800; margin-bottom:16px;">Add Payment Method</h3>

        <form method="POST" action="{{ route('payment-methods.save') }}">
            @csrf
            <input type="hidden" name="id" id="m-id">

            <div style="margin-bottom:14px;">
                <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">Method Name</label>
                <input type="text" name="name" id="m-name" required placeholder="e.g. bKash Personal" class="btn btn-outline" style="width:100%; text-align:left;">
            </div>

            <div style="display:grid; grid-template-columns:1fr 1fr; gap:12px; margin-bottom:14px;">
                <div>
                    <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">Account Type</label>
                    <select name="type" id="m-type" class="btn btn-outline" style="width:100%;">
                        <option value="PERSONAL">Personal</option>
                        <option value="AGENT">Agent</option>
                        <option value="MERCHANT">Merchant</option>
                    </select>
                </div>
                <div>
                    <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">Account Number</label>
                    <input type="text" name="number" id="m-number" required placeholder="01700000000" class="btn btn-outline" style="width:100%; text-align:left;">
                </div>
            </div>

            <div style="margin-bottom:14px;">
                <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">Instructions for User</label>
                <textarea name="instructions" id="m-instructions" rows="3" class="btn btn-outline" style="width:100%; text-align:left; resize:none;" placeholder="Payment instructions..."></textarea>
            </div>

            <div style="margin-bottom:20px; display:flex; align-items:center; gap:8px;">
                <input type="checkbox" name="isActive" id="m-active" style="width:16px; height:16px;" checked>
                <label for="m-active" style="font-size:13px; font-weight:700;">Active & Visible in App</label>
            </div>

            <div style="display:flex; justify-content:flex-end; gap:10px;">
                <button type="button" class="btn btn-outline" onclick="closeMethodModal()">Cancel</button>
                <button type="submit" class="btn btn-primary"><i class="fas fa-save"></i> Save Method</button>
            </div>
        </form>
    </div>
</div>
@endsection

@section('scripts')
<script>
function openMethodModal() {
    document.getElementById('method-modal-title').textContent = 'Add Payment Method';
    document.getElementById('m-id').value = '';
    document.getElementById('m-name').value = '';
    document.getElementById('m-type').value = 'PERSONAL';
    document.getElementById('m-number').value = '';
    document.getElementById('m-instructions').value = '';
    document.getElementById('m-active').checked = true;
    document.getElementById('method-modal').style.display = 'flex';
}

function editMethodData(btn) {
    try {
        const m = JSON.parse(btn.getAttribute('data-method'));
        document.getElementById('method-modal-title').textContent = 'Edit Payment Method';
        document.getElementById('m-id').value = m.id || '';
        document.getElementById('m-name').value = m.name || '';
        document.getElementById('m-type').value = (m.type || m.accountType || 'PERSONAL').toUpperCase();
        document.getElementById('m-number').value = m.number || m.accountNumber || '';
        document.getElementById('m-instructions').value = m.instructions || '';
        document.getElementById('m-active').checked = !!m.isActive;
        document.getElementById('method-modal').style.display = 'flex';
    } catch(e) {
        console.error('Error opening method modal:', e);
    }
}

function closeMethodModal() {
    document.getElementById('method-modal').style.display = 'none';
}
</script>
@endsection
