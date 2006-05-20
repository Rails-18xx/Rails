package ui;

import game.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import ui.elements.GUIStockSpace;

/**
 * This class displays the StockMarket Window.
 */

public class StockChart extends JFrame implements WindowListener, KeyListener
{

	private JPanel stockPanel;

	private GridLayout stockGrid;
	private GridBagConstraints gc;
	private StockSpace[][] market;

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
