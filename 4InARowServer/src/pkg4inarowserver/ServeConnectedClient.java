package pkg4inarowserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private int clientState;
    private boolean busy;
    private boolean myTurn;
    
    private final int rows = 6;
    private final int columns = 7;
    private int[][] fields; //0 - neutral fields, 1 - my fields, 2 - rival's fields
    private final int winningNumber = 4;
    
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
            this.clientState = 0;
            this.busy = false;
        } catch (IOException ex) {
            Logger.getLogger(ServeConnectedClient.class.getName()).log(Level.SEVERE, null, ex);
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
            switch (clientState) {
                case 0:
                    try {
                        //wait for username
                        this.username = this.br.readLine();

                        if (this.username != null && !this.username.equals("")) {
                            System.out.println("Connected user: " + this.username);
                            //inform everyone that new user is connected
                            connectedClientsUpdateStatus();
                            clientState++;
                        }
                    } catch (IOException ex) {
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
                            clientState++;
                        }
                    } catch (IOException ex) {
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
                    try {
                        if (myTurn) {
                            System.out.println("Game is on");
                            //wait for move 
                            //format of move is:
                            //name:column
                            String move = this.br.readLine();
                            System.out.println(move);
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
                            }

                            String result = checkIfGameIsFinished();
                            //if game is finished (somebody is winner or game is finished 
                            //without the winner) -> send message
                            //if game is still on -> message will not be sent
                            sendMessageAboutWinner(result);
                        }
                        else {
                            //wait for my turn
                            String turn = this.br.readLine();
                            if (turn.equals("Your turn"))
                            myTurn = true;
                        }
                    
                    } catch (IOException ex) {
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
                    //check if rivalUsername should be reseted
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
        for (ServeConnectedClient svimaUpdateCB : this.allClients) {
            svimaUpdateCB.pw.println(connectedUsers);
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
                for (int k = 1; k < winningNumber; k++) {
                    if (fields[i][j] == 0 || fields[i][j] != fields[i][j+k])
                        break;
                    if (k == winningNumber - 1)
                        result = "Winner " + fields[i][j];
                }
            }
        }
        //check vertically
        if (result.equals("NotFinished")) {
            for (int i = 0; i <= rows - winningNumber; i++) {
                for (int j = 0; j < columns; j++) {
                    for (int k = 1; k < winningNumber; k++) {
                        if (fields[i][j] == 0 || fields[i][j] != fields[i+k][j])
                            break;
                        if (k == winningNumber - 1)
                            result = "Winner " + fields[i][j];
                    }
                }
            }
        }
        //check diagonals
        if (result.equals("NotFinished")) {
            for (int i = 0; i <= rows - winningNumber; i++) {
                for (int j = 0; j <= columns - winningNumber; j++) {
                    for (int k = 1; k < winningNumber; k++) {
                        if (fields[i][j] == 0 || fields[i][j] != fields[i+k][j+k])
                            break;
                        if (k == winningNumber - 1)
                            result = "Winner " + fields[i][j];
                    }
                }
            }
        }
        if (result.equals("NotFinished")) {
            for (int i = 0; i <= rows - winningNumber; i++) {
                for (int j = winningNumber - 1; j < columns; j++) {
                    for (int k = 1; k < winningNumber; k++) {
                        if (fields[i][j] == 0 || fields[i][j] != fields[i+k][j-k])
                            break;
                        if (k == winningNumber - 1)
                            result = "Winner " + fields[i][j];
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
