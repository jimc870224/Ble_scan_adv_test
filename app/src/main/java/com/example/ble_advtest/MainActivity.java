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

    //advertise();
    private TextView mText;
    private Button mAdvertiseButton;
    private Button mDiscoverButton;

    //discover();
    List<BluetoothDevice> listBluetoothDevice;

    private static final long SCAN_PERIOD = 10000; // Stops scanning after 10 seconds.
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private Handler mHandler = new Handler();

    private static int PERMISSION_REQUEST_CODE = 1;
    private static final int REQUEST_FINE_LOCATION_PERMISSION = 102;
    private static final int REQUEST_ENABLE_BT = 2;



    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mText = (TextView) findViewById( R.id.text );
        mDiscoverButton = (Button) findViewById( R.id.discover_btn );
        mAdvertiseButton = (Button) findViewById( R.id.advertise_btn );

        mDiscoverButton.setOnClickListener( this );
        mAdvertiseButton.setOnClickListener( this );

        mBluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        checkPermission();

        listBluetoothDevice = new ArrayList<>();

        if( !BluetoothAdapter.getDefaultAdapter().isMultipleAdvertisementSupported() ) {

            Toast.makeText( this, "Multiple advertisement not supported", Toast.LENGTH_SHORT ).show();
            mAdvertiseButton.setEnabled( false );
            mDiscoverButton.setEnabled( false );
        }


        /*if(mBluetoothAdapter.isMultipleAdvertisementSupported()){
            Log.e( "onCreate", "isMultipleAdvertisementSupported() ");
        }*/



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
        if( v.getId() == R.id.discover_btn ) {
            discover();
        } else if( v.getId() == R.id.advertise_btn ) {
            advertise();
        }
    }

    public void discover() {

        scanLeDevice(true);

        /*
        List<ScanFilter> filters = null;

        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid( new ParcelUuid(UUID.fromString( getString(R.string.ble_uuid ) ) ) )
                .build();
        filters.add( filter );

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode( ScanSettings.SCAN_MODE_LOW_LATENCY )
                .build();

        mBluetoothLeScanner.startScan(filters, settings, mScanCallback);


        */

    }



    public void advertise() {


        BluetoothLeAdvertiser advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode( AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY )
                .setTxPowerLevel( AdvertiseSettings.ADVERTISE_TX_POWER_HIGH )
                .setConnectable( false )
                .build();

        //UUID
        ParcelUuid pUuid = new ParcelUuid( UUID.fromString( getString( R.string.ble_uuid ) ) );
        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName( true )
                .addServiceUuid( pUuid )
                .addServiceData( pUuid, "Data".getBytes( Charset.forName( "UTF-8" ) ) )
                .build();


        //Call back
        AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
            }

            @Override
            public void onStartFailure(int errorCode) {
                Log.e( "BLE", "Advertising onStartFailure: " + errorCode );
                super.onStartFailure(errorCode);
            }
        };

        advertiser.startAdvertising( settings, data, advertisingCallback );
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void getBluetoothAdapterAndLeScanner() {
        // Get BluetoothAdapter and BluetoothLeScanner.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        //mScanning = false;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            listBluetoothDevice.clear();
            //listViewLE.invalidateViews();

            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void run() {
                    mBluetoothLeScanner.stopScan(scanCallback);
                    //listViewLE.invalidateViews();

                    /*Toast.makeText(ScanActivity.this,
                            "Scan timeout",
                            Toast.LENGTH_LONG).show();*/

                    //mScanning = false;
                    mDiscoverButton.setEnabled(true);
                }
            }, SCAN_PERIOD);

            mBluetoothLeScanner.startScan(scanCallback);
            //mScanning = true;
            mDiscoverButton.setEnabled(false);
        } else {
            mBluetoothLeScanner.stopScan(scanCallback);
            //mScanning = false;
            mDiscoverButton.setEnabled(true);
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

}