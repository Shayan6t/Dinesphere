package com.example.dinesphere

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "DineSphereUser.db"
        private const val DATABASE_VERSION = 2 // Updated version for new tables

        // --- Session Table Constants (Preserved) ---
        private const val TABLE_SESSION = "user_session"
        private const val COLUMN_ID = "id"
        private const val COLUMN_USER_ID = "user_id"

        // --- Restaurant Cache Table Constants (New) ---
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

        // --- Pending Actions Table Constants (New for Offline Queuing) ---
        private const val TABLE_PENDING = "pending_actions"
        private const val COL_ACTION_TYPE = "action_type" // "SAVE" or "UNSAVE"
        private const val COL_TARGET_ID = "target_id"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 1. Create Session Table (Existing)
        val createSessionTable = """
            CREATE TABLE $TABLE_SESSION (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_ID TEXT
            )
        """.trimIndent()
        db.execSQL(createSessionTable)

        // 2. Create Restaurant Cache Table (New)
        val createRestaurantsTable = """
            CREATE TABLE $TABLE_RESTAURANTS (
                $COL_RES_ID INTEGER PRIMARY KEY,
                $COL_NAME TEXT,
                $COL_ADDR TEXT,
                $COL_IMG TEXT,
                $COL_LAT REAL,
                $COL_LNG REAL,
                $COL_RATING REAL,
                $COL_DIST REAL,
                $COL_IS_SAVED INTEGER,
                $COL_DISCOUNT TEXT,
                $COL_PHONE TEXT
            )
        """.trimIndent()
        db.execSQL(createRestaurantsTable)

        // 3. Create Pending Actions Table (New)
        val createPendingTable = """
            CREATE TABLE $TABLE_PENDING (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_ACTION_TYPE TEXT,
                $COL_TARGET_ID INTEGER
            )
        """.trimIndent()
        db.execSQL(createPendingTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop all tables on upgrade to ensure schema is fresh
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SESSION")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RESTAURANTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PENDING")
        onCreate(db)
    }

    // ==========================================
    // EXISTING SESSION METHODS (PRESERVED)
    // ==========================================

    /**
     * Save (or replace) the user id.
     */
    fun saveUserId(userId: String): Boolean {
        val db = writableDatabase
        try {
            db.beginTransaction()
            db.delete(TABLE_SESSION, null, null) // Clear old session
            val values = ContentValues().apply {
                put(COLUMN_USER_ID, userId)
            }
            val row = db.insert(TABLE_SESSION, null, values)
            if (row == -1L) return false
            db.setTransactionSuccessful()
            return true
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    /**
     * Returns the saved user id, or null if none saved.
     */
    fun getUserId(): String? {
        val db = readableDatabase
        var cursor = db.query(TABLE_SESSION, arrayOf(COLUMN_USER_ID), null, null, null, null, null)
        var userId: String? = null
        cursor.use {
            if (it.moveToFirst()) {
                userId = it.getString(0)
            }
        }
        db.close()
        return userId
    }

    /** Clears stored session. */
    fun clearSession() {
        val db = writableDatabase
        db.delete(TABLE_SESSION, null, null)
        db.close()
    }

    // ==========================================
    // NEW METHODS FOR OFFLINE CAPABILITIES
    // ==========================================

    /**
     * Save a list of restaurants to the local cache.
     * Uses CONFLICT_REPLACE to update existing entries.
     */
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

    /**
     * Retrieve all cached restaurants from SQLite.
     */
    fun getCachedRestaurants(): List<Restaurant> {
        val list = mutableListOf<Restaurant>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_RESTAURANTS", null)

        if (cursor.moveToFirst()) {
            do {
                val res = Restaurant(
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
                )
                list.add(res)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    /**
     * Add an offline action to the queue (e.g., "SAVE" or "UNSAVE").
     * Also updates the local cache immediately so the UI reflects the change.
     */
    fun addPendingAction(actionType: String, restaurantId: Int) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ACTION_TYPE, actionType)
            put(COL_TARGET_ID, restaurantId)
        }
        db.insert(TABLE_PENDING, null, values)
        db.close()

        // Update the local cache so the user sees the change immediately
        updateLocalSaveStatus(restaurantId, actionType == "SAVE")
    }

    /**
     * Helper to update the 'is_saved' status in the local cache.
     */
    private fun updateLocalSaveStatus(id: Int, isSaved: Boolean) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_IS_SAVED, if (isSaved) 1 else 0)
        }
        db.update(TABLE_RESTAURANTS, values, "$COL_RES_ID=?", arrayOf(id.toString()))
        db.close()
    }

    /**
     * Get all pending actions to sync with the server.
     * Returns a list of Pairs: (ActionType, RestaurantID).
     */
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

    /**
     * Clear the pending actions queue (usually called after successful sync).
     */
    fun clearPendingActions() {
        val db = writableDatabase
        db.delete(TABLE_PENDING, null, null)
        db.close()
    }
}