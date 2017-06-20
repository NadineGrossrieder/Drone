package renault.drone.risvrenault;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightOrientationMode;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.util.CommonCallbacks;
import dji.sdk.products.Aircraft;

/**
 * Created by Nadine Grossrieder on 23.05.2017.
 * Scan qrcode and adjust movement
 */

public class FollowQRCode extends View {
    private static final String TAG = FollowQRCode.class.getName();

    private static final Float SPEED = 0.2f;
    private static final Float FOV = 80.0f;
    private static final float MIN_ALTITUDE = 2.0f;

    private static Float currentAltitudeSinceStart;
    private Float altitudeToGo;


    private Rect targetRect = new Rect(500, 275, 750, 515);

    private BarcodeDetector detector;
    private Paint paint;
    private Rect[] facesArray = null;
    private final Object lock = new Object(); //Drawing mutex

    private Context context;
    private Aircraft drone;
    private TextureView cameraView;
    private int viewWidth;
    private int viewHeight;
    private DroneStates droneStates;
    private MissionListener missionListener;


    public readQrCode readThread;

    private float speed;
    public char[] state;

    private boolean needToResetRect = false;


    public FollowQRCode(Context context) {
        super(context);
        Log.d(TAG, "constructor 1");
//        init(context);
    }

    public FollowQRCode(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG, "constructor 2");
//        init(context);
    }

    public FollowQRCode(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Log.d(TAG, "constructor 3");
        init(context);
    }

    private void init(Context context) {
        Toast.makeText(context, "init", Toast.LENGTH_SHORT).show();

    }

    protected void setAltitude(float altitude){
        altitudeToGo = altitude;
    }


    void resume(Context context, Aircraft drone, TextureView cameraView, int viewWidth, int viewHeight, DroneStates droneStates, MissionListener missionListener) {
        this.context = context;
        this.drone = drone;
        this.cameraView = cameraView;
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
        this.droneStates = droneStates;
        this.missionListener = missionListener;

        detector = new BarcodeDetector.Builder(context).setBarcodeFormats(Barcode.QR_CODE).build();

        if (!detector.isOperational()) {
            Toast.makeText(context, "Could not set up the QR detector!", Toast.LENGTH_SHORT).show();
            return;
        }


        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);


        drone.getCamera().setFocusMode(SettingsDefinitions.FocusMode.AUTO, null);
        drone.getCamera().setExposureMode(SettingsDefinitions.ExposureMode.PROGRAM, null);


        //Point in the screen for the focus
        PointF point = new PointF(viewWidth / 2, viewHeight / 2);
        drone.getCamera().setFocusTarget(point, null);


        readThread = new readQrCode(context);
        readThread.start();
    }


    class readQrCode extends Thread {
        private final Handler handler;
        boolean interrupted = false;
        boolean killed = false;
        private String state = "start";

        private readQrCode(final Context ctx) {
            handler = new Handler(ctx.getMainLooper());
        }

        void interrupt(boolean isInterrupted) {
            interrupted = isInterrupted;
            if (isInterrupted) {
//                scanningQRCodeState += "Thread interrupted\n\r";
            } else {
                currentAltitudeSinceStart = droneStates.getAltitudeFromGPS();
//                scanningQRCodeState += "Thread restart\n\r";
            }
        }

        void kill() {
            killed = true;
            state += "Thread killed\n\r";
            if(missionListener != null){
                missionListener.onResultFollow(false, "Follow mode killed");
            }
            needToResetRect = true;
            readThread = null;
        }

        @Override
        public void run() {
            if(missionListener != null){
                missionListener.onResultFollow(false, "Follow mode started");
            }
            Log.d(TAG, "Thread started");
            state = "run thread";

            currentAltitudeSinceStart = droneStates.getAltitudeFromGPS();

            while (!killed) {
                if (viewWidth > 0 && viewHeight > 0) {

//
//                    if (isFirstLoop) {
//
//                        try {
//
//                            //Change Gimbal mode
//                            if(gimbalMode == GimbalMode.YAW_FOLLOW) {
//                                currentDrone.getGimbal().setMode(GimbalMode.YAW_FOLLOW, new CommonCallbacks.CompletionCallback() {
//                                    @Override
//                                    public void onResult(DJIError djiError) {
//                                        if (djiError != null) {
//                                            scanningQRCodeState += "Gimbal Mode : " + djiError.getDescription() + "\n\r";
//                                        } else {
//                                            scanningQRCodeState += "Change gimbal mode with success\n\r";
//                                            isFirstLoop = false;
//                                        }
//                                    }
//                                });
//                            }
//
//                            //Move gimbal to look down
//                            Rotation.Builder rotation = new Rotation.Builder();
//                            rotation.mode(RotationMode.ABSOLUTE_ANGLE);
//                            rotation.pitch(-90.0f);
//                            rotation.yaw(-90.0f);
//                            Rotation r = rotation.build();
//                            drone.getGimbal().rotate(r, new CommonCallbacks.CompletionCallback() {
//                                @Override
//                                public void onResult(DJIError djiError) {
//                                    if (djiError != null) {
////                                        scanningQRCodeState += "Pitch gimbal : " + djiError.getDescription() + "\n\r";
//                                    } else {
////                                        scanningQRCodeState += "Move gimbal pitch with success\n\r";
////                                        isFirstLoop = false;
//
//
//                                    }
//                                }
//                            });
//
//                            // Rotate the gimbal to have the front as the right on the phone
////                            Rotation.Builder rotationYaw = new Rotation.Builder();
////                            rotationYaw.mode(RotationMode.ABSOLUTE_ANGLE);
////                            rotation.yaw(-90);
////                            Rotation r2 = rotation.build();
////                            currentDrone.getGimbal().rotate(r2, new CommonCallbacks.CompletionCallback() {
////                                @Override
////                                public void onResult(DJIError djiError) {
////                                    if (djiError != null) {
////                                        scanningQRCodeState += "Yaw gimbal : "+ djiError.getDescription() + "\n\r";
////                                    } else {
////                                        scanningQRCodeState += "Move gimbal yaw with success\n\r";
////                                        isFirstLoop = false;
////                                    }
////                                }
////                            });
//
//
//
//                        } catch (Exception e) {
////                            scanningQRCodeState += "Error when moving gimbal : " + e.getMessage() + "\n\r";
//                        }
//                    }

//                    scanningQRCodeState += "Is first loop : " + isFirstLoop + "\n\r";


                    int left = (viewWidth / 2) - 150;
                    int top = (viewHeight / 2) - 150;
                    int right = (viewWidth / 2) + 150;
                    int bottom = (viewHeight / 2) + 150;
                    targetRect.set(left, top, right, bottom);

                    if (droneStates.isFlying()) {
                        try {
                            Bitmap source = cameraView.getBitmap();
                            if (source != null) {
                                Log.d(TAG, "source ok");
                                Frame convFram = new Frame.Builder().setBitmap(source).build();
                                final SparseArray<Barcode> barcodes = detector.detect(convFram);
                                Log.d(TAG, barcodes.toString());

                                if (barcodes.size() > 0) {
                                    Log.d(TAG, "QR Code detected");
//                                    if(missionListener != null){
//                                        missionListener.onResultFollow(false, "QRCodeDetected");
//                                    }
//                                    scanningQRCodeState += barcodes.valueAt(0).displayValue + "\n\r";

                                    Rect qrRect = barcodes.valueAt(0).getBoundingBox();
                                    Point[] qrPoints = barcodes.valueAt(0).cornerPoints;

                                    if (!interrupted) {
                                        adjustMovements(qrPoints);
                                    }

                                    synchronized (lock) {
                                        facesArray = new Rect[2];
                                        facesArray[0] = targetRect;
                                        facesArray[1] = qrRect;
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                invalidate();
                                            }
                                        });
                                    }


                                } else {
                                    noQRFound();
                                    state = "no qrcode found";

                                }
                            } else {
                                state += "Nothing to analyze\n\r";
                            }

                        } catch (Exception e) {
                            state += e.getMessage() + "\n\r";
                        }


                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            state += e.getMessage() + "\n\r";
                        }

                    } else {
                        Log.d(TAG, "Drone is not currently flying");
                        state += "Drone is not currently flying\n\r";

                    }
                } else {
                    state += "Wrong view height and width\n\r";
                }
            }
        }

        private void computeSpeed(Point qrMassPoint) {
            // Compute speed
            float distance = (float) Math.sqrt(Math.pow((targetRect.centerX() - qrMassPoint.x), 2) + Math.pow((targetRect.centerY() - qrMassPoint.y), 2));
            state += "Distance : " + distance + "\n\r";


            double halfViewMeter = Math.tan(Math.toRadians(FOV / 2)) * droneStates.getAltitudeFromGPS();

            //Ratio for 1m
            double ratioPixelMeter = (viewWidth / 2) / halfViewMeter;

            //Distance in pixels from the QR Code to the target
            double deltaX = Math.abs(qrMassPoint.x - targetRect.centerX());
            double deltaY = Math.abs(qrMassPoint.y - targetRect.centerY());

            //Distance in meters from the QR Code to the target
            double distTargetQRX = Math.round(deltaX / ratioPixelMeter);
            double distTargetQRY = Math.round(deltaY / ratioPixelMeter);

            state += "x:" + distTargetQRX + " y:" + distTargetQRY + "\n\r";
            state += "dx:" + deltaX + " dy:" + deltaY + "\n\r";
            state += "ratioPixelMeter:" + ratioPixelMeter + "\n\r";
            state += "halfViewMeter:" + halfViewMeter + "\n\r";

            if (distTargetQRX > distTargetQRY) {
                speed = (float) distTargetQRX * SPEED;
            } else {
                speed = (float) distTargetQRY * SPEED;
            }

            state += "Speed:" + speed + "\n\r";

        }

        //
        private float computeAngle(Point qrMassPoint, Boolean isLeft, float angle) {
            float angleSpeed;
            if (isLeft) {
                angleSpeed = -45;
            } else {
                angleSpeed = 45;
            }
            return angleSpeed;
        }


        /**
         * Called when the drone needs to move closer to a QR code, it is not very precise since it
         * is a triangulation from the screen to the 3D world. The main trick is to use small value
         * so the drone will be more precise. It corrects the movements on every axis, still it
         * does not affect the yaw.
         *
         * @param qrPoints the coordinates of the QR rectangle
         */
        private void adjustMovements(Point[] qrPoints) {
//            if(missionListener != null){
//                missionListener.onResultFollow(false, "Adjust position");
//            }
//            if (currentAltitude == 0) {
//                currentAltitude = droneStates.getAltitudeFromGPS();
//            }

            try {
                if (droneStates.getAltitudeFromSensor() > MIN_ALTITUDE || droneStates.getAltitudeFromGPS() > MIN_ALTITUDE) {

                    //preparations in order to get the Virtual Stick Mode available
                    drone.getFlightController().setVirtualStickModeEnabled(true, null);
                    drone.getFlightController().setFlightOrientationMode(FlightOrientationMode.AIRCRAFT_HEADING, null);
                    drone.getFlightController().setTerrainFollowModeEnabled(false, null);
                    drone.getFlightController().setTripodModeEnabled(false, null);

                    if (drone.getFlightController().isVirtualStickControlModeAvailable()) {
                        Log.d(TAG, "virtual stick control mode available");
                        if (!drone.getFlightController().isVirtualStickAdvancedModeEnabled()) {
                            drone.getFlightController().setVirtualStickAdvancedModeEnabled(true);
                            Log.d(TAG, "Virtual Stick Advanced Mode enabled");
                        }
                        //Setting the control modes for Roll, Pitch and Yaw
                        drone.getFlightController().setRollPitchControlMode(RollPitchControlMode.VELOCITY);
//                        ((Aircraft) currentDrone).getFlightController().setYawControlMode(YawControlMode.ANGLE);
                        drone.getFlightController().setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
//                        ((Aircraft) currentDrone).getFlightController().setVerticalControlMode(VerticalControlMode.POSITION);

                        if(altitudeToGo != null){
                            drone.getFlightController().setVerticalControlMode(VerticalControlMode.POSITION);

                        }else{
                            drone.getFlightController().setVerticalControlMode(VerticalControlMode.VELOCITY);
                        }
                        drone.getFlightController().setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);

                        // Disable the landing protection otherwise, during auto-landing, the downwards facing vision sensor will check if the ground surface is flat enough for a safe landing
//                        try {
//                            ((Aircraft) currentDrone).getFlightController().getFlightAssistant().setLandingProtectionEnabled(false, new CommonCallbacks.CompletionCallback() {
//                                @Override
//                                public void onResult(DJIError djiError) {
//                                    if (djiError != null) {
//                                        scanningQRCodeState += djiError.getDescription() + "\n\r";
//                                    }
//                                }
//                            });
//                        } catch (Exception e) {
//                            scanningQRCodeState += e.getMessage() + "\n\r";
//                        }
                    }


                    int qrWidth = qrPoints[1].x - qrPoints[0].x;
                    int qrHeight = qrPoints[2].y - qrPoints[0].y;


                    //center point of the QR Square
                    final Point qrMassPoint = new Point(qrPoints[1].x - (qrWidth / 2), qrPoints[2].y - (qrHeight / 2));

                    computeSpeed(qrMassPoint);

                    FlightControlData move = new FlightControlData(0, 0, 0, 0);
                    if(altitudeToGo != null) {
                        move = new FlightControlData(0, 0, 0, altitudeToGo);
                    }

//                    if (targetRect.contains(qrMassPoint.x, qrMassPoint.y) && isLandMode()) {
                    if (targetRect.contains(qrMassPoint.x, qrMassPoint.y) && altitudeToGo == null) {
                        //decrease altitude
                        state = "decrease altitude";
                        if (drone.getFlightController().isLandingGearMovable()) {

                            drone.getFlightController().getLandingGear().deploy(new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError != null) {
//                                        scanningQRCodeState += djiError.getDescription() + "\n\r";
                                    } else {
//                                        scanningQRCodeState += "Success deploy landing gear\n\r";
                                    }
                                }
                            });
                        }

//                    move.setVerticalThrottle(-SPEED);
//                        if (currentAltitude - 0.5f < 0) {
//                            move.setVerticalThrottle(0);
//                        } else {
//                            move.setVerticalThrottle(currentAltitude - 0.5f);
//                        }
                        move.setVerticalThrottle(-0.3f);

//                        scanningQRCodeState += "Decrease altitude\n\r";

                    }

//                    if (!isLandMode()) {
//                        scanningQRCodeState += "Follow mode\n\r";
//                        LandingGear gearState = ((Aircraft) currentDrone).getFlightController().getLandingGear();
//
//                        if (((Aircraft) currentDrone).getFlightController().isLandingGearMovable()
//                                && gearState == null && !gearState.getState().name().equals("RETRACTED")) {
//
//                            ((Aircraft) currentDrone).getFlightController().getLandingGear().retract(new CommonCallbacks.CompletionCallback() {
//                                @Override
//                                public void onResult(DJIError djiError) {
//                                    if (djiError != null) {
//                                        scanningQRCodeState += djiError.getDescription() + "\n\r";
//                                    } else {
//                                        scanningQRCodeState += "Success retract landing gear\n\r";
//                                    }
//                                }
//                            });
//                        }
//                    }

//                    if(!isLandMode() && currentAltitudeSinceStart > getAltitudeFromGPS()){
////                        scanningQRCodeState += "Adjust height - Move up\n\r";
//                        move.setVerticalThrottle(0.2f);
//                    }

//                    if(!isLandMode() && currentAltitudeSinceStart < getAltitudeFromGPS()){
////                        scanningQRCodeState += "Adjust height - Move down\n\r";
//                        move.setVerticalThrottle(-0.2f);
//                    }

                    // Camera is turn to have the right side of the picture as the front side of the drone
                    if (qrMassPoint.x > targetRect.right) {
                        //move drone forward
                        move.setRoll(speed);


                        // Check if must move to the right or left without using yaw
                        if (qrMassPoint.y > targetRect.bottom) {
                            //move drone to right
                            move.setPitch(speed);
                            state += "Move to Right\n\r";

                        } else if (qrMassPoint.y < targetRect.top) {
                            //move drone to left
                            move.setPitch(-speed);
                            state += "Move to Left\n\r";

                        }
                    } else {
                        if (qrMassPoint.x < targetRect.left) {
                            //move drone back
//                            move.setRoll(-speed);

                            // Turn the drone at 180° and move forward

//                            float yaw = (float)(currentYaw + 180);
//                            if(yaw > 180){
//                                yaw = -(360 - yaw);
//                            }
                            move.setYaw(computeAngle(null, true, 90));
                            // Move forward
                            //move.setRoll(speed);

                            state += "Turn back\n\r";


                            // Check if must go left or right without using yaw
//                            if (qrMassPoint.y > targetRect.bottom) {
//                                //move drone to left
//                                move.setPitch(-speed);
//                                scanningQRCodeState += "Move to Left\n\r";
//                            } else if (qrMassPoint.y < targetRect.top) {
//                                //move drone to left
//                                move.setPitch(speed);
//                                scanningQRCodeState += "Move to Right\n\r";
//                            }
                        } else if (qrMassPoint.y > targetRect.bottom) {
                            //move drone to right
                            move.setYaw(computeAngle(qrMassPoint, false, 0));
                            // Move forward
                            move.setRoll(speed);
                            state += "Turn to Right\n\r";

                        } else if (qrMassPoint.y < targetRect.top) {
                            //move drone to left
                            move.setYaw(computeAngle(qrMassPoint, true, 0));

                            // Move forward
                            move.setRoll(speed);
                            state += "Turn to Left\n\r";

                        }
                    }


//                    if (qrMassPoint.x < targetRect.left) {
//                        //move drone back
//                        move.setRoll(-speed);
//                        scanningQRCodeState += "Move back\n\r";
//                    } else if (qrMassPoint.x > targetRect.right) {
//                        //move drone forward
//                        move.setRoll(speed);
//                        scanningQRCodeState += "Move forward\n\r";
//                    }
//
//
//                    if (qrMassPoint.y > targetRect.bottom) {
//                        //move drone to right
//                        move.setPitch(speed);
//                        scanningQRCodeState += "Move to Right\n\r";
//                    } else if (qrMassPoint.y < targetRect.top) {
//                        //move drone to left
//                        move.setPitch(-speed);
//                        scanningQRCodeState += "Move to Left\n\r";
//                    }
//


//                    scanningQRCodeState += "Move :" + move.getPitch() + " " + move.getRoll() + " " + move.getYaw() + " " + move.getVerticalThrottle() + "\n\r";

//                ((Aircraft) currentDrone).getFlightController().sendVirtualStickFlightControlData(move, null);

                    drone.getFlightController().sendVirtualStickFlightControlData(move, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
//                                scanningQRCodeState += "Send move : " + djiError.getDescription() + "\n\r";
                                if(missionListener != null){
                                    missionListener.onResultFollow(false, djiError.getDescription());
                                }
                            }
                        }
                    });
                }
//                else if ((getAltitudeFromGPS() <= MIN_HEIGHT || getAltitudeFromSensor() <= MIN_HEIGHT) && isLandMode()) {
//                    land();
//                }
                else {
                    state += "Must flight higher\n\r";
                    if(missionListener != null){
                        missionListener.onResultFollow(false, "Must flight higher");
                    }
                }
            } catch (Exception e) {
//                scanningQRCodeState += e.getMessage() + "\n\r";
            }

        }


        /**
         * Called when a frame does not contain any QR code, this increase the number of frame
         * without QR code and delete the previous rectangles drawn on the canvas. When a certain
         * amount of frames without QR code are reached the autopilot is restarted.
         * Since the two functionality (autonomous flight & detection of QR code) have not
         * been tested together the startautopilot is commented and instead we set a default
         * movement as follow : currentDrone.moveDroneInMeters(0f,0f,0f,0f);
         */
        private void noQRFound() {
            // Disable virtual stick
//            if(((Aircraft) currentDrone).getFlightController().isVirtualStickControlModeAvailable() && ((Aircraft) currentDrone).getFlightController().isVirtualStickAdvancedModeEnabled()) {
//                ((Aircraft) currentDrone).getFlightController().setVirtualStickModeEnabled(false, new CommonCallbacks.CompletionCallback() {
//                    @Override
//                    public void onResult(DJIError djiError) {
//                        if(djiError != null) {
//                            scanningQRCodeState += djiError.getDescription() + "\n\r";
//                        }
//                    }
//                });
//            }
            synchronized (lock) {
                facesArray = new Rect[1];
                facesArray[0] = targetRect;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        invalidate();
                    }
                });
            }
        }

        private void runOnUiThread(Runnable r) {
            handler.post(r);
        }

    }

    /**
     * Used to display custom shapes over the texture. We use this to draw the rectangles.
     *
     * @param canvas the canvas that will get the rectangles drawn on
     */
    @Override
    protected void onDraw(Canvas canvas) {
        synchronized (lock) {
            if (facesArray != null && facesArray.length > 0) {
                for (Rect target : facesArray) {
                    if(needToResetRect) {
                        paint.setColor(Color.TRANSPARENT);
                    }
                    else {
                        if (target == targetRect) {
                            paint.setColor(Color.GREEN);
                        } else {
                            paint.setColor(Color.RED);
                        }
                    }
                    canvas.drawRect(target, paint);
                }
                needToResetRect = false;
            }
        }

        super.onDraw(canvas);
    }


}
