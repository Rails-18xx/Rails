/* $Header: /Users/blentz/rails_rcs/cvs/18xx/ui/Attic/MapWindow.java,v 1.8 2005/09/26 19:12:03 wakko666 Exp $
 * 
 * Created on 08-Aug-2005
 * Change Log:
 */
package ui;

import game.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import ui.hexmap.*;

/**
 * @author Erik Vos
 */
public class MapWindow extends JFrame implements ActionListener
{

	private MapManager mmgr;
	private HexMap map;
	
	private JMenuBar menuBar;
	private JMenu fileMenu, optMenu;
	private JMenuItem menuItem;

	public void initMenu()
	{
		menuBar =  new JMenuBar();
		fileMenu = new JMenu("File");
		optMenu = new JMenu("Options");
		
		menuItem = new JMenuItem("Open");
		menuItem.setMnemonic(KeyEvent.VK_O);
		menuItem.addActionListener(this);
		fileMenu.add(menuItem);
		menuItem = new JMenuItem("Save");
		menuItem.setMnemonic(KeyEvent.VK_S);
		menuItem.addActionListener(this);
		fileMenu.add(menuItem);
		
		fileMenu.addSeparator();
		
		menuItem = new JMenuItem("Quit");
		menuItem.setMnemonic(KeyEvent.VK_Q);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.ALT_MASK));
		menuItem.addActionListener(this);
		fileMenu.add(menuItem);		
		
		menuBar.add(fileMenu);
		
		menuItem = new JMenuItem("Set Scale");
		menuItem.setMnemonic(KeyEvent.VK_S);
		menuItem.addActionListener(this);
		optMenu.add(menuItem);
		
		menuBar.add(optMenu);
		
		setJMenuBar(menuBar);
	}
	
	public MapWindow()
	{

		mmgr = MapManager.getInstance();
		try
		{
			map = (HexMap) Class.forName(mmgr.getMapUIClassName())
					.newInstance();
		}
		catch (Exception e)
		{
			System.out.println("Map class instantiation error:\n");
			e.printStackTrace();
			return;
		}

		addMouseListener(map);
		getContentPane().add(map);
		
		initMenu();
		
		setSize(900, 600);
		setLocation(25, 25);
		setVisible(true);
		setTitle("Rails: Game Map");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public void actionPerformed(ActionEvent arg0)
	{
		JMenuItem source = (JMenuItem)(arg0.getSource());
		
		if (source.getText().equalsIgnoreCase("Quit"))
			System.exit(0);
	}
	
}
