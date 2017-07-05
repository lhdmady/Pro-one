package com.heimavista.apn;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.TextUtils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.heimavista.hvFrame.hvApp;
import com.heimavista.hvFrame.tools.HvAppConfig;

import java.io.IOException;

public class GcmHelpler {

	private GoogleCloudMessaging gcm;
	public static final String PROPERTY_REG_ID = "gcm_registration_id";
	private static final String PROPERTY_APP_VERSION = "gcm_appVersion";

	public GcmHelpler() {

	}

	public String getGcmSenderId() {
		return HvAppConfig.getInstance().getStringValue("C2DM", "senderId");
	}

	public SharedPreferences getGcmPreferences() {
		return hvApp.getInstance().getSharedPreferences("hvGcm",
				Application.MODE_PRIVATE);
	}

	public boolean registerGCM(Activity context) {
		try {
			if (checkPlayServices(context)) {
				gcm = GoogleCloudMessaging.getInstance(context);
				String regid = getRegistrationId(context);
				if (TextUtils.isEmpty(regid)) {
					registerInBackground(context);
				} else {
					hvApn.getInstance().onRegistered(context, regid);
				}
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private void registerInBackground(final Activity context) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					if (gcm == null) {
						gcm = GoogleCloudMessaging.getInstance(context);
					}
					String regid = gcm.register(getGcmSenderId());
					hvApp.getInstance().setApnType("android");
					hvApn.getInstance().onRegistered(context, regid);
					storeRegistrationId(context, regid);
				} catch (IOException ex) {
//					ex.printStackTrace();
					hvApn.getInstance().rabbitMQ();
				}
			}
		}).start();
	}

	// private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	private boolean checkPlayServices(Activity context) {
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(context);
		if (resultCode != ConnectionResult.SUCCESS) {
			// if
			// (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
			// GooglePlayServicesUtil.getErrorDialog(resultCode, context,
			// PLAY_SERVICES_RESOLUTION_REQUEST).show();
			// } else {
			// Log.i(TAG, "This device is not supported.");
			// finish();
			// }
			return false;
		}
		return true;
	}

	public String getRegistrationId(Context context) {
		String ret;
		final SharedPreferences prefs = getGcmPreferences();
		ret = prefs.getString(PROPERTY_REG_ID, "");
		if (!TextUtils.isEmpty(ret)) {

			// Check if app was updated; if so, it must clear the registration
			// ID
			// since the existing regID is not guaranteed to work with the new
			// app version.
			int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION,
					Integer.MIN_VALUE);
			int currentVersion = getAppVersion(context);
			if (registeredVersion != currentVersion) {
				ret = "";
			}
		}
		return ret;
	}

	private void storeRegistrationId(Context context, String regId) {
		final SharedPreferences prefs = getGcmPreferences();
		int appVersion = getAppVersion(context);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PROPERTY_REG_ID, regId);
		editor.putInt(PROPERTY_APP_VERSION, appVersion);
		editor.commit();
	}

	private static int getAppVersion(Context context) {
		try {
			PackageInfo packageInfo = context.getPackageManager()
					.getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}
}
