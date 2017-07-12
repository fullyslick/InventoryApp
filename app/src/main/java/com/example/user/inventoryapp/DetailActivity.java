package com.example.user.inventoryapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Created by Alexander Rashkov on 12.07.17.
 */

//  Allows user to insert a new product or edit an existing one.

public class DetailActivity extends AppCompatActivity {

    // Tag for the log messages
    public static final String LOG_TAG = MainActivity.class.getSimpleName();

    // Content URI for the existing product (null if it's a new product)
    private Uri mCurrentProductUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new product or editing an existing one.
        Intent intent = getIntent();

        // Apply the extracted uri to the member variable mCurrentProductUri
        mCurrentProductUri = intent.getData();

        // Show me in the logcat the uri extracted from the intent
        Log.i(LOG_TAG, "The passed uri from MainActivity is: " + mCurrentProductUri);

        // If the intent DOES NOT contain a any URI ( null ), then we know that we are
        // creating a new product, because ony clicking on ListItem (product) passes any uri
        if( mCurrentProductUri == null){
            // This is a new product, so change the app bar to say "Add a Product"
            setTitle(getString(R.string.detail_activity_title_new_product));

            // And call invalidate option
            // Now we will be able to modify the options menu (remove delete)
            // This happens in onPrepareOptionsMenu()
            invalidateOptionsMenu();

        } else {
            // Otherwise this is an existing product, so change app bar to say "Edit Product"
            setTitle(getString(R.string.detail_activity_title_edit_product));

            // TO:DO
            // Prepare the loader.  Either re-connect with an existing one,
            // or start a new one.
        }
    }

    // Called from InvalidateOptionsMenu
    // This method removes the "Delete" option when new product is inserting
    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        super.onPrepareOptionsMenu(menu);
        // Should work only for new product insertion, because we do not want to have
        // a "delete" option on a product that do not even exist yet
        super.onPrepareOptionsMenu(menu);
        if (mCurrentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    // Inflate the menu from the xml file
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_detail_activity, menu);
        return true;
    }

    // Implement the actions of the different options from the menu -> Save, Delete, Home
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // User clicked on a menu option in the app bar overflow menu
        // On click of "Save" option
        switch (item.getItemId()) {
            case R.id.action_save:

                // Call the saveProduct() method, in which check editText fields and insert or update db
                // TO:DO
                return  true;

            // On click of "Delete" option
            case R.id.action_delete:

                // Call delete confirmation dialog and from it call deleteProduct() method
                // TO:DO

                return true;

            case android.R.id.home:

                // Check for changes -  mProductHasChange for false return to MainActivity, for true show Dialog
                // TO:DO

                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
