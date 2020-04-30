package com.mycroft.audiocommunication;

import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {



    private static final int SAMPLING_RATE_IN_HZ = 44100;

    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;

    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    /**
     * Factor by that the minimum buffer size is multiplied. The bigger the factor is the less
     * likely it is that samples will be dropped, but more memory will be used. The minimum buffer
     * size is determined by {@link AudioRecord#getMinBufferSize(int, int, int)} and depends on the
     * recording settings.
     */
    private static final int BUFFER_SIZE_FACTOR = 2;

    /**
     * Size of the buffer where the audio data is stored by Android
     */
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE_IN_HZ,
            CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR;

    /**
     * Signals whether a recording is in progress (true) or not (false).
     */
    private final AtomicBoolean recordingInProgress = new AtomicBoolean(false);

    private AudioRecord recorder = null;

    private Thread recordingThread = null;

    private Button startButton;

    private Button stopButton;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Button startAudioButton = findViewById(R.id.audioButton);



        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    Socket s = new Socket("192.168.178.31", 65432);
                    // TODO: get Microphone Input
                    // TODO: have the Microphone data as byte thing
                    // TODO: send the microphone data over instead of the message
                    // TODO: have the server staysing open forever
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

        startAudioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                startRecording();
            }
                                                    /*
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
            recorder.release(); */
            //TODO: play the recorded file
            //TODO: send the data as a byte stream -> in die Socketproto App einfügen
        });

        final Button playAudioButton = findViewById(R.id.audioButton);
        playAudioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               stopRecording();
               /* try {
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
        }*/
            }

        });


        final Button sendMessage = findViewById(R.id.sendMessage);
        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Thread streamThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int sampleRate = 44100;
                        int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_DEFAULT;
                        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
                        int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig,
                                audioFormat);
                        byte[] buffer = new byte[minBufSize];
                        AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
                                channelConfig, audioFormat, minBufSize);
                        recorder.startRecording();
                        try {
                            Thread.sleep(1000);
                        }
                        catch (Exception e){
                            System.out.println("Can't got to sleep");
                        }
                        minBufSize = recorder.read(buffer, 0, buffer.length);
                        System.out.println("recorded: " + minBufSize);
                        System.out.println("buffer is "+ buffer.toString());
                        String myData = "";
                        for (int j = 0; j<buffer.length; j++){
                            myData += buffer[j] ;
                        }
                        System.out.println("myData: "+ myData);

                    }
                });


            }
        });
    }

    private void startRecording() {
        recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLING_RATE_IN_HZ,
                CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);

        recorder.startRecording();

        recordingInProgress.set(true);

        recordingThread = new Thread(new RecordingRunnable(), "Recording Thread");
        recordingThread.start();
    }

    private void stopRecording() {
        if (null == recorder) {
            return;
        }

        recordingInProgress.set(false);

        recorder.stop();

        recorder.release();

        recorder = null;

        recordingThread = null;
    }

    private class RecordingRunnable implements Runnable {

        @Override
        public void run() {
            final File file = new File(getExternalCacheDir().getAbsolutePath(), "recording.pcm");
            System.out.println("PATH IST" + getExternalCacheDir().getAbsolutePath());
            final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

            try (final FileOutputStream outStream = new FileOutputStream(file)) {
                while (recordingInProgress.get()) {
                    int result = recorder.read(buffer, BUFFER_SIZE);
                    if (result < 0) {
                        throw new RuntimeException("Reading of audio buffer failed: " +
                                getBufferReadFailureReason(result));
                    }
                    outStream.write(buffer.array(), 0, BUFFER_SIZE);
                    buffer.clear();
                }
            } catch (IOException e) {
                throw new RuntimeException("Writing of recorded audio failed", e);
            }
        }

        private String getBufferReadFailureReason(int errorCode) {
            switch (errorCode) {
                case AudioRecord.ERROR_INVALID_OPERATION:
                    return "ERROR_INVALID_OPERATION";
                case AudioRecord.ERROR_BAD_VALUE:
                    return "ERROR_BAD_VALUE";
                case AudioRecord.ERROR_DEAD_OBJECT:
                    return "ERROR_DEAD_OBJECT";
                case AudioRecord.ERROR:
                    return "ERROR";
                default:
                    return "Unknown (" + errorCode + ")";
            }
        }
    }
}
