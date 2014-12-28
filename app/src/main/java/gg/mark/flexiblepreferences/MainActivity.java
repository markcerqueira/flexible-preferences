package gg.mark.flexiblepreferences;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends PreferenceActivity {
    private View mLoadingView;
    private View mPreferencesView;

    private PreferenceScreen mRoot;

    // cache the PreferenceCategories we create in this list so we can remove and add them later
    private List<PreferenceCategory> mPreferenceCategoryList = new ArrayList<>();

    private static Handler sHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // this layout file must contain a ListView with id @id/android:list
        setContentView(R.layout.main_activity);

        mLoadingView = findViewById(R.id.loading_view);
        mPreferencesView = findViewById(R.id.preferences_view);

        mRoot = getPreferenceManager().createPreferenceScreen(this);

        toggleViewVisibility(false /* do not preference view */);

        getPreferencesFromNetwork();
    }

    private void toggleViewVisibility(boolean showPreferencesView) {
        mLoadingView.setVisibility(showPreferencesView ? View.GONE : View.VISIBLE);
        mPreferencesView.setVisibility(showPreferencesView ? View.VISIBLE : View.GONE);
    }

    // simulates getting preferences from the network
    private void getPreferencesFromNetwork() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // key is the category name and maps to a list of preferences within that category
                final LinkedHashMap<String, List<String>> preferencesMap = new LinkedHashMap<>();

                preferencesMap.put("Advanced Colors", Arrays.asList("Red", "Blue", "Green"));
                preferencesMap.put("Best Final Fantasy Games", Arrays.asList("FFX-2", "FF13", "FF13-2"));
                preferencesMap.put("Best Pokémon", Arrays.asList("Rattata", "Magikarp"));

                // simulate some time "on the wire"
                try {
                    Thread.sleep((long) (Math.random() * 1000) + 2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // a gentleman's dispatch onto the UI thread
                sHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        refreshWithPreferences(preferencesMap);
                    }
                });
            }
        }).start();
    }

    private void refreshWithPreferences(LinkedHashMap<String, List<String>> preferencesMap) {
        // the global switch will be used to test showing and hiding all the loaded preferences
        CheckBoxPreference globalSwitch = new CheckBoxPreference(this);
        globalSwitch.setKey("globalSwitch");
        globalSwitch.setTitle("Show Preferences");
        globalSwitch.setDefaultValue(true);

        globalSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                refreshPreferenceVisibility((boolean) newValue);

                return true;
            }
        });

        mRoot.addPreference(globalSwitch);

        for (Map.Entry<String, List<String>> entry : preferencesMap.entrySet()) {
            String preferenceCategoryName = entry.getKey();

            PreferenceCategory preferenceCategory = new PreferenceCategory(this);
            preferenceCategory.setKey(preferenceCategoryName);
            preferenceCategory.setTitle(preferenceCategoryName);

            // need to add the empty PreferenceCategory to the root BEFORE we add anything to it
            // otherwise, we'll get a NullPointerException when we call addPreference on the PreferenceCategory
            mRoot.addPreference(preferenceCategory);

            List<String> preferencesList = entry.getValue();

            for (String preference : preferencesList) {
                CheckBoxPreference checkBoxPreference = new CheckBoxPreference(this);
                checkBoxPreference.setKey(preference);
                checkBoxPreference.setTitle(preference);

                preferenceCategory.addPreference(checkBoxPreference);
            }

            // keep a reference to the PreferenceCategory so we can show/hide it later
            mPreferenceCategoryList.add(preferenceCategory);
        }

        setPreferenceScreen(mRoot);

        refreshPreferenceVisibility(globalSwitch.isChecked());

        toggleViewVisibility(true /* show preference view */);
    }

    private void refreshPreferenceVisibility(boolean isVisible) {
        for (PreferenceCategory preferenceCategory : mPreferenceCategoryList) {
            if (isVisible) {
                mRoot.addPreference(preferenceCategory);
            } else {
                mRoot.removePreference(preferenceCategory);
            }
        }
    }
}
