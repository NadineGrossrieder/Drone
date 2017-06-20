package renault.drone.risvrenault;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

    private MediaManager mediaManager;
    private Bitmap[] bitmapArray;
    private int currentDownloadMedia = 0;
    private final File filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

    private int retry = 0;

    private MediaFile media1;
    private MediaFile media2;

    private Bitmap bitmap1;
    private Bitmap bitmap2;

    private String filename1;
    private String filename2;


    Camera(Aircraft drone) {
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
     **/
    void takePicture(final Context activity) {
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

        // TODO handle error

    }

    void getTwoLastPictures(final Context activity, final MissionListener missionListener, final Drone droneContext) {
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

                        final DownloadListener downloadListener = new DownloadListener() {
                            @Override
                            public void onFinish() {
                                bitmapArray = new Bitmap[]{bitmap1, bitmap2};
                                missionListener.onResultCarCrash(false, "Medias downloaded with success", 0, bitmapArray);
                                droneContext.finishMission();
                            }
                        };

                        drone.getCamera().setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD, new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (null == djiError) {
                                    try {
                                        if (drone != null && drone.getCamera() != null && drone.getCamera().getMediaManager() != null) {
                                            drone.getCamera().getMediaManager().fetchMediaList(new MediaManager.DownloadListener<List<MediaFile>>() {
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

                                                    try {
                                                        if (djiMedias != null && !djiMedias.isEmpty()) {

                                                            media1 = djiMedias.get(djiMedias.size()-1);
                                                            media2 = djiMedias.get(djiMedias.size()-2);

                                                            MediaManager.DownloadListener<String> completion = new MediaManager.DownloadListener<String>() {
                                                                @Override
                                                                public void onStart() {
                                                                }

                                                                @Override
                                                                public void onRateUpdate(long total, long current, long persize) {
                                                                    if(missionListener != null) {
                                                                        float currentPercent = ((float)current / (float) total)*100;
                                                                        missionListener.onResultCarCrash(true, "File " + currentDownloadMedia +  "/2 downloaded at ", currentPercent,  null);

                                                                    }
                                                                }

                                                                @Override
                                                                public void onProgress(long total, long current) {

                                                                }

                                                                @Override
                                                                public void onSuccess(String s) {
                                                                    if(missionListener != null) {
                                                                        BitmapFactory.Options options = new BitmapFactory.Options();
                                                                        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

                                                                        missionListener.onResultCarCrash(true,"File download in : " + s, 0, null);
                                                                        if(currentDownloadMedia == 2){
                                                                            currentDownloadMedia = 0;

                                                                            bitmap2 = BitmapFactory.decodeFile(filePath.getAbsolutePath() + "/" + filename2 + ".jpeg", options);
                                                                            missionListener.onResultCarCrash(true,"Download Finished : " + s, 0, null);

                                                                            downloadListener.onFinish();
                                                                        }
                                                                        else if(currentDownloadMedia == 1 ){
                                                                            currentDownloadMedia = 2;
                                                                            bitmap1 = BitmapFactory.decodeFile(filePath.getAbsolutePath() + "/" + filename1 +".jpeg", options);
                                                                        }
                                                                    }
                                                                }

                                                                @Override
                                                                public void onFailure(DJIError djiError) {
                                                                    if (djiError != null) {
                                                                        if(missionListener != null) {
                                                                            missionListener.onResultCarCrash(true, "Fetch media data fail : " + djiError, 0, null);
                                                                        }

                                                                        if(retry <=1) {
                                                                            retry++;
                                                                            getTwoLastPictures(activity, missionListener, droneContext);
                                                                        }
                                                                    }

                                                                }
                                                            };

                                                            if (media1 == null || media2 == null) {
                                                                if(missionListener != null) {
                                                                    missionListener.onResultCarCrash(false, "Media is null", 0, null);
                                                                }
                                                            }
                                                            else {

                                                                if (mediaManager == null) {
                                                                    mediaManager = drone.getCamera().getMediaManager();
                                                                }

                                                                currentDownloadMedia = 1;
                                                                //Download file 1
                                                                filename1 = "carcrash1";
                                                                mediaManager.fetchMediaData(media1, filePath, filename1, completion);

                                                                //Download file 2
                                                                filename2 = "carcrash2";
                                                                mediaManager.fetchMediaData(media2, filePath, filename2, completion);
                                                            }
                                                        } else {
                                                            if(missionListener != null) {
                                                                missionListener.onResultCarCrash(false,  "No Media in SD Card", 0, null);
                                                            }
                                                        }
                                                    } catch (Exception e) {
                                                        if(missionListener != null) {
                                                            missionListener.onResultCarCrash(false, e.getMessage(), 0, null);
                                                        }
                                                    }
                                                }

                                                @Override
                                                public void onFailure(DJIError djiError) {
                                                    if (djiError != null) {
                                                        if(missionListener != null) {
                                                            missionListener.onResultCarCrash(false,"Fetch media list fail : " + djiError.getDescription(), 0, null);
                                                        }
                                                    }
                                                }
                                            });
                                        } else {
                                            if(missionListener != null) {
                                                missionListener.onResultCarCrash(false,"MediaManager is null or Camera is null", 0, null);
                                            }
                                        }
                                    } catch (Exception e) {
                                        if(missionListener != null) {
                                            missionListener.onResultCarCrash(false, e.getMessage(), 0, null);
                                        }
                                        Toast.makeText(activity, "Set mode catch error : " + e.getMessage(), Toast.LENGTH_SHORT).show();

                                    }
                                } else {
                                    if(missionListener != null) {
                                        missionListener.onResultCarCrash(false, djiError.getDescription(), 0, null);
                                    }
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
    }


    /**
     * Retrieves information about the orientation (pitch, roll, yaw) of the gimbal (camera)
     *
     * @return An instance of Orientation representing the pitch, roll and yaw of the gimbal
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




    public interface DownloadListener{

        void onFinish();


    }

}

