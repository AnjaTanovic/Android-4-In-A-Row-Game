package com.example.a4inarowgame;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
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

    private boolean myTurn;

    private final int rows = 6;
    private final int columns = 7;

    HashMap<String, ImageView> fields;
    LinearLayout layoutFields;
    TextView textTurn;

    public static String RESPONSE_MESSAGE = "Response_text";
    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        intent = getIntent();
        String message = (String) intent.getExtras().getString(MainActivity.REQUEST_MESSAGE);
        if (message.equals("true"))
            this.myTurn = true;
        else
            this.myTurn = false;

        //create new thread to react on server's messages
        new Thread(new GameReceiveMessageFromServer(GameActivity.this)).start();

        fields = new HashMap<String, ImageView>();
        layoutFields = findViewById(R.id.layoutFields);
        for (int row = 1; row <= rows; row++){
            LinearLayout llrow = new LinearLayout(this);
            llrow.setOrientation(LinearLayout.HORIZONTAL);
            for (int col = 1; col <= columns; col++){
                ImageView iv = new ImageView(this);
                iv.setTag(row + "," + col);
                fields.put(row + "," + col, iv);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0,200);
                layoutParams.weight = 1;
                layoutParams.setMargins(10,0,10,0);
                iv.setLayoutParams(layoutParams);
                iv.setImageResource(R.drawable.neutral2);
                iv.setOnClickListener((v)->{
                    Toast.makeText(GameActivity.this, "Point  "+ v.getTag().toString() + " is clicked.", Toast.LENGTH_SHORT).show();
                });
                llrow.addView(iv);
            }
            layoutFields.addView(llrow);
        }
        textTurn = (TextView) findViewById(R.id.textTurn);
        if (!myTurn)
            textTurn.setText("Wait for your turn!");
    }

    public BufferedReader getBr() {
        return br;
    }
}