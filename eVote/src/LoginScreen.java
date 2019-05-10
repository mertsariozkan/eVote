import javax.swing.*;

public class LoginScreen extends JFrame {

    Client client;

    public LoginScreen() {
        setSize(400, 600);
        setResizable(false);
        JPanel panel = new JPanel();


        JLabel ssn = new JLabel("SSN:");
        ssn.setBounds(0, 120, 140, 40);
        ssn.setHorizontalAlignment(SwingConstants.RIGHT);
        JTextField ssnField = new JTextField();

        ssnField.setBounds(160, 120, 180, 40);


        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setBounds(0, 180, 140, 40);
        nameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        JTextField nameField = new JTextField();

        nameField.setBounds(160, 180, 180, 40);


        JLabel surnameLabel = new JLabel("Surname:");
        surnameLabel.setBounds(0, 240, 140, 40);
        surnameLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JTextField surnameField = new JTextField();

        surnameField.setBounds(160, 240, 180, 40);


        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setBounds(0, 300, 140, 40);
        emailLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        JTextField emailField = new JTextField();
        emailField.setBounds(160, 300, 180, 40);


        JButton loginButton = new JButton("Login");
        loginButton.setBounds(120, 380, 160, 40);
        loginButton.addActionListener(e -> {
            if (!(ssnField.getText().isBlank() || nameField.getText().isBlank() || surnameField.getText().isBlank() || emailField.getText().isBlank())) {
                MailClient mailClient = new MailClient();
                mailClient.sendMail(emailField.getText());
                String input = JOptionPane.showInputDialog(this, "Enter verification code");
                if (Integer.parseInt(input) == mailClient.getVerificationCode()) {
                    client = new Client(ssnField.getText(), nameField.getText(), surnameField.getText(), emailField.getText(), this);
                }
                else {
                    JOptionPane.showMessageDialog(this , "Verification failed, wrong code.", "Failure" , JOptionPane.ERROR_MESSAGE);
                }
            }
            else {
                JOptionPane.showMessageDialog(this , "Every field is mandatory.", "Failure" , JOptionPane.WARNING_MESSAGE);
            }



        });

        panel.setLayout(null);
        panel.add(surnameField);
        panel.add(emailField);
        panel.add(nameField);
        panel.add(ssnField);
        panel.add(ssn);
        panel.add(surnameLabel);
        panel.add(emailLabel);
        panel.add(nameLabel);
        panel.add(loginButton);
        add(panel);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    public static void main(String[] args) {
        new LoginScreen();
    }


}
