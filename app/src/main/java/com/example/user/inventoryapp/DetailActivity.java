package com.example.user.inventoryapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
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

/**
 * Created by Alexander Rashkov on 12.07.17.
 */

//  Allows user to insert a new product or edit an existing one.

public class DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String STATE_URI = "STATE_URI";

    // Tag for the log messages
    public static final String LOG_TAG = DetailActivity.class.getSimpleName();

    // The id of the loader
    private static final int EXISTING_PRODUCT_LOADER = 2;
    // Request code intent that gets the image from the users' gallery
    private static final int PICK_IMAGE_REQUEST = 0;
    // Content URI for the existing product (null if it's a new product)
    private Uri mCurrentProductUri;

    // Uri that will be converted to bitmap and then displayed
    private Uri mProductPhotoUri;

    // Boolean flag that keeps track of whether the product has been edited (true) or not (false)
    private boolean mProductHasChanged = false;

    // Defines a variable to contain the number of updated rows
    int mRowsUpdated = 0;

    // EditText field to enter product's name
    private EditText mProductNameText;

    // EditText field to enter product's price
    private EditText mProductPriceText;

    // EditText field to enter product's quantity
    private EditText mProductQuantityText;

    // ImageView that holds the image of the product
    private ImageView mProductPhotoView;

    // EditText field to enter supplier for the product
    private EditText mSupplierNameText;

    // EditText field to enter supplier for the product
    private EditText mSupplierEmailText;

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

        // Apply the extracted uri to the member variable mCurrentProductUri
        mCurrentProductUri = intent.getData();

        // Show me in the logcat the uri extracted from the intent
        Log.i(LOG_TAG, "The passed uri from MainActivity is: " + mCurrentProductUri);

        // If the intent DOES NOT contain a any URI ( null ), then we know that we are
        // creating a new product, because ony clicking on ListItem (product) passes any uri
        if (mCurrentProductUri == null) {
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
            getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mProductNameText = (EditText) findViewById(R.id.edit_product_name);
        mProductPriceText = (EditText) findViewById(R.id.edit_product_price);
        mProductQuantityText = (EditText) findViewById(R.id.edit_product_quantity);
        mProductPhotoView = (ImageView) findViewById(R.id.edit_product_photo);
        mSupplierNameText = (EditText) findViewById(R.id.edit_suppliers_name);
        mSupplierEmailText = (EditText) findViewById(R.id.edit_suppliers_email);
        mIncreaseQuantityButton = (Button) findViewById(R.id.increase_button);
        mDecreaseQuantityButton = (Button) findViewById(R.id.decrease_button);

        // Attach onTouchListener to these views
        mProductNameText.setOnTouchListener(mTouchListener);
        mProductPriceText.setOnTouchListener(mTouchListener);
        mProductQuantityText.setOnTouchListener(mTouchListener);
        mSupplierNameText.setOnTouchListener(mTouchListener);
        mSupplierEmailText.setOnTouchListener(mTouchListener);
        mIncreaseQuantityButton.setOnTouchListener(mTouchListener);
        mDecreaseQuantityButton.setOnTouchListener(mTouchListener);

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

    }

    // This should handle screen orientation changes
    // Now when screen is rotated the image of the product remains
    // Because STATE_URI is put on onSaveInstanceState
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mProductPhotoUri != null)
            outState.putString(STATE_URI, mProductPhotoUri.toString());
    }

    // If there is STATE_URI ( nSaveInstanceState was called when screen was rotated )
    // start viewTreeObserver to get the ImageView object first and then set it a bitmap
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState.containsKey(STATE_URI) &&
                !savedInstanceState.getString(STATE_URI).equals("")) {
            mProductPhotoUri = Uri.parse(savedInstanceState.getString(STATE_URI));

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

    // Helper method that access the user gallery
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
                Log.i(LOG_TAG, "The Photo Uri picked from the gallery is " + mProductPhotoUri.toString());

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

        // Get the dimensions of the View
        int targetW = mProductPhotoView.getWidth();
        int targetH = mProductPhotoView.getHeight();

        // TO:DO FIX
        if(targetH == 0) targetH = 1;
        if(targetW == 0) targetW = 1;

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

                // Call the saveProduct() method, which checks editText fields and the ImageView, and insert or update db
                saveProduct();

                //Exit Activity
                finish();

                return true;

            // On click of "Delete" option
            case R.id.action_delete:

                // Call delete confirmation dialog and from it call deleteProduct() method
                // TO:DO

                return true;

            case android.R.id.home:

                // Check for changes - mProductHasChange for false return to MainActivity, for true show Dialog
                // Not changes was made, ( mProductHasChanged is still false ) so return to MainActivity
                if (!mProductHasChanged) {

                    // TO:DO Comment
                    onBackPressed();
                    return true;
                }

                // Create an on click listener for Dialog that will display,
                // when the user has edited any filed ( mProductHasChanged become true )
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {


                                // TO:DO Comment
                                finish();
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
        String productNameString = mProductNameText.getText().toString().trim();
        String productQuantityString = mProductQuantityText.getText().toString().trim();
        String productPriceString = mProductPriceText.getText().toString().trim();
        String supplierNameString = mSupplierNameText.getText().toString().trim();
        String supplierEmailString = mSupplierEmailText.getText().toString().trim();

        // Check if this is supposed to be a new product
        // and check if all the fields in the Detail Activity are blankCheck if fields are empty.
        if ( mCurrentProductUri == null &&
                TextUtils.isEmpty(productNameString) && TextUtils.isEmpty(productQuantityString) &&
                TextUtils.isEmpty(productPriceString) && TextUtils.isEmpty(supplierNameString) &&
                TextUtils.isEmpty(supplierEmailString) && mProductPhotoUri == null ){

            // Since no fields were modified and no photo is inserted,
            // we can return early without creating a new product.
            // No need to create ContentValues and no need to do any ContentProvider operations.
            return;
        }

          // Convert the Strings of product's quantity and price into a appropriate data type for the database
          int productQuantityInt = Integer.parseInt(productQuantityString);
          float productPriceFloat = Float.parseFloat(productPriceString);

          // Convert the photo uri to string, because DB accepts string not uri
          String productPhotoString = mProductPhotoUri.toString();

        // TO:DO Should check all fields for empty strings or default values or null for product image Photo (ImagesView)
        // TO:DO And show Toast Messages for every input the is not filed after every toast message there should be return, so if there is an empty filed the saveProduct() is escaped and npt saving int othe database
        // TO:DO Quantity and Price should not be a negative value - make toast message for that too

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_NAME, productNameString);
        values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, productQuantityInt);
        values.put(ProductEntry.COLUMN_PRODUCT_PRICE, productPriceFloat);
        values.put(ProductEntry.COLUMN_PRODUCT_PHOTO_URI, productPhotoString);
        values.put(ProductEntry.COLUMN_SUPPLIER_NAME, supplierNameString);
        values.put(ProductEntry.COLUMN_SUPPLIER_EMAIL, supplierEmailString);

        // Check if we are creating new product or updating an existing one
        // Check the uri passed from MainActivity,
        // if it is null then we are inserting new product
        if (mCurrentProductUri == null ){

            // Insert the new row, returning the primary key value of the new row
            Uri returnedUri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful
            if (returnedUri == null) {
                Toast.makeText(this, getString(R.string.save_product_failed), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.save_product_successful), Toast.LENGTH_SHORT).show();
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
            }
        }
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
            mProductNameText.setText(productName);
            mProductQuantityText.setText(Integer.toString(productQuantity));
            mProductPriceText.setText(Float.toString(productPrice));
            mSupplierNameText.setText(supplierName);
            mSupplierEmailText.setText(supplierEmail);

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
                mProductPhotoView.setImageBitmap(getBitmapFromUri(mProductPhotoUri));
            }

        }
    }

    // This is called when the last Cursor provided to onLoadFinished()
    // above is about to be closed.  We need to make sure we are no
    // longer using it.
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        // Set empty values to the field
        mProductNameText.setText("");
        mProductQuantityText.setText("");
        mProductPriceText.setText("");
        mSupplierNameText.setText("");
        mSupplierEmailText.setText("");

        // Set default drawable for the photo of the product
        mProductPhotoView.setImageResource(R.drawable.add_photo_placeholder);
    }
}
