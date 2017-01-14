package jp.java2.bleperiferal;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import android.bluetooth.BluetoothManager;

import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private final String SERVICE_UUID    = "00000001-0000-1000-8000-2f97f3b2dcd5";
    private final String CHAR_READ_UUID  = "00000010-0000-1000-8000-2f97f3b2dcd5";
    private final String CHAR_WRITE_UUID = "00000011-0000-1000-8000-2f97f3b2dcd5";

    private final String TAG = "BLE:Peri";
    private boolean mIsAdvertising = false;


    private BluetoothManager mBTManager;
    private BluetoothAdapter mBTAdapter;
    private BluetoothGattServer mGattServer;
    private BluetoothLeAdvertiser mBTAdvertiser;



    /* Button */
    private Button adButton;
    private Button stopButton;



    // Callback for GettServer
    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        public void onCharacteristicReadRequest(android.bluetooth.BluetoothDevice device, int requestId,
                                                int offset, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "caracteristic read");
            Log.d(TAG, "offset = " + offset);
            String response  = "Suwakun";
            byte value[] = response.getBytes();
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);

        }

        // invoked when central sent write request
        public void onCharacteristicWriteRequest(android.bluetooth.BluetoothDevice device, int requestId,
                                                 BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded,
                                                 int offset, byte[] value) {
            Log.d(TAG, "caracteristic write");
        }
    };

    // Callback for Advertise
    private AdvertiseCallback mAdvCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d(TAG, "Advertising successfully started");
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.d(TAG, "Advertising failed");
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate");
        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    private void init() {
        boolean bleEnable = this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
        if (!bleEnable) { // check if the device support BLE
            Log.d(TAG, "ble disable");
            Toast.makeText(this, "BLE is not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        } else {
            Log.d(TAG, "ble enable");
        }


        // Get BluetoothManager
        mBTManager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);

        if (mBTManager == null) {
            Log.d(TAG, "cannot get BluetoothManager");
            Toast.makeText(this, "cannot get BluetoothManager", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "manager enable");

        mBTAdapter = mBTManager.getAdapter();

        if (mBTAdapter == null) {
            Log.d(TAG, "cannot get BluetoothAdapter");
            Toast.makeText(this, "cannot get BluetoothAdapter", Toast.LENGTH_SHORT).show();
        }

        Log.d(TAG, "adapter enable");

        adButton = (Button)findViewById(R.id.adButton);
        adButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAdvertiseing();
            }
        });

        stopButton = (Button)findViewById(R.id.stopButton);
        stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopAdvertising();
            }
        });

    }

    // Service
    private void setService() {

        mGattServer = mBTManager.openGattServer(this, mGattServerCallback);

        // create Service UUID
        BluetoothGattService service = new BluetoothGattService(
                UUID.fromString(SERVICE_UUID),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        // create Characteristic UUID for reading
        BluetoothGattCharacteristic charRead = new BluetoothGattCharacteristic(
                UUID.fromString(CHAR_READ_UUID),
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
        );

        BluetoothGattCharacteristic charWrite = new BluetoothGattCharacteristic(
                UUID.fromString(CHAR_WRITE_UUID),
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE
        );

        service.addCharacteristic(charRead);
        service.addCharacteristic(charWrite);
        mGattServer.addService(service);
    }

    // AdvertiseSetting
    private AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder  settingBuilder = new AdvertiseSettings.Builder();
        settingBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
        settingBuilder.setTimeout(0);
        return settingBuilder.build();
    }

    // AdvertiseData
    private AdvertiseData buildAdvertiseData() {
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.addServiceUuid(ParcelUuid.fromString(SERVICE_UUID));
        dataBuilder.setIncludeDeviceName(true);
        return dataBuilder.build();
    }

    private void startAdvertiseing() {
        Log.d(TAG, "start advertise");
        setService();
        AdvertiseSettings settings = buildAdvertiseSettings();
        AdvertiseData data = buildAdvertiseData();

        mBTAdvertiser = mBTAdapter.getBluetoothLeAdvertiser();

        mBTAdvertiser.startAdvertising(settings, data, mAdvCallback);
        Log.d(TAG, "advertise start");

    }

    private void stopAdvertising() {
        Log.d(TAG, "stop advertise");
        if (mGattServer != null) {
            mGattServer.clearServices();
            mGattServer.close();
            mGattServer = null;
        }

        if (mBTAdvertiser != null) {
            mBTAdvertiser.stopAdvertising(mAdvCallback);
            mBTAdvertiser = null;
        }
    }






}
