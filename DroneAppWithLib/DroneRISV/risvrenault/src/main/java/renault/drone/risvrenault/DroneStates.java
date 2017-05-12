package renault.drone.risvrenault;

import dji.common.flightcontroller.LocationCoordinate3D;

/**
 * Created by Nadine Grossrieder on 10.05.2017.
 *
 * Represents a drone's state.
 */

public class DroneStates {

    /**
     * Retrieves altitude from the GPS location
     *
     * @return A float representing the altitude of the drone in meters
     */
    public float getAltitudeFromGPS(){
        return 0.0f;
    }

    /**
     * Retrieves altitude from the sensor
     *
     * @return A float representing the altitude of the drone in meters
     */
    public float getAltitudeFromSensor(){
        return 0.0f;
    }

    /**
     * Retrieves distance total in meter traveled since the takeoff
     *
     * @return A float representing the distance total
     */
    public float getDistanceTotal(){
        return 0.0f;
    }

    /**
     * Retrieves distance from the starting point
     *
     * @return A float reprenting the ditrance between the starting point and the drone
     */
    public float getDistance(){
        return 0.0f;
    }

    /**
     * Retrieves the current speed of the drone in the Y direction
     *
     * @return A float value of the current speed of the drone in the Y direction.
     */
    public float getVelocityY(){
        return 0.0f;
    }


    /**
     * Retrieves information about the state of drone's motor
     *
     * @return True, if motors are enabled. False, if motors are disabled
     */
    public boolean isMotorEnabled(){
        return true;
    }

    /**
     * Retrieves if the drone is currently flying or not
     *
     * @return True, if the drone is currently flying. False, if the drone is not currently flying
     */
    public boolean isFlying(){
        return true;
    }

    /**
     * Retrieves  GPS location information of the drone
     *
     * @return An instance of LocationCoordinate3D reprenting the current GPS position of the drone
     */
    public LocationCoordinate3D getLocation(){
        return new LocationCoordinate3D(0.0, 0.0, 0.0f);
    }

    /**
     * Retrieves the current speed of the drone in the x direction
     *
     * @return A float value of the current speed of the drone in the x direction.
     */
    public float getVelocityX(){
        return 0.0f;
    }



    /**
     * Retrieves the current speed of the drone in the Z direction
     *
     * @return A float value of the current speed of the drone in the Z direction.
     */
    public float getVelocityZ(){
        return 0.0f;
    }

    /**
     * Retrieves the total time, in secondes, since the drone was powered on
     *
     * @return An int representing the flight time in seconds
     */
    public int getTotalFlightTime(){
        return 0;
    }

    /**
     * Retrieves the number of GPS satellite found by the drone
     *
     * @return An int representing the number of GPS satellite
     */
    public int getNbSatellite(){
        return 0;
    }

    /**
     * Retrieves the drone's current GPS signal quality
     *
     * @return An int representing the drone's current GPS signal quality
     */
    public int getGPSSignal(){
        return 0;
    }




}
