import javax.crypto.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.util.Base64;

public class CertificateAuthority {
    private ServerSocket serverSocket;
    private BufferedReader inputStream;
    private PrintWriter outputStream;
    private Socket socket;
    private KeyPairGenerator keyPairGenerator;
    String votingServerCertificate;
    String encryptedAesKey;

    public CertificateAuthority() {
        try {
            // Key pair generation phase
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair CAkeyPair = keyPairGenerator.genKeyPair();
            Key CApublicKey = CAkeyPair.getPublic();
            Key CAprivateKey = CAkeyPair.getPrivate();
            boolean certificateIsDone = false;

            serverSocket = new ServerSocket(5000);
            while (true) {
                socket = serverSocket.accept();
                inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                outputStream = new PrintWriter(socket.getOutputStream(), true);

                if (!certificateIsDone) {
                    String input;
                    while ((input = inputStream.readLine()) != null) {
                        if (input.startsWith("$vse")) {

                            input = input.substring(4);


                            /**
                             * Encrypt VS public key with generated secret key.
                             */
                            KeyGenerator secretAesKeyGen = KeyGenerator.getInstance("DES");
                            SecretKey aesKey = secretAesKeyGen.generateKey();
                            Cipher aesEncryption = Cipher.getInstance("DES");
                            aesEncryption.init(Cipher.ENCRYPT_MODE, aesKey);
                            byte[] encryptedInput = aesEncryption.doFinal(Base64.getDecoder().decode(input));
                            votingServerCertificate = Base64.getEncoder().encodeToString(encryptedInput);


                            System.out.println(votingServerCertificate);

                            /**
                             * Encrypt secret key with CA privatekey to ensure secret key to unlock the certificate is not exposed to network.
                             */
                            Cipher certificateEncryption = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                            certificateEncryption.init(Cipher.ENCRYPT_MODE, CAprivateKey);
                            byte[] CAcertificate = certificateEncryption.doFinal(aesKey.getEncoded());
                            encryptedAesKey = Base64.getEncoder().encodeToString(CAcertificate);

                            socket.close();
                            inputStream.close();
                            outputStream.close();
                            certificateIsDone = true;
                            break;
                        }
                    }
                } else {
                    CertificateAuthorityThread certificateAuthorityThread = new CertificateAuthorityThread(socket, inputStream, outputStream, CAprivateKey, CApublicKey, votingServerCertificate, encryptedAesKey);
                    certificateAuthorityThread.start();
                    System.out.println("Thread started.");
                }
            }

        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        new CertificateAuthority();

    }
}
