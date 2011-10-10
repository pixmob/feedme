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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.pixmob.feedme.net.Entry;
import org.pixmob.feedme.net.NetworkClient;

import android.content.Intent;

import com.pixmob.actionservice.ActionExecutionFailedException;
import com.pixmob.actionservice.ActionService;

/**
 * Download new entries from Google Reader. Entries are stored in the local
 * database.
 * @author Pixmob
 */
public class EntriesDownloadService extends ActionService {
    public EntriesDownloadService() {
        super("Feedme/EntriesDownload");
    }
    
    @Override
    protected void onHandleAction(Intent intent) throws ActionExecutionFailedException,
            InterruptedException {
        final NetworkClient client = new NetworkClient(this);
        final List<Entry> entries = new ArrayList<Entry>(20);
        try {
            client.downloadUnreadEntries(entries);
        } catch (IOException e) {
            throw new ActionExecutionFailedException(
                    "Failed to download entries from Google Reader", e);
        } finally {
            client.close();
        }
    }
}
