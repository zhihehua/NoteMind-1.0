package com.example.NoteMind;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;

public class QuestionDao {
    private final DbHelper dbHelper;
    private final SQLiteDatabase db;

    public QuestionDao(Context context) {
        dbHelper = new DbHelper(context);
        db = dbHelper.getWritableDatabase();
    }

    public long addNote(QuestionNote note) {
        ContentValues values = new ContentValues();
        values.put(DbHelper.QUESTION, note.getQuestion());
        values.put(DbHelper.ANSWER, note.getAnswer());
        values.put(DbHelper.TAG, note.getTag());
        values.put(DbHelper.USER_NOTE, note.getUserNote());
        values.put(DbHelper.CATEGORY, note.getCategory());
        values.put(DbHelper.KNOWLEDGE_ID, note.getKnowledgeId());
        values.put(DbHelper.PHOTO_PATHS, note.getPhotoPaths());
        return db.insert(DbHelper.TABLE_NAME, null, values);
    }

    public List<QuestionNote> queryAll() {
        List<QuestionNote> list = new ArrayList<>();
        Cursor cursor = db.query(DbHelper.TABLE_NAME, null, null, null, null, null, DbHelper.ID + " DESC");
        while (cursor.moveToNext()) {
            list.add(cursorToNote(cursor));
        }
        cursor.close();
        return list;
    }

    public List<String> getAllUniqueTags() {
        List<String> tags = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT DISTINCT " + DbHelper.TAG + " FROM " + DbHelper.TABLE_NAME + " WHERE " + DbHelper.TAG + " IS NOT NULL AND " + DbHelper.TAG + " != ''", null);
        while (cursor.moveToNext()) {
            @SuppressLint("Range") String tag = cursor.getString(cursor.getColumnIndex(DbHelper.TAG));
            tags.add(tag);
        }
        cursor.close();
        return tags;
    }

    public List<String> getAllUniqueCategories() {
        List<String> categories = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT DISTINCT " + DbHelper.CATEGORY + " FROM " + DbHelper.TABLE_NAME + " WHERE " + DbHelper.CATEGORY + " IS NOT NULL AND " + DbHelper.CATEGORY + " != ''", null);
        while (cursor.moveToNext()) {
            @SuppressLint("Range") String category = cursor.getString(cursor.getColumnIndex(DbHelper.CATEGORY));
            categories.add(category);
        }
        cursor.close();
        return categories;
    }

    public List<QuestionNote> queryByTag(String tag) {
        List<QuestionNote> list = new ArrayList<>();
        Cursor cursor = db.query(DbHelper.TABLE_NAME, null, DbHelper.TAG + "=?", new String[]{tag}, null, null, null);
        while (cursor.moveToNext()) {
            list.add(cursorToNote(cursor));
        }
        cursor.close();
        return list;
    }

    public void updateNote(QuestionNote note) {
        ContentValues values = new ContentValues();
        values.put(DbHelper.QUESTION, note.getQuestion());
        values.put(DbHelper.ANSWER, note.getAnswer());
        values.put(DbHelper.TAG, note.getTag());
        values.put(DbHelper.USER_NOTE, note.getUserNote());
        values.put(DbHelper.CATEGORY, note.getCategory());
        values.put(DbHelper.PHOTO_PATHS, note.getPhotoPaths());
        db.update(DbHelper.TABLE_NAME, values, DbHelper.ID + "=?", new String[]{String.valueOf(note.getId())});
    }

    public void deleteById(int id) {
        db.delete(DbHelper.TABLE_NAME, DbHelper.ID + "=?", new String[]{String.valueOf(id)});
    }

    public QuestionNote queryById(int id) {
        Cursor cursor = db.query(DbHelper.TABLE_NAME, null, DbHelper.ID + "=?", new String[]{String.valueOf(id)}, null, null, null);
        if (cursor.moveToFirst()) {
            QuestionNote note = cursorToNote(cursor);
            cursor.close();
            return note;
        }
        cursor.close();
        return null;
    }

    @SuppressLint("Range")
    private QuestionNote cursorToNote(Cursor cursor) {
        // 重要：这里必须只传 8 个参数，严禁调用 CREATE_TIME
        return new QuestionNote(
                cursor.getInt(cursor.getColumnIndex(DbHelper.ID)),
                cursor.getString(cursor.getColumnIndex(DbHelper.QUESTION)),
                cursor.getString(cursor.getColumnIndex(DbHelper.ANSWER)),
                cursor.getString(cursor.getColumnIndex(DbHelper.TAG)),
                cursor.getString(cursor.getColumnIndex(DbHelper.USER_NOTE)),
                cursor.getString(cursor.getColumnIndex(DbHelper.CATEGORY)),
                cursor.getInt(cursor.getColumnIndex(DbHelper.KNOWLEDGE_ID)),
                cursor.getString(cursor.getColumnIndex(DbHelper.PHOTO_PATHS))
        );
    }

    public void close() {
        dbHelper.close();
    }
}