package com.heimavista.apn;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.text.TextUtils;

import com.heimavista.hvFrame.hvApp;
import com.heimavista.hvFrame.logger.Logger;
import com.heimavista.hvFrame.net.ApiHttpUtils;
import com.heimavista.hvFrame.net.RequestResponse;
import com.heimavista.hvFrame.tools.HvAppConfig;
import com.heimavista.hvFrame.tools.ParamJsonData;
import com.heimavista.hvFrame.tools.PublicUtil;
import com.heimavista.hvFrame.tools.ecEncryptor;
import com.heimavista.hvFrame.tools.environment;
import com.heimavista.hvFrame.vm.PushHandler;
import com.lidroid.xutils.http.RequestParams;
import com.lidroid.xutils.http.client.HttpRequest.HttpMethod;

import java.util.Locale;

public class hvApn {

    private final static String TAG_SALT = "salt";

    private final static String TAG_SALT_TOKEN = "saltToken";

    private final static String TAG_SALT_FORCE_FLAG = "saltForceFlag";

    private static class Instance {
        private static final hvApn instance = new hvApn();
    }

    private hvApn() {

    }

    public static hvApn getInstance() {
        return Instance.instance;
    }

    public String getSalt() {
        return hvApp.getInstance().getSharePref().getString(TAG_SALT, "");
    }

    public String getSaltToken() {
        return hvApp.getInstance().getSharePref().getString(TAG_SALT_TOKEN, "");
    }

    public boolean getSaltForceFlag() {
        return hvApp.getInstance().getSharePref().getBoolean(TAG_SALT_FORCE_FLAG, false);
    }

    public void setSalt(String salt, String token, boolean saltForceFlag) {
        SharedPreferences.Editor editor = hvApp.getInstance().getSharePref().edit();
        editor.putString(TAG_SALT, salt);
        editor.putString(TAG_SALT_TOKEN, token);
        editor.putBoolean(TAG_SALT_FORCE_FLAG, saltForceFlag);
        editor.commit();
    }

    public String registerDevice(String apnToken) {
        String salt = getSalt();
        if (getSaltForceFlag()) {
            return salt;
        } else if (!TextUtils.isEmpty(salt)) {
            if (TextUtils.isEmpty(apnToken)) {
                return salt;
            } else if (apnToken.equals(getSaltToken())) {
                return salt;
            }
        }
        String token = getToken();
        if (!TextUtils.isEmpty(token)) {
            salt = doRegister(token, apnToken);
            if (salt != null) {
                setSalt(salt, apnToken, false);
            }
        }
        return salt;
    }

    private String getToken() {
        String ret = null;
        ApiHttpUtils http = new ApiHttpUtils();
        RequestParams param = new RequestParams();
        param.addBodyParameter("index", "token");
        param.addBodyParameter("op", "token");
        RequestResponse response = http.sendSyncForResponse(HttpMethod.POST,
                hvApp.getInstance().getApnUrl(), param);
        if (!response.isError()) {
            ParamJsonData data = new ParamJsonData(response.getResponseString());
            ret = data.getStringValueByKey("token", null);
        }
        return ret;
    }

    private String doRegister(String token, String apnToken) {
        String ret = null;
        String chk = PublicUtil.getMD5(
                (environment.getUUID() + "HV,INC." + token).getBytes())
                .substring(0, 6);
        ApiHttpUtils http = new ApiHttpUtils();
        RequestParams param = new RequestParams();
        param.addBodyParameter("Fun", "index");
        param.addBodyParameter("op", "register");
        param.addBodyParameter("devCode", environment.getUUID());
        param.addBodyParameter("devType", hvApp.getInstance().getApnType());
        param.addBodyParameter("token", token);
        param.addBodyParameter("chk", chk);
        param.addBodyParameter("apnToken", apnToken);
        param.addBodyParameter("devBrand", android.os.Build.BRAND);
        param.addBodyParameter("devModel", android.os.Build.MODEL);
        param.addBodyParameter("devOs", android.os.Build.VERSION.RELEASE + " "
                + android.os.Build.DISPLAY);
        param.addBodyParameter("AreaCode", getLanguage());
        param.addBodyParameter("SdkVer", getVersion());
        RequestResponse response = http.sendSyncForResponse(HttpMethod.POST,
                hvApp.getInstance().getApnUrl(), param);
        if (!response.isError()) {
            ParamJsonData data = new ParamJsonData(response.getResponseString());
            ret = data.getStringValueByKey("salt", null);
        }
        return ret;
    }

    public void doC2DMRegister() {
        if ("true".equals(HvAppConfig.getInstance().getStringValue("C2DM",
                "receiveFlag"))) {
            boolean flag;
            GcmHelpler gcmHelper = new GcmHelpler();
            if ("".equals(gcmHelper.getRegistrationId(hvApp.getInstance()))) {
                flag = gcmHelper.registerGCM(hvApp.getInstance()
                        .getCurrentActivity());
            } else {
                flag = true;
                onRegistered(hvApp.getInstance(),
                        gcmHelper.getRegistrationId(hvApp.getInstance()));
            }
            if (!flag) {
                rabbitMQ();
            }
        }
    }

    /**
     * RabbitMQ推播
     */
    public void rabbitMQ() {
        ServiceData.getInstance().initData();
        hvApp.getInstance().setApnType("android_rabbitmq");
        String token = ServiceData.getInstance().getDeviceToken() + ";"
                + ServiceData.getInstance().getAppId();
        Logger.d(getClass(), "before enc:" + token);
        token = ecEncryptor.encryptData(token, "hv");
        onRegistered(hvApp.getInstance(), token);
        ServiceManager manager = new ServiceManager(hvApp.getInstance());
        manager.startService();
    }

    /**
     * 消息到來的時候會調用此方法
     */
    public void onMessage(Context context, Bundle extras) {
        Logger.i(getClass(), "ApnDelegate message extras:" + extras);
//        Log.e("Kite", "ApnDelegate message extras:" + extras);
        if (extras != null) {
            responseGCMMsg(context, extras);
        }
    }

    private void responseGCMMsg(Context context, Bundle extras) {
        if (extras.containsKey("T")) {
            String type = extras.getString("T");
            PushHandler handler = hvApp.getInstance()
                    .getPushHandlerByType(type);
            if (handler == null) {
                hvApp.getInstance().registerHandlers();
                handler = hvApp.getInstance().getPushHandlerByType(type);
            }
            Logger.d(getClass(), "handler:" + handler);
            if (handler != null) {
                handler.handle(context, extras);
            }
        }
    }

    /**
     * apn註冊失敗
     */
    public void onError(final Context context, String errorId) {
        // TODO Auto-generated method stub
        Logger.e(getClass(), "ApnDelegate error");
        Logger.e(getClass(), errorId);
    }

    /**
     * 注册成功
     */
    public void onRegistered(final Context context, final String registrationId) {
        Logger.i(getClass(), "ApnDelegate Register");
        Logger.i(getClass(), "RegistrationId:" + registrationId);
        new Thread(new Runnable() {
            @Override
            public void run() {
                registerDevice(registrationId);
            }
        }).start();
    }

    /**
     * apn 註銷
     */
    public void onUnregistered(Context context, String arg1) {
        // TODO Auto-generated method stub
        Logger.i(getClass(), "ApnDelegate UnRegister");

    }

    public String getLanguage() {
        Locale locale = Locale.getDefault();
        String language = locale.getLanguage();
        String area = locale.getCountry();
        if (language.equalsIgnoreCase("zh") && !TextUtils.isEmpty(area)) {
            language += "_" + area;
        }
        return language;
    }

    public String getLanguageWithLocal() {
        String ret;
        if ("CN".equals(Locale.getDefault().getCountry()))
            ret = "zh_CN";
        else if ("TW".equals(Locale.getDefault().getCountry()))
            ret = "zh_TW";
        else if ("JP".equals(Locale.getDefault().getCountry()))
            ret = "ja";
        else
            ret = "en";
        return ret;
    }

    /**
     * 获取应用的版本号
     *
     * @return
     */
    public String getVersion() {
        String version = null;
        PackageInfo packageInfo = environment.getPackageInfo();
        if (packageInfo != null) {
            version = packageInfo.versionName;
        }
        return version;
    }
}
