/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.example.audiorecord;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.locomotion.head.Head;
import com.segway.robot.sdk.locomotion.sbv.Base;
import com.segway.robot.sdk.vision.DTS;
import com.segway.robot.sdk.vision.Vision;
import com.segway.robot.support.control.HeadPIDController;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sample that demonstrates how to record a device's microphone using {@link AudioRecord}.
 */
public class AudioRecordActivity extends AppCompatActivity {

    Base mBase;
    Head mHead;
    private Vision mVision;

    private DTS mDts;
    private HeadPIDController mHeadPIDController = new HeadPIDController();
    private boolean isVisionBind;
    private boolean isHeadBind;
    private boolean isBaseBind;

    private static final int SAMPLING_RATE_IN_HZ = 16000;

    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_DEFAULT;

    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    /**
     * Factor by that the minimum buffer size is multiplied. The bigger the factor is the less
     * likely it is that samples will be dropped, but more memory will be used. The minimum buffer
     * size is determined by {@link AudioRecord#getMinBufferSize(int, int, int)} and depends on the
     * recording settings.
     */
    private static final int BUFFER_SIZE_FACTOR = 10;

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
    private Thread listeningThread = null;

    private Button startButton;
    TextToSpeech mTTS = null;
    private final int ACT_CHECK_TTS_DATA = 1000;
    WebSocketClient mWebSocketClient;
    private Button stopButton;

    private Button playButton;

    private String SERVER = "192.168.0.109";
    private int PORT  = 65432;
    String messageBusAddress = "ws://192.168.178.31:8181/core";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio);
        mTTS = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                mTTS.setLanguage(Locale.UK);
            }
        });
        setUpLoomo();
        connectWebSocket();
        mWebSocketClient.connect();
        stopButton = (Button) findViewById(R.id.btnStop);
        stopButton.setEnabled(true);
        playButton = (Button) findViewById(R.id.playButton);
        playButton.setEnabled(true);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startPlaying();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
            }
        });
        startRecording();

    }

    private void connectWebSocket(){
        URI uri;
        try {
            uri = new URI(messageBusAddress);
        }
        catch (URISyntaxException e){
            e.printStackTrace();
            return;
        }
        mWebSocketClient = new WebSocketClient(uri){
            @Override
            public void onOpen(ServerHandshake serverHandshake){
                System.out.println("WebSocket opened");
            }

            @Override
            public void onMessage(String message){
                try {
                    final JSONObject jsonObject = new JSONObject(message);
                    System.out.println(message);
                    if(jsonObject.get("type").equals("connected")){
                        System.out.println("it's connected");
                    }
                    if(jsonObject.get("type").equals("speak")){
                        JSONObject jsonObject1 = jsonObject.getJSONObject("data");
                        String utterance = jsonObject1.get("utterance").toString();
                        System.out.println("UTTERANCE: "+ utterance);
                        //   mTTS.speak(utterance, TextToSpeech.QUEUE_FLUSH, null);
                    }
                }
                catch (Throwable t){
                    System.out.println("Malformed JSON");
                }

            }
            @Override
            public void onClose(int i, String s, boolean b){
                Log.i("WebSocket", "Closed" + s);
                // conntected = false;
            }

            @Override
            public void onError(Exception e){
                Log.i("WebSocket", "Error "+ e.getMessage());
            }
        };
    }

    private void speakUtterance(String utterance){

    }

    private void startPlaying(){

    }

    private void startRecording() {
        recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLING_RATE_IN_HZ,
                CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);

        recorder.startRecording();

        recordingInProgress.set(true);
        recordingThread = new Thread(new RecordingRunnable(), "Recording Thread");
        //listeningThread = new Thread(new ListeningRunnable(), "Listening Thread");
        System.out.println("created both Threads");
        recordingThread.start();
        //listeningThread.start();
        System.out.println("startetd both Threads");
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

    public void setUpLoomo() {
        // setup the base
        mBase = Base.getInstance();
        mBase.bindService(this.getBaseContext(), new ServiceBinder.BindStateListener() {
            @Override
            public void onBind() {
                isBaseBind = true;
            }

            @Override
            public void onUnbind(String reason) {
                isHeadBind = false;
            }
        });
        // setup the head
        mHead = Head.getInstance();
        mHead.bindService(this.getApplicationContext(), new ServiceBinder.BindStateListener() {
            @Override
            public void onBind() {
                isHeadBind = true;
                mHeadPIDController.init(new HeadControlHandlerImpl(mHead));
                mHeadPIDController.setHeadFollowFactor(1.0f);
            }

            @Override
            public void onUnbind(String reason) {
                isHeadBind = false;
            }
        });
        mVision = Vision.getInstance();
    }

    // This thread is only for playing the sound, actions get triggered by the messageds received from the message bus
    private class ListeningRunnable implements Runnable {
        private BufferedReader input;
        @Override
        public void run(){
            System.out.println("in listening thread");
            try{
                // using the same port as the recording won't work, as there's constantly data written on by the client
                Socket s = new Socket(SERVER, PORT+1);
                this.input = new BufferedReader(new InputStreamReader(s.getInputStream()));
                String message = input.readLine();
                // there won't be any data written by the server if no recorded data has been sent over
                while (recordingInProgress.get()){
                    if (!message.isEmpty()) {
                        System.out.println("message is: " + message);
                    }
                    // TODO: play the sound, when data has been sent
                }
            }
            catch (Exception e){
                System.out.println("Exception in listeneing Thread "+ e);
            }
        }
    }
    private class RecordingRunnable implements Runnable {

        @Override
        public void run() {
            System.out.println("in recording thread");
            try {
                Socket s = new Socket(SERVER, PORT);
                DataOutputStream dataOutputStream = new DataOutputStream(s.getOutputStream());
                dataOutputStream.write("Connected to mic".getBytes());
                final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
                while (recordingInProgress.get()){
                    int result = recorder.read(buffer, BUFFER_SIZE);
                    if (result < 0){
                        throw new RuntimeException("Reading of audio buffer failed" +
                                getBufferReadFailureReason(result));
                    }
                    dataOutputStream.write(buffer.array(), 0, BUFFER_SIZE);
                    buffer.clear();
                    // String message = inputStream.readLine();
                    //  System.out.println(message);

                }
                // dataOutputStream.write("killsrv".getBytes());
                s.close();
            }
            catch (IOException e){
                e.printStackTrace();
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
