package at.zweng.smssenttimefix;

import static at.zweng.smssenttimefix.Constants.COL_ADDRESS;
import static at.zweng.smssenttimefix.Constants.COL_BODY;
import static at.zweng.smssenttimefix.Constants.COL_DATE;
import static at.zweng.smssenttimefix.Constants.COL_ID;
import static at.zweng.smssenttimefix.Constants.COL_SERVICE_CENTER;
import static at.zweng.smssenttimefix.Constants.COL_TYPE;
import static at.zweng.smssenttimefix.Constants.CONTENT_SMS_URI;
import static at.zweng.smssenttimefix.Constants.DEBUG_ENABLED_DEFAULT_VALUE;
import static at.zweng.smssenttimefix.Constants.EXTRA_BODIES;
import static at.zweng.smssenttimefix.Constants.EXTRA_ORIGINATORS;
import static at.zweng.smssenttimefix.Constants.EXTRA_SC_ADRESSES;
import static at.zweng.smssenttimefix.Constants.EXTRA_SC_TIMESTAMPS;
import static at.zweng.smssenttimefix.Constants.PREF_DATE_FORMAT;
import static at.zweng.smssenttimefix.Constants.PREF_DBG_DEBUG_ENABLED;
import static at.zweng.smssenttimefix.Constants.PREF_DBG_FILENAME;
import static at.zweng.smssenttimefix.Constants.PREF_TEXT_TO_APPEND;
import static at.zweng.smssenttimefix.Constants.PREF_TIMEZONE_FIX_1;
import static at.zweng.smssenttimefix.Constants.PREF_USE24H_FORMAT;
import static at.zweng.smssenttimefix.Constants.TAG;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * This service gets startet from the SMS broadcast receiver and correc ts the
 * time in the SMS database after the SMS got stored.
 * 
 * @author John Zweng
 * 
 */
public class SmsTimeFixService extends Service {

	private static final int NUM_RETRIES = 6;

	/**
	 * SD card logger
	 */
	private static SDCardLogger SD;

	/**
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		SD = SDCardLogger.getLogger(this.getSharedPreferences(
				PREF_DBG_FILENAME, 0).getBoolean(PREF_DBG_DEBUG_ENABLED,
				DEBUG_ENABLED_DEFAULT_VALUE));
		SD.log("SmsTimeFixService: onCreate - Service instance created.");
	}

	/**
	 * Our worker thread
	 */
	private Thread workerThread = null;

	/**
	 * Our work queue
	 */
	private BlockingQueue<Intent> queue = new LinkedBlockingQueue<Intent>();

	/**
	 * Constructor
	 * 
	 * @param name
	 *            Used to name the worker thread, important only for debugging
	 */
	public SmsTimeFixService() {
		super();
	}

	/**
	 * Called when binding to this service
	 * 
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		// Nobody binds to this service
		return null;
	}

	/**
	 * This is the old onStart method that will be called on the pre-2.0
	 * platform. On 2.0 or later we override onStartCommand() so this method
	 * will not be called.
	 */
	@Override
	public void onStart(Intent intent, int startId) {
		// Log.d(TAG, "Service: Old (pre-2.0) onStart was called.");
		SD.log("SmsTimeFixService: onStart was called.");
		handleIntent(intent);
	}

	/**
	 * This is the new onStart method (since 2.0 platform), called when the
	 * service gets started.
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Log.d(TAG, "Service: New (post-2.0) onStartCommand was called.");
		SD.log("SmsTimeFixService: onStartCommand was called.");
		handleIntent(intent);
		// We want this service to be restarted again if
		// it gets killed before it has done its work.
		return START_REDELIVER_INTENT;
	}

	/**
	 * Performs the work in a separate thread, we change the sms time stamp
	 * here.
	 * 
	 * @see android.app.IntentService#onHandleIntent(android.content.Intent)
	 */
	private void handleIntent(Intent intent) {
		// Log.d(TAG,
		// "Service: handleIntent was called. Will put intent in queue and start workerthread");
		SD.log("SmsTimeFixService: handleIntent was called.");

		try {
			SD.log("SmsTimeFixService: adding intent to work queue.");
			queue.put(intent);
		} catch (InterruptedException e) {
			// do nothing, should never happen as this queue has maximum
			// capacity and therefore we never shoud have to wait
			Log.w(TAG, "Catched exception while adding intent to workqueue", e);
			SD.log("SmsTimeFixService: Catched exception while adding intent to workqueue: "
					+ e + ": " + e.getMessage());
		}
		if (workerThread == null || !workerThread.isAlive()) {
			workerThread = new WorkerThread();
			SD.log("SmsTimeFixService: Created new worker thread and will start it NOW.");
			workerThread.start();
		} else {
			SD.log("SmsTimeFixService: A worker thread exists already and is running. It will take the work from queue.");
		}
	}

	/**
	 * @see android.app.IntentService#onDestroy()
	 */
	@Override
	public void onDestroy() {
		Log.d(TAG, "Service: onDestroy was called! BYE BYE..");
		SD.log("SmsTimeFixService: onDestroy was called! BYE BYE!");
		super.onDestroy();
	}

	/**
	 * Inner class as datastructure, represents one sms to fix
	 * 
	 * @author John Zweng
	 * 
	 */
	private class SmsToFix {

		private String scAddress;
		private String body;
		private String originator;
		private long scTimestamp;

		public SmsToFix(String scAddress, String body, String originator,
				long scTimestamp) {
			super();
			this.scAddress = scAddress;
			this.body = body;
			this.originator = originator;
			this.scTimestamp = scTimestamp;
		}

	}

	/**
	 * Workerthread, performs the work in a separate thread.
	 * 
	 * @author John Zweng
	 * 
	 */
	private class WorkerThread extends Thread {

		public WorkerThread() {
			super();
			this.setName("Workerthread-SMSSentTimeFix");
			SD.log("SmsTimeFixService [WorkerThread]: Thread constructor.");
		}

		/**
		 * Perform the SMS date fix in this separate thread
		 */
		@Override
		public void run() {
			SD.log("SmsTimeFixService [WorkerThread]: Thread is starting now.");
			SD.log("SmsTimeFixService [WorkerThread]: There are "
					+ queue.size() + " elements in the work queue.");
			Intent intentToProcess;
			// work as long we have intents in the workqueue.
			while ((intentToProcess = queue.poll()) != null) {
				SD.log("SmsTimeFixService [WorkerThread]: Yippie, got work from the queue!");
				Log.i(TAG, "Service: WorkerThread got some work from queue");

				try {
					List<SmsToFix> smsList = parseSmsFromIntent(intentToProcess);
					processSmsList(smsList);
				} catch (SmsFixException e) {
					Log.w(TAG,
							"Service: Some error occured during sms fix. Message was: "
									+ e.getMessage(), e);
					SD.log("SmsTimeFixService [WorkerThread]: An exception occured during sms fix. Message was: "
							+ e + ": " + e.getMessage());
				}
			}
			Log.i(TAG,
					"Service: Workerthread: No more work in queue. Will exit thread and stop service. GOOD BYE!, Thread: "
							+ Thread.currentThread().getName());
			SD.log("SmsTimeFixService [WorkerThread]: Found no more work in the queue. Will stop working and go for a beer now. BYE BYE..");
			stopSelf();
			workerThread = null;
		}

		/**
		 * Gets out the extra data from the intent and constructs a list object
		 * 
		 * @param intent
		 * @return list with sms to fix
		 * @throws SmsFixException
		 *             if something went wrong (eg. missing data in Intent)
		 */
		private List<SmsToFix> parseSmsFromIntent(Intent intent)
				throws SmsFixException {
			try {
				List<SmsToFix> list = new ArrayList<SmsToFix>();
				String scAddress[] = intent
						.getStringArrayExtra(EXTRA_SC_ADRESSES);
				String body[] = intent.getStringArrayExtra(EXTRA_BODIES);
				String originator[] = intent
						.getStringArrayExtra(EXTRA_ORIGINATORS);
				long scTimestamp[] = intent
						.getLongArrayExtra(EXTRA_SC_TIMESTAMPS);

				for (int i = 0; i < body.length; i++) {
					SmsToFix fixMe = new SmsToFix(scAddress[i], body[i],
							originator[i], scTimestamp[i]);
					list.add(fixMe);
				}
				// Log.d(TAG, "Service: parseSmsFromIntent returns " +
				// list.size()
				// + " sms");
				return list;
			} catch (Exception e) {
				Log.w(TAG, "Service: parseSmsFromIntent catched an exception: "
						+ e + " " + e.getMessage(), e);
				throw new SmsFixException(e.getMessage());
			}
		}

		/**
		 * Preocess the list of sms to fix
		 * 
		 * @param list
		 * @return <code>true</code> if all SMS could get fixed,
		 *         <code>false</code> otherwise
		 */
		private boolean processSmsList(List<SmsToFix> list) {
			// Log.d(TAG, "Service: processSmsList got " + list.size()
			// + " sms to fix.");
			for (int i = 0; i < NUM_RETRIES; i++) {
				SD.log("SmsTimeFixService [WorkerThread]: Waiting until SMS is inserted in database....");
				// wait some initial time, until sms is stored into database
				goSleeping(5000);
				SD.log("SmsTimeFixService [WorkerThread]: processSmsList starting run #"
						+ i);
				Log.v(TAG, "Service: processSmsList starting run #" + i);

				for (SmsToFix sms : list) {
					if (fixSms(sms)) {
						list.remove(sms);
					}
				}
				if (list.isEmpty()) {
					Log.v(TAG, "Service: processSmsList after run #" + i
							+ " list is empty and we will exit");
					SD.log("SmsTimeFixService [WorkerThread]: processSmsList after run #"
							+ i + ": list is empty and we will exit");
					return true;
				} else {
					Log.v(TAG,
							"Service: processSmsList after run #"
									+ i
									+ " list still not empty and we will go sleeping and try again");
					SD.log("SmsTimeFixService [WorkerThread]: processSmsList after run #"
							+ i
							+ ": list still not empty and we will go sleeping and try again");

					// increase sleep duration: 5s, 10s, 15s, 20s, 25s
					int sleepDuration = 5000 + (5000 * i);
					goSleeping(sleepDuration);
				}
			}
			Log.d(TAG,
					"Service: processSmsList finished. Sms todo list has now length: "
							+ list.size());
			SD.log("SmsTimeFixService [WorkerThread]: Sms todo list has now length: "
					+ list.size());
			return (list.isEmpty());
		}

		/**
		 * Fixes a single sms
		 * 
		 * @param sms
		 * @return <code>true</code> if SMS could be found and fixed,
		 *         <code>false</code> otherwise
		 */
		private boolean fixSms(SmsToFix sms) {
			try {
				Cursor cur = findSmsInDb(sms);
				return updateSmsInDB(cur, sms);
			} catch (SmsNotFoundException se) {
				Log.w(TAG, "SMS not found in DB: " + se.getMessage());
				SD.log("SmsTimeFixService [WorkerThread]: SMS not found in DB: "
						+ se.getMessage());
				return false;
			} catch (SmsFixException e) {
				Log.w(TAG, "Service: fix sms catched exception.", e);
				SD.log("SmsTimeFixService [WorkerThread]: fix sms catched an exception. "
						+ e + ": " + e.getMessage());
				return false;
			}
		}

		/**
		 * Tries to find a matching SMS in the database based on the given
		 * infos. Only returns the id if the sms could be identified in DB
		 * unambigous.
		 * 
		 * @param sms
		 * @return the id of the sms in the SMS database
		 * @throws SmsFixException
		 *             if the sms could not be found
		 */
		private Cursor findSmsInDb(SmsToFix sms) throws SmsFixException {
			Cursor cur = null;
			try {
				Uri smsUri = Uri.parse(CONTENT_SMS_URI);

				// ASSUMPTION: critical fields are not null and empty (body,
				// timestamp)
				// (we check this before in the filter method in the receiver)

				// if text is shorter than 145 chars, we do not use the LIKE
				// operator but = instead.
				//
				// (Info: We need the LIKE operator for
				// multi-sms messages, because we can search only with the text
				// of the first sms-part.
				// But on the other side we want prevent that "Hello" and
				// "Hallo there!" are matched (with the LIKE operator).
				// Therefore the length-based branch.)

				// Log.v(TAG, "Service: looking for SMS in DB: body:'" +
				// sms.body
				// + "', origin:" + sms.originator + ", scAddr:"
				// + sms.scAddress + ", timest:" + sms.scTimestamp);

				String selection;
				String selectionArgBody;
				String[] selectionArgs;

				// create the body selection string according to length
				String selBodyString;
				if (sms.body.length() < 145) {
					selBodyString = COL_BODY + "=?";
					selectionArgBody = sms.body;
				} else {
					selBodyString = COL_BODY + " LIKE ?";
					selectionArgBody = sms.body + "%";
				}
				// create selection string depending on which fields are
				// available
				if (sms.originator != null && sms.scAddress != null) {
					selection = COL_TYPE + "=? AND " + COL_ADDRESS + "=? AND "
							+ COL_SERVICE_CENTER + "=? AND " + selBodyString;
					selectionArgs = new String[] { "1", sms.originator,
							sms.scAddress, selectionArgBody };
				} else if (sms.originator == null && sms.scAddress != null) {
					selection = COL_TYPE + "=? AND " + COL_SERVICE_CENTER
							+ "=? AND " + selBodyString;
					selectionArgs = new String[] { "1", sms.scAddress,
							selectionArgBody };
				} else if (sms.originator != null && sms.scAddress == null) {
					selection = COL_TYPE + "=? AND " + COL_ADDRESS + "=? AND "
							+ selBodyString;
					selectionArgs = new String[] { "1", sms.originator,
							selectionArgBody };
				}
				// both are null
				else {
					selection = COL_TYPE + "=? AND " + selBodyString;
					selectionArgs = new String[] { "1", selectionArgBody };
				}

				cur = getContentResolver().query(
						smsUri,
						new String[] { COL_ID, COL_ADDRESS, COL_DATE, COL_BODY,
								COL_SERVICE_CENTER }, selection, selectionArgs,
						"_id DESC LIMIT 1");
			} catch (Exception e) {
				// try to close the cursor
				if (cur != null) {
					try {
						cur.close();
					} catch (Exception e1) {
						// do nothing if fails
					}
				}
				throw new SmsFixException(
						"Service: Exception while looking for SMS in DB.", e);
			}
			if (cur.getCount() == 1) {
				return cur;
			} else {
				throw new SmsNotFoundException(
						"Service: SMS not found or ambigous: Cursor contained "
								+ cur.getCount() + " results");
			}
		}

		/**
		 * Tries to update the sms in the database with the correct timestamp.
		 * We will close the cursor in this method afterwards.
		 * 
		 * @param cur
		 *            cursor for update in database, should only contain one row
		 * @param sms
		 * @return <code>true</code> if everything went ok, <code>false</code>
		 *         otherwise
		 */
		private boolean updateSmsInDB(Cursor cur, SmsToFix sms)
				throws SmsFixException {
			// Log.d(TAG, "Service: updateSmsInDB called");
			try {
				cur.moveToFirst();
				int bodyColumn = cur.getColumnIndex(COL_BODY);
				int idColumn = cur.getColumnIndex(COL_ID);
				int id = cur.getInt(idColumn);
				String bodyTxt = cur.getString(bodyColumn);
				ContentValues values = new ContentValues();

				// Only if logging to SD is enabled (to save work otherwise)
				if (SD.isLoggingEnabled()) {
					SimpleDateFormat sdf = new SimpleDateFormat(
							"yyyy-MM-dd'T'HH:mm:ss.SSSZ");
					SD.log("SmsTimeFixService [WorkerThread]: SMS details: sent_time as received from the SMSC: '"
							+ sdf.format(new Date(sms.scTimestamp))
							+ "', SMSC-number: '"
							+ sms.scAddress
							+ "', originator: '" + sms.originator + "'");
				}
				String stringToAdd = getStringToAdd(
						correctTimestampOffset(sms.scTimestamp), sms.scAddress);
				values.put(COL_BODY, bodyTxt + " " + stringToAdd);

				if (SD.isLoggingEnabled()) {
					SD.log("SmsTimeFixService [WorkerThread]: Will add the following string: '"
							+ stringToAdd + "'");
				}

				Uri smsUri = Uri.parse(CONTENT_SMS_URI);
				// Log.d(TAG,
				// "Service: updateSmsInDB: will update entry in DB now. MSG-Id: "
				// + id);
				SD.log("SmsTimeFixService [WorkerThread]: Will perform database update on SMS with id: "
						+ id);
				getContentResolver().update(smsUri, values, COL_ID + "=?",
						new String[] { Integer.toString(id) });
				// Log.d(TAG, "Service: updateSmsInDB: update DONE!");
				return true;
			} catch (Exception e) {
				Log.w(TAG, "Service: Exception while DB update: ", e);
				SD.log("SmsTimeFixService [WorkerThread]: Sorry, there was an exception while updating the SMS DB: "
						+ e + ": " + e.getMessage());
				return false;
			} finally {
				// try to close the cursor
				if (cur != null) {
					try {
						cur.close();
					} catch (Exception e1) {
						// do nothing if fails
					}
				}
			}
		}

		/**
		 * Repairs the timestamp (correct the offset) if enabled in settings.
		 * 
		 * @param timestamp
		 * @return
		 */
		private long correctTimestampOffset(long timestamp) {
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(getApplicationContext());
			Calendar cal = Calendar.getInstance();
			if (SD.isLoggingEnabled()) {
				SD.log("SmsTimeFixService [WorkerThread]: Timezone details: ZONE_OFFSET="
						+ (cal.get(Calendar.ZONE_OFFSET) / 60000)
						+ ", DST_OFFSET="
						+ (cal.get(Calendar.DST_OFFSET) / 60000));
			}
			if (prefs.getBoolean(PREF_TIMEZONE_FIX_1, false)) {
				SD.log("SmsTimeFixService [WorkerThread]: Timezonefix#1 is ENABLED. Will modify SMS timestamp by timezone/DST offset.");
				timestamp = timestamp
						- (cal.get(Calendar.ZONE_OFFSET) + cal
								.get(Calendar.DST_OFFSET));
			} else {
				SD.log("SmsTimeFixService [WorkerThread]: Timezonefix#1 is DISABLED. Will keep SMS timestamp as is.");
			}
			return timestamp;
		}

		/**
		 * Returns the string to add, according to apps prefernces
		 * 
		 * @param scTimestamp
		 * @return
		 */
		private String getStringToAdd(long scTimestamp, String smscAddr) {
			// Log.v(TAG, "Service: formatDateTime");
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(getApplicationContext());
			Date smscTime = new Date(scTimestamp);
			String dateStr = new SimpleDateFormat(prefs.getString(
					PREF_DATE_FORMAT,
					getString(R.string.p_default_date_pattern)))
					.format(smscTime);
			SimpleDateFormat timeFormatter;
			if (prefs.getBoolean(PREF_USE24H_FORMAT, true)) {
				timeFormatter = new SimpleDateFormat("HH:mm:ss");
			} else {
				timeFormatter = new SimpleDateFormat("h:mm:ss a");
			}

			// check if smsc is null (like on emulator)
			if (smscAddr == null) {
				smscAddr = getString(R.string.g_smsc_unknown);
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
			// Log.v(TAG, "Service: formatDateTime returning: " + textToAppend);
			return textToAppend;
		}

		/**
		 * Send thread to sleep
		 * 
		 * @param ms
		 *            milliseconds
		 */
		private void goSleeping(int ms) {
			try {
				Thread.sleep(ms);
			} catch (InterruptedException e) {
				Log.w(TAG, "Service: Thread sleep was interrupted.");
			}
		}

	}

}
