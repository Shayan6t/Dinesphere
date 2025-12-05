package com.example.dinesphere

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "DineSphereUser.db" // Changed name for User App
        private const val DATABASE_VERSION = 1

        private const val TABLE_SESSION = "user_session"
        private const val COLUMN_ID = "id"
        private const val COLUMN_USER_ID = "user_id" // Changed to user_id
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_SESSION (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_ID TEXT
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SESSION")
        onCreate(db)
    }

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
}