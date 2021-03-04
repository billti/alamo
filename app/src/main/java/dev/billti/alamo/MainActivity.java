/*
TODO
- Test camera capture of picture or video
- Test uploading the audio/video files to the server
- Add file picker for photo/video from library
  - https://developer.android.com/reference/android/webkit/WebChromeClient.FileChooserParams
- Test with service workers and activity reload while offline
- Test switching to other apps and back (including killing the app while in the background)
- Test killing the app and restarting (e.g. App Manager)
- Test that with camera/mic active, switch to another app that wants camera doesn't block it.
  - And that it still works on switching back to Alamo
- Test after disallowing permissions already granted via the Application Manager
- Figure out how to hook up JavaScript/host interaction (e.g. events or method calls).
  - Note: Don't actually have a use for this yet, but seems like it might be handy.

Notes:
- For Audio recording on the web see sample at https://github.com/mdn/web-dictaphone/

 */

package dev.billti.alamo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends AppCompatActivity {
    // Note: Ensure the manifest has the below attribute on the activity, else the activity is destroyed on rotation
    //     android:configChanges="orientation|screenSize|screenLayout|keyboardHidden"
    // See: https://stackoverflow.com/questions/1002085/android-webview-handling-orientation-changes

    // Docs: https://developer.android.com/reference/android/webkit/WebView
    WebView mainWebView;

    private static final String initialUrl = "https://billti.dev/";
    private static final boolean enableChromeDevTools = true;

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int REQUEST_AUDIO_PERMISSION = 2;
    private static final int REQUEST_GEO_PERMISSION = 3;

    private boolean requestCameraPermissionAtLaunch = false;
    private boolean requestAudioPermissionAtLaunch = false;
    private boolean requestGeoPermissionAtLaunch = false;
    private PermissionRequest cameraPermissionRequest;
    private PermissionRequest audioPermissionRequest;
    private GeolocationPermissions.Callback geoPermissionCallback;
    private String geoOrigin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Called once in the lifetime of an activity. If the activity has never existed before
        // 'savedInstanceState' is null, else will be any prior saved state.
        super.onCreate(savedInstanceState);
        Log.i("Alamo", "onCreate called");

        if (savedInstanceState != null) {
            // TODO: Restore state from prior activation.
        }

        if (requestCameraPermissionAtLaunch) {
            Log.i("Alamo", "Requesting camera permission at launch");
            requestCameraPermissions(null);
        }

        if (requestAudioPermissionAtLaunch) {
            Log.i("Alamo", "Requesting audio permission at launch");
            requestAudioPermissions(null);
        }

        if (requestGeoPermissionAtLaunch) {
            Log.i("Alamo", "Requesting geolocation permission at launch");
            requestGeoPermissions(null, null);
        }
        setContentView(R.layout.activity_main);
        mainWebView = (WebView)findViewById(R.id.main_webview);

        // Call the below to enable Chrome's DevTools to connect to the WebView
        // See https://developers.google.com/web/tools/chrome-devtools/remote-debugging/webviews
        if (enableChromeDevTools) {
            mainWebView.setWebContentsDebuggingEnabled(true);
        }
    }

    // TODO: Should override onPause and onResume to save/restore URL location?
    // Also see onSaveInstanceState/onRestoreInstanceState

    @Override
    protected void onResume() {
        // Note: This is called again when a permission is granted. (It can be called any time, technically)
        Log.i("Alamo", "onResume called");
        super.onResume();

        // Only set this up if it isn't already done. (i.e. not a re-run of onResume after approving a permission)
        if (!initialUrl.equals(mainWebView.getUrl())) {
            mainWebView.setWebChromeClient(webChromeClient);
            mainWebView.setWebViewClient(webViewClient);
            configureWebSettings(mainWebView.getSettings());
            Log.i("Alamo", String.format("Navigating the webview to: %s", initialUrl));
            mainWebView.loadUrl(initialUrl);
        } else {
            Log.i("Alamo", "Url was already loaded on entering onResume");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // The main job here is to funnel the result of the Android permission request back to the WebView permission request.

        if (permissions.length == 0) {
            // Seems pretty useless, but this does happen sometimes.
            Log.i("Alamo", "onRequestPermissionsResult called with permissions[] length of 0");
            return;
        }
        if (permissions.length != 1) {
            Log.e("Alamo", "!= 1 permissions result");
            throw new UnsupportedOperationException("Unable to handle onRequestPermissionResult with length != 1");
        } else {
            Log.i("Alamo", String.format("onRequestPermissionResults called with result: %s", grantResults[0] == PackageManager.PERMISSION_GRANTED ? "Granted" : "Not granted"));
            if (requestCode == REQUEST_CAMERA_PERMISSION) {
                if (cameraPermissionRequest != null) {
                    String[] resources = cameraPermissionRequest.getResources();
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Log.i("Alamo", String.format("Calling request.grant(%s)", resources[0]));
                        cameraPermissionRequest.grant(resources);

                    } else {
                        Log.i("Alamo", "Calling request.deny()");
                        cameraPermissionRequest.deny();
                    }
                    cameraPermissionRequest = null;
                } else {
                    Log.i("Alamo", "onRequestPermissionsResult called for camera with no active web request");
                }
            } else if (requestCode == REQUEST_AUDIO_PERMISSION) {
                if (audioPermissionRequest != null) {
                    String[] resources = audioPermissionRequest.getResources();
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Log.i("Alamo", String.format("Calling request.grant(%s)", resources[0]));
                        audioPermissionRequest.grant(resources);

                    } else {
                        Log.i("Alamo", "Calling request.deny()");
                        audioPermissionRequest.deny();
                    }
                    audioPermissionRequest = null;
                } else {
                    Log.i("Alamo", "onRequestPermissionsResult called for audio with no active web request");
                }
            }  else if (requestCode == REQUEST_GEO_PERMISSION) {
                if (geoPermissionCallback != null) {
                    boolean granted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    Log.i("Alamo", granted ? "geolocation permission granted" : "geolocation permission denied");
                    // Note: If 'retain' (last argument) is set to 'true' here, then the browser will never ask permission again.
                    // So it should be true if granted, false if denied, i.e., the same as the 'granted' result. That way
                    // if the user tries to use the feature again, if permission was not granted last time, it will ask again.
                    geoPermissionCallback.invoke(geoOrigin, granted, granted);
                    geoPermissionCallback = null;
                    geoOrigin = null;
                } else {
                    Log.i("Alamo", "onRequestPermissionsResult called for geolocation with no active callback");
                }
            } else {
                Log.w("Alamo", String.format("Didn't recognize requestCode %d. Calling super.onRequestPermissionResult", requestCode));
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }

    private void requestCameraPermissions(PermissionRequest request) {
        cameraPermissionRequest = request;
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Note: Best practice is to call shouldShowRequestPermissionRationale here and see if we should explain why we need it if they denied previously.
            // But here we'll just ask again, which works fine (i.e. the "shouldShow*" call isn't required).
            // See details at https://stackoverflow.com/questions/41310510/what-is-the-difference-between-shouldshowrequestpermissionrationale-and-requestp
            Log.i("Alamo", "Calling requestPermissions for camera");
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            Log.i("Alamo", "Camera permission was already granted");
            if (cameraPermissionRequest != null) {
                cameraPermissionRequest.grant(request.getResources());
                cameraPermissionRequest = null;
            }
        }
    }

    private void requestAudioPermissions(PermissionRequest request) {
        audioPermissionRequest = request;
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.i("Alamo", "Calling requestPermissions for audio");
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO_PERMISSION);
        } else {
            Log.i("Alamo", "Audio permission was already granted");
            if (audioPermissionRequest != null) {
                audioPermissionRequest.grant(request.getResources());
                audioPermissionRequest = null;
            }
        }
    }

    private void requestGeoPermissions(GeolocationPermissions.Callback callback, String origin) {
        geoPermissionCallback = callback;
        geoOrigin = origin;
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i("Alamo", "Calling requestPermissions for geolocation");
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_GEO_PERMISSION);
        } else {
            Log.i("Alamo", "Geolocation permission was already granted");
            if (geoPermissionCallback != null) {
                geoPermissionCallback.invoke(geoOrigin, true, true);
                geoPermissionCallback = null;
                geoOrigin = null;
            }
        }
    }

    private static void configureWebSettings(WebSettings settings) {
        settings.setJavaScriptEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        settings.setGeolocationEnabled(true);
        settings.setDatabaseEnabled(true);
    }

    private WebChromeClient webChromeClient = new WebChromeClient() {
        /* Called when the web page needs permissions to do something.
         * To process, request.grant() or request.deny() must be called.
         * Note this is distinct from the Android level permission, and that must be granted before
         * this can be granted.
         *
         * Note that for some reason the geolocation permission doesn't come through here, it has
         * its own handler (see the onGeolocationPermissionsShowPrompt override).
         */
        @Override
        public void onPermissionRequest(PermissionRequest request) {
            Log.i("Alamo", "onPermissionRequest called");
            final String[] requestedResources = request.getResources();
            for (String r : requestedResources) {
                if (r.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                    requestCameraPermissions(request);
                } else if (r.equals(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                    requestAudioPermissions(request);
                } else {
                    throw new RuntimeException(String.format("Request for unexpected permission: %s", r));
                }
            }
        }

        /* Per the docs at https://developer.android.com/reference/android/webkit/WebChromeClient
        Note that for applications targeting Android N and later SDKs (API level > Build.VERSION_CODES.M)
        this method is only called for requests originating from secure origins such as https. On
        non-secure origins geolocation requests are automatically denied.
        */
        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            requestGeoPermissions(callback, origin);
        }

        // TODO: Haven't wired this up or tested it yet.
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            return super.onShowFileChooser(webView, filePathCallback, fileChooserParams);
        }
    };

    private WebViewClient webViewClient = new WebViewClient() {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            // TODO: Just allow all navigtaion in place for now. Might want to filter those allowed by domain.
            return false;
        }
    };
}
