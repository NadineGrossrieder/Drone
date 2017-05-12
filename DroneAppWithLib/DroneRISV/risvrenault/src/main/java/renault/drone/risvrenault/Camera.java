package renault.drone.risvrenault;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;

/**
 * Created by Nadine Grossrieder on 10.05.2017.
 *
 * Represents the drone's camera and its actions
 */

public class Camera {

    /**
     * Take an picture with the drone's camera
     *
     * @return A Bitmap representing the last picture taken
     */
    public Bitmap takePicture(){
        return Bitmap.createBitmap(0, 0, Bitmap.Config.RGB_565);
    }

    /**
     * Retrieves information about the orientation (pitch, roll, yaw) of the gimbal (camera)
     *
     * @return An instance of Orientation representting the pitch, roll and yaw of the gimbal
     */
    public Orientation getGimbalOrientation(){
        return new Orientation();
    }

}
