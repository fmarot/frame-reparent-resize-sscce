package com.sscce.tmp;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.HWND;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * The problem demonstrated here is that when you move the parent-frame, the child-frame containing the button is also moved the same
 * distance inside the main frame. This behavior is new since Java16. Up to Java15, the child-frame would not move.
 */
public class JFrameSampleWithJava16Problem {

	/** name is used to lookup the window: should be unique on screen ! */
	private static final String MAIN_FRAME_NAME = "Main Frame name";
	/** name is used to lookup the window: should be unique on screen ! */
	private static final String CHILD_FRAME_NAME = "Child Frame name";

	/** Whitout the hack, starting w/ Java16, the child frame will move awkwardly inside the mainFrame when the mainFrame moves.
	 * With the hack, the child frame will be kept in place. */
	private static boolean hackForJava16Plus = false;

	private static JFrame	childFrame;

	private static Frame	mainFrame;
	private static HWND		hwndMainFrame;

	static Dimension getAvailableSpaceInMainFrame() {
		return new Dimension(mainFrame.getWidth() - 80, mainFrame.getHeight() - 80);	// yeah, not very precise yet...
	}

	public static void main(String[] args) {

		SwingUtilities.invokeLater(() -> {
			mainFrame = new Frame(MAIN_FRAME_NAME);
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

			// Use JNA to find the native handle of the parent Frame 
			// hwndMainFrame = User32.INSTANCE.FindWindow(null, MAIN_FRAME_NAME);
			Pointer mainFramePointer = com.sun.jna.Native.getComponentPointer(mainFrame);
			hwndMainFrame = new HWND(mainFramePointer);
		});

		// Create a child JFrame and 'reparent' it inside the parent Frame (but not using Java
		// because this sample demonstrate a generic use-case where the parent frame may be unrelated to Java)
		SwingUtilities.invokeLater(() -> {
			childFrame = new JFrame(CHILD_FRAME_NAME);
			childFrame.setUndecorated(true);
			Dimension mainFrameDim = getAvailableSpaceInMainFrame();
			childFrame.setSize((int)mainFrameDim.getWidth(),(int)mainFrameDim.getHeight());
			childFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			JButton button = new JButton("<html>Please move the mainframe <br> to observe the glitch in Java16+</html>");
			childFrame.getContentPane().setLayout(new BorderLayout());
			childFrame.getContentPane().add(button, BorderLayout.CENTER);
			childFrame.setVisible(true);

			// Use JNA to get the window native handle
			Pointer childFramePointer = com.sun.jna.Native.getComponentPointer(childFrame);
			HWND hwndChild = new HWND(childFramePointer);
			//HWND hwndChild = User32.INSTANCE.FindWindow(null, CHILD_FRAME_NAME);	// problem is this method may find the wrong window in case of multiple window w/ same name

			User32.INSTANCE.SetParent(hwndChild, hwndMainFrame);
		});
	}

	private static boolean messageDisplayedOnce = false;

	private static void resizedChild() {

		if (hwndMainFrame != null) {
			Dimension sizeFrame = mainFrame.getSize();
			
			if (hackForJava16Plus) {
				if (!messageDisplayedOnce) {
					System.out.println("Using Java16+ hack");
					messageDisplayedOnce = true;
				}
				WinDef.RECT rect = new WinDef.RECT();
				User32.INSTANCE.GetWindowRect(hwndMainFrame, rect);	// fill in 'rect' the real values of the mainFrame size
				childFrame.setBounds(-rect.left, -rect.top - 30, sizeFrame.width, sizeFrame.height);
			} else {
				if (!messageDisplayedOnce) {
					System.out.println("Using no hack");
					messageDisplayedOnce = true;
				}
				childFrame.setSize(new Dimension(sizeFrame.width - 80, sizeFrame.height - 80));
				childFrame.setLocation(0, 0);
			}
			
			childFrame.revalidate();
			childFrame.repaint();
		}

	}

}