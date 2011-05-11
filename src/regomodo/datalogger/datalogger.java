package regomodo.datalogger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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
	LocationManager lm;
	Location loc;
	Long drift, start_time;
	Handler mHandler = new Handler();
	TextView status;
	FileWriter csv;
	STATE appState;
	SensorManager mSensorManager;
	Sensor mAccelerometer;
	SensorEvent sense;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
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
	    				// TODO Auto-generated catch block
	    				e.printStackTrace();
	    			}
	    			mSensorManager.unregisterListener(sensorListener);
	    			status.setText("WAITING");
	    			appState = STATE.WAITING; 
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
    	start_time = SystemClock.uptimeMillis();
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
    		txt += "," + sense.values[0]; // x-val
    		txt += "," + sense.values[1]; // y-val
    		txt += "," + sense.values[2]; // z-val
    		txt += "," + sense.accuracy;
    		txt += "\n";
    		try {
				csv.write(txt);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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