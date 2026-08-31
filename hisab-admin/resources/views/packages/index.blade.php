@extends('layouts.app')

@section('title', 'Subscription Packages')

@section('content')
<div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:24px;">
    <div>
        <h1 style="font-size:24px; font-weight:800; color:var(--dark);">Subscription Packages Control</h1>
        <p style="font-size:13.5px; color:var(--text-muted); margin-top:2px;">Create, update pricing, Google Play Product SKUs, features & lifetime plans for Android mobile app</p>
    </div>

    <button class="btn btn-primary" onclick="openPackageModal()"><i class="fas fa-plus"></i> Create New Package</button>
</div>

<!-- PACKAGES GRID -->
<div style="display:grid; grid-template-columns:repeat(auto-fit, minmax(320px, 1fr)); gap:20px;">
    @forelse($packages as $pkg)
        <div class="card-panel" style="position:relative; border-top:4px solid {{ !empty($pkg['isPopular']) ? 'var(--warning)' : 'var(--primary)' }}; margin-bottom:0;">
            @if(!empty($pkg['badgeTag']) || !empty($pkg['isPopular']))
                <span class="badge badge-warning" style="position:absolute; top:12px; right:12px; font-weight:800;">
                    ★ {{ strtoupper($pkg['badgeTag'] ?: 'MOST POPULAR') }}
                </span>
            @endif

            <div style="display:flex; align-items:center; gap:8px; margin-bottom:8px;">
                <h3 style="font-size:18px; font-weight:800; color:var(--dark); margin:0;">{{ $pkg['name'] ?? 'Subscription Package' }}</h3>
                <span class="badge {{ $pkg['isActive'] ? 'badge-success' : 'badge-danger' }}" style="font-size:10px;">
                    {{ strtoupper($pkg['status'] ?? 'ACTIVE') }}
                </span>
            </div>

            <div style="display:flex; align-items:baseline; gap:10px; margin:10px 0;">
                <div style="font-size:32px; font-weight:800; color:var(--primary);">
                    ৳{{ number_format($pkg['price'] ?? 0, 0) }}
                </div>
                @if(($pkg['originalPrice'] ?? 0) > ($pkg['price'] ?? 0))
                    <div style="font-size:16px; color:var(--text-muted); text-decoration:line-through;">
                        ৳{{ number_format($pkg['originalPrice'], 0) }}
                    </div>
                @endif
            </div>

            <div style="font-size:13px; color:var(--text-muted); margin-bottom:12px; display:flex; flex-wrap:wrap; gap:12px;">
                <span><i class="fas fa-calendar-alt" style="color:var(--primary);"></i> <strong>{{ ($pkg['durationDays'] ?? 30) >= 9999 ? 'Life-Time Validity' : ($pkg['durationDays'] ?? 30).' Days' }}</strong></span>
                @if(!empty($pkg['smsCount']))
                    <span><i class="fas fa-comment-sms" style="color:var(--info);"></i> <strong>+{{ $pkg['smsCount'] }} Bonus SMS</strong></span>
                @endif
            </div>

            @if(!empty($pkg['playStoreProductId']))
                <div style="font-size:11px; font-weight:700; color:var(--text-muted); background:#F1F5F9; padding:4px 8px; border-radius:6px; margin-bottom:12px; display:inline-block;">
                    <i class="fab fa-google-play"></i> SKU: <code>{{ $pkg['playStoreProductId'] }}</code>
                </div>
            @endif

            @if(!empty($pkg['description']))
                <p style="font-size:12.5px; color:var(--text-main); line-height:1.4; margin-bottom:12px;">
                    {{ $pkg['description'] }}
                </p>
            @endif

            @if(!empty($pkg['features']))
                <div style="margin-bottom:20px; border-top:1px dashed var(--border-color); pt:10px; padding-top:10px;">
                    <span style="font-size:11px; font-weight:800; text-transform:uppercase; color:var(--text-muted); display:block; margin-bottom:6px;">Package Features:</span>
                    <ul style="list-style:none; padding:0; margin:0; font-size:12px; color:var(--dark);">
                        @foreach($pkg['features'] as $f)
                            <li style="margin-bottom:4px; display:flex; align-items:center; gap:6px;">
                                <i class="fas fa-check-circle" style="color:var(--success); font-size:11px;"></i> {{ $f }}
                            </li>
                        @endforeach
                    </ul>
                </div>
            @endif

            <div style="display:flex; gap:10px; border-top:1px solid var(--border-color); padding-top:14px; margin-top:auto;">
                <button type="button" class="btn btn-outline" style="flex:1; justify-content:center;" data-pkg="{{ json_encode($pkg) }}" onclick="editPackageData(this)">
                    <i class="fas fa-edit"></i> Edit
                </button>

                <form method="POST" action="{{ route('packages.delete') }}" style="display:inline;">
                    @csrf
                    <input type="hidden" name="id" value="{{ $pkg['id'] }}">
                    <button type="submit" class="btn btn-danger" onclick="return confirm('Delete this package?')"><i class="fas fa-trash-alt"></i></button>
                </form>
            </div>
        </div>
    @empty
        <div class="card-panel" style="grid-column:1 / -1; text-align:center; padding:40px; color:var(--text-muted);">
            No subscription packages configured. Click <strong>Create New Package</strong> above to add one.
        </div>
    @endforelse
</div>

<!-- ADD / EDIT PACKAGE MODAL -->
<div id="pkg-modal" style="display:none; position:fixed; inset:0; background:rgba(15,23,42,0.6); backdrop-filter:blur(4px); z-index:1000; align-items:center; justify-content:center; padding:20px; overflow-y:auto;">
    <div style="background:white; border-radius:16px; width:100%; max-width:560px; padding:24px; box-shadow:0 25px 50px rgba(0,0,0,0.25); max-height:90vh; overflow-y:auto;">
        <h3 id="pkg-modal-title" style="font-size:18px; font-weight:800; margin-bottom:16px;">Create New Package</h3>

        <form method="POST" action="{{ route('packages.save') }}">
            @csrf
            <input type="hidden" name="id" id="pkg-id">

            <div style="margin-bottom:14px;">
                <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">Package Name</label>
                <input type="text" name="name" id="pkg-name" required placeholder="e.g. Yearly Premium Plan" class="btn btn-outline" style="width:100%; text-align:left;">
            </div>

            <div style="display:grid; grid-template-columns:1fr 1fr; gap:12px; margin-bottom:14px;">
                <div>
                    <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">Discounted Price (৳)</label>
                    <input type="number" step="0.01" name="price" id="pkg-price" required placeholder="299" class="btn btn-outline" style="width:100%; text-align:left;">
                </div>
                <div>
                    <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">Original Regular Price (৳)</label>
                    <input type="number" step="0.01" name="originalPrice" id="pkg-original-price" placeholder="500 (Optional strike-through)" class="btn btn-outline" style="width:100%; text-align:left;">
                </div>
            </div>

            <div style="display:grid; grid-template-columns:1fr 1fr; gap:12px; margin-bottom:14px;">
                <div>
                    <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">Duration (Days)</label>
                    <input type="number" name="durationDays" id="pkg-duration" required placeholder="365 (or 99999 for Lifetime)" class="btn btn-outline" style="width:100%; text-align:left;">
                </div>
                <div>
                    <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">Status</label>
                    <select name="status" id="pkg-status" class="btn btn-outline" style="width:100%;">
                        <option value="active">Active (Visible in App)</option>
                        <option value="inactive">Inactive (Hidden)</option>
                    </select>
                </div>
            </div>

            <div style="display:grid; grid-template-columns:1fr 1fr; gap:12px; margin-bottom:14px;">
                <div>
                    <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">Google Play SKU / Product ID</label>
                    <input type="text" name="playStoreProductId" id="pkg-play-sku" placeholder="e.g. hisab_premium_1year" class="btn btn-outline" style="width:100%; text-align:left;">
                </div>
                <div>
                    <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">Bonus Free SMS Count</label>
                    <input type="number" name="smsCount" id="pkg-sms-count" placeholder="0" class="btn btn-outline" style="width:100%; text-align:left;">
                </div>
            </div>

            <div style="display:grid; grid-template-columns:1fr 1fr; gap:12px; margin-bottom:14px; align-items:center;">
                <div>
                    <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">Custom Badge Tag Text</label>
                    <input type="text" name="badgeTag" id="pkg-badge-tag" placeholder="e.g. MOST POPULAR, SAVE 40%" class="btn btn-outline" style="width:100%; text-align:left;">
                </div>
                <div style="padding-top:20px;">
                    <label style="font-size:13px; font-weight:700; display:flex; align-items:center; gap:8px; cursor:pointer;">
                        <input type="checkbox" name="isPopular" id="pkg-popular" style="width:16px; height:16px;">
                        Mark as Popular Package
                    </label>
                </div>
            </div>

            <div style="margin-bottom:14px;">
                <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">Short Description</label>
                <textarea name="description" id="pkg-desc" rows="2" class="btn btn-outline" style="width:100%; text-align:left; resize:none;" placeholder="Package subtitle / summary..."></textarea>
            </div>

            <div style="margin-bottom:20px;">
                <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">Package Features Bullet Points (1 per line)</label>
                <textarea name="features" id="pkg-features" rows="5" class="btn btn-outline" style="width:100%; text-align:left; font-family:monospace; font-size:12.5px; resize:vertical;" placeholder="Unlimited Customers&#10;Unlimited Transactions&#10;Reports & Analytics&#10;Cloud Backup&#10;Ad Free"></textarea>
            </div>

            <div style="display:flex; justify-content:flex-end; gap:10px;">
                <button type="button" class="btn btn-outline" onclick="closePackageModal()">Cancel</button>
                <button type="submit" class="btn btn-primary"><i class="fas fa-save"></i> Save Package</button>
            </div>
        </form>
    </div>
</div>
@endsection

@section('scripts')
<script>
function openPackageModal() {
    document.getElementById('pkg-modal-title').textContent = 'Create New Package';
    document.getElementById('pkg-id').value = '';
    document.getElementById('pkg-name').value = '';
    document.getElementById('pkg-price').value = '';
    document.getElementById('pkg-original-price').value = '';
    document.getElementById('pkg-duration').value = '30';
    document.getElementById('pkg-status').value = 'active';
    document.getElementById('pkg-play-sku').value = '';
    document.getElementById('pkg-sms-count').value = '0';
    document.getElementById('pkg-badge-tag').value = '';
    document.getElementById('pkg-popular').checked = false;
    document.getElementById('pkg-desc').value = '';
    document.getElementById('pkg-features').value = "Unlimited Customers\nUnlimited Transactions\nReports & Analytics\nPriority Support\nCloud Backup\nExport Data\nMultiple Devices\nAd Free";
    document.getElementById('pkg-modal').style.display = 'flex';
}

function editPackageData(btn) {
    try {
        const pkg = JSON.parse(btn.getAttribute('data-pkg'));
        document.getElementById('pkg-modal-title').textContent = 'Edit Package';
        document.getElementById('pkg-id').value = pkg.id || '';
        document.getElementById('pkg-name').value = pkg.name || '';
        document.getElementById('pkg-price').value = pkg.price || 0;
        document.getElementById('pkg-original-price').value = pkg.originalPrice || pkg.price || '';
        document.getElementById('pkg-duration').value = pkg.durationDays || 30;
        document.getElementById('pkg-status').value = pkg.status || 'active';
        document.getElementById('pkg-play-sku').value = pkg.playStoreProductId || '';
        document.getElementById('pkg-sms-count').value = pkg.smsCount || 0;
        document.getElementById('pkg-badge-tag').value = pkg.badgeTag || '';
        document.getElementById('pkg-popular').checked = !!pkg.isPopular;
        document.getElementById('pkg-desc').value = pkg.description || '';
        
        let feats = pkg.featuresText;
        if (!feats && Array.isArray(pkg.features)) {
            feats = pkg.features.join('\n');
        }
        document.getElementById('pkg-features').value = feats || '';
        document.getElementById('pkg-modal').style.display = 'flex';
    } catch(e) {
        console.error('Error opening package modal:', e);
    }
}

function closePackageModal() {
    document.getElementById('pkg-modal').style.display = 'none';
}
</script>
@endsection
