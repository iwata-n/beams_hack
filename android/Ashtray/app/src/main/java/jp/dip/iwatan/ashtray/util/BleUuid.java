package jp.dip.iwatan.ashtray.util;

/**
 * Created by @iwata_n on 2016/05/15.
 */
public class BleUuid {
    // 180A Device Information
    public static final String SERVICE_DEVICE_INFORMATION = "0000180a-0000-1000-8000-00805f9b34fb";
    public static final String CHAR_MANUFACTURER_NAME_STRING = "00002a29-0000-1000-8000-00805f9b34fb";
    public static final String CHAR_MODEL_NUMBER_STRING = "00002a24-0000-1000-8000-00805f9b34fb";
    public static final String CHAR_SERIAL_NUMBEAR_STRING = "00002a25-0000-1000-8000-00805f9b34fb";

    // 1802 Immediate Alert
    public static final String SERVICE_IMMEDIATE_ALERT = "00001802-0000-1000-8000-00805f9b34fb";
    public static final String CHAR_ALERT_LEVEL = "00002a06-0000-1000-8000-00805f9b34fb";
    // StickNFindではCHAR_ALERT_LEVELに0x01をWriteすると光り、0x02では音が鳴り、0x03では光って鳴る。

    // 180F Battery Service
    public static final String SERVICE_BATTERY_SERVICE = "0000180F-0000-1000-8000-00805f9b34fb";
    public static final String CHAR_BATTERY_LEVEL = "00002a19-0000-1000-8000-00805f9b34fb";


    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    public static String ASHTRAY = "19b10000-e8f2-537e-4f6c-d104768a1214";
    public static String ASHTRAY_LED = "19b10001-e8f2-537e-4f6c-d104768a1214";
    public static String ASHTRAY_AD = "00002a19-0000-1000-8000-00805f9b34fb";
}
