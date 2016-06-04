package mati.obd2bluetooth;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
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

import com.cardiomood.android.controls.gauge.SpeedometerGauge;
import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.control.VinCommand;
import com.github.pires.obd.commands.engine.LoadCommand;
import com.github.pires.obd.commands.engine.MassAirFlowCommand;
import com.github.pires.obd.commands.engine.OilTempCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.engine.ThrottlePositionCommand;
import com.github.pires.obd.commands.pressure.FuelPressureCommand;
import com.github.pires.obd.commands.pressure.IntakeManifoldPressureCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.ObdResetCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand;
import com.github.pires.obd.enums.ObdProtocols;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.jjoe64.graphview.series.DataPoint;

import java.io.IOException;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    BluetoothClient bc;
    //OBD_Running OBD_reading;
    TextView BT_status, rpm_value, speed_value, temperature_value, EngineLoad_Value, OilTemperature_Value;
    TextView ThrottlePosiotion_Value, MassAirFlow_Value, FuelPressure_Value, IntakeManifoldPressure_Value;
    TextView VinNr_Value;
    private static final String TAG = "MyActivity";
    private SpeedometerGauge speedometer, RPMmeter;
    int count=0;


    private GoogleApiClient client;
    private final Handler mHandler = new Handler();

    AmbientAirTemperatureCommand temperatureCommand = new AmbientAirTemperatureCommand();
    //ta powyżej do sprawdzenia
    RPMCommand engineRpmCommand = new RPMCommand();
    SpeedCommand speedCommand = new SpeedCommand();
    LoadCommand EngineLoadCommand = new LoadCommand();
    OilTempCommand OilTemperatureCommand = new OilTempCommand();
    ThrottlePositionCommand ThrottleCommand = new ThrottlePositionCommand();
    MassAirFlowCommand AirFlowCommand = new MassAirFlowCommand();
    FuelPressureCommand PressureFuelCommand = new FuelPressureCommand();
    IntakeManifoldPressureCommand IMPCommand = new IntakeManifoldPressureCommand();
    VinCommand VinNrCommand = new VinCommand();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bc = new BluetoothClient(this);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //OBD_reading = new OBD_Running();

        BT_status = (TextView) this.findViewById(R.id.status_connection);
        rpm_value = (TextView) this.findViewById(R.id.RPM_Value);
        speed_value = (TextView) this.findViewById(R.id.Speed_Value);
        temperature_value = (TextView) this.findViewById(R.id.Temperature_Value);
        EngineLoad_Value = (TextView) this.findViewById(R.id.engine_load_value);
        OilTemperature_Value = (TextView) this.findViewById(R.id.oil_temperature_value);
        ThrottlePosiotion_Value = (TextView) this.findViewById(R.id.throttle_position_value);
        MassAirFlow_Value = (TextView) this.findViewById(R.id.mass_air_flow_value);
        FuelPressure_Value = (TextView) this.findViewById(R.id.fuel_pressure_value);
        IntakeManifoldPressure_Value = (TextView) findViewById(R.id.IntakeManifoldPressure_Value);
        VinNr_Value = (TextView) findViewById(R.id.vin_number_value);

        //prędkościomierz
        speedometer = (SpeedometerGauge) findViewById(R.id.speedometer);
        speedometer.setMaxSpeed(200);
        speedometer.setMajorTickStep(20);
        speedometer.setMinorTicks(1);
        speedometer.addColoredRange(0, 120, Color.GREEN);
        speedometer.addColoredRange(120, 160, Color.YELLOW);
        speedometer.addColoredRange(160, 200, Color.RED);
        speedometer.setLabelTextSize(20);
        speedometer.setLabelConverter(new SpeedometerGauge.LabelConverter() {
            @Override
            public String getLabelFor(double progress, double maxProgress) {
                return String.valueOf((int) Math.round(progress));
            }
        });

        //Obrotomierz
        RPMmeter = (SpeedometerGauge) findViewById(R.id.RPMmeter);
        RPMmeter.setMaxSpeed(8000);
        RPMmeter.setMajorTickStep(1000);
        RPMmeter.setMinorTicks(4);
        RPMmeter.addColoredRange(0, 3000, Color.GREEN);
        RPMmeter.addColoredRange(3000, 6000, Color.YELLOW);
        RPMmeter.addColoredRange(6000, 8000, Color.RED);
        RPMmeter.setLabelTextSize(20);
        RPMmeter.setLabelConverter(new SpeedometerGauge.LabelConverter() {
            @Override
            public String getLabelFor(double progress, double maxProgress) {
                return String.valueOf((int) Math.round(progress));
            }
        });


        //Button przycisk = (Button) this.findViewById(R.id.button);
        Button przycisk2 = (Button) this.findViewById(R.id.button2);

        przycisk2.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                //configOBD();
                //OBD_reading.execute();
                mHandler.postDelayed(readingValues, 100); //TODO:czas dopasować na koniec
            }
        });

        /*przycisk.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                openOBD();
            }

        });*/

        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.connect) {
            try {
                bc.findBT();
                if (bc.mBluetoothAdapter.isEnabled()) {
                    bc.openBT();
                    openOBD();
                    BT_status.setText("Połączono");
                }
                else
                    Toast.makeText(MainActivity.this, "Najpierw włącz Bluetooth", Toast.LENGTH_LONG).show();
            }
            catch (IOException ignored) {
            }
        } else if (id == R.id.disconnect) {
            try {
                bc.closeBT();
                BT_status.setText("Rozłączono");
            }
            catch (IOException ignored) {
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private final Runnable readingValues;
    {
        readingValues = new Runnable() {
            public void run() {
                try {
                    //temperatureCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                    //TODO:sprawdzić tą powyżej
                    engineRpmCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                    speedCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                    EngineLoadCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                    //OilTemperatureCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                    ThrottleCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                    AirFlowCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                    //PressureFuelCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                    //IMPCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                    //VinNrCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                    //FuelTypeCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());


                    rpm_value.setText(engineRpmCommand.getFormattedResult());
                    speed_value.setText(speedCommand.getFormattedResult());
                    //temperature_value.setText(temperatureCommand.getFormattedResult());
                    EngineLoad_Value.setText(EngineLoadCommand.getFormattedResult());
                    //OilTemperature_Value.setText(OilTemperatureCommand.getFormattedResult());
                    ThrottlePosiotion_Value.setText(ThrottleCommand.getFormattedResult());
                    MassAirFlow_Value.setText(AirFlowCommand.getFormattedResult());
                    //FuelPressure_Value.setText(PressureFuelCommand.getFormattedResult());
                    //IntakeManifoldPressure_Value.setText(IMPCommand.getFormattedResult());
                    //VinNr_Value.setText(VinNrCommand.getFormattedResult());
                    //FuelType_Value.setText(FuelTypeCommand.getFormattedResult());

                    RPMmeter.setSpeed(2500, 1000, 300); //TODO:tu zmiana prędkości
                    speedometer.setSpeed(33, 1000, 300); //TODO:tu zmiana prędkości

                    Log.d(TAG, "RPM: " + engineRpmCommand.getFormattedResult());
                    Log.d(TAG, "Speed: " + speedCommand.getFormattedResult());
                    Log.d(TAG, "Temp: " + temperatureCommand.getFormattedResult());
                    Log.d(TAG, "Temp: " + EngineLoadCommand.getFormattedResult());

                }
                catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
                finally {
                    mHandler.postDelayed(readingValues, 100); //TODO: dopasować ten czas pomairu
                }
            }
        };
    }

    void openOBD()
    {
        try {
            new ObdResetCommand().run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
            Log.d(TAG, "Po Reset OBD");
            new EchoOffCommand().run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
            Log.d(TAG, "Po Echo OBD");
            new LineFeedOffCommand().run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
            Log.d(TAG, "Po LineFeed OBD");
            new TimeoutCommand(125).run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
            Log.d(TAG, "Po Timeout");
            new SelectProtocolCommand(ObdProtocols.AUTO).run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
            Log.d(TAG, "Po Select protokołu");
            //new AmbientAirTemperatureCommand().run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream()); TODO: Sprawdzić czy bez tego poniżej też pójdzie
            Log.d(TAG, "Po Ostatnim");
        } catch (Exception e) {
        }
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

    /*
    private DataPoint[] SetEngineLoad() {
        int count = 100;
        DataPoint[] values = new DataPoint[count];
        for (int i = 0; i < count; i++) {
            double x = i; //TODO: pomnożyć razy ustawiony czas
            double y = Double.parseDouble(EngineLoadCommand.getCalculatedResult());
            DataPoint v = new DataPoint(x, y);
            values[i] = v;
        }
        return values;
    }

    double mLastRandom = 2;
    Random mRand = new Random();

    private double getRandom() {
        return mLastRandom += mRand.nextDouble() * 0.5 - 0.25;
    }

    public class OBD_Running extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
        /*
         *    do things before doInBackground() code runs
         *    such as preparing and showing a Dialog or ProgressBar

           // openOBD();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        /*
         *    updating data
         *    such a Dialog or ProgressBar


        }

        @Override
        protected Void doInBackground(Void... params) {
            //do your work here


            try {
                //temperatureCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                //sprawdzić tą powyżej
                engineRpmCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                speedCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                EngineLoadCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                //OilTemperatureCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                ThrottleCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                AirFlowCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                //PressureFuelCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                //IMPCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                //VinNrCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                //FuelTypeCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                //temperatureCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            rpm_value.setText(engineRpmCommand.getFormattedResult());
            speed_value.setText(speedCommand.getFormattedResult());
            //temperature_value.setText(temperatureCommand.getFormattedResult());
            EngineLoad_Value.setText(EngineLoadCommand.getFormattedResult());
            //OilTemperature_Value.setText(OilTemperatureCommand.getFormattedResult());
            ThrottlePosiotion_Value.setText(ThrottleCommand.getFormattedResult());
            MassAirFlow_Value.setText(AirFlowCommand.getFormattedResult());
            //FuelPressure_Value.setText(PressureFuelCommand.getFormattedResult());
            //IntakeManifoldPressure_Value.setText(IMPCommand.getFormattedResult());
            //VinNr_Value.setText(VinNrCommand.getFormattedResult());
            //FuelType_Value.setText(FuelTypeCommand.getFormattedResult());

            Log.d(TAG, "RPM: " + engineRpmCommand.getFormattedResult());
            Log.d(TAG, "Speed: " + speedCommand.getFormattedResult());
            Log.d(TAG, "Temp: " + temperatureCommand.getFormattedResult());
            //Log.d(TAG, "Temp: " + OilTemperatureCommand.getFormattedResult());
        /*
         *    do something with data here
         *    display it or send to mainactivity
         *    close any dialogs/ProgressBars/etc...

        }
    }*/
}
