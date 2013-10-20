package at.zweng.smssenttimefix;

import static at.zweng.smssenttimefix.Constants.*;

import java.io.IOException;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

/**
 * Show debug settings page
 * 
 * @author 'Johannes Zweng, <johnny@zweng.at>'
 * 
 */
public class DebugSettingsActivity extends Activity {

	/**
	 * SD card logger
	 */
	private static SDCardLogger SD;

	/**
	 * preferences for debugging
	 */
	private SharedPreferences prefs;

	// GUI elements
	private Button btnTestLogging;
	private TextView textviewExplanation;
	private CheckBox chkEnabled;

	/**
	 * click on back button
	 */
	private OnClickListener mButtonExitListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			DebugSettingsActivity.this.finish();
		}
	};

	/**
	 * ENABLED Checkbox was changed
	 */
	private OnCheckedChangeListener mCheckBoxChangedListener = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean(PREF_DBG_DEBUG_ENABLED, isChecked);
			// Don't forget to commit your edits!!!
			editor.commit();
			SD.setLoggingEnabled(isChecked);
			updateGuiAfterPrefChange();
		}
	};

	/**
	 * test logging click
	 */
	private OnClickListener mButtonTestLoggingListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			String toastMsg;
			try {
				SD.testlogging();
				toastMsg = getString(R.string.dbg_toastTestLogOK);
			} catch (IOException e) {
				Log.w(TAG, "Test logging failed! Exception occured: ", e);
				toastMsg = String.format(
						getString(R.string.dbg_toastTestLogException),
						(e.toString() + ": " + e.getMessage()));
			} catch (SDNotMountedException e) {
				Log.w(TAG, "Test logging failed! SD Card not mounted");
				toastMsg = getString(R.string.dbg_toastTestLogNotMounted);
			}
			Toast.makeText(DebugSettingsActivity.this, toastMsg,
					Toast.LENGTH_SHORT).show();
		}
	};

	/**
	 * Click handler for delete logfile
	 */
	private OnClickListener mButtonDelLogfileListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			String toastMsg;
			try {
				SD.removeLogfile();
				toastMsg = getString(R.string.dbg_toastDeleteFileOk);
			} catch (IOException e) {
				Log.w(TAG, "Deleting of logfile failed! Exception occured: ", e);
				toastMsg = getString(R.string.dbg_toastDeleteFileFail);
			}
			Toast.makeText(DebugSettingsActivity.this, toastMsg,
					Toast.LENGTH_SHORT).show();
		}
	};

	/**
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.debug_settings);

		// get prefs manager and logger
		prefs = getSharedPreferences(PREF_DBG_FILENAME, 0);
		SD = SDCardLogger.getLogger(prefs.getBoolean(PREF_DBG_DEBUG_ENABLED,
				DEBUG_ENABLED_DEFAULT_VALUE));

		textviewExplanation = (TextView) findViewById(R.id.dbg_textViewExplanation);

		btnTestLogging = (Button) findViewById(R.id.dbg_btnTestLogging);
		btnTestLogging.setOnClickListener(mButtonTestLoggingListener);

		Button btnDeleteLogfile = (Button) findViewById(R.id.dbg_btnDeleteLogfile);
		btnDeleteLogfile.setOnClickListener(mButtonDelLogfileListener);

		Button btnExit = (Button) findViewById(R.id.dbg_btnExit);
		btnExit.setOnClickListener(mButtonExitListener);

		chkEnabled = (CheckBox) findViewById(R.id.dbg_checkbox);
		chkEnabled.setOnCheckedChangeListener(mCheckBoxChangedListener);
	}

	/**
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		updateGuiAfterPrefChange();
	}

	/**
	 * Reads current prefs and updates GUI accordingly
	 */
	private void updateGuiAfterPrefChange() {
		boolean enabled = prefs.getBoolean(PREF_DBG_DEBUG_ENABLED,
				DEBUG_ENABLED_DEFAULT_VALUE);
		btnTestLogging.setEnabled(enabled);
		chkEnabled.setChecked(enabled);
		if (enabled) {
			textviewExplanation.setText(String.format(
					getString(R.string.dbg_explanationtext_on),
					SD.getLogfilePath()));
		} else {
			textviewExplanation
					.setText(getString(R.string.dbg_explanationtext_off));
		}
	}

}
