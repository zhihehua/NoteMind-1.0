package com.example.NoteMind;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NoteMindAgent {
    private static final String TAG = "NoteMindAgent";
    private static final String API_KEY = "API_KEY";
    private static final String API_URL = "https://hello-occejwojlb.cn-hangzhou.fcapp.run";
    private static final String MODEL = "doubao-seed-1-8-251228";

    public enum NoteMindIntent { SUMMARIZE, MIND_MAP, UNKNOWN }

    public interface AgentCallback {
        void onStart();
        void onIntentDetected(NoteMindIntent intent);
        void onProgress(String chunk);
        void onSuccess(String finalResult);
        void onError(String error);
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void requestAiText(String text, AgentCallback callback) {
        new Thread(() -> {
            try {
                mainHandler.post(callback::onStart);
                JSONObject json = new JSONObject();
                json.put("model", MODEL);
                json.put("stream", true); // 开启流式
                
                JSONArray messages = new JSONArray();
                messages.put(new JSONObject().put("role", "system")
                        .put("content", getSystemPrompt()));
                messages.put(new JSONObject().put("role", "user").put("content", text));
                
                json.put("messages", messages);
                callApiStreaming(json.toString(), callback);
            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
        }).start();
    }

    public void requestAiOcr(Bitmap bitmap, String tag, AgentCallback callback) {
        new Thread(() -> {
            try {
                mainHandler.post(callback::onStart);
                String base64 = bitmapToSmallBase64(bitmap);
                
                JSONObject json = new JSONObject();
                json.put("model", MODEL);
                json.put("stream", true);

                JSONArray messages = new JSONArray();
                messages.put(new JSONObject().put("role", "system").put("content", getSystemPrompt()));
                
                JSONObject msg = new JSONObject();
                msg.put("role", "user");
                JSONArray contentArr = new JSONArray();
                
                JSONObject textObj = new JSONObject();
                textObj.put("type", "text");
                textObj.put("text", tag.isEmpty() ? "识别图片中的内容并根据其性质选择意图处理。" :
                        "【场景：" + tag + "】分析图片内容并处理。");
                contentArr.put(textObj);
                
                JSONObject imgObj = new JSONObject();
                imgObj.put("type", "image_url");
                imgObj.put("image_url", new JSONObject().put("url", "data:image/jpeg;base64," + base64));
                contentArr.put(imgObj);
                
                msg.put("content", contentArr);
                messages.put(msg);
                json.put("messages", messages);
                
                callApiStreaming(json.toString(), callback);
            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
        }).start();
    }

    private String getSystemPrompt() {
        return "你是一个具备意图识别能力的知识助手。请严格按照以下规则响应：\n" +
                "1. 首先判断用户意图：\n" +
                "   - 如果用户想了解知识体系、思维导图、逻辑结构或列出大纲，识别为 [MIND_MAP]。\n" +
                "   - 如果是普通问答、总结摘要、题目解答或闲聊，识别为 [SUMMARIZE]。\n" +
                "2. 输出规范：\n" +
                "   - 第一行必须且只能输出意图标识符，例如：INTENT:MIND_MAP 或 INTENT:SUMMARIZE。\n" +
                "   - 如果意图是 MIND_MAP，请输出符合 ECharts 树图格式的 JSON 字符串（包含 nodes 和 links 或符合树形结构的 data 数组）。\n" +
                "   - 如果意图是 SUMMARIZE，请严格按以下格式输出，不要包含多余文字：\n" +
                "     题目：[在此处提取或总结问题]\n" +
                "     解答：[在此处给出详细解答]\n" +
                "     标签：[学科分类，如：高数、英语、物理]\n" +
                "     分类：[该学科下的知识点分支，如：不定积分、从句、动量守恒]\n" +
                "3. 符号与公式约束（重要）：\n" +
                "   - 严禁使用 LaTeX 格式输出数学公式（如 \\\\frac, \\\\sqrt, \\\\int 等）。\n" +
                "   - 必须使用纯文本或常用字符表达数学含义。例如：用 'x^2' 代替平方，用 '根号' 或 'sqrt()' 代替二次方根，用 '/' 代替分数线，用 '积分' 代替积分符号。\n" +
                "   - 确保所有输出在普通文本框中清晰易读，不产生任何无法识别的专业公式符号。\n" +
                "4. 严禁输出 Markdown 代码块标识符（如 ```json）。";
    }

    private void callApiStreaming(String body, AgentCallback callback) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setDoOutput(true);
            conn.setChunkedStreamingMode(0); // 防止缓存流

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            StringBuilder fullResponse = new StringBuilder();
            NoteMindIntent detectedIntent = NoteMindIntent.UNKNOWN;
            boolean intentSent = false;

            while ((line = br.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6).trim();
                    if (data.equals("[DONE]")) break;

                    try {
                        JSONObject chunkJson = new JSONObject(data);
                        JSONArray choices = chunkJson.optJSONArray("choices");
                        if (choices != null && choices.length() > 0) {
                            JSONObject delta = choices.getJSONObject(0).optJSONObject("delta");
                            if (delta != null && delta.has("content")) {
                                String content = delta.getString("content");
                                fullResponse.append(content);

                                // 意图识别逻辑
                                if (!intentSent && fullResponse.toString().contains("INTENT:")) {
                                    if (fullResponse.toString().contains("MIND_MAP")) {
                                        detectedIntent = NoteMindIntent.MIND_MAP;
                                        intentSent = true;
                                        final NoteMindIntent fIntent = detectedIntent;
                                        mainHandler.post(() -> callback.onIntentDetected(fIntent));
                                    } else if (fullResponse.toString().contains("SUMMARIZE")) {
                                        detectedIntent = NoteMindIntent.SUMMARIZE;
                                        intentSent = true;
                                        final NoteMindIntent fIntent = detectedIntent;
                                        mainHandler.post(() -> callback.onIntentDetected(fIntent));
                                    }
                                }

                                mainHandler.post(() -> callback.onProgress(content));
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing chunk: " + line);
                    }
                }
            }

            String finalCleaned = cleanOutput(fullResponse.toString());
            mainHandler.post(() -> callback.onSuccess(finalCleaned));

        } catch (Exception e) {
            postError(callback, e.getMessage());
        }
    }

    private String cleanOutput(String raw) {
        // 移除意图行
        String cleaned = raw.replaceAll("INTENT:.*\\n?", "").trim();
        // 移除 Markdown 代码块
        cleaned = cleaned.replaceAll("```[a-zA-Z]*\\n?", "").replace("```", "").trim();
        return cleaned;
    }

    private String bitmapToSmallBase64(Bitmap bitmap) {
        Bitmap small = Bitmap.createScaledBitmap(bitmap, 640, 480, true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        small.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
    }

    private void postError(AgentCallback callback, String error) {
        mainHandler.post(() -> callback.onError(error));
    }
}
