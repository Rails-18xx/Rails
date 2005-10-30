 /* $Header: /Users/blentz/rails_rcs/cvs/18xx/ui/Attic/MapWindow2.java,v 1.1 2005/10/30 19:55:06 evos Exp $
 * 
 * Created on 08-Aug-2005
 * Change Log:
 */
package ui;

import game.*;
import javax.swing.*;

import java.awt.*;

import ui.hexmap.*;

/**
 * MapWindow class displays the Map Window. It's shocking, I know.
 * 
 * @author Erik Vos
 * @author Brett
 */
public class MapWindow2 extends JFrame
{
	private MapManager mmgr;
	private HexMap map;
	private JScrollPane scrollPane;

	public MapWindow2()
	{
		GUIHex.setOverlay(true);
		Scale.set(15);
		
		//Container contentPane = this.getContentPane();
		//contentPane.setLayout(new BorderLayout());
		JPanel contentPane = new JPanel(new GridBagLayout());

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
		
		map.addMouseListener(map);
		addMouseListener(map);
		map.addMouseMotionListener(map);
		//addMouseMotionListener(map);
		addWindowListener(map);

		scrollPane = new JScrollPane(map,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setSize(map.getPreferredSize());
		//scrollPane.setLocation(100,100);
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = gbc.gridy = 0;
		gbc.weightx = gbc.weighty = 0.5;
		gbc.fill = GridBagConstraints.BOTH;
		contentPane.add(scrollPane, gbc);
		//contentPane.add(scrollPane, BorderLayout.CENTER);
		setContentPane (contentPane);
		
		setSize(map.getPreferredSize());
		setLocation(25, 25);
		setTitle("Rails: Game Map");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		//System.out.println("HexMap.isLightWeight? " + map.isLightweight());
	}
}
