package at.zweng.smssenttimefix;

import static at.zweng.smssenttimefix.Constants.SD_CARD_LOGFILENAME;
import static at.zweng.smssenttimefix.Constants.TAG;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Environment;
import android.util.Log;

/**
 * Creates a logfile on the sdcard.
 * 
 * @author 'Johannes Zweng, <johnny@zweng.at>'
 * 
 */
public class SDCardLogger {

	private static SDCardLogger SINGLETON = null;
	private BufferedWriter writer;
	private final SimpleDateFormat sdf = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss.SSS");
	private final static String TEST_MESSAGE = "This is just a test message to check if the logging is working. Seems to be fine. :-)";
	private boolean loggingEnabled = false;

	/**
	 * @return the loggingEnabled
	 */
	public boolean isLoggingEnabled() {
		return loggingEnabled;
	}

	/**
	 * @param loggingEnabled
	 *            the loggingEnabled to set
	 */
	public void setLoggingEnabled(boolean loggingEnabled) {
		Log.d(TAG, "Logging enabled was set to: " + loggingEnabled);
		this.loggingEnabled = loggingEnabled;
	}

	/**
	 * private constructor
	 */
	private SDCardLogger() {
	}

	/**
	 * Call this to get the logger singleton object. I know, this looks strange
	 * with the loggingEnabled parameter, but I do not want to have this class
	 * reading the preferences itself, to avoid that it has to carry a
	 * (memory-)heavy Context object around. All calling classes already have a
	 * context and so there is no loss. :-)
	 * 
	 * @param loggingEnabled
	 *            if logging is enabled in the preferences
	 * @return
	 */
	public static SDCardLogger getLogger(boolean loggingEnabled) {
		if (SINGLETON == null) {
			SINGLETON = new SDCardLogger();
		}
		SINGLETON.setLoggingEnabled(loggingEnabled);
		return SINGLETON;
	}

	/**
	 * Log a message, will be immediately flushed (synced).
	 * 
	 * @param msg
	 */
	public void log(String msg) {
		if (!loggingEnabled) {
			// do nothing if not enabled
			return;
		}
		if (!Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			// do nothing if SD card not mounted
			return;
		}
		try {
			writer = new BufferedWriter(new FileWriter(new File(
					Environment.getExternalStorageDirectory(),
					SD_CARD_LOGFILENAME), true));
			writer.write("[" + sdf.format(new Date()) + "]: " + msg + "\r\n");
			writer.flush();
			writer.close();
		} catch (Exception e) {
			Log.w(TAG, "Execption while writing to sdcard logfile (msg: '"
					+ msg + "') ", e);
		}
	}

	/**
	 * Test logging
	 */
	public void testlogging() throws IOException, SDNotMountedException {

		String sdState = Environment.getExternalStorageState();
		if (!sdState.equals(Environment.MEDIA_MOUNTED)) {
			throw new SDNotMountedException(
					"SD card seems not to be mounted. Current state is: "
							+ sdState);
		}

		writer = new BufferedWriter(new FileWriter(
				new File(Environment.getExternalStorageDirectory(),
						SD_CARD_LOGFILENAME), true));
		writer.write("[" + sdf.format(new Date()) + "]: " + TEST_MESSAGE
				+ "\r\n");
		writer.flush();
		writer.close();
	}

	/**
	 * Deletes the logfile
	 */
	public void removeLogfile() throws IOException {
		File logfile = new File(Environment.getExternalStorageDirectory(),
				SD_CARD_LOGFILENAME);
		// do nothing if logfile did not exist
		if (!logfile.exists()) {
			return;
		}
		if (!logfile.delete()) {
			throw new IOException("Could not delete file.");
		}
		if (logfile.exists()) {
			throw new IOException("Could not delete file.");
		}
	}

	/**
	 * @return the full path to the logfile.
	 */
	public String getLogfilePath() {
		return new File(Environment.getExternalStorageDirectory(),
				SD_CARD_LOGFILENAME).getAbsolutePath();
	}
}
