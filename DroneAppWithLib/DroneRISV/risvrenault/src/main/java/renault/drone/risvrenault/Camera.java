package renault.drone.risvrenault;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;

import java.util.List;

import dji.common.camera.SDCardState;
import dji.common.camera.SettingsDefinitions;
import dji.common.camera.WhiteBalance;
import dji.common.error.DJIError;
import dji.common.gimbal.GimbalMode;
import dji.common.gimbal.GimbalState;
import dji.common.util.CommonCallbacks;
import dji.sdk.camera.MediaFile;
import dji.sdk.camera.MediaManager;
import dji.sdk.products.Aircraft;

/**
 * Created by Nadine Grossrieder on 10.05.2017.
 * <p>
 * Represents the drone's camera and its actions
 */

public class Camera {


    private Aircraft drone;
    private GimbalMode gimbalMode;
    private Orientation orientation;

    private int isoValue;
    private float shutterSpeedValue;
    private String whiteBalanceValue;
    private String fileFormat;
    private int availableRecordingTime;
    private MediaFile media;
    private MediaManager mediaManager;

    protected Camera(Aircraft drone) {
        this.drone = drone;

        drone.getGimbal().setStateCallback(new GimbalState.Callback() {
            @Override
            public void onUpdate(@NonNull GimbalState gimbalState) {
                gimbalMode = gimbalState.getMode();
                orientation = new Orientation(
                        gimbalState.getAttitudeInDegrees().getPitch(),
                        gimbalState.getAttitudeInDegrees().getRoll(),
                        gimbalState.getAttitudeInDegrees().getYaw()
                );


            }
        });


        drone.getCamera().getISO(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.ISO>() {
            @Override
            public void onSuccess(SettingsDefinitions.ISO iso) {
                isoValue = iso.value();
            }

            @Override
            public void onFailure(DJIError djiError) {

            }
        });

        drone.getCamera().getShutterSpeed(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.ShutterSpeed>() {
            @Override
            public void onSuccess(SettingsDefinitions.ShutterSpeed shutterSpeed) {
                shutterSpeedValue = shutterSpeed.value();
            }

            @Override
            public void onFailure(DJIError djiError) {

            }
        });

        drone.getCamera().getWhiteBalance(new CommonCallbacks.CompletionCallbackWith<WhiteBalance>() {
            @Override
            public void onSuccess(WhiteBalance whiteBalance) {
                whiteBalanceValue = whiteBalance.getWhiteBalancePreset().name();
            }

            @Override
            public void onFailure(DJIError djiError) {

            }
        });

        drone.getCamera().getVideoFileFormat(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.VideoFileFormat>() {
            @Override
            public void onSuccess(SettingsDefinitions.VideoFileFormat videoFileFormat) {
                fileFormat = videoFileFormat.name();
            }

            @Override
            public void onFailure(DJIError djiError) {

            }
        });

        drone.getCamera().setSDCardStateCallBack(new SDCardState.Callback() {
            @Override
            public void onUpdate(@NonNull SDCardState sdCardState) {
                availableRecordingTime = sdCardState.getAvailableRecordingTimeInSeconds();
            }
        });
    }

    /**
     * Take an picture with the drone's camera
     *
     * @return A Bitmap representing the last picture taken
     */
    public Bitmap takePicture() {
        //TODO handle state
        drone.getCamera().setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError != null) {
//                    scanningQRCodeState += djiError.getDescription() + "\n\r";
                } else {
//                    scanningQRCodeState += "Mode photo changed with success \n\r";
                    drone.getCamera().startShootPhoto(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
//                                scanningQRCodeState += djiError.getDescription() + "\n\r";
                            } else {
//                                scanningQRCodeState += "Photo taken \n\r";
                            }
                        }
                    });

                }
            }
        });


        drone.getCamera()
                .setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD,
                        new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (null == djiError){
                                    drone.getCamera().getMediaManager().fetchMediaList(new MediaManager.DownloadListener<List<MediaFile>>() {
                                        String str;

                                        @Override
                                        public void onStart() {
                                        }

                                        @Override
                                        public void onRateUpdate(long total, long current, long persize) {
                                        }

                                        @Override
                                        public void onProgress(long l, long l1) {

                                        }

                                        @Override
                                        public void onSuccess(List<MediaFile> djiMedias) {
                                            if (djiMedias != null) {
                                                if (!djiMedias.isEmpty()) {
                                                    media = djiMedias.get(0);
                                                    str = "Total Media files:"
                                                            + djiMedias.size()
                                                            + "\n"
                                                            + "Media 1: "
                                                            + djiMedias.get(0).getFileName();
                                                } else {
                                                    str = "No Media in SD Card";
                                                }
                                            }
                                        }

                                        @Override
                                        public void onFailure(DJIError djiError) {
                                        }
                                    });                                }
                            }
                        });
        if (mediaManager == null) {
            mediaManager = drone.getCamera().getMediaManager();
        }


        // TODO handle error


        return Bitmap.createBitmap(0, 0, Bitmap.Config.RGB_565);
    }

    /**
     * Retrieves information about the orientation (pitch, roll, yaw) of the gimbal (camera)
     *
     * @return An instance of Orientation representting the pitch, roll and yaw of the gimbal
     */
    public Orientation getGimbalOrientation() {
        return orientation;
    }

    public int getIsoValue() {
        return isoValue;
    }

    public float getShutterSpeedValue() {
        return shutterSpeedValue;
    }

    public String getWhiteBalanceValue() {
        return whiteBalanceValue;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public int getAvailableRecordingTime() {
        return availableRecordingTime;
    }
}
