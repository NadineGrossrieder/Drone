package renault.drone.risvrenault;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.widget.Toast;

import java.io.File;
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
import dji.sdk.sdkmanager.DJISDKManager;

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
    public Bitmap takePicture(final Context activity) {
        //TODO handle state
        drone.getCamera().setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError != null) {
                    Toast.makeText(activity, djiError.getDescription(), Toast.LENGTH_SHORT).show();

                } else {
                    drone.getCamera().startShootPhoto(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                Toast.makeText(activity, djiError.getDescription(), Toast.LENGTH_SHORT).show();

                            } else {

                                Toast.makeText(activity, "Picture taken", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });

//        if (mediaManager == null) {
//            mediaManager = drone.getCamera().getMediaManager();
//        }


        // TODO handle error


        return null;
    }

    public Bitmap getTwoLastPictures(final Context activity) {
        try {
            Thread th = new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.getMessage();
                    }

                    if (!drone.getCamera().isMediaDownloadModeSupported()) {
                        Toast.makeText(activity, "Not supported", Toast.LENGTH_SHORT).show();
                    } else {
                        drone.getCamera().setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD, new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (null == djiError) {
                                    try {
                                        if (drone != null && drone.getCamera() != null && drone.getCamera().getMediaManager() != null) {
                                            drone.getCamera().getMediaManager().fetchMediaList(new MediaManager.DownloadListener<List<MediaFile>>() {
                                                String str;

                                                @Override
                                                public void onStart() {
                                                    Toast.makeText(activity, "start", Toast.LENGTH_SHORT).show();
                                                }

                                                @Override
                                                public void onRateUpdate(long total, long current, long persize) {

                                                }

                                                @Override
                                                public void onProgress(long l, long l1) {

                                                }

                                                @Override
                                                public void onSuccess(List<MediaFile> djiMedias) {
                                                    Toast.makeText(activity, "Success", Toast.LENGTH_SHORT).show();

                                                    try {
                                                        if (djiMedias != null && !djiMedias.isEmpty()) {
                                                            Toast.makeText(activity, "not empty", Toast.LENGTH_SHORT).show();

                                                            media = djiMedias.get(0);

                                                            Toast.makeText(activity, media.getFileName(), Toast.LENGTH_SHORT).show();

                                                            File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
//                                              File file = new File("/download");


                                                            MediaManager.DownloadListener<String> completion = new MediaManager.DownloadListener<String>() {
                                                                @Override
                                                                public void onStart() {
                                                                    Toast.makeText(activity, "start fetch media data", Toast.LENGTH_SHORT).show();

                                                                }

                                                                @Override
                                                                public void onRateUpdate(long l, long l1, long l2) {

                                                                }

                                                                @Override
                                                                public void onProgress(long l, long l1) {

                                                                }

                                                                @Override
                                                                public void onSuccess(String s) {
                                                                    Toast.makeText(activity, "fetch media data success : " + s, Toast.LENGTH_SHORT).show();

                                                                }

                                                                @Override
                                                                public void onFailure(DJIError djiError) {
                                                                    if (djiError != null) {
                                                                        Toast.makeText(activity, "fetch media data fail" + djiError, Toast.LENGTH_LONG).show();
                                                                    }

                                                                }
                                                            };

                                                            if (media == null) {
                                                                Toast.makeText(activity, "media is null", Toast.LENGTH_SHORT).show();
                                                            }
                                                            if (file == null) {
                                                                Toast.makeText(activity, "file is null " + file.toString(), Toast.LENGTH_SHORT).show();
                                                            }


                                                            if (completion == null) {
                                                                Toast.makeText(activity, "completion is null", Toast.LENGTH_SHORT).show();
                                                            }

                                                            if(mediaManager == null){
                                                                Toast.makeText(activity, "media manager is null", Toast.LENGTH_SHORT).show();
                                                                mediaManager = drone.getCamera().getMediaManager();
                                                            }

                                                            mediaManager.fetchMediaData(media, file, "carcrash1", completion);


                                                            str = "Total Media files:"
                                                                    + djiMedias.size()
                                                                    + "\n"
                                                                    + "Media 1: "
                                                                    + djiMedias.get(0).getFileName();
                                                        } else {
                                                            str = "No Media in SD Card";
                                                        }
                                                    } catch (Exception e) {
                                                        Toast.makeText(activity, "fetch media " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                    }
                                                }

                                                @Override
                                                public void onFailure(DJIError djiError) {
                                                    if (djiError != null) {
                                                        Toast.makeText(activity, "fetch media fail : " + djiError.getDescription(), Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                        } else {
                                            Toast.makeText(activity, "Something is null", Toast.LENGTH_SHORT).show();
                                        }
                                    } catch (Exception e) {
                                        Toast.makeText(activity, "Set mode catch " + e.getMessage(), Toast.LENGTH_SHORT).show();

                                    }
                                } else {
                                    Toast.makeText(activity, "set mode error : " + djiError.getDescription(), Toast.LENGTH_SHORT).show();

                                }

                            }
                        });
                    }
                }
            });
            th.start();
        } catch (Exception e) {
            Toast.makeText(activity, "retrieve picture : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        return null;
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
