package com.mycroft.audiocommunication;

import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;

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
            //TODO: play the recorded file
            //TODO: send the data as a byte stream -> in die Socketproto App einfügen
            }
        }
        );
    }
}
