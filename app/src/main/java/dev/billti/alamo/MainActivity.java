package dev.billti.alamo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends AppCompatActivity {
    private static final String initialUrl = "https://news.ycombinator.com/";
    WebView mainWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Called once in the lifetime of an activity. If the activity has never existed before
        // 'savedInstanceState' is null, else will be any prior saved state.
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            // TODO: Restore state from prior activation
        }
        setContentView(R.layout.activity_main);
        mainWebView = (WebView)findViewById(R.id.main_webview);
    }

    // TODO: Should override onPause and onResume to save/restore URL location?
    // Also see onSaveInstanceState/onRestoreInstanceState

    @Override
    protected void onResume() {
        super.onResume();

        mainWebView.setWebChromeClient(webChromeClient);
        mainWebView.setWebViewClient(webViewClient);
        configureWebSettings(mainWebView.getSettings());
        mainWebView.loadUrl(initialUrl);
    }

    private static void configureWebSettings(WebSettings settings) {
        settings.setJavaScriptEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        settings.setGeolocationEnabled(true);
        settings.setAppCacheEnabled(true);
        settings.setDatabaseEnabled(true);
    }

    private WebChromeClient webChromeClient = new WebChromeClient() {
        @Override
        public void onPermissionRequest(PermissionRequest request) {
            // TODO: Fix below to limit requests approved
            request.grant(request.getResources());
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            callback.invoke(origin, true, true);
        }
    };

    private WebViewClient webViewClient = new WebViewClient() {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            // TODO: Just allow all in place for now. Might want to filter those allowed by domain.
            return false;
        }
    };
}
