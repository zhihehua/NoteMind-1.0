package com.example.NoteMind;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class SummaryActivity extends AppCompatActivity {
    private String targetTag;
    private QuestionDao dao;
    private TextView tvContent;

    // 复用 MainActivity 的配置
    private static final String API_KEY = "你的API_KEY";
    private static final String API_URL = "https://hello-occejwojlb.cn-hangzhou.fcapp.run";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        targetTag = getIntent().getStringExtra("TARGET_TAG");
        dao = new QuestionDao(this);
        tvContent = findViewById(R.id.tv_ai_content);

        TextView tvTitle = findViewById(R.id.tv_summary_title);
        tvTitle.setText(targetTag + " · 智核总结");

        startAiAnalysis();

        findViewById(R.id.btn_re_summary).setOnClickListener(v -> startAiAnalysis());
    }

    private void startAiAnalysis() {
        tvContent.setText("🔍 正在扫描数据库中关于 [" + targetTag + "] 的原子碎片...");

        new Thread(() -> {
            List<QuestionNote> notes = dao.queryByTag(targetTag);
            if (notes.isEmpty()) {
                runOnUiThread(() -> tvContent.setText("该领域暂无足够数据进行复盘。"));
                return;
            }

            // 核心：构建带数据的 Prompt
            StringBuilder sb = new StringBuilder();
            sb.append("请作为知识管家，总结我在【").append(targetTag).append("】领域的笔记。\n笔记列表：\n");
            for (QuestionNote n : notes) {
                sb.append("- ").append(n.getQuestion()).append(" (分类:").append(n.getCategory()).append(")\n");
            }
            sb.append("\n要求：总结知识架构、发现我的关注点、并给出下一步学习/生活建议,禁止Markdown符号,300字内。");

            sendRequestToAi(sb.toString());
        }).start();
    }

    private void sendRequestToAi(String prompt) {
        try {
            JSONObject json = new JSONObject();
            json.put("model", "doubao-seed-1-8-251228");
            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "system").put("content", "你是一个深刻的知识复盘助手。"));
            messages.put(new JSONObject().put("role", "user").put("content", prompt));
            json.put("messages", messages);

            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.toString().getBytes());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder resSb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) resSb.append(line);

            JSONObject resJson = new JSONObject(resSb.toString());
            String result = resJson.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");

            runOnUiThread(() -> tvContent.setText(result));

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> tvContent.setText("智核连接失败，请检查网络。"));
        }
    }
}