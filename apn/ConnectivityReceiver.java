package com.heimavista.apn;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.heimavista.hvFrame.logger.Logger;
import com.heimavista.hvFrame.tools.workerThread;

/**
 * A broadcast receiver to handle the changes in network connection states.
 * 
 * @author apple
 */
public class ConnectivityReceiver extends BroadcastReceiver {

	private RmIntentService apnService;

	public ConnectivityReceiver() {

	}

	public void setApnService(RmIntentService notificationService) {
		this.apnService = notificationService;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Logger.d(getClass(), "ConnectivityReceiver.onReceive()...");

		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

		if (networkInfo != null) {
			Logger.d(getClass(), "Network Type  = " + networkInfo.getTypeName());
			Logger.d(getClass(), "Network State = " + networkInfo.getState());
			if (networkInfo.isConnected()) {
				Logger.d(getClass(), "Network connected");
				if (apnService != null)
					apnService.connect();
				workerThread.appToForeground();
				return;
			}

		}
		Logger.e(getClass(), "Network unavailable");
		if (apnService != null)
			apnService.disconnect();

	}
}
