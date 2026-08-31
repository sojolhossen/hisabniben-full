<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="csrf-token" content="{{ csrf_token() }}">
    <title>@yield('title', 'HisabNiben') — Enterprise Admin Panel</title>

    <!-- Google Fonts & Icons -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@300;400;500;600;700;800&family=Hind+Siliguri:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    
    <!-- Alpine.js & Chart.js & Toastr -->
    <script defer src="https://cdn.jsdelivr.net/npm/alpinejs@3.x.x/dist/cdn.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>

    <style>
        :root {
            --primary: #F54927;
            --primary-hover: #E03816;
            --primary-light: rgba(245, 73, 39, 0.08);
            --dark: #0F172A;
            --dark-medium: #1E293B;
            --bg-body: #F8FAFC;
            --bg-surface: #FFFFFF;
            --bg-card: #FFFFFF;
            --border-color: #E2E8F0;
            --text-main: #1E293B;
            --text-muted: #64748B;
            --success: #10B981;
            --warning: #F59E0B;
            --danger: #EF4444;
            --info: #3B82F6;
            --radius-sm: 8px;
            --radius-md: 12px;
            --radius-lg: 16px;
            --shadow-sm: 0 1px 3px rgba(15, 23, 42, 0.06);
            --shadow-md: 0 4px 14px rgba(15, 23, 42, 0.08);
            --shadow-lg: 0 10px 25px rgba(15, 23, 42, 0.12);
        }

        [x-cloak] { display: none !important; }

        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
            font-family: 'Plus Jakarta Sans', 'Hind Siliguri', -apple-system, sans-serif;
        }

        body {
            background-color: var(--bg-body);
            color: var(--text-main);
            display: flex;
            min-height: 100vh;
            overflow-x: hidden;
        }

        /* Sidebar Styles */
        .sidebar {
            width: 260px;
            background: var(--dark);
            color: #94A3B8;
            display: flex;
            flex-direction: column;
            position: fixed;
            top: 0;
            bottom: 0;
            left: 0;
            z-index: 100;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            border-right: 1px solid rgba(255, 255, 255, 0.05);
        }

        .sidebar-brand {
            padding: 24px 20px;
            display: flex;
            align-items: center;
            gap: 12px;
            border-bottom: 1px solid rgba(255, 255, 255, 0.08);
        }

        .brand-icon {
            width: 38px;
            height: 38px;
            background: linear-gradient(135deg, var(--primary), #FF6B4A);
            border-radius: var(--radius-sm);
            display: flex;
            align-items: center;
            justify-content: center;
            color: white;
            font-size: 18px;
            box-shadow: 0 4px 12px rgba(245, 73, 39, 0.35);
        }

        .brand-title {
            color: white;
            font-weight: 800;
            font-size: 18px;
            letter-spacing: -0.5px;
        }

        .sidebar-nav {
            padding: 16px 12px;
            flex: 1;
            overflow-y: auto;
            display: flex;
            flex-direction: column;
            gap: 4px;
        }

        .nav-section-label {
            padding: 12px 12px 6px;
            font-size: 10.5px;
            font-weight: 800;
            text-transform: uppercase;
            letter-spacing: 1px;
            color: #64748B;
        }

        .nav-link {
            display: flex;
            align-items: center;
            gap: 12px;
            padding: 11px 14px;
            color: #94A3B8;
            text-decoration: none;
            font-weight: 600;
            font-size: 13.5px;
            border-radius: var(--radius-sm);
            transition: all 0.2s ease;
        }

        .nav-link:hover {
            color: white;
            background: rgba(255, 255, 255, 0.06);
        }

        .nav-link.active {
            color: white;
            background: var(--primary);
            box-shadow: 0 4px 12px rgba(245, 73, 39, 0.3);
        }

        .nav-link i {
            width: 20px;
            text-align: center;
            font-size: 15px;
        }

        .nav-badge {
            margin-left: auto;
            background: rgba(255, 255, 255, 0.15);
            color: white;
            padding: 2px 7px;
            border-radius: 20px;
            font-size: 11px;
            font-weight: 700;
        }

        .nav-badge.danger {
            background: var(--danger);
        }

        /* Main Content Wrapper */
        .main-wrapper {
            margin-left: 260px;
            flex: 1;
            display: flex;
            flex-direction: column;
            min-width: 0;
        }

        /* Header Navbar */
        .header-navbar {
            height: 70px;
            background: var(--bg-surface);
            border-bottom: 1px solid var(--border-color);
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 0 28px;
            position: sticky;
            top: 0;
            z-index: 90;
            box-shadow: var(--shadow-sm);
        }

        .header-left {
            display: flex;
            align-items: center;
            gap: 16px;
        }

        .command-search {
            display: flex;
            align-items: center;
            gap: 10px;
            background: var(--bg-body);
            border: 1px solid var(--border-color);
            padding: 8px 14px;
            border-radius: var(--radius-md);
            width: 320px;
            font-size: 13px;
            color: var(--text-muted);
            cursor: pointer;
        }

        .header-right {
            display: flex;
            align-items: center;
            gap: 16px;
        }

        .user-profile-btn {
            display: flex;
            align-items: center;
            gap: 10px;
            cursor: pointer;
            padding: 6px 10px;
            border-radius: var(--radius-sm);
            transition: background 0.2s ease;
        }

        .user-profile-btn:hover {
            background: var(--bg-body);
        }

        .user-avatar {
            width: 36px;
            height: 36px;
            border-radius: 50%;
            background: linear-gradient(135deg, #3B82F6, #1D4ED8);
            color: white;
            font-weight: 800;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 14px;
        }

        /* Content Area */
        .content-container {
            padding: 28px;
            flex: 1;
        }

        /* Card System */
        .card-panel {
            background: var(--bg-card);
            border: 1px solid var(--border-color);
            border-radius: var(--radius-md);
            box-shadow: var(--shadow-sm);
            padding: 22px;
            margin-bottom: 24px;
        }

        /* Bento KPI Grid */
        .kpi-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
            gap: 18px;
            margin-bottom: 24px;
        }

        .kpi-card {
            background: var(--bg-card);
            border: 1px solid var(--border-color);
            border-radius: var(--radius-md);
            padding: 20px;
            box-shadow: var(--shadow-sm);
            display: flex;
            align-items: center;
            justify-content: space-between;
            transition: transform 0.2s ease, box-shadow 0.2s ease;
        }

        .kpi-card:hover {
            transform: translateY(-2px);
            box-shadow: var(--shadow-md);
        }

        .kpi-icon {
            width: 48px;
            height: 48px;
            border-radius: var(--radius-md);
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 20px;
        }

        .kpi-title {
            font-size: 12px;
            font-weight: 700;
            color: var(--text-muted);
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .kpi-value {
            font-size: 24px;
            font-weight: 800;
            color: var(--dark);
            margin-top: 4px;
        }

        /* Buttons */
        .btn {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            padding: 9px 16px;
            font-size: 13px;
            font-weight: 700;
            border-radius: var(--radius-sm);
            border: none;
            cursor: pointer;
            transition: all 0.2s ease;
            text-decoration: none;
        }

        .btn-primary {
            background: var(--primary);
            color: white;
        }

        .btn-primary:hover {
            background: var(--primary-hover);
        }

        .btn-outline {
            background: transparent;
            border: 1px solid var(--border-color);
            color: var(--text-main);
        }

        .btn-outline:hover {
            background: var(--bg-body);
        }

        .btn-danger {
            background: var(--danger);
            color: white;
        }

        /* Status Badges */
        .badge {
            padding: 4px 10px;
            border-radius: 20px;
            font-size: 11.5px;
            font-weight: 700;
            display: inline-block;
        }

        .badge-success { background: rgba(16, 185, 129, 0.12); color: #059669; }
        .badge-warning { background: rgba(245, 158, 11, 0.12); color: #D97706; }
        .badge-danger { background: rgba(239, 68, 68, 0.12); color: #DC2626; }
        .badge-info { background: rgba(59, 130, 246, 0.12); color: #2563EB; }

        /* Tables */
        .table-responsive {
            width: 100%;
            overflow-x: auto;
        }

        .data-table {
            width: 100%;
            border-collapse: collapse;
            font-size: 13.5px;
        }

        .data-table th {
            background: var(--bg-body);
            color: var(--text-muted);
            font-weight: 700;
            text-transform: uppercase;
            font-size: 11px;
            letter-spacing: 0.5px;
            padding: 14px 16px;
            text-align: left;
            border-bottom: 1px solid var(--border-color);
        }

        .data-table td {
            padding: 14px 16px;
            border-bottom: 1px solid var(--border-color);
            vertical-align: middle;
        }

        .data-table tr:hover {
            background: rgba(248, 250, 252, 0.8);
        }
    </style>
    @yield('styles')
</head>
<body x-data="{ sidebarOpen: true }">

    <!-- SIDEBAR -->
    <aside class="sidebar">
        <div class="sidebar-brand">
            <div class="brand-icon"><i class="fas fa-calculator"></i></div>
            <div class="brand-title">HisabNiben</div>
        </div>

        <nav class="sidebar-nav">
            <div class="nav-section-label">Main Navigation</div>

            <a href="{{ route('dashboard') }}" class="nav-link {{ request()->routeIs('dashboard') ? 'active' : '' }}">
                <i class="fas fa-chart-pie"></i>
                <span>Dashboard</span>
            </a>

            <a href="{{ route('users.index') }}" class="nav-link {{ request()->routeIs('users.*') ? 'active' : '' }}">
                <i class="fas fa-users"></i>
                <span>Users & CRM</span>
            </a>

            <div class="nav-section-label">Financial Management</div>

            <a href="{{ route('payments.requests') }}" class="nav-link {{ request()->routeIs('payments.requests') ? 'active' : '' }}">
                <i class="fas fa-receipt"></i>
                <span>Payment Requests</span>
                @if(isset($pendingPaymentsCount) && $pendingPaymentsCount > 0)
                    <span class="nav-badge danger">{{ $pendingPaymentsCount }}</span>
                @endif
            </a>

            <a href="{{ route('payments.history') }}" class="nav-link {{ request()->routeIs('payments.history') ? 'active' : '' }}">
                <i class="fas fa-history"></i>
                <span>Purchase History</span>
            </a>

            <a href="{{ route('packages.index') }}" class="nav-link {{ request()->routeIs('packages.*') ? 'active' : '' }}">
                <i class="fas fa-box"></i>
                <span>Packages Manager</span>
            </a>

            <a href="{{ route('payment-methods.index') }}" class="nav-link {{ request()->routeIs('payment-methods.*') ? 'active' : '' }}">
                <i class="fas fa-credit-card"></i>
                <span>Payment Methods</span>
            </a>

            <div class="nav-section-label">Communication & Support</div>

            <a href="{{ route('notifications.index') }}" class="nav-link {{ request()->routeIs('notifications.*') ? 'active' : '' }}">
                <i class="fas fa-paper-plane"></i>
                <span>Push Notifications</span>
            </a>

            <a href="{{ route('sms.settings') }}" class="nav-link {{ request()->routeIs('sms.settings') ? 'active' : '' }}">
                <i class="fas fa-comment-sms"></i>
                <span>SMS Gateway Settings</span>
            </a>

            <a href="{{ route('sms.packages') }}" class="nav-link {{ request()->routeIs('sms.packages') ? 'active' : '' }}">
                <i class="fas fa-comments"></i>
                <span>SMS Bundles</span>
            </a>

            <a href="{{ route('tutorials.index') }}" class="nav-link {{ request()->routeIs('tutorials.*') ? 'active' : '' }}">
                <i class="fas fa-play-circle"></i>
                <span>Video Tutorials</span>
            </a>

            <div class="nav-section-label">System Control</div>

            <a href="{{ route('settings.index') }}" class="nav-link {{ request()->routeIs('settings.*') ? 'active' : '' }}">
                <i class="fas fa-sliders"></i>
                <span>System Settings</span>
            </a>
        </nav>
    </aside>

    <!-- MAIN WRAPPER -->
    <div class="main-wrapper">
        <!-- HEADER NAVBAR -->
        <header class="header-navbar">
            <div class="header-left">
                <div class="command-search">
                    <i class="fas fa-search"></i>
                    <span>Quick Search users, transactions...</span>
                    <span style="margin-left:auto; background:var(--border-color); padding:2px 6px; border-radius:4px; font-size:10px; font-weight:700;">Ctrl + K</span>
                </div>
            </div>

            <div class="header-right">
                <div style="font-size:13px; font-weight:700; color:var(--text-muted);">
                    <i class="fas fa-calendar-day" style="color:var(--primary);"></i>
                    {{ date('d M, Y') }}
                </div>

                <form method="POST" action="{{ route('logout') }}" style="display:inline;">
                    @csrf
                    <button type="submit" class="btn btn-outline" style="padding:6px 12px; font-size:12px;">
                        <i class="fas fa-right-from-bracket"></i> Logout
                    </button>
                </form>
            </div>
        </header>

        <!-- CONTENT -->
        <main class="content-container">
            @if(session('success'))
                <script>
                    document.addEventListener('DOMContentLoaded', () => {
                        Swal.fire({ icon: 'success', title: 'Success!', text: "{{ session('success') }}", timer: 3000, showConfirmButton: false });
                    });
                </script>
            @endif

            @if(session('error'))
                <script>
                    document.addEventListener('DOMContentLoaded', () => {
                        Swal.fire({ icon: 'error', title: 'Error!', text: "{{ session('error') }}", timer: 4000, showConfirmButton: false });
                    });
                </script>
            @endif

            @yield('content')
        </main>
    </div>

    @yield('scripts')
</body>
</html>
