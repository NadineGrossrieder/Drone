package renault.drone.risvrenault;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import static com.google.android.gms.location.LocationServices.FusedLocationApi;

/**
 * Created by Nadine Grossrieder on 23.05.2017.
 */

class PhoneGPSLocation extends Service
{
    private static final String TAG = PhoneGPSLocation.class.getName();
    private static final int UPDATE_FREQUENCY = 5000;

    static GoogleApiClient mGoogleApiClient;
    static Location mCurrentLocation;
    static String mGPSLocation;

    @Override
    public IBinder onBind(Intent arg0)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);

        if(mGoogleApiClient == null){
            new LocationListener();
        }

        if(mGoogleApiClient.isConnected()) {
            mGoogleApiClient.reconnect();
        }
        else{
            mGoogleApiClient.connect();
            Toast.makeText(this, "Phone GPS service started", Toast.LENGTH_SHORT).show();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d(TAG, "onCreate Service");
    }

    @Override
    public void onDestroy()
    {
        Log.i(TAG, "Service destroyed");
        Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show();

        mGoogleApiClient.disconnect();

        super.onDestroy();
    }


    //Listener
    class LocationListener implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener{

        private LocationRequest mLocationRequest;
        private LocationListener mLocationListener;

        private LocationListener(){
            mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(AppIndex.API).build();

            setLocationListener(this);
        }

        void setLocationListener(LocationListener locationListener) {
            Log.v("LocationGPSService", "new Listener");
            mLocationListener = locationListener;
        }

        @Override
        public void onLocationChanged(final Location location)
        {
            mCurrentLocation = location;
            mGPSLocation = location.getLatitude() + ", " + location.getLongitude();
        }


        //GOOGLE LOCATION LISTENER
        @Override
        public void onConnected(@Nullable Bundle bundle) {
            mLocationRequest = LocationRequest.create();
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            mLocationRequest.setInterval(UPDATE_FREQUENCY);
            mLocationRequest.setMaxWaitTime(UPDATE_FREQUENCY);

            try {
                FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, mLocationListener);
            } catch (java.lang.SecurityException ex) {
                Log.e(TAG, "fail to request location update", ex);
                onDestroy();
            }
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.e(TAG, "Connection suspended");
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.e(TAG, "Fail to connect to get location" + connectionResult.toString());
            Toast.makeText(getApplicationContext(), "Fail to connect to get location. Reason : " + connectionResult.toString(), Toast.LENGTH_LONG).show();
            Toast.makeText(getApplicationContext(), "Google service missing or not up to date", Toast.LENGTH_LONG).show();

            onDestroy();
        }
    }
}