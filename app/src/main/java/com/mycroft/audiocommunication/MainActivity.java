package com.mycroft.audiocommunication;

import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    final MediaRecorder recorder = new MediaRecorder();
    final MediaPlayer player = new MediaPlayer();
    private static String fileName = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Button startAudioButton = findViewById(R.id.audioButton);
        fileName = getExternalCacheDir().getAbsolutePath();
        fileName += "/audiorecordtest.3gp";


        startAudioButton.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setOutputFile(fileName);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            try{
                recorder.prepare();
            }
            catch (Exception e){
                System.out.println("Error occured " +e);
            }
            recorder.start();
            try {
                Thread.sleep(1000);
            } catch (Exception e){
                System.out.println("Can't go to sleep");
            }
            recorder.stop();
            recorder.release();
            //TODO: play the recorded file
            //TODO: send the data as a byte stream -> in die Socketproto App einfügen

        final Button playAudioButton = findViewById(R.id.audioButton);
        playAudioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    player.setDataSource(fileName);
                    player.prepare();
                    player.start();
                } catch (IOException e){
                    Log.e("player", "prepare failed");
                }
                try {
                    Thread.sleep(1000);
                }
                catch (Exception e){
                    System.out.println("Can't go to sleep");
                }
                player.stop();
                player.release();
            }
        });
            }
        }
        );
        final Button sendMessage = findViewById(R.id.sendMessage);
        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: first: connect to the socket
                // TODO: second: send a sample text message
                Thread t = new Thread() {
                    @Override
                    public void run() {
                        try {
                            Socket s = new Socket("192.168.178.31", 65432);
                            DataOutputStream dos = new DataOutputStream(s.getOutputStream());
                            dos.write("Hello Android World".getBytes());

                            //read input stream
                            DataInputStream dis2 = new DataInputStream(s.getInputStream());
                            InputStreamReader disR2 = new InputStreamReader(dis2);
                            BufferedReader br = new BufferedReader(disR2);
                            System.out.println("Received: " + br.toString());
                            dis2.close();
                            s.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                };
                t.start();
            }
        });
    }
}
