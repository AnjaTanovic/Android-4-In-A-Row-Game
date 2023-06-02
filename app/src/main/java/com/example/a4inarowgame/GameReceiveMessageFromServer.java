package com.example.a4inarowgame;

import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.io.BufferedReader;
import java.io.IOException;

public class GameReceiveMessageFromServer implements Runnable {

    GameActivity parent;
    BufferedReader br;
    int clientState;
    boolean myTurn;

    public GameReceiveMessageFromServer(GameActivity parent) {
        this.parent = parent;
        this.br = parent.getBr();
        this.clientState = 0;
        myTurn = false;
    }

    @Override
    public void run() {
        while (true) {
            //0 - get rival's moves
            //1 -
            switch (clientState) {
                case 0:

                    break;
                case 1:

                    break;
                default:
                    break;
            }
        }
    }
}
