package com.example.a4inarowgame;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.io.BufferedReader;
import java.io.IOException;

public class MainReceiveMessageFromServer implements Runnable {

    MainActivity parent;
    BufferedReader br;
    String myTurn;

    public MainReceiveMessageFromServer(MainActivity parent) {
        this.parent = parent;
        this.br = parent.getBr();
        myTurn = "false";
    }

    @Override
    public void run() {
        while (true) {
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
                            AlertDialog.Builder acceptance = new AlertDialog.Builder(parent);

                            acceptance.setTitle("Request for game");
                            acceptance.setMessage("Accept request from " + rival + "?");

                            acceptance.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    parent.sendMessage(parent.getEditUsername() + ":accepts:" + rival);

                                    //Close the dialog
                                    dialog.dismiss();
                                }
                            });
                            acceptance.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    parent.sendMessage(parent.getEditUsername() + ":doesnt accept:" + rival);

                                    //Close the dialog
                                    dialog.dismiss();
                                }
                            });

                            AlertDialog alert = acceptance.create();
                            alert.show();
                        }
                    });
                } else if (line.equals("rejected")) {
                    parent.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            parent.spinnerUsers.setEnabled(true);
                            parent.buttonChoose.setEnabled(true);
                        }
                    });
                } else if (line.startsWith("First move:")) {
                    //game starts
                    String first = line.split(":")[1];

                    if (first.equals(parent.getEditUsername()))
                        myTurn = "true";
                    else
                        myTurn = "false";

                    parent.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            parent.startGame(myTurn);
                        }
                    });
                    return;
                }
            } catch (IOException ex) {
                MainActivity.serverNotAvailable();

                //parent.logout(); //disconnect from server and reset first state
                return;
            }
        }
    }
}
