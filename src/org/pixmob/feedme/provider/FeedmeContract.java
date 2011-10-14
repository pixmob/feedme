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

import java.net.URI;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * The contract between the content provider and applications.
 * @author Pixmob
 */
public class FeedmeContract {
    /**
     * The authority for the content provider.
     */
    public static final String AUTHORITY = "org.pixmob.feedme";
    
    protected static interface EntriesColumns {
        String GRID = "grid";
        String SOURCE = "source";
        String PUBLISHED = "published";
        String TITLE = "title";
        String SUMMARY = "summary";
        String URL = "url";
        String STARRED = "starred";
        String STATUS = "status";
        String IMAGE = "image";
    }
    
    /**
     * Table for feed entries.
     * @author Pixmob
     */
    public static class Entries implements BaseColumns, EntriesColumns {
        /**
         * The content:// style URI for this table.
         */
        public static final Uri CONTENT_URI = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT).authority(AUTHORITY).appendPath("entries")
                .build();
        /**
         * The MIME type of a {@link #CONTENT_URI} subdirectory of a single
         * entry.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/entry";
        /**
         * The MIME type of {@link #CONTENT_TYPE} providing a directory of
         * entries.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/entry";
        /**
         * Status for an unread entry.
         */
        public static final int STATUS_UNREAD = 1;
        /**
         * Status for a read entry.
         */
        public static final int STATUS_READ = 2;
        /**
         * Status for an entry which is about to be deleted.
         */
        public static final int STATUS_PENDING_DELETE = 3;
        /**
         * Status for an entry which is about to be starred.
         */
        public static final int STATUS_PENDING_STARRED = 4;
        
        /**
         * Get a entry {@link URI} from a Google Reader identifier.
         */
        public static Uri getEntryUri(ContentResolver resolver, String grid) {
            final Cursor c = resolver.query(CONTENT_URI, new String[] { _ID }, GRID + "=?",
                new String[] { grid }, null);
            final Uri uri;
            try {
                if (c.moveToNext()) {
                    final int id = c.getInt(c.getColumnIndex(_ID));
                    uri = Uri.withAppendedPath(CONTENT_URI, String.valueOf(id));
                } else {
                    uri = null;
                }
            } finally {
                c.close();
            }
            
            return uri;
        }
    }
}
