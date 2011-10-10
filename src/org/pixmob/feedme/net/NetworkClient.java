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
import static org.pixmob.feedme.Constants.TAG;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.util.Log;

public class NetworkClient {
    private static String userAgent;
    private static String clientId;
    private final Context context;
    private final DefaultHttpClient client;
    private final SharedPreferences prefs;
    private final SharedPreferences.Editor prefsEditor;
    
    public NetworkClient(final Context context) {
        this.context = context;
        
        if (userAgent == null) {
            userAgent = generateUserAgent(context);
        }
        if (clientId == null) {
            clientId = generateClientId(context);
        }
        
        client = SSLEnabledHttpClient.newInstance(userAgent);
        prefs = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        prefsEditor = prefs.edit();
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
        return "feedme/" + applicationVersion;
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
    
    private String createServiceUri(String uri) {
        return "http://www.google.com/reader" + (uri.startsWith("/") ? uri : ("/" + uri));
    }
    
    public void downloadUnreadEntries(List<Entry> entries) throws IOException {
        final HttpGet req = new HttpGet(
                createServiceUri("/atom/user/-/state/com.google/reading-list"));
        prepareRequest(req);
        
        HttpResponse resp = null;
        int statusCode = 0;
        try {
            resp = client.execute(req);
            final StatusLine statusLine = resp.getStatusLine();
            statusCode = statusLine.getStatusCode();
            
            final HttpEntity entity = resp.getEntity();
            Log.i(TAG, "Got response: " + statusLine.getReasonPhrase() + "/" + statusCode);
            Log.i(TAG, "Entity: " + entity);
        } catch (IOException e) {
            throw new NetworkClientException("Failed to get unread entries", req.getURI()
                    .toString(), statusCode, e);
        } finally {
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
