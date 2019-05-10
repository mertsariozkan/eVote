import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;

public class VotingServerThread extends Thread {

    private DatabaseOperations db;
    private Socket socket;
    private BufferedReader inputStream;
    private PrintWriter outputStream;
    private Key privateKey;
    private HashMap<String, Integer> voteCounter;
    private boolean isSignatureValid;
    private boolean isVotedBefore;
    public VotingServerThread(Socket socket, BufferedReader inputStream, PrintWriter outputStream, Key privateKey, HashMap<String, Integer> voteCounter) {
        this.socket = socket;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.privateKey = privateKey;
        this.voteCounter = voteCounter;
    }

    @Override
    public void run() {
        db = new DatabaseOperations();
        String input;
        try {
            while ((input = inputStream.readLine()) != null) {
                if (input.startsWith("$evs")){ // evs: encrypted vote string
                    if (isSignatureValid && !isVotedBefore) { // signature is valid AND first vote.
                        // Then: decrypt the vote and count the vote into hashmap.
                        String encVote = input.substring(4);
                        Cipher voteDecryption = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                        voteDecryption.init(Cipher.DECRYPT_MODE , privateKey);
                        byte[] decipheredVote = voteDecryption.doFinal(Base64.getDecoder().decode(encVote));
                        String finalVote = Base64.getEncoder().encodeToString(decipheredVote);
                        System.out.println(finalVote);
                        // count vote
                        synchronized (voteCounter) {
                            switch (finalVote.substring(3)) {
                                case "a":
                                    voteCounter.replace("A party", voteCounter.get("A party") + 1);
                                    break;
                                case "b":
                                    voteCounter.replace("B party", voteCounter.get("B party") + 1);
                                    break;
                                case "c":
                                    voteCounter.replace("C party", voteCounter.get("C party") + 1);
                                    break;
                            }
                            System.out.println(voteCounter);

                        }
                        // send success message
                        outputStream.println("$siv"); // siv: signature is valid
                        outputStream.flush();
                    }
                }
                else if (input.startsWith("$sig")) { // sig: signature
                    String signature = input.substring(4);
                    db.connectDatabaseSignatures();
                    if (db.signatureCheck(signature)){
                        isSignatureValid = true;
                        if(db.isVotedBefore(signature)) {
                            isVotedBefore = true;
                            outputStream.println("$ivb"); // ivb: is voted before
                            outputStream.flush();
                            db.closeConnectionSignatures();
                            break;
                        } else {
                            db.updateVoter(signature);
                            db.closeConnectionSignatures();
                        }
                    }

                    else {
                        isSignatureValid = false;
                        outputStream.println("$snv"); // snv : signature not valid.
                        outputStream.flush();
                    }


                }
            }
        } catch (IOException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        } finally {
            try {
                db.closeConnectionUsers();
                db.connectDatabaseSignatures();
                socket.close();
                inputStream.close();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
