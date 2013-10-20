package at.zweng.smssenttimefix;

/**
 * @author 'Johannes Zweng, <johnny@zweng.at>'
 *
 */
public class SDNotMountedException extends Exception {

	/**
	 * serial version uid
	 */
	private static final long serialVersionUID = 8918041460330499214L;

	public SDNotMountedException() {
		super();
	}

	public SDNotMountedException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public SDNotMountedException(String detailMessage) {
		super(detailMessage);
	}

	public SDNotMountedException(Throwable throwable) {
		super(throwable);
	}

}
