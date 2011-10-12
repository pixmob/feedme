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

import static org.pixmob.feedme.Constants.DEVELOPER_MODE;
import static org.pixmob.feedme.Constants.TAG;

import org.pixmob.feedme.feature.Features;
import org.pixmob.feedme.feature.StrictModeFeature;

import android.util.Log;

/**
 * Execute actions when the application is starting.
 * @author Pixmob
 */
public class Application extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        if (DEVELOPER_MODE) {
            Log.i(TAG, "Enabling StrictMode features");
            Features.getFeature(StrictModeFeature.class).enable();
        }
    }
}
