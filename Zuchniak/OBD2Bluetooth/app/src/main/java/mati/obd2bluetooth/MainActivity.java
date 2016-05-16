package mati.obd2bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.ObdResetCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand;
import com.github.pires.obd.commands.temperature.TemperatureCommand;
import com.github.pires.obd.enums.ObdProtocols;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {


    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    BluetoothClient bc;
    TextView BT_status, rpm_value, speed_value, temperature_value;
    private static final String TAG = "MyActivity";
    int count=0;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;
    private final Handler mHandler = new Handler();
    RPMCommand engineRpmCommand = new RPMCommand();
    SpeedCommand speedCommand = new SpeedCommand();
    AmbientAirTemperatureCommand temperatureCommand = new AmbientAirTemperatureCommand();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bc = new BluetoothClient(this);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        BT_status = (TextView) this.findViewById(R.id.status_connection);
        rpm_value = (TextView) this.findViewById(R.id.RPM_Value);
        speed_value = (TextView) this.findViewById(R.id.Speed_Value);
        temperature_value = (TextView) this.findViewById(R.id.Temperature_Value);
        Button przycisk = (Button) this.findViewById(R.id.button);
        Button przycisk2 = (Button) this.findViewById(R.id.button2);

        przycisk.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                //configOBD();
                mHandler.postDelayed(readingValues, 3 * 1000);
            }
        });

        przycisk2.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                openOBD();
            }

        });

        // run the mUpdateUITimerTask's run() method in 10 seconds from now
        //mHandler.postDelayed(mUpdateUITimerTask, 10 * 1000);

        /*GraphView graph = (GraphView) findViewById(R.id.graph);
        LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(new DataPoint[] {
                new DataPoint(0, 1),
                new DataPoint(1, 5),
                new DataPoint(2, 3),
                new DataPoint(3, 2),
                new DataPoint(4, 6)
        });
        graph.addSeries(series);*/


        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.connect) {
            try {
                findBT();
                if (mBluetoothAdapter.isEnabled()) {
                    openBT();
                    //openOBD();
                    BT_status.setText("Połączono");
                    //Toast.makeText(MainActivity.this, "Otwarto Bluetooth", Toast.LENGTH_LONG).show();
                } else
                    Toast.makeText(MainActivity.this, "Najpierw włącz Bluetooth", Toast.LENGTH_LONG).show();
            } catch (IOException ex) {
            }
        } else if (id == R.id.disconnect) {
            try {
                closeBT();
                BT_status.setText("Rozłączono");
                Toast.makeText(MainActivity.this, "Rozłączono Bluetooth", Toast.LENGTH_LONG).show();
                // TODO: dodać jakieś zabezpieczenie przed wcześniejszym kliknięciem
            } catch (IOException ex) {
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private final Runnable readingValues;

    {
        readingValues = new Runnable() {
            public void run() {

                try{
                    engineRpmCommand.run(mmSocket.getInputStream(), mmSocket.getOutputStream());
                    speedCommand.run(mmSocket.getInputStream(), mmSocket.getOutputStream());
                    //temperatureCommand.run(mmSocket.getInputStream(), mmSocket.getOutputStream());
                    rpm_value.setText(engineRpmCommand.getFormattedResult());
                    Log.d(TAG, "RPM: " + engineRpmCommand.getFormattedResult());
                    speed_value.setText(speedCommand.getFormattedResult());
                    Log.d(TAG, "Speed: " + speedCommand.getFormattedResult());
                    temperature_value.setText(temperatureCommand.getFormattedResult());
                    Log.d(TAG, "Temp: " + temperatureCommand.getFormattedResult());
                    } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                } finally{
                    mHandler.postDelayed(readingValues, 500);
                    //TODO: dopasować ten czas pomairu
                }

                /*while (!Thread.currentThread().isInterrupted()) {
                    //speedCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                    try {
                        Thread.currentThread().sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    count++;
                    speed_value.setText("S"+count);
                    //rpm_value.setText(String.valueOf(count));
                    Log.d(TAG, "Speed: " + count);
                    //Thread.currentThread().sleep(1000);
                }*/

            }
        };
    }

    void startRepeatingTask() {
        readingValues.run();
    }

    void configOBD(){
        try {
            engineRpmCommand.run(mmSocket.getInputStream(), mmSocket.getOutputStream());
            //speedCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
            //temperatureCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "RPM: " + engineRpmCommand.getFormattedResult());
        //Log.d(TAG, "Speed: " + speedCommand.getFormattedResult());
        //Log.d(TAG, "Temp: " + temperatureCommand.getFormattedResult());
    }


    void readRPM() throws IOException, InterruptedException {
        RPMCommand engineRpmCommand = new RPMCommand();
        //SpeedCommand speedCommand = new SpeedCommand();
        AmbientAirTemperatureCommand temperatureCommand = new AmbientAirTemperatureCommand();
        while (!Thread.currentThread().isInterrupted()) {
            engineRpmCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
            //speedCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
            temperatureCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
            // TODO handle commands result
            rpm_value.setText(engineRpmCommand.getFormattedResult());
            //speed_value.setText(speedCommand.getFormattedResult());
            temperature_value.setText(temperatureCommand.getFormattedResult());
            Log.d(TAG, "RPM: " + engineRpmCommand.getFormattedResult());
           // Log.d(TAG, "Speed: " + speedCommand.getFormattedResult());
        }
    }

    void readSpeed() throws IOException, InterruptedException {
        //SpeedCommand speedCommand = new SpeedCommand();
        while (!Thread.currentThread().isInterrupted()) {
            //speedCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
            count++;
            //speed_value.append(String.valueOf(count));
            rpm_value.setText(String.valueOf(count));
            Log.d(TAG, "Predkosc: " + count);
            //Thread.currentThread().sleep(1000);
        }
        //}
    }

    void openOBD()
    {
        try {
            new ObdResetCommand().run(mmSocket.getInputStream(), mmSocket.getOutputStream());
            Log.d(TAG, "Po Reset OBD");
            new EchoOffCommand().run(mmSocket.getInputStream(), mmSocket.getOutputStream());
            Log.d(TAG, "Po Echo OBD");
            new LineFeedOffCommand().run(mmSocket.getInputStream(), mmSocket.getOutputStream());
            Log.d(TAG, "Po LineFeed OBD");
            new TimeoutCommand(125).run(mmSocket.getInputStream(), mmSocket.getOutputStream());
            Log.d(TAG, "Po Timeout");
            new SelectProtocolCommand(ObdProtocols.AUTO).run(mmSocket.getInputStream(), mmSocket.getOutputStream());
            Log.d(TAG, "Po Select protokołu");
            new AmbientAirTemperatureCommand().run(mmSocket.getInputStream(), mmSocket.getOutputStream());
            Log.d(TAG, "Po Ostatnim");
            //Log.d(TAG, "Po Socketach");
            //new AmbientAirTemperatureCommand().run(mmSocket.getInputStream(), mmSocket.getOutputStream());
            //new EchoOffCommand().run(mmInputStream, mmOutputStream);
            //new LineFeedOffCommand().run(mmInputStream, mmOutputStream);
            //new TimeoutCommand(125).run(mmInputStream, mmOutputStream);
            //new SelectProtocolCommand(ObdProtocols.AUTO).run(mmInputStream, mmOutputStream);
            //new AmbientAirTemperatureCommand().run(mmInputStream, mmOutputStream);
            //new RPMCommand().run(mmInputStream,mmOutputStream);
        } catch (Exception e) {
            // handle errors
        }
    }

    void findBT()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            Toast.makeText(this, "Brak Bluetooth w telefonie", Toast.LENGTH_LONG).show();
        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this.startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals("OBDII"))
                {
                    mmDevice = device;
                    Toast.makeText(this, "Wykryto OBD2 i otwarto połączenie Bluetooth", Toast.LENGTH_LONG).show();
                    break;
                }
            }
        }
    }

    void openBT() throws IOException
    {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        Log.d(TAG, "Utworzono UUID");
        mmSocket.connect();
        Log.d(TAG, "Połączono socket po uuid");
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();
        Log.d(TAG, "Otwarto połączenie BT");
    }

    void closeBT() throws IOException
    {
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        Log.d(TAG, "Zamknięto połączenie BT");
        //BT_status.setText("Rozłączono");
    }


    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://mati.obd2bluetooth/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://mati.obd2bluetooth/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }
}
