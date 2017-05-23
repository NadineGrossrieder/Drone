package renault.drone.risvrenault;

/**
 * Created by Nadine Grossrieder on 10.05.2017.
 *
 * Represents the orientation of a drone or a gimbal (camera).
 * Represented by a pitch, roll and yaw value
 */

public class Orientation {

    private final float pitch;
    private final float roll;
    private final float yaw;

    Orientation(float pitch, float roll, float yaw){
        this.pitch = pitch;
        this.roll = roll;
        this.yaw = yaw;
    }

    /**
     * Retrieves pitch value in m/s (-15m/s to 15m/s)
     *
     * @return a float representing the pitch value in m/s
     */
    public float getPitch(){
        return pitch;
    }

    /**
     * Retrieves roll value in m/s (-15m/s to 15m/s)
     *
     * @return a float representing the roll value in m/s
     */
    public float getRoll(){
        return roll;
    }

    /**
     * Retrieves yaw value in degrees (-180° to 180°)
     *
     * @return a float representing the yaw value in degrees
     */
    public float getYaw(){
        return yaw;
    }
}
