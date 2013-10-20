package at.zweng.smssenttimefix;

/**
 * Some constants
 * 
 * @author John Zweng
 */
public final class Constants {

//	public final static boolean SD_CARD_LOGGING  = true;
	
	/**
	 * File name of sdcard logfile. will be placed in root dir.
	 */
	public final static String SD_CARD_LOGFILENAME ="SMS_Sent_Time_LOGFILE.txt";
	
	/**
	 * Tag used in logcat logger. Will be used for all logging in the
	 * application.
	 */
	public final static String TAG = "SMSSentTimeFix";

	/**
	 * Intent action for incoming SMS. We register a broadcast listener for
	 * this.
	 */
	public static final String ACTION_SMS_RCVD = "android.provider.Telephony.SMS_RECEIVED";

	/**
	 * Content provider URI for incoming sms on Android
	 */
	public static final String CONTENT_SMS_URI = "content://sms";

	// Android sms db column names
	public static final String COL_ID = "_id";
	public static final String COL_ADDRESS = "address";
	public static final String COL_DATE = "date";
	public static final String COL_BODY = "body";
	public static final String COL_SERVICE_CENTER = "service_center";
	public static final String COL_TYPE = "type";

	// Some key names for the intent extras
	// which we use to pass needed information to our fix-service
	public static final String EXTRA_SC_ADRESSES = "sms_scAdresses";
	public static final String EXTRA_BODIES = "sms_bodies";
	public static final String EXTRA_ORIGINATORS = "sms_originators";
	public static final String EXTRA_SC_TIMESTAMPS = "sms_scTimestamps";

	// keys in preferences
	// DOUBLE-CHECK: the values are also located in "preferences.xml"
	// So, changes please in both locations!
	public static final String PREF_SVC_ENABLED = "service_enabled";
	public static final String PREF_TEXT_TO_APPEND = "text_to_append";
	public static final String PREF_USE24H_FORMAT = "use_24h_format";
	public static final String PREF_DATE_FORMAT = "date_format";
	public static final String PREF_TIMEZONE_FIX_1 = "timezone_fix_no_1";
	public static final String PREF_EXAMPLE_FORMAT = "example_timestamp_format";
	public static final String PREF_GO_SMS_WARNING_SHOWN = "go_sms_warning_already_shown";

	// ids for dialogs
	// Used for creating dialog windows
	public static final int DIALOG_ID_CREDITS = 1;
	public static final int DIALOG_ID_ABOUT = 2;
	public static final int DIALOG_ID_GOSMS_WARN = 3;

	
	// Preferences for debug settings
	public static final String PREF_DBG_FILENAME = "debug_prefs";
	public static final String PREF_DBG_DEBUG_ENABLED = "debugging_enabled";
	
	/**
	 * Debug enabled DEFAULT VALUE
	 */
	public static final boolean DEBUG_ENABLED_DEFAULT_VALUE = false;
	
}
