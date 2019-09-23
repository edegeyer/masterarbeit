package com.segway.robot.followsample.presenter;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.Surface;

import com.segway.robot.algo.dts.BaseControlCommand;
import com.segway.robot.algo.dts.DTSPerson;
import com.segway.robot.algo.dts.PersonDetectListener;
import com.segway.robot.algo.dts.PersonTrackingListener;
import com.segway.robot.algo.dts.PersonTrackingProfile;
import com.segway.robot.algo.dts.PersonTrackingWithPlannerListener;
import com.segway.robot.followsample.CustomApplication;
import com.segway.robot.followsample.R;
import com.segway.robot.followsample.interfaces.PresenterChangeInterface;
import com.segway.robot.followsample.interfaces.ViewChangeInterface;
import com.segway.robot.followsample.util.HeadControlHandlerImpl;
import com.segway.robot.followsample.view.AutoFitDrawableView;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.locomotion.head.Head;
import com.segway.robot.sdk.locomotion.sbv.Base;
import com.segway.robot.sdk.vision.DTS;
import com.segway.robot.sdk.vision.Vision;
import com.segway.robot.support.control.HeadPIDController;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;


/**
 * @author jacob
 * @date 5/29/18
 */

class DetectedPerson{
    public long timestamp = System.currentTimeMillis();
    public Boolean asked = false;
}

public class FollowMePresenter {

    private static final String TAG = "FollowMePresenter";

    private static final int TIME_OUT = 10 * 1000;

    private PresenterChangeInterface mPresenterChangeInterface;
    private ViewChangeInterface mViewChangeInterface;


    private HeadPIDController mHeadPIDController = new HeadPIDController();
    private Vision mVision;
    private Head mHead;
    private Base mBase;


    private boolean isVisionBind;
    private boolean isHeadBind;
    private boolean isBaseBind;

    private DTS mDts;

    //private List personList = Collections.synchronizedList(new ArrayList());
    private DetectedPerson detectedPerson = new DetectedPerson();
    private long startTime;

    private RobotStateType mCurrentState;

    // audio variables
    private boolean keepGoing = true;
    private static final int SAMPLING_RATE_IN_HZ = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_DEFAULT;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE_FACTOR = 10;
    /**
     * Size of the buffer where the audio data is stored by Android
     */
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE_IN_HZ,
            CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR;
    private final AtomicBoolean recordingInProgress = new AtomicBoolean(false);
    MediaPlayer mplayer = new MediaPlayer();
    private AudioRecord recorder = null;

    private Thread recordingThread = null;
    WebSocketClient mWebSocketClient;
    private String lastCommand;
    private String SERVER = "192.168.0.109";
    //private String SERVER = "192.168.178.31";
    private int PORT = 65432;
    String messageBusAddress = "ws://192.168.0.109:8181/core";
    private int audioDuration;
    private AudioManager audioManager;


    //String messageBusAddress = "ws://192.168.178.31:8181/core";

    public enum RobotStateType {
        INITIATE_DETECT, TERMINATE_DETECT;
    }

    public FollowMePresenter(PresenterChangeInterface mPresenterChangeInterface, ViewChangeInterface mViewChangeInterface) {
        this.mPresenterChangeInterface = mPresenterChangeInterface;
        this.mViewChangeInterface = mViewChangeInterface;
    }

    public void startPresenter() {
        mVision = Vision.getInstance();
        mHead = Head.getInstance();
        mBase = Base.getInstance();
        mVision.bindService(CustomApplication.getContext(), mVisionBindStateListener);
        mHead.bindService(CustomApplication.getContext(), mHeadBindStateListener);
        mBase.bindService(CustomApplication.getContext(), mBaseBindStateListener);
        audioManager = (AudioManager) CustomApplication.getContext().getSystemService(Context.AUDIO_SERVICE);
        //detectedPerson.asked = false;
        //detectedPerson.time = Calendar.getInstance().getTime();
        connectWebSocket();
        mWebSocketClient.connect();
        startRecording();

        /**
         * the second parameter is the distance between loomo and the followed target. must > 1.0f
         */
    }


    public void stopPresenter() {
        if (mDts != null) {
            mDts.stop();
            mDts = null;
        }
        mVision.unbindService();
        mHeadPIDController.stop();
        mHead.unbindService();
        //mBase.unbindService();
    }

    /******************************************* audio actions ************************************/
    private void connectWebSocket() {
        URI uri;
        try {
            uri = new URI(messageBusAddress);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                System.out.println("WebSocket opened");
            }

            @Override
            public void onMessage(String message) {
                //System.out.println(message);

                try {
                    final JSONObject jsonObject = new JSONObject(message);

                    if (jsonObject.get("type").equals("recognizer_loop:record_begin")) {
                        // play sound, when wakeword was detected -> user knows then, that loomo is listening
                        MediaPlayer mPlayer = MediaPlayer.create(CustomApplication.getContext(), R.raw.start_listening);
                        mPlayer.start();
                    } else if (jsonObject.get("type").equals("mycroft.skill.handler.start")) {
                        // make loomo stop during an action
                        JSONObject jsonData = jsonObject.getJSONObject("data");
                        if (jsonData.get("name").equals("StopSkill.handle_stop")) {
                            System.out.println("STOP");
                            stop();
                        }
                    } else if (jsonObject.get("type").equals("loomoInstruction")) {
                        try {
                            JSONObject jsonData = jsonObject.getJSONObject("data");
                            String action = jsonData.get("action").toString();
                            mBase.setControlMode(Base.CONTROL_MODE_RAW);

                            switch (action) {
                                case "turn":
                                    String direction = jsonData.get("direction").toString();
                                    lastCommand = action + " " +direction;
                                    detectTurnDirection(direction);
                                    break;
                                case "goPlace":
                                    String destination = jsonData.get("destination").toString();
                                    lastCommand = action;
                                    goTo(destination);
                                    break;
                                case "getItem":
                                    String item = jsonData.get("item").toString();
                                    lastCommand = action;
                                    getItem(item);
                                    break;
                                case "outofway":
                                    lastCommand = action;
                                    outofway();
                                    break;
                                case "comeback":
                                    comeback(lastCommand);
                                    lastCommand = "";
                                    break;

                                case "straight":
                                    lastCommand = action;
                                    goStraight();
                                    break;
                                default:
                                    System.out.println("No idea what to do");
                                    break;
                            }
                        } catch (Throwable t) {
                            System.out.println("Issue: " + t);
                        }
                    }
                } catch (Throwable t) {
                    System.out.println("Malformed JSON");
                }

            }

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.i("WebSocket", "Closed" + s);
            }

            @Override
            public void onError(Exception e) {
                Log.i("WebSocket", "Error " + e.getMessage());
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
        startAudioProcessingServer();
    }

    public void startAudioProcessingServer() {
        final ExecutorService clientProcessingPool = Executors.newFixedThreadPool(10);

        Runnable serverTask = new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket serverSocket = new ServerSocket(65433);
                    System.out.println("Created socket. Listening....");

                    while (true) {
                        Socket clientSocket = serverSocket.accept();
                        clientProcessingPool.submit(new ClientTask((clientSocket)));
                    }
                } catch (IOException e) {
                    System.out.println("Unable to process client reuqest");
                    e.printStackTrace();
                }

            }
        };
        Thread serverThread = new Thread(serverTask);
        serverThread.start();
    }

    private class ClientTask implements Runnable {


        private final Socket clientSocket;
        MediaPlayer mediaPlayer;

        private ClientTask(Socket clientSocket) {
            this.clientSocket = clientSocket;

        }

        @Override
        public void run() {
            System.out.println("Got a client");

            File file = new File(CustomApplication.getContext().getCacheDir(), "cachedAudio.wav");
            try (OutputStream output = new FileOutputStream(file)) {
                DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = dataInputStream.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
                String path = CustomApplication.getContext().getCacheDir().getPath().concat("/cachedAudio.wav");
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(path);
                mediaPlayer.prepare();
                audioDuration = mediaPlayer.getDuration();
                // mute microphone for the duration of the audio
                audioManager.setMicrophoneMute(true);
                mediaPlayer.start();
                try {
                    Thread.sleep(audioDuration);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // reactivate micro when audio finished played
                audioManager.setMicrophoneMute(false);
                output.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                System.out.println("closing connecting");
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class RecordingRunnable implements Runnable {

        @Override
        public void run() {
            try {
                Socket s = new Socket(SERVER, PORT);
                DataOutputStream dataOutputStream = new DataOutputStream(s.getOutputStream());
                final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
                while (recordingInProgress.get()) {
                    int result = recorder.read(buffer, BUFFER_SIZE);
                    if (result < 0) {
                        throw new RuntimeException("Reading of audio buffer failed" +
                                getBufferReadFailureReason(result));
                    }
                    dataOutputStream.write(buffer.array(), 0, BUFFER_SIZE);
                    buffer.clear();
                }
                s.close();
            } catch (IOException e) {
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

    /******************************************* functions to control loomo ***********************/
    public void goTo(String destination) {
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (keepGoing) {
                    mBase.setLinearVelocity(1);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mBase.setLinearVelocity(0);
                }
            }
        }.start();

    }

    public void outofway() {
        new Thread() {
            @Override
            public void run() {

                if (keepGoing) {
                    detectTurnDirection("left");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (keepGoing) {
                    mBase.setLinearVelocity(1);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                mBase.setLinearVelocity(0);
                keepGoing = true;
            }
        }.start();
    }

    public void getItem(String item) {
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (keepGoing) {
                    goStraight();
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (keepGoing) {
                    detectTurnDirection("around");
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (keepGoing) {
                    goStraight();
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                mBase.setLinearVelocity(0);
                mBase.setAngularVelocity(0);
                keepGoing = true;
            }
        }.start();
    }

    public void detectTurnDirection(String direction) {
        switch (direction) {
            case "left":
                turn((float) 2.2);
                break;
            case "right":
                turn((float) -2.2);
                break;
            case "around":
                turn((float) 4.8);
                break;
            default:
                break;
        }
    }

    public void turn(final float velocity) {
        new Thread() {
            @Override
            public void run() {

                mBase.setLinearVelocity(0);
                mBase.setAngularVelocity(velocity);
                try {
                    // sets for how long the robot is turning with that speed
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // make the robot stop
                mBase.setAngularVelocity(0);
            }
        }.start();
    }


    private void comeback(String lastCommand){
        System.out.println("last command is: " + lastCommand);
        switch (lastCommand){
            case "straight":
                detectTurnDirection("around");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                goStraight();
                break;
            case "outofway":
                detectTurnDirection("around");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mBase.setLinearVelocity(1);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mBase.setLinearVelocity(0);
                detectTurnDirection("left");
                break;
            case "getItem":
                // can be empty, because robot should be back with the user
                break;
            case "goPlace":
                detectTurnDirection("around");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mBase.setLinearVelocity(1);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mBase.setLinearVelocity(0);
                break;
            case "turn left":
                detectTurnDirection("right");
                break;
            case "turn right":
                detectTurnDirection("left");
                break;
            case "turn around":
                detectTurnDirection("around");
                break;
            default:
                break;
        }
        // set it to empty string
        this.lastCommand = "";
    }

    private void goStraight(){
        new Thread() {
            @Override
            public void run() {
                mBase.setLinearVelocity(2);
                try {
                    Thread.sleep(600);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mBase.setLinearVelocity(2);
                try {
                    Thread.sleep(600);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mBase.setLinearVelocity(0);
            }
        }.start();
    }

    private void stop() {
        keepGoing = false;
        mBase.setLinearVelocity(0);
        mBase.setAngularVelocity(0);

    }

    public void actionInitiateDetect() {
        if (mCurrentState == RobotStateType.INITIATE_DETECT) {
            return;
        }
        startTime = System.currentTimeMillis();
        mCurrentState = RobotStateType.INITIATE_DETECT;
        mDts.startDetectingPerson(mPersonDetectListener);
        mPresenterChangeInterface.showToast("initiate detecting....");
    }

    public void actionTerminateDetect() {
        if (mCurrentState == RobotStateType.INITIATE_DETECT) {
            mCurrentState = RobotStateType.TERMINATE_DETECT;
            mDts.stopDetectingPerson();
            mPresenterChangeInterface.showToast("terminate detecting....");
        } else {
            mPresenterChangeInterface.showToast("The app is not in detecting mode yet.");
        }
    }



    /**************************  detecting and tracking listeners   *****************************/

    private PersonDetectListener mPersonDetectListener = new PersonDetectListener() {
        @Override
        public void onPersonDetected(DTSPerson[] persons) {
            if (persons == null || persons.length == 0) {
                detectedPerson.asked = false;

                if (System.currentTimeMillis() - startTime > TIME_OUT) {
                    resetHead();
                    // when person left the camera area, assume person was never asked
                }
                return;
            }
            startTime = System.currentTimeMillis();
            mPresenterChangeInterface.drawPersons(persons);


            if (!detectedPerson.asked){
                // ask person, if first seen
                detectedPerson.asked = true;
                detectedPerson.timestamp = System.currentTimeMillis();
                mWebSocketClient.send("{\"type\": \"recognizer_loop:utterance\"," +
                        "\"data\":{\"utterances\":[\"loomo1234567\"], \"lang\": \"en-us\"}, \"context\":{}}");
            }
            else{
                // when person wasn't seen for defined time, ask again if help can be provided
                long timedifference = System.currentTimeMillis() - detectedPerson.timestamp;
                // check if person was asked over 30s before
                System.out.println("ASKED BEFORE " +timedifference/1000);
                if (timedifference > 30000){
                    // TODO: ask again
                    System.out.println("ASKING AGAIN");
                    detectedPerson.timestamp = System.currentTimeMillis();
                    mWebSocketClient.send("{\"type\": \"recognizer_loop:utterance\"," +
                            "\"data\":{\"utterances\":[\"loomo1234567\"], \"lang\": \"en-us\"}, \"context\":{}}");
                }
            }

            if (isServicesAvailable()) {
                mHead.setMode(Head.MODE_ORIENTATION_LOCK);
                mHeadPIDController.updateTarget(persons[0].getTheta(), persons[0].getDrawingRect(), 480);
            }

        }

        @Override
        public void onPersonDetectionResult(DTSPerson[] person) {

        }

        @Override
        public void onPersonDetectionError(int errorCode, String message) {
            mCurrentState = null;
            mPresenterChangeInterface.showToast("PersonDetectListener: " + message);
        }
    };



    /***************************************** bind services **************************************/

    private ServiceBinder.BindStateListener mVisionBindStateListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            isVisionBind = true;
            mDts = mVision.getDTS();
            mDts.setVideoSource(DTS.VideoSource.CAMERA);
            AutoFitDrawableView autoFitDrawableView = mViewChangeInterface.getAutoFitDrawableView();
            Surface surface = new Surface(autoFitDrawableView.getPreview().getSurfaceTexture());
            mDts.setPreviewDisplay(surface);
            mDts.start();
            checkAvailable();
        }

        @Override
        public void onUnbind(String reason) {
            isVisionBind = false;
            mPresenterChangeInterface.showToast("Vision service: " + reason);
        }
    };

    private ServiceBinder.BindStateListener mHeadBindStateListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            isHeadBind = true;
            resetHead();
            mHeadPIDController.init(new HeadControlHandlerImpl(mHead));
            mHeadPIDController.setHeadFollowFactor(1.0f);
            checkAvailable();
        }

        @Override
        public void onUnbind(String reason) {
            isHeadBind = false;
            mPresenterChangeInterface.showToast("Head service: " + reason);
        }
    };
    private ServiceBinder.BindStateListener mBaseBindStateListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            isBaseBind = true;
            checkAvailable();
        }

        @Override
        public void onUnbind(String reason) {
            isBaseBind = false;
            mPresenterChangeInterface.showToast("Base service: " + reason);
        }
    };


    public boolean isServicesAvailable() {
        return isVisionBind && isHeadBind && isBaseBind;
    }

    private void checkAvailable() {
        if (isServicesAvailable()) {
            mPresenterChangeInterface.dismissLoading();
        }
    }

    /**
     * reset head when timeout
     */
    private void resetHead() {
        mHead.setMode(Head.MODE_SMOOTH_TACKING);
        mHead.setWorldYaw(0);
        mHead.setWorldPitch(0.7f);
    }

}
