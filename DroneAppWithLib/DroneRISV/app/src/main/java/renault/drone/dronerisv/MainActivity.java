package renault.drone.dronerisv;

import android.Manifest;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import renault.drone.risvrenault.Drone;
import renault.drone.risvrenault.FollowQRCode;
import renault.drone.risvrenault.MissionListener;

public class MainActivity extends Activity implements View.OnClickListener, TextureView.SurfaceTextureListener {

    private static final String TAG = MainActivity.class.getName();
    private long SPEED_REFRESH = 1000;

    private long speedRefresh = 1000;


    protected TextureView mVideoSurface = null;
    private FollowQRCode cameraZone;

    private Button mStopBtn;
    private TextView altitudeTxt;
    private TextView gpsTxt;
    private TextView phoneGPSText;
    private TextView isFlyingTxt;
    private TextView downloadPercent;

    private int sWidth = -1;
    private int sHeight = -1;
    private boolean interrupted = true;

    private Button btnTakeoff;
    private Button btnLand;
    private Button btnCarCrash;
    private Button btnFollowMode;


    private TextView satellite;
    private TextView signalGPS;
    private TextView battery;

    private Drone drone;
    private boolean isFirst = true;


    private TextView velocityX;
    private TextView velocityY;
    private TextView velocityZ;

    private TextView flightTime;

    private ImageView photo;
    private Thread displayThread;
    private TextView logTxt;
    private TextView distanceText;
    private TextView missionTxt;
    private TextView isoTxt;
    private TextView homeTxt;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        // CHECK permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
                            Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                            Manifest.permission.READ_PHONE_STATE
                    }
                    , 1);
        }

        initUI();

        // Register the application
        drone = new Drone(this);
    }

    @Override
    public void onResume() {

        Log.e(TAG, "onResume");
        super.onResume();
        drone.initLiveVideo();

        drone.resumeLiveVideo(this);
        interrupted = false;

        isFirst = true;
        startDisplayThread();

        MissionListener missionListener = new MissionListener() {
            @Override
            public void onResultFollow(Boolean isSuccess, final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        downloadPercent.setVisibility(View.GONE);
                    }
                });
                showLog(message);
            }

            @Override
            public void onResultCarCrash(Boolean isSuccess, final String message, final float percentDownload, final Bitmap[] photos) {

                if (percentDownload > 0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            downloadPercent.setVisibility(View.VISIBLE);
                            downloadPercent.setText(message + percentDownload + "%");
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            downloadPercent.setVisibility(View.GONE);
                        }
                    });
                    showLog(message);
                }

                if (photos != null && photos[0] != null && photos[1] != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            photo.setVisibility(View.VISIBLE);
                            photo.setImageBitmap(photos[0]);
//                            photo.setImageBitmap(photos[1]);
                        }
                    });
                }
            }

            @Override
            public void onResultLand(Boolean isSuccess, String message) {
                showLog(message);
            }

            @Override
            public void onResultLaunch(Boolean isSuccess, String message) {
                showLog(message);
            }

            @Override
            public void onResultNoMission(Boolean isSuccess, String message) {
                showLog(message);
            }
        };

        drone.callbackRegisterEndMission(missionListener);
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        interrupted = true;
        if (drone != null) {
            drone.abortMission();
        }
        if (displayThread != null) {
            displayThread.interrupt();
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        drone.unInitLiveView(this);
        super.onDestroy();
    }

    private void initUI() {
        // init mVideoSurface
        mVideoSurface = (TextureView) findViewById(R.id.video_previewer_surface);
        cameraZone = (FollowQRCode) findViewById(R.id.cameraZone);

        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }

        photo = (ImageView) findViewById(R.id.image);


        altitudeTxt = (TextView) findViewById(R.id.altitude);
        gpsTxt = (TextView) findViewById(R.id.positionGPS);
        phoneGPSText = (TextView) findViewById(R.id.phonePositionGPS);
        distanceText = (TextView) findViewById(R.id.distanceTxt);

        missionTxt = (TextView) findViewById(R.id.missionText);

        isFlyingTxt = (TextView) findViewById(R.id.isFlyingBool);

        downloadPercent = (TextView) findViewById(R.id.DownloadPercent);

        isoTxt = (TextView) findViewById(R.id.isotxt);
        homeTxt = (TextView) findViewById(R.id.textHomePosition);
        logTxt = (TextView) findViewById(R.id.logcontent);
        logTxt.setMovementMethod(new ScrollingMovementMethod());

        velocityX = (TextView) findViewById(R.id.velocityX);
        velocityY = (TextView) findViewById(R.id.velocityY);
        velocityZ = (TextView) findViewById(R.id.velocityZ);

        flightTime = (TextView) findViewById(R.id.flightTime);

        btnTakeoff = (Button) findViewById(R.id.btn_takeoff);
        btnLand = (Button) findViewById(R.id.btn_land);
        btnCarCrash = (Button) findViewById(R.id.btn_car_crash);
        mStopBtn = (Button) findViewById(R.id.btn_stop);
        btnFollowMode = (Button) findViewById(R.id.btn_follow_mode);

        satellite = (TextView) findViewById(R.id.satellite);
        signalGPS = (TextView) findViewById(R.id.signalGPS);
        battery = (TextView) findViewById(R.id.batteryLvl);

        mStopBtn.setOnClickListener(this);
        btnTakeoff.setOnClickListener(this);
        btnLand.setOnClickListener(this);
        btnCarCrash.setOnClickListener(this);
        btnFollowMode.setOnClickListener(this);


    }

    public void showLog(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(msg != null && !msg.equals("")) {
                    logTxt.setText(msg);
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_stop: {
                drone.abortMission();
                break;
            }
            case R.id.btn_takeoff: {
                drone.setMission(Drone.Mission.LAUNCH);
                break;
            }
            case R.id.btn_land: {
                drone.setMission(Drone.Mission.LAND);
                break;
            }
            case R.id.btn_car_crash: {
                drone.setMission(Drone.Mission.CAR_CRASH);
                break;
            }
            case R.id.btn_follow_mode: {
                drone.setMission(Drone.Mission.FOLLOW);
                break;
            }
            default:
                break;
        }
    }

    private void startDisplayThread() {
        final Handler handler = new Handler();

        displayThread = new Thread(new Runnable() {
            public void run() {

                while (!interrupted) {

                    try {
                        if (drone.isConnected()) {
                            if (isFirst) {
                                try {
                                    isFirst = !(drone.init(getApplicationContext(), mVideoSurface, cameraZone, drone.getDrone(), sWidth, sHeight));
//                                    if(!isFirst){
//                                        interrupted = true;
//                                    }
                                    speedRefresh = SPEED_REFRESH;
                                } catch (Exception e) {
                                    showLog("Error when init the drone : "+e.getMessage());
                                }
                            }

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (drone.getDroneStates() != null && drone.getGPSPositionDrone() != null) {
                                        altitudeTxt.setText("Altitude sensor: " + drone.getDroneStates().getAltitudeFromSensor() + "m / GPS : " + drone.getDroneStates().getAltitudeFromGPS() + "m");
                                        gpsTxt.setText(drone.getGPSPositionDrone().getLatitude() + " " + drone.getGPSPositionDrone().getLongitude());
                                        if (drone.getGPSPositionRC() != null) {
                                            phoneGPSText.setText(drone.getGPSPositionRC().getLatitude() + " " + drone.getGPSPositionRC().getLongitude());
                                        }
                                        homeTxt.setText(drone.getDroneStates().getHomeLocation().getLatitude() + " " +  drone.getDroneStates().getHomeLocation().getLongitude());

                                        isFlyingTxt.setText("Is flying: " + String.valueOf(drone.getDroneStates().isFlying()));

                                        velocityX.setText(String.valueOf(drone.getDroneStates().getVelocityX()));
                                        velocityY.setText(String.valueOf(drone.getDroneStates().getVelocityY()));
                                        velocityZ.setText(String.valueOf(drone.getDroneStates().getVelocityZ()));

                                        missionTxt.setText(drone.getCurrentMission().name());
                                        isoTxt.setText("ISO: " + drone.getDroneStates().getIso());


                                        distanceText.setText("Distance :" + drone.getDroneStates().getDistanceBetweenDroneAndRC() + "m");

                                        int m = 0;
                                        int s = drone.getDroneStates().getTotalFlightTime();

                                        while (s >= 60) {
                                            if (s >= 60) {
                                                m += 1;
                                                s = s - 60;
                                            }
                                        }
//                                        flightTime.setText(String.valueOf(drone.getDroneStates().getTotalFlightTime()));
                                        flightTime.setText(m + ":" + s);

                                        signalGPS.setText("Signal GPS : " + String.valueOf(drone.getDroneStates().getGPSSignal()));
                                        satellite.setText("Satellite : " + (drone.getDroneStates().getNbSatellite()));
                                        battery.setText(drone.getBatteryLevel() + "%");

                                        if (drone.getDroneStates().isFlying()) {
                                            btnTakeoff.setVisibility(View.GONE);
                                            btnLand.setVisibility(View.VISIBLE);
                                        } else {
                                            btnTakeoff.setVisibility(View.VISIBLE);
                                            btnLand.setVisibility(View.GONE);
                                        }

                                        if (mStopBtn.getText().equals("Start")) {
                                            mStopBtn.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorGreen));
                                        } else {
                                            mStopBtn.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRed));
                                        }
                                    } else {
                                        phoneGPSText.setText("Drone state is null");
                                    }
                                }
                            });
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//
//                                    if (drone.getDroneStates() != null && drone.getGPSPositionDrone() != null) {
//                                        altitudeTxt.setText("Altitude sensor: " + drone.getDroneStates().getAltitudeFromSensor() + "m / GPS : " + drone.getDroneStates().getAltitudeFromGPS() + "m");
//                                        gpsTxt.setText(drone.getGPSPositionDrone().getLatitude() + " " + drone.getGPSPositionDrone().getLongitude());
//                                        if (drone.getGPSPositionRC() != null) {
//                                            phoneGPSText.setText(drone.getGPSPositionRC().getLatitude() + " " + drone.getGPSPositionRC().getLongitude());
//                                        }
//                                        isFlyingTxt.setText(String.valueOf(drone.getDroneStates().isFlying()));
//
//                                        velocityX.setText(String.valueOf(drone.getDroneStates().getVelocityX()));
//                                        velocityY.setText(String.valueOf(drone.getDroneStates().getVelocityY()));
//                                        velocityZ.setText(String.valueOf(drone.getDroneStates().getVelocityZ()));
//
//                                        int m = 0;
//                                        int s = drone.getDroneStates().getTotalFlightTime();
//
//                                        while (s >= 60) {
//                                            if (s >= 60) {
//                                                m += 1;
//                                                s = s - 60;
//                                            }
//                                        }
////                                        flightTime.setText(String.valueOf(drone.getDroneStates().getTotalFlightTime()));
//                                        flightTime.setText(m + ":" + s);
//
//                                        signalGPS.setText("Signal GPS : " + String.valueOf(drone.getDroneStates().getGPSSignal()));
//                                        satellite.setText("Satellite : " + (drone.getDroneStates().getNbSatellite()));
//                                        battery.setText(drone.getBatteryLevel() + "%");
//
//                                        if (drone.getDroneStates().isFlying()) {
//                                            btnTakeoff.setVisibility(View.GONE);
//                                            btnLand.setVisibility(View.VISIBLE);
//                                        } else {
//                                            btnTakeoff.setVisibility(View.VISIBLE);
//                                            btnLand.setVisibility(View.GONE);
//                                        }
//
//                                        if (mStopBtn.getText().equals("Start")) {
//                                            mStopBtn.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorGreen));
//                                        } else {
//                                            mStopBtn.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRed));
//                                        }
//                                    } else {
//                                        phoneGPSText.setText("Drone state is null");
//                                    }
//
//
//
//                            }
//                        });
                    }
                } catch(Exception e){
                    showLog("Error when displaying drone state : "+e.getMessage());
                }

                try {
                    Thread.sleep(speedRefresh);
                } catch (InterruptedException e) {
                    showLog(e.getMessage());
                }
            }
        }
    });
        displayThread.start();
}


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        drone.onSurfaceTextureAvailable(surface, width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
//        drone.onSurfaceTextureAvailable(surface, width, height);

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return drone.onSurfaceTextureDestroyed();
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }


}