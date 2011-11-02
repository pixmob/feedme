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
package org.pixmob.feedme.service;

import static org.pixmob.feedme.Constants.DEVELOPER_MODE;
import static org.pixmob.feedme.Constants.TAG;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.pixmob.actionservice.ActionExecutionFailedException;
import org.pixmob.actionservice.ActionService;
import org.pixmob.feedme.R;
import org.pixmob.feedme.net.NetworkClient;
import org.pixmob.feedme.provider.FeedmeContract;
import org.pixmob.feedme.provider.FeedmeContract.Entries;
import org.pixmob.feedme.ui.Feedme;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

/**
 * Download new entries from Google Reader. Entries are stored in the local
 * database.
 * @author Pixmob
 */
public class EntriesDownloadService extends ActionService {
    private PendingIntent openEntriesIntent;
    
    public EntriesDownloadService() {
        super("Feedme/EntriesDownload");
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        openEntriesIntent = PendingIntent.getActivity(this, 0, new Intent(this, Feedme.class),
            PendingIntent.FLAG_CANCEL_CURRENT);
    }
    
    @Override
    protected void onHandleAction(Intent intent) throws ActionExecutionFailedException,
            InterruptedException {
        final Notification n = new Notification(android.R.drawable.stat_notify_sync,
                getString(R.string.downloading_entries), System.currentTimeMillis());
        n.setLatestEventInfo(this, getString(R.string.app_name),
            getString(R.string.downloading_entries), openEntriesIntent);
        startForeground(R.string.downloading_entries, n);
        
        try {
            downloadEntries();
        } finally {
            stopForeground(true);
        }
    }
    
    private void downloadEntries() throws ActionExecutionFailedException {
        final long start = System.currentTimeMillis();
        Log.i(TAG, "Start entries download");
        
        final NetworkClient client = new NetworkClient(this);
        final List<ContentValues> entries = new ArrayList<ContentValues>(20);
        try {
            client.downloadUnreadEntries(entries);
        } catch (IOException e) {
            throw new ActionExecutionFailedException(
                    "Failed to download entries from Google Reader", e);
        } finally {
            client.close();
        }
        
        Log.i(TAG, "Downloaded entries: " + entries.size());
        
        final ContentResolver cr = getContentResolver();
        final ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>(
                entries.size());
        int nbUnread = 0;
        for (final ContentValues entry : entries) {
            // Set the entry status to UNREAD by default.
            if (!entry.containsKey(Entries.STATUS)) {
                entry.put(Entries.STATUS, Entries.STATUS_UNREAD);
            }
            
            // Check if this entry already exists in the database.
            final ContentProviderOperation op;
            final Uri entryUri = Entries.getEntryUri(cr, entry.getAsString(Entries.GRID));
            if (entryUri == null) {
                // We do not know this entry: this is an insert.
                op = ContentProviderOperation.newInsert(Entries.CONTENT_URI).withValues(entry)
                        .build();
            } else {
                // This entry is already known: update it.
                op = ContentProviderOperation.newUpdate(entryUri).withValues(entry).build();
            }
            
            if (Entries.STATUS_UNREAD == entry.getAsInteger(Entries.STATUS)) {
                nbUnread++;
            }
            
            ops.add(op);
        }
        
        if (DEVELOPER_MODE) {
            Log.d(TAG, "Insert " + ops.size() + " new entrie(s) into database");
        }
        
        // If there is no more unread entries, the user has read everything.
        // The database can be updated to mark as read every entries.
        if (nbUnread == 0) {
            final ContentValues cv = new ContentValues();
            cv.put(Entries.STATUS, Entries.STATUS_READ);
            ops.add(ContentProviderOperation.newUpdate(Entries.CONTENT_URI).withValues(cv)
                    .withSelection(Entries.STATUS + "=?",
                        new String[] { String.valueOf(Entries.STATUS_UNREAD) }).build());
        }
        
        try {
            getContentResolver().applyBatch(FeedmeContract.AUTHORITY, ops);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to insert new entries", e);
            throw new ActionExecutionFailedException("Failed to insert "
                    + "new entries into database", e);
        } catch (OperationApplicationException e) {
            throw new ActionExecutionFailedException("Failed to insert "
                    + "new entries into database", e);
        }
        
        if (DEVELOPER_MODE) {
            final long now = System.currentTimeMillis();
            final long elapsed = now - start;
            Log.d(TAG, "Entries download done in " + elapsed + " ms");
        }
    }
}
