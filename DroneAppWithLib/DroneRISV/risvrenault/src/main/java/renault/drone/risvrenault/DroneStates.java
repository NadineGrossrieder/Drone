package renault.drone.risvrenault;

import android.support.annotation.NonNull;

import dji.common.battery.BatteryState;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.GPSSignalLevel;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.remotecontroller.GPSData;
import dji.common.remotecontroller.HardwareState;
import dji.sdk.products.Aircraft;

/**
 * Created by Nadine Grossrieder on 10.05.2017.
 * <p>
 * Represents a drone's state.
 */

public class DroneStates {


    private Aircraft drone;
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
    private float sensorAltitude;
    private float gpsAltitude;

    private int batteryChargeRemaining;
    private int remainingFlightTime;

     DroneStates(Aircraft drone) {
        this.drone = drone;

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
     * Retrieves  GPS location information of the drone
     *
     * @return An instance of LocationCoordinate3D reprenting the current GPS position of the drone
     */
    public LocationCoordinate3D getLocation() {
        return location;
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
     * Retrieves the total time, in secondes, since the drone was powered on
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


}
