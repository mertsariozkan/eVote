import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class MailClient {
    private Properties props;
    private Authenticator authenticator;
    private Session session;
    private int verificationCode;

    public MailClient() {

        final String username="reminderservice00@gmail.com";
        final String password = "mailreminder123";

        props = new Properties();
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.auth" , "true");
        props.put("mail.smtp.host" , "smtp.gmail.com");
        props.put("mail.smtp.port","587");

        authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username,password);
            }
        };
        session = Session.getDefaultInstance(props , authenticator);

    }

    public void sendMail(String recipient) {
        verificationCode = (int)(Math.random() * (9999-1000)) + 1;

        Message message = new MimeMessage(session);

        try {
            message.setFrom(new InternetAddress("reminderservice00@gmail.com"));
            message.setRecipients(Message.RecipientType.TO , InternetAddress.parse(recipient));
            message.setSubject("Confirmation Mail");
            message.setContent("Your verification code is: " + verificationCode, "text/html; charset=utf-8");
            Transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public int getVerificationCode() {
        return verificationCode;
    }


}
