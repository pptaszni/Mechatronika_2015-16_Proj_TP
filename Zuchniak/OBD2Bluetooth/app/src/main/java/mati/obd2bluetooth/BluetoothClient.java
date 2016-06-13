package mati.obd2bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothClient {
    //zmienne BT: adapter BT w telefonie, wykryte urządzenie BT, Socket BT, kanały I/O do przesyłu danych
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;

    Activity activity;

    //połaczenie klasy Bluetooth z MainActivity
    public BluetoothClient(Activity activity)
    {
        this.activity=activity;
        activity.setContentView(R.layout.activity_main);
    }

    //sprawdzenie czy jest adapter BT w telefonie, jeśli wyłączony prośba o jego uruchomienie
    //ustawienie "OBDII" jako domyślnego urządzenia BT
    void findBT()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            Toast.makeText(this.activity, "Brak Bluetooth w telefonie", Toast.LENGTH_LONG).show();
        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals("OBDII"))
                {
                    mmDevice = device;
                    break;
                }
            }
        }
    }

    //otwarcie połączenia i kanałów I/O z wybranym urządzeniem
    void openBT() throws IOException
    {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();
    }

    //zamknięcie połączenia i kanałów I/O z wybranym urządzeniem
    void closeBT() throws IOException
    {
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
    }
}
