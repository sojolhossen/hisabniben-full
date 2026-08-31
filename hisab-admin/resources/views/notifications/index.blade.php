@extends('layouts.app')

@section('title', 'Push Notifications')

@section('content')
<div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:24px;">
    <div>
        <h1 style="font-size:24px; font-weight:800; color:var(--dark);">Push Notifications Command Center</h1>
        <p style="font-size:13.5px; color:var(--text-muted); margin-top:2px;">Broadcast push campaigns to all Android merchant devices via OneSignal API</p>
    </div>
</div>

<div style="display:grid; grid-template-columns: 1fr 1fr; gap:24px;">
    <!-- CAMPAIGN FORM -->
    <div class="card-panel">
        <h3 style="font-size:16px; font-weight:800; margin-bottom:16px;"><i class="fas fa-paper-plane" style="color:var(--primary);"></i> Dispatch New Notification</h3>

        <form method="POST" action="{{ route('notifications.send') }}">
            @csrf
            <div style="margin-bottom:14px;">
                <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">Target Audience</label>
                <select name="targetAudience" class="btn btn-outline" style="width:100%;">
                    <option value="ALL">All Active Merchants (সকল ব্যবহারকারী)</option>
                    <option value="FREE">Free & Trial Merchants Only</option>
                    <option value="PREMIUM">Premium Plan Subscribers Only</option>
                </select>
            </div>

            <div style="margin-bottom:14px;">
                <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">Notification Title</label>
                <input type="text" name="title" required placeholder="e.g. 🌾 স্পেশাল ডিল ও বাকি হিসাব আপডেট!" class="btn btn-outline" style="width:100%; text-align:left;">
            </div>

            <div style="margin-bottom:18px;">
                <label style="font-size:12px; font-weight:700; display:block; margin-bottom:6px;">Message Body</label>
                <textarea name="message" rows="4" required placeholder="আজই আপনার চালের দোকানের বাকি খাতা আপডেট করুন এবং SMS অ্যালার্ট পাঠাল..." class="btn btn-outline" style="width:100%; text-align:left; resize:none;"></textarea>
            </div>

            <button type="submit" class="btn btn-primary" style="width:100%; justify-content:center; padding:12px;"><i class="fas fa-paper-plane"></i> Send Push Notification Now</button>
        </form>
    </div>

    <!-- RECENT CAMPAIGNS LOG -->
    <div class="card-panel">
        <h3 style="font-size:16px; font-weight:800; margin-bottom:16px;"><i class="fas fa-clock-rotate-left" style="color:var(--info);"></i> Campaign Broadcast History</h3>

        <div class="table-responsive">
            <table class="data-table">
                <thead>
                    <tr>
                        <th>Title & Message</th>
                        <th>Target</th>
                        <th>Sent Date</th>
                        <th style="text-align:right;">Action</th>
                    </tr>
                </thead>
                <tbody>
                    @forelse($notifications as $n)
                        <tr>
                            <td>
                                <strong>{{ $n['title'] ?? 'Notification' }}</strong>
                                <div style="font-size:11.5px; color:var(--text-muted); line-height:1.4; margin-top:2px;">{{ Str::limit($n['message'] ?? '', 45) }}</div>
                            </td>
                            <td><span class="badge badge-info">{{ $n['targetAudience'] ?? 'ALL' }}</span></td>
                            <td>
                                @if(!empty($n['sentAt']))
                                    {{ date('d M, h:i A', is_numeric($n['sentAt']) ? $n['sentAt']/1000 : time()) }}
                                @else
                                    -
                                @endif
                            </td>
                            <td style="text-align:right;">
                                <form method="POST" action="{{ route('notifications.delete') }}" style="display:inline;">
                                    @csrf
                                    <input type="hidden" name="id" value="{{ $n['id'] }}">
                                    <button type="submit" class="btn btn-danger" style="padding:3px 8px; font-size:11px;"><i class="fas fa-trash-alt"></i></button>
                                </form>
                            </td>
                        </tr>
                    @empty
                        <tr>
                            <td colspan="4" style="text-align:center; padding:20px; color:var(--text-muted);">No broadcast notification history found.</td>
                        </tr>
                    @endforelse
                </tbody>
            </table>
        </div>
    </div>
</div>
@endsection
