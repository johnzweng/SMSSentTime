package at.zweng.smssenttimefix;

import static at.zweng.smssenttimefix.Constants.*;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/**
 * @author John Zweng
 * 
 */
public class Util {

	/**
	 * Returns app version string.
	 * 
	 * @param ctx
	 * @return
	 */
	public static String getAppVersion(Context ctx) {
		PackageManager pm = ctx.getPackageManager();
		try {
			PackageInfo info = pm.getPackageInfo(Util.class.getPackage()
					.getName(), PackageManager.GET_ACTIVITIES);
			if (info == null) {
				return "?";
			}
			return info.versionName;
		} catch (NameNotFoundException e) {
			return "?";
		}
	}

	/**
	 * Return appropriate credits dialog
	 */
	public static Dialog getCreditsDialog(Context ctx) {
		final Dialog dialog = new Dialog(ctx);

		dialog.setContentView(R.layout.credit_dialog);
		dialog.setTitle(R.string.cr_title);

		TextView text = (TextView) dialog.findViewById(R.id.creditDialog_text);
		text.setText(creditText(ctx));

		// close button
		Button close = (Button) dialog.findViewById(R.id.btnCreditDialogOk);
		close.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		return dialog;

	}

	/**
	 * Return appropriate info (whats this) dialog
	 */
	public static Dialog getAboutDialog(Context ctx) {
		final Dialog dialog = new Dialog(ctx);

		dialog.setContentView(R.layout.whatsthis_dialog);
		dialog.setTitle(R.string.info_title);

		TextView text = (TextView) dialog.findViewById(R.id.aboutDialog_text);
		text.setText(aboutText(ctx));

		// close button
		Button close = (Button) dialog.findViewById(R.id.btnInfoDialogOk);
		close.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		return dialog;
	}

	/**
	 * Return appropriate info (whats this) dialog
	 */
	public static Dialog getGoSMSInfoDialog(final Context ctx) {
		final Dialog dialog = new Dialog(ctx);

		dialog.setContentView(R.layout.gosms_warning_dialog);
		dialog.setTitle(R.string.gosms_info_title);

		TextView text = (TextView) dialog.findViewById(R.id.warnDialog_text);
		text.setText(goSMSWarnText(ctx));

		// close button
		Button close = (Button) dialog.findViewById(R.id.gosms_info_btn_Ok);
		close.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SharedPreferences.Editor editor = PreferenceManager
						.getDefaultSharedPreferences(ctx).edit();
				editor.putBoolean(PREF_GO_SMS_WARNING_SHOWN, true);
				// Don't forget to commit your edits!!!
				editor.commit();
				dialog.dismiss();
			}
		});
		return dialog;
	}

	/**
	 * info text
	 * 
	 * @param ctx
	 * @return
	 */
	private static Spanned aboutText(Context ctx) {
		StringBuilder sb = new StringBuilder();
		sb.append("<b><font color=\"#e7d832\">");
		sb.append(ctx.getString(R.string.info_head_1));
		sb.append("</font></b><br/> ");
		sb.append(ctx.getString(R.string.info_text_1));

		sb.append("<br/><br/><br/>");
		sb.append("<b><font color=\"#e7d832\">");
		sb.append(ctx.getString(R.string.info_head_2));
		sb.append("</font></b><br/> ");
		sb.append(ctx.getString(R.string.info_text_2));

		sb.append("<br/><br/><br/>");
		sb.append("<b><font color=\"#e7d832\">");
		sb.append(ctx.getString(R.string.info_head_3));
		sb.append("</font></b><br/> ");
		sb.append(ctx.getString(R.string.info_text_3));

		sb.append("<br/><br/><br/>");
		sb.append("<b><font color=\"#e7d832\">");
		sb.append(ctx.getString(R.string.info_head_4));
		sb.append("</font></b><br/> ");
		sb.append(ctx.getString(R.string.info_text_4));

		return Html.fromHtml(sb.toString());
	}

	/**
	 * go sms warn text
	 * 
	 * @param ctx
	 * @return
	 */
	private static Spanned goSMSWarnText(Context ctx) {
		StringBuilder sb = new StringBuilder();
		sb.append("<b><font color=\"#e7d832\">");
		sb.append(ctx.getString(R.string.gosms_info_head_1));
		sb.append("</font></b><br/> ");
		sb.append(ctx.getString(R.string.gosms_info_text_1));
		return Html.fromHtml(sb.toString());
	}

	/**
	 * credit text
	 * 
	 * @param ctx
	 * @return
	 */
	private static Spanned creditText(Context ctx) {
		StringBuilder sb = new StringBuilder();
		sb.append("<b><font color=\"#e7d832\">Version:</font></b> ");
		sb.append(getAppVersion(ctx));
		sb.append("<br/><br/>");
		sb.append("<b><font color=\"#e7d832\">Author:</font></b><br/>Johannes Zweng<br/><a ");
		sb.append("href=\"mailto:android-dev2011@zweng.at?subject=SmsSentTimeFix\">");
		sb.append("android-dev2011@zweng.at</a><br/><br/>");
		sb.append("<b><font color=\"#e7d832\">Icon credits:</font></b><br/>");
		sb.append("Thanks to Everaldo Coelho (<a href=\"http://www.everaldo.com/crystal\">www.everaldo.com/crystal</a>) for the nicely designed clock logo (released under LGPL).");
		return Html.fromHtml(sb.toString());
	}

}
