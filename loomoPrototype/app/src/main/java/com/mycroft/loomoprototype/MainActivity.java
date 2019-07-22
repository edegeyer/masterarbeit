package com.mycroft.loomoprototype;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.locomotion.head.Head;
import com.segway.robot.sdk.locomotion.sbv.Base;

import java.util.Timer;

public class MainActivity extends AppCompatActivity {

    Base mBase;
    Head mHead;
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

                //TODO: wenn Mycroft eingebunden: Ansprechen der Person
            }
        });


    }

    public void setUpLoomo(){
        // setup the base
        mBase=Base.getInstance();
        mBase.bindService(this.getBaseContext(), new ServiceBinder.BindStateListener() {
            @Override
            public void onBind() {

            }

            @Override
            public void onUnbind(String reason) {

            }
        });
        // setup the head
        mHead = Head.getInstance();
        mHead.bindService(this.getApplicationContext(), new ServiceBinder.BindStateListener() {
            @Override
            public void onBind() {

            }

            @Override
            public void onUnbind(String reason) {

            }
        });
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
