/**
 * Video Tutorials Management JavaScript — Matching Android App Categories
 * HisabNiben Admin Panel
 */

let allTutorials = [];
let currentCategoryFilter = 'all';

const CATEGORY_MAP = {
    'getting_started': '🚀 শুরু করুন',
    'sales': '🌾 চাল বিক্রি',
    'purchase': '🛒 চাল ক্রয়',
    'stock': '📦 স্টক / মজুদ',
    'customer': '👥 কাস্টমার',
    'supplier': '🏢 মহাজন',
    'wallet': '💼 ওয়ালেট / ক্যাশ',
    'expense': '💸 খরচ / ব্যয়',
    'reports': '📊 রিপোর্ট',
    'sms': '💬 SMS',
    'subscription': '💳 সাবস্ক্রিপশন',
    'settings': '⚙️ সেটিংস',
    'other': '📌 অন্যান্য'
};

function initTutorialsPage() {
    loadTutorials();
}

function filterCategory(catKey, btnElement) {
    currentCategoryFilter = catKey;
    document.querySelectorAll('.category-tab').forEach(b => b.classList.remove('active'));
    if (btnElement) btnElement.classList.add('active');

    renderTutorialsGrid();
}

function loadTutorials() {
    const grid = document.getElementById('tutorials-grid');
    if (!grid) return;

    grid.innerHTML = `
        <div style="grid-column: 1/-1; text-align: center; padding: 40px; color: #94a3b8;">
            <i class="fas fa-spinner fa-spin fa-2x"></i>
            <p style="margin-top: 12px;">টিউটোরিয়াল ভিডিও লোড হচ্ছে...</p>
        </div>
    `;

    const db = getFirestore();
    if (!db) {
        grid.innerHTML = `<div style="grid-column: 1/-1; text-align: center; color: #ef4444; padding: 20px;">Database connection failed</div>`;
        return;
    }

    db.collection('tutorial_videos')
        .onSnapshot((snapshot) => {
            allTutorials = [];
            snapshot.forEach((doc) => {
                allTutorials.push({ id: doc.id, ...doc.data() });
            });

            renderTutorialsGrid();
        }, (error) => {
            console.error('Error fetching tutorials:', error);
            grid.innerHTML = `<div style="grid-column: 1/-1; text-align: center; color: #ef4444; padding: 20px;">Failed to load tutorials: ${error.message}</div>`;
        });
}

function renderTutorialsGrid() {
    const grid = document.getElementById('tutorials-grid');
    if (!grid) return;

    let filtered = allTutorials;
    if (currentCategoryFilter !== 'all') {
        filtered = allTutorials.filter(t => (t.category || 'other').toLowerCase() === currentCategoryFilter.toLowerCase());
    }

    if (filtered.length === 0) {
        grid.innerHTML = `
            <div style="grid-column: 1/-1; text-align: center; padding: 60px 20px; background: #0f172a; border-radius: 16px; border: 1px dashed rgba(255,255,255,0.1);">
                <i class="fas fa-video-slash" style="font-size: 40px; color: #64748b; margin-bottom: 12px;"></i>
                <h3 style="font-size: 16px; font-weight: 700; color: #fff; margin-bottom: 6px;">এই ক্যাটাগরিতে কোনো টিউটোরিয়াল নেই</h3>
                <p style="font-size: 13.5px; color: #94a3b8; margin-bottom: 20px;">অ্যাপ ব্যবহারকারীদের জন্য নতুন ভিডিও টিউটোরিয়াল যুক্ত করুন।</p>
                <button onclick="openCreateModal()" style="background: #f54927; color: #fff; border: none; padding: 10px 20px; border-radius: 10px; font-weight: 600; cursor: pointer;">
                    <i class="fas fa-plus"></i> নতুন টিউটোরিয়াল যুক্ত করুন
                </button>
            </div>
        `;
        return;
    }

    grid.innerHTML = filtered.map((item) => {
        const thumb = item.thumbnailUrl || (item.videoUrl ? getYouTubeThumbnail(item.videoUrl) : 'https://placehold.co/600x400/0f172a/f54927?text=No+Thumbnail');
        const catKey = (item.category || 'other').toLowerCase();
        const catLabel = CATEGORY_MAP[catKey] || item.category || 'অন্যান্য';

        return `
            <div class="tutorial-card">
                <div class="tutorial-thumb">
                    <img src="${thumb}" alt="${Utils.escapeHtml(item.title || '')}" onerror="this.src='https://placehold.co/600x400/0f172a/f54927?text=Video+Guide'">
                    <a href="${item.videoUrl || '#'}" target="_blank" class="play-overlay">
                        <div class="play-btn-circle"><i class="fas fa-play"></i></div>
                    </a>
                </div>
                <div style="padding: 16px; display: flex; flex-direction: column; flex-grow: 1;">
                    <h4 style="font-size: 15px; font-weight: 700; color: #fff; margin-bottom: 6px; line-height: 1.4;">${Utils.escapeHtml(item.title || 'Untitled Tutorial')}</h4>
                    <p style="font-size: 13px; color: #94a3b8; margin-bottom: 14px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; flex-grow: 1;">${Utils.escapeHtml(item.description || 'বিবরণ দেওয়া হয়নি।')}</p>
                    <div style="display: flex; align-items: center; justify-content: space-between; border-top: 1px solid rgba(255,255,255,0.06); padding-top: 12px; margin-top: auto;">
                        <span style="font-size: 11.5px; font-weight: 600; background: rgba(245,73,39,0.15); color: #f54927; padding: 4px 10px; border-radius: 6px;">${Utils.escapeHtml(catLabel)}</span>
                        <div style="display: flex; gap: 10px;">
                            <button onclick="editTutorial('${item.id}')" style="background: none; border: none; color: #38bdf8; cursor: pointer; font-size: 14px;" title="Edit"><i class="fas fa-edit"></i></button>
                            <button onclick="deleteTutorial('${item.id}')" style="background: none; border: none; color: #ef4444; cursor: pointer; font-size: 14px;" title="Delete"><i class="fas fa-trash"></i></button>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

function getYouTubeThumbnail(url) {
    if (!url) return '';
    const match = url.match(/(?:youtube\.com\/(?:[^\/]+\/.+\/|(?:v|e(?:mbed)?)\/|.*[?&]v=)|youtu\.be\/)([^"&?\/\s]{11})/);
    return match ? `https://img.youtube.com/vi/${match[1]}/hqdefault.jpg` : '';
}

function autoFetchThumbnail() {
    const url = document.getElementById('input-url').value;
    const thumbInput = document.getElementById('input-thumbnail');
    if (url && !thumbInput.value) {
        const thumb = getYouTubeThumbnail(url);
        if (thumb) thumbInput.value = thumb;
    }
}

function openCreateModal() {
    document.getElementById('edit-id').value = '';
    document.getElementById('modal-title').textContent = 'নতুন ভিডিও টিউটোরিয়াল যুক্ত করুন';
    document.getElementById('tutorial-form').reset();
    document.getElementById('input-category').value = 'getting_started';
    document.getElementById('input-videotype').value = 'youtube';
    document.getElementById('tutorial-modal').classList.add('active');
}

function editTutorial(id) {
    const item = allTutorials.find(t => t.id === id);
    if (!item) return;

    document.getElementById('edit-id').value = item.id;
    document.getElementById('modal-title').textContent = 'ভিডিও টিউটোরিয়াল সম্পাদন করুন (Edit)';
    document.getElementById('input-category').value = (item.category || 'getting_started').toLowerCase();
    document.getElementById('input-videotype').value = (item.videoType || 'youtube').toLowerCase();
    document.getElementById('input-title').value = item.title || '';
    document.getElementById('input-url').value = item.videoUrl || '';
    document.getElementById('input-thumbnail').value = item.thumbnailUrl || '';
    document.getElementById('input-description').value = item.description || '';

    document.getElementById('tutorial-modal').classList.add('active');
}

function closeModal() {
    document.getElementById('tutorial-modal').classList.remove('active');
}

async function saveTutorial(event) {
    event.preventDefault();

    const db = getFirestore();
    if (!db) {
        Toast.show('Database connection missing', 'error');
        return;
    }

    const editId = document.getElementById('edit-id').value;
    const category = document.getElementById('input-category').value;
    const videoType = document.getElementById('input-videotype').value;
    const title = document.getElementById('input-title').value.trim();
    const videoUrl = document.getElementById('input-url').value.trim();
    let thumbnailUrl = document.getElementById('input-thumbnail').value.trim();
    const description = document.getElementById('input-description').value.trim();

    if (!thumbnailUrl && videoUrl && videoType === 'youtube') {
        thumbnailUrl = getYouTubeThumbnail(videoUrl);
    }

    const data = {
        category,
        videoType,
        title,
        videoUrl,
        thumbnailUrl,
        description,
        isPublished: true,
        sortOrder: 1,
        updatedAt: Date.now(),
        createdBy: 'Admin'
    };

    try {
        if (editId) {
            await db.collection('tutorial_videos').doc(editId).update(data);
            Toast.show('ভিডিও টিউটোরিয়াল আপডেট হয়েছে!', 'success');
        } else {
            data.createdAt = Date.now();
            await db.collection('tutorial_videos').add(data);
            Toast.show('নতুন ভিডিও টিউটোরিয়াল যুক্ত হয়েছে!', 'success');
        }
        closeModal();
    } catch (error) {
        console.error('Error saving tutorial:', error);
        Toast.show('সংরক্ষণে সমস্যা হয়েছে: ' + error.message, 'error');
    }
}

async function deleteTutorial(id) {
    if (!confirm('আপনি কি এই ভিডিও টিউটোরিয়ালটি মুছে ফেলতে নিশ্চিত?')) return;

    const db = getFirestore();
    if (!db) return;

    try {
        await db.collection('tutorial_videos').doc(id).delete();
        Toast.show('ভিডিও টিউটোরিয়াল মুছে ফেলা হয়েছে', 'info');
    } catch (error) {
        console.error('Error deleting tutorial:', error);
        Toast.show('মুছে ফেলতে সমস্যা হয়েছে', 'error');
    }
}
