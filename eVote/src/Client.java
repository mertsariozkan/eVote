import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

public class Client {
    private Socket CAsocket;
    private BufferedReader CAinputStream;
    private PrintWriter CAoutputStream;
    private Key certificateAuthorityPublicKey;
    private SecretKey aesKey;
    private PublicKey votingServerPublicKey;
    private SecretKey secretKey;
    private boolean userExist;
    private Socket VSsocket;
    private BufferedReader VSinputStream;
    private PrintWriter VSoutputStream;
    private String signatureString;
    private JFrame currFrame;


    public Client(String ssn, String name, String surname, String email, JFrame currFrame) {
        this.currFrame = currFrame;
        userExist = true;
        try {
            CAsocket = new Socket("localhost", 5000);
            CAinputStream = new BufferedReader(new InputStreamReader(CAsocket.getInputStream()));
            CAoutputStream = new PrintWriter(CAsocket.getOutputStream());
            // Concatenate user info and send to Certificate Authority
            String userInfo = ssn + "-" + name + "-" + surname + "-" + email;
            CAoutputStream.println(userInfo);
            CAoutputStream.flush();

            String input;
            // Listen input from CA
            while ((input = CAinputStream.readLine()) != null) {
                if (input.equals("$kye")) { // kye: key exist situation check
                    CAoutputStream.println("$vsc"); //vsc: Voting server certificate
                    CAoutputStream.flush();

                    byte[] keyBytes = Files.readAllBytes(new File("/Users/mertsariozkan/Downloads/secret_"+ssn+".key").toPath());
                    SecretKeySpec spec = new SecretKeySpec(keyBytes , "DES");
                    SecretKeyFactory kf = SecretKeyFactory.getInstance("DES");
                    secretKey = kf.generateSecret(spec);

                } else if (input.equals("$kde")) { // kde: key does not exist but user exist situation check

                    // Generate secret key of the instantiated user
                    KeyGenerator keyGenerator = KeyGenerator.getInstance("DES");
                    secretKey = keyGenerator.generateKey();
                    byte[] key = secretKey.getEncoded();

                    // Write user secret key to file
                    FileOutputStream fileOutputStream = new FileOutputStream(new File("/Users/mertsariozkan/Downloads/secret_"+ssn+".key"));
                    fileOutputStream.write(key);
                    fileOutputStream.flush();
                    fileOutputStream.close();

                    //Certificate request for VotingServer Certificate
                    CAoutputStream.println("$vsc"); //vsc: Voting server certificate
                    CAoutputStream.flush();
                    System.out.println("VSC sent.");
                } else if (input.equals("$ude")) { // ude: user does not exist in the main Users table
                    JOptionPane.showMessageDialog(currFrame, "There is no such user", "Warning", JOptionPane.WARNING_MESSAGE);
                    userExist = false;
                    break;
                } else if (input.startsWith("$vcr")) { // vcr: voting server cert. reply
                    /**
                     * --> Retrieve encrypted voting server's public key (PU of VS was encrypted using symmetric encryption. )
                     * --> Build VS_PU from the decrypted data.
                     * --> Create client's signature by: encrypt(votingServerPublicKey , secretKey)
                     * --> Send signature to Certificate Auth. to save the signature to Signatures table.
                     * **/
                    System.out.println("VCR before substring : " + input);
                    input = input.substring(4);
                    System.out.println("VCR : "+input);
                    Cipher certificateDecryption = Cipher.getInstance("DES");
                    certificateDecryption.init(Cipher.DECRYPT_MODE,aesKey);
                    byte[] votingServerCert = certificateDecryption.doFinal(Base64.getDecoder().decode(input));
                    System.out.println("Plain crt : "+Base64.getEncoder().encodeToString(votingServerCert));

                    X509EncodedKeySpec spec = new X509EncodedKeySpec(votingServerCert);
                    KeyFactory kf = KeyFactory.getInstance("RSA");
                    votingServerPublicKey = kf.generatePublic(spec);
                    System.out.println("VSP: "+Arrays.toString(votingServerPublicKey.getEncoded()));

                    Cipher signatureCreation = Cipher.getInstance("DES");
                    signatureCreation.init(Cipher.ENCRYPT_MODE,secretKey);
                    byte[] signature = signatureCreation.doFinal(votingServerPublicKey.getEncoded());
                    signatureString = Base64.getEncoder().encodeToString(signature);
                    CAoutputStream.println("$sig"+signatureString);
                    CAoutputStream.flush();

                    //Go to voting screen
                    break;
                } else if (input.startsWith("$cpu")) {
                    /**
                     * Retrieve certificate authority's public key, and build the key from the retrieved data.
                     * **/byte publicKeyData[] = Base64.getDecoder().decode(input.substring(4));
                    X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKeyData);
                    KeyFactory kf = KeyFactory.getInstance("RSA");
                    certificateAuthorityPublicKey = kf.generatePublic(spec);
                    System.out.println("CPU : "+input.substring(4));



                }
                else if (input.startsWith("$aes")){
                    /**
                     * DES secret key for decrypting Voting Server's public key should be ready before process.
                     * We created encrypted data with RSA encryption, encrypted secretKey with certificateAuthPrivate key
                     * Now, we are decrypting the cipher for retrieving secret key to unlock the certificate of the voting server.
                     * **/
                    input = input.substring(4);
                    System.out.println("AES : " + input);
                    //byte[] decodedAesString = Base64.getDecoder().decode(input);
                    Cipher decoderCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                    decoderCipher.init(Cipher.DECRYPT_MODE, certificateAuthorityPublicKey);
                    byte[] decryptedSecretKeyComp = decoderCipher.doFinal(Base64.getDecoder().decode(input));
                    SecretKeySpec spec = new SecretKeySpec(decryptedSecretKeyComp , "DES");
                    SecretKeyFactory kf = SecretKeyFactory.getInstance("DES");
                    aesKey = kf.generateSecret(spec);
                    System.out.println(Base64.getEncoder().encodeToString(aesKey.getEncoded()));




                }
            }


            // Creating new connection bridge between client and votingserver.
            VSsocket = new Socket("localhost",5001);
            VSoutputStream = new PrintWriter(VSsocket.getOutputStream());
            VSinputStream = new BufferedReader(new InputStreamReader(VSsocket.getInputStream()));

            if (userExist) goToVotingScreen(); // if user exists go with the flow. If it does not exist, this client terminates.




        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        } finally {
            try {
                // Close CertAuth connections because client's work is done with the CA.
                CAsocket.close();
                CAinputStream.close();
                CAoutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Change frames; login screen --> voting screen
     * Encrypt the vote with the voting server's previously retrieved public key.
     * Send 'signature' and 'encVote' to voting server for verification and vote counting.
     */
    private void goToVotingScreen() {
        currFrame.setVisible(false);
        currFrame = new VotingScreen();
        ((VotingScreen) currFrame).getVoteButton().addActionListener(e -> {
            try {
                Cipher voteEncryption = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                voteEncryption.init(Cipher.ENCRYPT_MODE,votingServerPublicKey);
                String selectedButton = ((VotingScreen) currFrame).getSelectedButtonText(((VotingScreen) currFrame).getButtonGroup());
                byte[] encryptedVote = voteEncryption.doFinal(Base64.getDecoder().decode(selectedButton));
                String voteString = Base64.getEncoder().encodeToString(encryptedVote);
                VSoutputStream.println("$sig"+signatureString);
                VSoutputStream.flush();
                VSoutputStream.println("$evs"+voteString); // evs: encrypted vote string
                VSoutputStream.flush();
                String in;

                // Wait for voting server's answer after it has computed the relevant info using signature and encryptedvote.
                while ((in = VSinputStream.readLine()) != null){
                    if (in.startsWith("$snv")) {
                        JOptionPane.showMessageDialog(currFrame , "You do not have a valid signature registered in the system." , "Signature Error", JOptionPane.ERROR_MESSAGE);
                        break;
                    }
                    else if (in.startsWith("$siv")) {
                        JOptionPane.showMessageDialog(currFrame, "Voting is successful." , "Voting Complete" , JOptionPane.INFORMATION_MESSAGE);
                        break;
                    }
                    else if (in.startsWith("$ivb")) {
                        JOptionPane.showMessageDialog(currFrame, "You have voted before!", "Duplicate Vote",JOptionPane.WARNING_MESSAGE);
                        break;
                    }
                }
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | IOException ex) {
                ex.printStackTrace();
            } finally {
                try {
                    VSsocket.close();
                    VSinputStream.close();
                    VSoutputStream.close();
                    currFrame.setVisible(false);
                    currFrame = new LoginScreen();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

            }
        });

    }


}
