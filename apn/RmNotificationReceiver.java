package com.heimavista.apn;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.heimavista.hvFrame.hvApp;
import com.heimavista.hvFrame.logger.Logger;

/**
 * 收到推播信息后的廣播接收類
 * @author apple
 *
 */
public class RmNotificationReceiver extends BroadcastReceiver{
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		Logger.d(getClass(), "broadcast receive:"+hvApp.getInstance().getPackageName());
		Bundle bundle = intent.getExtras();
		hvApn.getInstance().onMessage(context, bundle);
	}

}
