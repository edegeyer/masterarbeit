package com.mycroft.socketprotoyp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity {


    String websocket = "http://192.168.178.32";
    String wsPort = "8181";
    WebSocketClient mWebSocketClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        connectWebSocket();
        mWebSocketClient.connect();
    }


    private void connectWebSocket(){
        URI uri;
        try {
            // standard myCroft Messagebus URI: ws://IP:8181/core
            uri = new URI("ws://192.168.178.32:8181/core");
            System.out.println("URI is: "+ uri);
        }
        catch (URISyntaxException e){
            e.printStackTrace();
            return;
        }

        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake){
                System.out.println("WebSocket Opened");
            }

            @Override
            public void onMessage(String s){
                final String message = s;
                System.out.println("message is: " + message);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView textView = (TextView)findViewById(R.id.messages);
                        textView.setText(textView.getText() + "\n" + message);
                        System.out.println("received Message: "+ message);
                    }
                });
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
}
