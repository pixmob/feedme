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
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

/**
 * Display entry details, with a web view.
 * @author Pixmob
 */
public class EntryDetailsFragment extends Fragment implements LoaderCallbacks<Cursor> {
    private TextView entryDetailsTitle;
    private WebView browser;
    private boolean entryDisplayed;
    
    public static EntryDetailsFragment newInstance(Uri entryUri) {
        final EntryDetailsFragment f = new EntryDetailsFragment();
        if (entryUri != null) {
            final Bundle args = new Bundle(1);
            args.putString("entryUri", entryUri.toString());
            f.setArguments(args);
        }
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.add(Menu.NONE, R.string.show_in_browser, 1, R.string.show_in_browser);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.string.show_in_browser:
                onShowInBrowser();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void onShowInBrowser() {
        if (entryDisplayed) {
            final Intent i = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(browser.getUrl()));
            startActivity(i);
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.entry_details, container, false);
        browser = (WebView) v.findViewById(R.id.browser);
        entryDetailsTitle = (TextView) v.findViewById(R.id.entry_details_title);
        return v;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        final Bundle args = getArguments();
        if (args != null) {
            final String entryUri = args.getString("entryUri");
            if (entryUri != null) {
                getLoaderManager().initLoader(0, getArguments(), this);
            }
        }
    }
    
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final Uri entryUri = Uri.parse(args.getString("entryUri"));
        return new CursorLoader(getActivity(), entryUri,
                new String[] { Entries.TITLE, Entries.URL }, null, null, null);
    }
    
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.moveToNext()) {
            final String entryUrl = data.getString(data.getColumnIndex(Entries.URL));
            final String entryTitle = data.getString(data.getColumnIndexOrThrow(Entries.TITLE));
            browser.loadUrl(entryUrl);
            entryDetailsTitle.setText(entryTitle);
            entryDisplayed = true;
            
            getLoaderManager().destroyLoader(0);
        }
    }
    
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}
