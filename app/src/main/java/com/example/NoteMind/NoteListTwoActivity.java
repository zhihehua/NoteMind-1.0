package com.example.NoteMind;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.DashedLine;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NoteListTwoActivity extends AppCompatActivity {
    private LinearLayout container;
    private QuestionDao dao;
    private TextView tvEmpty, tvManage;
    private LinearLayout llBatchOp;

    private String currentType, currentKeyword;
    private boolean isManaging = false;
    private List<QuestionNote> displayedNotes = new ArrayList<>();
    private Set<Integer> selectedIds = new HashSet<>();

    private AlertDialog loadingDialog;
    private boolean isExporting = false;
    private final ExecutorService exportExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_list_two);

        container = findViewById(R.id.ll_note_list);
        tvEmpty = findViewById(R.id.tv_empty);
        tvManage = findViewById(R.id.tv_manage);
        llBatchOp = findViewById(R.id.ll_batch_op);
        dao = new QuestionDao(this);

        currentType = getIntent().getStringExtra("type");
        currentKeyword = getIntent().getStringExtra("keyword");

        tvManage.setOnClickListener(v -> {
            if (isExporting) return;
            toggleManageMode();
        });

        findViewById(R.id.btn_batch_delete).setOnClickListener(v -> {
            if (isExporting) return;
            performBatchDelete();
        });

        findViewById(R.id.btn_batch_export).setOnClickListener(v -> {
            if (isExporting) return;
            if (selectedIds.isEmpty()) {
                Toast.makeText(this, "请先勾选笔记", Toast.LENGTH_SHORT).show();
            } else {
                exportToPdf(new ArrayList<>(selectedIds));
            }
        });

        loadData();
    }

    private void showLoading() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null);
        ((TextView)v.findViewById(R.id.loading_text)).setText("正在生成 PDF...");
        loadingDialog = new MaterialAlertDialogBuilder(this)
                .setView(v)
                .setCancelable(false)
                .create();
        loadingDialog.show();
        if (loadingDialog.getWindow() != null) {
            loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void dismissLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    /**
     * 优化后的 PDF 导出逻辑：
     * 1. 包含图片导出
     * 2. 导出过程显示 Loading
     * 3. 进程锁定防止干扰
     */
    private void exportToPdf(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;
        isExporting = true;
        showLoading();

        exportExecutor.execute(() -> {
            String fileName = "NoteMind_Full_" + System.currentTimeMillis() + ".pdf";
            Uri uri = null;
            boolean success = false;
            String errorMsg = "";

            try {
                OutputStream outputStream;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                    uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (uri == null) throw new Exception("无法创建文件");
                    outputStream = getContentResolver().openOutputStream(uri);
                } else {
                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
                    outputStream = new java.io.FileOutputStream(file);
                }

                if (outputStream == null) throw new Exception("无法打开输出流");

                PdfWriter writer = new PdfWriter(outputStream);
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf);
                document.setMargins(40, 40, 40, 40);

                // 字体加载
                com.itextpdf.kernel.font.PdfFont chineseFont = null;
                String[] fontPaths = {"/system/fonts/NotoSansSC-Regular.otf", "/system/fonts/NotoSansCJK-Regular.ttc,0", "/system/fonts/DroidSansFallback.ttf"};
                for (String path : fontPaths) {
                    try {
                        String actualPath = path.contains(",") ? path.split(",")[0] : path;
                        if (new File(actualPath).exists()) {
                            chineseFont = com.itextpdf.kernel.font.PdfFontFactory.createFont(path, com.itextpdf.io.font.PdfEncodings.IDENTITY_H, com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
                            break;
                        }
                    } catch (Exception ignored) {}
                }
                if (chineseFont != null) document.setFont(chineseFont);

                // 标题
                document.add(new Paragraph("NoteMind 完整数据备份")
                        .setFontSize(22).setFontColor(ColorConstants.DARK_GRAY).setTextAlignment(TextAlignment.CENTER));
                document.add(new Paragraph("导出时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()))
                        .setFontSize(10).setTextAlignment(TextAlignment.RIGHT).setMarginBottom(10));
                document.add(new LineSeparator(new SolidLine(1f)).setMarginBottom(20));

                for (QuestionNote note : displayedNotes) {
                    if (ids.contains(note.getId())) {
                        // 1. 标题 (Question)
                        document.add(new Paragraph("【标题】 " + safeText(note.getQuestion())).setFontSize(14).setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY).setPadding(5));
                        
                        // 2. 分类与标签
                        document.add(new Paragraph("分类: " + safeText(note.getCategory()) + "  |  标签: " + safeText(note.getTag())).setFontSize(10).setFontColor(ColorConstants.GRAY));

                        // 3. 内容 (Answer)
                        document.add(new Paragraph("解答内容：\n" + safeText(note.getAnswer())).setFontSize(12).setMultipliedLeading(1.5f).setMarginTop(10));

                        // 4. 用户笔记
                        if (note.getUserNote() != null && !note.getUserNote().trim().isEmpty()) {
                            document.add(new Paragraph("用户笔记：\n" + safeText(note.getUserNote())).setFontSize(11).setFontColor(ColorConstants.DARK_GRAY).setItalic());
                        }

                        // 5. 图片导出
                        String paths = note.getPhotoPaths();
                        if (paths != null && !paths.isEmpty()) {
                            document.add(new Paragraph("附图：").setFontSize(11).setMarginTop(10));
                            String[] pathArr = paths.split(";");
                            for (String p : pathArr) {
                                File imgFile = new File(p);
                                if (imgFile.exists()) {
                                    try {
                                        // 压缩图片防止 PDF 过大或内存溢出
                                        BitmapFactory.Options options = new BitmapFactory.Options();
                                        options.inSampleSize = 2; 
                                        Bitmap bmp = BitmapFactory.decodeFile(p, options);
                                        if (bmp != null) {
                                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                            bmp.compress(Bitmap.CompressFormat.JPEG, 70, stream);
                                            ImageData imageData = ImageDataFactory.create(stream.toByteArray());
                                            Image pdfImg = new Image(imageData);
                                            pdfImg.setMaxWidth(400); // 限制图片宽度
                                            pdfImg.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
                                            document.add(pdfImg);
                                            bmp.recycle();
                                        }
                                    } catch (Exception imgEx) { imgEx.printStackTrace(); }
                                }
                            }
                        }

                        document.add(new Paragraph("\n"));
                        document.add(new LineSeparator(new DashedLine(0.5f)).setMarginBottom(20));
                    }
                }

                document.close();
                outputStream.close();
                success = true;

                if (uri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    scanIntent.setData(uri);
                    sendBroadcast(scanIntent);
                }

            } catch (Exception e) {
                e.printStackTrace();
                errorMsg = e.getLocalizedMessage();
            }

            final boolean finalSuccess = success;
            final String finalError = errorMsg;
            new Handler(Looper.getMainLooper()).post(() -> {
                dismissLoading();
                isExporting = false;
                if (finalSuccess) {
                    Toast.makeText(NoteListTwoActivity.this, "导出成功！已保存至 Downloads 文件夹", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(NoteListTwoActivity.this, "导出失败: " + finalError, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private String safeText(String input) {
        if (input == null) return "";
        return input.replaceAll("[^\\u4e00-\\u9fa5\\uFF00-\\uFFEF\\u0000-\\u007F\\s]", "").replace("\r", "");
    }

    private void loadData() {
        List<QuestionNote> all = dao.queryAll();
        displayedNotes.clear();
        if (currentType == null || currentKeyword == null) {
            displayedNotes.addAll(all);
        } else {
            for (QuestionNote n : all) {
                if ("question".equals(currentType) && n.getQuestion().contains(currentKeyword)) {
                    displayedNotes.add(n);
                } else if ("tag".equals(currentType) && n.getTag() != null && n.getTag().contains(currentKeyword)) {
                    displayedNotes.add(n);
                } else if ("category".equals(currentType) && n.getCategory() != null && n.getCategory().contains(currentKeyword)) {
                    displayedNotes.add(n);
                }
            }
        }
        renderList();
    }

    private void renderList() {
        container.removeAllViews();
        tvEmpty.setVisibility(displayedNotes.isEmpty() ? View.VISIBLE : View.GONE);
        tvManage.setVisibility(displayedNotes.isEmpty() ? View.GONE : View.VISIBLE);

        for (QuestionNote note : displayedNotes) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(-1, -2);
            itemParams.setMargins(0, 10, 0, 10);
            item.setLayoutParams(itemParams);

            CheckBox cb = new CheckBox(this);
            cb.setVisibility(isManaging ? View.VISIBLE : View.GONE);
            cb.setChecked(selectedIds.contains(note.getId()));
            cb.setOnCheckedChangeListener((b, checked) -> {
                if (checked) selectedIds.add(note.getId());
                else selectedIds.remove(note.getId());
            });

            TextView tv = new TextView(this);
            tv.setText(note.getQuestion());
            tv.setTextSize(16);
            tv.setBackgroundResource(R.drawable.dialog_bg);
            LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
            tvParams.setMargins(10, 0, 10, 0);
            tv.setLayoutParams(tvParams);
            tv.setPadding(40, 30, 40, 30);

            tv.setOnClickListener(v -> {
                if (isExporting) return;
                if (isManaging) cb.setChecked(!cb.isChecked());
                else startActivity(new Intent(this, DetailActivity.class).putExtra("id", note.getId()));
            });

            item.addView(cb);
            item.addView(tv);
            container.addView(item);
        }
    }

    private void toggleManageMode() {
        isManaging = !isManaging;
        tvManage.setText(isManaging ? "取消" : "管理");
        llBatchOp.setVisibility(isManaging ? View.VISIBLE : View.GONE);
        selectedIds.clear();
        renderList();
    }

    private void performBatchDelete() {
        if (selectedIds.isEmpty()) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle("确认删除")
                .setPositiveButton("确定", (d, w) -> {
                    for (Integer id : selectedIds) dao.deleteById(id);
                    isManaging = false;
                    loadData();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        dao.close();
        exportExecutor.shutdown();
    }
}