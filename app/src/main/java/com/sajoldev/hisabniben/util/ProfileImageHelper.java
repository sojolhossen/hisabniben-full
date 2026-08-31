package com.sajoldev.hisabniben.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;

public class ProfileImageHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "profile_images.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "profile_images";
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_IMAGE = "image";

    private static ProfileImageHelper instance;

    public static synchronized ProfileImageHelper getInstance(Context context) {
        if (instance == null) {
            instance = new ProfileImageHelper(context.getApplicationContext());
        }
        return instance;
    }

    private ProfileImageHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_USER_ID + " TEXT PRIMARY KEY, " +
                COLUMN_IMAGE + " BLOB)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void saveProfileImage(String userId, Bitmap bitmap) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, userId);
        
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        values.put(COLUMN_IMAGE, stream.toByteArray());
        
        db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public Bitmap getProfileImage(String userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME,
                new String[]{COLUMN_IMAGE},
                COLUMN_USER_ID + " = ?",
                new String[]{userId},
                null, null, null);

        Bitmap bitmap = null;
        if (cursor.moveToFirst()) {
            byte[] byteArray = cursor.getBlob(cursor.getColumnIndexOrThrow(COLUMN_IMAGE));
            if (byteArray != null) {
                bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            }
        }
        cursor.close();
        db.close();
        return bitmap;
    }

    public void deleteProfileImage(String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, COLUMN_USER_ID + " = ?", new String[]{userId});
        db.close();
    }
}
