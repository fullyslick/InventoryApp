package com.example.user.inventoryapp;

import android.content.ContentValues;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.example.user.inventoryapp.data.ProductContract.ProductEntry;

import com.example.user.inventoryapp.data.ProductContract;

public class MainActivity extends AppCompatActivity {

    /**
     * Tag for the log messages
     */
    public static final String LOG_TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_main_activity.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Insert dummy data" menu option
            case R.id.action_insert_dummy_data:
                 // TO:DO INSERT METHOD HERE
                // Call helper method insertDummyData
                insertDummyData();

                return true;

            // Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_entries:
                // TO:DO howDeleteConfirmationDialog(); HERE

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // This method is called when "Insert Dummy Data" option is clicked
    private void insertDummyData() {
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_NAME, "Dummy Product");
        values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, 10);
        values.put(ProductEntry.COLUMN_PRODUCT_PRICE, 7.5 );
        // I will use default value for photo uri,
        // and later in ProductProvider will check the string and for "no image" will give some drawable
        values.put(ProductEntry.COLUMN_PRODUCT_PHOTO_URI, "no image");
        values.put(ProductEntry.COLUMN_SUPPLIER_NAME, "Dummy Supplier");
        values.put(ProductEntry.COLUMN_SUPPLIER_EMAIL, "mail@dummysupplier.com");

        // Call the contentResolver to communicate with ProductProvider and insert the dummy data
        Uri newUri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);

        // Check if the dummy data is inserted by showing a message in the logcat
        Log.i(LOG_TAG, "Inserted dummy data from MainActivity. The new row Uri is: " + newUri);
    }
}
