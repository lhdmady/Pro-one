package com.heimavista.apn;

import android.os.Environment;
import android.util.Log;

import com.heimavista.hvFrame.hvApp;
import com.heimavista.hvFrame.logger.Logger;
import com.heimavista.hvFrame.tools.PublicUtil;
import com.heimavista.hvFrame.tools.ecEncryptor;
import com.heimavista.hvFrame.tools.environment;

import java.io.File;
import java.util.Date;

/**
 * service數據存儲類，用於存儲deviceToken和每個app的代號，用於數據派發
 *
 * @author apple
 */
public class ServiceData {
    private String filePath;

    private static class Instance {
        private static final ServiceData instance = new ServiceData();
    }

    private final static String PATH_SERVICE = "/heimavista/service/";

    public static ServiceData getInstance() {
        return Instance.instance;
    }

    private ServiceData() {
        filePath = Environment.getExternalStorageDirectory().getAbsolutePath()
                + PATH_SERVICE;
        File file = new File(filePath);
        if (!file.exists())
            file.mkdirs();
    }

    public void initData() {
        setDeviceToken();
        setAppId();
    }

    /**
     * 設置deviceCode
     */
    private void setDeviceToken() {
        File file = new File(filePath + "device_token");
        if (file.exists())
            return;
        String code = environment.getUUID()
                + environment.formatDateTime(new Date(), "yyyyMMdd");
        code = PublicUtil.getMD5(code.getBytes());
        Log.d("apnService", code);
        code = ecEncryptor.encryptData(code, "hv");
        PublicUtil.writeToFile(filePath + "device_token", code, false);
    }

    public String getDeviceToken() {
        return PublicUtil.readFile(filePath + "device_token", true);
    }

    public String getAppId() {
        String packageName = hvApp.getInstance().getPackageName();
        File[] list = new File(filePath).listFiles();
        for (int i = 0; i < list.length; i++) {
            if (packageName
                    .equals(PublicUtil.readFile(list[i].getPath(), true)))
                return list[i].getName();
        }
        return "";
    }

    public String getPackageNameById(String id) {
        return PublicUtil.readFile(filePath + id, true);
    }

    public String getAppId(String packageName) {
        File[] list = new File(filePath).listFiles();
        for (int i = 0; i < list.length; i++) {
            if (packageName
                    .equals(PublicUtil.readFile(list[i].getPath(), true)))
                return list[i].getName();
        }
        return "";
    }

    /**
     * 設置app代號
     */
    public void setAppId() {
        // 当前版本的包名
        String packageName = hvApp.getInstance().getPackageName();
        File[] list = new File(filePath).listFiles();
        for (int i = 0; i < list.length; i++) {
            if (packageName
                    .equals(PublicUtil.readFile(list[i].getPath(), true)))
                return;
        }
        Logger.d(ServiceData.class, "apnService:" + packageName);
        PublicUtil.writeToFile(filePath + createAppId(), packageName, false);
    }

    private int createAppId() {
        File[] list = new File(filePath).listFiles();
        int appid = (int) (Math.random() * 10000);
        for (int i = 0; i < list.length; i++) {
            if (String.valueOf(appid).equals(list[i].getName()))
                return createAppId();
        }
        return appid;
    }

    public void deleteCurrentAppData() {
        String packageName = hvApp.getInstance().getPackageName();
        File[] list = new File(filePath).listFiles();
        for (int i = 0; i < list.length; i++) {
            if (packageName
                    .equals(PublicUtil.readFile(list[i].getPath(), true))) {
                list[i].delete();
                break;
            }
        }
        list = new File(filePath).listFiles();
        if (list.length == 1
                && list[0].getName().equalsIgnoreCase("device_token"))
            list[0].delete();
    }
}
