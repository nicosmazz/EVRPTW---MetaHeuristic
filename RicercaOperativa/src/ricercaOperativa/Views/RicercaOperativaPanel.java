package ricercaOperativa.Views;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.BoxLayout;
import java.awt.BorderLayout;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

@SuppressWarnings("serial")
public class RicercaOperativaPanel extends JPanel {
	private JProgressBar progressBar;
	private JLabel lblLeRotteSono;
	public RicercaOperativaPanel() {
		setLayout(new BorderLayout(0, 0));
		
		JPanel contenitore = new JPanel();
		add(contenitore, BorderLayout.CENTER);
		contenitore.setLayout(new BorderLayout(0, 0));
		
		lblLeRotteSono = new JLabel("Le rotte sono in fase di calcolo");
		lblLeRotteSono.setHorizontalAlignment(SwingConstants.CENTER);
		contenitore.add(lblLeRotteSono);
		
		JPanel panel = new JPanel();
		add(panel, BorderLayout.SOUTH);
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		
		progressBar = new JProgressBar();
		progressBar.setIndeterminate(true);
		progressBar.setVisible(false);
		panel.add(progressBar);
	}
	public JProgressBar getProgressBar() {
		return progressBar;
	}
	public JLabel getLblLeRotteSono() {
		return lblLeRotteSono;
	}

}
