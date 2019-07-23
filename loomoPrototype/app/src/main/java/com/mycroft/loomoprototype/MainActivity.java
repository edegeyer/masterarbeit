package com.mycroft.loomoprototype;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.Surface;
import android.view.View;
import android.widget.Button;


import com.segway.robot.algo.dts.DTSPerson;
import com.segway.robot.algo.dts.PersonDetectListener;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.locomotion.head.Head;
import com.segway.robot.sdk.locomotion.sbv.Base;
import com.segway.robot.sdk.vision.DTS;
import com.segway.robot.sdk.vision.Vision;
import com.segway.robot.support.control.HeadPIDController;

import java.util.Timer;

public class MainActivity extends AppCompatActivity {

    Base mBase;
    Head mHead;
    private Vision mVision;

    private DTS mDts;
    private HeadPIDController mHeadPIDController = new HeadPIDController();
    private boolean isVisionBind;
    private boolean isHeadBind;
    private boolean isBaseBind;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUpLoomo();
        final Button turnLeftButton = findViewById(R.id.turnLeftButton);
        final Button turnRightButton = findViewById(R.id.turnRightButton);
        final Button turnAroundButton = findViewById(R.id.turnAroundButton);
        final Button checkAudioButton = findViewById(R.id.checkAudioButton);
        final Button simulatePersonDetectedButton = findViewById(R.id.detectedpersonButton);

        // Head ist standardmäßig auf smooth eingestellt, sprich der Kopf wird entsprechen mitbewegt, aber leicht verzögert
        // TODO: bewegen des Kopfes mit der gleichen Geschwindigkeit wie den restlichen Körper
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
            }
        });

        simulatePersonDetectedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO: Zugriff auf die Kamera und Erkennung von Personen durchführen
                //TODO: wenn Person erkannt: links&rechts drehen zum signalisieren
                trackPerson();
                initateDetect();
                //TODO: wenn Mycroft eingebunden: Ansprechen der Person
            }
        });


    }

    public boolean isServiceAvailable(){
        return isVisionBind && isHeadBind && isBaseBind;
    }


    private PersonDetectListener mPersonDetectListener = new PersonDetectListener() {
        @Override
        public void onPersonDetected(DTSPerson[] person) {
            if (isServiceAvailable()){
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

    public void initateDetect(){
        mDts.startDetectingPerson(mPersonDetectListener);
    }





    public void setUpLoomo(){
        // setup the base
        mBase=Base.getInstance();
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
        mVision.bindService(this.getApplicationContext(), new ServiceBinder.BindStateListener() {
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
        });
    }

    public void trackPerson(){

        //TODO: Reaktion auf entdeckte Person: ohne Sprache: wackeln der Base
        //TODO: Aktion, wenn es nicht die gewünschte Person ist (optional)

    }

    public void detectTurnDirection(String direction){
        Timer mTimer = new Timer();
        mBase.setControlMode(Base.CONTROL_MODE_RAW);
        switch (direction) {
            case "left":
                turn((float)2.2);
                break;
            case "right":
                turn((float)-2.2);
                break;
            case "around":
                turn((float)4.4);
                break;
            default:
                break;
        }
    }

    public void turn(final float velocity){
        new Thread(){
            @Override
            public void run(){
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
}
