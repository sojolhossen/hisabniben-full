<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin Login — HisabNiben</title>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
    <style>
        * { margin:0; padding:0; box-sizing:border-box; font-family:'Plus Jakarta Sans', sans-serif; }
        body { background:#0F172A; min-height:100vh; display:flex; align-items:center; justify-content:center; padding:20px; }
        .login-card { background:#1E293B; border:1px solid rgba(255,255,255,0.1); border-radius:16px; width:100%; max-width:420px; padding:36px; box-shadow:0 20px 40px rgba(0,0,0,0.4); text-align:center; color:white; }
        .logo-box { width:60px; height:60px; background:linear-gradient(135deg, #F54927, #FF6B4A); border-radius:14px; display:inline-flex; align-items:center; justify-content:center; font-size:28px; color:white; margin-bottom:16px; box-shadow:0 8px 20px rgba(245,73,39,0.4); }
        .login-title { font-size:24px; font-weight:800; margin-bottom:6px; }
        .login-subtitle { font-size:13px; color:#94A3B8; margin-bottom:28px; }
        .form-group { text-align:left; margin-bottom:20px; }
        .form-label { display:block; font-size:12px; font-weight:700; color:#CBD5E1; margin-bottom:8px; text-transform:uppercase; letter-spacing:0.5px; }
        .input-box { width:100%; padding:12px 16px; background:#0F172A; border:1px solid #334155; border-radius:10px; color:white; font-size:14px; outline:none; transition:border 0.2s ease; }
        .input-box:focus { border-color:#F54927; }
        .btn-login { width:100%; padding:14px; background:#F54927; color:white; border:none; border-radius:10px; font-size:15px; font-weight:800; cursor:pointer; transition:background 0.2s ease; margin-top:10px; }
        .btn-login:hover { background:#E03816; }
    </style>
</head>
<body>
    <div class="login-card">
        <div class="logo-box"><i class="fas fa-calculator"></i></div>
        <h2 class="login-title">HisabNiben Admin</h2>
        <p class="login-subtitle">Enterprise Super Admin Control Panel</p>

        @if(session('error'))
            <script>
                document.addEventListener('DOMContentLoaded', () => {
                    Swal.fire({ icon: 'error', title: 'Authentication Failed', text: "{{ session('error') }}" });
                });
            </script>
        @endif

        <form method="POST" action="{{ route('login.post') }}">
            @csrf
            <div class="form-group">
                <label class="form-label">Admin Email</label>
                <input type="email" name="email" class="input-box" placeholder="admin@hisabniben.com" required value="admin@hisabniben.com">
            </div>

            <div class="form-group">
                <label class="form-label">Password</label>
                <input type="password" name="password" class="input-box" placeholder="••••••••" required value="admin123456">
            </div>

            <button type="submit" class="btn-login"><i class="fas fa-lock"></i> Sign In to Admin Panel</button>
        </form>
    </div>
</body>
</html>
