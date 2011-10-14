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
package org.pixmob.feedme.net;

import static org.pixmob.feedme.Constants.TAG;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;

import org.pixmob.feedme.provider.FeedmeContract.Entries;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentValues;
import android.util.Log;
import android.util.Xml;

/**
 * Parse entries from a Google Reader Atom feed.
 * @author Pixmob
 */
public class EntriesParser {
    public void parse(InputStream input, String encoding, Results results) throws IOException {
        try {
            doParse(input, encoding, results);
        } finally {
            try {
                input.close();
            } catch (IOException ignore) {
            }
        }
    }
    
    private void doParse(InputStream input, String encoding, Results results) throws IOException {
        final XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setInput(input, encoding);
        } catch (XmlPullParserException e) {
            throw new IOException("Failed to configure Atom feed parser", e);
        }
        
        final SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateParser.setLenient(true);
        
        ContentValues entry = null;
        String currentTag = null;
        boolean inEntry = false;
        boolean inSource = false;
        try {
            for (int eventType; (eventType = parser.next()) != XmlPullParser.END_DOCUMENT;) {
                if (eventType == XmlPullParser.START_TAG) {
                    currentTag = parser.getName();
                    if ("entry".equals(currentTag)) {
                        entry = new ContentValues();
                        inEntry = true;
                    } else if ("link".equals(currentTag) && inEntry && !inSource) {
                        // Get the original entry link.
                        final String linkType = parser.getAttributeValue(null, "rel");
                        if ("alternate".equals(linkType)) {
                            final String link = parser.getAttributeValue(null, "href");
                            entry.put(Entries.URL, link);
                        }
                    } else if ("source".equals(currentTag)) {
                        inSource = true;
                    } else if ("category".equals(currentTag) && inEntry) {
                        // Skip read entries.
                        final String category = parser.getAttributeValue(null, "label");
                        if ("read".equals(category)) {
                            entry.put(Entries.STATUS, Entries.STATUS_READ);
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    final String tag = parser.getName();
                    if ("entry".equals(tag) && inEntry) {
                        results.entries.add(entry);
                        inEntry = false;
                    } else if ("source".equals(tag) && inEntry && inSource) {
                        inSource = false;
                    }
                } else if (eventType == XmlPullParser.TEXT) {
                    if ("id".equals(currentTag) && inEntry && !inSource) {
                        // Get the Google Reader entry identifier.
                        final String grid = parser.getText().trim();
                        entry.put(Entries.GRID, grid);
                    } else if ("title".equals(currentTag) && inEntry && inSource) {
                        // Get the feed name.
                        final String source = parser.getText().trim();
                        entry.put(Entries.SOURCE, source);
                    } else if ("published".equals(currentTag) && inEntry) {
                        // Parse the time when this entry was published.
                        final String dateStr = parser.getText().trim();
                        long published;
                        try {
                            published = dateParser.parse(dateStr).getTime();
                        } catch (ParseException e) {
                            published = System.currentTimeMillis();
                            Log.w(TAG, "Failed to parse entry publication date: " + dateStr, e);
                        }
                        entry.put(Entries.PUBLISHED, published);
                    } else if ("title".equals(currentTag) && inEntry && !inSource) {
                        // Get the entry title.
                        final String title = parser.getText().trim();
                        entry.put(Entries.TITLE, title);
                    } else if (("content".equals(currentTag) || "summary".equals(currentTag))
                            && inEntry) {
                        // Get the entry summary.
                        final String summary = parser.getText().trim();
                        entry.put(Entries.SUMMARY, summary);
                    } else if ("continuation".equals(currentTag)) {
                        // Get the string used for continuation process.
                        final String cont = parser.getText().trim();
                        results.continuation = cont;
                    }
                }
            }
        } catch (XmlPullParserException e) {
            throw new IOException("Failed to parse Atom feed", e);
        }
    }
    
    /**
     * Parse result.
     * @author Pixmob
     */
    public static class Results {
        public Collection<ContentValues> entries;
        public String continuation;
    }
}
