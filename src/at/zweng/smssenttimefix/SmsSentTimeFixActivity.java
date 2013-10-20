package at.zweng.smssenttimefix;

import static at.zweng.smssenttimefix.Constants.*;
import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


// TODO: debuggen absturz SMSRemote und kontaktieren developper
// DONE: mit diesem SMS TExt hat es nicht funktioniert: (jetzt funktionierts)
// ...looking for SMS in DB: body:'Theft Aware (www.theftaware.com) Aktualisierungsnachricht. Besitzer [Johannes Zweng] Netz [3 AT] Signal [2/7] Sendezelle [13205368] Land [Austri', origin:+4369911398186, scAddr:+4366000660, timest:1297808488000
// TODO: Vibrate bei btn click --> nicht unbedingt
// TODO: evtl das "Fix" aus dem Namen rausnehmen (und auch aus der Titelgrafik)

public class SmsSentTimeFixActivity extends Activity {
	private ImageView ledGreen;
	private ImageView ledRed;
	private TextView dbgReminderTextView;

	/**
	 * click on exit button
	 */
	private OnClickListener mButtonExitListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			SmsSentTimeFixActivity.this.finish();
		}
	};

	/**
	 * Click on the what's this button
	 */
	private OnClickListener mButtonInfoListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			showDialog(DIALOG_ID_ABOUT);
		}
	};

	/**
	 * Click on settings button
	 */
	private OnClickListener mButtonSettingsListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent i = new Intent();
			i.setComponent(new ComponentName(getApplicationContext(),
					SmsTimeFixPrefs.class));
			startActivity(i);
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// load default values from xml if they have been set never before
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Button btnInfo = (Button) findViewById(R.id.btnInfo);
		btnInfo.setOnClickListener(mButtonInfoListener);

		Button btnSettings = (Button) findViewById(R.id.btnPrefs);
		btnSettings.setOnClickListener(mButtonSettingsListener);

		Button btnExit = (Button) findViewById(R.id.btnExit);
		btnExit.setOnClickListener(mButtonExitListener);

		ledGreen = (ImageView) findViewById(R.id.imgBtnGreen);
		ledRed = (ImageView) findViewById(R.id.imgBtnRed);
		dbgReminderTextView = (TextView) findViewById(R.id.textViewDebugReminder);

		// place / resize the buttons:
		prepareButtons();

	}

	/**
	 * Called when menu button gets pressed
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		return true;
	}

	/**
	 * When the user selects an item in the menu
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuPreferences:
			Intent i = new Intent();
			i.setComponent(new ComponentName(getApplicationContext(),
					SmsTimeFixPrefs.class));
			startActivity(i);
			return true;
			// DISABLED MENU-POINT "EXIT":
			// case R.id.menuQuit:
			// Log.d(TAG, "will finish this activity.. bye bye..");
			// finish();
			// return true;
		case R.id.menCredits:
			showDialog(DIALOG_ID_CREDITS);
			return true;
		case R.id.menDebugSettings:
			Intent iDbg = new Intent();
			iDbg.setComponent(new ComponentName(getApplicationContext(),
					DebugSettingsActivity.class));
			startActivity(iDbg);
			return true;
		}
		return false;
	}

	/**
	 * Will be called when dialog needs to be created (only one time, if the
	 * dialog is shown more than once during lifetime). For everytime change use
	 * {@link #onPrepareDialog(int, Dialog)}.
	 * 
	 * @see android.app.Activity#onCreateDialog(int)
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_ID_CREDITS:
			return Util.getCreditsDialog(this);

		case DIALOG_ID_ABOUT:
			return Util.getAboutDialog(this);

		case DIALOG_ID_GOSMS_WARN:
			return Util.getGoSMSInfoDialog(this);

		default:
			Log.w(TAG,
					"onCreateDialog: got unknown DIALOG ID, showing credits dialog as default");
			return Util.getCreditsDialog(this);
		}
	}

	/**
	 * Programatically place the 3 different color status buttons (as I did not
	 * get it only with xml, I know this is bad.. :-( )
	 */
	private void prepareButtons() {
		// int btnWidth= (int) (imgHeader.getHeight() * 0.65);
		// int rightMargin = (int) (imgHeader.getWidth()*0.045f);

		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		// Log.d(TAG, "display width: " + metrics.widthPixels);

		// if display is wider than the header graphic, limit it to the width of
		// the header:
		int useWidth = Math.min(metrics.widthPixels, 480);
		// Log.d(TAG, "using width: " + useWidth);

		int btnWidth = (int) (useWidth * 0.134f);
		int rightMargin = (int) (useWidth * 0.065f);

		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.WRAP_CONTENT,
				FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL
						| Gravity.RIGHT);
		layoutParams.setMargins(0, 0, rightMargin, 0);

		// green:
		ledGreen.setMaxHeight(btnWidth);
		ledGreen.setMaxWidth(btnWidth);
		ledGreen.setLayoutParams(layoutParams);
		ledGreen.setVisibility(View.INVISIBLE);

		// red:
		ledRed.setMaxHeight(btnWidth);
		ledRed.setMaxWidth(btnWidth);
		ledRed.setLayoutParams(layoutParams);
		ledRed.setVisibility(View.INVISIBLE);

		// layout
		ledRed.requestLayout();
		ledGreen.requestLayout();

		// ledGreen.setOnTouchListener(touchListener);
		// ledGrey.setOnTouchListener(touchListener);
		// ledRed.setOnTouchListener(touchListener);

		ledGreen.setOnClickListener(clickListener);
		ledRed.setOnClickListener(clickListener);
	}

	/**
	 * makes the button visible according to state
	 * 
	 * @param enabled
	 */
	private void setButtonEnabled(boolean enabled) {
		if (enabled) {
			ledGreen.setVisibility(View.VISIBLE);
			ledRed.setVisibility(View.INVISIBLE);
		} else {
			ledGreen.setVisibility(View.INVISIBLE);
			ledRed.setVisibility(View.VISIBLE);
		}
	}

	private View.OnClickListener clickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(SmsSentTimeFixActivity.this);
			boolean currState = prefs.getBoolean(Constants.PREF_SVC_ENABLED,
					false);
			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean(Constants.PREF_SVC_ENABLED, !currState);
			editor.commit();
			setButtonEnabled(!currState);
			String toast;
			if (!currState) {
				toast = getString(R.string.toast_svc_enabled);
			} else {
				toast = getString(R.string.toast_svc_disabled);
			}
			Toast.makeText(SmsSentTimeFixActivity.this, toast,
					Toast.LENGTH_SHORT).show();
		}
	};

	/**
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		setButtonEnabled(prefs.getBoolean(Constants.PREF_SVC_ENABLED, false));
		if (getSharedPreferences(PREF_DBG_FILENAME, 0).getBoolean(
				PREF_DBG_DEBUG_ENABLED, DEBUG_ENABLED_DEFAULT_VALUE)) {
			dbgReminderTextView.setVisibility(View.VISIBLE);
		} else {
			dbgReminderTextView.setVisibility(View.INVISIBLE);
		}

		// on first run display note for Users of Go SMS:
		if (!prefs.getBoolean(PREF_GO_SMS_WARNING_SHOWN, false)) {
			showDialog(DIALOG_ID_GOSMS_WARN);
		}
	}
}