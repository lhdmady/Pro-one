/*
 * Copyright (C) 2010 Moduad Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.heimavista.apn;

import android.content.Context;
import android.content.Intent;

/**
 * rabbitMQ推播服務管理類
 * @author apple
 *
 */
public final class ServiceManager {

	private Context context;
	public ServiceManager(Context context) {
		this.context = context;
	}
	public void startService() {
		Thread serviceThread = new Thread(new Runnable() {
			@Override
			public void run() {
				Intent intent = RmIntentService.getIntent();
				if(context!=null)
					context.startService(intent);
			}
		});
		serviceThread.start();
	}

	public void stopService() {
		Intent intent = RmIntentService.getIntent();
		if(context!=null)
			context.stopService(intent);
	}
}
