package li.bodhisattva.tangobotfinal;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.atap.tangoservice.TangoAreaDescriptionMetaData;
import com.google.atap.tangoservice.TangoPoseData;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoOutOfDateException;

public class MainActivity extends AppCompatActivity {
    private String TAG = "ATTN";
    private boolean usbPermission;
    private Tango mTango;
    private TangoConfig mConfig;
    private UsbManager manager;
    private List<UsbSerialDriver> availableDrivers;
    private UsbSerialDriver driver;
    private PendingIntent mPermissionIntent;
    private UsbDeviceConnection connection;
    private UsbSerialPort sPort;
    private TextView mTitleTextView;
    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            usbPermission = true;
                        }
                    } else {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.mTitleTextView = (TextView) findViewById(R.id.chome);
        startActivityForResult(
                Tango.getRequestPermissionIntent(Tango.PERMISSIONTYPE_ADF_LOAD_SAVE),
                Tango.TANGO_INTENT_ACTIVITYCODE);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        this.manager = (UsbManager) this.getSystemService(Context.USB_SERVICE);
        this.availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(this.manager);
        this.driver = this.availableDrivers.get(0);
        UsbDevice device = driver.getDevice();
        manager.requestPermission(device, mPermissionIntent);
        tryConnect();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Tango.TANGO_INTENT_ACTIVITYCODE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Log.v("ATTN", "starting tango stuff");
                mTango = new Tango(this);
                mConfig = mTango.getConfig(TangoConfig.CONFIG_TYPE_CURRENT);
                mConfig.putBoolean(TangoConfig.KEY_BOOLEAN_MOTIONTRACKING, true);
                mConfig.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);
                mConfig.putBoolean(TangoConfig.KEY_STRING_AREADESCRIPTION, true);
                ArrayList<String> fullUUIDList = mTango.listAreaDescriptions();
                if (fullUUIDList.size() > 0) {
                    mConfig.putString(TangoConfig.KEY_STRING_AREADESCRIPTION,
                            fullUUIDList.get(fullUUIDList.size() - 1));
                    Log.v("ATTN", "Loaded ADF");
                    this.mTitleTextView.setText(getName(fullUUIDList.get(fullUUIDList.size()-1)));
                } else {
                    Log.v("ATTN", "No ADFs found");
                }
            }
        }
    }

    private void tryConnect() {
        this.availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(this.manager);
        if (this.availableDrivers.isEmpty()) {
            return;
        }
        this.driver = this.availableDrivers.get(0);
        UsbDevice device = driver.getDevice();
        manager.requestPermission(device, mPermissionIntent);

        this.connection = this.manager.openDevice(this.driver.getDevice());
        if (this.connection == null) {
            return;
        }
        sPort = this.driver.getPorts().get(0);
        sendCommand('w');
        sendCommand(' ');
    }

    public void sendCommand(char c) {
        try {
            UsbSerialPort usbSerialPort = sPort;
            usbSerialPort.open(this.connection);
            usbSerialPort.setParameters(115200, 8, 1, 0);
            byte[] arrby = new byte[8];
            arrby[0] = (byte) c;
            usbSerialPort.write(arrby, 1000);
            usbSerialPort.close();
        } catch (IOException e) {
            Log.v("OOPS", "we done fucked up :/");
        }
    }

    public void onPoseAvailable(TangoPoseData pose) {
        if (pose.baseFrame == TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION
                && pose.targetFrame == TangoPoseData.COORDINATE_FRAME_DEVICE) {
            Log.v(TAG, "new ADF to device pose data");
        } else if (pose.baseFrame == TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION
                && pose.targetFrame == TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE) {
            Log.v(TAG, "new localization");
        }
    }

    public String getName(String uuid) {

        TangoAreaDescriptionMetaData metadata = new TangoAreaDescriptionMetaData();
        metadata = mTango.loadAreaDescriptionMetaData(uuid);
        byte[] nameBytes = metadata.get(TangoAreaDescriptionMetaData.KEY_NAME);
        if (nameBytes != null) {
            String name = new String(nameBytes);
            return name;
        } // Do something if null
        return "haha fuck u";
    }

}
