package regomodo.datalogger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.widget.TextView;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.view.View;



public class datalogger extends Activity {
    
    private LocationManager lm;
    private Location loc;
    private Long drift;
    private Handler mHandler = new Handler();
    private TextView status;
    private FileWriter csv;
    private STATE appState;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private SensorEvent sense;
    private float[] gravity;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        gravity = new float[3];
        status = (TextView) findViewById(R.id.statusView);        
        lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        loc = null;
        request_updates();        
    }
    
    public enum STATE{
        ERROR, LOCKED, RUNNING, WAITING
    }
    
    /*
    * See if GPS is turned on
    */
    private void request_updates() {
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            appState = STATE.WAITING;
            status.setText("READY");            
        } else {
            appState = STATE.ERROR;
            status.setText("NO GPS");
        }       
    }
    
    LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            loc = location;
            drift = System.currentTimeMillis() - loc.getTime();
            if (appState == STATE.WAITING){
                appState = STATE.LOCKED;
                status.setText("LOCKED");
            }    	
        }
        public void onProviderDisabled(String arg0) {}
        public void onProviderEnabled(String provider) {}
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };
    
    SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        
        @Override
        public void onSensorChanged(SensorEvent event) {
            sense = event;
            final float alpha = (float) 0.8;
            gravity[0] = alpha  * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha  * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
        }    	
    };
    
    public void myClickHandler(View view)
    {
        if (view.getId() == R.id.button){
            switch (appState){
                case ERROR:
                    request_updates();
                    break;
                case WAITING:
                    status.setText("NO LOCK");
                case LOCKED:
                    //Start recording	    			
                    mSensorManager.registerListener(sensorListener, mAccelerometer,
                                        SensorManager.SENSOR_DELAY_GAME);
                    File fname = new File(getExternalFilesDir(null),"test.csv");
                    try {
                        csv = new FileWriter(fname);
                    } catch(Exception e) {
                        status.setText(e.getLocalizedMessage());
                        break;
                    }
                    start_timer();
                    appState = STATE.RUNNING;
                    status.setText("RUNNING");
                    break;
                case RUNNING:    			
                    stop_timer();
                    try {
                        csv.close();
                    } catch (IOException e) {
                        status.setText(e.getLocalizedMessage());
                    }
                    mSensorManager.unregisterListener(sensorListener);
                    status.setText("WAITING");
                    appState = STATE.WAITING;
                    break;
                default:
                    break;
            }
        } else if (view.getId() == R.id.lock) {
            switch (appState){
                case WAITING:
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,0, 0,
                                        locationListener);
                    status.setText("GETTING LOCK");
                    break;
                default:
                    status.setText("?");
                    break;
            }            
        }
    }
    
    private void start_timer()
    {
        mHandler.removeCallbacks(mTimer);
        mHandler.postDelayed(mTimer, 0);
    }
    
    private void stop_timer()
    {
        mHandler.removeCallbacks(mTimer);
    }
    
    private void write_data()
    {
        String sd_state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(sd_state)) {
            // We can read and write the media
            Long t_now = System.currentTimeMillis() + drift;
            String txt = t_now.toString();
            txt += "," + loc.getLatitude();
            txt += "," + loc.getLongitude();
            txt += "," + loc.getAltitude();
            txt += "," + loc.getBearing();
            txt += "," + loc.getSpeed();
            txt += "," + loc.getAccuracy();
            txt += "," + (sense.values[0] - gravity[0]); // x-val
            txt += "," + (sense.values[1] - gravity[1]); // y-val
            txt += "," + (sense.values[2] - gravity[2]); // z-val
            txt += "," + sense.accuracy;
            txt += "\n";
            try {
                csv.write(txt);
                status.setText(txt);
            } catch (IOException e) {
                status.setText(e.getLocalizedMessage());
            }
        }
    }
    
    private Runnable mTimer = new Runnable() {
        public void run() {
            Long now = SystemClock.uptimeMillis(); // needed for timer
            write_data();
            mHandler.postAtTime(this, now+1000);
        }    
    };
}