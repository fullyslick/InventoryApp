package com.example.user.inventoryapp.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by Alexander Rashkov on 10.07.17.
 */

public class ProductContract {

    // Declare "Content authority"
    public static final String CONTENT_AUTHORITY = "com.example.user.inventoryapp";

    /**
     * Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
     * the content provider.
     */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // Possible path (appended to base content URI for possible URI's)
    public static final String PATH_PRODUCTS = "products";

    // Preventing someone from accidentally instantiating the contract class,
    // by giving it an empty constructor.
    private ProductContract() {
    }

    public static abstract class ProductEntry implements BaseColumns {
        /**
         * The content URI to access the product data in the provider
         * CONTENT_URI = content://com.example.user.inventoryapp/products
         */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_PRODUCTS);

        // Name of database table for products
        public static final String TABLE_NAME = "products";

        // Unique ID number for the product (only for use in the database table).
        public static final String _ID = BaseColumns._ID;

        // Declare table rows name constants
        public static final String COLUMN_PRODUCT_NAME = "product_name";
        public static final String COLUMN_PRODUCT_QUANTITY = "quantity";
        public static final String COLUMN_PRODUCT_PRICE = "price";
        public static final String COLUMN_PRODUCT_PHOTO_URI = "photo_uri";
        public static final String COLUMN_SUPPLIER_NAME = "supplier_name";
        public static final String COLUMN_SUPPLIER_EMAIL = "supplier_email";

        // I do not have a column for the "Restock Quantity" because I assume
        // that the user will want a different restock quantity every time,
        // so no need to save any value into the database for that.

        // The MIME type of the {@link #CONTENT_URI} for a list of products.
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCTS;

        // The MIME type of the {@link #CONTENT_URI} for a single product.
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCTS;
    }
}
