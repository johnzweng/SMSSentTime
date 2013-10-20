package at.zweng.smssenttimefix;

/**
 * Custom exception class for handling error conditions gracefully.
 * 
 * @author John Zweng
 */
public class SmsFixException extends Exception {

	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = -5470132218974045441L;

	/**
	 * Constructor. Default constructor
	 */
	public SmsFixException() {
	}

	/**
	 * Constructor.
	 * 
	 * @param detailMessage
	 * @param throwable
	 */
	public SmsFixException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	/**
	 * Constructor.
	 * 
	 * @param detailMessage
	 */
	public SmsFixException(String detailMessage) {
		super(detailMessage);
	}

	/**
	 * Constructor.
	 * 
	 * @param throwable
	 */
	public SmsFixException(Throwable throwable) {
		super(throwable);
	}

}
