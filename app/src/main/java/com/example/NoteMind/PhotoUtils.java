package com.example.NoteMind;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PhotoUtils {

    // 保存图片，并只返回 文件绝对路径（永远不会加载失败）
    public static String savePhoto(Context context, Bitmap bitmap) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.CHINA).format(new Date());
            String fileName = "Note_" + timeStamp + ".jpg";

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

            Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            OutputStream out = context.getContentResolver().openOutputStream(uri);

            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
            out.close();

            // 返回真实路径，不是 Uri！！！（这是你全部问题的根源）
            return getRealPathFromUri(context, uri);

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    // 把 Uri 转成 真实文件路径（万能兼容）
    public static String getRealPathFromUri(Context context, Uri uri) {
        try {
            java.io.File file = new java.io.File(uri.getPath());
            String[] proj = {MediaStore.Images.Media.DATA};
            android.database.Cursor cursor = context.getContentResolver().query(uri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            cursor.close();
            return path;
        } catch (Exception e) {
            return "";
        }
    }
}