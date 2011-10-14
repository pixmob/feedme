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
package org.pixmob.feedme;

import android.accounts.AccountManager;

/**
 * Application constants.
 * @author Pixmob
 */
public final class Constants {
    /**
     * Preference key for setting the selected account.
     */
    public static final String SP_KEY_ACCOUNT = "account";
    /**
     * Internal preference key for storing the authentication token.
     */
    public static final String SP_KEY_AUTH_TOKEN = "authToken";
    /**
     * Preference key for setting how many new entries should be downloaded.
     */
    public static final String SP_KEY_NUMBER_OF_ITEMS = "numberOfItems";
    /**
     * Preferences file name.
     */
    public static final String SHARED_PREFERENCES = "sharedprefs";
    /**
     * Google account type, used for {@link AccountManager} methods.
     */
    public static final String GOOGLE_ACCOUNT = "com.google";
    /**
     * Log tag.
     */
    public static final String TAG = "Feedme";
    /**
     * Is this application running in developer mode?
     */
    public static final boolean DEVELOPER_MODE = true;
    
    private Constants() {
    }
}
