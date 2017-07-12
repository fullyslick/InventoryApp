package com.example.user.inventoryapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Created by Alexander Rashkov on 12.07.17.
 */

//  Allows user to insert a new product or edit an existing one.

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
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

                // Call the saveProduct() method
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
