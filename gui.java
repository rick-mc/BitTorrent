import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

public class gui extends JFrame{
	
	TorrentPanel torrentPanel;
	DisplayPanel displayPanel;
	
	protected GridBagLayout gb = new GridBagLayout();
    protected GridBagConstraints gc = new GridBagConstraints();
	
	public gui(String title){
		super(title);
		torrentPanel = new TorrentPanel();
		displayPanel = new DisplayPanel();
		layOut();
	}
	
	protected void layOut(){
		setLayout(gb);
		gc.gridwidth = GridBagConstraints.REMAINDER;
		gc.insets = new Insets(4,2,2,2);
		
		gc.gridy = 2;
		gc.gridwidth = 2;
		gb.setConstraints(torrentPanel, gc);
		add(torrentPanel);
		
		
		gb.setConstraints(displayPanel, gc);
		add(displayPanel);
	}
	
	public static void main(String[] args) {
		JFrame gui = new gui("Torrent");
		gui.pack();
		gui.setVisible(true);
		gui.setLocationRelativeTo(null);
		gui.setResizable(false);
		gui.addWindowListener(new WindowAdapter() {
        	public void windowClosing(WindowEvent e) {
        		System.exit(1);
        	}
        });
	}

}
