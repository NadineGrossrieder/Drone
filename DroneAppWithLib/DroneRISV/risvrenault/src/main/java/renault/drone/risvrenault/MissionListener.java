package renault.drone.risvrenault;

import android.graphics.Bitmap;

/**
 * Created by Nadine Grossrieder on 11.05.2017.
 *
 * Callback interface for missions
 */

public interface MissionListener{

    void onResultFollow(Boolean isSuccess, String message);

    void onResultCarCrash(Boolean isSuccess, String message, float percentDownload, Bitmap[] photos);

    void onResultLand(Boolean isSuccess, String message);

    void onResultLaunch(Boolean isSuccess, String message);
}