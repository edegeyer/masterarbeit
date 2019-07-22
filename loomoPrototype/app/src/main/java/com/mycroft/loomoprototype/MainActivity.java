package com.mycroft.loomoprototype;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.locomotion.sbv.Base;

import java.util.Timer;

public class MainActivity extends AppCompatActivity {

    Base mBase;
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


    }

    public void setUpLoomo(){
        mBase=Base.getInstance();
        mBase.bindService(this.getBaseContext(), new ServiceBinder.BindStateListener() {
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
}
