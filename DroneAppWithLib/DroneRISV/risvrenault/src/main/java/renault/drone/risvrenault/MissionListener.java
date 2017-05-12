package renault.drone.risvrenault;

import android.graphics.Bitmap;

/**
 * Created by Nadine Grossrieder on 11.05.2017.
 *
 * Callback interface for missions
 */

public interface MissionListener{

    void onResultFollow(Boolean isSuccess);

    void onResultCarCrash(Boolean isSuccess, Bitmap[] photos);

    void onResultLand(Boolean isSuccess);

    void onResultLaunch(Boolean isSuccess);
}