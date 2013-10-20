package at.zweng.smssenttimefix;

import static at.zweng.smssenttimefix.Constants.*;
import static at.zweng.smssenttimefix.Constants.PREF_EXAMPLE_FORMAT;
import static at.zweng.smssenttimefix.Constants.PREF_SVC_ENABLED;
import static at.zweng.smssenttimefix.Constants.PREF_TEXT_TO_APPEND;
import static at.zweng.smssenttimefix.Constants.PREF_USE24H_FORMAT;
import static at.zweng.smssenttimefix.Constants.TAG;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * Preferences page
 * 
 * @author John Zweng
 * 
 */
public class SmsTimeFixPrefs extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	// TODO: adde noch ein drop-down menü mit einigen default textmustern!)
	// TODO: oder adde eine preview unterhalb des eingabefensters (overwrite
	// eingabe popup?)

	Preference mPrefSrvcEnabled;
	Preference mPrefAppendTxtPref;
	Preference mPref24h;
	ListPreference mPrefDateformat;
	Preference mPrefTimezoneFix1;
	Preference mPrefExampleFormat;
	Date mSampleDate;
	SimpleDateFormat mFormatter;
	SharedPreferences prefs;

	/**
	 * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		this.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

		addPreferencesFromResource(R.xml.preferences);
		prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		mSampleDate = new GregorianCalendar(2010, 8, 2, 13, 00, 00).getTime();
		mFormatter = new SimpleDateFormat("yyyy");
		mPrefAppendTxtPref = findPreference(PREF_TEXT_TO_APPEND);
		mPref24h = findPreference(PREF_USE24H_FORMAT);
		mPrefDateformat = (ListPreference) findPreference(PREF_DATE_FORMAT);
		mPrefSrvcEnabled = findPreference(PREF_SVC_ENABLED);
		mPrefExampleFormat = findPreference(PREF_EXAMPLE_FORMAT);
		mPrefTimezoneFix1 = findPreference(PREF_TIMEZONE_FIX_1);
		prefs.registerOnSharedPreferenceChangeListener(this);

		fillDateFormatList();

		// set up some change listener:
		mPrefAppendTxtPref
				.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						if ((!((String) newValue).contains("%TIME%"))
								&& (!((String) newValue).contains("%DATE%"))
								&& (!((String) newValue).contains("%SMSC%"))) {
							Log.w(TAG,
									"Prefs: new string doesn't contain any placeholders. Will show warning toast to user.");
							Toast.makeText(
									getApplicationContext(),
									getString(R.string.p_toast_warnung_invalid_text_append),
									Toast.LENGTH_LONG).show();
							return true;
						}
						return true;
					}
				});

		// refresh enable disable status
		refreshEnabledStatus();
		// and initially update example
		updateExample();
		updateSummaryDateFormat();
	}

	/**
	 * Fill date format list prefence with values
	 */
	private void fillDateFormatList() {

		Resources res = getResources();
		TypedArray dateFormats = res
				.obtainTypedArray(R.array.arr_date_format_strings);
		String[] dfKeys = new String[dateFormats.length()];
		String[] dfLabels = new String[dateFormats.length()];
		for (int i = 0; i < dateFormats.length(); i++) {
			dfKeys[i] = dateFormats.getString(i);
			mFormatter.applyPattern(dfKeys[i]);
			dfLabels[i] = mFormatter.format(mSampleDate);
		}
		mPrefDateformat.setEntries(dfLabels);
		mPrefDateformat.setEntryValues(dfKeys);
	}

	/**
	 * Called when sometging in the preference changed
	 * 
	 * @param sharedPreferences
	 * @param key
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		// update example text
		updateExample();
		updateSummaryDateFormat();
		// refresh enable disable status
		refreshEnabledStatus();
	}

	/**
	 * Refresh the enabled/disabled status absed on prefs
	 */
	private void refreshEnabledStatus() {
		boolean enabled = PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext()).getBoolean(PREF_SVC_ENABLED, true);
		enableDisableSettings(enabled);
	}

	/**
	 * Disables all other preferences if service gets disabled and vice versa.
	 * 
	 * @param enabled
	 */
	private void enableDisableSettings(boolean enabled) {
		mPrefAppendTxtPref.setEnabled(enabled);
		mPref24h.setEnabled(enabled);
		mPrefDateformat.setEnabled(enabled);
		mPrefTimezoneFix1.setEnabled(enabled);
	}

	/**
	 * Sets the summary of the example pref like it will look like
	 */
	private void updateExample() {
		if (!prefs.getBoolean(PREF_SVC_ENABLED, false)) {
			mPrefExampleFormat.setSummary("");
			return;
		}
		String smscAddr = "+1234567890";

		// ------ copied from service
		Date smscTime = new Date();
		String dateStr = new SimpleDateFormat(prefs.getString(PREF_DATE_FORMAT,
				"dd.MM.yyyy")).format(smscTime);
		SimpleDateFormat timeFormatter;
		if (prefs.getBoolean(PREF_USE24H_FORMAT, false)) {
			timeFormatter = new SimpleDateFormat("HH:mm:ss");
		} else {
			timeFormatter = new SimpleDateFormat("h:mm:ss a");
		}

		// Some tested defaults for "inspiration" :)
		// en-UK: "14 Feb 2011 17:34:53"
		// en-US: "Feb 14, 2011 5:38:27 PM"
		// de-AT/DE: "14.02.2011 17:40:29"

		String timeStr = timeFormatter.format(smscTime);

		String textToAppend = prefs.getString(PREF_TEXT_TO_APPEND,
				getString(R.string.p_default_text_append));
		textToAppend = textToAppend.replaceAll("%DATE%", dateStr);
		textToAppend = textToAppend.replaceAll("%TIME%", timeStr);
		textToAppend = textToAppend.replaceAll("%SMSC%", smscAddr);
		// ------

		mPrefExampleFormat.setSummary(textToAppend);

		// and set the summary text of the 24h checkbox:
		mPref24h.setSummary(timeFormatter.format(mSampleDate));
	}

	/**
	 * Update summary of dateformat
	 */
	private void updateSummaryDateFormat() {
		mFormatter
				.applyPattern(prefs.getString(PREF_DATE_FORMAT, "dd.MM.yyyy"));
		mPrefDateformat.setSummary(mFormatter.format(mSampleDate));

	}

}
