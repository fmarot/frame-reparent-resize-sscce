package com.sscce.tmp;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Constructor;


public class NoProblem {

	// No need for hack in Java16+ + WEmbeddedFrame
	// private static boolean hackForJava16Plus = false;

	private static Frame	childFrame;

	private static Frame	mainFrame;
	private static HWND		hwndMainFrame;

	static Dimension getAvailableSpaceInMainFrame() {
		return new Dimension(mainFrame.getWidth() - 80, mainFrame.getHeight() - 80);	// yeah, not very precise yet...
	}

	public static void main(String[] args) {

		SwingUtilities.invokeLater(() -> {
			mainFrame = new Frame("Main Frame");
			mainFrame.setSize(400, 400);
			mainFrame.setLayout(new BorderLayout());

			mainFrame.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent we) {
					mainFrame.dispose();
				}
			});

			mainFrame.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentResized(ComponentEvent e) {
					if (childFrame != null) {
						resizedChild();
					}
				}

				@Override
				public void componentMoved(ComponentEvent e) {
					resizedChild();
				}
			});

			// Afficher la Frame principale
			mainFrame.setVisible(true);
		});

		// Utilisation de JNA pour récupérer le handle natif (HWND) de la Frame principale
		hwndMainFrame = User32.INSTANCE.FindWindow(null, "Main Frame");

		// Create a child JFrame and 'reparent' it inside the parent Frame (but not using Java
		// because this sample demonstrate a generic use-case where the parent frame may be unrelated to Java)
		SwingUtilities.invokeLater(() -> {
			childFrame = new JFrame("Internal JFrame");
			String className = "sun.awt.windows.WEmbeddedFrame";
			try {
				// use class name because this class does not exist on Linux so  the code won't compile on Linux
				Class<?> clazz = Class.forName(className);
				Constructor<?> constructor = clazz.getConstructor(long.class);
				childFrame = (Frame) constructor.newInstance(Pointer.nativeValue(hwndMainFrame.getPointer()));
			} catch(Exception e) {
				e.printStackTrace();
				throw new RuntimeException("WEmbeddedFrame error");
			}
			JApplet applet = new JApplet();
			childFrame.add(applet);

			// childFrame.setUndecorated(true);
			Dimension mainFrameDim = getAvailableSpaceInMainFrame();
			childFrame.setSize((int)mainFrameDim.getWidth(),(int)mainFrameDim.getHeight());

			JButton button = new JButton("<html>Please move the mainframe <br> to demonstrate no glitch using WEmbededWindow </html>");
			applet.setLayout(new BorderLayout());
			applet.add(button, BorderLayout.CENTER);
			childFrame.setVisible(true);

			// Use JNA to get the window native handle
			HWND hwndChild = User32.INSTANCE.FindWindow(null, "Internal Frame (WEmbeddedFrame)");

			User32.INSTANCE.SetParent(hwndChild, hwndMainFrame);
		});
	}

	private static void resizedChild() {

		if (hwndMainFrame != null) {
			Dimension sizeFrame = mainFrame.getSize();
			
			childFrame.setSize(new Dimension(sizeFrame.width - 80, sizeFrame.height - 80));
			childFrame.setLocation(0, 0);
			
			childFrame.revalidate();
			childFrame.repaint();
		}

	}

}