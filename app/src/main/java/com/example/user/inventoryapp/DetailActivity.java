package com.example.user.inventoryapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.user.inventoryapp.data.ProductContract.ProductEntry;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Alexander Rashkov on 12.07.17.
 */

//  Allows user to insert a new product or edit an existing one.

public class DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    // Tag for the log messages
    public static final String LOG_TAG = DetailActivity.class.getSimpleName();

    // Key for the saved state instance of product's photo Uri
    private static final String STATE_URI = "STATE_URI";

    // The id of the loader
    private static final int EXISTING_PRODUCT_LOADER = 2;

    // Request code intent that gets the image from the users' gallery
    private static final int PICK_IMAGE_REQUEST = 0;

    // Stores the float value of product's price
    float mProductPriceFloat;

    // Stores the int value of product's quantity
    int mProductQuantityInt;

    // Stores the string value of the product's photo uri
    String mProductPhotoString;

    // Defines a variable to contain the number of updated rows
    int mRowsUpdated = 0;

    // Content URI for the existing product (null if it's a new product)
    private Uri mCurrentProductUri;

    // Stores the Uri of the product's photo
    private Uri mProductPhotoUri;

    // Boolean flag that keeps track of whether the product has been edited (true) or not (false)
    private boolean mProductHasChanged = false;

    // EditText field to enter product's name
    private EditText mProductNameEditText;

    // EditText field to enter product's price
    private EditText mProductPriceEditText;

    // EditText field to enter product's quantity
    private EditText mProductQuantityEditText;

    // ImageView that holds the image of the product
    private ImageView mProductPhotoView;

    // EditText field to enter supplier for the product
    private EditText mSupplierNameEditText;

    // EditText field to enter supplier for the product
    private EditText mSupplierEmailEditText;

    // Button that increases product's quantity
    private Button mIncreaseQuantityButton;

    // Button that decreases product's quantity
    private Button mDecreaseQuantityButton;

    // OnTouchListener to register when user is modifying and input
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new product or editing an existing one.
        Intent intent = getIntent();

        // Get the data from the intent and assign it to mCurrentProductUri
        mCurrentProductUri = intent.getData();

        // Show me in the logcat the uri extracted from the intent
        Log.i(LOG_TAG, "The passed uri from MainActivity is: " + mCurrentProductUri);

        // If the intent DOES NOT contain any URI ( null ), then we know that we are
        // creating a new product, because only clicking on ListItem (product) passes any (data) uri
        if (mCurrentProductUri == null) {
            // This is a new product, so change the app bar to say "Add a Product"
            setTitle(getString(R.string.detail_activity_title_new_product));

            // Call invalidate option
            // Now we will be able to modify the options menu (remove delete)
            // This happens in onPrepareOptionsMenu()
            invalidateOptionsMenu();

        } else {
            // Otherwise this is an existing product, so change app bar to say "Edit Product"
            setTitle(getString(R.string.detail_activity_title_edit_product));

            // Prepare the loader.  Either re-connect with an existing one,
            // or start a new one.
            getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user's input from
        mProductNameEditText = (EditText) findViewById(R.id.edit_product_name);
        mProductPriceEditText = (EditText) findViewById(R.id.edit_product_price);
        mProductQuantityEditText = (EditText) findViewById(R.id.edit_product_quantity);
        mProductPhotoView = (ImageView) findViewById(R.id.edit_product_photo);
        mSupplierNameEditText = (EditText) findViewById(R.id.edit_suppliers_name);
        mSupplierEmailEditText = (EditText) findViewById(R.id.edit_suppliers_email);
        mIncreaseQuantityButton = (Button) findViewById(R.id.increase_button);
        mDecreaseQuantityButton = (Button) findViewById(R.id.decrease_button);

        // Attach onTouchListener to these views
        mProductNameEditText.setOnTouchListener(mTouchListener);
        mProductPriceEditText.setOnTouchListener(mTouchListener);
        mProductQuantityEditText.setOnTouchListener(mTouchListener);
        mSupplierNameEditText.setOnTouchListener(mTouchListener);
        mSupplierEmailEditText.setOnTouchListener(mTouchListener);
        mIncreaseQuantityButton.setOnTouchListener(mTouchListener);
        mDecreaseQuantityButton.setOnTouchListener(mTouchListener);

        // On Click listener for the increase quantity button
        mIncreaseQuantityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Call the helper method increaseQuantityByOne
                increaseQuantityByOne();
            }
        });

        // On Click listener for the decrease quantity button
        mDecreaseQuantityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Call the helper method decreaseQuantityByOne
                decreaseQuantityByOne();
            }
        });

        // The onTouchListener for the ImageView that holds the products photo
        // should also perform an intent to the user gallery
        mProductPhotoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mProductHasChanged = true;

                // Call helper method to open user's gallery
                openImageSelector();
            }
        });

        // Inflate the "Order Now" button
        Button orderNowButton = (Button) findViewById(R.id.order_restock_button);

        // Attach on click listener for the "Order Now" button
        orderNowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // On click of "Order Now" button call helper method orderRestockProduct()
                orderRestockProduct();
            }
        });
    }

    /**
     * Helper method that increases the quantity of the product by one
     **/
    private void increaseQuantityByOne() {

        // Get the string value of the EditText with the quantity of the product
        String quantityFromInputString = mProductQuantityEditText.getText().toString();

        // Stores the int value of the quantity in the EditText
        int quantityFromInputInt;

        if (quantityFromInputString.isEmpty()) {

            // if there is no quantity inserted the int value should be 0
            quantityFromInputInt = 0;
        } else {

            // if there is quantity inserted, convert the string value from EditText to int
            quantityFromInputInt = Integer.parseInt(quantityFromInputString);
        }

        // Increase the int value of quantity by one, then convert it to string
        // and set it as text to the EditText view, containing the quantity of the product
        mProductQuantityEditText.setText(String.valueOf(quantityFromInputInt + 1));
    }

    /**
     * Helper method that decreases the quantity of the product by one
     **/
    private void decreaseQuantityByOne() {

        // Get the string value of the EditText with the quantity of the product
        String quantityFromInputString = mProductQuantityEditText.getText().toString();

        // Stores the int value of the quantity in the EditText
        int quantityFromInputInt;

        if (quantityFromInputString.isEmpty()) {

            // if there is no quantity inserted the int value should be 0
            quantityFromInputInt = 0;
        } else {

            // if there is quantity inserted, convert the string value from EditText to int
            quantityFromInputInt = Integer.parseInt(quantityFromInputString);

            if (quantityFromInputInt == 0) {

                // If the quantity is 0 prompt the user to insert a positive value
                Toast.makeText(this, getString(R.string.enter_positive_product_quantity), Toast.LENGTH_SHORT).show();
            } else {

                // Else, decrease the int value of quantity by one, then convert it to string
                // and set it as text to the EditText view, containing the quantity of the product
                mProductQuantityEditText.setText(String.valueOf(quantityFromInputInt - 1));
            }
        }
    }

    /**
     * Helper method that creates e-mail intent for restocking a product
     **/
    private void orderRestockProduct() {

        // Escape early if we are inserting a new product.
        // There is no sense to restock a product that we are just inserting.
        if (mProductPhotoUri == null) {

            // Inform the user that restocking a new product is not available
            Toast.makeText(this, getString(R.string.restocking_new_product), Toast.LENGTH_LONG).show();
            return;
        }

        // Inflate the restock quantity EditText view
        EditText restockQuantityEditText = (EditText) findViewById(R.id.edit_restock_quantity);

        // Get the input from the EditTexts
        String productNameString = mProductNameEditText.getText().toString().trim();
        String supplierNameString = mSupplierNameEditText.getText().toString().trim();
        String supplierEmailString = mSupplierEmailEditText.getText().toString().trim();
        String restockQuantityString = restockQuantityEditText.getText().toString().trim();

        // Check for empty product name field
        if (TextUtils.isEmpty(productNameString)) {

            // Prompt the user to insert product's name
            Toast.makeText(this, getString(R.string.enter_product_name), Toast.LENGTH_SHORT).show();
            return;
        }

        // Check for empty supplier name field
        if (TextUtils.isEmpty(supplierNameString)) {

            // Prompt the user to insert supplier's name
            Toast.makeText(this, getString(R.string.enter_supplier_name), Toast.LENGTH_SHORT).show();
            return;
        }

        // Check for empty supplier e-mail field
        if (TextUtils.isEmpty(supplierEmailString)) {

            // Prompt the user to insert supplier's e-mail
            Toast.makeText(this, getString(R.string.enter_supplier_email), Toast.LENGTH_SHORT).show();
            return;

        }
        // Else, check if the inputted supplier e-mail is in valid format
        else if (!isEmailValid(supplierEmailString)) {

            // Inform the user that the inserted suppliers e-mail is not properly formated
            Toast.makeText(this, getString(R.string.invalid_supplier_email), Toast.LENGTH_LONG).show();
            return;
        }

        // Check for empty restock quantity field
        if (TextUtils.isEmpty(restockQuantityString)) {
            Toast.makeText(this, getString(R.string.enter_restock_quantity), Toast.LENGTH_LONG).show();
            return;

        } else {
            // If the restock quantity field is not empty then convert its string value to int
            int restockQuantityInt = Integer.parseInt(restockQuantityString);

            // Check if the int value of product's restock quantity is negative value
            if (restockQuantityInt <= 0) {

                // If so escape early and prompt the user to insert a positive value
                Toast.makeText(this, getString(R.string.enter_positive_restock_quantity), Toast.LENGTH_LONG).show();
                return;
            }
        }

        // Create restock e-mail subject
        String restockSubject = getString(R.string.ordering) + " " + productNameString;

        // Create restock message to send as E-mail message
        String restockMessage = getString(R.string.hello) + " " + supplierNameString + "\n" +
                getString(R.string.i_would_like_to_order) + " " +
                restockQuantityString + " " +
                getString(R.string.restock_quantity_measurement_units) + " " +
                productNameString;

        // Create an e-mail intent to send an e-mail to the supplier
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:" + supplierEmailString));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, restockSubject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, restockMessage);

        // Start the e-mail intent
        startActivity(Intent.createChooser(emailIntent, "Send Email"));
    }

    /**
     * Helper method that checks if the supplier's e-mail is properly formatted
     *
     * @param supplierEmailString the e-mail of the supplier
     * @return true if the e-mail is the properly formatted
     */
    private boolean isEmailValid(String supplierEmailString) {
        String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(supplierEmailString);
        return matcher.matches();
    }

    // This should handle screen orientation changes
    // Now when screen is rotated the image of the product remains
    // By saving the state of mProductPhotoUri and assign it to the key STATE_URI
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Perform this only if there is already image selected from the gallery
        if (mProductPhotoUri != null)
            outState.putString(STATE_URI, mProductPhotoUri.toString());
    }

    // When activity is restarted on screen orientation change,
    // check if the key STATE_URI for the saved instance of mProductPhotoUri.
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState.containsKey(STATE_URI) &&
                !savedInstanceState.getString(STATE_URI).equals("")) {
            mProductPhotoUri = Uri.parse(savedInstanceState.getString(STATE_URI));

            // Then start viewTreeObserver to get the ImageView object first and then set it a bitmap
            ViewTreeObserver viewTreeObserver = mProductPhotoView.getViewTreeObserver();
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        mProductPhotoView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                    mProductPhotoView.setImageBitmap(getBitmapFromUri(mProductPhotoUri));
                }
            });
        }
    }

    // Helper method that access the user's gallery
    private void openImageSelector() {

        // Create intent object
        Intent intent;

        // For older devices call ACTION_GET_CONTENT
        // which asks the user to to choose a single app from which to pick a file
        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        }

        // For newer devices call ACTION_OPEN_DOCUMENT which displays system-controlled picker UI controlled,
        // that allows the user to browse all files that other apps have made available.
        // CATEGORY_OPENABLE - show only results that can be "opened".
        else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }

        // The type of files we are looking for is images
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_photo)), PICK_IMAGE_REQUEST);
    }

    // Called after the user selects a document in the picker called from openImageSelector()
    // The resultData parameter contains the URI that points to the selected document.
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code PICK_IMAGE_REQUEST.
        // If the request code seen here doesn't match, it's the response to some other intent,
        // and the below code shouldn't run at all.
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {

            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.  Pull that uri using "resultData.getData()"
            if (resultData != null) {
                mProductPhotoUri = resultData.getData();

                // Let me check ih the logcat the uri that was picked from intent
                Log.i(LOG_TAG, "The Uri of picked photo from the user's gallery is: " + mProductPhotoUri.toString());

                // These lines solved the problem with user permission on restart of the emulator
                // Now all the products' photos can be successfully displayed on device restart
                int takeFlags = resultData.getFlags();
                takeFlags &= (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        getContentResolver().takePersistableUriPermission(mProductPhotoUri, takeFlags);
                    }
                } catch (SecurityException e) {
                    e.printStackTrace();
                }

                // Call helper method getBitmapFromUri to convert the uri to bitmap object
                // and then assign the bitmap object to the mProductPhotoView
                mProductPhotoView.setImageBitmap(getBitmapFromUri(mProductPhotoUri));
            }
        }
    }

    // Helper method that converts the photo Uri to bitmap object
    private Bitmap getBitmapFromUri(Uri uri) {
        if (uri == null || uri.toString().isEmpty())
            return null;

        // Get the dimensions of the View where we will visualizer the photo
        int targetW = mProductPhotoView.getWidth();
        int targetH = mProductPhotoView.getHeight();

        InputStream input = null;
        try {
            input = this.getContentResolver().openInputStream(uri);

            // Get the dimensions of the bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();

            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            // Determine how much to scale down the image
            int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;

            input = this.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();

            return bitmap;

        } catch (FileNotFoundException fne) {
            Log.e(LOG_TAG, "Failed to load image.", fne);
            return null;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to load image.", e);
            return null;
        } finally {
            try {
                input.close();
            } catch (IOException ioe) {

            }
        }
    }

    // Called from InvalidateOptionsMenu
    // This method removes the "Delete" option when new product is inserting
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
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

        // This adds menu items ( menu options) to the app bar.
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

                // Call the saveProduct() method,
                // which will insert a new product or update the databse
                saveProduct();

                return true;

            // On click of "Delete" option
            case R.id.action_delete:

                // Call delete confirmation dialog and from it call deleteProduct() method
                showDeleteConfirmationDialog();

                return true;

            case android.R.id.home:

                // Check for changes - mProductHasChange for false return to MainActivity, for true show Dialog
                // Not changes was made, ( mProductHasChanged is still false ) so return to MainActivity
                if (!mProductHasChanged) {
                    NavUtils.navigateUpFromSameTask(DetailActivity.this);
                    return true;
                }

                // Create an on click listener for Dialog that will display,
                // when the user has edited any filed ( mProductHasChanged become true )
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                // This listener will bring the user back to MainActivity
                                NavUtils.navigateUpFromSameTask(DetailActivity.this);
                            }
                        };

                // Now show this Dialog to prompt the user
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Get users' input and save it into the database
    private void saveProduct() {

        // Get the input from the EditText
        String productNameString = mProductNameEditText.getText().toString().trim();
        String productQuantityString = mProductQuantityEditText.getText().toString().trim();
        String productPriceString = mProductPriceEditText.getText().toString().trim();
        String supplierNameString = mSupplierNameEditText.getText().toString().trim();
        String supplierEmailString = mSupplierEmailEditText.getText().toString().trim();

        // Check if this is supposed to be a new product
        // and check if all the fields in the Detail Activity are empty
        if (mCurrentProductUri == null &&
                TextUtils.isEmpty(productNameString) && TextUtils.isEmpty(productQuantityString) &&
                TextUtils.isEmpty(productPriceString) && TextUtils.isEmpty(supplierNameString) &&
                TextUtils.isEmpty(supplierEmailString) && mProductPhotoUri == null) {

            // Since no fields were modified and no photo is inserted,
            // we can return early without creating a new product.
            // No need to create ContentValues and no need to do any ContentProvider operations.
            // Close the activity and escape early
            finish();

            return;
        }

        // Check for empty product name field
        if (TextUtils.isEmpty(productNameString)) {
            Toast.makeText(this, getString(R.string.enter_product_name), Toast.LENGTH_LONG).show();
            return;
        }

        // Check for empty price field
        if (TextUtils.isEmpty(productPriceString)) {
            Toast.makeText(this, getString(R.string.enter_product_price), Toast.LENGTH_LONG).show();
            return;

        } else {
            // If the price field is not empty then convert its string value to float
            mProductPriceFloat = Float.parseFloat(productPriceString);

            // Now check if the price is a negative number
            if (mProductPriceFloat <= 0) {

                // Then escape early and prompt the user to insert a positive value
                Toast.makeText(this, getString(R.string.enter_positive_product_price), Toast.LENGTH_LONG).show();
                return;
            }
        }

        // Check for empty quantity field
        if (TextUtils.isEmpty(productQuantityString)) {
            Toast.makeText(this, getString(R.string.enter_product_quantity), Toast.LENGTH_LONG).show();
            return;

        } else {
            // If the quantity field is not empty then convert its string value to int
            mProductQuantityInt = Integer.parseInt(productQuantityString);

            // Check if the int value of product's quantity is negative value
            if (mProductQuantityInt < 0) {

                // Then escape early and prompt the user to insert a positive value
                Toast.makeText(this, getString(R.string.enter_positive_product_quantity), Toast.LENGTH_LONG).show();
                return;
            }
        }

        // Convert the photo uri to string, because DB accepts strings not uris.
        mProductPhotoString = mProductPhotoUri.toString();

        // Check for empty image of the product
        // Check for default drawable image of a dummy product
        if (mProductPhotoUri == null || mProductPhotoString.equals("no image")) {

            // If there is no uri for the photo of the product, then the user have not selected one yet.
            // So prompt the user to select a photo for the product.
            Toast.makeText(this, getString(R.string.enter_product_photo), Toast.LENGTH_LONG).show();
            return;
        }

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_NAME, productNameString);
        values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, mProductQuantityInt);
        values.put(ProductEntry.COLUMN_PRODUCT_PRICE, mProductPriceFloat);
        values.put(ProductEntry.COLUMN_PRODUCT_PHOTO_URI, mProductPhotoString);
        values.put(ProductEntry.COLUMN_SUPPLIER_NAME, supplierNameString);
        values.put(ProductEntry.COLUMN_SUPPLIER_EMAIL, supplierEmailString);

        // Check if we are creating new product or updating an existing one
        // Look at the uri passed from MainActivity, if it is null then we are inserting new product
        if (mCurrentProductUri == null) {

            // Insert the new row, returning the primary key value of the new row
            Uri returnedUri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful
            if (returnedUri == null) {
                Toast.makeText(this, getString(R.string.save_product_failed), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.save_product_successful), Toast.LENGTH_SHORT).show();

                // The insert was successful so close this activity
                finish();
            }

            // Show in the log cat the key of the new inserted row
            Log.i(LOG_TAG, "The new inserted row key is: " + returnedUri);

        } else {

            // Update the existing product
            mRowsUpdated = getContentResolver().update(mCurrentProductUri, values, null, null);

            // Show a toast message depending on whether or not the update was successful
            if (mRowsUpdated == 0) {
                Toast.makeText(this, getString(R.string.update_product_failed), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.update_product_successful), Toast.LENGTH_SHORT).show();

                // The update was successful so close this activity
                finish();
            }
        }
    }

    // This method shows alert dialog before deleting a product.
    // On click of positive ("Delete") button a helper method deleteProduct() is called.
    private void showDeleteConfirmationDialog() {

        // Build an alert dialog object and set it a message
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);

        // Create an on click listener for the positive button ("Delete")
        // calling the helper method deleteProduct()
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                // User clicked the "Delete" button, so delete the product.
                deleteProduct();
            }
        });

        // Set title and on click listener for the negative button ("Cancel")
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Helper method that deletes product from the database
     */
    private void deleteProduct() {

        // Stores the number of rows that are deleted
        int mRowsDeleted = 0;

        // Only perform the delete if this is an existing product.
        if (mCurrentProductUri != null) {

            // Deletes the product that match the selection criteria
            mRowsDeleted = getContentResolver().delete(
                    mCurrentProductUri,     // URI of the rproduct we want ot delete
                    null,                   // the column to select on
                    null                    // the value to compare to
            );
        }

        // Check if the product was successfully deleted, by checking the number of rows deleted.
        // If they are different from 0, then deletion was successfully.
        if (mRowsDeleted != 0) {

            // Show message to inform the user that the pet was deleted
            Toast.makeText(this, getString(R.string.detail_delete_product_successful), Toast.LENGTH_SHORT).show();
        } else {

            // Show message to inform the user that the app was not able to delete the product
            Toast.makeText(this, getString(R.string.detail_delete_product_failed), Toast.LENGTH_SHORT).show();
        }

        // Call finish to close the cursor and return to previous activity
        finish();
    }

    // This method is called when the physical back button is pressed.
    @Override
    public void onBackPressed() {

        // If no change was made to the product ( mProductHasChange is still false)
        // just return to previous activity
        if (!mProductHasChanged) {
            super.onBackPressed();
            return;
        }

        // But if mProductHasChanged is true ( registered by mTouchListener ),
        // then create a listener for the discard option of the dialog that will be displayed
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        // User has clicked on discard
                        // so dismiss the dialog and close the cursor object
                        finish();
                    }
                };

        // Show dialog to inform the user that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    // This dialog will be displayed when the user clicks on back button
    // or uses the physical back button of the device
    // It has as an argument, an already predefined on click listener on the discard option
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Set message for the dialog
        builder.setMessage(R.string.unsaved_changes_dialog_msg);

        // Set a title and behaviour on click of the positive button ("Discard")
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);

        // Set a title and behaviour on click of the negative button ("Keep editing")
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                // The user has clicked "Keep Editing" so stay in the Detail activity
                // just dismiss the dialog
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create the alert dialog and display it
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    // Create the loader
    // Set projection, the fields from the database we want to visualize on the UI
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Detail Activity will display all product's attributes except "Restock quantity"
        // which I have not even included into the database.
        // See ProductContract comments for more info.
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_PHOTO_URI,
                ProductEntry.COLUMN_SUPPLIER_NAME,
                ProductEntry.COLUMN_SUPPLIER_EMAIL};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentProductUri,     // Query the content URI for the current product;
                projection,             // Columns to include in the resulting Cursor;
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    // Loader has finished its job on a background
    // Extract the data and visualize it on the UI
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {

            // Find the column index of the attributes we are interested in displaying
            int productNameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
            int productQuantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);
            int productPriceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
            int productPhotoUriColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PHOTO_URI);
            int supplierColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_SUPPLIER_NAME);
            int supplierEmailColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_SUPPLIER_EMAIL);

            // Extract out the value from the Cursor for the given column index
            String productName = cursor.getString(productNameColumnIndex);
            int productQuantity = cursor.getInt(productQuantityColumnIndex);
            float productPrice = cursor.getFloat(productPriceColumnIndex);
            String productPhotoUri = cursor.getString(productPhotoUriColumnIndex);
            String supplierName = cursor.getString(supplierColumnIndex);
            String supplierEmail = cursor.getString(supplierEmailColumnIndex);

            // Get the string value of the photo uri from the database
            // convert it to uri and assign it to the member variable
            mProductPhotoUri = Uri.parse(productPhotoUri);

            // Update the fields on the screen with the data from the cursor
            mProductNameEditText.setText(productName);
            mProductQuantityEditText.setText(Integer.toString(productQuantity));
            mProductPriceEditText.setText(Float.toString(productPrice));
            mSupplierNameEditText.setText(supplierName);
            mSupplierEmailEditText.setText(supplierEmail);

            // Check if we are visualizing a dummy product,
            // this means that it will have no photo,
            // so the productPhotoUri should be equal to default value "no image"
            if (productPhotoUri.equals("no image")) {

                // If it there is no image due to dummy data,
                // then set default drawable for the photo of the product
                mProductPhotoView.setImageResource(R.drawable.add_photo_placeholder);
            } else {

                // Call helper method getBitmapFromUri to convert the uri to bitmap object
                // The argument should be uri that is why productPhotoUri is converted from string to uri
                // When bitmap object is returned, set it as image on the mProductPhotoView
                mProductPhotoView.setImageURI(mProductPhotoUri);
            }
        }
    }

    // This is called when the last Cursor provided to onLoadFinished()
    // above is about to be closed.  We need to make sure we are no
    // longer using it.
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        // Set empty values to the field
        mProductNameEditText.setText("");
        mProductQuantityEditText.setText("");
        mProductPriceEditText.setText("");
        mSupplierNameEditText.setText("");
        mSupplierEmailEditText.setText("");

        // Set default drawable for the photo of the product
        mProductPhotoView.setImageResource(R.drawable.add_photo_placeholder);
    }
}
