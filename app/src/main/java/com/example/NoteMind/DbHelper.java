package com.example.NoteMind;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {
    // 1. 数据库版本提升到 2
    private static final String DATABASE_NAME = "notemind.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_NAME = "question_note";
    public static final String ID = "id";
    public static final String QUESTION = "question";
    public static final String ANSWER = "answer";
    public static final String TAG = "tag";
    public static final String USER_NOTE = "user_note";
    public static final String CATEGORY = "category";
    public static final String KNOWLEDGE_ID = "knowledge_id";
    public static final String PHOTO_PATHS = "photo_paths";

    // 【新增字段】创建时间
    public static final String CREATE_TIME = "create_time";

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 创建表语句，增加 DEFAULT CURRENT_TIMESTAMP，这样不用手动插时间，数据库自动生成
        String createTable = "CREATE TABLE " + TABLE_NAME + " ("
                + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + QUESTION + " TEXT, "
                + ANSWER + " TEXT, "
                + TAG + " TEXT, "
                + USER_NOTE + " TEXT, "
                + CATEGORY + " TEXT, "
                + KNOWLEDGE_ID + " INTEGER, "
                + PHOTO_PATHS + " TEXT, "
                + CREATE_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP"
                + ")";
        db.execSQL(createTable);

        // 为 TAG 增加索引，提升后面“领域分析”时的查询速度
        db.execSQL("CREATE INDEX idx_tag ON " + TABLE_NAME + "(" + TAG + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // --- 无损迁移逻辑 ---
            // 1. 创建一个临时新表 (带 create_time)
            String createNewTable = "CREATE TABLE " + TABLE_NAME + "_new ("
                    + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + QUESTION + " TEXT, "
                    + ANSWER + " TEXT, "
                    + TAG + " TEXT, "
                    + USER_NOTE + " TEXT, "
                    + CATEGORY + " TEXT, "
                    + KNOWLEDGE_ID + " INTEGER, "
                    + PHOTO_PATHS + " TEXT, "
                    + CREATE_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP"
                    + ")";
            db.execSQL(createNewTable);

            // 2. 将旧表数据导入新表
            db.execSQL("INSERT INTO " + TABLE_NAME + "_new ("
                    + QUESTION + ", " + ANSWER + ", " + TAG + ", " + USER_NOTE + ", "
                    + CATEGORY + ", " + KNOWLEDGE_ID + ", " + PHOTO_PATHS
                    + ") SELECT "
                    + QUESTION + ", " + ANSWER + ", " + TAG + ", " + USER_NOTE + ", "
                    + CATEGORY + ", " + KNOWLEDGE_ID + ", " + PHOTO_PATHS
                    + " FROM " + TABLE_NAME);

            // 3. 删除旧表
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);

            // 4. 将新表重命名回原名
            db.execSQL("ALTER TABLE " + TABLE_NAME + "_new RENAME TO " + TABLE_NAME);

            // 5. 重新补上索引
            db.execSQL("CREATE INDEX idx_tag ON " + TABLE_NAME + "(" + TAG + ")");
        }
    }
}