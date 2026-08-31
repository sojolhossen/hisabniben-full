@extends('layouts.app')

@section('title', 'Video Tutorials Publisher')

@section('content')
<div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:24px;">
    <div>
        <h1 style="font-size:24px; font-weight:800; color:var(--dark);">App Video Tutorials Publisher</h1>
        <p style="font-size:13.5px; color:var(--text-muted); margin-top:2px;">Publish video guides synced with the Android merchant dashboard video tutorial banner</p>
    </div>

    <button class="btn btn-primary" onclick="openTutModal()"><i class="fas fa-plus"></i> Publish New Tutorial</button>
</div>

<div style="display:grid; grid-template-columns:repeat(auto-fit, minmax(280px, 1fr)); gap:20px;">
    @forelse($tutorials as $t)
        <div class="card-panel" style="margin-bottom:0;">
            <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:10px;">
                <h3 style="font-size:16px; font-weight:800; color:var(--dark);">{{ $t['title'] ?? 'Tutorial' }}</h3>
                <span class="badge {{ !empty($t['isPublished']) ? 'badge-success' : 'badge-warning' }}">
                    {{ !empty($t['isPublished']) ? 'PUBLISHED' : 'DRAFT' }}
                </span>
            </div>

            <div style="font-size:12px; color:var(--text-muted); margin-bottom:14px;">
                <code>{{ $t['youtubeUrl'] ?? 'YouTube Link' }}</code>
            </div>

            <p style="font-size:12.5px; color:var(--text-main); line-height:1.5; margin-bottom:16px;">
                {{ $t['description'] ?? 'Learn how to manage customer dues and rice inventory.' }}
            </p>

            <button class="btn btn-outline" style="width:100%; justify-content:center;" onclick="editTut('{{ $t['id'] }}', '{{ addslashes($t['title'] ?? '') }}', '{{ addslashes($t['youtubeUrl'] ?? '') }}', '{{ addslashes($t['description'] ?? '') }}', {{ !empty($t['isPublished']) ? 'true' : 'false' }})">
                <i class="fas fa-edit"></i> Edit Tutorial
            </button>
        </div>
    @empty
        <div class="card-panel" style="grid-column:1 / -1; text-align:center; padding:40px; color:var(--text-muted);">
            No video tutorials published yet. Click <strong>Publish New Tutorial</strong> above.
        </div>
    @endforelse
</div>

<!-- MODAL -->
<div id="tut-modal" style="display:none; position:fixed; inset:0; background:rgba(15,23,42,0.6); backdrop-filter:blur(4px); z-index:1000; align-items:center; justify-content:center; padding:20px;">
    <div style="background:white; border-radius:16px; width:100%; max-width:460px; padding:24px; box-shadow:0 25px 50px rgba(0,0,0,0.25);">
        <h3 id="tut-modal-title" style="font-size:18px; font-weight:800; margin-bottom:16px;">Publish Video Tutorial</h3>

        <form method="POST" action="{{ route('tutorials.save') }}">
            @csrf
            <input type="hidden" name="id" id="t-id">

            <div style="margin-bottom:14px;">
                <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">Tutorial Title</label>
                <input type="text" name="title" id="t-title" required placeholder="e.g. কীভাবে সহজে বাকি খাতা হিসাব করবেন?" class="btn btn-outline" style="width:100%; text-align:left;">
            </div>

            <div style="margin-bottom:14px;">
                <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">YouTube Video Link / URL</label>
                <input type="url" name="youtubeUrl" id="t-url" required placeholder="https://www.youtube.com/watch?v=..." class="btn btn-outline" style="width:100%; text-align:left;">
            </div>

            <div style="margin-bottom:14px;">
                <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">Description</label>
                <textarea name="description" id="t-desc" rows="3" class="btn btn-outline" style="width:100%; text-align:left; resize:none;" placeholder="Short tutorial description..."></textarea>
            </div>

            <div style="margin-bottom:20px; display:flex; align-items:center; gap:8px;">
                <input type="checkbox" name="isPublished" id="t-published" style="width:16px; height:16px;" checked>
                <label for="t-published" style="font-size:13px; font-weight:700;">Publish Live to Android Merchant App</label>
            </div>

            <div style="display:flex; justify-content:flex-end; gap:10px;">
                <button type="button" class="btn btn-outline" onclick="closeTutModal()">Cancel</button>
                <button type="submit" class="btn btn-primary"><i class="fas fa-save"></i> Save & Publish</button>
            </div>
        </form>
    </div>
</div>
@endsection

@section('scripts')
<script>
function openTutModal() {
    document.getElementById('tut-modal-title').textContent = 'Publish Video Tutorial';
    document.getElementById('t-id').value = '';
    document.getElementById('t-title').value = '';
    document.getElementById('t-url').value = '';
    document.getElementById('t-desc').value = '';
    document.getElementById('t-published').checked = true;
    document.getElementById('tut-modal').style.display = 'flex';
}

function editTut(id, title, url, desc, isPublished) {
    document.getElementById('tut-modal-title').textContent = 'Edit Tutorial';
    document.getElementById('t-id').value = id;
    document.getElementById('t-title').value = title;
    document.getElementById('t-url').value = url;
    document.getElementById('t-desc').value = desc;
    document.getElementById('t-published').checked = isPublished;
    document.getElementById('tut-modal').style.display = 'flex';
}

function closeTutModal() {
    document.getElementById('tut-modal').style.display = 'none';
}
</script>
@endsection
