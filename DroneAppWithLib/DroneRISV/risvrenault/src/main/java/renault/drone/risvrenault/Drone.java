package renault.drone.risvrenault;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.TextureView;

import java.io.File;

import dji.common.airlink.SignalQualityCallback;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.flightcontroller.FlightOrientationMode;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.product.Model;
import dji.common.remotecontroller.GPSData;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

/**
 * Created by Nadine Grossrieder on 10.05.2017.
 * <p>
 * Represents the drone and its actions
 */

public class Drone {


    /**
     * Represents all available missions
     */
    public enum Mission {
        /**
         * No mission defined
         */
        NO_MISSION,

        /**
         * Mission to launch the drone
         */
        LAUNCH,

        /**
         * Mission to land the drone
         */
        LAND,

        /**
         * Mission to take pictures of a car crash
         */
        CAR_CRASH,

        /**
         * Mission to follow a QRCode
         */
        FOLLOW
    }

    private static final String TAG = "Drone";

    private static final Float FIRST_ALTITUDE = 3.0f;
    private static final Float SECOND_ALTITUDE = 2.0f;
    private static final Float ALTITUDE_MARGIN = 0.1f;
    private static final float MIN_ALTITUDE = 2.5f;

    private static final float FOLLOW_ALTITUDE = 3.0f;

    private static final long WAIT_100MS = 100;
    private static final long WAIT_1000MS = 1000;
    private static final long WAIT_2000MS = 2000;

    private Boolean isReady = false;

    private Mission currentMission = Mission.NO_MISSION;
    private MissionListener missionListener;

    private Handler mHandler;

    private Context activity;
    private TextureView cameraView;
    private FollowQRCode cameraZone;
    private BaseProduct baseProduct;
    private Aircraft drone;
    private int cameraViewHeight = -1;
    private int cameraViewWidth = -1;

    private Camera camera;
    private DroneStates droneStates;
    private boolean droneIsConnected;


    private GPSData.GPSLocation rcLocation;
    private Intent intent;


    private int downlinkSignalQuality;
    private int uplinkSignalQuality;

    // Fields for video
    private VideoFeeder.VideoDataCallback mReceivedVideoDataCallBack;
    private DJICodecManager mCodecManager;

    // Mission Thread
    private Thread threadMission;


    public Drone(final Context activity) {
//        Toast.makeText(activity, "Drone", Toast.LENGTH_SHORT).show();

        this.activity = activity;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    activity.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED &&
                    activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                sendMessageBack(false, "Permission must be granted");
            } else {
                if (!DJISDKManager.getInstance().hasSDKRegistered()) {
                    mHandler = new Handler(Looper.getMainLooper());
                    DJISDKManager.getInstance().registerApp(activity, mDJISDKManagerCallback);

                } else {
                    sendMessageBack(false, "Already registered");
                }
            }

            // The callback for receiving the raw H264 video data for camera live view
            if (mReceivedVideoDataCallBack == null) {
                mReceivedVideoDataCallBack = new VideoFeeder.VideoDataCallback() {
                    @Override
                    public void onReceive(byte[] videoBuffer, int size) {
                        if (mCodecManager != null) {
                            mCodecManager.sendDataToDecoder(videoBuffer, size);

                        }
                    }
                };
            }


        } catch (Exception e) {
            sendMessageBack(false, "Register the drone : " + e.getMessage());
        }
    }


    /**
     * When starting SDK services, an instance of interface DJISDKManager.DJISDKManagerCallback will be used to listen to
     * the SDK Registration result and the product changing.
     */
    private DJISDKManager.SDKManagerCallback mDJISDKManagerCallback = new DJISDKManager.SDKManagerCallback() {

        //Listens to the SDK registration result
        @Override
        public void onRegister(DJIError error) {
            if (error == DJISDKError.REGISTRATION_SUCCESS) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        sendMessageBack(false, "Register success");
                        DJISDKManager.getInstance().startConnectionToProduct();
                    }
                });


            } else {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        sendMessageBack(false, "Register sdk fails, check network is available");
                    }
                });

            }
        }

        //Listens to the connected product changing, including two parts, component changing or product connection changing.
        @Override
        public void onProductChange(BaseProduct oldProduct, BaseProduct newProduct) {
            baseProduct = newProduct;
            if (baseProduct != null) {
                baseProduct.setBaseProductListener(mDJIBaseProductListener);
            }
        }
    };

    private BaseProduct.BaseProductListener mDJIBaseProductListener = new BaseProduct.BaseProductListener() {

        @Override
        public void onComponentChange(BaseProduct.ComponentKey key, BaseComponent oldComponent, BaseComponent newComponent) {
            if (newComponent != null) {
                newComponent.setComponentListener(mDJIComponentListener);
            }
        }

        @Override
        public void onConnectivityChange(boolean isConnected) {
            droneIsConnected = isConnected;
        }

    };

    private BaseComponent.ComponentListener mDJIComponentListener = new BaseComponent.ComponentListener() {

        @Override
        public void onConnectivityChange(boolean isConnected) {
            droneIsConnected = isConnected;
        }
    };


    /**
     * Init the drone class
     *
     * @param activity         : Context of an app view
     * @param cameraView       : TextureView where the live camera is displayed
     * @param cameraZone       : Instance of FollowQRCode that extend View
     * @param baseProduct      : instance of the BaseProduct corresponding to the drone
     * @param cameraViewWidth  : width of the live camera
     * @param cameraViewHeight : height of the live camera    @return True, if success, false if not success
     */
    public Boolean init(final Context activity, TextureView cameraView, FollowQRCode cameraZone, BaseProduct baseProduct, int cameraViewWidth, int cameraViewHeight) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                activity.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED &&
                activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            sendMessageBack(false, "Permission must be granted");
        } else {
            if (DJISDKManager.getInstance().hasSDKRegistered() && activity != null && cameraView != null && baseProduct != null && cameraZone != null) {
                initLiveView();
                sendMessageBack(false, "Aircraft : " + baseProduct.getModel().getDisplayName());

                // GET phone GPS position
                intent = new Intent(this.activity, PhoneGPSLocation.class);
                this.activity.startService(intent);


//            this.activity = activity;
                this.cameraView = cameraView;
                this.cameraZone = cameraZone;
                this.drone = (Aircraft) baseProduct;
                this.baseProduct = baseProduct;
//            if(cameraViewWidth > 0 && cameraViewHeight > 0) {
//                this.cameraViewWidth = cameraViewWidth;
//                this.cameraViewHeight = cameraViewHeight;
//            }

                this.droneStates = new DroneStates(drone);
                this.camera = new Camera(drone);

                drone.getCamera().setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, null);


                // GET RC GPS position
//            drone.getRemoteController().setGPSDataCallback(new GPSData.Callback() {
//                @Override
//                public void onUpdate(@NonNull GPSData gpsData) {
//                    rcLocation = gpsData.getLocation();
//                    Toast.makeText(activity, "gps data:" + (gpsData.toString()) , Toast.LENGTH_SHORT).show();
//
//                }
//            });


                // GET downlink signal quality
                drone.getAirLink().setDownlinkSignalQualityCallback(new SignalQualityCallback() {
                    @Override
                    public void onUpdate(int i) {
                        downlinkSignalQuality = i;
                    }
                });

                // GET uplink signal quality
                drone.getAirLink().setUplinkSignalQualityCallback(new SignalQualityCallback() {
                    @Override
                    public void onUpdate(int i) {
                        uplinkSignalQuality = i;
                    }
                });

                isReady = true;
            } else if (activity != null) {
                if (!DJISDKManager.getInstance().hasSDKRegistered()) {
                    sendMessageBack(false, "DJI registration has failed");
                }
                if (cameraView == null) {
                    sendMessageBack(false, "TextureView is null");
                }
                if (baseProduct == null) {
                    sendMessageBack(false, "Drone is null");
                }
                if (cameraZone == null) {
                    sendMessageBack(false, "Instance of FollowQRCode is null");
                }
            }
        }
        return isReady;
    }


    public void initLiveVideo() {
        mReceivedVideoDataCallBack = new VideoFeeder.VideoDataCallback() {

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };
    }

    public void resumeLiveVideo(TextureView.SurfaceTextureListener listener) {
        DJISDKManager.getInstance().startConnectionToProduct();

        if (drone == null || !drone.isConnected()) {
            sendMessageBack(false, "Drone not connected");
        } else {
            sendMessageBack(false, "Drone connected");

            if (null != cameraView) {
                cameraView.setSurfaceTextureListener(listener);
            }
            if (!drone.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                if (VideoFeeder.getInstance().getVideoFeeds() != null
                        && VideoFeeder.getInstance().getVideoFeeds().size() > 0) {

                    VideoFeeder.getInstance().getVideoFeeds().get(0).setCallback(mReceivedVideoDataCallBack);
                }
            }
        }
    }

    /**
     * Retrieves a BaseProduct corresponding to the drone connected
     *
     * @return BaseProduct when a drone is connected, null if drone not connected
     */
    public BaseProduct getDrone() {
        return DJISDKManager.getInstance().getProduct();
    }

    /**
     * Retrieves the LocationCoordinate3D to locate the drone with its GPS location
     *
     * @return LocationCoordinate3D that contains latitude, longitude and altitude of the drone
     */
    public LocationCoordinate3D getGPSPositionDrone() {
        return droneStates.getLocation();
    }

    /**
     * Retrieves the LocationCoordinate3D to locate the remote controller with its GPS location
     *
     * @return Location that contains latitude, longitude and altitude of the remote controller
     */
    public Location getGPSPositionRC() {
//        Location location = new Location("RCLocation");
//        if(rcLocation != null) {
//            location.setLatitude(rcLocation.getLatitude());
//            location.setLongitude(rcLocation.getLongitude());
//        }
//        return location;
        return PhoneGPSLocation.mCurrentLocation;
    }

    /**
     * Retrieves distance total in meter traveled since the takeoff
     *
     * @return A float representing the distance total
     */
    public float getDistanceTotal() {
        // TODO distance total
        return 0.0f;
    }

    /**
     * Retrieves distance from the starting point
     *
     * @return A float reprenting the ditrance between the starting point and the drone
     */
    public float getDistance() {
        Location droneLocation = new Location("drone");
        droneLocation.setLatitude(getGPSPositionDrone().getLatitude());
        droneLocation.setLongitude(getGPSPositionDrone().getLongitude());
        return getGPSPositionRC().distanceTo(droneLocation);
    }

    /**
     * Retrieves orientation information (pitch, roll, yaw) for the drone
     *
     * @return an instance of Orientation that contains current yaw, pitch and roll values
     */
    public Orientation getDroneOrientation() {
        return droneStates.getOrientation();
    }

    /**
     * Get the drone's state
     *
     * @return An instance of DroneStates
     */
    public DroneStates getDroneStates() {
        try {
            return droneStates;
        } catch (Exception e) {
            return null;
        }

    }

    public Camera getCameraState() {
        return camera;
    }

    /**
     * Retrieves the current mission defined
     *
     * @return the current mission
     */
    public Mission getMission() {
        return currentMission;
    }

    /**
     * Define a new mission to execute
     * No Mission : does nothing. if mission is running, stop the mission
     * Launch : The drone takes off. Condition: must have no mission and must not already fly
     * Land : The drone lands. Current mission must be No Mission and the drone must fly
     * Car Crash : The drone takes off if not already flying.
     * Go to the First_Altitude, takes picture. Go to Second_Altitude, takes a picture.
     * Go to Min_Altitude and Land
     * Start the download of the last pictures when on the ground
     * (The download freeze the live video)
     *
     * @param mission : Mission to start
     * @return true if operation is authorized, false if no right to define this mission
     */
    public boolean setMission(final Mission mission) {
        boolean isAuthorized = false;

        if (isReady) {
            droneStates = new DroneStates(drone);

            switch (mission) {
                case NO_MISSION:
                    abortMission();
                    break;
                case LAUNCH:
//                    if(mission != Mission.NO_MISSION) {
//                        finishMission(true);
//                    }
//                    if (currentMission == Mission.NO_MISSION && !droneStates.isFlying()) {
                    sendMessageBack(true, "Launch");
                    currentMission = mission;
                    isAuthorized = true;
                    abortMission();

                    threadMission = new Thread(new Runnable() {
                        public void run() {
                            try {
                                Thread.sleep(WAIT_2000MS);

                                takeoff();
                                Thread.sleep(WAIT_2000MS);
//                                finishMission(false);
                            } catch (Exception e) {
                                sendMessageBack(false, "launch error: " + e.getMessage());

                            }
                        }
                    });
                    threadMission.start();

//                    }else{
//                        sendMessageBack(false, "Not authorized to launch, another mission is defined");
//                    }
                    break;
                case LAND:
//                    if(mission != Mission.NO_MISSION) {
//                        finishMission(true);
//                    }

//                    if (currentMission == Mission.NO_MISSION && droneStates.isFlying()) {
                    isAuthorized = true;
                    sendMessageBack(true, "Land");
                    currentMission = mission;
                    abortMission();

                    threadMission = new Thread(new Runnable() {
                        public void run() {
                            try {
//                                do {
                                    Thread.sleep(WAIT_2000MS);

                                    land();
                                Thread.sleep(WAIT_2000MS);

//                                }while(droneStates.isFlying());
//                                finishMission(false);

                            } catch (Exception e) {
                                sendMessageBack(false, "Land error: " + e.getMessage());

                            }
                        }
                    });
                    threadMission.start();

//                    }
//                    else{
//                        sendMessageBack(false, "Not authorized to land, another mission is defined");
//                    }
                    break;
                case CAR_CRASH:
                    if (currentMission == Mission.NO_MISSION) {
                        currentMission = mission;
                        sendMessageBack(true, "Car crash");

                        if (camera != null) {


                            threadMission = new Thread(new Runnable() {
                                public void run() {
                                    try {
                                        if (!droneStates.isFlying()) {
                                            takeoff();
                                        }

                                        Thread.sleep(WAIT_1000MS);

                                        do {
                                            changeAltitude(MIN_ALTITUDE);
                                            Thread.sleep(WAIT_100MS);
                                        }
                                        while (droneStates.getAltitudeFromGPS() < MIN_ALTITUDE - ALTITUDE_MARGIN);

                                        while (droneStates.getAltitudeFromGPS() < FIRST_ALTITUDE - ALTITUDE_MARGIN || droneStates.getAltitudeFromGPS() > FIRST_ALTITUDE + ALTITUDE_MARGIN) {
                                            if (cameraZone.readThread == null) {
                                                cameraZone.setAltitude(FIRST_ALTITUDE);
                                                cameraZone.resume(Drone.this, activity, drone, cameraView, cameraViewWidth, cameraViewHeight, droneStates, missionListener);
                                            }

                                            Thread.sleep(WAIT_1000MS);
                                        }
                                        sendMessageBack(true, "Follow mode killed");
                                        if (cameraZone.readThread != null) {
                                            cameraZone.readThread.kill();
                                        }

                                        camera.takePicture(missionListener, activity);

                                        Thread.sleep(WAIT_2000MS);
                                        sendMessageBack(true, "Go to altitude 2");

                                        while (droneStates.getAltitudeFromGPS() < SECOND_ALTITUDE - ALTITUDE_MARGIN || droneStates.getAltitudeFromGPS() > SECOND_ALTITUDE + ALTITUDE_MARGIN) {
                                            if (cameraZone.readThread == null) {
                                                cameraZone.setAltitude(SECOND_ALTITUDE);
                                                cameraZone.resume(Drone.this, activity, drone, cameraView, cameraViewWidth, cameraViewHeight, droneStates, missionListener);
                                            }
                                            Thread.sleep(WAIT_1000MS);
                                        }

                                        sendMessageBack(true, "Altitude 2 reached");
                                        if (cameraZone.readThread != null) {
                                            cameraZone.readThread.kill();
                                        }
                                        camera.takePicture(missionListener, activity);

//                                        if(droneStates.getAltitudeFromGPS() > MIN_ALTITUDE){
//                                            while (droneStates.getAltitudeFromGPS() > MIN_ALTITUDE + ALTITUDE_MARGIN) {
//                                                if(cameraZone.readThread == null) {
//                                                    cameraZone.setAltitude(SECOND_ALTITUDE);
//                                                    cameraZone.resume(Drone.this, activity, drone, cameraView, cameraViewWidth, cameraViewHeight, droneStates, missionListener);
//                                                }
//                                                Thread.sleep(WAIT_1000MS);
//                                            }
//                                        }
//                                            if(cameraZone.readThread != null) {
//                                        cameraZone.readThread.kill();
//                                            }
                                        Thread.sleep(WAIT_2000MS);


                                        try {
                                            drone.getFlightController().setVirtualStickModeEnabled(false, new CommonCallbacks.CompletionCallback() {
                                                @Override
                                                public void onResult(DJIError djiError) {
                                                    if (djiError != null) {
                                                        sendMessageBack(false, "Set virtual stick mode : " + djiError.getDescription());
                                                    }
                                                }
                                            });
                                        } catch (Exception e) {
                                            sendMessageBack(false, "Error when set mode camera : " + e.getMessage());
                                        }

                                        Thread.sleep(WAIT_1000MS);

                                        sendMessageBack(false, "Landing...");

                                        // Wait to download pictures that the drone is on the ground
                                        while (droneStates.isFlying()) {
                                            land();
                                            Thread.sleep(WAIT_1000MS);
                                        }

                                        Thread.sleep(WAIT_1000MS);
                                        sendMessageBack(false, "Downloading pictures...");
                                        camera.getTwoLastPictures(missionListener, Drone.this);

                                    } catch (Exception e) {
                                        sendMessageBack(false, "Car crash : " + e.getMessage());
                                    }
                                }
                            });
                            threadMission.start();
                            isAuthorized = true;
                        } else {
                            sendMessageBack(false, "Camera is null, Check drone connectivity");
                        }
                    } else {
                        isAuthorized = false;
                        sendMessageBack(false, "Unauthorized. Cannot set mission car crash");
                    }
                    break;
                case FOLLOW:
                    if (currentMission == Mission.NO_MISSION) {
                        currentMission = mission;
                        sendMessageBack(false, "Follow  mission");

                        threadMission = new Thread(new Runnable() {
                            public void run() {
                                try {
                                    if (!droneStates.isFlying()) {
                                        takeoff();
                                    }

                                    Thread.sleep(WAIT_1000MS);
                                    enableVirtualStick();

                                    // Go to min altitude
                                    do {
                                        enableVirtualStick();
                                        changeAltitude(MIN_ALTITUDE);
                                        Thread.sleep(WAIT_100MS);
                                    }
                                    while (droneStates.getAltitudeFromGPS() < MIN_ALTITUDE - ALTITUDE_MARGIN);

                                    Thread.sleep(WAIT_1000MS);

                                    cameraZone.setAltitude(FOLLOW_ALTITUDE);
                                    cameraZone.resume(Drone.this, activity, drone, cameraView, cameraViewWidth, cameraViewHeight, droneStates, missionListener);
                                } catch (Exception e) {
                                    sendMessageBack(false, "Follow mission : " + e.getMessage());
                                    finishMission(false);
                                }
                            }
                        });
                        threadMission.start();
                        isAuthorized = true;
                    } else {
                        isAuthorized = false;
                        sendMessageBack(false, "Cannot set mission Follow, another mission is in progress");
                    }
                    break;
            }
        }
        return isReady && isAuthorized;
    }


    /**
     * Get information about the current mission
     *
     * @return an instance of the object representing the mission
     */
    public Object getMissionInfo() {
        // TODO define mission info
        return new Object();
    }

    /**
     * Allow to register a mission listener to get end mission information
     *
     * @param listener : An instance of MissionListener to handle results of missions
     */
    public void callbackRegisterEndMission(MissionListener listener) {
        missionListener = listener;
    }

    /**
     * Abort the execution of the current mission
     */
    public void abortMission() {
        try {
            if (activity != null) {
                deleteCache(activity);
            }

            if (drone != null) {
                drone.getFlightController().cancelLanding(null);

                drone.getFlightController().cancelTakeoff(null);

                drone.getFlightController().setVirtualStickModeEnabled(false, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            sendMessageBack(false, "Disable virtual stick : " + djiError.getDescription());
                        }
                    }
                });
            }
            currentMission = Mission.NO_MISSION;

            if (threadMission != null) {
                threadMission.interrupt();
            }

            if (cameraZone.readThread != null) {
                cameraZone.readThread.kill();
            }

            camera.killThreadDownload();
            sendMessageBack(true, "Mission aborted");

        } catch (Exception e) {
            sendMessageBack(false, "Error when abort mission : " + e.getMessage());
        }
    }


    /**
     * Get drone's battery level
     *
     * @return a float representing the battery level in percentage between 0 and 100
     */
    public int getBatteryLevel() {
        return droneStates.getBatteryChargeRemaining();
    }

    /**
     * Get information about the connectivity of the drone with the remote controller
     *
     * @return True, if the drone is connected. False, if the drone is not connected
     */
    public boolean isConnected() {
//        return drone.getFlightController() != null && drone.getFlightController().isConnected();
        droneIsConnected = DJISDKManager.getInstance().getProduct() != null && DJISDKManager.getInstance().getProduct().isConnected();
        return droneIsConnected;
    }


    /**
     * Retrieves the downlink quality of the signal between the remote controller and the drone
     *
     * @return a int representing the quality of the signal between the RC and the drone
     */
    public int getDownlinkSignalQualityBetweenDroneAndRC() {
        return downlinkSignalQuality;
    }

    /**
     * Retrieves the uplink quality of the signal between the remote controller and the drone
     *
     * @return a int representing the quality of the signal between the RC and the drone
     */
    public int getUplinkSignalQualityBetweenDroneAndRC() {
        return uplinkSignalQuality;
    }

    /**
     * Retrieves the quality of the video signal
     *
     * @return a float representing the quality of the video signal
     */
    public float getVideoSignalQuality() {
        // TODO get video quality
        return 0.0f;
    }

    /**
     * Get the estimated flight time remaining
     *
     * @return the estimated remaining time of flight in seconds
     */
    public int getEstimatedFlightTimeRemaining() {
        // TODO check value of remaining flight time
        return droneStates.getRemainingFlightTime();
    }

    /**
     * Enable drone's motor and takes off the drone
     */
    private void takeoff() {
        drone.getFlightController().startTakeoff(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError != null) {
                    sendMessageBack(true, djiError.getDescription());
                } else {
                    sendMessageBack(false, "take off success");
                }
            }
        });
    }

    /**
     * Landing the drone
     */
    private void land() {
        drone.getFlightController().startLanding(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError != null) {
                    sendMessageBack(true, djiError.getDescription());
                } else {
                    sendMessageBack(false, "land success");
                }
            }
        });
    }


    public void initLiveView() {
        if (drone != null && drone.getModel() != null && !drone.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {

            if (VideoFeeder.getInstance().getVideoFeeds() != null
                    && VideoFeeder.getInstance().getVideoFeeds().size() > 0) {
                VideoFeeder.getInstance().getVideoFeeds().get(0).setCallback(mReceivedVideoDataCallBack);
//                if (mReceivedVideoDataCallBack != null) {
//                Toast.makeText(activity, "live camera ON", Toast.LENGTH_SHORT).show();
//
//                    VideoFeeder.getInstance().getVideoFeeds().get(0).setCallback(mReceivedVideoDataCallBack);
//                } else {
//                    Toast.makeText(activity, "Receiver is null", Toast.LENGTH_SHORT).show();
//
//                }
            } else {
//                Toast.makeText(activity, "No live camera", Toast.LENGTH_SHORT).show();
            }
        }

    }

    public void unInitLiveView(Context c) {
        isReady = false;
        if (drone != null) {
            if (drone.getCamera() != null) {
                // Reset the callback
                VideoFeeder.getInstance().getVideoFeeds().get(0).setCallback(null);
            }
        }

        if (intent != null) {
            activity.stopService(intent);
        }
        if (cameraZone != null && cameraZone.readThread != null) {
            cameraZone.readThread.kill();
        }

        deleteCache(c);
    }


    public static void deleteCache(Context context) {
        try {
            File dir = context.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception e) {
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }


    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mCodecManager == null) {
//            Toast.makeText(activity, "Surface available width :" + width + " Height :" + height, Toast.LENGTH_SHORT).show();

            mCodecManager = new DJICodecManager(activity, surface, width, height);
            cameraViewHeight = height;
            cameraViewWidth = width;
//            initLiveView();
        }

    }

    public Boolean onSurfaceTextureDestroyed() {
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }

        return false;
    }


    /**
     * Finish the execution of the current mission
     */
    protected void finishMission(Boolean isAborted) {
        try {
            drone.getFlightController().setVirtualStickModeEnabled(false, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        Log.d(TAG, djiError.getDescription());
                    } else {
                        currentMission = Mission.NO_MISSION;
                    }
                }
            });

            if (activity != null) {
                deleteCache(activity);
            }

            drone.getCamera().setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, null);

            if (threadMission != null) {
                threadMission.interrupt();
            }
            if (cameraZone.readThread != null) {
                cameraZone.readThread.kill();
            }

            if (!isAborted) {
                sendMessageBack(true, "Mission finished");
            } else {
                sendMessageBack(true, "Mission canceled");
            }
            currentMission = Mission.NO_MISSION;

        } catch (Exception e) {
            sendMessageBack(false, "Error when finish mission : " + e.getMessage());
        }

    }


    private void enableVirtualStick() {
        try {
            drone.getFlightController().getVirtualStickModeEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
                @Override
                public void onSuccess(Boolean activated) {
                    if (!activated) {
                        //preparations in order to get the Virtual Stick Mode available
//            if(drone.getFlightController().isVirtualStickControlModeAvailable()) {
                        drone.getFlightController().setVirtualStickModeEnabled(true, null);
//                drone.getFlightController().setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
//                    @Override
//                    public void onResult(DJIError djiError) {
//                        if (djiError != null) {
//                                sendMessageBack(false, djiError.getDescription());
//                        }
//                    }
//                });

                        drone.getFlightController().setFlightOrientationMode(FlightOrientationMode.AIRCRAFT_HEADING, null);
                        drone.getFlightController().setTerrainFollowModeEnabled(false, null);
                        drone.getFlightController().setTripodModeEnabled(false, null);


//            if (drone.getFlightController().isVirtualStickControlModeAvailable()) {

                        if (!drone.getFlightController().isVirtualStickAdvancedModeEnabled()) {
                            drone.getFlightController().setVirtualStickAdvancedModeEnabled(true);
                        }

                        //Setting the control modes for Roll, Pitch and Yaw
                        drone.getFlightController().setRollPitchControlMode(RollPitchControlMode.VELOCITY);
                        drone.getFlightController().setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
                        drone.getFlightController().setVerticalControlMode(VerticalControlMode.POSITION);
//                        drone.getFlightController().setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
                    }
                }

                @Override
                public void onFailure(DJIError djiError) {
                    if (djiError != null) {
                        sendMessageBack(true, djiError.getDescription());
                    }
                }
            });


//            }
        } catch (Exception e) {
            sendMessageBack(false, "Mission canceled. Error when enabling virtual stick : " + e.getMessage());
            abortMission();
        }
    }

    private void changeAltitude(final float altitude) {

        enableVirtualStick();
        try {
            FlightControlData move = new FlightControlData(0, 0, 0, altitude);
            drone.getFlightController().sendVirtualStickFlightControlData(move, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        sendMessageBack(false, djiError.getDescription());
                    }
                }
            });
        } catch (Exception e) {
            sendMessageBack(false, "Error when changing altitude : " + e.getMessage());
        }
    }

    public Mission getCurrentMission() {
        return currentMission;
    }

    protected void sendMessageBack(boolean isSuccess, String message) {
        if (missionListener != null) {
            switch (currentMission) {
                case LAND:
                    missionListener.onResultLand(isSuccess, message);
                case LAUNCH:
                    missionListener.onResultLaunch(isSuccess, message);
                case CAR_CRASH:
                    missionListener.onResultCarCrash(isSuccess, message, 0.0f, null);
                case FOLLOW:
                    missionListener.onResultFollow(isSuccess, message);
                default:
                    missionListener.onResultNoMission(isSuccess, message);
            }
        }
    }


}
