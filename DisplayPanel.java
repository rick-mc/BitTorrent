import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class TorrentPanel extends JPanel{
	
	JTextField TorrentName;
	JTextField FileName;
	JButton done;
	
	static String TN;
	static String FN;
	
	String Size = "0";
	
	JLabel Tname = new JLabel("Torrent name");
	JLabel Fname = new JLabel("File name");

	
	/**
	 * create the panel.
	 */
	public TorrentPanel(){
		//make text field
		makeTextField();
		//lay out the text field
		textFieldLayout();
		
	}
	
	protected void makeTextField(){
		TorrentName = new JTextField();
		TorrentName.setEditable(true);
		FileName = new JTextField();
		FileName.setEditable(true);
		done = new JButton("done");
		done.addActionListener(new ActionListener() {
 
            public void actionPerformed(ActionEvent e)
            {
                TN = TorrentName.getText();
                FN = FileName.getText();
                ErrorCheck();
            }
        });
	}
	
	protected void textFieldLayout(){
		setLayout(new GridLayout(0,2));
		add(Tname);
		add(TorrentName);
		add(Fname);
		add(FileName);
		add(done);
	}
	
	public void setTorrentName(String Name){
		TorrentName.setText(Name);
	}
	public void setFileName(String Name){
		FileName.setText(Name);
	}
	
	protected void ErrorCheck(){
		if(TN.contentEquals("")){
			DisplayPanel.setText(1);
		}
		else if(FN.contentEquals("")){
			DisplayPanel.setText(2);
		}
	}
	
	
}
