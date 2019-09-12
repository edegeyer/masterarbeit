
package com.example.audiorecord;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

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
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private boolean keepGoing = true;
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
    MediaPlayer mplayer = new MediaPlayer();
    private AudioRecord recorder = null;

    private Thread recordingThread = null;
    private Thread playerThread = null;

    TextToSpeech mTTS = null;
    WebSocketClient mWebSocketClient;

    private MediaPlayer player;

    private String SERVER = "192.168.0.109";
    //private String SERVER = "192.168.178.31";
    private int PORT = 65432;
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
        startRecording();
        // Should make sure, that loomo always tries to detect persons
        //initateDetect();

    }


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
                    //ImageView img = (ImageView) findViewById(R.id.imageView);

                    //check if a sound file has been sent
                    if (jsonObject.get("type").equals("recognizer_loop:record_begin")) {
                        MediaPlayer mPlayer = MediaPlayer.create(getApplicationContext(), R.raw.start_listening);
                        mPlayer.start();
                    } else if (jsonObject.get("type").equals("mycroft.skill.handler.start")) {
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
                                    detectTurnDirection(direction);
                                    break;
                                case "goPlace":
                                    String destination = jsonData.get("destination").toString();
                                    goTo(destination);
                                    break;
                                case "getItem":
                                    String item = jsonData.get("item").toString();
                                    getItem(item);
                                    break;
                                case "outofway":
                                    outofway();
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

    private void stop() {
        keepGoing = false;
        mBase.setLinearVelocity(0);
        mBase.setAngularVelocity(0);

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


    public void goTo(String destination) {
        new Thread() {
            @Override
            public void run() {
                mBase.setLinearVelocity(1);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mBase.setLinearVelocity(0);
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
                if (keepGoing) {
                    mBase.setLinearVelocity(1);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (keepGoing) {
                    detectTurnDirection("around");
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
                //mHead.setMode(Head.MODE_ORIENTATION_LOCK);
                mBase.setAngularVelocity(velocity);
                //mHead.setYawAngularVelocity(velocity);
                try {
                    // sets for how long the robot is turning with that speed
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // make the robot stop
                mBase.setAngularVelocity(0);
                //mHead.setYawAngularVelocity(0);
                //  mHead.setMode(Head.MODE_SMOOTH_TACKING);
            }
        }.start();
    }

    // TODO: funktioniert noch nicht
    public void initateDetect() {
        new Thread() {
            @Override
            public void run() {
                //mDts.startDetectingPerson(mPersonDetectListener);
                //DTSPerson person[] = mDts.detectPersons(3 * 1000 * 1000);
                //System.out.println("PErsons: " + person.length);
                //    mVisionBindStateListener.onBind();
                DTS dts = mVision.getDTS();
                dts.setVideoSource(DTS.VideoSource.CAMERA);
                dts.start();
                Person[] persons = dts.detectPersons(3 * 1000 * 1000);
                if (persons.length > 0) {
                    mplayer = MediaPlayer.create(getApplicationContext(), R.raw.beep);
                    mplayer.start();
                }

                /*
                if (isVisionBind) {
                    Person[] persons = mDts.detectPersons(3 * 1000 * 1000);
                    System.out.println("PErsoons: " + persons.length);
                    // TODO: Datenstruktur wie für eine utterance, nur so, dass das system denkt, es wurde eine utterance gesprochen
                    //mWebSocketClient.send("{\"loomo\": \"personDetect\"}");
                    if (persons.length > 0){
                        mplayer = MediaPlayer.create(getApplicationContext(), R.raw.beep);
                        mplayer.start();
                    }
                } else {
                    System.out.println("not bound");
                }*/
            }
        }.start();

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

            File file = new File(getCacheDir(), "cachedAudio.wav");
            try (OutputStream output = new FileOutputStream(file)) {
                DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = dataInputStream.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
                String path = getCacheDir().getPath().concat("/cachedAudio.wav");
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(path);
                mediaPlayer.prepare();
                mediaPlayer.start();
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
                dataOutputStream.write("Connected to mic".getBytes());
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
}
