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
package org.pixmob.feedme.ui;

import org.pixmob.feedme.R;
import org.pixmob.feedme.provider.FeedmeContract.Entries;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * {@link CursorAdapter} implementation for displaying a feed entry.
 * @author Pixmob
 */
class EntryCursorAdapter extends CursorAdapter {
    public static final int ID_TAG = R.id.entry_title;
    public static final int IMAGE_TAG = R.id.entry_image;
    
    public EntryCursorAdapter(final Context context) {
        super(context, null, 0);
    }
    
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final int entryId = cursor.getInt(cursor.getColumnIndex(Entries._ID));
        final String entryTitle = cursor.getString(cursor.getColumnIndex(Entries.TITLE));
        final String entrySource = cursor.getString(cursor.getColumnIndex(Entries.SOURCE));
        final String imagePath = cursor.getString(cursor.getColumnIndex(Entries.IMAGE));
        
        TextView tv = (TextView) view.findViewById(R.id.entry_title);
        tv.setText(entryTitle);
        
        tv = (TextView) view.findViewById(R.id.entry_source);
        tv.setText(entrySource);
        
        view.setTag(ID_TAG, entryId);
        view.setTag(IMAGE_TAG, imagePath);
    }
    
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(R.layout.entry_row, parent, false);
    }
}
