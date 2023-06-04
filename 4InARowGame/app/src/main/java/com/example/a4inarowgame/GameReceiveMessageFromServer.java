package com.example.a4inarowgame;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;

public class GameReceiveMessageFromServer implements Runnable {

    GameActivity parent;
    BufferedReader br;
    int clientState;
    private String username;
    private boolean myTurn;
    private String color;
    HashMap<String, ImageView> fields;

    private final int rows = 6;
    private final int columns = 7;

    public GameReceiveMessageFromServer(GameActivity parent) {
        this.parent = parent;
        this.br = parent.getBr();
        this.clientState = 0;
        this.myTurn = parent.myTurn;
        this.username = parent.username;
        this.color = parent.color;
        this.fields = parent.fields;
    }

    @Override
    public void run() {
        while (true) {
            //0 - get rival's moves
            switch (clientState) {
                case 0:
                    try {
                        String line = this.br.readLine();
                        System.out.println("Recieved line: " + line);

                        //move
                        //name:column
                        if (line.split(":").length == 2) {

                            String[] nameColumn = line.split(":");
                            fillFields(nameColumn[0], Integer.parseInt(nameColumn[1]));

                            if (!nameColumn[0].equals(this.username)) {
                                parent.sendMessage("Your turn");

                                parent.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        parent.textTurn.setText("Your turn!");
                                        parent.myTurn = true;
                                    }
                                });
                            }
                            else {
                                myTurn = false;
                                parent.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        parent.textTurn.setText("Wait for your turn!");
                                    }
                                });
                            }
                        }
                        else if (line.split(":").length == 1) {
                            //"NoWinner" or "Winner name"
                            if (line.equals("NoWinner")) {
                                System.out.println("Game is finished without winner.");
                            }
                            else if (line.split(" ")[0].equals("Winner")) {
                                System.out.println("Winner is " + line.split(" ")[1]);
                            }
                        }
                    } catch (IOException ex) {
                        MainActivity.serverNotAvailable();

                        //parent.logout(); //disconnect from server and reset first state
                        return;
                    }
                    break;
                case 1:

                    break;
                default:
                    break;
            }
        }
    }

    void fillFields(String username, int column) {
        String colorForMove;
        if (username.equals(this.username))
            colorForMove = this.color;
        else {
            if (this.color.equals("red"))
                colorForMove = "blue";
            else
                colorForMove = "red";
        }

        for (int i = rows - 1; i >= 0; i--) {
            String tag = fields.get(i + "," + column).getTag().toString();
            String rowColumnColor[] = tag.split(",");
            if (rowColumnColor[2].equals("neutral")) {
                fields.get(i + "," + column).setTag(i + "," + column + "," + colorForMove);

                int row = i;
                parent.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Change image
                        if (colorForMove.equals("red"))
                            parent.fields.get(row + "," + column).setImageResource(R.drawable.red);
                        else
                            parent.fields.get(row + "," + column).setImageResource(R.drawable.blue);
                    }
                });

                break;
            }
        }
    }
}
