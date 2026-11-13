package com.example.campusfind;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.security.MessageDigest;

public class DBHelper extends SQLiteOpenHelper {

    public DBHelper(Context context) {
        super(context, "CampusFind.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users(email TEXT PRIMARY KEY, name TEXT, password TEXT)");
        db.execSQL("CREATE TABLE items(id TEXT PRIMARY KEY, type TEXT, title TEXT, category TEXT, place TEXT, contact TEXT, date TEXT, lat REAL, lng REAL, status TEXT, reported_by_email TEXT, reported_by_name TEXT)");

        // Starter Data - 6 Reported Items (CF-01 to CF-06)
        insertStarter(db, "CF-01", "Found", "Black leather wallet", "Wallets & Cards", "Main Library", "03001234567", "12-Nov-2026", 31.38955, 74.24070);
        insertStarter(db, "CF-02", "Lost", "Casio fx-991ES calculator", "Electronics", "CS Block", "03001234567", "11-Nov-2026", 31.38990, 74.24120);
        insertStarter(db, "CF-03", "Found", "Student ID card", "Wallets & Cards", "Cafeteria", "03001234567", "10-Nov-2026", 31.38880, 74.24010);
        insertStarter(db, "CF-04", "Lost", "Blue water bottle", "Other", "Sports Ground", "03001234567", "09-Nov-2026", 31.38800, 74.24200);
        insertStarter(db, "CF-05", "Found", "Car keys, red keychain", "Keys", "Main Parking", "03001234567", "08-Nov-2026", 31.39050, 74.23950);
        insertStarter(db, "CF-06", "Lost", "32 GB USB flash drive", "Electronics", "Mosque", "03001234567", "07-Nov-2026", 31.38920, 74.24180);
    }

    private void insertStarter(SQLiteDatabase db, String id, String type, String title, String cat, String place, String contact, String date, double lat, double lng) {
        ContentValues cv = new ContentValues();
        cv.put("id", id);
        cv.put("type", type);
        cv.put("title", title);
        cv.put("category", cat);
        cv.put("place", place);
        cv.put("contact", contact);
        cv.put("date", date);
        cv.put("lat", lat);
        cv.put("lng", lng);
        cv.put("status", "Open");
        cv.put("reported_by_email", "affairs@uol.edu.pk");
        cv.put("reported_by_name", "Student Affairs");
        db.insert("items", null, cv);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL("DROP TABLE IF EXISTS items");
        onCreate(db);
    }

    // R2: Passwords are never stored as plain text (SHA-256 Hash)
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(password.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return password;
        }
    }

    public boolean emailExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM users WHERE email=?", new String[]{email});
        boolean exists = c.getCount() > 0;
        c.close();
        return exists;
    }

    public boolean registerUser(String name, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("email", email);
        cv.put("password", hashPassword(password));
        return db.insert("users", null, cv) != -1;
    }

    public String loginUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT name FROM users WHERE email=? AND password=?", new String[]{email, hashPassword(password)});
        String name = null;
        if (c.moveToFirst()) name = c.getString(0);
        c.close();
        return name;
    }

    // R5: Next ID generator (CF-07, CF-08, ...)
    public String getNextItemId() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM items", null);
        int count = 6;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return String.format("CF-%02d", count + 1);
    }

    public boolean insertItem(String id, String type, String title, String category, String place, String contact, String date, double lat, double lng, String email, String userName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("id", id);
        cv.put("type", type);
        cv.put("title", title);
        cv.put("category", category);
        cv.put("place", place);
        cv.put("contact", contact);
        cv.put("date", date);
        cv.put("lat", lat);
        cv.put("lng", lng);
        cv.put("status", "Open");
        cv.put("reported_by_email", email);
        cv.put("reported_by_name", userName);
        return db.insert("items", null, cv) != -1;
    }

    // R6: Order newest item first (higher ID first as dates are sequential)
    public Cursor getAllItems(String filterType, String query) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT * FROM items WHERE 1=1";
        if (!filterType.equals("All")) {
            sql += " AND type='" + filterType + "'";
        }
        if (query != null && !query.trim().isEmpty()) {
            String q = query.trim().replace("'", "''");
            sql += " AND (title LIKE '%" + q + "%' OR place LIKE '%" + q + "%' OR category LIKE '%" + q + "%')";
        }
        sql += " ORDER BY id DESC";
        return db.rawQuery(sql, null);
    }

    public Cursor getItemById(String id) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM items WHERE id=?", new String[]{id});
    }

    // R8: Mark as Returned
    public void markItemReturned(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("status", "Returned");
        db.update("items", cv, "id=?", new String[]{id});
    }
}
