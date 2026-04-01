package com.example.NoteMind;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DetailActivity extends AppCompatActivity {

    private EditText et_question, et_answer, et_tag, et_category, et_user_note;
    private GridLayout gl_photos;
    private ImageButton btn_photo_note, btn_gallery_note;
    private TextView btnAddTag, btnAddCategory;
    private QuestionDao dao;
    private int currentId = -1;
    private ArrayList<String> photoPathList = new ArrayList<>();
    private String currentPhotoPath;

    private static final int REQUEST_CAMERA = 100;
    private static final int REQUEST_GALLERY = 101;
    private static final int PERMISSION_REQUEST_CODE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        if (savedInstanceState != null) {
            currentPhotoPath = savedInstanceState.getString("temp_path");
        }

        et_question = findViewById(R.id.et_question);
        et_answer = findViewById(R.id.et_answer);
        et_tag = findViewById(R.id.et_tag);
        et_category = findViewById(R.id.et_category);
        et_user_note = findViewById(R.id.et_user_note);
        gl_photos = findViewById(R.id.gl_photos);

        btn_photo_note = findViewById(R.id.btn_photo_note);
        btn_gallery_note = findViewById(R.id.btn_gallery_note);
        btnAddTag = findViewById(R.id.btn_add_tag_detail);
        btnAddCategory = findViewById(R.id.btn_add_category_detail);

        dao = new QuestionDao(this);

        btn_photo_note.setOnClickListener(v -> takePhoto());
        btn_gallery_note.setOnClickListener(v -> openGallery());
        
        btnAddTag.setOnClickListener(v -> showExistingTags());
        btnAddCategory.setOnClickListener(v -> showExistingCategories());

        findViewById(R.id.btn_update).setOnClickListener(v -> updateNote());
        findViewById(R.id.btn_delete).setOnClickListener(v -> deleteNote());

        loadData();
    }

    private void showExistingTags() {
        List<String> tags = dao.getAllUniqueTags();
        if (tags.isEmpty()) {
            Toast.makeText(this, "暂无已存标签", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] tagArray = tags.toArray(new String[0]);
        new MaterialAlertDialogBuilder(this)
                .setTitle("选择已有标签")
                .setItems(tagArray, (dialog, which) -> et_tag.setText(tagArray[which]))
                .show();
    }

    private void showExistingCategories() {
        List<String> categories = dao.getAllUniqueCategories();
        if (categories.isEmpty()) {
            Toast.makeText(this, "暂无已存分类", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] catArray = categories.toArray(new String[0]);
        new MaterialAlertDialogBuilder(this)
                .setTitle("选择已有分类")
                .setItems(catArray, (dialog, which) -> et_category.setText(catArray[which]))
                .show();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("temp_path", currentPhotoPath);
    }

    private void takePhoto() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
            return;
        }

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "文件创建失败", Toast.LENGTH_SHORT).show();
            }

            if (photoFile != null) {
                Uri photoUri = FileProvider.getUriForFile(this,
                        "com.example.NoteMind.fileprovider", photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(cameraIntent, REQUEST_CAMERA);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CAMERA) {
                if (currentPhotoPath != null) {
                    photoPathList.add(currentPhotoPath);
                    addImageToView(currentPhotoPath);
                }
            } else if (requestCode == REQUEST_GALLERY && data != null) {
                Uri uri = data.getData();
                String path = getImagePathFromUri(uri);
                if (path != null) {
                    photoPathList.add(path);
                    addImageToView(path);
                }
            }
        }
    }

    private String getImagePathFromUri(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        android.database.Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(columnIndex);
            cursor.close();
            return path;
        }
        return null;
    }

    private void addImageToView(String path) {
        ImageView iv = new ImageView(this);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 240;
        params.height = 240;
        params.setMargins(8, 8, 8, 8);
        iv.setLayoutParams(params);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);

        iv.setImageURI(Uri.fromFile(new File(path)));

        iv.setOnClickListener(v -> {
            Intent intent = new Intent(DetailActivity.this, ImagePreviewActivity.class);
            intent.putExtra("path", path);
            startActivity(intent);
        });

        iv.setOnLongClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("确认删除")
                    .setMessage("确定要从该笔记中移除此图片吗？")
                    .setPositiveButton("确定", (dialog, which) -> {
                        photoPathList.remove(path);
                        gl_photos.removeView(iv);
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        });

        gl_photos.addView(iv);
    }

    private void loadData() {
        currentId = getIntent().getIntExtra("id", -1);
        if (currentId == -1) return;

        QuestionNote note = dao.queryById(currentId);
        if (note == null) return;

        et_question.setText(note.getQuestion());
        et_answer.setText(note.getAnswer());
        et_tag.setText(note.getTag());
        et_category.setText(note.getCategory());
        et_user_note.setText(note.getUserNote());

        String paths = note.getPhotoPaths();
        if (paths != null && !paths.isEmpty()) {
            String[] arr = paths.split(";");
            photoPathList = new ArrayList<>(Arrays.asList(arr));
            for (String p : arr) {
                if (new File(p).exists()) {
                    addImageToView(p);
                }
            }
        }
    }

    private void updateNote() {
        String question = et_question.getText().toString();
        String answer = et_answer.getText().toString();
        String tag = et_tag.getText().toString();
        String category = et_category.getText().toString();
        String userNote = et_user_note.getText().toString();

        StringBuilder sb = new StringBuilder();
        for (String s : photoPathList) sb.append(s).append(";");
        String photoPaths = sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";

        QuestionNote note = new QuestionNote(currentId, question, answer, tag, userNote, category, 0, photoPaths);

        dao.updateNote(note);
        Toast.makeText(this, "修改已保存", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void deleteNote() {
        dao.deleteById(currentId);
        Toast.makeText(this, "笔记已删除", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dao != null) dao.close();
    }
}