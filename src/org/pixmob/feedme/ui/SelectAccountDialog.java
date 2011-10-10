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

import static org.pixmob.feedme.Constants.GOOGLE_ACCOUNT;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import org.pixmob.feedme.R;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Dialog for selecting an account.
 * @author Pixmob
 */
public class SelectAccountDialog extends DialogFragment {
    private WeakReference<OnAccountSelectedListener> listener;
    private final String previousAccount;
    
    public SelectAccountDialog(final String previousAccount) {
        this.previousAccount = previousAccount;
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        final AccountManager accountManager = (AccountManager) activity
                .getSystemService(Context.ACCOUNT_SERVICE);
        
        // Get Google accounts which are registered on this device.
        final Account[] accounts = accountManager.getAccountsByType(GOOGLE_ACCOUNT);
        if (accounts.length == 0) {
            // Found no Google account: an error dialog is displayed.
            return new AlertDialog.Builder(activity).setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.error).setMessage(R.string.no_google_account_found)
                    .setPositiveButton(android.R.string.ok, null).create();
        }
        
        // Sort accounts by name.
        final String[] accountNames = new String[accounts.length];
        for (int i = 0; i < accountNames.length; ++i) {
            accountNames[i] = accounts[i].name;
        }
        Arrays.sort(accountNames);
        
        // The current account is selected by default.
        int checkedIndex = -1;
        if (previousAccount != null) {
            for (int i = 0; checkedIndex == -1 && i < accountNames.length; ++i) {
                if (accountNames[i].equals(previousAccount)) {
                    checkedIndex = i;
                }
            }
        }
        
        return new AlertDialog.Builder(activity).setTitle(R.string.select_google_account)
                .setSingleChoiceItems(accountNames, checkedIndex,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final OnAccountSelectedListener l = listener != null ? listener.get()
                                    : null;
                            if (l != null) {
                                l.onAccountSelected(accountNames[which]);
                            }
                            dismiss();
                        }
                    }).create();
    }
    
    public void setOnAccountSelectedListener(OnAccountSelectedListener listener) {
        this.listener = listener == null ? null : new WeakReference<OnAccountSelectedListener>(
                listener);
    }
    
    /**
     * Interface for getting the selected account.
     * @author Pixmob
     */
    public static interface OnAccountSelectedListener {
        /**
         * This method is called when an account is selected.
         */
        void onAccountSelected(String account);
    }
}
