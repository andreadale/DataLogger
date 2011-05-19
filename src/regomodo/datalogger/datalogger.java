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
import android.widget.Button;
import android.widget.TextView;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.view.View;
import android.widget.Toast;



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
    private Button runBut;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        status = (TextView) findViewById(R.id.statusView);
        runBut = (Button) findViewById(R.id.button);
        lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        loc = null;
        request_updates();        
    }
    
    public enum STATE{
        ERROR, LOCKED, RUNNING, WAITING,NOLOCK
    }
    
    /*
    * See if GPS is turned on
    */
    private void request_updates() {
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            appState = STATE.WAITING;
            status.setText("READY");
            status.setTextColor(0xFFFF8800); // orange
            runBut.setText("GET LOCK");
        } else {
            appState = STATE.ERROR;
            status.setText("NO GPS");
            status.setTextColor(0xFFFF0000);
            runBut.setText("RESTART");
            Toast.makeText(this, 
                    "GPS is disabled in your device. Try turning it on.", 
                    Toast.LENGTH_LONG).show();  
        }       
    }
    
    LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            loc = location;
            drift = System.currentTimeMillis() - loc.getTime();
            if (appState == STATE.WAITING){
                appState = STATE.LOCKED;
                status.setText("LOCKED");
                status.setTextColor(0xFF00FF00); // green
                runBut.setText("RUN");
                runBut.setEnabled(true);
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
            // TODO: maybe use arrays for vals and average them when logging 
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
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,0, 0,
                            locationListener);
                    status.setText("GETTING LOCK");
                    status.setTextColor(0xFFFFFF00); //yellow
                    runBut.setEnabled(false);
                    break;
                    
                case LOCKED:
                    //Start recording	    			
                    mSensorManager.registerListener(sensorListener, mAccelerometer,
                                        SensorManager.SENSOR_DELAY_GAME);
                    Date date_now = new Date();
                    String d_text = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, 
                            DateFormat.LONG,Locale.UK).format(date_now);                    
                    d_text = d_text.replace(':', '-');
                    d_text = d_text.replace(',', ' ');
                    d_text += ".csv";
                    
                    File fname = new File(getExternalFilesDir(null),d_text);
                    try {
                        csv = new FileWriter(fname);
                    } catch(Exception e) {
                        status.setText(e.getLocalizedMessage());
                        break;
                    }
                    start_timer();
                    appState = STATE.RUNNING;
                    status.setText("RUNNING");
                    status.setTextColor(0xFF0000FF); // blue
                    runBut.setText("STOP");
                    Toast.makeText(this, 
                            "Data saved to: "+d_text, 
                            Toast.LENGTH_LONG).show(); 
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
                    status.setTextColor(0xFF00FF00); //green
                    appState = STATE.WAITING;
                    runBut.setText("RUN");
                    break;
                default:
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
            txt += "," + sense.values[0]; // x-val
            txt += "," + sense.values[1]; // y-val
            txt += "," + sense.values[2]; // z-val
            txt += "," + sense.accuracy;
            txt += "\n";
            try {
                csv.write(txt);
//                status.setText(txt);
            } catch (IOException e) {
                status.setText(e.getLocalizedMessage());
            }
        }
    }
    
    private Runnable mTimer = new Runnable() {
        public void run() {
            Long now = SystemClock.uptimeMillis(); // needed for timer
            write_data();
            mHandler.postAtTime(this, now+500);
        }    
    };
}