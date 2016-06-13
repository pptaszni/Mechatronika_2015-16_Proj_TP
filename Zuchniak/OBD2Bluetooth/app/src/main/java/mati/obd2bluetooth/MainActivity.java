package mati.obd2bluetooth;

/*
Aplikacja sczytujaca bieżące parametry z ECU auta poprzez ELM327 Bluetooth
1. Najpierw sparować ELM327 z telefonem
2. Po uruchomeiniu apki można nawiązać połączenie klikając w menu "Połącz z ELM327 przez Bluetooth"
3. W przypadku wyłączonego Bluetooth, użytkownik zostanie poproszony o jego włączenie
4. Po poprawnym połączeniu status połączenia powinien zmienić się na "Połączono"
5. W tym momencie można kliknać "Rozpocznij pomiar", aby uruchomić odczyt parametrów auta
*/

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
    private GoogleApiClient client;
    //klasa od funkcji połączenia Bluetooth
    BluetoothClient bc;
    //TextView do wyświetlania parametrów
    TextView BT_status, rpm_value, speed_value, temperature_value, EngineLoad_Value, OilTemperature_Value;
    TextView ThrottlePosiotion_Value, MassAirFlow_Value, FuelPressure_Value, IntakeManifoldPressure_Value, VinNr_Value;
    //TAG to logów
    private static final String TAG = "MyActivity";
    //obrotomierze
    private SpeedometerGauge speedometer, RPMmeter;
    //Handler do wątku Runnable readingValues
    private final Handler mHandler = new Handler();
    //komendy z biblioteki OBD do odczytu parametrów
    RPMCommand engineRpmCommand = new RPMCommand();
    SpeedCommand speedCommand = new SpeedCommand();
    LoadCommand EngineLoadCommand = new LoadCommand();
    ThrottlePositionCommand ThrottleCommand = new ThrottlePositionCommand();
    MassAirFlowCommand AirFlowCommand = new MassAirFlowCommand();
    AmbientAirTemperatureCommand temperatureCommand = new AmbientAirTemperatureCommand();
    OilTempCommand OilTemperatureCommand = new OilTempCommand();
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

        //prędkościomierz - inicjalizacja, ustawienie zakresu, podziałki, kolorów zakresów
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

        //Obrotomierz - inicjalizacja, ustawienie zakresu, podziałki, kolorów zakresów
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

        //przycisk do rozpoczęcia pomiaru
        Button przycisk2 = (Button) this.findViewById(R.id.button2);

        przycisk2.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                //uruchomienie wątku readingValues po 100 ms
                mHandler.postDelayed(readingValues, 100);
            }
        });


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
        //połączenie id przycisków w menu do funkcji połączenia/rozłączenia Bluetooth
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
        }
        else if (id == R.id.disconnect) {
            try {
                bc.closeBT();
                BT_status.setText("Rozłączono");
            }
            catch (IOException ignored) {
            }
        }
        return super.onOptionsItemSelected(item);
    }

    //wątek do odczytu i wyświetlania parametrów
    private final Runnable readingValues;
    {
        readingValues = new Runnable() {
            public void run() {
                try {
                    //odczyt poszczególnych parametrów z komputera pokładowego poprzez Bluetooth
                    //temperatureCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                    engineRpmCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                    speedCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                    EngineLoadCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                    //OilTemperatureCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                    ThrottleCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                    //AirFlowCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                    //PressureFuelCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                    IMPCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                    //VinNrCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());
                    //FuelTypeCommand.run(bc.mmSocket.getInputStream(), bc.mmSocket.getOutputStream());

                    //wyświetlenie poszczególnych wartości wraz z odpowiednimi jednostkami
                    rpm_value.setText(engineRpmCommand.getFormattedResult());
                    speed_value.setText(speedCommand.getFormattedResult());
                    //temperature_value.setText(temperatureCommand.getFormattedResult());
                    EngineLoad_Value.setText(EngineLoadCommand.getFormattedResult());
                    //OilTemperature_Value.setText(OilTemperatureCommand.getFormattedResult());
                    ThrottlePosiotion_Value.setText(ThrottleCommand.getFormattedResult());
                    //MassAirFlow_Value.setText(AirFlowCommand.getFormattedResult());
                    //FuelPressure_Value.setText(PressureFuelCommand.getFormattedResult());
                    IntakeManifoldPressure_Value.setText(IMPCommand.getFormattedResult());
                    //VinNr_Value.setText(VinNrCommand.getFormattedResult());
                    //FuelType_Value.setText(FuelTypeCommand.getFormattedResult());

                    //ustawienie odczytanych wartości na obrotomierze, w nawiasie kolejno: wartość, okres, opóźnienie startu ustawienia
                    RPMmeter.setSpeed(Double.parseDouble(engineRpmCommand.getCalculatedResult()), 0, 0);
                    speedometer.setSpeed(Double.parseDouble(speedCommand.getCalculatedResult()), 0, 0);

                    //logi do sprawdzania odczytanych wartości w debugerze
                    //Log.d(TAG, "RPM: " + engineRpmCommand.getFormattedResult());
                    //Log.d(TAG, "Speed: " + speedCommand.getFormattedResult());
                    //Log.d(TAG, "Temp: " + temperatureCommand.getFormattedResult());
                    //Log.d(TAG, "Temp: " + EngineLoadCommand.getFormattedResult());

                }
                catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
                finally {
                    mHandler.postDelayed(readingValues, 100);
                }
            }
        };
    }

    //inicjalizacja połączenia z ELM327 poprzez Bluetooth
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
        }
        catch (Exception e) {
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
}
