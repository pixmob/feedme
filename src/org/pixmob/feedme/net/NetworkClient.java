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

import static org.pixmob.feedme.Constants.SHARED_PREFERENCES;
import static org.pixmob.feedme.Constants.SP_KEY_AUTH_TOKEN;
import static org.pixmob.feedme.Constants.SP_KEY_NUMBER_OF_ITEMS;
import static org.pixmob.feedme.Constants.TAG;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.pixmob.feedme.feature.Features;
import org.pixmob.feedme.feature.SharedPreferencesSaverFeature;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

/**
 * Execute network requests.
 * @author Pixmob
 */
public class NetworkClient {
    private static final String SP_KEY_CONTINUATION = "continuation";
    private static String userAgent;
    private static String clientId;
    private final DefaultHttpClient client;
    private final SharedPreferences prefs;
    private final SharedPreferences.Editor prefsEditor;
    private final EntriesParser parser;
    
    public NetworkClient(final Context context) {
        if (userAgent == null) {
            userAgent = generateUserAgent(context);
        }
        if (clientId == null) {
            clientId = generateClientId(context);
        }
        
        client = SSLEnabledHttpClient.newInstance(userAgent);
        prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        prefsEditor = prefs.edit();
        
        parser = new EntriesParser();
    }
    
    /**
     * Generate an Http User-Agent.
     */
    private static final String generateUserAgent(Context context) {
        String applicationVersion = null;
        try {
            applicationVersion = context.getPackageManager().getPackageInfo(
                context.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            applicationVersion = "0.0.0";
        }
        return context.getApplicationInfo().name + "/" + applicationVersion + " ("
                + Build.MANUFACTURER + " " + Build.MODEL + " with Android " + Build.VERSION.RELEASE
                + "/" + Build.VERSION.SDK_INT + ")";
    }
    
    /**
     * Generate a Google Reader client ID.
     */
    private static final String generateClientId(Context context) {
        String applicationVersion = null;
        try {
            applicationVersion = context.getPackageManager().getPackageInfo(
                context.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            applicationVersion = "0.0.0";
        }
        return Uri.encode("feedme_" + applicationVersion);
    }
    
    private String getAuthToken() {
        return prefs.getString(SP_KEY_AUTH_TOKEN, null);
    }
    
    public void close() {
        client.getConnectionManager().shutdown();
    }
    
    private void prepareRequest(HttpUriRequest req) throws NetworkClientException {
        final String authToken = getAuthToken();
        if (authToken == null) {
            throw new NetworkClientException("Missing authentication token", req.getURI()
                    .toString());
        }
        req.setHeader("Authorization", "GoogleLogin auth=" + authToken);
    }
    
    private String createServiceUri(String uri, Map<String, String> parameters) {
        final StringBuilder buf = new StringBuilder("http://www.google.com/reader");
        if (!uri.startsWith("/")) {
            buf.append('/');
        }
        buf.append(uri).append("?c=").append(clientId);
        
        if (parameters != null) {
            for (final Map.Entry<String, String> e : parameters.entrySet()) {
                buf.append('&').append(e.getKey()).append("=").append(e.getValue());
            }
        }
        return buf.toString();
    }
    
    public void downloadUnreadEntries(List<ContentValues> entries) throws IOException {
        final Map<String, String> params = new HashMap<String, String>(4);
        params.put("client", clientId);
        params.put("n", prefs.getString(SP_KEY_NUMBER_OF_ITEMS, "50"));
        params.put("ck", String.valueOf(System.currentTimeMillis()));
        
        final String continuation = prefs.getString(SP_KEY_CONTINUATION, null);
        if (continuation != null) {
            params.put("c", continuation);
        }
        
        final HttpGet req = new HttpGet(createServiceUri(
            "/atom/user/-/state/com.google/reading-list", params));
        prepareRequest(req);
        
        Log.i(TAG, "Sending request for downloading entries: " + req.getURI().toASCIIString());
        
        final EntriesParser.Results parseResults = new EntriesParser.Results();
        parseResults.entries = entries;
        
        HttpResponse resp = null;
        FileOutputStream output = null;
        int statusCode = 0;
        try {
            resp = client.execute(req);
            final StatusLine statusLine = resp.getStatusLine();
            statusCode = statusLine.getStatusCode();
            
            Log.i(TAG, "Entries download status code: " + statusCode);
            
            if (statusCode != 200) {
                throw new IOException("Entries download error");
            }
            
            final HttpEntity entity = resp.getEntity();
            final InputStream input = entity.getContent();
            parser.parse(input, EntityUtils.getContentCharSet(entity), parseResults);
            
            prefsEditor.putString(SP_KEY_CONTINUATION, parseResults.continuation);
            Features.getFeature(SharedPreferencesSaverFeature.class).save(prefsEditor);
        } catch (IOException e) {
            throw new NetworkClientException("Failed to get unread entries", req.getURI()
                    .toString(), statusCode, e);
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ignore) {
                }
            }
            closeResources(req, resp);
        }
    }
    
    private static void closeResources(HttpUriRequest req, HttpResponse resp) {
        try {
            req.abort();
        } catch (UnsupportedOperationException ignore) {
        }
        final HttpEntity entity = resp != null ? resp.getEntity() : null;
        if (entity != null) {
            try {
                entity.consumeContent();
            } catch (IOException ignore) {
            }
        }
    }
}
