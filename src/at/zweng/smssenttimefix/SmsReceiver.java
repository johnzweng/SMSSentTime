package at.zweng.smssenttimefix;

import static at.zweng.smssenttimefix.Constants.ACTION_SMS_RCVD;
import static at.zweng.smssenttimefix.Constants.DEBUG_ENABLED_DEFAULT_VALUE;
import static at.zweng.smssenttimefix.Constants.EXTRA_BODIES;
import static at.zweng.smssenttimefix.Constants.EXTRA_ORIGINATORS;
import static at.zweng.smssenttimefix.Constants.EXTRA_SC_ADRESSES;
import static at.zweng.smssenttimefix.Constants.EXTRA_SC_TIMESTAMPS;
import static at.zweng.smssenttimefix.Constants.PREF_DBG_DEBUG_ENABLED;
import static at.zweng.smssenttimefix.Constants.PREF_DBG_FILENAME;
import static at.zweng.smssenttimefix.Constants.PREF_SVC_ENABLED;
import static at.zweng.smssenttimefix.Constants.TAG;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.gsm.SmsMessage;
import android.util.Log;
import android.widget.Toast;

/**
 * Broadcast receiver, registered to get informed about new incoming SMS by the
 * Android framework. If we get a new SMS we remember the original sent
 * timestamp from the SMSC and start a service which changes the date in the
 * SMS-DB after the sms got stored by the SMS application.
 * 
 * @author John Zweng
 */

// Info: we use the deprecated class "SmsMessage" to
// ensure compatibility to older Android SDK levels.
@SuppressWarnings("deprecation")
public class SmsReceiver extends BroadcastReceiver {

	/**
	 * our context we are running in
	 */
	private Context ctx;

	/**
	 * SD card logger
	 */
	private static SDCardLogger SD;

	/**
	 * Gets called from Android framework whenever a SMS is received
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(TAG, "BroadcastReceiver: Incoming SMS received.");

		// get logger
		SD = SDCardLogger.getLogger(context.getSharedPreferences(
				PREF_DBG_FILENAME, 0).getBoolean(PREF_DBG_DEBUG_ENABLED,
				DEBUG_ENABLED_DEFAULT_VALUE));

		// only if logging is really enabled
		if (SD.isLoggingEnabled()) {
			Toast toast = Toast.makeText(context,
					context.getString(R.string.dbg_toastOnSMSReceived),
					Toast.LENGTH_LONG);
			toast.show();
		}

		SD.log("BroadcastReceiver: Android notified us that an incoming SMS arrived (onReceive broadcast)");
		try {
			this.ctx = context;
			if (!PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
					PREF_SVC_ENABLED, true)) {
				// Log.i(TAG,
				// "BroadcastReceiver: Received SMS, but SMS Time Fix DISABLED"
				// + " in preferences. Will do nothing and exit immediately!");
				SD.log("BroadcastReceiver: SMS Sent Time is DISABLED in preferences, will do nothing.");
				return;
			}

			// Do some checks and log:
			if (intent == null) {
				Log.w(TAG,
						"BroadcastReceiver: Received SMS, but Intent was null. Will exit and do nothing.");
				SD.log("BroadcastReceiver: Received SMS, but Intent was null. Will exit and do nothing.");
				return;
			}
			if (intent.getAction() == null) {
				Log.w(TAG,
						"BroadcastReceiver: Received SMS, but Intent action was null. Will exit and do nothing");
				SD.log("BroadcastReceiver: Received SMS, but Intent action was null. Will exit and do nothing");
				return;
			}
			SD.log("BroadcastReceiver: SMS Sent Time is ENABLED.");
			String action = intent.getAction();
			// Log.d(TAG, "BroadcastReceiver: Received intent action: " +
			// action);

			if (!action.equals(ACTION_SMS_RCVD)) {
				Log.w(TAG,
						"BroadcastReceiver: Intent action was not for an incoming SMS. Will exit and do nothing.");
				SD.log("BroadcastReceiver: Intent action was not for an incoming SMS. Will exit and do nothing.");
				return;
			}

			// Get out the message pdrotocol data units (PDUs), convert them
			// into message objects and start the Fix service for them
			Bundle bundle = intent.getExtras();
			SmsMessage sms[] = constructSmsFromPDUs((Object[]) bundle
					.get("pdus"));
			// Filter messages before we start the service
			// (we do not send every message to the fix service
			// -> see filter method)
			SD.log("BroadcastReceiver: Extracted " + sms.length
					+ " sms objects from PDU. Will start SmsTimeFixService.");
			startFixServiceForSMS(filterMessages(sms));

		} catch (Exception t) {
			Log.e(TAG,
					"BroadcastReceiver: SOMETHING WENT WRONG! Catched Exception:  "
							+ t, t);
			SD.log("BroadcastReceiver: SOMETHING WENT WRONG! Catched Exception:  "
					+ t);
		} finally {
			Log.d(TAG,
					"BroadcastReceiver: Work successfully done. :-) Will exit now. GOOD BYE!");
			SD.log("BroadcastReceiver: Receiver is finished now and will exit. GOOD BYE!");
		}
	}

	/**
	 * Construct SMS java objects from raw data bytes
	 * 
	 * @param rawPduData
	 * @return
	 */
	private SmsMessage[] constructSmsFromPDUs(Object[] rawPduData) {
		SmsMessage smsMessages[] = new SmsMessage[rawPduData.length];
		for (int n = 0; n < rawPduData.length; n++) {
			smsMessages[n] = SmsMessage.createFromPdu((byte[]) rawPduData[n]);
			// remove debug
			// Log.i(TAG, "BroadcastReceiver: sms #" + n + " body: '"
			// + smsMessages[n].getMessageBody() + "'");
		}
		// Log.d(TAG, "BroadcastReceiver: Parsed " + rawPduData.length
		// + " SMS message objects from raw sms data PDUs.");
		return smsMessages;
	}

	/**
	 * Takes a bunch of messages and filters out 'system' messages like delivery
	 * reports and others like that. Also returns only one message as I realized
	 * later that all messages in the array belong to the same SMS
	 * (multipart-sms) and for our check it's sufficcient to look at the first
	 * one (in the most cases).
	 * 
	 * @param msgs
	 * @return
	 */
	private SmsMessage[] filterMessages(SmsMessage msgs[]) {
		// Log.d(TAG, "BroadcastReceiver: filterMessages: got " + msgs.length
		// + " msgs");
		List<SmsMessage> filtered = new ArrayList<SmsMessage>();

		// ASSUMPTION: at the moment we always return just 1 msg!!
		// Ich bin mir nicht sicher, ob das in allen Handynetzen so
		// funktioniert, aber wenn man überlange SMS sendet, kommen mehrere
		// PDUs. Ich gehe daher mal davon aus, dass mehrere PDUs IMMER zur
		// selben SMS gehören. --> ANNAHME STIMMT!! Habe Sourceode von 2.2.1
		// gecheckt:
		/*
		 * Broadcast Action: A new text based SMS message has been received by
		 * the device. The intent will have the following extra values:</p>
		 * 
		 * <ul> <li><em>pdus</em> - An Object[] od byte[]s containing the PDUs
		 * that make up the message.</li> </ul>
		 * 
		 * <p>The extra values can be extracted using {@link
		 * #getMessagesFromIntent(Intent)}.</p>
		 * 
		 * <p>If a BroadcastReceiver encounters an error while processing this
		 * intent it should set the result code appropriately.</p>
		 */
		for (SmsMessage sms : msgs) {

			if (sms.isEmail()) {
				SD.log("BroadcastReceiver: filterMessages: SMS has 'isEmail' flag. Will not process it (because we don't know if this may brteak something).");
				continue;
			}
			if (sms.isCphsMwiMessage()) {
				SD.log("BroadcastReceiver: filterMessages: SMS has 'isCphsMwiMessage' flag.");
				// continue;
			}
			if (sms.isMWIClearMessage()) {
				SD.log("BroadcastReceiver: filterMessages: SMS has 'isMWIClearMessage' flag.");
				// continue;
			}
			if (sms.isMwiDontStore()) {
				SD.log("BroadcastReceiver: filterMessages: SMS has 'isMwiDontStore' flag.");
				// continue;
			}
			if (sms.isMWISetMessage()) {
				SD.log("BroadcastReceiver: filterMessages: SMS has 'isMWISetMessage' flag.");
				// continue;
			}
			if (sms.isStatusReportMessage()) {
				SD.log("BroadcastReceiver: filterMessages: SMS has 'isStatusReportMessage' flag. Will not process it.");
				SD.log("BroadcastReceiver: filterMessages: status field has value: "
						+ sms.getStatus());
				continue;
			}

			// check body
			if (sms.getMessageBody() == null) {
				SD.log("BroadcastReceiver: filterMessages: SMS message body is null. Will ignore this sms.");
				continue;
			}
			if (sms.getMessageBody().length() == 0) {
				SD.log("BroadcastReceiver: filterMessages: SMS message body length == 0. Will ignore this sms.");
				continue;
			}
			// check timestamp (just simple check if it's not 0 or kind of..
			if (sms.getTimestampMillis() < 5000L) {
				SD.log("BroadcastReceiver: filterMessages: SMS message timestamp is invalid: "
						+ sms.getTimestampMillis() + ". Will ignore this sms.");
				continue;
			}

			filtered.add(sms);
			// INFO: assumption that we always only need to
			// inspect the first pdu sms:
			if (filtered.size() >= 1) {
				SD.log("BroadcastReceiver: filterMessages: We have >=1 pdu sms in the filtered work queue.");
				break;
			}
		}
		// Log.d(TAG,
		// "BroadcastReceiver: filterMessages: will return "
		// + filtered.size() + " msgs");
		return filtered.toArray(new SmsMessage[filtered.size()]);
	}

	/**
	 * Starts the worker service which wll change the timestamp in DB
	 * 
	 * @param msgs
	 */
	private void startFixServiceForSMS(SmsMessage msgs[]) {
		// Log.v(TAG,
		// "BroadcastReceiver: Constructing intent for service start.");
		if (msgs.length == 0) {
			Log.i(TAG,
					"BroadcastReceiver: After sorting out special sms we have 0 sms messages "
							+ "left to process. Will do nothing and exit. GOOD BYE!");
			SD.log("BroadcastReceiver: After sorting out special sms we have 0 sms messages "
					+ "left to process. Will do nothing and exit. GOOD BYE!");
			return;
		}

		Intent svcIntent = new Intent(ctx, SmsTimeFixService.class);

		// pack the data into the intent
		String scAddress[] = new String[msgs.length];
		String body[] = new String[msgs.length];
		String originator[] = new String[msgs.length];
		long scTimestamp[] = new long[msgs.length];
		for (int i = 0; i < msgs.length; i++) {
			scAddress[i] = msgs[i].getServiceCenterAddress();
			body[i] = msgs[i].getMessageBody();
			originator[i] = msgs[i].getOriginatingAddress();
			scTimestamp[i] = msgs[i].getTimestampMillis();
		}
		svcIntent.putExtra(EXTRA_SC_ADRESSES, scAddress);
		svcIntent.putExtra(EXTRA_BODIES, body);
		svcIntent.putExtra(EXTRA_ORIGINATORS, originator);
		svcIntent.putExtra(EXTRA_SC_TIMESTAMPS, scTimestamp);

		// Start the service
		ctx.startService(svcIntent);
	}

}
