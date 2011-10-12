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

import java.io.IOException;

/**
 * Error thrown when a {@link NetworkClient} request failed.
 * @author Pixmob
 */
public class NetworkClientException extends IOException {
    private static final long serialVersionUID = 1L;
    private final int statusCode;
    private final String uri;
    
    public NetworkClientException(final String message, final String uri) {
        this(message, uri, 0, null);
    }
    
    public NetworkClientException(final String message, final String uri, final int statusCode,
            final Throwable cause) {
        super(message, cause);
        this.uri = uri;
        this.statusCode = statusCode;
    }
    
    public String getUri() {
        return uri;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
}
