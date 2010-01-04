/*
 * Copyright (C) 2010 Felix Bechstein
 * 
 * This file is part of WebSMS.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */
package de.ub0r.android.andGMXsms;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;

import android.util.Log;

/**
 * AsyncTask to manage IO to cherry-sms.com API.
 * 
 * @author flx
 */
public class ConnectorCherrySMS extends Connector {
	/** Tag for output. */
	private static final String TAG = "WebSMS.cherry";

	/** CherrySMS Gateway URL. */
	private static final String URL = "https://gw.cherry-sms.com/";

	/** CherrySMS connector. */
	private final boolean sendWithSender;

	/**
	 * Create an CherrySMS Connector..
	 * 
	 * @param u
	 *            user
	 * @param p
	 *            password
	 * @param con
	 *            connector type
	 */
	public ConnectorCherrySMS(final String u, final String p, final short con) {
		super(u, p, con);
		switch (con) {
		case CHERRY_WO_SENDER:
			this.sendWithSender = false;
			break;
		case CHERRY_W_SENDER:
			this.sendWithSender = true;
			break;
		default:
			this.sendWithSender = false;
			break;
		}
	}

	/**
	 * Check return code from cherry-sms.com.
	 * 
	 * @param ret
	 *            return code
	 * @return true if no error code
	 * @throws WebSMSException
	 *             WebSMSException
	 */
	private boolean checkReturnCode(final int ret) throws WebSMSException {
		Log.d(TAG, "ret=" + ret);
		switch (ret) {
		case 100:
			return true;
		case 10:
			throw new WebSMSException(this.context,
					R.string.log_error_cherry_10);
		case 20:
			throw new WebSMSException(this.context,
					R.string.log_error_cherry_20);
		case 30:
			throw new WebSMSException(this.context,
					R.string.log_error_cherry_30);
		case 31:
			throw new WebSMSException(this.context,
					R.string.log_error_cherry_31);
		case 40:
			throw new WebSMSException(this.context,
					R.string.log_error_cherry_40);
		case 50:
			throw new WebSMSException(this.context,
					R.string.log_error_cherry_50);
		case 60:
			throw new WebSMSException(this.context,
					R.string.log_error_cherry_60);
		case 70:
			throw new WebSMSException(this.context,
					R.string.log_error_cherry_70);
		case 71:
			throw new WebSMSException(this.context,
					R.string.log_error_cherry_71);
		case 80:
			throw new WebSMSException(this.context,
					R.string.log_error_cherry_80);
		case 90:
			throw new WebSMSException(this.context,
					R.string.log_error_cherry_90);
		default:
			throw new WebSMSException(this.context, R.string.log_error,
					" code: " + ret);
		}
	}

	/**
	 * Send data.
	 * 
	 * @return successful?
	 * @throws WebSMSException
	 *             WebSMSException
	 */
	private boolean sendData() throws WebSMSException {
		// do IO
		try { // get Connection
			final StringBuilder url = new StringBuilder(URL);
			url.append("?user=");
			url.append(this.user);
			url.append("&password=");
			url.append(this.password);

			if (this.text != null && this.to != null && this.to.length > 0) {
				Log.d(TAG, "send with sender = " + this.sendWithSender);
				if (this.sendWithSender) {
					url.append("&from=1");
				}
				url.append("&message=");
				url.append(URLEncoder.encode(this.text));
				url.append("&to=");
				String[] recvs = this.to;
				final int e = recvs.length;
				StringBuilder toBuf = new StringBuilder(
						international2oldformat(recvs[0]));
				for (int j = 1; j < e; j++) {
					toBuf.append(";");
					toBuf.append(international2oldformat(recvs[j]));
				}
				url.append(toBuf.toString());
				toBuf = null;
			} else {
				url.append("&check=guthaben");
			}
			// send data
			HttpResponse response = getHttpClient(url.toString(), null, null,
					null, null);
			int resp = response.getStatusLine().getStatusCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				throw new WebSMSException(this.context,
						R.string.log_error_http, "" + resp);
			}
			String htmlText = stream2str(response.getEntity().getContent())
					.trim();
			String[] lines = htmlText.split("\n");
			Log.d(TAG, htmlText);
			htmlText = null;
			int l = lines.length;
			WebSMS.SMS_BALANCE[CHERRY] = lines[l - 1].trim();
			this.pushMessage(WebSMS.MESSAGE_FREECOUNT, null);
			if (l > 1) {
				final int ret = Integer.parseInt(lines[0].trim());
				return this.checkReturnCode(ret);
			}
		} catch (IOException e) {
			Log.e(TAG, null, e);
			throw new WebSMSException(e.getMessage());
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final boolean updateMessages() throws WebSMSException {
		return this.sendData();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final boolean sendMessage() throws WebSMSException {
		if (!this.sendData()) {
			// failed!
			throw new WebSMSException(this.context, R.string.log_error);
		} else {
			// result: ok
			return true;
		}
	}
}
