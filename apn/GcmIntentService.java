package com.heimavista.apn;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.heimavista.hvFrame.logger.Logger;

public class GcmIntentService extends IntentService {
	public static final int NOTIFICATION_ID = 1;

	public GcmIntentService() {
		super("GcmIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle extras = intent.getExtras();
		Logger.d(getClass(), "onHandleIntent:" + extras);

		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
		String messageType = gcm.getMessageType(intent);
		if (!extras.isEmpty()) { // has effect of unparcelling Bundle
			/*
			 * Filter messages based on message type. Since it is likely that
			 * GCM will be extended in the future with new message types, just
			 * ignore any message types you're not interested in, or that you
			 * don't recognize.
			 */
			// if (GoogleCloudMessaging.
			// MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
			// sendNotification("Send error: " + extras.toString());
			// } else if (GoogleCloudMessaging.
			// MESSAGE_TYPE_DELETED.equals(messageType)) {
			// sendNotification("Deleted messages on server: " +
			// extras.toString());
			// } else
			if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
				hvApn.getInstance().onMessage(getApplicationContext(), extras);
			}
		}
		GcmBroadcastReceiver.completeWakefulIntent(intent);
	}
}
