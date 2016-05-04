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
import android.speech.tts.TextToSpeech;

import com.google.atap.tangoservice.TangoAreaDescriptionMetaData;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoOutOfDateException;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.Tango.OnTangoUpdateListener;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoInvalidException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import org.w3c.dom.Text;

import java.text.DecimalFormat;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {
    private ServerSocket serverSocket;
    Thread serverThread = null;
    private String ip;
    public static final int SERVERPORT = 6000;

    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";
    public double π = Math.PI;
    TextToSpeech t1;
    private String TAG = "ATTN";
    private boolean usbPermission;
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
    private Tango mTango;
    private TangoConfig mConfig;
    private UsbManager manager;
    private List<UsbSerialDriver> availableDrivers;
    private UsbSerialDriver driver;
    private PendingIntent mPermissionIntent;
    private UsbDeviceConnection connection;
    private UsbSerialPort sPort;
    private TextView mTitleTextView;
    private Button theButton;
    private Button goButton;
    private Button testButton;
    private Button stop;
    private double turnRate = (2 * π) / 4600;
    private boolean looping;
    private boolean tangoPermissions = false;
    private boolean mIsRelocalized;
    private Object mSharedLock = new Object();
    private TangoPoseData currentPose;
    private TangoPoseData target;
    private boolean rotating;
    private boolean moving;
    private Button right;
    private TextView rotationText;
    private TextView bearingText;
    private TextView bearingDeltaText;
    private TextView localizationText;
    private TextView adfText;
    private TextView ipText;
    private TextView distanceText;
    private boolean connected = false;
    private boolean arrived = false;
    private String messageOut = "";
    private boolean started;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.theButton = (Button) findViewById(R.id.lomarf);
        this.goButton = (Button) findViewById(R.id.go_button);
        this.testButton = (Button) findViewById(R.id.test_button);
        this.stop = (Button) findViewById(R.id.stop);
        this.mTitleTextView = (TextView) findViewById(R.id.chome);
        this.right = (Button) findViewById(R.id.right_button);
        this.rotationText = (TextView) findViewById(R.id.rotation_text);
        this.bearingText = (TextView) findViewById(R.id.bearing_text);
        this.bearingDeltaText = (TextView) findViewById(R.id.bearing_delta);
        this.localizationText = (TextView) findViewById(R.id.loc_text);
        this.adfText = (TextView) findViewById(R.id.adfText);
        this.ipText = (TextView) findViewById(R.id.ipaddress);
        this.distanceText = (TextView) findViewById(R.id.distance);

        t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.US);
                }
            }
        });

        this.theButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mTitleTextView.setText("Setting target");
                setTarget();

            }
        });
        this.goButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mTitleTextView.setText("going to target");
                go();

            }
        });
        this.testButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mTitleTextView.setText("testing connection");
                t1.speak("testing connection", TextToSpeech.QUEUE_FLUSH, null);
                testConnection();
            }
        });
        this.stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mTitleTextView.setText("halting");
                t1.speak("halting", TextToSpeech.QUEUE_FLUSH, null);
                stop();
            }
        });
        this.right.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                turn_right();
            }
        });
        mTango = new Tango(this);

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

        Thread rotationThread = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (currentPose != null) {
                                    double rotation = quateq(currentPose);
                                    if (rotation < 0) rotation += 2 * π;
                                    rotationText.setText(String.valueOf(rotation));
                                }
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        rotationThread.start();

        Thread bearingThread = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (currentPose != null && target != null) {
                                    double bearing = bearing(currentPose, target);
                                    if (bearing < 0) bearing += 2 * π;
                                    bearingText.setText(String.valueOf(bearing));
                                }
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        bearingThread.start();

        Thread bearingDeltaThread = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (currentPose != null && target != null) {
                                    double rotation = quateq(currentPose);
                                    if (rotation < 0) rotation += 2 * π;
                                    double bearing = bearing(currentPose, target);
                                    if (bearing < 0) bearing += 2 * π;
                                    double bearing_delta = bearing - rotation;
                                    if (bearing_delta < 0) bearing_delta += 2 * π;
                                    bearingDeltaText.setText(String.valueOf(bearing_delta));
                                }
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        bearingDeltaThread.start();

        final Thread localizationThread = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (mIsRelocalized) {
                                    localizationText.setText("Localized");
                                } else {
                                    localizationText.setText("Not Localized");
                                }
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        localizationThread.start();

        final Thread distanceThread = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (currentPose != null && target != null) {
                                    distanceText.setText(String.valueOf(distance(currentPose, target)));
                                }
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        distanceThread.start();

        ip = getIpAddress();
        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();
        ipText.setText(ip);
    }

    private void testConnection() {
        sendCommand('w');
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sendCommand(' ');
    }

    private void stop() {
        sendCommand(' ');
    }

    /*
        This should give us the amount we have to turn to align with the target
        if it doesn't work
     */
    private double bearing(TangoPoseData bot, TangoPoseData target) {
        double x1 = bot.translation[0];
        double y1 = bot.translation[1];

        double x2 = target.translation[0];
        double y2 = target.translation[1];

        return Math.atan2((y2 - y1), (x2 - x1));
        //return Math.atan2((x2-x1), (y2-y1)); //should work if the axes are... unconventional (i.e. angles are measured in terms of anti-clockwise from y)
    }

    private double distance(TangoPoseData bot, TangoPoseData target) {
        double dx = bot.translation[0] - target.translation[0];
        double dy = bot.translation[1] - target.translation[1];

        double distance = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));

        return distance;
    }


    private void go() {
        if (!started){
            started = true;
            String toSpeak = "go to target";
            //String toSpeak = "IF THE ZOO BANS ME FOR HOLLERING AT THE ANIMALS I WILL FACE GOD AND WALK BACKWARDS INTO HELL";
            t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
        }
        double rotation = quateq(currentPose);
        if (rotation < 0) rotation += 2 * π;
        Log.v(TAG, "Rotation is " + rotation);

        //double targetRotation = 0;

        rotating = false;
        moving = false;

        double bearing = bearing(currentPose, target);
        if (bearing < 0) bearing += 2 * π;
        Log.v(TAG, "Target bearing is " + bearing);

        double bearing_delta = bearing - rotation;
        if (bearing_delta < 0) bearing_delta += 2 * π;
        Log.v(TAG, "Bearing ∆ is " + bearing_delta);

        double distance = distance(currentPose, target);

        double delay = Math.abs(bearing_delta / turnRate);

        Log.v(TAG, "Turn for " + delay + "ms");

//        if (delay >= 75) {
//            Log.v(TAG, "Angle large enough to turn");
//            sendCommand('a');
//            //sendCommand('d');
//            try {
//                Thread.sleep((long) delay);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            sendCommand(' ');
//            sendCommand(' ');
//        }
        if (delay >= 75) {
            Log.v(TAG, "Angle large enough to turn");
            rotating = true;
            if (bearing_delta < π) {
                sendCommand('d');
            } else {
                sendCommand('a');
            }
            try {
                Thread.sleep((long) delay / 3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            sendCommand(' ');
        }

        if (distance > 0.1 && !rotating) {
            Log.v(TAG, "Still far");
            moving = true;
            sendCommand('w');
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            sendCommand(' ');
        }
        if (distance <= 0.1 && !arrived) {
            arrived = true;
            t1.speak("Engaging target", TextToSpeech.QUEUE_FLUSH, null);
            rotating = false;
            moving = false;
        }
    }

    private double quateq(TangoPoseData pose) {
        return Math.atan2(2 * (pose.rotation[3] * pose.rotation[2] + pose.rotation[0] * pose.rotation[1]), 1 - 2 * (pose.rotation[1]
                * pose.rotation[1] + pose.rotation[2] * pose.rotation[2])) + π / 2;
    }

    private void setTarget() {
        String toSpeak = "Setting target";
        t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
        target = currentPose;
        arrived = false;
        started = false;
    }

    private void turn_right() {
        sendCommand('d');
    }

    //robot turns 2*π radians in ~4.5 seconds
    @Override
    protected void onResume() {
        mIsRelocalized = false;
        super.onResume();
        if (tangoPermissions) {
            // Clear the relocalization state: we don't know where the device has been since our app
            // was paused.
            mIsRelocalized = false;

            // Re-attach listeners.
            try {
                setTangoListeners();
            } catch (TangoErrorException e) {
                Toast.makeText(getApplicationContext(), R.string.tango_error, Toast.LENGTH_SHORT)
                        .show();
            } catch (SecurityException e) {
                Toast.makeText(getApplicationContext(), R.string.no_permissions, Toast.LENGTH_SHORT)
                        .show();
            }

            // Connect to the tango service (start receiving pose updates).
            try {
                mTango.connect(mConfig);
            } catch (TangoOutOfDateException e) {
                Toast.makeText(getApplicationContext(), R.string.tango_out_of_date_exception, Toast
                        .LENGTH_SHORT).show();
            } catch (TangoErrorException e) {
                Toast.makeText(getApplicationContext(), R.string.tango_error, Toast.LENGTH_SHORT)
                        .show();
            } catch (TangoInvalidException e) {
                Toast.makeText(getApplicationContext(), R.string.tango_invalid, Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            mTango.disconnect();
        } catch (TangoErrorException e) {
            Toast.makeText(getApplicationContext(), R.string.tango_error, Toast.LENGTH_SHORT)
                    .show();
        }
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
                tangoPermissions = true;
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
                    this.adfText.setText(getName(fullUUIDList.get(fullUUIDList.size() - 1)));
                } else {
                    Log.v("ATTN", "No ADFs found");
                }
                setTangoListeners();
            }
        }
    }

    private void tryConnect() {
        Log.v(TAG, "Trying to connect");
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
        try {
            sPort.open(this.connection);
            sPort.setParameters(115200, 8, 1, 0);
            connected = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendCommand(char c) {
        if (connected) {
            Log.v(TAG, "Sending command: " + c);
            try {
                byte[] arrby = new byte[8];
                arrby[0] = (byte) c;
                sPort.write(arrby, 1000);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.v(TAG, "Not connected");
            tryConnect();
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

    private void setTangoListeners() {
        // Select coordinate frame pairs
        ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<TangoCoordinateFramePair>();
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                TangoPoseData.COORDINATE_FRAME_DEVICE));
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                TangoPoseData.COORDINATE_FRAME_DEVICE));
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE));
        Log.v(TAG, "Entering connectListener");
        mTango.connectListener(framePairs, new Tango.OnTangoUpdateListener() {

            @Override
            public void onPoseAvailable(TangoPoseData pose) {
                // Make sure to have atomic access to Tango Data so that
                // UI loop doesn't interfere while Pose call back is updating
                // the data.
                synchronized (mSharedLock) {
                    // Check for Device wrt ADF pose, Device wrt Start of Service pose,
                    // Start of Service wrt ADF pose (This pose determines if the device
                    // is relocalized or not).
                    if (pose.baseFrame == TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION
                            && pose.targetFrame == TangoPoseData.COORDINATE_FRAME_DEVICE) {

                        if (mIsRelocalized) {
                            currentPose = pose;
                        }
                    } else if (pose.baseFrame == TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION
                            && pose.targetFrame == TangoPoseData
                            .COORDINATE_FRAME_START_OF_SERVICE) {
                        if (pose.statusCode == TangoPoseData.POSE_VALID) {
                            mIsRelocalized = true;
                            // Set the color to green
                        } else {
                            mIsRelocalized = false;
                            // Set the color blue
                        }
                    }
                }
                if (rotating || moving) {
                    //Log.v(TAG, "still moving");
                    go();
                }

            }

            @Override
            public void onXyzIjAvailable(TangoXyzIjData tangoXyzIjData) {

            }

            @Override
            public void onFrameAvailable(int i) {

            }

            @Override
            public void onTangoEvent(TangoEvent tangoEvent) {

            }

        });
        Log.v(TAG, "Successfully implemented pose listener!");
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class ServerThread implements Runnable {

        public void run() {
            Log.v(TAG, "Server Thread Started");
            Socket socket = null;
            try {
                serverSocket = new ServerSocket(SERVERPORT);
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (!Thread.currentThread().isInterrupted()) {

                try {
                    //Log.v(TAG, "waiting for client");
                    socket = serverSocket.accept();
                    Log.v(TAG, "accepted client socket");
                    CommunicationThread commThread = new CommunicationThread(socket);
                    new Thread(commThread).start();

                } catch (IOException e) {
                   // e.printStackTrace();
                }
            }
        }
    }

    class CommunicationThread implements Runnable {

        private Socket clientSocket;

        private BufferedReader input;

        public CommunicationThread(Socket clientSocket) {
            Log.v(TAG, "attempting to set up communications");
            this.clientSocket = clientSocket;

            try {

                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
                Log.v(TAG, "Input ok");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {

            while (!Thread.currentThread().isInterrupted()) {

                try {

                    String read = input.readLine();
                    char first = read.charAt(0);
                    switch (first){
                        case 116:
                            setTarget();
                            break;
                        case 103:
                            go();
                            break;
                        default:
                            sendCommand(first);
                            break;
                    }

                } catch (IOException e) {

                    e.printStackTrace();
                }
            }
        }

    }


    public static String getIpAddress() {
        String ipAddress = "Unable to Fetch IP..";
        try {
            Enumeration en;
            en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface intf = (NetworkInterface) en.nextElement();
                for (Enumeration enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = (InetAddress) enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        ipAddress = inetAddress.getHostAddress().toString();
                        return ipAddress;
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return ipAddress;
    }


}
