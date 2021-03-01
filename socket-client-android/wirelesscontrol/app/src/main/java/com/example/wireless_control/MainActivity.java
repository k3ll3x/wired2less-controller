package com.example.wireless_control;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

//client socket
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;


public class MainActivity extends AppCompatActivity {
    //client socket
    Thread Thread1 = null;
    EditText server;
    Button btnConnect;
    TextView log, ipport;
    String SERVER_IP;
    int SERVER_PORT;
    boolean isConnected = false;
    private PrintWriter output;
    private BufferedReader input;
    //Dpad dpad = new Dpad();

    public int countCharInString(char c, String str){
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }

    public void addTextToLog(String text){
        if(countCharInString('\n', "" + log.getText()) > 10){
            log.setText("");
        }
        log.append(text + '\n');
    }

    class Thread1 implements Runnable {
        public void run() {
            Socket socket;
            try {
                socket = new Socket(SERVER_IP, SERVER_PORT);
                output = new PrintWriter(socket.getOutputStream());
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                runOnUiThread(() -> {
                    addTextToLog("Connected");
                    ipport.setTextColor(Color.GREEN);
                    ipport.setText("IP: " + SERVER_IP + "             PORT: " + SERVER_PORT);
                    isConnected = true;
                });
                new Thread(new Thread2()).start();
            } catch (IOException e) {
                isConnected = false;
                addTextToLog(e.getMessage());
                e.printStackTrace();
            }
        }
    }
    class Thread2 implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    final String message = input.readLine();
                    if (message != null) {
                        runOnUiThread(() -> addTextToLog("\t\tserver: " + message));
                    } else {
                        Thread1 = new Thread(new Thread1());
                        Thread1.start();
                        return;
                    }
                } catch (IOException e) {
                    isConnected = false;
                    addTextToLog(e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
    class Thread3 implements Runnable {
        private String message;
        Thread3(String message) {
            this.message = message;
        }
        @Override
        public void run() {
            output.write(message);
            output.flush();
            runOnUiThread(() -> addTextToLog("client: " + message));
        }
    }

    private static float getCenteredAxis(MotionEvent event, InputDevice device, int axis, int historyPos) {
        final InputDevice.MotionRange range = device.getMotionRange(axis, event.getSource());

        // A joystick at rest does not always report an absolute position of
        // (0,0). Use the getFlat() method to determine the range of values
        // bounding the joystick axis center.
        if (range != null) {
            final float flat = range.getFlat();
            final float value = historyPos < 0 ? event.getAxisValue(axis) : event.getHistoricalAxisValue(axis, historyPos);

            // Ignore axis values that are within the 'flat' region of the
            /* joystick axis center.
            if (Math.abs(value) > flat) {
                return value;
            }*/
            return value;
        }
        return 0;
    }


    private void processJoystickInput(MotionEvent event, int historyPos) {
        InputDevice inputDevice = event.getDevice();

        //LEFT
        float x = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_X, historyPos);
        if( x != 0 && isConnected){
            new Thread(new Thread3("left\tx\t" + x + '\n')).start();
        }
        float y = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_Y, historyPos);
        if( y != 0 && isConnected){
            new Thread(new Thread3("left\ty\t" + y + '\n')).start();
        }

        //RIGHT
        x = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_Z, historyPos);
        if( x != 0 && isConnected){
            new Thread(new Thread3("right\tx\t" + x + '\n')).start();
        }
        y = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_RZ, historyPos);
        if( y != 0 && isConnected){
            new Thread(new Thread3("right\ty\t" + y + '\n')).start();
        }
    }

    public boolean onGenericMotionEvent(MotionEvent event) {
        /*if (Dpad.isDpadDevice(event)) {
            int press = dpad.getDirectionPressed(event);
            if(isConnected){
                new Thread(new Thread3("dpad\t" + press + '\n')).start();
            }
        }*/

        // Check that the event came from a game controller
        if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK && event.getAction() == MotionEvent.ACTION_MOVE) {

            // Process all historical movement samples in the batch
            final int historySize = event.getHistorySize();

            // Process the movements starting from the
            // earliest historical position in the batch
            for (int i = 0; i < historySize; i++) {
                // Process the event at historical position i
                processJoystickInput(event, i);
            }

            // Process the current movement sample in the batch (position -1)
            processJoystickInput(event, -1);
            return true;
        }
        return super.onGenericMotionEvent(event);
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (event.getRepeatCount() == 0) {
            if(isConnected) {
                new Thread(new Thread3(String.valueOf(keyCode) + "\tup\n")).start();
            }
        }
        return true;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getRepeatCount() == 0) {
            if(isConnected) {
                new Thread(new Thread3(String.valueOf(keyCode) + "\tdown\n")).start();
            }
        }
        return true;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        super.onCreate(savedInstanceState);

        //client socket
        server = findViewById(R.id.server);
        log = findViewById(R.id.log);
        ipport = findViewById(R.id.ip_port);
        btnConnect = (Button)findViewById(R.id.connect_btn);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("connect button","is clicked!");
                String srv_ip_port = server.getText().toString();
                int iend = srv_ip_port.indexOf(':');
                if (iend != -1) {
                    SERVER_IP = srv_ip_port.substring(0, iend).trim();
                    SERVER_PORT = Integer.parseInt(srv_ip_port.substring(iend + 1).trim());
                    Log.i("IP",String.valueOf(SERVER_IP));
                    Log.i("PORT",String.valueOf(SERVER_PORT));
                    Thread1 = new Thread(new Thread1());
                    Thread1.start();
                } else {
                    log.setText("Error parsing IP and PORT!");
                }
            }
        });
    }
}