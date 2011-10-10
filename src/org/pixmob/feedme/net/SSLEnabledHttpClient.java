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

import org.apache.http.client.HttpClient;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

/**
 * Internal {@link HttpClient} implementation accepting all SSL certificates.
 * With API level 7, the class <code>AndroidHttpClient</code> is not available,
 * and the default {@link HttpClient} implementation does not accept untrusted
 * SSL certificates. This class provides a way for silently accepting these SSL
 * certificates.
 * @author Pixmob
 */
class SSLEnabledHttpClient extends DefaultHttpClient {
    private SSLEnabledHttpClient(ClientConnectionManager manager, HttpParams params) {
        super(manager, params);
    }
    
    public static SSLEnabledHttpClient newInstance(String userAgent) {
        // the following code comes from AndroidHttpClient (API level 10)
        
        final HttpParams params = new BasicHttpParams();
        
        // Turn off stale checking. Our connections break all the time anyway,
        // and it's not worth it to pay the penalty of checking every time.
        HttpConnectionParams.setStaleCheckingEnabled(params, false);
        
        final int timeout = 60 * 1000;
        HttpConnectionParams.setConnectionTimeout(params, timeout);
        HttpConnectionParams.setSoTimeout(params, timeout);
        HttpConnectionParams.setSocketBufferSize(params, 8192);
        
        // Don't handle redirects -- return them to the caller. Our code
        // often wants to re-POST after a redirect, which we must do ourselves.
        HttpClientParams.setRedirecting(params, false);
        
        // Set the specified user agent and register standard protocols.
        HttpProtocolParams.setUserAgent(params, userAgent);
        
        // Prevent UnknownHostException error with 3G connections:
        // http://stackoverflow.com/questions/2052299/httpclient-on-android-nohttpresponseexception-through-umts-3g
        HttpProtocolParams.setUseExpectContinue(params, false);
        
        final SSLSocketFactory sslSocketFactory = SSLSocketFactory.getSocketFactory();
        sslSocketFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        
        final SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        schemeRegistry.register(new Scheme("https", sslSocketFactory, 443));
        
        final ClientConnectionManager manager = new SingleClientConnManager(params, schemeRegistry);
        final SSLEnabledHttpClient client = new SSLEnabledHttpClient(manager, params);
        client.addRequestInterceptor(new GzipRequestInterceptor());
        client.addResponseInterceptor(new GzipResponseInterceptor());
        
        return client;
    }
}
