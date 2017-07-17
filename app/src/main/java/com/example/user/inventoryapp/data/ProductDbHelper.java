package com.example.user.inventoryapp.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.user.inventoryapp.data.ProductContract.ProductEntry;

/**
 * Created by Alexander Rashkov on 10.07.17.
 */

public class ProductDbHelper extends SQLiteOpenHelper {

    // When changing the database schema, here the database version must be incremented.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "inventory.db";

    // Construct the ProductDbHelper class
    public ProductDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Create the database
    @Override
    public void onCreate(SQLiteDatabase db) {
        String SQL_CREATE_ENTRIES = "CREATE TABLE " + ProductEntry.TABLE_NAME + " (" +
                ProductEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                ProductEntry.COLUMN_PRODUCT_NAME + " TEXT NOT NULL," +
                ProductEntry.COLUMN_PRODUCT_QUANTITY + " INTEGER NOT NULL DEFAULT 0," +
                ProductEntry.COLUMN_PRODUCT_PRICE + " REAL NOT NULL," +
                ProductEntry.COLUMN_PRODUCT_PHOTO_URI + " TEXT NOT NULL DEFAULT 'no image'," +
                ProductEntry.COLUMN_SUPPLIER_NAME + " TEXT," +
                ProductEntry.COLUMN_SUPPLIER_EMAIL + " TEXT);";

        // Execute the database
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    // This is called when the database needs to be updated.
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + ProductEntry.TABLE_NAME);
        onCreate(db);
    }
}
