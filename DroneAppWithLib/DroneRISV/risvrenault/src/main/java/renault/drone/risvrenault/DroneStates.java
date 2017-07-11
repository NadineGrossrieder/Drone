package renault.drone.risvrenault;

import android.location.Location;
import android.support.annotation.NonNull;

import dji.common.battery.BatteryState;
import dji.common.camera.ExposureSettings;
import dji.common.camera.SettingsDefinitions.ISO;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.GPSSignalLevel;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.sdk.products.Aircraft;

/**
 * Created by Nadine Grossrieder on 10.05.2017.
 * <p>
 * Represents a drone's state.
 */

public class DroneStates {

    private static final double EARTH_RADIUS = 6373;
    private int nbSatellite;
    private GPSSignalLevel signalStrenght;
    private Orientation orientation;
    private float velocityX;
    private float velocityY;
    private float velocityZ;
    private int totalFlightTime;
    private boolean isFlying;
    private boolean areMotorsOn;
    private LocationCoordinate3D location;
    private LocationCoordinate3D homeLocation;
    private float sensorAltitude;
    private float gpsAltitude;
    private int isoValue;

    private int batteryChargeRemaining;
    private int remainingFlightTime;
    private double distanceBetweenRCAndDrone;

    DroneStates(final Aircraft drone) {
        drone.getCamera().setExposureSettingsCallback(new ExposureSettings.Callback() {
            @Override
            public void onUpdate(@NonNull ExposureSettings exposureSettings) {
                if(exposureSettings.getISO() != ISO.AUTO && exposureSettings.getISO() != ISO.UNKNOWN){
                    isoValue =Integer.parseInt(exposureSettings.getISO().name().substring(4));
                }
                else{
                    isoValue = 0;
                }
            }
        });

        drone.getFlightController().setStateCallback(new FlightControllerState.Callback() {
            @Override
            public void onUpdate(@NonNull FlightControllerState flightControllerState) {
                orientation = new Orientation(
                        (float) flightControllerState.getAttitude().pitch,
                        (float) flightControllerState.getAttitude().yaw,
                        (float) flightControllerState.getAttitude().roll
                );

                nbSatellite = flightControllerState.getSatelliteCount();
                signalStrenght = flightControllerState.getGPSSignalLevel();

                velocityX = flightControllerState.getVelocityX();
                velocityY = flightControllerState.getVelocityY();
                velocityZ = -flightControllerState.getVelocityZ();

                totalFlightTime = flightControllerState.getFlightTimeInSeconds()/10;

                isFlying = flightControllerState.isFlying();
                areMotorsOn = flightControllerState.areMotorsOn();

                location = flightControllerState.getAircraftLocation();
                sensorAltitude = flightControllerState.getUltrasonicHeightInMeters();
                gpsAltitude = flightControllerState.getAircraftLocation().getAltitude();

                if(homeLocation == null){
                    homeLocation = location;
                }

                computeDistance();

                remainingFlightTime = flightControllerState.getGoHomeAssessment().getRemainingFlightTime();
            }
        });

        drone.getBattery().setStateCallback(new BatteryState.Callback() {
            @Override
            public void onUpdate(BatteryState batteryState) {
                batteryChargeRemaining = batteryState.getChargeRemainingInPercent();
            }
        });
    }

    private void computeDistance() {
        if(PhoneGPSLocation.mCurrentLocation != null && location != null) {
            Location rcLocation = PhoneGPSLocation.mCurrentLocation;

            double dLon = Math.toRadians(rcLocation.getLongitude() - location.getLongitude());
            double dLat = Math.toRadians(rcLocation.getLatitude() - location.getLatitude());

            double a = Math.pow((Math.sin(dLat / 2)), 2) + Math.cos(Math.toRadians(location.getLatitude())) * Math.cos(Math.toRadians(rcLocation.getLatitude())) * (Math.pow(Math.sin(dLon / 2), 2));
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            double d = EARTH_RADIUS * c * 1000;


            double height = getAltitudeFromGPS();

            distanceBetweenRCAndDrone = Math.sqrt(Math.pow(d, 2) + Math.pow(height, 2));

//            distanceBetweenRCAndDrone = d;


        }
    }


    /**
     * Retrieves distance between the drone and the remote controller in meters
     *
     * @return A float representing the distance in meters
     */
    public float getDistanceBetweenDroneAndRC() {
        return (float)distanceBetweenRCAndDrone;
    }


    /**
     * Retrieves altitude from the GPS location
     *
     * @return A float representing the altitude of the drone in meters
     */
    public float getAltitudeFromGPS() {
        return gpsAltitude;
    }

    /**
     * Retrieves altitude from the sensor
     *
     * @return A float representing the altitude of the drone in meters
     */
    public float getAltitudeFromSensor() {
        return sensorAltitude;
    }


    /**
     * Retrieves information about the state of drone's motor
     *
     * @return True, if motors are enabled. False, if motors are disabled
     */
    public boolean isMotorEnabled() {
        return areMotorsOn;
    }

    /**
     * Retrieves if the drone is currently flying or not
     *
     * @return True, if the drone is currently flying. False, if the drone is not currently flying
     */
    public boolean isFlying() {
        return isFlying;
    }

    /**
     * Retrieves Home location information (first location of the drone when activated)
     *
     * @return An instance of LocationCoordinate3D that represent the home location
     */
    public LocationCoordinate3D getHomeLocation() {
        return homeLocation;
    }

    /**
     * Retrieves  GPS location information of the drone
     *
     * @return An instance of LocationCoordinate3D that represent the current GPS position of the drone
     */
    public LocationCoordinate3D getLocation() {
        return location;
    }


    /**
     * Retrieves the current horizontal speed of the drone
     *
     * @return A float value of the current horizontal speed of the drone
     */
    public float getHorizontalSpeed() {
        return (float) Math.sqrt(Math.pow(velocityX, 2) + Math.pow(velocityY, 2));
    }

    /**
     * Retrieves the current vertical speed of the drone
     *
     * @return A float value of the current vertical speed of the drone
     */
    public float getVerticalSpeed() {
        return velocityZ;
    }

    /**
     * Retrieves the current speed of the drone in the x direction
     *
     * @return A float value of the current speed of the drone in the x direction.
     */
    public float getVelocityX() {
        return velocityX;
    }

    /**
     * Retrieves the current speed of the drone in the Y direction
     *
     * @return A float value of the current speed of the drone in the Y direction.
     */
    public float getVelocityY() {
        return velocityY;
    }


    /**
     * Retrieves the current speed of the drone in the Z direction
     *
     * @return A float value of the current speed of the drone in the Z direction.
     */
    public float getVelocityZ() {
        return velocityZ;
    }

    /**
     * Retrieves the total time, in seconds, since the drone was powered on
     *
     * @return An int representing the flight time in seconds
     */
    public int getTotalFlightTime() {
        return totalFlightTime;
    }

    /**
     * Retrieves the number of GPS satellite found by the drone
     *
     * @return An int representing the number of GPS satellite
     */
    public int getNbSatellite() {
        return nbSatellite;
    }

    /**
     * Retrieves the drone's current GPS signal quality
     *
     * @return An int representing the drone's current GPS signal quality
     */
    public int getGPSSignal() {
        return signalStrenght.value();
    }


    Orientation getOrientation() {
        return orientation;
    }

    int getBatteryChargeRemaining() {
        return batteryChargeRemaining;
    }

    int getRemainingFlightTime() {
        return remainingFlightTime;
    }

    /**
     * Retrieves iso value
     *
     * @return A integer representing the iso value
     */
    public int getIso() {
        return isoValue;
    }
}
