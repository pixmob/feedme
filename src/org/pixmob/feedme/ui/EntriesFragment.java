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

import static org.pixmob.feedme.Constants.DEVELOPER_MODE;
import static org.pixmob.feedme.Constants.GOOGLE_ACCOUNT;
import static org.pixmob.feedme.Constants.SHARED_PREFERENCES;
import static org.pixmob.feedme.Constants.SP_KEY_ACCOUNT;
import static org.pixmob.feedme.Constants.SP_KEY_AUTH_TOKEN;
import static org.pixmob.feedme.Constants.TAG;

import java.io.IOException;

import org.pixmob.feedme.R;
import org.pixmob.feedme.feature.Features;
import org.pixmob.feedme.feature.SharedPreferencesSaverFeature;
import org.pixmob.feedme.provider.FeedmeContract.Entries;
import org.pixmob.feedme.service.EntriesDownloadService;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.MenuInflater;
import android.widget.AbsListView;
import android.widget.Toast;

/**
 * Display a list of RSS entries.
 * @author Pixmob
 */
public class EntriesFragment extends ListFragment implements
        SelectAccountDialog.OnAccountSelectedListener, LoaderCallbacks<Cursor> {
    private static final String[] ENTRIES_COLUMNS = { Entries._ID, Entries.SOURCE,
            Entries.PUBLISHED, Entries.TITLE, Entries.SUMMARY, Entries.URL, Entries.IMAGE };
    private static final String ENTRIES_SELECTION = Entries.STATUS + "=?";
    private static final String[] ENTRIES_SELECTION_ARGS = { String.valueOf(Entries.STATUS_UNREAD) };
    private CursorAdapter cursorAdapter;
    private SharedPreferences prefs;
    private SharedPreferences.Editor prefsEditor;
    private Intent refreshEntriesIntent;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        prefs = getActivity().getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        prefsEditor = prefs.edit();
        
        refreshEntriesIntent = new Intent(getActivity(), EntriesDownloadService.class);
        
        cursorAdapter = new EntryCursorAdapter(getActivity());
        setListAdapter(cursorAdapter);
        
        // One selected event at a time.
        getListView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        
        // The list is hidden until event cursor is loaded.
        setListShown(false);
        setEmptyText(getString(R.string.no_entry_found));
        
        // Start entries loading.
        getLoaderManager().initLoader(0, null, this);
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.add(Menu.NONE, R.string.refresh, 0, R.string.refresh)
                .setIcon(R.drawable.ic_menu_refresh)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, R.string.settings, 0, R.string.settings).setIcon(
            R.drawable.ic_menu_preferences);
        menu.add(Menu.NONE, R.string.accounts, 0, R.string.accounts).setIcon(
            R.drawable.ic_menu_login);
    }
    
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final CursorLoader loader = new CursorLoader(getActivity(), Entries.CONTENT_URI,
                ENTRIES_COLUMNS, ENTRIES_SELECTION, ENTRIES_SELECTION_ARGS, null);
        loader.setUpdateThrottle(1000);
        return loader;
    }
    
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        cursorAdapter.swapCursor(data);
        
        if (isResumed()) {
            // Entries are available: the list is shown.
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }
    
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        cursorAdapter.swapCursor(null);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.string.settings:
                onShowSettings();
                return true;
            case R.string.accounts:
                onShowAccountDialog();
                return true;
            case R.string.refresh:
                onRefresh();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void onShowAccountDialog() {
        final String accountDialogTag = "accounts";
        final boolean accountDialogVisible = getSupportFragmentManager().findFragmentByTag(
            accountDialogTag) != null;
        
        if (!accountDialogVisible) {
            final String currentAccount = prefs.getString(SP_KEY_ACCOUNT, null);
            final SelectAccountDialog d = new SelectAccountDialog(currentAccount);
            d.setOnAccountSelectedListener(this);
            d.show(getSupportFragmentManager(), accountDialogTag);
        }
    }
    
    private void onShowSettings() {
        // TODO implements activity for settings
    }
    
    private void onRefresh() {
        getActivity().startService(refreshEntriesIntent);
    }
    
    @Override
    public void onAccountSelected(String account) {
        authenticateAccount(account);
    }
    
    private void authenticateAccount(String accountName) {
        Log.i(TAG, "Authenticating account: " + accountName);
        
        final Activity a = getActivity();
        final AccountManager am = (AccountManager) a.getSystemService(Context.ACCOUNT_SERVICE);
        final Account account = new Account(accountName, GOOGLE_ACCOUNT);
        am.getAuthToken(account, "reader", null, a, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> resultContainer) {
                String authToken = null;
                final Bundle result;
                try {
                    result = resultContainer.getResult();
                    authToken = result.getString(AccountManager.KEY_AUTHTOKEN);
                } catch (IOException e) {
                    Log.w(TAG, "I/O error while authenticating account " + account.name, e);
                } catch (OperationCanceledException e) {
                    Log.w(TAG, "Authentication was canceled for account " + account.name, e);
                } catch (AuthenticatorException e) {
                    Log.w(TAG, "Authentication failed for account " + account.name, e);
                }
                
                if (authToken == null) {
                    Toast.makeText(a, a.getString(R.string.authentication_failed),
                        Toast.LENGTH_SHORT).show();
                } else {
                    Log.i(TAG, "Authentication done");
                    onAuthenticationDone(account.name, authToken);
                }
            }
        }, null);
    }
    
    private void onAuthenticationDone(String account, String authToken) {
        if (DEVELOPER_MODE) {
            Log.d(TAG, "Got authentication token: " + authToken);
        }
        
        // Save the selected account.
        prefsEditor.putString(SP_KEY_ACCOUNT, account);
        prefsEditor.putString(SP_KEY_AUTH_TOKEN, authToken);
        Features.getFeature(SharedPreferencesSaverFeature.class).save(prefsEditor);
        
        final String previousAccount = prefs.getString(SP_KEY_ACCOUNT, null);
        if (previousAccount != null && !account.equals(previousAccount)) {
            Toast.makeText(getActivity(), getString(R.string.new_account_selected),
                Toast.LENGTH_SHORT).show();
        }
    }
}
