package com.mycroft.socketprotoyp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.*;

import java.net.URI;
import java.net.URISyntaxException;



public class MainActivity extends AppCompatActivity {


    WebSocketClient mWebSocketClient;
    boolean conntected = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectWebSocket();

        mWebSocketClient.connect();
        final Button mButton = findViewById(R.id.sendMessageButton);
        mButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                sendMessageToMycroft();
            }
        });
    }

    private void sendMessageToMycroft(){
        //TODO: 1. Send over a Message to Mycroft
        //TODO: 2. Create Mycroft to show a reaction
        //TODO: 3. enable parameters to be given to this function
        JSONObject messageJsonData = new JSONObject();
        JSONObject messageJson = new JSONObject();
        try{
            //messageJson.put("type", "loomoMessage");
            messageJsonData.put("utterances", "loomo discover");
            messageJsonData.put("type", "recognizer_loop:utterance");

            messageJsonData.put("context", "null");
            messageJson.put("data", messageJsonData);
            //messageJson.put("data","discoveredPerson");
        } catch (JSONException e){
            e.printStackTrace();
        }
        try{
            if (mWebSocketClient == null){
                //TODO: check for WIFI
                connectWebSocket();
            }
            try {
                String message = "{\"data\": {\"utterances\": [\"loomodiscover123456789\"]}, \"type\": \"recognizer_loop:utterance\", \"context\": null}";

                System.out.println("Going to send a message: " + messageJson.toString());
                System.out.println("Going to send 2 message: " + message);

                mWebSocketClient.send(message);
            } catch (Exception e){
                e.printStackTrace();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
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
                        //TODO: Show toast, that connection is made
                        System.out.println("it's connected");

                    }
                    else if (jsonObject.get("type").equals("loomoInstruction")) {
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
                conntected = false;
            }

            @Override
            public void onError(Exception e){
                Log.i("WebSocket", "Error "+ e.getMessage());
            }
        };
    }
}
