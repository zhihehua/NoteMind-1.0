package com.example.NoteMind;

import android.content.Intent;
import android.os.Build; // 新增
import android.os.Bundle;
import android.view.View; // 新增
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AtomGraphActivity extends AppCompatActivity {
    private WebView webView;
    private QuestionDao dao;
    private String targetTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_atom_graph);

        // --- 核心修复 1：在 Activity 级别开启硬件加速 ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getWindow().setFlags(
                    android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            );
        }

        targetTag = getIntent().getStringExtra("TARGET_TAG");
        if (targetTag == null) targetTag = "未知标签";

        TextView tvTitle = findViewById(R.id.tv_graph_title);
        tvTitle.setText(targetTag + " · 原子分布");

        webView = findViewById(R.id.webview_atom);

        // --- 核心修复 2：在 WebView 级别二次强化硬件加速 ---
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        dao = new QuestionDao(this);

        initGraph();
        findViewById(R.id.btn_graph_back).setOnClickListener(v -> finish());
    }

    private void initGraph() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setDomStorageEnabled(true);
        // 提高渲染优先级
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);

        webView.addJavascriptInterface(new WebAppInterface(), "AndroidBridge");
        webView.loadUrl("file:///android_asset/atom_engine.html");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // --- 核心修复 3：增加延迟注入，确保 HTML 里的 JS 函数已声明 ---
                webView.postDelayed(() -> injectDataToGraph(), 300);
            }
        });
    }

    private void injectDataToGraph() {
        try {
            JSONObject data = new JSONObject();
            JSONArray nodes = new JSONArray();
            JSONArray links = new JSONArray();

            // 用于去重，防止节点重复添加
            java.util.Set<String> addedNodeNames = new java.util.HashSet<>();

            // 1. 中心节点 (Root: e.g., "高数")
            JSONObject root = new JSONObject();
            root.put("name", targetTag);
            root.put("value", 70); // 大尺寸
            root.put("category", 0);
            nodes.put(root);
            addedNodeNames.add(targetTag);

            // 2. 获取第一层：属于 targetTag 的所有 Category
            List<QuestionNote> level1Notes = dao.queryByTag(targetTag);
            java.util.Set<String> level1Categories = new java.util.HashSet<>();
            for (QuestionNote note : level1Notes) {
                String cat = note.getCategory();
                if (cat != null && !cat.trim().isEmpty()) {
                    level1Categories.add(cat.trim());
                }
            }

            // 3. 遍历第一层分类
            for (String cat1 : level1Categories) {
                // 添加第一层节点 (e.g., "积分")
                if (!addedNodeNames.contains(cat1)) {
                    JSONObject node1 = new JSONObject();
                    node1.put("name", cat1);
                    node1.put("value", 45);
                    node1.put("category", 1);
                    nodes.put(node1);
                    addedNodeNames.add(cat1);
                }

                // 建立连接：Root -> Level1
                JSONObject link1 = new JSONObject();
                link1.put("source", targetTag);
                link1.put("target", cat1);
                links.put(link1);

                // --- 核心逻辑扩展：检查 cat1 是否也作为 Tag 存在 (第二层) ---
                List<QuestionNote> level2Notes = dao.queryByTag(cat1);
                if (!level2Notes.isEmpty()) {
                    java.util.Set<String> level2SubCategories = new java.util.HashSet<>();
                    for (QuestionNote n2 : level2Notes) {
                        String cat2 = n2.getCategory();
                        if (cat2 != null && !cat2.trim().isEmpty() && !cat2.equals(cat1)) {
                            level2SubCategories.add(cat2.trim());
                        }
                    }

                    // 添加第二层节点 (e.g., "微分", "定积分")
                    for (String cat2 : level2SubCategories) {
                        if (!addedNodeNames.contains(cat2)) {
                            JSONObject node2 = new JSONObject();
                            node2.put("name", cat2);
                            node2.put("value", 25); // 较小尺寸
                            node2.put("category", 2); // 新的分类，用于变色
                            nodes.put(node2);
                            addedNodeNames.add(cat2);
                        }

                        // 建立连接：Level1 -> Level2
                        JSONObject link2 = new JSONObject();
                        link2.put("source", cat1);
                        link2.put("target", cat2);
                        links.put(link2);
                    }
                }
            }

            data.put("nodes", nodes);
            data.put("links", links);

            final String jsonData = data.toString().replace("\\", "\\\\").replace("'", "\\'");
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    webView.evaluateJavascript("renderGraph('" + jsonData + "')", null);
                } else {
                    webView.loadUrl("javascript:renderGraph('" + jsonData + "')");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class WebAppInterface {
        @android.webkit.JavascriptInterface
        public void openCategoryList(String categoryName) {
            runOnUiThread(() -> {
                Intent intent = new Intent(AtomGraphActivity.this, NoteListTwoActivity.class);
                intent.putExtra("type", "category");
                intent.putExtra("keyword", categoryName);
                startActivity(intent);
            });
        }

        @android.webkit.JavascriptInterface
        public void startAiReview() {
            runOnUiThread(() -> {
                Intent intent = new Intent(AtomGraphActivity.this, SummaryActivity.class);
                intent.putExtra("TARGET_TAG", targetTag);
                startActivity(intent);
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dao != null) dao.close();
    }
}