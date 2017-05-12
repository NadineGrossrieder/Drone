package renault.drone.risvrenault;

/**
 * Created by Nadine Grossrieder on 10.05.2017.
 *
 * Represents the orientation of a drone or a gimbal (camera).
 * Represented by a pitch, roll and yaw value
 */

public class Orientation {

    /**
     * Retrieves pitch value in m/s (-15m/s to 15m/s)
     *
     * @return a float representing the pitch value in m/s
     */
    public float getPitch(){
        return 0.0f;
    }

    /**
     * Retrieves roll value in m/s (-15m/s to 15m/s)
     *
     * @return a float representing the roll value in m/s
     */
    public float getRoll(){
        return 0.0f;
    }

    /**
     * Retrieves yaw value in degrees (-180° to 180°)
     *
     * @return a float representing the yaw value in degrees
     */
    public float getYaw(){
        return 0.0f;
    }
}
