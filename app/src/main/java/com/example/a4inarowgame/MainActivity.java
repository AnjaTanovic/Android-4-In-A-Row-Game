package com.example.a4inarowgame;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    Button buttonConnect;
    TextView textUsername;
    EditText editUsername;
    Button buttonName;
    TextView textChoose;
    Spinner spinnerUsers;
    Button buttonChoose;

    private Socket socket;

    private BufferedReader br;
    private PrintWriter pw;

    public static String REQUEST_MESSAGE = "Request_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        System.out.println("Client started.");

        buttonConnect = (Button) findViewById(R.id.buttonConnect);
        textUsername = (TextView) findViewById(R.id.textUsername);
        textUsername.setVisibility(View.INVISIBLE);
        editUsername = (EditText) findViewById(R.id.editUsername);
        editUsername.setVisibility(View.INVISIBLE);
        buttonName = (Button) findViewById(R.id.buttonName);
        buttonName.setVisibility(View.INVISIBLE);
        textChoose = (TextView) findViewById(R.id.textChoose);
        textChoose.setVisibility(View.INVISIBLE);
        spinnerUsers = (Spinner) findViewById(R.id.spinnerUsers);
        spinnerUsers.setVisibility(View.INVISIBLE);
        buttonChoose = (Button) findViewById(R.id.buttonChoose);
        buttonChoose.setVisibility(View.INVISIBLE);

        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Create new socket on local host
                connectToServer();
            }
        });

        buttonName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!MainActivity.this.editUsername.getText().toString().equals("")){

                    sendMessage(MainActivity.this.editUsername.getText().toString());

                    //create new thread to react on server's messages
                    new Thread(new MainReceiveMessageFromServer(MainActivity.this)).start();

                    MainActivity.this.buttonName.setEnabled(false);
                    MainActivity.this.editUsername.setEnabled(false);

                    MainActivity.this.textChoose.setVisibility(View.VISIBLE);
                    MainActivity.this.spinnerUsers.setVisibility(View.VISIBLE);
                    MainActivity.this.buttonChoose.setVisibility(View.VISIBLE);

                }
            }
        });

        buttonChoose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = MainActivity.this.editUsername.getText().toString() + ":chooses:" +
                        MainActivity.this.spinnerUsers.getSelectedItem().toString();
                sendMessage(message);

                MainActivity.this.spinnerUsers.setEnabled(false);
                MainActivity.this.buttonChoose.setEnabled(false);
            }
        });
    }

    public BufferedReader getBr() {
        return br;
    }

    public Spinner getSpinnerUsers() {
        return spinnerUsers;
    }

    public String getEditUsername() {
        return editUsername.getText().toString();
    }

    public void connectToServer(){

        new Thread(new Runnable() {
            @Override
            public void run() {
                Singleton singleton = Singleton.getInstance();
                if (singleton != null){
                    MainActivity.this.socket = singleton.socket;
                    MainActivity.this.br = singleton.br;
                    MainActivity.this.pw = singleton.pw;

                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.this.buttonConnect.setEnabled(false);

                            MainActivity.this.textUsername.setVisibility(View.VISIBLE);
                            MainActivity.this.editUsername.setVisibility(View.VISIBLE);
                            MainActivity.this.buttonName.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        }).start();
    }

    public static void serverNotAvailable() {
        System.out.println("Server is not available");
    }

    public static void serverAvailable() {
        System.out.println("Connected on server");
    }
    public void sendMessage(String message){

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (MainActivity.this.pw != null){
                    MainActivity.this.pw.println(message);
                    System.out.println("Message to server: " + message);
                }
            }
        }).start();
    }

    ActivityResultLauncher<Intent> activity2Launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK){
                        Intent data = result.getData();
                        String response;
                        response = data.getStringExtra(GameActivity.RESPONSE_MESSAGE);
                    }
                }
            }
    );

    public void startGame() {
        Intent intent = new Intent(MainActivity.this, GameActivity.class);
        intent.putExtra(REQUEST_MESSAGE, "hello");
        activity2Launcher.launch(intent);
    }
}