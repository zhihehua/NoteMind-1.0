package com.example.NoteMind;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;

public class ImagePreviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        ImageView iv_preview = findViewById(R.id.iv_preview);
        ImageButton btn_back = findViewById(R.id.btn_back);
        
        String path = getIntent().getStringExtra("path");

        // 纯安卓原生加载图片，不用任何第三方库
        if (path != null) {
            iv_preview.setImageURI(android.net.Uri.fromFile(new File(path)));
        }

        // 返回按钮逻辑
        btn_back.setOnClickListener(v -> finish());
    }
}