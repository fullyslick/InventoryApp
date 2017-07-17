package com.example.user.inventoryapp.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.example.user.inventoryapp.data.ProductContract.ProductEntry;

import android.util.Log;

import static java.security.AccessController.getContext;

/**
 * Created by Alexander Rashkov on 10.07.17.
 */

public class ProductProvider extends ContentProvider {

    /**
     * Tag for the log messages
     */
    public static final String LOG_TAG = ProductProvider.class.getSimpleName();
    /**
     * URI matcher code for the content URI for the products table
     */
    private static final int PRODUCTS = 100;

    /**
     * URI matcher code for the content URI for a single product in the products table
     */
    private static final int PRODUCT_ID = 101;
    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        // The calls to addURI() go here, for all of the content URI patterns that the provider
        // should recognize. All paths added to the UriMatcher have a corresponding code to return
        // when a match is found.
        // Code for all table selected
        sUriMatcher.addURI(ProductContract.CONTENT_AUTHORITY, ProductContract.PATH_PRODUCTS, PRODUCTS);

        // Code for a single product selected
        sUriMatcher.addURI(ProductContract.CONTENT_AUTHORITY, ProductContract.PATH_PRODUCTS + "/#", PRODUCT_ID);
    }

    // Database helper that will provide us access to the database
    private ProductDbHelper mDbHelper;

    /**
     * Initialize the provider and the database helper object.
     */
    @Override
    public boolean onCreate() {
        mDbHelper = new ProductDbHelper(getContext());

        return true;
    }

    // Perform the query for the given URI. Use the given projection, selection, selection arguments, and sort order.
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);

        switch (match) {
            // If the passed Uri matches the PRODUCTS int, then query all items from the database
            case PRODUCTS:
                // Perform the query for the whole table and return the cursor object containing the data
                cursor = database.query(ProductEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;

            // If the passed Uri matches the PRODUCT_ID int, then query only a single row from the database
            case PRODUCT_ID:
                // Extract out the ID of the requested row from the URI
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                // Perform the query on the table where the _id equals _ID extracted from the passed Uri
                // return the cursor object containing the data
                cursor = database.query(ProductEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;

            // If the Uri do not match the templates throw an exception
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // This method helps me determine when query is performed by showing the uri inserted in logcat
        Log.i(LOG_TAG, "Querying the database. The uri passed for querying is:" + uri);

        // Set notification URI on the Cursor,
        // so we know what content URI the Cursor was created for.
        // If the data at this URI changes, then when know we need to update the Cursor
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    // Insert data into the database
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);

        // This method should work only for Uri-s asking for access on the whole database
        switch (match) {
            case PRODUCTS:
                // Calls the insertProduct helper method to insert data into the database
                return insertProduct(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    // Helper method that inserts data into the database and returns the Uri for the inserted row
    private Uri insertProduct(Uri uri, ContentValues values) {
        // Check that the product name is not null. I think that extraction data from the input may result in
        // inserting an "empty" string, which is different from null.
        // That is why I also check for an "empty" string: length() == 0
        String productName = values.getAsString(ProductEntry.COLUMN_PRODUCT_NAME);
        if (productName == null || productName.length() == 0) {
            throw new IllegalArgumentException("Insert Exception! Product requires a product name!");
        }

        // Check that the price is not null or not a negative number
        Float price = values.getAsFloat(ProductEntry.COLUMN_PRODUCT_PRICE);
        if (price == null || price < 0) {
            throw new IllegalArgumentException("Insert Exception! Product requires a price or a positive value!");
        }

        // No need to check other inputs because they have default values, and "suppliers e-mail" can have no value

        // Get writable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Insert the new product with the given values
        long id = database.insert(ProductEntry.TABLE_NAME, null, values);

        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Notify all listeners that the data has changed for the product content URI
        getContext().getContentResolver().notifyChange(uri, null);

        // Once we know the ID of the new row in the table,
        // assign to new Uri variable, the new URI with the ID appended to the end of it
        Uri returnedUri = ContentUris.withAppendedId(uri, id);

        // This method helps me determine when insert is performed by showing in the logcat,
        // the number of rows inserted and the new uri
        // and the values passed
        Log.i(LOG_TAG, "Inserting into the database! The number of row inserted is " + id + "\n" +
                        " and the new uri is: " + returnedUri + "\n" + " values: " + values);

        return returnedUri ;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection,
                      String[] selectionArgs) {

        // Figure out if the URI matcher can match the URI to a specific code
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                // If it matches the int for the whole database, do not modify selection & selectionArgs
                // and call helper method updateProduct()
                return updateProduct(uri, contentValues, selection, selectionArgs);
            case PRODUCT_ID:
                // It it matches the int for a single row, provide a selection & selectionArgs
                // Extract out the ID of the requested row from the URI
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                // Now call helper method updateProduct()
                return updateProduct(uri, contentValues, selection, selectionArgs);
            default:
                // If the Uri do not match the int templates throw an exception
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    // Helper method that sends command to the database to perform an update
    // Returns the number of rows that have been updated
    private int updateProduct(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        // Check if product_name column key is present,
        // and that its value is not null. I think that extraction data from the input may result in
        // inserting an empty string, which is different from null.
        // That is why I also check for an "empty" string: length() == 0
        if (values.containsKey(ProductEntry.COLUMN_PRODUCT_NAME)) {
            String productName = values.getAsString(ProductEntry.COLUMN_PRODUCT_NAME);
            if (productName == null || productName.length() == 0) {
                // If there is no product name, do not try to update database
                return 0;
            }
        }

        // Check if product_price column key is present,
        // and that its value is not null or negative
        if (values.containsKey(ProductEntry.COLUMN_PRODUCT_PRICE)) {
            // Check that the price is not null or not a negative number
            Float price = values.getAsFloat(ProductEntry.COLUMN_PRODUCT_PRICE);
            if (price == null || price < 0) {
                // If there is no price for the product or it is a negative value, do not try to update database
                return 0;
            }
        }

        // No need to check other inputs because they have default values, and "suppliers e-mail" can have no value

        // If there are no values to update, then don't try to update the database
        if (values.size() == 0) {
            return 0;
        }

        // Otherwise, get writable database to update the data
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Perform the update on the database and get the number of rows affected
        int rowsUpdated = database.update(ProductEntry.TABLE_NAME, values, selection, selectionArgs);

        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // This method helps me determine when update is performed by showing in the logcat,
        // the number of rows updated
        Log.i(LOG_TAG, "Updating the database! The number of rows updated is: " + rowsUpdated );

        // Return the number of rows updated
        return rowsUpdated;
    }

    // Delete the data at the given selection and selection arguments.
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Get writable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Track the number of rows that were deleted
        int rowsDeleted;

        // Figure out if the URI matcher can match the URI to a specific code
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case PRODUCTS:
                // If it matches the int for the whole database, do not modify selection & selectionArgs
                // Execute an SQLite command to delete the whole database rows
                rowsDeleted = database.delete(ProductEntry.TABLE_NAME, selection, selectionArgs);
                break;

            case PRODUCT_ID:
                // If it matches the int for a single row or rows,
                // modify selection & selectionArgs to get the id of the row
                // Extract out the ID of the requested row from the URI
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                // Execute an SQLite command to delete the selected row from database
                rowsDeleted = database.delete(ProductEntry.TABLE_NAME, selection, selectionArgs);
                break;

            // If the pass Uri do not match any of the int values, throw an exception
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        // If 1 or more rows were deleted, then notify all listeners that the data at the
        // given URI has changed
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // This method helps me determine when delete is performed by showing in the logcat,
        // the number of rows deleted
        Log.i(LOG_TAG, "Deleting from database! The number of rows deleted is: " + rowsDeleted );

        // Return the number of rows deleted
        return rowsDeleted;
    }

    /**
     * The purpose of this method is to return a String,
     * that describes the type of the data stored at the input Uri.
     * Returns the MIME type of data for the content URI.
     */
    @Override
    public String getType(Uri uri) {
        // Figure out if the URI matcher can match the URI to a specific code
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case PRODUCTS:
                return ProductEntry.CONTENT_LIST_TYPE;
            case PRODUCT_ID:
                return ProductEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }
}
