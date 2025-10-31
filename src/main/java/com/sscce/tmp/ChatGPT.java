package com.sscce.tmp;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.lang.reflect.Constructor;

/** This version provided by ChatGPT improves may be more reliable than the NoProblem sample.
 * Assure-toi que mainFrame est visibile et displayable avant d’obtenir son HWND (retry loop ou WindowListener.windowOpened).
Crée le WEmbeddedFrame après avoir obtenu le HWND, et ne fais setVisible(true) qu’après avoir correctement configuré le parent (idéalement le constructeur le fait).
Si tu dois reparenter dynamiquement, fais dispose() du top-level avant de recréer le peer natif.
Pour débugger des black screens, configurer D3D/DirectDraw  : -Dsun.java2d.d3d=false -Dsun.java2d.noddraw=true.
Désactive temporairement le double buffering pendant l’opération de reparenting.
*/
public class ChatGPT {

	private static Frame	childFrame;
	private static Frame	mainFrame;
	private static HWND		hwndMainFrame;

	static Dimension getAvailableSpaceInMainFrame() {
		return new Dimension(Math.max(0, mainFrame.getWidth() - 80), Math.max(0, mainFrame.getHeight() - 80));
	}

	public static void main(String[] args) throws Exception {
		// Conseil debug : lancer la JVM avec ces flags si tu suspects Java2D/D3D :
		// -Dsun.java2d.d3d=false -Dsun.java2d.noddraw=true -Dsun.java2d.trace=log

		SwingUtilities.invokeLater(() -> {
			mainFrame = new Frame("Main Frame");
			mainFrame.setSize(600, 420);
			mainFrame.setLayout(new BorderLayout());
			mainFrame.addWindowListener(new java.awt.event.WindowAdapter() {
				@Override
				public void windowClosing(java.awt.event.WindowEvent we) {
					mainFrame.dispose();
					if (childFrame != null)
						childFrame.dispose();
					System.exit(0);
				}
			});

			mainFrame.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentResized(ComponentEvent e) {
					SwingUtilities.invokeLater(ChatGPT::resizedChild);
				}

				@Override
				public void componentMoved(ComponentEvent e) {
					SwingUtilities.invokeLater(ChatGPT::resizedChild);
				}
			});

			mainFrame.setVisible(true);
		});

		// WAIT: attendre que la fenêtre soit visible / displayable avant de chercher HWND
		hwndMainFrame = waitForWindowHandle("Main Frame", 5000);
		if (hwndMainFrame == null) {
			System.err.println("Cannot find HWND for Main Frame -- aborting");
			System.exit(2);
		}

		// Maintenant créer le WEmbeddedFrame **après** que le parent existe.
		SwingUtilities.invokeLater(() -> {
			try {
				// Désactiver temporairement le double buffering pendant la phase de reparent/creation
				RepaintManager rm = RepaintManager.currentManager(null);
				boolean oldDoubleBuffering = rm.isDoubleBufferingEnabled();
				rm.setDoubleBufferingEnabled(false);
				try {
					// Construire le WEmbeddedFrame en lui passant le handle parent
					String className = "sun.awt.windows.WEmbeddedFrame";
					Class<?> clazz = Class.forName(className);
					Constructor<?> ctor = clazz.getConstructor(long.class);
					long parentPtr = Pointer.nativeValue(hwndMainFrame.getPointer());
					childFrame = (Frame) ctor.newInstance(parentPtr);

					// Ajout UI
					JApplet applet = new JApplet();
					applet.setLayout(new BorderLayout());
					JButton button = new JButton("<html>Move the mainframe to test reparenting<br/>(WEmbeddedFrame)</html>");
					applet.add(button, BorderLayout.CENTER);
					childFrame.add(applet);

					Dimension mainFrameDim = getAvailableSpaceInMainFrame();
					childFrame.setSize(mainFrameDim.width, mainFrameDim.height);
					childFrame.setLocation(0, 0);
					childFrame.validate();
					childFrame.setVisible(true);
				} finally {
					// Restaurer le double buffering
					rm.setDoubleBufferingEnabled(oldDoubleBuffering);
				}
			} catch (Throwable t) {
				t.printStackTrace();
				JOptionPane.showMessageDialog(null, "WEmbeddedFrame error: " + t);
			}
		});
	}

	/** Selon chatGPT: construire la fentre principale dans un InvokeAndWait ne suffit pas toujours,
	 * selon la charge graphique, le look & feel, la machine distante (ex : RDP, GPU, timing...),
	 * le peer peut être créé légèrement plus tard... résultat : FindWindow(...) retourne null, et
	 * ton reparenting échoue ou crée une fenêtre noire. */
	private static HWND waitForWindowHandle(String title, long timeoutMs) {
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < deadline) {
			HWND hwnd = User32.INSTANCE.FindWindow(null, title);
			if (hwnd != null && Pointer.nativeValue(hwnd.getPointer()) != 0) {
				return hwnd;
			}
			try {
				Thread.sleep(100); // petit délai ; pas bloquer l'EDT ici (on est hors EDT)
			} catch (InterruptedException ignored) {
			}
		}
		return null;
	}

	private static void resizedChild() {
		if (childFrame == null || mainFrame == null)
			return;
		SwingUtilities.invokeLater(() -> {
			Dimension size = mainFrame.getSize();
			childFrame.setSize(Math.max(0, size.width - 80), Math.max(0, size.height - 80));
			childFrame.setLocation(0, 0);
			childFrame.revalidate();
			childFrame.repaint();
		});
	}
}