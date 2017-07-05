package com.heimavista.apn;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.heimavista.hvFrame.logger.Logger;


public class GcmBroadcastReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Explicitly specify that GcmIntentService will handle the intent.
    	Logger.d(getClass(), "onReceive");
        ComponentName comp = new ComponentName(context.getPackageName(),
                GcmIntentService.class.getName());
        // Start the service, keeping the device awake while it is launching.
        startWakefulService(context, (intent.setComponent(comp)));
        setResultCode(Activity.RESULT_OK);
    }
}

//public class GcmBroadcastReceiver extends BroadcastReceiver {
//
//	public final void onReceive(Context context, Intent intent) {
//		Logger.d(getClass(), "onReceive: " + intent.getAction());
//		String className = GcmIntentService.class.getName();
//		Logger.d(getClass(), "GCM IntentService class: " + className);
//		runIntentInService(context, intent, className);
//		setResultCode(Activity.RESULT_OK);
//	}
//
//    void runIntentInService(Context context, Intent intent,
//			String className) {
//		intent.setClassName(context, className);
//		context.startService(intent);
//	}
//
//}