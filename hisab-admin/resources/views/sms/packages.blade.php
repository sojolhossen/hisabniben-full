@extends('layouts.app')

@section('title', 'SMS Bundles Control')

@section('content')
<div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:24px;">
    <div>
        <h1 style="font-size:24px; font-weight:800; color:var(--dark);">SMS Credit Bundles Control</h1>
        <p style="font-size:13.5px; color:var(--text-muted); margin-top:2px;">Set pricing and SMS credit pack amounts for merchant in-app top-up</p>
    </div>

    <button class="btn btn-primary" onclick="openSmsPkgModal()"><i class="fas fa-plus"></i> Create SMS Bundle</button>
</div>

<div style="display:grid; grid-template-columns:repeat(auto-fit, minmax(260px, 1fr)); gap:20px;">
    @forelse($packages as $sp)
        <div class="card-panel" style="margin-bottom:0;">
            <h3 style="font-size:18px; font-weight:800; color:var(--dark);">{{ $sp['name'] ?? 'SMS Pack' }}</h3>
            <div style="font-size:32px; font-weight:800; color:#3B82F6; margin:10px 0;">
                {{ number_format($sp['smsCount'] ?? 0) }} <span style="font-size:14px; color:var(--text-muted);">SMS</span>
            </div>

            <div style="font-size:16px; font-weight:800; color:var(--primary); margin-bottom:16px;">
                Price: ৳{{ number_format($sp['price'] ?? 0, 2) }}
            </div>

            @if(!empty($sp['smsCount']) && $sp['smsCount'] > 0)
                <div style="font-size:12px; color:var(--text-muted); margin-bottom:16px;">
                    Unit Cost: ৳{{ number_format(($sp['price'] ?? 0) / $sp['smsCount'], 3) }} / SMS
                </div>
            @endif

            <button class="btn btn-outline" style="width:100%; justify-content:center;" onclick="editSmsPkg('{{ $sp['id'] }}', '{{ addslashes($sp['name'] ?? '') }}', {{ $sp['price'] ?? 0 }}, {{ $sp['smsCount'] ?? 0 }})">
                <i class="fas fa-edit"></i> Edit SMS Bundle
            </button>
        </div>
    @empty
        <div class="card-panel" style="grid-column:1 / -1; text-align:center; padding:40px; color:var(--text-muted);">
            No SMS bundles configured. Click <strong>Create SMS Bundle</strong> above.
        </div>
    @endforelse
</div>

<!-- MODAL -->
<div id="sms-pkg-modal" style="display:none; position:fixed; inset:0; background:rgba(15,23,42,0.6); backdrop-filter:blur(4px); z-index:1000; align-items:center; justify-content:center; padding:20px;">
    <div style="background:white; border-radius:16px; width:100%; max-width:440px; padding:24px; box-shadow:0 25px 50px rgba(0,0,0,0.25);">
        <h3 id="sms-pkg-modal-title" style="font-size:18px; font-weight:800; margin-bottom:16px;">Create SMS Bundle</h3>

        <form method="POST" action="{{ route('sms.packages.save') }}">
            @csrf
            <input type="hidden" name="id" id="sp-id">

            <div style="margin-bottom:14px;">
                <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">Bundle Name</label>
                <input type="text" name="name" id="sp-name" required placeholder="e.g. 500 SMS Pack" class="btn btn-outline" style="width:100%; text-align:left;">
            </div>

            <div style="display:grid; grid-template-columns:1fr 1fr; gap:12px; margin-bottom:20px;">
                <div>
                    <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">Price (৳)</label>
                    <input type="number" step="0.01" name="price" id="sp-price" required placeholder="250" class="btn btn-outline" style="width:100%; text-align:left;">
                </div>
                <div>
                    <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">SMS Quantity</label>
                    <input type="number" name="smsCount" id="sp-count" required placeholder="500" class="btn btn-outline" style="width:100%; text-align:left;">
                </div>
            </div>

            <div style="display:flex; justify-content:flex-end; gap:10px;">
                <button type="button" class="btn btn-outline" onclick="closeSmsPkgModal()">Cancel</button>
                <button type="submit" class="btn btn-primary"><i class="fas fa-save"></i> Save Bundle</button>
            </div>
        </form>
    </div>
</div>
@endsection

@section('scripts')
<script>
function openSmsPkgModal() {
    document.getElementById('sms-pkg-modal-title').textContent = 'Create SMS Bundle';
    document.getElementById('sp-id').value = '';
    document.getElementById('sp-name').value = '';
    document.getElementById('sp-price').value = '';
    document.getElementById('sp-count').value = '';
    document.getElementById('sms-pkg-modal').style.display = 'flex';
}

function editSmsPkg(id, name, price, count) {
    document.getElementById('sms-pkg-modal-title').textContent = 'Edit SMS Bundle';
    document.getElementById('sp-id').value = id;
    document.getElementById('sp-name').value = name;
    document.getElementById('sp-price').value = price;
    document.getElementById('sp-count').value = count;
    document.getElementById('sms-pkg-modal').style.display = 'flex';
}

function closeSmsPkgModal() {
    document.getElementById('sms-pkg-modal').style.display = 'none';
}
</script>
@endsection
