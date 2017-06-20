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
import android.widget.Toast;

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
    static final Float SPEED = 0.2f;

    private static final Float FIRST_ALTITUDE = 10.0f;
    private static final Float SECOND_ALTITUDE = 5.0f;
    private static final Float ALTITUDE_MARGIN = 0.1f;
    private static final float MIN_ALTITUDE = 2.0f;

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
        Toast.makeText(activity, "Drone", Toast.LENGTH_SHORT).show();

        this.activity = activity;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    activity.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED &&
                    activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

//                Toast.makeText(activity, "Authorisation must be granted", Toast.LENGTH_SHORT).show();
            }
            else {
                if (!DJISDKManager.getInstance().hasSDKRegistered()) {
                    mHandler = new Handler(Looper.getMainLooper());
                    DJISDKManager.getInstance().registerApp(activity, mDJISDKManagerCallback);

                }
//                else {
//                    Toast.makeText(activity, "Already registered", Toast.LENGTH_SHORT).show();
//                }
            }

            // The callback for receiving the raw H264 video data for camera live view
            if(mReceivedVideoDataCallBack == null) {
                mReceivedVideoDataCallBack = new VideoFeeder.VideoDataCallback() {
                    @Override
                    public void onReceive(byte[] videoBuffer, int size) {
                        Toast.makeText(activity, "on receive", Toast.LENGTH_SHORT).show();

                        if (mCodecManager != null) {
                            Toast.makeText(activity, "CodecManager != null", Toast.LENGTH_SHORT).show();

                            mCodecManager.sendDataToDecoder(videoBuffer, size);

                        } else {
                            Toast.makeText(activity, "CodecManager is null", Toast.LENGTH_SHORT).show();

                        }
                    }
                };
            }


        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
            Toast.makeText(activity, e.getMessage(), Toast.LENGTH_SHORT).show();
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
            Toast.makeText(activity, "on Register", Toast.LENGTH_SHORT).show();

            if (error == DJISDKError.REGISTRATION_SUCCESS) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, "Register Success", Toast.LENGTH_SHORT).show();
                        DJISDKManager.getInstance().startConnectionToProduct();
                    }
                });


            } else {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, "Register sdk fails, check network is available", Toast.LENGTH_SHORT).show();
                    }
                });

            }
            Log.e("TAG", error.toString());

        }

        //Listens to the connected product changing, including two parts, component changing or product connection changing.
        @Override
        public void onProductChange(BaseProduct oldProduct, BaseProduct newProduct) {
            baseProduct = newProduct;
            if (baseProduct != null) {
                baseProduct.setBaseProductListener(mDJIBaseProductListener);
            }
//            initLiveView();
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
//            Toast.makeText(activity, "Connected : " + isConnected, Toast.LENGTH_SHORT).show();
            droneIsConnected = isConnected;
        }

    };

    private BaseComponent.ComponentListener mDJIComponentListener = new BaseComponent.ComponentListener() {

        @Override
        public void onConnectivityChange(boolean isConnected) {
//            Toast.makeText(activity, "Component Connected : " + isConnected, Toast.LENGTH_SHORT).show();
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
            Toast.makeText(activity, "Already registered", Toast.LENGTH_SHORT).show();
        }
        else {
            if (DJISDKManager.getInstance().hasSDKRegistered() && activity != null && cameraView != null && baseProduct != null && cameraZone != null) {
                initLiveView();
                Toast.makeText(this.activity, "Aircraft : " + baseProduct.getModel().getDisplayName(), Toast.LENGTH_SHORT).show();

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

//            Toast.makeText(activity, "Init camera", Toast.LENGTH_SHORT).show();


//                // The callback for receiving the raw H264 video data for camera live view
//                mReceivedVideoDataCallBack = new VideoFeeder.VideoDataCallback() {
//                    @Override
//                    public void onReceive(byte[] videoBuffer, int size) {
//                        if (mCodecManager != null) {
//                            Toast.makeText(activity, "CodecManager != null", Toast.LENGTH_SHORT).show();
//
//                            mCodecManager.sendDataToDecoder(videoBuffer, size);
//
//                        }
//                    }
//                };

                isReady = true;
            }
            else if (activity != null){
                if(!DJISDKManager.getInstance().hasSDKRegistered()) {
                    Toast.makeText(this.activity, "DJI registration has failed", Toast.LENGTH_SHORT).show();
                }
                if(cameraView == null ){
                    Toast.makeText(this.activity, "TextureView is null", Toast.LENGTH_SHORT).show();
                }
                if(baseProduct == null){
                    Toast.makeText(this.activity, "Drone is null", Toast.LENGTH_SHORT).show();
                }
                if( cameraZone == null){
                    Toast.makeText(this.activity, "Instance of FollowQRCode is null", Toast.LENGTH_SHORT).show();
                }
            }
        }
        return isReady;
    }


    public void initLiveVideo(){
        mReceivedVideoDataCallBack = new VideoFeeder.VideoDataCallback() {

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };
    }

    public void resumeLiveVideo(TextureView.SurfaceTextureListener listener){
        DJISDKManager.getInstance().startConnectionToProduct();

        if (drone == null || !drone.isConnected()) {
            Toast.makeText(activity, "disconnected", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(activity, "connected", Toast.LENGTH_SHORT).show();

            if (null != cameraView) {
                cameraView.setSurfaceTextureListener(listener);
            }
            if (!drone.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                if (VideoFeeder.getInstance().getVideoFeeds() != null
                        && VideoFeeder.getInstance().getVideoFeeds().size() > 0) {
                    Toast.makeText(activity, "set callback", Toast.LENGTH_SHORT).show();

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
//        Toast.makeText(activity, "Drone state : " + (droneStates != null), Toast.LENGTH_SHORT).show();
        return droneStates;
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
     *
     * @param mission :
     * @return true if operation is authorized, false if no right to define this mission
     */
    public boolean setMission(final Mission mission) {
        boolean isAuthorized = false;

        if(isReady) {
            // TODO : check if mission is possible and start mission

//            droneStates = new DroneStates(drone);

            switch (mission) {
                case NO_MISSION:
                    abortMission();
                    break;
                case LAUNCH:
                    if (currentMission == Mission.NO_MISSION && !droneStates.isFlying()) {
                        Toast.makeText(activity, "Launch", Toast.LENGTH_SHORT).show();
                        currentMission = mission;
                        isAuthorized = true;
                        takeoff();
                    }
                    break;
                case LAND:
                    if (currentMission != Mission.LAUNCH && droneStates.isFlying()) {
                        Toast.makeText(activity, "Land", Toast.LENGTH_SHORT).show();
                        currentMission = mission;
                        isAuthorized = true;
                        land();
                    }
                    break;
                case CAR_CRASH:
                    if (currentMission != Mission.LAND) {
                        currentMission = mission;
                        Toast.makeText(activity, "Car Crash", Toast.LENGTH_SHORT).show();

                        if (camera != null) {


                            threadMission = new Thread(new Runnable() {
                                public void run() {
                                    // TODO
                                    try {
                                        if (!droneStates.isFlying()) {
                                            takeoff();
                                        }

                                        Thread.sleep(WAIT_1000MS);

                                        while (droneStates.getAltitudeFromGPS() < MIN_ALTITUDE - ALTITUDE_MARGIN || droneStates.getAltitudeFromGPS() > MIN_ALTITUDE + ALTITUDE_MARGIN) {
                                            changeAltitude(MIN_ALTITUDE);
                                            Thread.sleep(WAIT_100MS);
                                        }


                                        Thread threadFollow = new Thread(new Runnable() {
                                            public void run() {
                                                cameraZone.setAltitude(FIRST_ALTITUDE);
                                                cameraZone.resume(activity, drone, cameraView, cameraViewWidth, cameraViewHeight, droneStates);
                                            }
                                        });
                                        threadFollow.start();
                                        Toast.makeText(activity, "ok", Toast.LENGTH_SHORT).show();
                                        while (droneStates.getAltitudeFromGPS() < FIRST_ALTITUDE - ALTITUDE_MARGIN || droneStates.getAltitudeFromGPS() > FIRST_ALTITUDE + ALTITUDE_MARGIN) {
//                                        changeAltitude(FIRST_ALTITUDE);
                                            Thread.sleep(WAIT_100MS);
                                        }
                                        threadFollow.interrupt();

                                        camera.takePicture(activity);

                                        Thread.sleep(WAIT_2000MS);

                                        Thread threadFollow2 = new Thread(new Runnable() {
                                            public void run() {
                                                cameraZone.setAltitude(SECOND_ALTITUDE);
                                                cameraZone.resume(activity, drone, cameraView, cameraViewWidth, cameraViewHeight, droneStates);
                                            }
                                        });
                                        threadFollow2.start();

                                        while (droneStates.getAltitudeFromGPS() < SECOND_ALTITUDE - ALTITUDE_MARGIN || droneStates.getAltitudeFromGPS() > SECOND_ALTITUDE + ALTITUDE_MARGIN) {
                                            changeAltitude(SECOND_ALTITUDE);
                                            Thread.sleep(WAIT_100MS);
                                        }

                                        camera.takePicture(activity);

                                        Thread.sleep(WAIT_2000MS);


                                        // TODO : remove the comment
//                                    land();

                                        while (droneStates.isFlying()) {
                                            Thread.sleep(WAIT_100MS);
                                        }
                                        camera.getTwoLastPictures(activity, missionListener, Drone.this);

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
                                        } catch (Exception e) {
                                            Log.e(TAG, e.getMessage());
                                        }


                                    } catch (Exception e) {
                                        if (missionListener != null) {
                                            missionListener.onResultCarCrash(false, e.getMessage(), 0, null);
                                        }
                                    }
                                }
                            });
                            threadMission.start();
                            isAuthorized = true;
                        } else {
                            if (missionListener != null) {
                                missionListener.onResultCarCrash(false, "Camera is null, Check drone connectivity", 0, null);
                            }
                        }
//                    carCrash();
                    } else {
                        isAuthorized = false;
                        if (missionListener != null) {
                            missionListener.onResultCarCrash(false, "Cannot set mission car crash", 0, null);
                        }
                    }
                    break;
                case FOLLOW:
                    if (currentMission != Mission.LAND) {
                        currentMission = mission;
                        Toast.makeText(activity, "Follow :" + cameraViewWidth + " Height :" + cameraViewHeight, Toast.LENGTH_SHORT).show();

                        threadMission = new Thread(new Runnable() {
                            public void run() {
                                cameraZone.resume(activity, drone, cameraView, cameraViewWidth, cameraViewHeight, droneStates);
                            }
                        });
                        threadMission.start();
                        isAuthorized = true;
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
            drone.getFlightController().setVirtualStickModeEnabled(false, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        Log.d(TAG, djiError.getDescription());
                    } else {
                        Log.d(TAG, "Emergency Stop succeeded");
                        currentMission = Mission.NO_MISSION;
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        if(threadMission != null){
            threadMission.interrupt();
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
                    if (missionListener != null) {
                        missionListener.onResultLaunch(true, djiError.getDescription());
                    }
                } else {
                    if (missionListener != null) {
                        missionListener.onResultLaunch(false, null);
                    }
                }

                currentMission = Mission.NO_MISSION;
            }
        });
    }

    /**
     * Landing the drone
     */
    private void land() {
        // TODO handle error and check if landing is possible
        drone.getFlightController().startLanding(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError != null) {
                    if (missionListener != null) {
                        missionListener.onResultLaunch(true, djiError.getDescription());
                    }
                } else {
                    if (missionListener != null) {
                        missionListener.onResultLaunch(false, null);
                    }
                }
                currentMission = Mission.NO_MISSION;
            }
        });
    }


    /**
     * Start the mode "Car crash"
     */
    private void carCrash() {
        //TODO car crash
        currentMission = Mission.NO_MISSION;
    }

    /**
     * Start the mode "Follow"
     */
    private void follow() {
        //TODO follow
        currentMission = Mission.NO_MISSION;
    }

    public void initLiveView() {
        if (drone != null && drone.getModel() != null && !drone.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {

            if (VideoFeeder.getInstance().getVideoFeeds() != null
                    && VideoFeeder.getInstance().getVideoFeeds().size() > 0) {
                VideoFeeder.getInstance().getVideoFeeds().get(0).setCallback(mReceivedVideoDataCallBack);
//                if (mReceivedVideoDataCallBack != null) {
                    Toast.makeText(activity, "live camera ON", Toast.LENGTH_SHORT).show();
//
//                    VideoFeeder.getInstance().getVideoFeeds().get(0).setCallback(mReceivedVideoDataCallBack);
//                } else {
//                    Toast.makeText(activity, "Receiver is null", Toast.LENGTH_SHORT).show();
//
//                }
            } else {
                Toast.makeText(activity, "No live camera", Toast.LENGTH_SHORT).show();
            }
        }

    }

    public void unInitLiveView() {
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

    }

    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mCodecManager == null) {
            Toast.makeText(activity, "Surface available width :" + width + " Height :" + height, Toast.LENGTH_SHORT).show();

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
    protected void finishMission() {
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

            drone.getCamera().setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, null);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        if(threadMission != null){
            threadMission.interrupt();
        }
    }




    private void enableVirtualStick() {
        try {
            //preparations in order to get the Virtual Stick Mode available
//            if(drone.getFlightController().isVirtualStickControlModeAvailable()) {
                drone.getFlightController().setVirtualStickModeEnabled(true, null);
//                drone.getFlightController().setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
//                    @Override
//                    public void onResult(DJIError djiError) {
//                        if (djiError != null) {
//                            if (missionListener != null) {
//                                missionListener.onResultCarCrash(false, djiError.getDescription(), null);
//                            }
//                        }
//                    }
//                });

                drone.getFlightController().setFlightOrientationMode(FlightOrientationMode.AIRCRAFT_HEADING, null);
                drone.getFlightController().setTerrainFollowModeEnabled(false, null);
                drone.getFlightController().setTripodModeEnabled(false, null);


                if (drone.getFlightController().isVirtualStickControlModeAvailable()) {

                    if (!drone.getFlightController().isVirtualStickAdvancedModeEnabled()) {
                        drone.getFlightController().setVirtualStickAdvancedModeEnabled(true);


                    }
                    //Setting the control modes for Roll, Pitch and Yaw
                    drone.getFlightController().setRollPitchControlMode(RollPitchControlMode.VELOCITY);
//                        ((Aircraft) currentDrone).getFlightController().setYawControlMode(YawControlMode.ANGLE);
                    drone.getFlightController().setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
//                        ((Aircraft) currentDrone).getFlightController().setVerticalControlMode(VerticalControlMode.POSITION);
                    drone.getFlightController().setVerticalControlMode(VerticalControlMode.POSITION);
                    drone.getFlightController().setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);

                }
//            }
//            else{
//                throw new Exception("Virtual stick mode not available");
//            }
        } catch (Exception e) {
            if (missionListener != null) {
                missionListener.onResultCarCrash(false, e.getMessage(), 0, null);
            }        }
    }

    private void changeAltitude(final float altitude) {
//        if(!drone.getFlightController().isVirtualStickAdvancedModeEnabled()) {
            enableVirtualStick();
//        }
        try {
            FlightControlData move = new FlightControlData(0, 0, 0, altitude);
            drone.getFlightController().sendVirtualStickFlightControlData(move, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                            if (missionListener != null) {
                                missionListener.onResultCarCrash(false, djiError.getDescription(), 0, null);
                            }
                    }
                }
            });
        } catch (Exception e) {
            if (missionListener != null) {
                missionListener.onResultCarCrash(false,  e.getMessage(), 0, null);
            }
        }
    }


}
