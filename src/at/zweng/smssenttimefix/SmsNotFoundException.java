package at.zweng.smssenttimefix;

/**
 * Custom exception class for indicating that no matching sms was found.
 * 
 * @author John Zweng
 */
public class SmsNotFoundException extends SmsFixException {

	/**
	 * Serial version UID for serializable objects
	 */
	private static final long serialVersionUID = 2650351914807572663L;

	/**
	 * Default constructor
	 */
	public SmsNotFoundException() {
	}

	/**
	 * Constructor.
	 * 
	 * @param detailMessage
	 * @param throwable
	 */
	public SmsNotFoundException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	/**
	 * Constructor.
	 * 
	 * @param detailMessage
	 */
	public SmsNotFoundException(String detailMessage) {
		super(detailMessage);
	}

	/**
	 * Constructor.
	 * 
	 * @param throwable
	 */
	public SmsNotFoundException(Throwable throwable) {
		super(throwable);
	}

}
