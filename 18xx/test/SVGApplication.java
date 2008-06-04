package test;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.gvt.GVTTreeRendererAdapter;
import org.apache.batik.swing.gvt.GVTTreeRendererEvent;
import org.apache.batik.swing.svg.SVGDocumentLoaderAdapter;
import org.apache.batik.swing.svg.SVGDocumentLoaderEvent;
import org.apache.batik.swing.svg.GVTTreeBuilderAdapter;
import org.apache.batik.swing.svg.GVTTreeBuilderEvent;

public class SVGApplication {

    public static void main(String[] args) {
        JFrame f = new JFrame("Batik");
        SVGApplication app = new SVGApplication(f);
        f.getContentPane().add(app.createComponents());

        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        f.setSize(400, 400);
        f.setVisible(true);
    }

    JFrame frame;
    JButton button = new JButton("Load...");
    JLabel label = new JLabel();
    JSVGCanvas svgCanvas = new JSVGCanvas();

    public SVGApplication(JFrame f) {
        frame = f;
    }

    public JComponent createComponents() {
        final JPanel panel = new JPanel(new BorderLayout());

        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.add(button);
        p.add(label);

        panel.add("North", p);
        panel.add("Center", svgCanvas);

        // Set the button action.
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                JFileChooser fc = new JFileChooser(".");
                int choice = fc.showOpenDialog(panel);
                if (choice == JFileChooser.APPROVE_OPTION) {
                    File f = fc.getSelectedFile();
                    try {
                        svgCanvas.setURI(f.toURL().toString());
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        // Set the JSVGCanvas listeners.
        svgCanvas.addSVGDocumentLoaderListener(new SVGDocumentLoaderAdapter() {

            public void documentLoadingStarted(SVGDocumentLoaderEvent e) {
                label.setText("Document Loading...");
            }

            public void documentLoadingCompleted(SVGDocumentLoaderEvent e) {
                label.setText("Document Loaded.");
            }
        });

        svgCanvas.addGVTTreeBuilderListener(new GVTTreeBuilderAdapter() {

            public void gvtBuildStarted(GVTTreeBuilderEvent e) {
                label.setText("Build Started...");
            }

            public void gvtBuildCompleted(GVTTreeBuilderEvent e) {
                label.setText("Build Done.");
                frame.pack();
            }
        });

        svgCanvas.addGVTTreeRendererListener(new GVTTreeRendererAdapter() {

            public void gvtRenderingPrepare(GVTTreeRendererEvent e) {
                label.setText("Rendering Started...");
            }

            public void gvtRenderingCompleted(GVTTreeRendererEvent e) {
                label.setText("");
            }
        });

        return panel;
    }
}
