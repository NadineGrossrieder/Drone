package renault.drone.risvrenault;

import android.location.Location;
import android.view.TextureView;

import dji.common.flightcontroller.LocationCoordinate3D;
import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKManager;

/**
 * Created by Nadine Grossrieder on 10.05.2017.
 *
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

    static final Float SPEED = 0.2f;

    private Mission currentMission = Mission.NO_MISSION;
    private MissionListener missionListener;

    private DroneStates droneStates;

    /**
     * Init the drone class
     *
     * @param cameraView : TextureView where the live camera is displayed
     * @param drone : instance of the BaseProduct corresponding to the drone
     * @param cameraViewWidth : width of the live camera
     * @param cameraViewHeight : height of the live camera
     */
    public void init(TextureView cameraView, BaseProduct drone, int cameraViewWidth, int cameraViewHeight) {

    }

    /**
     * Retrieves a BaseProduct corresponding to the drone connected
     *
     * @return BaseProduct when a drone is connected, null if drone not connected
     */
    public static BaseProduct getDrone() {
        return DJISDKManager.getInstance().getProduct();
    }

    /**
     * Retrieves the LocationCoordinate3D to locate the drone with its GPS location
     *
     * @return LocationCoordinate3D that contains latitude, longitude and altitude of the drone
     */
    public LocationCoordinate3D getGPSPositionDrone() {
        return new LocationCoordinate3D(0.0, 0.0, 0.0f);
    }

    /**
     * Retrieves the LocationCoordinate3D to locate the remote controller with its GPS location
     *
     * @return Location that contains latitude, longitude and altitude of the remote controller
     */
    public Location getGPSPositionRC() {
        return new Location("");

    }

    /**
     * Retrieves orientation information (pitch, roll, yaw) for the drone
     *
     * @return an instance of Orientation that contains current yaw, pitch and roll values
     */
    public Orientation getDroneOrientation() {
        return new Orientation();
    }

    /**
     * Get the drone's state
     *
     * @return An instance of DroneStates
     */
    public DroneStates getDroneStates(){
        return droneStates;
    }

    /**
     * Retrieves the current mission defined
     *
     * @return the current mission
     */
    public Mission getMission(){
        return currentMission;
    }

    /**
     * Define a new mission to execute
     *
     * @param mission :
     *
     * @return true if operation success, false if no right to define this mission
     */
    public boolean setMission(Mission mission){
        currentMission = mission;
        return true;
    }

    /**
     * Get informations about the current mission
     *
     * @return an instance of the object representing the mission
     */
    public Object getMissionInfo(){
        return new Object();
    }

    /**
     * Allow to register a mission listener to get end mission informations
     *
     * @param listener : An instance of MissionListener to handle results of missions
     */
    public void callbackRegisterEndMission(MissionListener listener){
        missionListener = listener;
    }

    /**
     * Abort the execution of the current mission
     */
    public void abortMission(){

    }

    /**
     * Get drone's battery level
     *
     * @return a float representing the battery level in percentage between 0 and 100
     */
    public int getBatteryLevel(){
        return 0;
    }

    /**
     * Get information about the connectivity of the drone with the remote controller
     *
     * @return True, if the drone is connected. False, if the drone is not connected
     */
    public boolean isConnected(){
        return true;
    }


    /**
     * Retrieves the quality of the signal between the remote controller and the drone
     *
     * @return a float representing the quality of the signal between the RC and the drone
     */
    public float getSignalQualityBetweenDroneAndRC(){
        return 0.0f;
    }

    /**
     * Retrieves the quality of the video signal
     *
     * @return a float representing the quality of the video signal
     */
    public float getVideoSignalQuality(){
        return 0.0f;
    }

    /**
     * Get the estimated flight time remaining
     *
     * @return the estimated remaining time of flight in seconds
     */
    public int getEstimatedFlightTimeRemaining(){
        return 0;
    }

    /**
     * Enable drone's motor and takes off the drone
     *
     * @return true, if operation success. False, if no right to do this action now
     */
    public boolean takeoff(){
        return true;
    }

    /**
     * Landing the drone
     *
     * @return true, if operation success. False, if no right to do this action now
     */
    public boolean land(){
        return true;
    }



}
