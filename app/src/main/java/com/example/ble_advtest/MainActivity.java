package com.example.ble_advtest;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;


//import android.bluetooth.le.ScanFilter;
//import android.bluetooth.le.ScanCallback;
//import android.bluetooth.le.ScanResult;
//import android.bluetooth.le.ScanSettings;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    //UI
    private TextView mText;
    private Button mStartButton;
    private Button mStopButton;

    //handler
    private Handler advertisingHandler = new Handler();
    private Handler scanHandler = new Handler();
    //private final int TIMEOUT = 300;//(ms)
    private final int Scanning_TIMEOUT = 500;//(ms)
    private final int Advtising_TIMEOUT = 10000;//(ms)
    private boolean mScanning = false;
    private boolean mAdvertising = false;

    //--Ble--
    //UUID
    private ParcelUuid pUUID;
    private ParcelUuid pUUID_16;
    private ParcelUuid pUUID_mask;

    private BluetoothAdapter mBluetoothAdapter;
    //advertiser
    private BluetoothLeAdvertiser mAdvertiser;
    private AdvertiseSettings mAdvertiseSettings;
    private AdvertiseData mAdvertiseData;
    //scaner
    List<ScanFilter> mScanfilters = null;
    List<BluetoothDevice> listBluetoothDevice;
    private ScanSettings mScanSettings;
    private BluetoothLeScannerCompat mBluetoothLeScanner;

    //PERMISSION
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


        checkPermission();
        mText.setText("Ready to Start");

        listBluetoothDevice = new ArrayList<>();


    }

    //---permission and setup---
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkPermission() {

        Log.d("TAG", "Request Location Permissions:");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            String[] PERMISSIONS = {
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
            };


            requestPermissions(PERMISSIONS, PERMISSION_REQUEST_CODE);
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

        //getBleAdvertiser();
        getBluetoothAdapterAndLeScannerAndAdvertiser();

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
            Log.d("TAG", String.valueOf(grantResults.length));

            /*for(String per : permissions){
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Log.d("TAG", per);
                }
            }*/
            //Do something based on grantResults

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("TAG", "ACCESS_COARSE_LOCATION permission granted");
            } else {
                Log.d("TAG", "ACCESS_COARSE_LOCATION permission denied");
            }

            if (grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Log.d("TAG", "ACCESS_FINE_LOCATION permission granted");
            } else {
                Log.d("TAG", "ACCESS_FINE_LOCATION permission denied");
            }

            if (grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                Log.d("TAG", "BLUETOOTH permission granted");
            } else {
                Log.d("TAG", "BLUETOOTH permission denied");
            }

            if (grantResults[3] == PackageManager.PERMISSION_GRANTED) {
                Log.d("TAG", "BLUETOOTH_ADMIN permission granted");
            } else {
                Log.d("TAG", "BLUETOOTH_ADMIN permission denied");
            }
        }

    }




    @RequiresApi(api = Build.VERSION_CODES.M)
    private void getBluetoothAdapterAndLeScannerAndAdvertiser() {
        // Get BluetoothAdapter and BluetoothLeScanner and BluetoothLeAdvertiser.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        //UUID
        pUUID = new ParcelUuid( UUID.fromString( getString( R.string.ble_uuid ) ) );
        pUUID_16 = ParcelUuid.fromString(getString( R.string.ble_uuid_16 ));
        pUUID_mask = new ParcelUuid( UUID.fromString( getString( R.string.ble_uuid_mask ) ) );


        //--Scanner--
        mScanfilters = new ArrayList<>();
        //mScanfilters.add(new ScanFilter.Builder().setServiceData(pUUID,null).build());
        mScanfilters.add(new ScanFilter.Builder().setServiceUuid(pUUID).build());
        mScanSettings = new ScanSettings.Builder()
                .setScanMode( ScanSettings.SCAN_MODE_LOW_LATENCY )
                .build();

        //nordic
        mBluetoothLeScanner = BluetoothLeScannerCompat.getScanner();

        mScanning = false;

        //--Advertiser--
        mAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();

        mAdvertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode( AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY )
                .setTxPowerLevel( AdvertiseSettings.ADVERTISE_TX_POWER_HIGH )
                .setConnectable( false )
                .build();

        mAdvertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName( false )
                .addServiceUuid(pUUID)
                .addServiceData( pUUID_16, "Data".getBytes( Charset.forName( "US-ASCII" ) ) )
                .build();

        //Log.e( "Data", "-----stop-----" );
        mAdvertising = false;
    }
    //---permission and setup---

    //---UI---
    public void onClick(View v) {
        if( v.getId() == R.id.stop_btn ) {
            advertisingHandler.removeCallbacks(stop_advertising);
            scanHandler.removeCallbacks(stop_scan);

            Log.e( "BLE", "-----stop-----" );
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
    //---UI---

    //---advertise and scan---
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void advertise(final boolean enable) {

        if (enable) {
            //stop handler
            advertisingHandler.postDelayed(stop_advertising, Advtising_TIMEOUT);

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
    private void scan(final boolean enable) {
        if (enable) {
            listBluetoothDevice.clear();
            //listViewLE.invalidateViews();

            scanHandler.postDelayed(stop_scan,Scanning_TIMEOUT);

            Log.e( "BLE", "scanning" );
            mText.setText("Scanning...");
            mBluetoothLeScanner.startScan(mScanfilters,mScanSettings,scanCallback);
            //mBluetoothLeScanner.startScan(scanCallback);
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

            Log.e( "BLE", "onResults " );

            addBluetoothDevice(result.getDevice());
            /*if(result.getDevice().getAddress() != null) {
                Log.e( "BLE", result.getDevice().getAddress() );
            }*/


            if(result.getScanRecord().getDeviceName() != null) {
                Log.e( "Name", result.getScanRecord().getDeviceName() );
            }

            if(result.getScanRecord().getServiceData() != null)
            {
                Map<ParcelUuid,byte[]> mMap = result.getScanRecord().getServiceData();
                int size = mMap.size();
                Log.e( "uuid", String.valueOf(size) );
                Set<ParcelUuid> uuidSet = mMap.keySet();
                for(ParcelUuid u :uuidSet){
                    Log.e( "uuid", u.toString() );
                    String sData = new String(mMap.get(u), StandardCharsets.US_ASCII);
                    Log.e( "data", sData );

                }
            }

            /*if(result.getScanRecord().getServiceUuids() != null)
            {
                Log.e( "uuid", result.getScanRecord().getServiceUuids().toString() );
            }

            String sData = bytesToHex(result.getScanRecord().getBytes());
            Log.e( "data", sData );*/

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
    //---advertise and scan---

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
    //runnable

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

}