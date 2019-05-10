import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.util.Enumeration;
import java.util.HashMap;


public class VotingScreen extends JFrame {



    public JButton getVoteButton() {
        return voteButton;
    }

    public ButtonGroup getButtonGroup() {
        return bg;
    }

    private JButton voteButton;
    private ButtonGroup bg;
    private HashMap<String,String> partyMap;
    public VotingScreen(){

        partyMap = new HashMap<>();
        partyMap.put("Party A","btna");
        partyMap.put("Party B","btnb");
        partyMap.put("Party C","btnc");


        voteButton = new JButton("Vote");


        JRadioButton r1=new JRadioButton("Party A");
        JRadioButton r2=new JRadioButton("Party B");
        JRadioButton r3=new JRadioButton("Party C");
        r1.setBounds(75,50,100,30);
        r2.setBounds(75,100,100,30);
        r3.setBounds(75,150,100,30);
        voteButton.setBounds(75,200,100,30);
        bg=new ButtonGroup();
        voteButton.addActionListener(e -> {
            System.out.println(getSelectedButtonText(bg));
        });
        bg.add(r1);bg.add(r2);bg.add(r3);
        add(r1);
        add(r2);
        add(r3);
        add(voteButton);
        setSize(300,300);
        setLayout(null);
        setVisible(true);

    }

    public String getSelectedButtonText(ButtonGroup buttonGroup) {
        for (Enumeration<AbstractButton> buttons = buttonGroup.getElements(); buttons.hasMoreElements();) {
            AbstractButton button = buttons.nextElement();

            if (button.isSelected()) {
                return partyMap.get(button.getText());
            }
        }

        return null;
    }

}

