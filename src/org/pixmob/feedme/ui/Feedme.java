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
import android.support.v4.app.FragmentTransaction;
import android.view.View;

/**
 * Main activity.
 * @author Pixmob
 */
public class Feedme extends FragmentActivity implements EntriesFragment.OnEntrySelectionListener {
    private Uri selectedEntryUri;
    private boolean dualPane;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        final EntriesFragment entriesFragment = (EntriesFragment) getSupportFragmentManager()
                .findFragmentById(R.id.entries);
        if (entriesFragment != null) {
            entriesFragment.setOnEntrySelectionListener(this);
        }
        
        final View entryDetailsView = findViewById(R.id.entry_details);
        dualPane = entryDetailsView != null && entryDetailsView.getVisibility() == View.VISIBLE;
    }
    
    @Override
    public void onEntrySelected(Uri entryUri) {
        if (!dualPane) {
            // There is not enough space to display the entry details fragment:
            // start a new activity to show the entry URL.
            final Cursor c = getContentResolver().query(entryUri, new String[] { Entries.URL },
                null, null, null);
            try {
                if (c.moveToNext()) {
                    final String entryUrl = c.getString(c.getColumnIndexOrThrow(Entries.URL));
                    final Intent i = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(entryUrl));
                    startActivity(i);
                }
            } finally {
                c.close();
            }
        } else if (!entryUri.equals(selectedEntryUri)) {
            // Update the entry details fragment.
            final EntryDetailsFragment entryDetailsFragment = EntryDetailsFragment
                    .newInstance(entryUri);
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.entry_details, entryDetailsFragment);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.commit();
        }
        
        selectedEntryUri = entryUri;
    }
}
