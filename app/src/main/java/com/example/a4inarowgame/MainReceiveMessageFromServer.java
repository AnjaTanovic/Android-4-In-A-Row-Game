package com.example.a4inarowgame;

import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.io.BufferedReader;
import java.io.IOException;

public class MainReceiveMessageFromServer implements Runnable {

    MainActivity parent;
    BufferedReader br;
    int clientState;
    boolean myTurn;

    public MainReceiveMessageFromServer(MainActivity parent) {
        this.parent = parent;
        this.br = parent.getBr();
        this.clientState = 0;
        myTurn = false;
    }

    @Override
    public void run() {
        while (true) {
            //0 - get all users
            //1 - get rival's move
            switch (clientState) {
                case 0:
                    try {
                        String line = this.br.readLine();

                        if (line.startsWith("Users:")) {
                            String[] names = line.split(":")[1].trim().split(" ");

                            parent.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // Clean Spinner
                                    parent.getSpinnerUsers().setAdapter(null);
                                    // Fill Spinner with new data (users)
                                    Spinner spinner = parent.getSpinnerUsers();
                                    // Create ArrayAdapter based on usernames from server's message
                                    // Set this adapter on spinner
                                    ArrayAdapter<String> adapter = new ArrayAdapter<>(parent, android.R.layout.simple_spinner_item, names);
                                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                    spinner.setAdapter(adapter);
                                }
                            });
                        } else if (line.startsWith("request from:")) {
                            String rival = line.split(":")[1];

                            parent.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    parent.sendMessage(parent.getEditUsername() + ":accepts:" + rival);
                                }
                            });
                        } else if (line.startsWith("First move:")) {
                            //game starts
                            String first = line.split(":")[1];

                            if (first.equals(parent.getEditUsername()))
                                myTurn = true;
                            else
                                myTurn = false;

                            parent.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    parent.startGame();
                                }
                            });

                            clientState++;
                        }
                    } catch (IOException ex) {
                        MainActivity.serverNotAvailable();

                        //parent.logout(); //disconnect from server and reset first state
                        return;
                    }
                    break;
                case 1:
                    //game is on

                    //return this thread (new will be started in GameActivity)
                    //or wait until game is finished
                    break;
                default:
                    break;
            }
        }
    }
}
