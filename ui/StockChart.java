/*
 * Rails: an 18xx game system. Copyright (C) 2005 Brett Lentz
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package ui;

import game.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import ui.elements.GUIStockSpace;

import java.util.*;

/**
 * This class displays the StockMarket Window.
 * 
 * @author Brett
 */

public class StockChart extends JFrame implements WindowListener, KeyListener
{

	private JPanel stockPanel;
	private JLabel priceLabel;
	private JLayeredPane layeredPane;

	private int depth;
	private GridLayout stockGrid;
	private GridBagConstraints gc;
	private StockSpace[][] market;
	private Dimension size;
	private ArrayList tokenList;

	public StockChart()
	{
		super();

		initialize();
		populateGridBag();
		populateStockPanel();

		stockPanel.setBackground(Color.LIGHT_GRAY);
		
		addWindowListener(this);
		addKeyListener(this);
		pack();
	}
	
	private void initialize()
	{
		setTitle("Rails: Stock Chart");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		getContentPane().setLayout(new GridBagLayout());
		
		stockPanel = new JPanel();

		stockGrid = new GridLayout();
		stockGrid.setHgap(0);
		stockGrid.setVgap(0);
		stockPanel.setLayout(stockGrid);

		gc = new GridBagConstraints();

		market = Game.getStockMarket().getStockChart();
	}

	private void populateGridBag()
	{
		gc.gridx = 0;
		gc.gridy = 0;
		gc.weightx = 1.0;
		gc.weighty = 1.0;
		gc.gridwidth = 2;
		gc.fill = GridBagConstraints.BOTH;
		getContentPane().add(stockPanel, gc);
	}

	private void populateStockPanel()
	{
		stockGrid.setColumns(market[0].length);
		stockGrid.setRows(market.length);
		
		for (int i = 0; i < market.length; i++)
		{
			for (int j = 0; j < market[0].length; j++)
			{
		        //setupChartSpace(i, j);
		        //stockPanel.add(layeredPane);
			    stockPanel.add (new GUIStockSpace (i, j, market[i][j]));
			}
		}
	}


	public void windowActivated(WindowEvent e)
	{
	}

	public void windowClosed(WindowEvent e)
	{
	}

	public void windowClosing(WindowEvent e)
	{
		StatusWindow.uncheckMenuItemBox(StatusWindow.MARKET);
		dispose();
	}

	public void windowDeactivated(WindowEvent e)
	{
	}

	public void windowDeiconified(WindowEvent e)
	{
	}

	public void windowIconified(WindowEvent e)
	{
	}

	public void windowOpened(WindowEvent e)
	{
	}

	public void keyPressed(KeyEvent e)
	{
		if (e.getKeyCode() == KeyEvent.VK_F1)
		{
			HelpWindow.displayHelp(GameManager.getInstance().getHelp());
			e.consume();
		}
	}

	public void keyReleased(KeyEvent e)
	{
	}

	public void keyTyped(KeyEvent e)
	{
	}

}
