package com.sajoldev.hisabniben.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.adapter.TutorialAdapter;
import com.sajoldev.hisabniben.model.TutorialVideo;

public class TutorialVideoActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private PlayerView playerView;
    private WebView webViewPlayer;
    private FrameLayout layoutFallbackPlayer;
    private ImageView ivDetailThumbnail, ivBigPlayButton;
    private ProgressBar playerProgressBar;

    private TextView tvDetailCategory, tvDetailPlatform, tvDetailDuration, tvDetailTitle, tvDetailDescription;
    private MaterialButton btnOpenExternalVideo;

    private TutorialVideo video;
    private ExoPlayer exoPlayer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_tutorial_video_detail);

        if (getIntent() != null && getIntent().hasExtra(TutorialListActivity.EXTRA_TUTORIAL_VIDEO)) {
            video = (TutorialVideo) getIntent().getSerializableExtra(TutorialListActivity.EXTRA_TUTORIAL_VIDEO);
        }

        if (video == null) {
            Toast.makeText(this, "ভিডিও পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (video != null && video.getId() != null) {
            com.sajoldev.hisabniben.util.SessionManager.getInstance(this).markVideoAsWatched(video.getId());
        }

        initViews();
        setupWindowInsets();
        setupToolbar();
        bindVideoData();
        setupPlayer();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        playerView = findViewById(R.id.playerView);
        webViewPlayer = findViewById(R.id.webViewPlayer);
        layoutFallbackPlayer = findViewById(R.id.layoutFallbackPlayer);
        ivDetailThumbnail = findViewById(R.id.ivDetailThumbnail);
        ivBigPlayButton = findViewById(R.id.ivBigPlayButton);
        playerProgressBar = findViewById(R.id.playerProgressBar);

        tvDetailCategory = findViewById(R.id.tvDetailCategory);
        tvDetailPlatform = findViewById(R.id.tvDetailPlatform);
        tvDetailDuration = findViewById(R.id.tvDetailDuration);
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        btnOpenExternalVideo = findViewById(R.id.btnOpenExternalVideo);
    }

    private void setupWindowInsets() {
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, windowInsets) -> {
                int topInset = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                int bottomInset = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                v.setPadding(0, topInset, 0, bottomInset);
                return WindowInsetsCompat.CONSUMED;
            });
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(video.getTitle() != null ? video.getTitle() : "ভিডিও টিউটোরিয়াল");
        }
        toolbar.setTitleTextColor(getResources().getColor(R.color.text_primary));
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void bindVideoData() {
        tvDetailTitle.setText(video.getTitle() != null ? video.getTitle() : "");
        tvDetailDescription.setText(video.getDescription() != null ? video.getDescription() : "");
        tvDetailCategory.setText(video.getCategoryLabelBangla());

        String typeStr = video.getVideoType() != null ? video.getVideoType().toUpperCase() : "VIDEO";
        tvDetailPlatform.setText(typeStr);

        if (video.getDuration() != null && !video.getDuration().trim().isEmpty()) {
            tvDetailDuration.setText("⏱️ " + video.getDuration().trim());
        } else {
            tvDetailDuration.setText("");
        }

        String thumbUrl = video.getThumbnailUrl();
        if (thumbUrl == null || thumbUrl.trim().isEmpty()) {
            if (TutorialVideo.TYPE_YOUTUBE.equalsIgnoreCase(video.getVideoType())) {
                String ytId = TutorialAdapter.extractYouTubeId(video.getVideoUrl());
                if (ytId != null) {
                    thumbUrl = "https://img.youtube.com/vi/" + ytId + "/hqdefault.jpg";
                }
            }
        }

        if (thumbUrl != null && !thumbUrl.trim().isEmpty()) {
            Glide.with(this)
                    .load(thumbUrl)
                    .placeholder(R.drawable.bg_rounded_card)
                    .into(ivDetailThumbnail);
        } else {
            ivDetailThumbnail.setImageResource(R.drawable.bg_rounded_card);
        }
    }

    private void setupPlayer() {
        String videoType = video.getVideoType() != null ? video.getVideoType().toLowerCase() : TutorialVideo.TYPE_YOUTUBE;
        String videoUrl = video.getVideoUrl();

        if (TutorialVideo.TYPE_YOUTUBE.equals(videoType)) {
            String ytId = TutorialAdapter.extractYouTubeId(videoUrl);
            if (ytId != null) {
                setupYouTubeWebView(ytId);
                btnOpenExternalVideo.setText("YouTube App-এ ভিডিওটি দেখুন");
            } else {
                setupFallbackMode("ভিডিও চালু করুন");
            }
            btnOpenExternalVideo.setOnClickListener(v -> openExternalUrl(videoUrl));

        } else if (TutorialVideo.TYPE_FACEBOOK.equals(videoType)) {
            setupFallbackMode("Facebook-এ ভিডিওটি দেখুন");
            btnOpenExternalVideo.setOnClickListener(v -> openExternalUrl(videoUrl));

        } else if (TutorialVideo.TYPE_DIRECT.equals(videoType) && videoUrl != null && !videoUrl.isEmpty()) {
            setupExoPlayer(videoUrl);
            btnOpenExternalVideo.setText("ব্রাউজারে খুলুন");
            btnOpenExternalVideo.setOnClickListener(v -> openExternalUrl(videoUrl));

        } else {
            setupFallbackMode("ভিডিও লিঙ্কটি খুলুন");
            btnOpenExternalVideo.setOnClickListener(v -> openExternalUrl(videoUrl));
        }
    }

    private void setupYouTubeWebView(String ytId) {
        layoutFallbackPlayer.setVisibility(View.GONE);
        playerView.setVisibility(View.GONE);
        webViewPlayer.setVisibility(View.VISIBLE);

        WebSettings webSettings = webViewPlayer.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        webViewPlayer.setWebViewClient(new WebViewClient());
        webViewPlayer.setWebChromeClient(new WebChromeClient());

        String embedHtml = "<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0\"><style>body{margin:0;padding:0;background-color:#000000;}.embed-container{position:relative;padding-bottom:56.25%;height:0;overflow:hidden;max-width:100%;}.embed-container iframe{position:absolute;top:0;left:0;width:100%;height:100%;border:0;}</style></head><body><div class=\"embed-container\"><iframe src=\"https://www.youtube.com/embed/" + ytId + "?autoplay=1&rel=0&modestbranding=1\" allowfullscreen allow=\"autoplay\"></iframe></div></body></html>";
        webViewPlayer.loadDataWithBaseURL("https://www.youtube.com", embedHtml, "text/html", "utf-8", null);
    }

    private void setupExoPlayer(String videoUrl) {
        layoutFallbackPlayer.setVisibility(View.GONE);
        webViewPlayer.setVisibility(View.GONE);
        playerView.setVisibility(View.VISIBLE);

        try {
            exoPlayer = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(exoPlayer);

            MediaItem mediaItem = MediaItem.fromUri(Uri.parse(videoUrl));
            exoPlayer.setMediaItem(mediaItem);
            exoPlayer.prepare();
            exoPlayer.setPlayWhenReady(true);

            exoPlayer.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    if (playbackState == Player.STATE_BUFFERING) {
                        playerProgressBar.setVisibility(View.VISIBLE);
                    } else {
                        playerProgressBar.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onPlayerError(PlaybackException error) {
                    playerProgressBar.setVisibility(View.GONE);
                    Toast.makeText(TutorialVideoActivity.this, "ভিডিওটি চালানো যাচ্ছে না, ব্রাউজারে খোলার চেষ্টা করুন", Toast.LENGTH_LONG).show();
                    setupFallbackMode("ব্রাউজারে খুলুন");
                }
            });
        } catch (Exception e) {
            setupFallbackMode("ব্রাউজারে খুলুন");
        }
    }

    private void setupFallbackMode(String buttonText) {
        playerView.setVisibility(View.GONE);
        webViewPlayer.setVisibility(View.GONE);
        layoutFallbackPlayer.setVisibility(View.VISIBLE);
        btnOpenExternalVideo.setText(buttonText);

        ivBigPlayButton.setOnClickListener(v -> openExternalUrl(video.getVideoUrl()));
    }

    private void openExternalUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(this, "ভিডিও লিঙ্ক পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .build();
            customTabsIntent.launchUrl(this, Uri.parse(url));
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            } catch (Exception ex) {
                Toast.makeText(this, "লিঙ্কটি খোলা যাচ্ছে না", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (exoPlayer != null && exoPlayer.isPlaying()) {
            exoPlayer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
        if (webViewPlayer != null) {
            webViewPlayer.destroy();
        }
    }
}
