package com.example.dinesphere

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "DineSphereUser.db"
        private const val DATABASE_VERSION = 3 // Version bumped to 3 for new Menu tables

        // --- Session Table ---
        private const val TABLE_SESSION = "user_session"
        private const val COLUMN_ID = "id"
        private const val COLUMN_USER_ID = "user_id"

        // --- Restaurant Cache ---
        private const val TABLE_RESTAURANTS = "restaurants_cache"
        private const val COL_RES_ID = "restaurant_id"
        private const val COL_NAME = "business_name"
        private const val COL_ADDR = "address"
        private const val COL_IMG = "image_url"
        private const val COL_LAT = "latitude"
        private const val COL_LNG = "longitude"
        private const val COL_RATING = "rating"
        private const val COL_DIST = "distance_km"
        private const val COL_IS_SAVED = "is_saved"
        private const val COL_DISCOUNT = "discount"
        private const val COL_PHONE = "phone"

        // --- Pending Actions ---
        private const val TABLE_PENDING = "pending_actions"
        private const val COL_ACTION_TYPE = "action_type"
        private const val COL_TARGET_ID = "target_id"

        // --- Category Cache (NEW) ---
        private const val TABLE_CATEGORIES = "categories_cache"
        private const val COL_CAT_ID = "category_id"
        private const val COL_CAT_RES_ID = "restaurant_id" // Link to restaurant
        private const val COL_CAT_NAME = "category_name"
        private const val COL_CAT_IMG = "category_image"
        private const val COL_CAT_COUNT = "item_count"

        // --- Menu Items Cache (NEW) ---
        private const val TABLE_MENU = "menu_items_cache"
        private const val COL_MENU_ID = "menu_id"
        private const val COL_MENU_CAT_ID = "category_id" // Link to category
        private const val COL_MENU_NAME = "menu_name"
        private const val COL_MENU_DESC = "menu_description"
        private const val COL_MENU_PRICE = "menu_price"
        private const val COL_MENU_IMG = "menu_image"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 1. Session
        db.execSQL("CREATE TABLE $TABLE_SESSION ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_USER_ID TEXT)")

        // 2. Restaurants
        db.execSQL("""
            CREATE TABLE $TABLE_RESTAURANTS (
                $COL_RES_ID INTEGER PRIMARY KEY,
                $COL_NAME TEXT, $COL_ADDR TEXT, $COL_IMG TEXT, $COL_LAT REAL, $COL_LNG REAL,
                $COL_RATING REAL, $COL_DIST REAL, $COL_IS_SAVED INTEGER, $COL_DISCOUNT TEXT, $COL_PHONE TEXT
            )
        """.trimIndent())

        // 3. Pending Actions
        db.execSQL("CREATE TABLE $TABLE_PENDING ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_ACTION_TYPE TEXT, $COL_TARGET_ID INTEGER)")

        // 4. Categories (NEW)
        db.execSQL("""
            CREATE TABLE $TABLE_CATEGORIES (
                $COL_CAT_ID INTEGER PRIMARY KEY,
                $COL_CAT_RES_ID INTEGER,
                $COL_CAT_NAME TEXT,
                $COL_CAT_IMG TEXT,
                $COL_CAT_COUNT INTEGER
            )
        """.trimIndent())

        // 5. Menu Items (NEW)
        db.execSQL("""
            CREATE TABLE $TABLE_MENU (
                $COL_MENU_ID INTEGER PRIMARY KEY,
                $COL_MENU_CAT_ID INTEGER,
                $COL_MENU_NAME TEXT,
                $COL_MENU_DESC TEXT,
                $COL_MENU_PRICE REAL,
                $COL_MENU_IMG TEXT
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SESSION")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RESTAURANTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PENDING")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CATEGORIES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MENU")
        onCreate(db)
    }

    // ================= EXISTING SESSION & RESTAURANT METHODS =================

    fun saveUserId(userId: String): Boolean {
        val db = writableDatabase
        db.delete(TABLE_SESSION, null, null)
        val values = ContentValues().apply { put(COLUMN_USER_ID, userId) }
        val res = db.insert(TABLE_SESSION, null, values) != -1L
        db.close()
        return res
    }

    fun getUserId(): String? {
        val db = readableDatabase
        val cursor = db.query(TABLE_SESSION, arrayOf(COLUMN_USER_ID), null, null, null, null, null)
        var userId: String? = null
        if (cursor.moveToFirst()) userId = cursor.getString(0)
        cursor.close()
        db.close()
        return userId
    }

    fun clearSession() {
        val db = writableDatabase
        db.delete(TABLE_SESSION, null, null)
        db.close()
    }

    fun cacheRestaurants(list: List<Restaurant>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (res in list) {
                val values = ContentValues().apply {
                    put(COL_RES_ID, res.restaurantId)
                    put(COL_NAME, res.businessName)
                    put(COL_ADDR, res.address)
                    put(COL_IMG, res.imageUrl)
                    put(COL_LAT, res.latitude)
                    put(COL_LNG, res.longitude)
                    put(COL_RATING, res.rating)
                    put(COL_DIST, res.distanceKm)
                    put(COL_IS_SAVED, if (res.isSaved) 1 else 0)
                    put(COL_DISCOUNT, res.discount)
                    put(COL_PHONE, res.phone)
                }
                db.insertWithOnConflict(TABLE_RESTAURANTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    fun getCachedRestaurants(): List<Restaurant> {
        val list = mutableListOf<Restaurant>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_RESTAURANTS", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(Restaurant(
                    restaurantId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_RES_ID)),
                    businessName = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
                    address = cursor.getString(cursor.getColumnIndexOrThrow(COL_ADDR)),
                    phone = cursor.getString(cursor.getColumnIndexOrThrow(COL_PHONE)),
                    latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LAT)),
                    longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LNG)),
                    imageUrl = cursor.getString(cursor.getColumnIndexOrThrow(COL_IMG)),
                    discount = cursor.getString(cursor.getColumnIndexOrThrow(COL_DISCOUNT)),
                    distanceKm = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_DIST)),
                    rating = cursor.getFloat(cursor.getColumnIndexOrThrow(COL_RATING)),
                    isSaved = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_SAVED)) == 1
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    fun addPendingAction(actionType: String, restaurantId: Int) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ACTION_TYPE, actionType)
            put(COL_TARGET_ID, restaurantId)
        }
        db.insert(TABLE_PENDING, null, values)
        db.close()
        updateLocalSaveStatus(restaurantId, actionType == "SAVE")
    }

    private fun updateLocalSaveStatus(id: Int, isSaved: Boolean) {
        val db = writableDatabase
        val values = ContentValues().apply { put(COL_IS_SAVED, if (isSaved) 1 else 0) }
        db.update(TABLE_RESTAURANTS, values, "$COL_RES_ID=?", arrayOf(id.toString()))
        db.close()
    }

    fun getPendingActions(): List<Pair<String, Int>> {
        val list = mutableListOf<Pair<String, Int>>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_PENDING", null)
        if (cursor.moveToFirst()) {
            do {
                val type = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACTION_TYPE))
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TARGET_ID))
                list.add(Pair(type, id))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    fun clearPendingActions() {
        val db = writableDatabase
        db.delete(TABLE_PENDING, null, null)
        db.close()
    }

    // ================= NEW METHODS FOR MENU & CATEGORIES =================

    fun cacheCategories(restaurantId: Int, list: List<Category>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (item in list) {
                val values = ContentValues().apply {
                    put(COL_CAT_ID, item.categoryId)
                    put(COL_CAT_RES_ID, restaurantId)
                    put(COL_CAT_NAME, item.categoryName)
                    put(COL_CAT_IMG, item.categoryImage)
                    put(COL_CAT_COUNT, item.itemCount)
                }
                db.insertWithOnConflict(TABLE_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    fun getCachedCategories(restaurantId: Int): List<Category> {
        val list = mutableListOf<Category>()
        val db = readableDatabase
        val cursor = db.query(TABLE_CATEGORIES, null, "$COL_CAT_RES_ID = ?", arrayOf(restaurantId.toString()), null, null, null)
        if (cursor.moveToFirst()) {
            do {
                list.add(Category(
                    categoryId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CAT_ID)),
                    categoryName = cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT_NAME)),
                    categoryImage = cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT_IMG)),
                    itemCount = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CAT_COUNT)),
                    isSelected = false
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    fun cacheMenuItems(categoryId: Int, list: List<MenuItem>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (item in list) {
                val values = ContentValues().apply {
                    put(COL_MENU_ID, item.menuId)
                    put(COL_MENU_CAT_ID, categoryId)
                    put(COL_MENU_NAME, item.menuName)
                    put(COL_MENU_DESC, item.menuDescription)
                    put(COL_MENU_PRICE, item.menuPrice)
                    put(COL_MENU_IMG, item.menuImage)
                }
                db.insertWithOnConflict(TABLE_MENU, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.close()
        }
    }


    fun getCachedMenuItems(categoryId: Int): List<MenuItem> {
        val list = mutableListOf<MenuItem>()
        val db = readableDatabase
        val cursor = db.query(TABLE_MENU, null, "$COL_MENU_CAT_ID = ?", arrayOf(categoryId.toString()), null, null, null)
        if (cursor.moveToFirst()) {
            do {
                list.add(MenuItem(
                    menuId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_MENU_ID)),
                    menuName = cursor.getString(cursor.getColumnIndexOrThrow(COL_MENU_NAME)),
                    menuDescription = cursor.getString(cursor.getColumnIndexOrThrow(COL_MENU_DESC)),
                    menuPrice = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_MENU_PRICE)),
                    menuImage = cursor.getString(cursor.getColumnIndexOrThrow(COL_MENU_IMG))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

}