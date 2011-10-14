/*
 * Copyright (C) 2011 Pixmob (http://github.com/pixmob)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pixmob.feedme.provider;

import static org.pixmob.feedme.Constants.DEVELOPER_MODE;
import static org.pixmob.feedme.Constants.TAG;

import java.util.ArrayList;

import org.pixmob.feedme.provider.FeedmeContract.Entries;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

/**
 * The content provider for the application database.
 * @author Pixmob
 */
public class FeedmeContentProvider extends ContentProvider {
    private static final String ENTRIES_TABLE = "entries";
    
    private static final int ENTRIES = 1;
    private static final int ENTRY_ID = 2;
    
    private static final UriMatcher URI_MATCHER;
    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(FeedmeContract.AUTHORITY, "entries", ENTRIES);
        URI_MATCHER.addURI(FeedmeContract.AUTHORITY, "entries/*", ENTRY_ID);
    }
    
    private SQLiteOpenHelper dbHelper;
    
    @Override
    public boolean onCreate() {
        try {
            dbHelper = new DatabaseHelper(getContext(), "feedme.db", null, 2);
        } catch (Exception e) {
            Log.e(TAG, "Failed to create database", e);
            return false;
        }
        return true;
    }
    
    @Override
    public String getType(Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case ENTRIES:
                return Entries.CONTENT_TYPE;
            case ENTRY_ID:
                return Entries.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unsupported Uri: " + uri);
        }
    }
    
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        final int count;
        
        switch (URI_MATCHER.match(uri)) {
            case ENTRIES:
                count = db.delete(ENTRIES_TABLE, selection, selectionArgs);
                if (DEVELOPER_MODE) {
                    Log.d(TAG, "All entries were deleted");
                }
                break;
            case ENTRY_ID:
                final String id = uri.getPathSegments().get(1);
                String fullSelection = Entries._ID + "='" + id + "'";
                if (!TextUtils.isEmpty(selection)) {
                    fullSelection += " AND (" + selection + ")";
                }
                count = db.delete(ENTRIES_TABLE, fullSelection, selectionArgs);
                if (DEVELOPER_MODE) {
                    Log.d(TAG, "Entry deleted: " + uri);
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported Uri: " + uri);
        }
        
        getContext().getContentResolver().notifyChange(uri, null, false);
        return count;
    }
    
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        final long rowId = db.insertOrThrow(ENTRIES_TABLE, "notNull", values);
        if (rowId == -1) {
            throw new SQLException("Failed to insert new entry");
        }
        
        final Uri entryUri = Uri.withAppendedPath(Entries.CONTENT_URI, String.valueOf(rowId));
        if (DEVELOPER_MODE) {
            Log.d(TAG, "New entry inserted: " + entryUri);
        }
        getContext().getContentResolver().notifyChange(uri, null, false);
        
        return entryUri;
    }
    
    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        if (operations.isEmpty()) {
            return new ContentProviderResult[0];
        }
        
        // Execute batch operations in a single transaction for performance.
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            final int numOperations = operations.size();
            final ContentProviderResult[] results = new ContentProviderResult[numOperations];
            for (int i = 0; i < numOperations; i++) {
                results[i] = operations.get(i).apply(this, results, i);
            }
            db.setTransactionSuccessful();
            return results;
        } finally {
            db.endTransaction();
        }
    }
    
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        String realSortOrder = sortOrder;
        
        final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (URI_MATCHER.match(uri)) {
            case ENTRIES:
                qb.setTables(ENTRIES_TABLE);
                if (TextUtils.isEmpty(realSortOrder)) {
                    realSortOrder = Entries.PUBLISHED + " DESC";
                }
                break;
            case ENTRY_ID:
                qb.setTables(ENTRIES_TABLE);
                qb.appendWhere(Entries._ID + "=" + uri.getPathSegments().get(1));
                break;
        }
        
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        final Cursor c = qb.query(db, projection, selection, selectionArgs, null, null,
            realSortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        
        return c;
    }
    
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        final int count;
        switch (URI_MATCHER.match(uri)) {
            case ENTRIES:
                count = db.update(ENTRIES_TABLE, values, selection, selectionArgs);
                if (DEVELOPER_MODE) {
                    Log.d(TAG, "All entries were updated");
                }
                break;
            case ENTRY_ID:
                final String id = uri.getPathSegments().get(1);
                String fullSelection = Entries._ID + "='" + id + "'";
                if (!TextUtils.isEmpty(selection)) {
                    fullSelection += " AND (" + selection + ")";
                }
                count = db.update(ENTRIES_TABLE, values, fullSelection, selectionArgs);
                if (DEVELOPER_MODE) {
                    Log.d(TAG, "Entry updated: " + uri);
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported Uri: " + uri);
        }
        
        getContext().getContentResolver().notifyChange(uri, null, false);
        return count;
    }
    
    /**
     * Helper class for managing the application database.
     * @author Pixmob
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String CREATE_ENTRIES_TABLE = "CREATE TABLE " + ENTRIES_TABLE + " ("
                + Entries._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + Entries.GRID + " TEXT, "
                + Entries.SOURCE + " TEXT, " + Entries.PUBLISHED + " LONG, " + Entries.STARRED
                + " INTEGER, " + Entries.TITLE + " TEXT, " + Entries.SUMMARY + " TEXT, "
                + Entries.URL + " TEXT, " + Entries.STATUS + " INTEGER, " + Entries.IMAGE
                + " TEXT);";
        
        public DatabaseHelper(final Context context, final String name,
                final CursorFactory cursorFactory, final int version) {
            super(context, name, cursorFactory, version);
        }
        
        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.i(TAG, "Create database");
            db.execSQL(CREATE_ENTRIES_TABLE);
            
            if (DEVELOPER_MODE) {
                Log.i(TAG, "Insert sample data into database");
                
                final String notNull = "not_null";
                final ContentValues cv = new ContentValues();
                
                cv.put(Entries.GRID, "feed/http://www.androidnews.com/feed/");
                cv.put(Entries.SOURCE, "Android News");
                cv.put(Entries.PUBLISHED, System.currentTimeMillis());
                cv.put(Entries.TITLE, "Feedme 1.0 is out!");
                cv.put(Entries.SUMMARY, "Feedme 1.0 is out! Get this version while it's hot!");
                cv.put(Entries.URL, "http://github.com/pixmob/feedme");
                cv.put(Entries.STATUS, Entries.STATUS_UNREAD);
                db.insertOrThrow(ENTRIES_TABLE, notNull, cv);
            }
        }
        
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Reset database (all data will be destroyed)");
            db.execSQL("DROP TABLE ID EXISTS " + ENTRIES_TABLE);
            onCreate(db);
        }
    }
}
