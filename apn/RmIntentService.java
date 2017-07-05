package com.heimavista.apn;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.heimavista.hvFrame.hvApp;
import com.heimavista.hvFrame.logger.Logger;
import com.heimavista.hvFrame.tools.HvAppConfig;
import com.heimavista.hvFrame.tools.workerThread;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * rabbitMQ推播服務類，多個app共用此service
 * 
 * @author apple
 * 
 */
public class RmIntentService extends Service {
	public static final String RABBITMQ_RECV = "com.heimavista.apn.rabbitmq.intent.RECEIVE";
	public static final String SERVICE_NAME = "com.heimavista.magicsquarebasic.service.hvApnService";
	private final static String QUEUE_NAME = ServiceData.getInstance()
			.getDeviceToken();

	private ConnectionFactory mFactory;
	// private Connection connection;
	// private Channel channel;
	private workerThread mWorkerThread;
	private QueueingConsumer mConsumer;

	private ConnectivityReceiver mConnectivityReceiver;

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		Log.d("apnService", "onCreate");
		mFactory = new ConnectionFactory();
		try {
			mFactory.setHost(HvAppConfig.getInstance().getStringValue(
					"Apn_RabbitMQ", "host"));
			mFactory.setPort(Integer.valueOf(HvAppConfig.getInstance()
					.getStringValue("Apn_RabbitMQ", "port")));
			mFactory.setUsername(HvAppConfig.getInstance().getStringValue(
					"Apn_RabbitMQ", "username"));
			mFactory.setPassword(HvAppConfig.getInstance().getStringValue(
					"Apn_RabbitMQ", "password"));
			mFactory.setVirtualHost(HvAppConfig.getInstance().getStringValue(
					"Apn_RabbitMQ", "vhost"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		mWorkerThread = workerThread.workerWithName("hv_apn_service");
		mWorkerThread.setJobEntry(new workerThread.WorkerThread() {

			@Override
			public void work(Map<String, Object> param) {
				// TODO Auto-generated method stub
				connect(param);
			}
		});
		Map<String, Object> param = new HashMap<String, Object>();
		mWorkerThread.setDefaultParam(param);
		mWorkerThread.setNeedNetwork(true); // Need network to perform the
											// method
		mWorkerThread.setSdCard(true); // need sdcard to be available
		mWorkerThread.setTimeInterval(60); // 60s
		mWorkerThread.startThread();

		mConnectivityReceiver = new ConnectivityReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(android.net.ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(mConnectivityReceiver, filter);
		mConnectivityReceiver.setApnService(this);
	}

	private void onReceived(String message) {
		Logger.v(getClass(), message);
		try {
			JSONObject json = new JSONObject(message);
			String category = json.getString("appid");
			category = ServiceData.getInstance().getPackageNameById(category);
			Intent intent = new Intent(RABBITMQ_RECV);
			intent.addCategory(category);
			Bundle param = new Bundle();
			json = json.getJSONObject("content");
			Iterator<String> ite = json.keys();
			while (ite.hasNext()) {
				String key = ite.next();
				param.putString(key, json.getString(key));
			}
			intent.putExtras(param);
			sendBroadcast(intent);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Log.d("apnService", "onStart()...");

	}

	@Override
	public void onRebind(Intent intent) {
		// TODO Auto-generated method stub
		Log.d("apnService", "onRebind()...");
		super.onRebind(intent);
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		Log.d("apnService", "onBind()...");
		return null;
	}

	@Override
	public void onDestroy() {
		Log.d("apnService", "onDestroy()...");
		unregisterReceiver(mConnectivityReceiver);
		mWorkerThread.shutdown();
		disconnect();
		ServiceManager serviceManager = new ServiceManager(RmIntentService.this);
		serviceManager.stopService();
		serviceManager.startService();
	}

	public static Intent getIntent() {
		return new Intent(hvApp.getInstance(), RmIntentService.class);
	}

	/**
	 * 連接
	 */
	public void connect() {
		Logger.d(getClass(), "connect");
		mWorkerThread.notifyNewJob();
	}

	public void connect(Map<String, Object> param) {
		Logger.d(getClass(), "connect:" + param);
		Connection connection = null;
		Channel channel = null;
		try {
			connection = mFactory.newConnection();
			channel = connection.createChannel();
			channel.queueDeclare(QUEUE_NAME, false, false, true, null);
			Logger.d(getClass(), " [*] Waiting for messages. heartbeat is "
					+ connection.getHeartbeat());
			mConsumer = new QueueingConsumer(channel);
			channel.basicConsume(QUEUE_NAME, true, QUEUE_NAME, mConsumer);
			while (true) {
				QueueingConsumer.Delivery delivery;
				try {
					delivery = mConsumer.nextDelivery();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					Logger.e(getClass(), "error nextDelivery");
					e.printStackTrace();
					break;
				}
				String message = new String(delivery.getBody());
				Logger.d(getClass(), " [x] Received '" + message + "'");
				onReceived(message);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			// if (channel != null && channel.isOpen()){
			// Logger.e(getClass(), "channel");
			// channel.close();
			// }
			if (connection != null && connection.isOpen()) {
				Logger.e(getClass(), "connect");
				connection.close(10);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Logger.e(getClass(), "end");
	}

	/**
	 * 斷開連接
	 */
	public void disconnect() {
		try {
			if (mConsumer != null)
				mConsumer.handleCancel(QUEUE_NAME);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
