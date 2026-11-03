package atm.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JFrame;

import atm.ATM;
import atm.ui.panels.MainPanel;

public class MainUI extends JFrame{
	public MainUI(ATM atm) {
		super("ATM");
		
		MainPanel mainPanel = new MainPanel(atm);
		atm.setMainPanel(mainPanel);

		this.setLayout(new GridBagLayout());
		this.add(mainPanel, new GridBagConstraints());
	}
}
