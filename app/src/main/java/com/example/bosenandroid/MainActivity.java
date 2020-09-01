package com.example.bosenandroid;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.honeywell.aidc.AidcManager;
import com.honeywell.aidc.BarcodeFailureEvent;
import com.honeywell.aidc.BarcodeReadEvent;
import com.honeywell.aidc.BarcodeReader;
import com.honeywell.aidc.TriggerStateChangeEvent;


import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.view.inputmethod.InputMethodManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity  implements BarcodeReader.TriggerListener, BarcodeReader.BarcodeListener{

    String barcodeData;
    //创建AidcManager和BarcodeReader对象
    AidcManager manager;
    BarcodeReader barcodeReader;

    public WebView webView;
    public boolean mLoadFinished = false;
    private SoundPool soundPool;
    private int sucessSoundId;
    private int failSoundId;
    private int noticeSoundId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        soundPool = new SoundPool(5, AudioManager.STREAM_ALARM, 5);
        sucessSoundId = soundPool.load(this, R.raw.sucess, Integer.MAX_VALUE);

        failSoundId = soundPool.load(this, R.raw.fail, Integer.MAX_VALUE);

        noticeSoundId  = soundPool.load(this, R.raw.notice, Integer.MAX_VALUE);

        webView = (WebView) findViewById(R.id.wv);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setDomStorageEnabled(true);



        webView.setWebViewClient(new myWebClient()); // 加入这个用于监听页面加载事件，因为页面需要完全加载完成才能接受js调用
        webView.setWebChromeClient(new WebChromeClient());
        //webView.addJavascriptInterface(this, "jswebview");
        // 传入的第二个参数angle，就是可已在js代码中直接使用的实例名称
        webView.addJavascriptInterface(new WebAppInterface(this), "makeJava");
        mLoadFinished = false;

        webView.loadUrl("file:///android_asset/login.html");


        /*findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.loadUrl("javascript:test()");
                System.out.println("*******************************************88okokokokokokokokok"+mLoadFinished);
            }
        });*/



        //第一步：获取扫描资源，设置扫描参数，开启扫描功能
        AidcManager.create(this, new AidcManager.CreatedCallback() {
            @Override
            public void onCreated(AidcManager aidcManager) {
                manager = aidcManager;
                barcodeReader = manager.createBarcodeReader();

                try {
                    //设置扫描参数
                    barcodeReader.setProperty(BarcodeReader.PROPERTY_CODE_128_ENABLED, true);    //开启Code128码制
                    barcodeReader.setProperty(BarcodeReader.PROPERTY_EAN_13_ENABLED, false);    //关闭EAN13码制
                    barcodeReader.setProperty(BarcodeReader.PROPERTY_TRIGGER_CONTROL_MODE,
                            BarcodeReader.TRIGGER_CONTROL_MODE_CLIENT_CONTROL);                    //设置手动触发模式

                    barcodeReader.claim();        //开启扫描功能
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "设置参数失败", Toast.LENGTH_SHORT).show();
                }

                //第二步：添加侧面扫描键事件监听和条码事件监听
                barcodeReader.addTriggerListener(MainActivity.this);
                barcodeReader.addBarcodeListener(MainActivity.this);
            }
        });

    }

    private class myWebClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            mLoadFinished = true;
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();//返回上个页面
            return true;
        }
        return super.onKeyDown(keyCode, event);//退出H5界面
    }

    //第三步：实现条码事件和侧面扫描键触发事件
    @Override
    public void onBarcodeEvent(BarcodeReadEvent barcodeReadEvent) {
        final String   barcodeData = barcodeReadEvent.getBarcodeData();    //获取扫描数据
        if(mLoadFinished){
            webView.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, barcodeData, Toast.LENGTH_SHORT).show();
                    webView.loadUrl("javascript:vue.addiReceivedQTY('"+barcodeData+"')");
                    webView.loadUrl("javascript:vue.test('"+barcodeData+"nide ')");
                }
            });
        }
    }
    @Override
    public void onFailureEvent(BarcodeFailureEvent barcodeFailureEvent) {

    }
    @Override
    public void onTriggerEvent(TriggerStateChangeEvent triggerStateChangeEvent) {
        if (triggerStateChangeEvent.getState() == true) {
            try {
                barcodeReader.light(true);        //开启补光
                barcodeReader.aim(true);        //开启瞄准线
                barcodeReader.decode(true);        //开启解码
            } catch (Exception e) {
                Toast.makeText(this, "开启扫描失败", Toast.LENGTH_SHORT).show();
            }
        } else if (triggerStateChangeEvent.getState() == false) {
            try {
                barcodeReader.light(false);        //关闭补光
                barcodeReader.aim(false);        //关闭瞄准线
                barcodeReader.decode(false);    //关闭解码
            } catch (Exception e) {
                Toast.makeText(this, "关闭扫描失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //第四步：资源的释放与恢复
    @Override
    protected void onResume() {
        super.onResume();
        if (barcodeReader != null) {
            try {
                barcodeReader.claim();        //开启扫描功能
            } catch (Exception e) {
                Toast.makeText(this, "开启扫描失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (barcodeReader != null) {
            barcodeReader.release();        //释放扫描资源
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (barcodeReader != null) {
            barcodeReader.removeBarcodeListener(this);
            barcodeReader.removeTriggerListener(this);
            barcodeReader.close();
        }

        if (manager != null) {
            manager.close();
        }
    }

    public void makeSuccessSound(){
        soundPool.play(sucessSoundId, 0.8f, 0.8f, 1, 0, 1);
    }
    public void makeFailSound(){
        soundPool.play(failSoundId, 0.8f, 0.8f, 1, 0, 1);
    }
    public void makeNoticeSound(){
        soundPool.play(noticeSoundId, 0.8f, 0.8f, 1, 0, 1);
    }


}
