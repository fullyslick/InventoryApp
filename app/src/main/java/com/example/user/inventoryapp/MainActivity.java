package com.example.user.inventoryapp;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.user.inventoryapp.data.ProductContract.ProductEntry;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    // Tag for the log messages
    public static final String LOG_TAG = MainActivity.class.getSimpleName();

    // Constants that holds the ID of the product Loader
    private static final int PRODUCT_LOADER = 1;

    // Member variable to store the ProductCursorAdapter object
    ProductCursorAdapter mCursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup FAB to open DetailActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Start an intent to DetailActivity on FAB click, to insert new product
                Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                startActivity(intent);
            }
        });

        // Find the ListView which will be populated with the product data
        ListView productListView = (ListView) findViewById(R.id.list);

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_view);
        productListView.setEmptyView(emptyView);

        // Setup empty cursor adapter to create a list view from each row from DB ( cursor )
        mCursorAdapter = new ProductCursorAdapter(this, null);

        // Attach cursor adapter to the ListView
        productListView.setAdapter(mCursorAdapter);

        // Set a listener on a ListItem (product) click
        productListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

                // Create new intent to go to {@link DetailActivity}
                Intent intent = new Intent(MainActivity.this, DetailActivity.class);

                // Form the content URI that represents the specific product that was clicked on,
                // by appending the "id" (passed as input to this method) onto the
                // {@link ProductEntry#CONTENT_URI}.
                // If the id passed for a particular ListItem (product) is 5
                // The currentProductUri would be : content://com.example.user.inventoryapp/products/5
                Uri currentProductUri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, id);

                // Set the URI on the data field of the intent
                intent.setData(currentProductUri);

                // Launch the {@link DetailActivity} to display the data for the current product.
                startActivity(intent);
            }
        });

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
            // If user clicked on "Delete All Products" option then show a confirmation dialog
            case R.id.action_delete_all_entries:

                showDeleteConfirmationDialog();

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
        values.put(ProductEntry.COLUMN_PRODUCT_PRICE, 7.5);
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

    // This method displays a confirmation dialog to prompt the user, if he wants to proceed with deleting
    private void showDeleteConfirmationDialog() {
        // Create new AlertDialog object
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Set a message for the dialog
        builder.setMessage(R.string.delete_all_product_dialog_message);

        // Set title for the positive button ("Delete") and attach a listener
        // Call deleteAllProducts() on click of this button
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                deleteAllProducts();
            }
        });

        // Set title for the negative button ("Cancel") and attach a listener
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    // This method deletes all products from the database
    private void deleteAllProducts() {
        // This variable stores the number of rows deleted
        int mRowsDeleted = 0;

        // Now delete all rows from the database
        mRowsDeleted = getContentResolver().delete(
                ProductEntry.CONTENT_URI,  // Select all database
                null,                      // No additional arguments because we delete all of the rows
                null
        );

        // Check if all the rows were deleted and inform the user
        if (mRowsDeleted != 0) {
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
                ProductEntry.COLUMN_PRODUCT_PHOTO_URI};

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
