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

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

/**
 * Main activity.
 * @author Pixmob
 */
public class Feedme extends FragmentActivity implements LoaderCallbacks<Cursor>,
        EntriesFragment.OnEntrySelectionListener {
    private View entryDetails;
    private TextView entryDetailsTitle;
    private WebView browser;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        final EntriesFragment entriesFragment = (EntriesFragment) getSupportFragmentManager()
                .findFragmentById(R.id.entries);
        if (entriesFragment != null) {
            entriesFragment.setOnEntrySelectionListener(this);
        }
        
        entryDetails = findViewById(R.id.entry_details);
        entryDetailsTitle = (TextView) findViewById(R.id.entry_details_title);
        browser = (WebView) findViewById(R.id.browser);
    }
    
    @Override
    public void onEntrySelected(Uri entryUri) {
        final Bundle args = new Bundle(1);
        args.putString("entryUri", entryUri.toString());
        getSupportLoaderManager().initLoader(0, args, this);
    }
    
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final Uri entryUri = Uri.parse(args.getString("entryUri"));
        return new CursorLoader(this, entryUri, new String[] { Entries.TITLE, Entries.SOURCE,
                Entries.PUBLISHED, Entries.URL }, null, null, null);
    }
    
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data == null || !data.moveToNext()) {
            entryDetails.setVisibility(View.INVISIBLE);
        } else {
            final String entryUrl = data.getString(data.getColumnIndexOrThrow(Entries.URL));
            if (browser == null) {
                final Intent i = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(entryUrl));
                startActivity(i);
            } else {
                browser.loadUrl(entryUrl);
                entryDetailsTitle
                        .setText(data.getString(data.getColumnIndexOrThrow(Entries.TITLE)));
                entryDetails.setVisibility(View.VISIBLE);
            }
        }
        
        getSupportLoaderManager().destroyLoader(0);
    }
    
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        entryDetails.setVisibility(View.INVISIBLE);
    }
}
