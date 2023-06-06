package pkg4inarowserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

/**
 *
 * @author Anja Tanovic
 */
public class ServeConnectedClient implements Runnable {
    
    private Socket socket;
    private BufferedReader br;
    private PrintWriter pw;
    
    private ArrayList<ServeConnectedClient> allClients;
    private String username;
    private String rivalUsername;
    private boolean playFirst;
    private int clientState;
    private boolean busy;
    private boolean myTurn;
    
    private final int rows = 6;
    private final int columns = 7;
    private int[][] fields; //0 - neutral fields, 1 - my fields, 2 - rival's fields
    private final int winningNumber = 4;
    
    public void setClientState(int clientState) {
        this.clientState = clientState;
    }

    public int getClientState() {
        return clientState;
    }
    
    public boolean isBusy() {
        return busy;
    }
    
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    
    public ServeConnectedClient(Socket socket, ArrayList<ServeConnectedClient> allClients) {
        this.socket = socket;
        this.allClients = allClients;
        this.fields = new int[rows][columns];
        
        //get InputStream and OutputStream from socket
        try {
            this.br = new BufferedReader(new InputStreamReader(this.socket.getInputStream(), "UTF-8"));
            this.pw = new PrintWriter(new OutputStreamWriter(this.socket.getOutputStream()), true);
            //user data is still not known
            this.username = "";
            this.rivalUsername = "";
            this.playFirst = false;
            this.clientState = 0;
            this.busy = false;
        } catch (Exception ex) {
            System.out.println("Cannoot get data from server.");
        }
    }    
    
    @Override
    public void run() {
        
        while (true) {
            if (clientState == 0 || clientState == 1)
                this.busy = false;
            else
                this.busy = true;
            
            //0 - get username
            //1 - user chooses player
            //2 - game, get and forward player's move, check if game is finished
            //3 - game finished, play again? (from state 3 to state 1 or 2)
            //4 - check if other user agrees with your decision
            switch (clientState) {
                case 0:
                    //System.out.println(this.username + " state:" + this.clientState);
                    try {
                        //wait for username
                        this.username = this.br.readLine();

                        if (this.username != null && !this.username.equals("")) {
                            System.out.println("Connected user: " + this.username);
                            //inform everyone that new user is connected
                            connectedClientsUpdateStatus();
                            clientState++;
                        }
                    } catch (Exception ex) {
                        System.out.println("Disconnected user: " + this.username);
                        
                        for (ServeConnectedClient cl : this.allClients) {
                            if (cl.getUsername().equals(this.username)) {
                                this.allClients.remove(cl);
                                connectedClientsUpdateStatus();
                                return;
                            }
                        }
                    }
                    break;
                case 1:
                    //System.out.println(this.username + " state:" + this.clientState);
                    try {
                        //wait for game initiaton and accepting
                        String init = this.br.readLine();
                        
                        String[] A_B = init.split(":");
                        if (A_B.length == 3 && A_B[1].trim().equals("chooses"))
                        {
                            String A = A_B[0].trim();
                            String B = A_B[2].trim();
                            System.out.println(A + " chooses " + B);
                            this.rivalUsername = B;
                                                        
                            String message = "request from:" + A;
                            for (ServeConnectedClient c : this.allClients) {
                                if (c.getUsername().equals(B)) {
                                    c.pw.println(message);
                                    System.out.println("Request sent to " + c.getUsername());
                                    break;
                                }
                            }
                        }
                        else if (A_B.length == 3 && A_B[1].trim().equals("accepts")) {
                            String A = A_B[0].trim();
                            String B = A_B[2].trim();
                            System.out.println(A + " accepted " + B);
                            
                            String message = "First move:" + B;
                            for (ServeConnectedClient c : this.allClients) {
                                if (c.getUsername().equals(B) || c.getUsername().equals(A)) {
                                    c.pw.println(message);
                                }
                            }
                            resetFields();
                            this.rivalUsername = B;
                            this.myTurn = false;
                            this.playFirst = false;
                            
                            clientState++;
                        }
                        else if (A_B.length == 3 && A_B[1].trim().equals("doesnt accept")) {
                            String A = A_B[0].trim();
                            String B = A_B[2].trim();
                            System.out.println(A + " doesnt accept " + B);
                            
                            String message = "rejected";
                            for (ServeConnectedClient c : this.allClients) {
                                if (c.getUsername().equals(B)) {
                                    c.pw.println(message);
                                }
                            }
                        } 
                        else if (init.equals("request accepted")) {
                            System.out.println("Request accepted.");
                            
                            resetFields();
                            myTurn = true;
                            this.playFirst = true;
                            clientState++;
                        }
                    } catch (Exception ex) {
                        System.out.println("Disconnected user: " + this.username);
                        
                        for (ServeConnectedClient cl : this.allClients) {
                            if (cl.getUsername().equals(this.username)) {
                                this.allClients.remove(cl);
                                connectedClientsUpdateStatus();
                                return;
                            }
                        }                    
                    } 
                    break;
                case 2:
                    //System.out.println(this.username + " state:" + this.clientState);
                    try {
                        if (myTurn) {
                            System.out.println(this.username + "'s turn:");
                            //wait for move 
                            //format of move is:
                            //name:column
                            String move = this.br.readLine();
                            System.out.println(move);
                            
                            if (move.equals("You lost"))
                                clientState++;
                            else {
                                String nameColumn[] = move.split(":");
                                System.out.println(nameColumn[0] + " made a move in column " + nameColumn[1] + ".");

                                boolean successful = false;

                                if (nameColumn.length == 2 && nameColumn[0].equals(this.username)) {
                                    successful = fillFields(1, Integer.parseInt(nameColumn[1]));

                                    if (successful) {
                                        for (ServeConnectedClient c : this.allClients) {
                                            if (c.getUsername().equals(this.rivalUsername) || c.getUsername().equals(this.username)) {
                                                c.pw.println(move);
                                            }
                                        }
                                        myTurn = false; 
                                   }
                                    else {
                                        for (ServeConnectedClient c : this.allClients) {
                                            if (c.getUsername().equals(this.username)) {
                                                c.pw.println("Try again");
                                            }
                                        }
                                    }
                                    
                                    for (ServeConnectedClient c : this.allClients) {
                                        if (c.getUsername().equals(this.rivalUsername)) {
                                            boolean ok = c.fillFields(2, Integer.parseInt(nameColumn[1]));
                                            if (!ok)
                                                System.out.println("Problem with fields logic");
                                            break;
                                        }
                                    }
                                }

                                String result = checkIfGameIsFinished();
                                //if game is finished (somebody is winner or game is finished 
                                //without the winner) -> send message
                                //if game is still on -> message will not be sent
                                sendMessageAboutWinner(result);
                                if (result.startsWith("Winner")) 
                                    clientState++;
                            }
                        }
                        else {
                            //wait for my turn
                            String turn = this.br.readLine();
                            if (turn.equals("Your turn"))
                                myTurn = true;
                        }
                    
                    } catch (Exception ex) {
                        System.out.println("Disconnected user: " + this.username);
                        
                        for (ServeConnectedClient cl : this.allClients) {
                            if (cl.getUsername().equals(this.username)) {
                                this.allClients.remove(cl);
                                connectedClientsUpdateStatus();
                                return;
                            }
                        } 
                    }
                    break;
                case 3:
                    //System.out.println(this.username + " state:" + this.clientState);
                    //play again or choose rival again
                    try {                       
                        String line = this.br.readLine();

                        if (line.equals("Play again") && clientState!= 1) {
                            
                            resetFields();
                            if (this.playFirst) {
                                this.myTurn = false;
                                this.playFirst = false;
                            }
                            else {
                                this.myTurn = true;
                                this.playFirst = true;
                            }
                            
                            clientState = 4;
                        }
                        else if (line.equals("New game")) {
                            
                            for (ServeConnectedClient c : this.allClients) {
                                if (c.getUsername().equals(this.rivalUsername)) {
                                    c.setClientState(1);
                                    c.pw.println(line);
                                }
                            }
                            this.pw.println("End");
                            
                            //this.rivalUsername = "";
                            clientState = 1;
                        }
                    } catch (Exception ex) {
                        System.out.println("Disconnected user: " + this.username);
                        
                        for (ServeConnectedClient cl : this.allClients) {
                            if (cl.getUsername().equals(this.username)) {
                                this.allClients.remove(cl);
                                connectedClientsUpdateStatus();
                                return;
                            }
                        }
                    }
                    break;
                case 4:
                    //check if your rival wants to play again
                    for (ServeConnectedClient c : this.allClients) {
                        if (c.getUsername().equals(this.rivalUsername)) {
                            if (c.getClientState() == 4) {
                                this.clientState = 2;
                                c.setClientState(2);
                                break;
                            }
                        }
                    }
                    break;
                default:
                    break;
            }   
        }
    }

    /**
     * Method for updating status of connected students
     * format: Users: name1 name2 name3 ...
     * this message is sent to all clients
     */
    void connectedClientsUpdateStatus() {
        //Create string, for example:
        //Users: name1 name2 name3
        String connectedUsers = "Users:";
        for (ServeConnectedClient c : this.allClients) {
            if (!c.isBusy())
                connectedUsers += " " + c.getUsername();
        }

        //send this info to all connected and available clients
        for (ServeConnectedClient update : this.allClients) {
            update.pw.println(connectedUsers);
        }

        System.out.println(connectedUsers);
    }
    
    void resetFields() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                fields[i][j] = 0;
            }
        }
    }
    
    String checkIfGameIsFinished() {
        String result = "NotFinished";
        
        //check horizontally
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j <= columns - winningNumber; j++) {
                int color = fields[i][j];
                if (color != 0) {
                    for (int k = 1; k < winningNumber; k++) {
                        if (color != fields[i][j+k])
                            break;
                        if (k == winningNumber - 1) {
                            result = "Winner " + fields[i][j];
                            System.out.println("Winning fields: from (" + i + "," + j + ") to (" + i + "," + (j + k) +")");
                        }
                    }
                }
            }
        }
        //check vertically
        if (result.equals("NotFinished")) {
            for (int i = 0; i <= rows - winningNumber; i++) {
                for (int j = 0; j < columns; j++) {
                    int color = fields[i][j];
                    if (color != 0) {
                        for (int k = 1; k < winningNumber; k++) {
                            if (color != fields[i+k][j])
                                break;
                            if (k == winningNumber - 1) {
                                result = "Winner " + fields[i][j];
                                System.out.println("Winning fields: from (" + i + "," + j + ") to (" + (i + k) + "," + j + ")");
                            }
                        }
                    }
                }
            }
        }
        //check diagonals
        if (result.equals("NotFinished")) {
            for (int i = 0; i <= rows - winningNumber; i++) {
                for (int j = 0; j <= columns - winningNumber; j++) {
                    int color = fields[i][j];
                    if (color != 0) {
                        for (int k = 1; k < winningNumber; k++) {
                            if (color != fields[i+k][j+k])
                                break;
                            if (k == winningNumber - 1) {
                                result = "Winner " + fields[i][j];
                                System.out.println("Winning fields: from (" + i + "," + j + ") to (" + (i + k) + "," + (j + k) + ")");
                            }
                        }
                    }
                }
            }
        }
        if (result.equals("NotFinished")) {
            for (int i = 0; i <= rows - winningNumber; i++) {
                for (int j = winningNumber - 1; j < columns; j++) {
                    int color = fields[i][j];
                    if (color != 0) {
                        for (int k = 1; k < winningNumber; k++) {
                            if (color != fields[i+k][j-k])
                                break;
                            if (k == winningNumber - 1) {
                                result = "Winner " + fields[i][j];
                                System.out.println("Winning fields: from (" + i + "," + j + ") to (" + (i + k) + "," + (j - k) + ")");
                            }
                        }
                    }
                }
            }
        }
        //check if everything is full but there is no winner
        if (result.equals("NotFinished")) {
            result = "NoWinner";
            //checking only first row is enough
            for (int j = 0; j < columns; j++) {
                if (fields[0][j] == 0)
                    result = "NotFinished";
            }
        }
        return result;
    }
    
    void sendMessageAboutWinner(String result) {
        if (result.equals("NoWinner")) {
            String message = result;
            for (ServeConnectedClient c : this.allClients) {
                if (c.getUsername().equals(this.username) || c.getUsername().equals(this.rivalUsername)) {
                    c.pw.println(message);
                }
            }
        }
        else if (result.split(" ").length == 2 && result.split(" ")[0].equals("Winner")){
            String winner; 
            if (result.split(" ")[1].equals("1"))
                winner = this.username;
            else 
                winner = this.rivalUsername;
            
            String message = "Winner " + winner;
            for (ServeConnectedClient c : this.allClients) {
                if (c.getUsername().equals(this.username) || c.getUsername().equals(this.rivalUsername)) {
                    c.pw.println(message);
                }
            }
        }
    }

    boolean fillFields(int user, int column) {
        boolean ok = false;
        
        for (int i = rows - 1; i >= 0; i--) {
            if (fields[i][column] == 0) {
                fields[i][column] = user;
                ok = true;
                break;
            }
        }
        return ok;
    }
}
