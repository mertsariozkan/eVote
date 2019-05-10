import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.Key;
import java.util.Base64;

public class CertificateAuthorityThread extends Thread {

    BufferedReader inputStream;
    PrintWriter outputStream;
    DatabaseOperations db;
    Key privateKey;
    Key publicKey;
    String votingServerCertificate;
    Socket socket;
    String encryptedAesKey;
    public CertificateAuthorityThread(Socket socket, BufferedReader inputStream, PrintWriter outputStream, Key privateKey, Key publicKey, String votingServerCertificate, String encryptedAesKey) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.socket = socket;
        this.votingServerCertificate = votingServerCertificate;
        this.encryptedAesKey = encryptedAesKey;
        db = new DatabaseOperations();

    }


    @Override
    public void run() {
        try {
            String input;
            System.out.println("run");
            while ((input = inputStream.readLine()) != null) {
                System.out.println("in while");

                if(input.startsWith("$vsc")) { // vsc : voting server cert request comes.
                    outputStream.println("$aes"+encryptedAesKey);
                    outputStream.flush();
                    outputStream.println("$vcr"+votingServerCertificate); // vcr: Voting Server Certificate Reply
                    outputStream.flush();
                    System.out.println("in vcs");
                }
                else if(input.startsWith("$sig")){ // get the signature from client
                    input = input.substring(4);
                    db.connectDatabaseSignatures();
                    if(!db.signatureCheck(input)) { // Check if signature is existent
                        db.insertSignature(input);  // if not insert sig to DB
                        db.closeConnectionSignatures();
                    }
                    db.closeConnectionSignatures();
                    System.out.println("in sig");
                }
                else {
                    System.out.println("in elseeee");
                    // Initially when client connects to the CA automatically send the CA's public key.
                    outputStream.println("$cpu"+ Base64.getEncoder().encodeToString(publicKey.getEncoded())); // cpu: Certificate authority public key
                    outputStream.flush();
                    String[] userInfo = input.split("-");
                    db.connectToDatabaseUsers();
                    if (db.userCheck(userInfo[0], userInfo[1], userInfo[2], userInfo[3])) {
                        if (db.userCheckSum(userInfo[0])) {
                            System.out.println("kye");
                            // Key exist.
                            outputStream.println("$kye");
                            outputStream.flush();
                            db.closeConnectionUsers();
                        } else {
                            System.out.println("kde");
                            //User exist but no key.
                            db.insertUser(userInfo[0]);
                            outputStream.println("$kde");
                            outputStream.flush();
                            db.closeConnectionUsers();
                        }
                    } else {
                        System.out.println("in ude");
                        //User doesn't exist.
                        outputStream.println("$ude");
                        outputStream.flush();
                        db.closeConnectionUsers();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
                inputStream.close();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
