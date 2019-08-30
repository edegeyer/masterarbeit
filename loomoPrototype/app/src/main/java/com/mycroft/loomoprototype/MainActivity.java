package com.mycroft.loomoprototype;

import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;


import com.segway.robot.algo.dts.DTSPerson;
import com.segway.robot.algo.dts.Person;
import com.segway.robot.algo.dts.PersonDetectListener;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.locomotion.head.Head;
import com.segway.robot.sdk.locomotion.sbv.Base;
import com.segway.robot.sdk.vision.DTS;
import com.segway.robot.sdk.vision.Vision;
import com.segway.robot.support.control.HeadPIDController;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    Base mBase;
    Head mHead;
    private Vision mVision;

    private DTS mDts;
    private HeadPIDController mHeadPIDController = new HeadPIDController();
    private boolean isVisionBind;
    private boolean isHeadBind;
    private boolean isBaseBind;

    // Variables for recording audio and sending it over to a server
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
    WebSocketClient mWebSocketClient;
    // data for the mycroft core server including a port that's different from the messagebus port
    private String SERVER = "192.168.178.31";
    private int PORT  = 65432;
    // address of the message bus used by the core instance
    String messageBusAddress = "ws://192.168.178.31:8181/core";


    final MediaRecorder mediaRecorder = new MediaRecorder();
    private static String fileName = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUpLoomo();
        connectWebSocket();
        mWebSocketClient.connect();
        final Button turnLeftButton = findViewById(R.id.turnLeftButton);
        final Button turnRightButton = findViewById(R.id.turnRightButton);
        final Button turnAroundButton = findViewById(R.id.turnAroundButton);
        final Button checkAudioButton = findViewById(R.id.checkAudioButton);
        final Button simulatePersonDetectedButton = findViewById(R.id.detectedpersonButton);
        fileName = getExternalCacheDir().getAbsolutePath();
        fileName += "/audiorecordtest.3gp";
        turnLeftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                detectTurnDirection("left");
            }
        });
        turnAroundButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                detectTurnDirection("around");
            }
        });
        turnRightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                detectTurnDirection("right");
            }
        });
        checkAudioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO: zugriff auf die Mikrofone und ausgeben der Richtung, aus der die Geräusche kommen
                // TODO: optional: loomo dreht sich entsprechend in die Richtung der Geräusche
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mediaRecorder.setOutputFile(fileName);
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                try {
                    mediaRecorder.prepare();

                } catch (Exception e){
                    System.out.println("Error occured: " + e);
                }
                mediaRecorder.start();
                try {
                    Thread.sleep(1000);
                } catch (Exception e){
                    System.out.println("Can't go to sleep");
                }
                mediaRecorder.stop();

                //TODO: play the recorded file
                //TODO: send the data as a byte stream

            }
        });

        simulatePersonDetectedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO: Zugriff auf die Kamera und Erkennung von Personen durchführen
                //TODO: wenn Person erkannt: links&rechts drehen zum signalisieren
                initateDetect();
                //TODO: wenn Mycroft eingebunden: Ansprechen der Person
            }
        });
        startRecording();

    }

    public boolean isServiceAvailable() {
        return isVisionBind && isHeadBind && isBaseBind;
    }


    private PersonDetectListener mPersonDetectListener = new PersonDetectListener() {
        @Override
        public void onPersonDetected(DTSPerson[] person) {
            if (isServiceAvailable()) {
                mHead.setMode(Head.MODE_ORIENTATION_LOCK);
                mHeadPIDController.updateTarget(person[0].getTheta(), person[0].getDrawingRect(), 480);
            }
        }

        @Override
        public void onPersonDetectionResult(DTSPerson[] person) {

        }

        @Override
        public void onPersonDetectionError(int errorCode, String message) {

        }
    };

    public void initateDetect() {
        //mDts.startDetectingPerson(mPersonDetectListener);
        //DTSPerson person[] = mDts.detectPersons(3 * 1000 * 1000);
        //System.out.println("PErsons: " + person.length);
    //    mVisionBindStateListener.onBind();
        if (isVisionBind) {
            Person[] persons = mDts.detectPersons(3 * 1000 * 1000);
            System.out.println("PErsoons: " + persons.length);
            if (persons.length > 0){
                turn(2);
                turn(-4);
                turn(2);
            }
        } else {
            System.out.println("not bound");
        }
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




    private ServiceBinder.BindStateListener mVisionBindStateListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            isVisionBind = true;
            mDts = mVision.getDTS();
            mDts.setVideoSource(DTS.VideoSource.CAMERA);
            mDts.start();

        }

        @Override
        public void onUnbind(String reason) {
            isVisionBind = false;

        }
    };

    public void detectTurnDirection(String direction) {
        Timer mTimer = new Timer();
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

    private void connectWebSocket(){
        URI uri;
        try {
            // TODO: adjust if connected to different network
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
                        // TODO: speak byte Stream

                        // TODO: call the required action of Loomo
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

    private void startRecording() {
        recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLING_RATE_IN_HZ,
                CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);
        recorder.startRecording();
        recordingInProgress.set(true);
        recordingThread = new Thread(new RecordingRunnable(), "Recording Thread");
        System.out.println("created Thread");
        recordingThread.start();
        System.out.println("startetd Thread");
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
