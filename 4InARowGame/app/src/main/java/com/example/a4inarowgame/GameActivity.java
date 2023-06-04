package com.example.a4inarowgame;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;

public class GameActivity extends AppCompatActivity {

    private Socket socket;
    private BufferedReader br;
    private PrintWriter pw;

    String username;
    boolean myTurn;
    String color;

    private final int rows = 6;
    private final int columns = 7;

    HashMap<String, ImageView> fields;
    LinearLayout layoutFields;
    TextView textTurn;
    ImageView yourColor;

    public static String RESPONSE_MESSAGE = "Response_text";
    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        intent = getIntent();
        String message = (String) intent.getExtras().getString(MainActivity.REQUEST_MESSAGE);
        String nameFirst[] = message.split(":");

        this.username = nameFirst[0];
        if (nameFirst[1].equals("true")) {
            this.myTurn = true;
            this.color = "red";
        }
        else {
            this.myTurn = false;
            this.color = "blue";
        }

        //get socket, br and pw from singleton
        connectToServer();

        fields = new HashMap<String, ImageView>();
        layoutFields = findViewById(R.id.layoutFields);
        for (int row = 0; row < rows; row++){
            LinearLayout llrow = new LinearLayout(this);
            llrow.setOrientation(LinearLayout.HORIZONTAL);
            for (int col = 0; col < columns; col++){
                ImageView iv = new ImageView(this);
                iv.setTag(row + "," + col + ",neutral");
                fields.put(row + "," + col, iv);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0,200);
                layoutParams.weight = 1;
                layoutParams.setMargins(10,0,10,0);
                iv.setLayoutParams(layoutParams);
                iv.setImageResource(R.drawable.neutral2);
                iv.setOnClickListener((v)->{
                    Toast.makeText(GameActivity.this, "Point  "+ v.getTag().toString() + " is clicked.", Toast.LENGTH_SHORT).show();
                    System.out.println(myTurn);
                    if (myTurn) {
                        String column = v.getTag().toString().split(",")[1];
                        sendMessage(username + ":" + column);
                        myTurn = false;
                    }
                });
                llrow.addView(iv);
            }
            layoutFields.addView(llrow);
        }
        textTurn = (TextView) findViewById(R.id.textTurn);
        yourColor = (ImageView) findViewById(R.id.yourColor);
        if (!myTurn) {
            textTurn.setText("Wait for your turn!");

            //first player has red color, second blue
            yourColor.setImageResource(R.drawable.blue);
        }

        //create new thread to react on server's messages
        new Thread(new GameReceiveMessageFromServer(GameActivity.this)).start();
    }

    public BufferedReader getBr() {
        return br;
    }

    public void connectToServer(){

        new Thread(new Runnable() {
            @Override
            public void run() {
                Singleton singleton = Singleton.getInstance();
                if (singleton != null){
                    GameActivity.this.socket = singleton.socket;
                    GameActivity.this.br = singleton.br;
                    GameActivity.this.pw = singleton.pw;
                }
                else {
                    System.out.println("Problem with socket, pw and br!");
                }
            }
        }).start();
    }

    public void sendMessage(String message){

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (GameActivity.this.pw != null){
                    GameActivity.this.pw.println(message);
                    System.out.println("Message to server: " + message);
                }
            }
        }).start();
    }
}