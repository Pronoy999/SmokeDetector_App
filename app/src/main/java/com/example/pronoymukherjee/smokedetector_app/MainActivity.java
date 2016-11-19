package com.example.pronoymukherjee.smokedetector_app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mSocket;
    BluetoothDevice mDevice;
    InputStream inputStream;
    Thread workerThread;
    byte readBuffer[];
    int bufferPosition;
    volatile boolean stopWorker;

    EditText phone;
    Button button;
    TextView label;
    String data="";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        phone=(EditText) findViewById(R.id.phone);
        button=(Button) findViewById(R.id.button);
        label=(TextView) findViewById(R.id.message);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    findBt();
                    OpenBt();
                    SendSms(data);
                }
                catch (Exception ignored){}
            }
        });
    }

    public void findBt(){
        mBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter==null)
            label.setText("No Bluetooth Adapter");
        if(!mBluetoothAdapter.isEnabled()){
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth,0);
        }
        Set<BluetoothDevice> pairedDevices =mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0){
            for(BluetoothDevice device:pairedDevices){
                if(device.getName().equals("HC-05")){
                    mDevice=device;
                    break;
                }
            }
        }
        label.setText("BlueToothDevice Found");
    }
    public void OpenBt()throws IOException {
        UUID uuid= UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        mSocket=mDevice.createInsecureRfcommSocketToServiceRecord(uuid);
        mSocket.connect();
        inputStream=mSocket.getInputStream();
        beginListenForData();
        label.setText("Bluetooth Opened.");
    }
    public  void beginListenForData(){
        final Handler handler=new Handler();
        final byte delimiter =10;
        stopWorker=false;
        bufferPosition=0;
        readBuffer= new byte[1024];
        workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!Thread.currentThread().isInterrupted() && !stopWorker){
                    try{
                        int bytesAvailable=inputStream.available();
                        if(bytesAvailable>0){
                            byte packetBytes[]= new byte[bytesAvailable];
                            inputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++){
                                byte b= packetBytes[i];
                                if(b==delimiter){
                                    byte encodedBytes[]= new byte[bufferPosition];
                                    System.arraycopy(readBuffer,0,encodedBytes,0,encodedBytes.length);
                                    data= new String(encodedBytes,"US-ASCII");
                                    bufferPosition=0;
                                    handler.post(new Runnable(){
                                        public void run(){
                                            label.setText(data);
                                        }
                                    });
                                }
                                else readBuffer[bufferPosition++]=b;
                            }
                        }
                    }
                    catch (IOException e){ stopWorker=true;}
                }
            }
        });
        workerThread.start();
    }
    public void SendSms(String d){
        d="";
        d="There may be a fire in your house, please CHECK ASAP!";
        String phoneNumber=phone.getText().toString();
        if(phoneNumber.equalsIgnoreCase("")){
            Toast.makeText(this,"Set the Phone Number!",Toast.LENGTH_LONG).show();return;}
        try{
            SmsManager smsManager= SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber,null,d,null,null);
            Toast.makeText(this,"SMS Sent!",Toast.LENGTH_LONG).show();
        }
        catch(Exception e){
            Toast.makeText(this,"FAILED!",Toast.LENGTH_LONG).show();
        }
    }
}
