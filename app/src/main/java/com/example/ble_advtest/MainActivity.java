package com.example.ble_advtest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;

import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static com.example.ble_advtest.BluetoothLeService.byteArrayToHexStr;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    //UI
    private TextView mText;
    private Button mStartButton;
    private Button mStopButton;

    //handler
    private Handler advertisingHandler = new Handler();
    private Handler scanHandler = new Handler();
    private final int TIMEOUT = 1000;//(ms)
    private boolean mScanning = false;
    private boolean mAdvertising = false;

    //advertiser
    private BluetoothLeAdvertiser mAdvertiser;
    private AdvertiseSettings mAdvertiseSettings;
    private AdvertiseData mAdvertiseData;

    //scaner
    List<BluetoothDevice> listBluetoothDevice;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;

    private static int PERMISSION_REQUEST_CODE = 1;


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mText = (TextView) findViewById( R.id.text );
        mStopButton = (Button) findViewById( R.id.stop_btn );
        mStartButton = (Button) findViewById( R.id.start_btn );

        mStopButton.setOnClickListener( this );
        mStartButton.setOnClickListener( this );

        mBluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        checkPermission();
        mText.setText("Ready to Start");

        listBluetoothDevice = new ArrayList<>();


    }

    private void checkPermission() {

        Log.d("TAG", "Request Location Permissions:");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
        }

        // Check if BLE is supported on the device.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this,
                    "BLUETOOTH_LE not supported in this device!",
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        if( !BluetoothAdapter.getDefaultAdapter().isMultipleAdvertisementSupported() ) {

            Toast.makeText( this, "Multiple advertisement not supported", Toast.LENGTH_SHORT ).show();
            mStartButton.setEnabled( false );
            mStopButton.setEnabled( false );
        }
        else Log.d("TAG", "Multiple advertisement supported");


        getBleAdvertiser();
        getBluetoothAdapterAndLeScanner();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this,
                    "bluetoothManager.getAdapter()==null",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            //Do something based on grantResults
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("TAG", "coarse location permission granted");
            } else {
                Log.d("TAG", "coarse location permission denied");
            }
        }

    }

    public void onClick(View v) {
        if( v.getId() == R.id.stop_btn ) {
            advertisingHandler.removeCallbacks(stop_advertising);
            scanHandler.removeCallbacks(stop_scan);

            mText.setText("Ready to Start");
            advertise(false);
            scan(false);
            mStartButton.setEnabled(true);
            mStopButton.setEnabled(false);

        } else if( v.getId() == R.id.start_btn ) {
            advertise(true);
            mStartButton.setEnabled(false);
            mStopButton.setEnabled(true);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void getBleAdvertiser() {

        mAdvertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();

        mAdvertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode( AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY )
                .setTxPowerLevel( AdvertiseSettings.ADVERTISE_TX_POWER_HIGH )
                .setConnectable( true )
                .build();

        //UUID
        ParcelUuid pUUID = new ParcelUuid( UUID.fromString( getString( R.string.ble_uuid ) ) );
        mAdvertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName( true )
                .addServiceData( pUUID, "Data".getBytes( Charset.forName( "UTF-8" ) ) )
                .build();

        mAdvertising = false;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void advertise(final boolean enable) {

        if (enable) {
            //stop handler
            advertisingHandler.postDelayed(stop_advertising, TIMEOUT);

            Log.e( "BLE", "advertising" );
            mText.setText("Advertising...");
            mAdvertiser.startAdvertising(mAdvertiseSettings, mAdvertiseData, advertisingCallback);
            mAdvertising = true;

        }else {
            mAdvertiser.stopAdvertising(advertisingCallback);
            mAdvertising = false;
        }
    }

    private AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);

            Log.e( "BLE", "Advertising onStartSuccess ");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.e( "BLE", "Advertising onStartFailure: " + errorCode );
            super.onStartFailure(errorCode);
        }
    };


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void getBluetoothAdapterAndLeScanner() {
        // Get BluetoothAdapter and BluetoothLeScanner.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        mScanning = false;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void scan(final boolean enable) {
        if (enable) {
            listBluetoothDevice.clear();
            //listViewLE.invalidateViews();

            scanHandler.postDelayed(stop_scan,TIMEOUT);

            Log.e( "BLE", "scanning" );
            mText.setText("Scanning...");
            mBluetoothLeScanner.startScan(scanCallback);
            mScanning = true;

        } else {
            mBluetoothLeScanner.stopScan(scanCallback);
            mScanning = false;

        }
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            addBluetoothDevice(result.getDevice());

            if(result.getDevice().getAddress() != null) {
                Log.e( "BLE", result.getDevice().getAddress() );
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);

            Log.e( "BLE", "onBatchScanResults " );
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e( "BLE", "Discovery onScanFailed: " + errorCode );
            super.onScanFailed(errorCode);
        }

        private void addBluetoothDevice(BluetoothDevice device) {
            if (!listBluetoothDevice.contains(device)) {
                listBluetoothDevice.add(device);
                //listViewLE.invalidateViews();
            }
        }
    };

    //runnable
    private Runnable stop_scan = new Runnable() {
        @Override
        public void run() {
            mBluetoothLeScanner.stopScan(scanCallback);
            advertise(true);
        }
    };

    private Runnable stop_advertising = new Runnable() {
        @Override
        public void run() {
            mAdvertiser.stopAdvertising(advertisingCallback);
            scan(true);
        }
    };

}