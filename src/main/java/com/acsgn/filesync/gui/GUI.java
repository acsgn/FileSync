package gui;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JFileChooser;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;

import main.Controller;
import main.Listener;

import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class GUI extends JFrame implements Listener {
	private static final long serialVersionUID = 1L;

	private int width = Toolkit.getDefaultToolkit().getScreenSize().width / 3;
	private int height = Toolkit.getDefaultToolkit().getScreenSize().height / 2;

	private Font font = new Font("Tahoma", Font.PLAIN, width * 3 / 80);

	private JTextArea informationText;

	private String folder;
	private boolean start = true;

	public GUI() {
		setTitle("FileSync");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setResizable(false);
		setBounds(width, height / 2, width, height);
		getContentPane().setLayout(null);

		int panelHeight = height - 35;
		int panelWidth = width - 6;

		int informationPanelHeight = 3 * panelHeight / 5;
		int controlPanelHeight = panelHeight - informationPanelHeight;

		int panelXMargin = panelWidth / 45;
		int panelYMargin = panelHeight / 36;

		JPanel informationPanel = new JPanel(null);
		informationPanel.setBounds(0, 0, panelWidth, informationPanelHeight);

		informationText = new JTextArea();
		informationText.setEditable(false);
		informationText.setFont(font);
		JScrollPane informationScrollPane = new JScrollPane(informationText);
		informationScrollPane.setBounds(panelXMargin, panelYMargin, panelWidth - 2 * panelXMargin,
				informationPanelHeight - 2 * panelYMargin);
		informationPanel.add(informationScrollPane);

		getContentPane().add(informationPanel);

		JPanel controlPanel = new JPanel(null);
		controlPanel.setBounds(0, informationPanelHeight, panelWidth, controlPanelHeight);

		ButtonGroup buttonGroup = new ButtonGroup();

		JRadioButton masterButton = new JRadioButton("Master");
		masterButton.setBounds(panelXMargin, 0, panelWidth / 4 - 2 * panelXMargin,
				controlPanelHeight / 2 - panelYMargin);
		masterButton.setFont(font);
		masterButton.setHorizontalAlignment(SwingConstants.CENTER);
		buttonGroup.add(masterButton);
		controlPanel.add(masterButton);

		JRadioButton followerButton = new JRadioButton("Follower");
		followerButton.setBounds(panelXMargin, controlPanelHeight / 2, panelWidth / 4 - 2 * panelXMargin,
				controlPanelHeight / 2 - panelYMargin);
		followerButton.setFont(font);
		followerButton.setHorizontalAlignment(SwingConstants.CENTER);
		followerButton.setSelected(true);
		buttonGroup.add(followerButton);
		controlPanel.add(followerButton);

		JPanel TimePanel = new JPanel(null);
		TimePanel.setBounds(panelXMargin + panelWidth / 4, 0, panelWidth / 3 - 2 * panelXMargin,
				controlPanelHeight / 2 - panelYMargin);

		JLabel TimeLabel = new JLabel("Time Interval:");
		TimeLabel.setBounds(0, 0, panelWidth / 3 - 2 * panelXMargin, (controlPanelHeight / 2 - panelYMargin) / 2);
		TimeLabel.setHorizontalAlignment(SwingConstants.CENTER);
		TimeLabel.setFont(font);
		TimePanel.add(TimeLabel);

		JTextField TimeTextField = new JTextField();
		TimeTextField.setBounds(0, (controlPanelHeight / 2 - panelYMargin) / 2, panelWidth / 3 - 2 * panelXMargin,
				(controlPanelHeight / 2 - panelYMargin) / 2);
		TimeTextField.setHorizontalAlignment(SwingConstants.CENTER);
		TimeTextField.setFont(font);
		TimeTextField.setEnabled(false);
		TimePanel.add(TimeTextField);

		controlPanel.add(TimePanel);

		JPanel IPPanel = new JPanel(null);
		IPPanel.setBounds(panelXMargin + panelWidth / 4, controlPanelHeight / 2, panelWidth / 3 - 2 * panelXMargin,
				controlPanelHeight / 2 - panelYMargin);

		JLabel IPLabel = new JLabel("IP of Master:");
		IPLabel.setBounds(0, 0, panelWidth / 3 - 2 * panelXMargin, (controlPanelHeight / 2 - panelYMargin) / 2);
		IPLabel.setHorizontalAlignment(SwingConstants.CENTER);
		IPLabel.setFont(font);
		IPPanel.add(IPLabel);

		JTextField IPTextField = new JTextField();
		IPTextField.setBounds(0, (controlPanelHeight / 2 - panelYMargin) / 2, panelWidth / 3 - 2 * panelXMargin,
				(controlPanelHeight / 2 - panelYMargin) / 2);
		IPTextField.setHorizontalAlignment(SwingConstants.CENTER);
		IPTextField.setFont(font);
		IPPanel.add(IPTextField);

		controlPanel.add(IPPanel);

		JButton folderButton = new JButton("Choose Folder");
		folderButton.setBounds(panelXMargin + 7 * panelWidth / 12, 0, 5 * panelWidth / 12 - 2 * panelXMargin,
				controlPanelHeight / 2 - panelYMargin);
		folderButton.setFont(font);
		controlPanel.add(folderButton);

		JButton startStopButton = new JButton("Start");
		startStopButton.setBounds(panelXMargin + 7 * panelWidth / 12, controlPanelHeight / 2,
				5 * panelWidth / 12 - 2 * panelXMargin, controlPanelHeight / 2 - panelYMargin);
		startStopButton.setFont(font);
		controlPanel.add(startStopButton);

		getContentPane().add(controlPanel);

		setVisible(true);

		followerButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				TimeTextField.setEnabled(false);
				IPTextField.setEnabled(true);
			}
		});

		masterButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				TimeTextField.setEnabled(true);
				IPTextField.setEnabled(false);
			}
		});

		JFileChooser chooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setAcceptAllFileFilterUsed(false);
		chooser.setFileFilter(new FileFilter() {
			public String getDescription() {
				return "Only folders";
			}

			public boolean accept(File file) {
				return file.isDirectory();
			}
		});

		folderButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
					folder = chooser.getSelectedFile().getPath();
			}
		});

		startStopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (start) {
					if (folder == null) {
						JOptionPane.showMessageDialog(null, "Please select a folder to sync !", "Error",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
					if (followerButton.isSelected()) {
						String IP = IPTextField.getText();
						if (!isLegitIP(IP)) {
							JOptionPane.showMessageDialog(null, "Please enter a valid IP Address (e.g. 192.168.1.2) !",
									"Error", JOptionPane.ERROR_MESSAGE);
							return;
						}
						Controller.getInstance().dispatchMessage("FOLDER/" + folder);
						Controller.getInstance().dispatchMessage("FOLLOWER/" + IP);
					} else {
						String timeText = TimeTextField.getText();
						int time;
						try {
							time = Integer.parseInt(timeText);
						} catch (NumberFormatException numberException) {
							JOptionPane.showMessageDialog(null, "Please enter a valid time interval for synchronization !",
									"Error", JOptionPane.ERROR_MESSAGE);
							return;
						}
						Controller.getInstance().dispatchMessage("FOLDER/" + folder);
						Controller.getInstance().dispatchMessage("MASTER/" + time);
					}
					startStopButton.setText("Stop");
					start = false;
				} else {
					startStopButton.setText("Start");
					Controller.getInstance().dispatchMessage("STOP");
					start = true;
				}
			}

			private boolean isLegitIP(String IP) {
				String[] areas = IP.split("\\.");
				if (areas.length != 4)
					return false;
				for (String ip : areas) {
					int number;
					try {
						number = Integer.parseInt(ip);
					} catch (NumberFormatException numberException) {
						return false;
					}
					if (number < 0 || number > 255) {
						return false;
					}
				}
				return true;
			}
		});

	}

	@Override
	public void onEvent(String message) {
		String[] parsed = message.split("/");
		switch (parsed[0]) {
		case "UPDATE":
			informationText.insert(parsed[1], 0);
			break;
		}

	}

}