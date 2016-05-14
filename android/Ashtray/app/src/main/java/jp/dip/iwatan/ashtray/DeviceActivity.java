package jp.dip.iwatan.ashtray;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;

import jp.dip.iwatan.ashtray.util.BleUtil;
import jp.dip.iwatan.ashtray.util.BleUuid;

public class DeviceActivity extends AppCompatActivity {
    private static final String TAG = "BLEDevice";

    public static final String EXTRA_BLUETOOTH_DEVICE = "BT_DEVICE";
    private BluetoothAdapter mBTAdapter;
    private BluetoothDevice mDevice;
    private BluetoothGatt mConnGatt;
    private int mStatus;

    private Button mWriteButton;
    private Button mReadButton;
    private TextView mAnalogText;

    public final static UUID UUID_ASHTRAY_AD = UUID.fromString(BleUuid.ASHTRAY_AD);

    private byte ledValue = 0;

    private final BluetoothGattCallback mGattcallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            Log.d(TAG, "onConnectionStateChange");
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "device connected");
                mStatus = newState;
                mConnGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "device disconnected");
                mStatus = newState;
                runOnUiThread(new Runnable() {
                    public void run() {
                        mWriteButton.setEnabled(false);
                        // ToDo ここに忘れた際の処理を書く
                    };
                });
            }
        };

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onServicesDiscovered");
            for (BluetoothGattService service : gatt.getServices()) {
                if ((service == null) || (service.getUuid() == null)) {
                    continue;
                }
                if (BleUuid.ASHTRAY.equalsIgnoreCase(service.getUuid().toString())) {
                    // AD値の読み込み設定
                    final BluetoothGattCharacteristic characteristic =
                            service.getCharacteristic(UUID.fromString(BleUuid.ASHTRAY_AD));
                    final int charaProp = characteristic.getProperties();

                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                        mConnGatt.readCharacteristic(characteristic);
                        if (UUID_ASHTRAY_AD.equals(characteristic.getUuid())) {
                            mConnGatt.setCharacteristicNotification(characteristic, true);
                            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                                    UUID.fromString(BleUuid.CLIENT_CHARACTERISTIC_CONFIG));
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            mConnGatt.writeDescriptor(descriptor);
                        }
                    }

                    // ボタンを押したらLEDに指示が飛ぶ
                    mWriteButton.setTag(service.getCharacteristic(UUID.fromString(BleUuid.ASHTRAY_LED)));
                    mReadButton.setTag(service.getCharacteristic(UUID.fromString(BleUuid.ASHTRAY_AD)));
                    runOnUiThread(new Runnable() {
                        public void run() {
                            mWriteButton.setEnabled(true);
                        };
                    });
                }
            }

            runOnUiThread(new Runnable() {
                public void run() {
                    setProgressBarIndeterminateVisibility(false);
                };
            });
        };

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicRead");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (BleUuid.ASHTRAY_AD.equalsIgnoreCase(characteristic.getUuid().toString())) {
                    final String value = characteristic.getStringValue(0);

                    runOnUiThread(new Runnable() {
                        public void run() {
                            mAnalogText.setText(value);
                            setProgressBarIndeterminateVisibility(false);
                        };
                    });
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicWrite");

            runOnUiThread(new Runnable() {
                public void run() {
                    setProgressBarIndeterminateVisibility(false);
                };
            });
        };
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_device);

        // state
        mStatus = BluetoothProfile.STATE_DISCONNECTED;

        mWriteButton = (Button) findViewById(R.id.write_alert_level_button);
        mWriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((v.getTag() != null) && (v.getTag() instanceof BluetoothGattCharacteristic)) {
                    BluetoothGattCharacteristic ch = (BluetoothGattCharacteristic) v.getTag();
                    if (ledValue == 0) {
                        ledValue = 1;
                    } else {
                        ledValue = 0;
                    }
                    ch.setValue(new byte[] { ledValue });
                    if (mConnGatt.writeCharacteristic(ch)) {
                        setProgressBarIndeterminateVisibility(true);
                    }
                }
            }
        });

        mReadButton = (Button) findViewById(R.id.btn_read);
        mReadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((v.getTag() != null) && (v.getTag() instanceof BluetoothGattCharacteristic)) {
                    BluetoothGattCharacteristic ch = (BluetoothGattCharacteristic) v.getTag();
                    mConnGatt.readCharacteristic(ch);
                }
            }
        });

        mAnalogText = (TextView) findViewById(R.id.text_ad);
    }

    @Override
    protected void onResume() {
        super.onResume();

        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mConnGatt != null) {
            if ((mStatus != BluetoothProfile.STATE_DISCONNECTING)
                    && (mStatus != BluetoothProfile.STATE_DISCONNECTED)) {
                mConnGatt.disconnect();
            }
            mConnGatt.close();
            mConnGatt = null;
        }
    }

    private void init() {
        // BLE check
        if (!BleUtil.isBLESupported(this)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT)
                    .show();
            finish();
            return;
        }

        // BT check
        BluetoothManager manager = BleUtil.getManager(this);
        if (manager != null) {
            mBTAdapter = manager.getAdapter();
        }
        if (mBTAdapter == null) {
            Toast.makeText(this, R.string.bt_unavailable, Toast.LENGTH_SHORT)
                    .show();
            finish();
            return;
        }

        // check BluetoothDevice
        if (mDevice == null) {
            mDevice = getBTDeviceExtra();
            if (mDevice == null) {
                finish();
                return;
            }
        }

        // connect to Gatt
        if ((mConnGatt == null)
                && (mStatus == BluetoothProfile.STATE_DISCONNECTED)) {
            // try to connect
            mConnGatt = mDevice.connectGatt(this, false, mGattcallback);
            mStatus = BluetoothProfile.STATE_CONNECTING;
        } else {
            if (mConnGatt != null) {
                // re-connect and re-discover Services
                mConnGatt.connect();
                mConnGatt.discoverServices();
            } else {
                Log.e(TAG, "state error");
                finish();
                return;
            }
        }
        setProgressBarIndeterminateVisibility(true);
    }

    private BluetoothDevice getBTDeviceExtra() {
        Intent intent = getIntent();
        if (intent == null) {
            return null;
        }

        Bundle extras = intent.getExtras();
        if (extras == null) {
            return null;
        }

        return extras.getParcelable(EXTRA_BLUETOOTH_DEVICE);
    }
}
