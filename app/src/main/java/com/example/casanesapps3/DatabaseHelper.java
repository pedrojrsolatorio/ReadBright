package com.example.casanesapps3;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "AI_Learn.db";
    private static final int DATABASE_VERSION = 5;

    private static DatabaseHelper instance;

    // Tables
    public static final String TABLE_USERS = "users";
    public static final String TABLE_PROGRESS = "progress";
    public static final String TABLE_AI_HISTORY = "ai_history";
    public static final String TABLE_AI_PERFORMANCE = "ai_performance";

    // Columns - Users
    public static final String COL_ID = "id";
    public static final String COL_STUDENT_ID = "student_id";
    public static final String COL_USERNAME = "username";
    public static final String COL_PASSWORD = "password";
    public static final String COL_NAME = "name";
    public static final String COL_GRADE = "grade";
    public static final String COL_ROLE = "role";
    public static final String COL_STARS = "stars";
    public static final String COL_VOICE_SPEED = "voice_speed";
    public static final String COL_VOICE_ACCENT = "voice_accent";
    public static final String COL_BIRTHDAY = "birthday";
    public static final String COL_GENDER = "gender";
    public static final String COL_AGE = "age";

    // Columns - Progress
    public static final String COL_PROG_ID = "prog_id";
    public static final String COL_USER_REF = "username_ref";
    public static final String COL_MODULE_NAME = "module_name";
    public static final String COL_IS_COMPLETED = "is_completed";

    // Columns - AI History
    public static final String COL_HIST_ID = "hist_id";
    public static final String COL_HIST_USER = "hist_username";
    public static final String COL_HIST_CONTENT = "hist_content";
    public static final String COL_HIST_GRADE = "hist_grade";

    // Columns - AI Performance
    public static final String COL_PERF_ID = "perf_id";
    public static final String COL_PERF_USER = "perf_username";
    public static final String COL_PERF_CONTENT = "perf_content";
    public static final String COL_PERF_GRADE = "perf_grade";
    public static final String COL_PERF_CORRECT = "correct_count";
    public static final String COL_PERF_WRONG = "wrong_count";
    public static final String COL_PERF_ATTEMPTS = "attempts";
    public static final String COL_PERF_DIFFICULTY = "difficulty";
    public static final String COL_PERF_LAST_USED = "last_used";

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USERS + "("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_STUDENT_ID + " TEXT,"
                + COL_USERNAME + " TEXT UNIQUE,"
                + COL_PASSWORD + " TEXT,"
                + COL_NAME + " TEXT,"
                + COL_GRADE + " TEXT,"
                + COL_ROLE + " TEXT,"
                + COL_STARS + " INTEGER DEFAULT 0,"
                + COL_VOICE_SPEED + " REAL DEFAULT 1.0,"
                + COL_VOICE_ACCENT + " TEXT DEFAULT 'US',"
                + COL_BIRTHDAY + " TEXT,"
                + COL_GENDER + " TEXT,"
                + COL_AGE + " INTEGER"
                + ")");

        db.execSQL("CREATE TABLE " + TABLE_PROGRESS + "("
                + COL_PROG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_USER_REF + " TEXT,"
                + COL_MODULE_NAME + " TEXT,"
                + COL_IS_COMPLETED + " INTEGER DEFAULT 0,"
                + "UNIQUE(" + COL_USER_REF + ", " + COL_MODULE_NAME + ")"
                + ")");

        db.execSQL("CREATE TABLE " + TABLE_AI_HISTORY + "("
                + COL_HIST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_HIST_USER + " TEXT,"
                + COL_HIST_CONTENT + " TEXT,"
                + COL_HIST_GRADE + " TEXT,"
                + "UNIQUE(" + COL_HIST_USER + ", " + COL_HIST_CONTENT + ", " + COL_HIST_GRADE + ")"
                + ")");

        createPerformanceTable(db);
    }

    private void createPerformanceTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS "
                + TABLE_AI_PERFORMANCE + "("
                + COL_PERF_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_PERF_USER + " TEXT,"
                + COL_PERF_CONTENT + " TEXT,"
                + COL_PERF_GRADE + " TEXT,"
                + COL_PERF_CORRECT + " INTEGER DEFAULT 0,"
                + COL_PERF_WRONG + " INTEGER DEFAULT 0,"
                + COL_PERF_ATTEMPTS + " INTEGER DEFAULT 0,"
                + COL_PERF_DIFFICULTY + " TEXT DEFAULT 'easy',"
                + COL_PERF_LAST_USED + " INTEGER DEFAULT 0,"
                + "UNIQUE(" + COL_PERF_USER + "," + COL_PERF_CONTENT + "," + COL_PERF_GRADE + ")"
                + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 4) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_AI_HISTORY + "("
                    + COL_HIST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COL_HIST_USER + " TEXT,"
                    + COL_HIST_CONTENT + " TEXT,"
                    + COL_HIST_GRADE + " TEXT,"
                    + "UNIQUE(" + COL_HIST_USER + "," + COL_HIST_CONTENT + "," + COL_HIST_GRADE + ")"
                    + ")");
        }
        if (oldVersion < 5) createPerformanceTable(db);
    }

    // USER METHODS
    public boolean addUser(String studentId, String username, String password, String name, String grade, String role, String birthday, String gender, int age) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COL_STUDENT_ID, studentId);
        v.put(COL_USERNAME, username);
        v.put(COL_PASSWORD, password);
        v.put(COL_NAME, name);
        v.put(COL_GRADE, grade);
        v.put(COL_ROLE, role);
        v.put(COL_BIRTHDAY, birthday);
        v.put(COL_GENDER, gender);
        v.put(COL_AGE, age);
        return db.insert(TABLE_USERS, null, v) != -1;
    }

    public Cursor checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + COL_USERNAME + "=? AND " + COL_PASSWORD + "=?", new String[]{username, password});
    }

    public boolean checkUsernameExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + COL_USERNAME + "=?", new String[]{username});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public Cursor getUser(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + COL_USERNAME + "=?", new String[]{username});
    }

    public void addStars(String username, int amount) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_USERS + " SET " + COL_STARS + " = " + COL_STARS + " + ? WHERE " + COL_USERNAME + "=?", new Object[]{amount, username});
    }

    public int getStars(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_STARS + " FROM " + TABLE_USERS + " WHERE " + COL_USERNAME + "=?", new String[]{username});
        int stars = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                stars = cursor.getInt(cursor.getColumnIndexOrThrow(COL_STARS));
            }
            cursor.close();
        }
        return stars;
    }

    public boolean updateUserGrade(String username, String newGrade) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COL_GRADE, newGrade);
        return db.update(TABLE_USERS, v, COL_USERNAME + "=?", new String[]{username}) > 0;
    }

    // AI METHODS
    public void addToAIHistory(String username, String content, String grade) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COL_HIST_USER, username);
        v.put(COL_HIST_CONTENT, content.toLowerCase().trim());
        v.put(COL_HIST_GRADE, grade);
        db.insertWithOnConflict(TABLE_AI_HISTORY, null, v, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public List<String> getAIHistory(String username, String grade) {
        List<String> history = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT " + COL_HIST_CONTENT + " FROM " + TABLE_AI_HISTORY + " WHERE " + COL_HIST_USER + "=? AND " + COL_HIST_GRADE + "=?", new String[]{username, grade});
        if (c.moveToFirst()) {
            do { history.add(c.getString(0)); } while (c.moveToNext());
        }
        c.close();
        return history;
    }

    public Map<String, Long> getUsageHistory(String username, String grade) {
        Map<String, Long> usage = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT " + COL_PERF_CONTENT + ", " + COL_PERF_LAST_USED + " FROM " + TABLE_AI_PERFORMANCE + " WHERE " + COL_PERF_USER + "=? AND " + COL_PERF_GRADE + "=?", new String[]{username, grade});
        if (c.moveToFirst()) {
            do { usage.put(c.getString(0).toLowerCase().trim(), c.getLong(1)); } while (c.moveToNext());
        }
        c.close();
        return usage;
    }

    public void markAsSeen(String username, String content, String grade) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COL_PERF_USER, username);
        v.put(COL_PERF_CONTENT, content.toLowerCase().trim());
        v.put(COL_PERF_GRADE, grade);
        v.put(COL_PERF_LAST_USED, System.currentTimeMillis());
        db.insertWithOnConflict(TABLE_AI_PERFORMANCE, null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void clearAIHistory(String username, String grade) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_AI_HISTORY, COL_HIST_USER + "=? AND " + COL_HIST_GRADE + "=?", new String[]{username, grade});
        db.delete(TABLE_AI_PERFORMANCE, COL_PERF_USER + "=? AND " + COL_PERF_GRADE + "=?", new String[]{username, grade});
    }
}
