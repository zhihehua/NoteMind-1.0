package com.example.NoteMind;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private ImageButton navHome, navAtom, navMy;

    private CameraUtils cameraUtils;
    private QuestionDao questionDao;
    private NoteMindAgent agent;

    private String currentSelectedTag = "";
    private AlertDialog loadingDialog;
    private static final int ALL_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        questionDao = new QuestionDao(this);
        cameraUtils = new CameraUtils(this);
        agent = new NoteMindAgent();

        // --- 1. 初始化 ViewPager2 ---
        viewPager = findViewById(R.id.main_view_pager);
        MainFragmentAdapter adapter = new MainFragmentAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(false); // 禁用滑动切换，改用按钮

        // --- 2. 绑定导航按钮 ---
        navHome = findViewById(R.id.nav_btn_home);
        navAtom = findViewById(R.id.nav_btn_atom);
        navMy = findViewById(R.id.nav_btn_my);

        navHome.setOnClickListener(v -> switchTab(0));
        navAtom.setOnClickListener(v -> switchTab(1));
        navMy.setOnClickListener(v -> switchTab(2));

        checkPermissions();
        switchTab(0); // 默认首页

        // 拍照与相册回调
        cameraUtils.setOnCameraResultListener(new CameraUtils.OnCameraResultListener() {
            @Override
            public void onCameraSuccess(Bitmap bitmap) {
                agent.requestAiOcr(bitmap, currentSelectedTag, createAgentCallback());
            }
            @Override
            public void onGallerySuccess(Bitmap bitmap) {
                agent.requestAiOcr(bitmap, currentSelectedTag, createAgentCallback());
            }
            @Override
            public void onFail(String msg) {
                Toast.makeText(MainActivity.this, "操作失败: " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 供 Fragment 调用的 Getter
    public QuestionDao getQuestionDao() { return questionDao; }
    public NoteMindAgent getAgent() { return agent; }
    public CameraUtils getCameraUtils() { return cameraUtils; }
    public void setCurrentSelectedTag(String tag) { this.currentSelectedTag = tag; }

    public NoteMindAgent.AgentCallback createAgentCallback() {
        return new NoteMindAgent.AgentCallback() {
            private NoteMindAgent.NoteMindIntent currentIntent = NoteMindAgent.NoteMindIntent.UNKNOWN;

            @Override
            public void onStart() {
                showLoading();
            }

            @Override
            public void onIntentDetected(NoteMindAgent.NoteMindIntent intent) {
                currentIntent = intent;
            }

            @Override
            public void onProgress(String chunk) { }

            @Override
            public void onSuccess(String finalResult) {
                if (loadingDialog != null) loadingDialog.dismiss();
                
                if (currentIntent == NoteMindAgent.NoteMindIntent.MIND_MAP) {
                    Intent intent = new Intent(MainActivity.this, AtomGraphActivity.class);
                    intent.putExtra("TARGET_TAG", currentSelectedTag);
                    intent.putExtra("RAW_JSON", finalResult);
                    startActivity(intent);
                } else {
                    parseAndNavigateToConfirm(finalResult);
                }
            }

            @Override
            public void onError(String error) {
                handleError(error);
            }
        };
    }

    private void parseAndNavigateToConfirm(String finalResult) {
        String q = "智能识别", a = finalResult, t = currentSelectedTag, c = "";
        try {
            String[] lines = finalResult.split("\n");
            StringBuilder answerBuilder = new StringBuilder();
            boolean inAnswer = false;
            for (String line : lines) {
                if (line.startsWith("题目：")) {
                    q = line.replace("题目：", "").trim();
                    inAnswer = false;
                } else if (line.startsWith("解答：")) {
                    answerBuilder.append(line.replace("解答：", "").trim()).append("\n");
                    inAnswer = true;
                } else if (line.startsWith("标签：")) {
                    t = line.replace("标签：", "").trim();
                    inAnswer = false;
                } else if (line.startsWith("分类：")) {
                    c = line.replace("分类：", "").trim();
                    inAnswer = false;
                } else if (inAnswer) {
                    answerBuilder.append(line).append("\n");
                }
            }
            if (answerBuilder.length() > 0) a = answerBuilder.toString().trim();
        } catch (Exception e) { e.printStackTrace(); }

        Intent intent = new Intent(this, ConfirmActivity.class);
        intent.putExtra("question", q);
        intent.putExtra("answer", a);
        intent.putExtra("tag", t);
        intent.putExtra("category", c);
        startActivity(intent);
    }

    private void switchTab(int index) {
        viewPager.setCurrentItem(index, false);
        navHome.setAlpha(index == 0 ? 1.0f : 0.5f);
        navAtom.setAlpha(index == 1 ? 1.0f : 0.5f);
        navMy.setAlpha(index == 2 ? 1.0f : 0.5f);
    }

    private void handleError(String msg) {
        runOnUiThread(() -> {
            if (loadingDialog != null) loadingDialog.dismiss();
            Toast.makeText(this, "识别出错: " + msg, Toast.LENGTH_LONG).show();
        });
    }

    private void checkPermissions() {
        List<String> list = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            list.add(Manifest.permission.CAMERA);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                list.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                list.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
        if (!list.isEmpty()) {
            ActivityCompat.requestPermissions(this, list.toArray(new String[0]), ALL_PERMISSION_CODE);
        }
    }

    private void showLoading() {
        runOnUiThread(() -> {
            View v = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null);
            loadingDialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setView(v)
                    .setCancelable(false)
                    .create();
            loadingDialog.show();
            if (loadingDialog.getWindow() != null) {
                loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        });
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        cameraUtils.onActivityResult(requestCode, resultCode, data);
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        if (questionDao != null) questionDao.close();
    }
}
