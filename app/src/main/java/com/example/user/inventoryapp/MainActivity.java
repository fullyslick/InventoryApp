package com.example.user.inventoryapp;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.example.user.inventoryapp.data.ProductContract.ProductEntry;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Tag for the log messages
     */
    public static final String LOG_TAG = MainActivity.class.getSimpleName();

    // Constants that holds the ID of the product Loader
    private static final int PRODUCT_LOADER = 1 ;

    // Member variable to store the ProductCursorAdapter object
    ProductCursorAdapter mCursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find the ListView which will be populated with the product data
        ListView productListView = (ListView) findViewById(R.id.list);

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_view);
        productListView.setEmptyView(emptyView);

        // Setup empty cursor adapter to create a list view from each row from DB ( cursor )
        mCursorAdapter = new ProductCursorAdapter(this, null);

        // Attach cursor adapter to the ListView
        productListView.setAdapter(mCursorAdapter);

        // Prepare the loader.  Either re-connect with an existing one, or start a new one.
        getLoaderManager().initLoader(PRODUCT_LOADER, null, this);
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
                // Call helper method insertDummyData
                insertDummyData();

                return true;

            // Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_entries:
                // TO:DO howDeleteConfirmationDialog(); HERE
                deleteAllProducts();

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

    // This method deletes all products from the database
    private void deleteAllProducts(){
        // This variable stores the number of rows deleted
        int mRowsDeleted = 0;

        // Now delete all rows from the database
        mRowsDeleted = getContentResolver().delete(
                ProductEntry.CONTENT_URI,  // Select all database
                null,                      // No additional arguments because we delete all of the rows
                null
        );

        // Check if all the rows were deleted and inform the user
        if( mRowsDeleted != 0) {
            //  Show message to inform the user that all products were deleted
            Toast.makeText(this, getString(R.string.all_products_deleted), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, getString(R.string.failed_deleting_all_products), Toast.LENGTH_SHORT).show();
        }
    }

    // Create the cursor loader
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // The columns that should be delivered by the content resolver
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_PHOTO_URI };

        return new CursorLoader(this,     // Parent activity context (Main Activity)
                ProductEntry.CONTENT_URI, // Provider content uri to query
                projection,               // Columns to include in the resulting Cursor
                null,                     // No selection clause
                null,                     // No selection argument
                null);                    // Default sort order
    }

    // Querying asynchronously the database has finished
    // Get the loader object and the data from the cursor
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // ProductCursorAdapter with the new data from the database delivered in cursor object
        mCursorAdapter.swapCursor(data);
    }

    // This is called when the last Cursor provided to onLoadFinished()
    // above is about to be closed.  We need to make sure we are no
    // longer using it.
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Pass empty values for the adapter
        mCursorAdapter.swapCursor(null);
    }
}
