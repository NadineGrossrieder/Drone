package renault.drone.dronerisv;

import android.Manifest;
import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import renault.drone.risvrenault.Drone;
import renault.drone.risvrenault.FollowQRCode;

public class MainActivity extends Activity implements View.OnClickListener, TextureView.SurfaceTextureListener {

    private static final String TAG = MainActivity.class.getName();
    private long SPEED_REFRESH = 500;

    private long speedRefresh = 5000;


    protected TextureView mVideoSurface = null;
    private FollowQRCode cameraZone;

    private Button mStopBtn;
    private TextView altitudeTxt;
    private TextView gpsTxt;
    private TextView phoneGPSText;
    private TextView isFlyingTxt;

    private int sWidth = -1;
    private int sHeight = -1;
    private boolean interrupted = true;

    private Button btnTakeoff;
    private Button btnLand;
    private Button btnCarCrash;
    private Button btnFollowMode;
//    private Button btnLandMode;


    private TextView satellite;
    private TextView signalGPS;
    private TextView battery;

    private Drone drone;
    private boolean isFirst = true;


    private TextView velocityX;
    private TextView velocityY;
    private TextView velocityZ;

    private TextView flightTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

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

    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();

        initUI();


        drone = new Drone(this);


//        if (mVideoSurface == null) {
//            Log.e(TAG, "mVideoSurface is null");
//        }
        mVideoSurface.setSurfaceTextureListener(this);

        drone.initLiveView();

        interrupted = false;

        isFirst = true;
        startDisplayThread();
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        interrupted = true;
        drone.unInitLiveView();
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
        drone.unInitLiveView();
        super.onDestroy();
    }

    private void initUI() {
        // init mVideoSurface
        mVideoSurface = (TextureView) findViewById(R.id.video_previewer_surface);
        cameraZone = (FollowQRCode) findViewById(R.id.cameraZone);

        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }


        altitudeTxt = (TextView) findViewById(R.id.altitude);
        gpsTxt = (TextView) findViewById(R.id.positionGPS);
        phoneGPSText = (TextView) findViewById(R.id.phonePositionGPS);

        isFlyingTxt = (TextView) findViewById(R.id.isFlyingBool);

        velocityX = (TextView) findViewById(R.id.velocityX);
        velocityY = (TextView) findViewById(R.id.velocityY);
        velocityZ = (TextView) findViewById(R.id.velocityZ);

        flightTime = (TextView) findViewById(R.id.flightTime);

        btnTakeoff = (Button) findViewById(R.id.btn_takeoff);
        btnLand = (Button) findViewById(R.id.btn_land);
        btnCarCrash = (Button) findViewById(R.id.btn_car_crash);
        mStopBtn = (Button) findViewById(R.id.btn_stop);
        btnFollowMode = (Button) findViewById(R.id.btn_follow_mode);
//        btnLandMode = (Button) findViewById(R.id.btn_land_mode);

        satellite = (TextView) findViewById(R.id.satellite);
        signalGPS = (TextView) findViewById(R.id.signalGPS);
        battery = (TextView) findViewById(R.id.batteryLvl);
//        currentAltitudeSinceStart = (TextView) findViewById(R.id.currentAltitudeSinceStart);


        mStopBtn.setOnClickListener(this);
        btnTakeoff.setOnClickListener(this);
        btnLand.setOnClickListener(this);
        btnCarCrash.setOnClickListener(this);
        btnFollowMode.setOnClickListener(this);
//        btnLandMode.setOnClickListener(this);


    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_stop: {
                drone.setMission(Drone.Mission.NO_MISSION);
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
//            case R.id.btn_land_mode: {
//                drone.setMission(Drone.Mission.LAND);
//                break;
//            }
            default:
                break;
        }
    }

    private void startDisplayThread() {
        Thread th = new Thread(new Runnable() {
            public void run() {

                while (!interrupted) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (drone.isConnected()) {
                                    velocityY.setText(isFirst + "");
                                    if (isFirst) {
                                        try {
//                                            isFirst = false;
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    isFirst = !(drone.init(getApplicationContext(), mVideoSurface, cameraZone, drone.getDrone(), sWidth, sHeight));
                                                    drone.initLiveView();
                                                }
                                            });
                                            speedRefresh = SPEED_REFRESH;
                                        } catch (Exception e) {
                                            Log.e(TAG, e.getMessage());
                                            velocityX.setText(e.getMessage());

                                        }
                                    }
                                    if (drone.getDroneStates() != null) {
                                        altitudeTxt.setText("Altitude sensor: " + drone.getDroneStates().getAltitudeFromSensor() + "m / GPS : " + drone.getDroneStates().getAltitudeFromGPS() + "m");
                                        gpsTxt.setText(drone.getGPSPositionDrone().getLatitude() + " " + drone.getGPSPositionDrone().getLongitude());
                                        phoneGPSText.setText(drone.getGPSPositionRC().getLatitude() + " " + drone.getGPSPositionRC().getLongitude());
                                        isFlyingTxt.setText(String.valueOf(drone.getDroneStates().isFlying()));

                                        velocityX.setText(String.valueOf(drone.getDroneStates().getVelocityX()));
                                        velocityY.setText(String.valueOf(drone.getDroneStates().getVelocityY()));
                                        velocityZ.setText(String.valueOf(drone.getDroneStates().getVelocityZ()));

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
                            } catch (Exception e) {
                                Log.e(TAG, e.getMessage());
//                                velocityZ.setText(e.getMessage());
                            }
                        }
                    });

                    try {
                        Thread.sleep(speedRefresh);
                    } catch (InterruptedException e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            }
        });
        th.start();
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        drone.onSurfaceTextureAvailable(surface, width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        drone.onSurfaceTextureAvailable(surface, width, height);

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return drone.onSurfaceTextureDestroyed();
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }


}