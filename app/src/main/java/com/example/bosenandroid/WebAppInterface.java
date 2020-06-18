package com.example.bosenandroid;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class WebAppInterface {

    Context mContext;

    /** Instantiate the interface and set the context */
    WebAppInterface(Context c) {
        mContext = c;
    }

    /** Show a toast from the web page */
    @JavascriptInterface
    public void showToast(String toast) {
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
    }

    @JavascriptInterface
    public void jumpHtml(final String url) {
        final MainActivity mainActivity=(MainActivity)mContext;
        mainActivity.mLoadFinished=false;

        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.webView.loadUrl(url);
            }
        });

    }

    @JavascriptInterface
    public void makeSuccessSound(){
        final MainActivity mainActivity=(MainActivity)mContext;
        mainActivity.makeSuccessSound();
    }

    @JavascriptInterface
    public void makeFailSound(){
        final MainActivity mainActivity=(MainActivity)mContext;
        mainActivity.makeFailSound();
    }
}
