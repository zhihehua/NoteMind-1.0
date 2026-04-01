package com.example.NoteMind;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NoteListActivity extends AppCompatActivity implements SensorEventListener {
    private EditText etSearch;
    private WebView webViewTags;
    private QuestionDao dao;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastUpdate = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_list);

        dao = new QuestionDao(this);
        etSearch = findViewById(R.id.et_search);
        webViewTags = findViewById(R.id.webview_tags);

        // 传感器初始化
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        initWebView();
        findViewById(R.id.btn_search).setOnClickListener(v -> showSearchTypeDialog());
    }

    private void initWebView() {

        webViewTags.setBackgroundColor(0); // 关键：开启 WebView 背景透明支持
        webViewTags.getSettings().setJavaScriptEnabled(true);
        webViewTags.getSettings().setDomStorageEnabled(true);
        // 关闭硬件加速可能让 WebView 渲染 Canvas 更稳，但小米14性能强，建议开启

        webViewTags.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void openTagList(String tagName) {
                runOnUiThread(() -> {
                    Intent intent = new Intent(NoteListActivity.this, NoteListTwoActivity.class);
                    intent.putExtra("type", "tag");
                    intent.putExtra("keyword", tagName);
                    startActivity(intent);
                });
            }
        }, "AndroidBridge");

        webViewTags.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                refreshAtoms();
            }
        });
        webViewTags.loadUrl("file:///android_asset/tag_atoms.html");
    }

    private void refreshAtoms() {
        List<QuestionNote> all = dao.queryAll();
        Set<String> tagSet = new HashSet<>();
        for (QuestionNote n : all) if (n.getTag() != null) tagSet.add(n.getTag().trim());
        String json = new JSONArray(tagSet).toString();
        webViewTags.evaluateJavascript("renderTags('" + json + "')", null);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long curTime = System.currentTimeMillis();
        // 降低 JS 调用频率 (约 16ms 一次，对应 60fps)
        if ((curTime - lastUpdate) > 16) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // 将传感器坐标直接传递给 Matter.js
            webViewTags.post(() -> {
                webViewTags.evaluateJavascript("updateSensorData(" + x + "," + y + "," + z + ")", null);
            });
            lastUpdate = curTime;
        }
    }

    private void showSearchTypeDialog() {
        String text = etSearch.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "请输入搜索内容", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] types = {"按标题", "按标签", "按分类"};
        new MaterialAlertDialogBuilder(this).setTitle("搜索类型").setItems(types, (d, w) -> {
            Intent intent = new Intent(this, NoteListTwoActivity.class);
            intent.putExtra("keyword", text);
            intent.putExtra("type", w == 0 ? "question" : (w == 1 ? "tag" : "category"));
            startActivity(intent);
        }).show();
    }

    @Override protected void onResume() {
        super.onResume();
        if (accelerometer != null) sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    @Override protected void onDestroy() { super.onDestroy(); dao.close(); }
}