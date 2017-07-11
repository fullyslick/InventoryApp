package com.example.user.inventoryapp;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.user.inventoryapp.data.ProductContract.ProductEntry;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Alexander Rashkov on 11.07.17.
 */

/**
 * {@link ProductCursorAdapter} is an adapter for a list or grid view
 * that uses a {@link Cursor} of product data as its data source. This adapter knows
 * how to create list items for each row of product data in the {@link Cursor}.
 */
public class ProductCursorAdapter extends CursorAdapter {

    /**
     * Tag for the log messages
     */
    public static final String LOG_TAG = MainActivity.class.getSimpleName();

    // This ImageView object will hold the reference to the ImageView that holds
    // the photo of the product in the ListItem
    ImageView mPhotoImageView;

    /**
     * Constructs a new {@link ProductCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public ProductCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the product data (in the current row pointed by cursor) to the given
     * list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        // Get the id of the current ListItem
        final int id = cursor.getInt(cursor.getColumnIndex(ProductEntry._ID));

        // Find the TextView that will display product's name
        // Get the string value for the product name from the cursor object
        // Populate the productNameTextView
        TextView productNameTextView = (TextView) view.findViewById(R.id.product_name);
        String productName = cursor.getString(cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME));
        productNameTextView.setText(productName);

        // Find the TextView that will display product's quantity
        // Get the int value for the product quantity from the cursor object
        // Convert the int value of productQuantity to string & Populate the quantityTextView
        final TextView quantityTextView = (TextView) view.findViewById(R.id.quantity);
        final int productQuantity = cursor.getInt(cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY));
        quantityTextView.setText(String.valueOf(productQuantity));

        // Find the TextView that will display product's price
        // Get the float value for the product price from the cursor object and then converted it to String
        // Populate the priceTextView
        TextView priceTextView = (TextView) view.findViewById(R.id.price);
        String price = Float.toString(cursor.getFloat(cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE)));
        priceTextView.setText(price);

        // Find the ImageView that will display product's preview picture
        mPhotoImageView = (ImageView) view.findViewById(R.id.list_item_photo);

        // First, get the string value for the image uri from the database,
        // and later if it is different from default string, convert it to uri
        String productImageString = cursor.getString(cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PHOTO_URI));

        // Check if productImageString is equal to the default value, set a drawable resource
        if (productImageString.equals("no image") ) {
            mPhotoImageView.setImageResource(R.drawable.ic_add_a_photo_white_36dp);
        }
        // if it's not default value then convert the productImageString into uri and
        // assign it to a member variable mPhotoUri
        else {
            Uri productImageUri = Uri.parse(productImageString);

            // call helper method getBitmapFromUri to extract a Bitmap from the image uri
            mPhotoImageView.setImageBitmap(getBitmapFromUri(context, productImageUri));
        }

        // Get the sale button view
        Button saleButton = (Button) view.findViewById(R.id.sale_button);

        // Attach a listener to "Sale" button to perform an update on the database
        saleButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                // Create  ContentResolver object to update the database
                ContentResolver resolver = context.getContentResolver();

                // Create ContentValues to select the right "key" to "value" pair to update
                ContentValues values = new ContentValues();

                // If the quantity of the products is more than 0, then we can reduce ot by one
                // We do not want any negative values
                if( productQuantity > 0 ){

                    // Create a new uri for this product ( ListItem)
                    Uri CurrentProductUri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, id);

                    // Present a new variable to send the reduced quantity to database
                    int currentAvailableQuantity = productQuantity;
                    currentAvailableQuantity -= 1;

                    // Assign the new variable of the quantity as a contentValue,
                    // that will be updated into the database
                    values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, currentAvailableQuantity);

                    // Perform an update on the database
                    resolver.update(
                            CurrentProductUri,
                            values,
                            null,
                            null
                    );

                    // Notify all listener to Update the UI
                    // Now the quantity of this product is reduced on the UI
                    context.getContentResolver().notifyChange(CurrentProductUri, null);
                }
               else{
                    // Show a message to the UI to inform the user for the 0 quantity of this product
                    Toast.makeText(v.getContext(), R.string.out_of_stock, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Helper method that will extract the bitmap from the provided uri
    // Needs context to getContentResolver()
    private Bitmap getBitmapFromUri(Context context, Uri uri) {
        if (uri == null || uri.toString().isEmpty())
            return null;

        // Get the dimensions of the View
        int targetW = mPhotoImageView.getWidth();
        int targetH = mPhotoImageView.getHeight();

        InputStream input = null;
        try {
            input = context.getContentResolver().openInputStream(uri);

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

            input = context.getContentResolver().openInputStream(uri);
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
            } catch (IOException ioe) {}
        }
    }
}
