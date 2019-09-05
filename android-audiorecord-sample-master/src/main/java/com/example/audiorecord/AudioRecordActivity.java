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
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.segway.robot.algo.dts.Person;
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
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Timer;
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
    private final AtomicBoolean listeningInProgress = new AtomicBoolean(false);

    private AudioRecord recorder = null;

    private Thread recordingThread = null;
    private Thread playerThread = null;

    private Button startButton;
    TextToSpeech mTTS = null;
    private final int ACT_CHECK_TTS_DATA = 1000;
    WebSocketClient mWebSocketClient;
    private Button stopButton;

    private Button playButton;
    MediaPlayer player;

    private String SERVER = "192.168.0.109";
    //private String SERVER = "192.168.178.31";
    private int PORT  = 65432;
    String messageBusAddress = "ws://192.168.0.109:8181/core";
    //String messageBusAddress = "ws://192.168.178.31:8181/core";
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
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
            }
        });
        startRecording();
        // Should make sure, that loomo always tries to detect persons
        initateDetect();

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
               // System.out.println(message);
                try {
                    final JSONObject jsonObject = new JSONObject(message);
                    // check if a sound file has been sent
                    if(jsonObject.get("type").equals("Audio")){
                        String soundstring = "";
                        try {
                            JSONObject jsonData = jsonObject.getJSONObject("data");


                            String action = jsonData.get("action").toString();
                            if (action.equals("end")){
                                // TODO: convert to byte & play sound
                                byte[] bytes = soundstring.getBytes("UTF-8");
                                File file = File.createTempFile("mySound", "wav", getCacheDir());
                                file.deleteOnExit();
                                FileOutputStream fos = new FileOutputStream(file);
                                fos.write(bytes);
                                fos.close();
                                FileInputStream fis = new FileInputStream(file);
                                System.out.println("SOUNDSTRING " + soundstring);
                                MediaPlayer player = new MediaPlayer();
                                player.setDataSource(fis.getFD());
                                player.prepare();
                                player.start();
                                player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                    @Override
                                    public void onCompletion(MediaPlayer mp) {
                                        // TODO Auto-generated method stub
                                        mp.release();
                                    }
                                });
                            }
                            else {
                                soundstring.concat(action);
                            }
                        }catch (Exception e){
                            System.out.println("Issue " + e);
                        }

                        System.out.println(jsonObject.toString());

                    }
                    if(jsonObject.get("type").equals("speak")){
                        JSONObject jsonObject1 = jsonObject.getJSONObject("data");
                        String utterance = jsonObject1.get("utterance").toString();
                        System.out.println("UTTERANCE: "+ utterance);
                        mTTS.speak(utterance, TextToSpeech.QUEUE_FLUSH, null);
                        // TODO: Wiedergabe der Soundfiles
                    }
                    if (jsonObject.get("type").equals("loomoInstruction")){
                        try {
                            JSONObject jsonData = jsonObject.getJSONObject("data");
                            System.out.println(jsonData.toString());
                            String action = jsonData.get("action").toString();
                            //detectTurnDirection(direction);
                            if (action.equals("turn")){
                                String direction = jsonData.get("direction").toString();
                                detectTurnDirection(direction);
                            }
                            else if (action.equals("goPlace")){
                                mBase.setControlMode(Base.CONTROL_MODE_RAW);
                                String destination = jsonData.get("destination").toString();
                                goTo(destination);

                            }
                            else if (action.equals("getItem")){
                                mBase.setControlMode(Base.CONTROL_MODE_RAW);
                                String item = jsonData.get("item").toString();
                                getItem(item);
                            }
                            else {
                                System.out.println("No idea what to do");
                                // TODO: implement action
                            }
                        }
                        catch (Throwable t){
                            System.out.println("Issue: " + t);
                        }
                    }
                }
                catch (Throwable t){
                    System.out.println("Malformed JSON");
                }

            }
            @Override
            public void onClose(int i, String s, boolean b){
                Log.i("WebSocket", "Closed" + s);
            }

            @Override
            public void onError(Exception e){
                Log.i("WebSocket", "Error "+ e.getMessage());
            }
        };
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

    public void goTo(String destination){
        new Thread(){
            @Override
            public void run(){
                mBase.setLinearVelocity(1);
                try {
                    // sets for how long the robot is turning with that speed
                    Thread.sleep(2500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mBase.setLinearVelocity(0);
            }
        }.start();

    }

    public void getItem(String item){
        new Thread(){
            @Override
            public void run(){
                mBase.setLinearVelocity(1);
                turn((float)4.4);
                mBase.setLinearVelocity(1);

            }
        }.start();
    }

    public void detectTurnDirection(String direction) {
        mBase.setControlMode(Base.CONTROL_MODE_RAW);
        switch (direction) {
            case "left":
                turn((float) 2.2);
                break;
            case "right":
                turn((float) -2.2);
                break;
            case "around":
                turn((float) 4.4);
                break;
            default:
                break;
        }
    }

    public void turn(final float velocity) {
        new Thread() {
            @Override
            public void run() {
                System.out.println(mHead.getHeadJointYaw());
                mHead.setMode(Head.MODE_ORIENTATION_LOCK);
                mBase.setAngularVelocity(velocity);
                mHead.setYawAngularVelocity(velocity);
                try {
                    // sets for how long the robot is turning with that speed
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // make the robot stop
                mBase.setAngularVelocity(0);
                mHead.setYawAngularVelocity(0);
                mHead.setMode(Head.MODE_SMOOTH_TACKING);
            }
        }.start();
    }

    public void initateDetect() {
        new Thread(){
            @Override
            public void run(){
                //mDts.startDetectingPerson(mPersonDetectListener);
                //DTSPerson person[] = mDts.detectPersons(3 * 1000 * 1000);
                //System.out.println("PErsons: " + person.length);
                //    mVisionBindStateListener.onBind();
                if (isVisionBind) {
                    Person[] persons = mDts.detectPersons(3 * 1000 * 1000);
                    System.out.println("PErsoons: " + persons.length);
                    mWebSocketClient.send("{\"loomo\": \"personDetect\"}");
                    if (persons.length > 0){
                        turn(2);
                        turn(-4);
                        turn(2);
                    }
                } else {
                    System.out.println("not bound");
                }
            }
        }.start();

    }



    private class RecordingRunnable implements Runnable {

        @Override
        public void run() {
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
                }
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
