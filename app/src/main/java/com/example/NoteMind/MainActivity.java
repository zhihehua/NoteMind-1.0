package com.example.NoteMind;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    // 标签页容器
    private View layoutHome, layoutAtom, layoutMy;
    // 导航按钮
    private ImageButton navHome, navAtom, navMy;

    private ChipGroup chipGroupTags;
    private String currentSelectedTag = "";
    private HorizontalScrollView tagScroll;

    private CameraUtils cameraUtils;
    private QuestionDao questionDao;
    private NoteMindAgent agent;

    private AlertDialog loadingDialog;
    private static final int ALL_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        questionDao = new QuestionDao(this);
        cameraUtils = new CameraUtils(this);
        agent = new NoteMindAgent();

        // --- 1. 绑定 ID 与切换逻辑 ---
        layoutHome = findViewById(R.id.layout_tab_home);
        layoutAtom = findViewById(R.id.layout_tab_atom);
        layoutMy = findViewById(R.id.layout_tab_my);

        navHome = findViewById(R.id.nav_btn_home);
        navAtom = findViewById(R.id.nav_btn_atom);
        navMy = findViewById(R.id.nav_btn_my);

        navHome.setOnClickListener(v -> switchTab(0));
        navAtom.setOnClickListener(v -> switchTab(1));
        navMy.setOnClickListener(v -> switchTab(2));

        // --- 2. 首页录入功能绑定 ---
        findViewById(R.id.btn_input_text_container).setOnClickListener(v -> v.postDelayed(this::showInputTextDialog, 50));
        findViewById(R.id.btn_take_photo_container).setOnClickListener(v -> v.postDelayed(cameraUtils::openCamera, 50));
        findViewById(R.id.btn_gallery_container).setOnClickListener(v -> v.postDelayed(cameraUtils::openGallery, 50));

        // --- 3. 原子图谱功能绑定 ---
        findViewById(R.id.btn_knowledge_atom_tab).setOnClickListener(v -> v.postDelayed(this::showAtomTagDialog, 50));

        // --- 4. 个人中心跳转绑定 ---
        findViewById(R.id.btn_show_list_tab).setOnClickListener(v -> v.postDelayed(() -> {
            startActivity(new Intent(this, NoteListActivity.class));
        }, 50));

        // 原有初始化
        chipGroupTags = findViewById(R.id.chip_group_tags);
        tagScroll = findViewById(R.id.tag_scroll);

        checkPermissions();
        refreshTagChips();
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

    private NoteMindAgent.AgentCallback createAgentCallback() {
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
            public void onProgress(String chunk) {
                // 流式进度处理
            }

            @Override
            public void onSuccess(String finalResult) {
                if (loadingDialog != null) loadingDialog.dismiss();
                
                if (currentIntent == NoteMindAgent.NoteMindIntent.MIND_MAP) {
                    Intent intent = new Intent(MainActivity.this, AtomGraphActivity.class);
                    intent.putExtra("TARGET_TAG", currentSelectedTag);
                    intent.putExtra("RAW_JSON", finalResult);
                    startActivity(intent);
                } else {
                    // 解析字段：题目、解答、标签、分类
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
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    Intent intent = new Intent(MainActivity.this, ConfirmActivity.class);
                    intent.putExtra("question", q);
                    intent.putExtra("answer", a);
                    intent.putExtra("tag", t);
                    intent.putExtra("category", c);
                    startActivity(intent);
                }
            }

            @Override
            public void onError(String error) {
                handleError(error);
            }
        };
    }

    private void switchTab(int index) {
        layoutHome.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        layoutAtom.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        layoutMy.setVisibility(index == 2 ? View.VISIBLE : View.GONE);

        navHome.setAlpha(index == 0 ? 1.0f : 0.5f);
        navAtom.setAlpha(index == 1 ? 1.0f : 0.5f);
        navMy.setAlpha(index == 2 ? 1.0f : 0.5f);
    }

    private void refreshTagChips() {
        if (chipGroupTags == null) return;
        chipGroupTags.removeAllViews();

        Chip addBtn = new Chip(this);
        addBtn.setText("新标签");
        addBtn.setTextSize(15f);
        addBtn.setChipIcon(ContextCompat.getDrawable(this, android.R.drawable.ic_input_add));
        addBtn.setCheckable(false);
        addBtn.setOnClickListener(v -> showNewTagInput());
        chipGroupTags.addView(addBtn);

        List<String> tags = questionDao.getAllUniqueTags();
        for (String tag : tags) {
            Chip chip = new Chip(this);
            chip.setText(tag);
            chip.setTextSize(15f);
            chip.setCheckable(true);
            if (tag.equals(currentSelectedTag)) {
                chip.setChecked(true);
            }
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) currentSelectedTag = tag;
                else if (currentSelectedTag.equals(tag)) currentSelectedTag = "";
            });
            chipGroupTags.addView(chip);
        }
    }

    private void showNewTagInput() {
        View v = LayoutInflater.from(this).inflate(R.layout.layout_custom_dialog, null);
        TextView tvTitle = v.findViewById(R.id.dialog_title);
        EditText etInput = v.findViewById(R.id.dialog_input);
        Button btnConfirm = v.findViewById(R.id.dialog_btn_confirm);

        tvTitle.setText("定义新场景: (如：高数、菜谱)");
        etInput.setHint("");
        etInput.setBackgroundResource(R.drawable.edittext_bg);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(v)
                .setCancelable(true)
                .create();

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnConfirm.setOnClickListener(view -> {
            String val = etInput.getText().toString().trim();
            if(!val.isEmpty()) {
                currentSelectedTag = val;
                refreshTagChips();
                dialog.dismiss();
            }
        });
    }

    private void showAtomTagDialog() {
        List<String> tags = questionDao.getAllUniqueTags();
        if (tags.isEmpty()) {
            Toast.makeText(this, "暂无数据", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] tagArray = tags.toArray(new String[0]);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        ListView listView = new ListView(this);
        listView.setAdapter(new android.widget.ArrayAdapter<>(this, android.R.layout.simple_list_item_1, tagArray));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int)(400 * getResources().getDisplayMetrics().density));
        listView.setLayoutParams(params);
        layout.addView(listView);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("选择要探索的原子领域")
                .setView(layout)
                .setNegativeButton("取消", null)
                .create();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(this, AtomGraphActivity.class);
            intent.putExtra("TARGET_TAG", tagArray[position]);
            startActivity(intent);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showInputTextDialog() {
        View v = LayoutInflater.from(this).inflate(R.layout.layout_custom_dialog, null);
        TextView tvTitle = v.findViewById(R.id.dialog_title);
        EditText etInput = v.findViewById(R.id.dialog_input);
        Button btnConfirm = v.findViewById(R.id.dialog_btn_confirm);

        String label = "智能问答: (请在此输入问题)";
        if(!currentSelectedTag.isEmpty()) {
            label = "智能问答: (针对 [" + currentSelectedTag + "] 提问)";
        }
        tvTitle.setText(label);
        etInput.setHint("");
        etInput.setBackgroundResource(R.drawable.edittext_bg);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(v)
                .setCancelable(true)
                .create();

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnConfirm.setOnClickListener(view -> {
            String text = etInput.getText().toString().trim();
            if (!text.isEmpty()) {
                dialog.dismiss();
                String wrappedPrompt = currentSelectedTag.isEmpty() ? text :
                        "【场景标签：" + currentSelectedTag + "】用户问题：" + text;
                agent.requestAiText(wrappedPrompt, createAgentCallback());
            }
        });
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

    @Override protected void onResume() { super.onResume(); refreshTagChips(); }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        cameraUtils.onActivityResult(requestCode, resultCode, data);
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        if (questionDao != null) questionDao.close();
    }
}
