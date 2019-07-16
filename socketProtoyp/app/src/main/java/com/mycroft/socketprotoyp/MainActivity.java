package com.mycroft.socketprotoyp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.*;

import java.net.URI;
import java.net.URISyntaxException;



public class MainActivity extends AppCompatActivity {


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
            uri = new URI("ws://192.168.178.31:8181/core");
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
            public void onMessage(String message){
                try{
                    final JSONObject jsonObject = new JSONObject(message);
                    Log.d("JSON is: ", jsonObject.toString());

                    if (jsonObject.get("type").equals("connected")){
                        // TODO: Toast not showing
                        Toast.makeText(getBaseContext(), "connected", Toast.LENGTH_SHORT).show();

                        System.out.println("it's connected");

                    }
                    if (jsonObject.get("type").equals("loomoInstruction")) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TextView textView = findViewById(R.id.messages);
                                try {
                                    JSONObject jsonData = jsonObject.getJSONObject("data");
                                    String action = jsonData.get("action").toString();
                                    String direction = jsonData.get("direction").toString();
                                    textView.setText("Going to "+ action + " " + direction);
                                }
                                catch (Throwable t){
                                    Log.e("Nested JSON", "no transformation possible");
                                }
                            }
                        });
                    }
                }
                catch (Throwable t){
                    System.out.println("Malformed JSOn");
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
}
