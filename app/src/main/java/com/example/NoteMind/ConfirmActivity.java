package com.example.NoteMind;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.List;

public class ConfirmActivity extends AppCompatActivity {
    private EditText etQuestion, etAnswer, etTag, etUserNote, etCategory;
    private Button btnCancel, btnSave;
    private TextView btnAddTag, btnAddCategory;
    private QuestionDao dao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm);

        etQuestion = findViewById(R.id.et_question);
        etAnswer = findViewById(R.id.et_answer);
        etTag = findViewById(R.id.et_tag);
        etUserNote = findViewById(R.id.et_user_note);
        etCategory = findViewById(R.id.et_category);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);
        btnAddTag = findViewById(R.id.btn_add_tag);
        btnAddCategory = findViewById(R.id.btn_add_category);
        
        dao = new QuestionDao(this);

        String question = getIntent().getStringExtra("question");
        String answer = getIntent().getStringExtra("answer");
        etQuestion.setText(question);
        etAnswer.setText(answer);

        btnAddTag.setOnClickListener(v -> showExistingTags());
        btnAddCategory.setOnClickListener(v -> showExistingCategories());

        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> {
            String q = etQuestion.getText().toString().trim();
            String a = etAnswer.getText().toString().trim();
            String tag = etTag.getText().toString().trim();
            String note = etUserNote.getText().toString().trim();
            String cate = etCategory.getText().toString().trim();

            if (q.isEmpty()) {
                Toast.makeText(this, "标题不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            if (tag.isEmpty()) {
                Toast.makeText(this, "标签不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            if (cate.isEmpty()) {
                Toast.makeText(this, "分类不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            QuestionNote noteObj = new QuestionNote(q, a, tag, note, cate);
            long id = dao.addNote(noteObj);
            if (id > 0) {
                Toast.makeText(this, "保存成功！", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
            }
        });
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
                .setItems(tagArray, (dialog, which) -> etTag.setText(tagArray[which]))
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
                .setItems(catArray, (dialog, which) -> etCategory.setText(catArray[which]))
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dao.close();
    }
}