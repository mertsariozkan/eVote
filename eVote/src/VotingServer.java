import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.Timer;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.chart.ChartUtilities;
import org.jfree.data.general.PieDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import javax.swing.*;

public class VotingServer extends ApplicationFrame {
    private ServerSocket serverSocket;
    private BufferedReader inputStream;
    private PrintWriter outputStream;
    private HashMap<String, Integer> voteCounter;

    public VotingServer() {
        super("Voting Results");

        voteCounter = new HashMap<>();
        voteCounter.put("A party", 0);
        voteCounter.put("B party", 0);
        voteCounter.put("C party", 0);

        DefaultPieDataset dataset = new DefaultPieDataset();

        setSize( 560 , 367 );
        RefineryUtilities.centerFrameOnScreen( this );


        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                System.out.println("In timer...");
                dataset.setValue("Party A", voteCounter.get("A party"));
                dataset.setValue("Party B", voteCounter.get("B party"));
                dataset.setValue("Party C", voteCounter.get("C party"));

                JFreeChart chart = ChartFactory.createPieChart(
                        "Voting Results",   // chart title
                        dataset,          // data
                        true,             // include legend
                        true,
                        false);
                JPanel panel = new ChartPanel(chart);
                setContentPane(panel);
                setVisible( true );
            }
        };
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(timerTask,0,2000);

        KeyPairGenerator keyPairGenerator = null;
        try {
            serverSocket = new ServerSocket(5001);
            Socket socket = new Socket("localhost", 5000);
            BufferedReader inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter outputStream = new PrintWriter(socket.getOutputStream());

            // Key pair generation phase
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(512);
            KeyPair votingServerKeyPair = keyPairGenerator.genKeyPair();
            Key votingServerPublicKey = votingServerKeyPair.getPublic();
            Key votingServerPrivateKey = votingServerKeyPair.getPrivate();

            System.out.println(Arrays.toString(votingServerPublicKey.getEncoded()));
            //Sending concatanated info with PU
            String voteServerInfo = "";
            String publicKeyStr = Base64.getEncoder().encodeToString(votingServerPublicKey.getEncoded());
            voteServerInfo = "$vse" + publicKeyStr;
            outputStream.println(voteServerInfo);
            outputStream.flush();

            //System.out.println(Base64.getEncoder().encodeToString(votingServerPublicKey.getEncoded()));
            socket.close();
            inputStream.close();
            outputStream.close();


            System.out.println(votingServerPublicKey);

            // ***


            while (true) {
                socket = serverSocket.accept();
                inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                outputStream = new PrintWriter(socket.getOutputStream(), true);
                VotingServerThread serverThread = new VotingServerThread(socket, inputStream, outputStream, votingServerPrivateKey, voteCounter);
                serverThread.start();
            }
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }


    }




    public static void main(String[] args) {
        new VotingServer();
    }
}

