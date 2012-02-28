package fi.csc.microarray.client.visualisation.methods.gbrowser;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.WindowConstants;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fi.csc.microarray.client.visualisation.methods.gbrowser.PreviewManager.GBrowserPreview;
import fi.csc.microarray.client.visualisation.methods.gbrowser.PreviewManager.PreviewUpdateListener;
import fi.csc.microarray.client.visualisation.methods.gbrowser.message.Chromosome;
import fi.csc.microarray.client.visualisation.methods.gbrowser.message.Region;

/**
 * Example class to demonstrate and test PreviewManager. Contains lot of GUI code, but the relevant 
 * parts of the PreviewManager are quite simple:
 * 
 * First preview has to created:
 * 		public  GBrowserPreview createPreview(Region region, File bamData, File bamIndex, File cytobandData, File cytobandRegions, File gtfAnnotation)
	
	When you have created the GBrowserPreview object with the method above, you set it to show
	correct location in data:
		public void setRegion(Region region);
		
	Get the image of the visualisation (image is buffered, so this can be called often, e.g. on every
	frame):	
		public BufferedImage getPreview();
		
	Or the actual Swing component: 
		public JComponent getJComponent();
		
	If you can't use the Swing component directly, you can open it to new window:
		public JFrame showFrame();
	Make sure that you keep the returned JFrame object, if you need programmable access to that
	frame later.
	
	If you wan't to be notified when preview is updated, there is possibility to add listener:
		public void addPreviewUpdateListener(PreviewUpdateListener l);
		public void removePreviewUpdateListener(PreviewUpdateListener l);
	However, the previews don't currently know when the actual visualisation updates, but just
	execute fixed number of updates with fixed timer delay.

	Two visualisation can be opened side by side with either of following methods:
		public JComponent getSplitJComponent(GBrowserPreview first, GBrowserPreview second);
		public JFrame showSplitFrame(GBrowserPreview first, GBrowserPreview second);
 * 
 *  And finally useless visualisation should be removed with the following:
 * 		public void removePreview(GBrowserPreview preview);	
 *  However, currently there is a huge memory leak, around 100 megabytes per visualisation.

 * 
 * @author klemela
 *
 */
public class TrackViewDemo extends JFrame implements ActionListener, ChangeListener{

	private static final File BAM_DATA_FILE;
	private static final File BAI_DATA_FILE;
	private static final File CYTOBAND_FILE;
	private static final File CYTOBAND_REGION_FILE;
	private static final File GTF_ANNOTATION_FILE;

	private static final String dataPath;

	static {

		dataPath = System.getProperty("user.home") + "/chipster/ohtu/";

		BAM_DATA_FILE = new File(dataPath + "ohtu-within-chr.bam");
		BAI_DATA_FILE = new File(dataPath + "ohtu-within-chr.bam.bai");
		CYTOBAND_FILE = new File(dataPath + "Homo_sapiens.GRCh37.65.cytobands.txt");
		CYTOBAND_REGION_FILE = new File(dataPath + "Homo_sapiens.GRCh37.65.seq_region.txt");
		
//ftp://ftp.ensembl.org/pub/release-65/gtf/homo_sapiens/Homo_sapiens.GRCh37.65.gtf.gz
		GTF_ANNOTATION_FILE = new File(dataPath + "Homo_sapiens.GRCh37.65.gtf");
	}

	public static void main(String[] args) throws IOException {

		TrackViewDemo frame = new TrackViewDemo();
		frame.pack();
		frame.setVisible(true);
	}


	protected JPanel settingsPanel;
	protected JPanel previewPanel;
	protected JPanel visualizationPanel;

	private JSlider chrSlider;
	private JSlider locationSlider;
	private JSlider sizeSlider;
	private JButton goButton;

	private JLabel chrLabel;
	private JLabel locationLabel;
	private JLabel sizeLabel;

	private PreviewManager previewManager = new PreviewManager();

	public TrackViewDemo() {

		settingsPanel = new JPanel();
		previewPanel = new JPanel();
		visualizationPanel = new JPanel();

		chrLabel = new JLabel();
		locationLabel = new JLabel();
		sizeLabel = new JLabel();

		chrSlider = new JSlider(1, 20, 1);
		locationSlider = new JSlider(1, 270000000, 52030000); //about the size of the biggest chromosomes, just some interesting location
		sizeSlider = new JSlider(1, 1000000, 10000);
		goButton = new JButton("Go");

		this.stateChanged(null); //Init JLabels

		chrSlider.addChangeListener(this);
		locationSlider.addChangeListener(this);
		sizeSlider.addChangeListener(this);

		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		settingsPanel.setPreferredSize(new Dimension(1024, 30));
		previewPanel.setPreferredSize(new Dimension(1024, 130));
		visualizationPanel.setPreferredSize(new Dimension(1024, 576));
		visualizationPanel.setLayout(new BorderLayout());

		setLayout(new BorderLayout());
		add(settingsPanel, BorderLayout.NORTH);
		add(previewPanel, BorderLayout.CENTER);
		add(visualizationPanel, BorderLayout.SOUTH);

		goButton.addActionListener(this);

		settingsPanel.setLayout(new FlowLayout());
		settingsPanel.add(chrLabel);
		settingsPanel.add(chrSlider);
		settingsPanel.add(locationLabel);
		settingsPanel.add(locationSlider);
		settingsPanel.add(sizeLabel);
		settingsPanel.add(sizeSlider);
		settingsPanel.add(goButton);

		previewPanel.setLayout(new FlowLayout());
	}



	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(goButton)) {

			if (previewPanel.getComponentCount() == 0) {
				previewPanel.add(new JLabel("<html>Left click:  Open, Right click: New window<br>Double click: Close<br>Ctrl click two: Split</html>"));
			}

			long start = locationSlider.getValue();
			long end = start + sizeSlider.getValue();
			Chromosome chr = new Chromosome("" + chrSlider.getValue());

			Region region = new Region(start, end, chr);
			GBrowserPreview preview = previewManager.createPreview(region, BAM_DATA_FILE, BAI_DATA_FILE, CYTOBAND_FILE, CYTOBAND_REGION_FILE, GTF_ANNOTATION_FILE);

			JButton previewButton = new PreviewButton(preview, this);

			previewPanel.add(previewButton);
		}
	}

	private PreviewButton selection;

	class PreviewButton extends JButton implements MouseListener, PreviewUpdateListener {

		private GBrowserPreview preview;
		private TrackViewDemo parent;

		public PreviewButton(GBrowserPreview preview, TrackViewDemo parent) {

			this.setBorder(null);

			this.setPreferredSize(new Dimension(70, 46));
			this.preview = preview;
			this.parent = parent;

			preview.addPreviewUpdateListener(this);	

			this.addMouseListener(this);
		}
		
		private void showVisualization(JComponent component) {
			parent.visualizationPanel.removeAll();
			if (component != null) {
				parent.visualizationPanel.add(component, BorderLayout.CENTER);
				component.repaint();
			}
			parent.visualizationPanel.validate();
			parent.visualizationPanel.repaint();
		}
		
		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() > 1) {

				showVisualization(null);
				previewManager.removePreview(preview);

				preview.removePreviewUpdateListener(this);
				
				Container parent = this.getParent();
				parent.remove(this);
				parent.validate();
				parent.repaint();
				return;
			}

			if (e.getButton() == MouseEvent.BUTTON1) {
				if (e.isControlDown()) {
					if (selection == null) {
						selection = this;
						this.setBorder(new LineBorder(Color.black, 5));

					} else {
						selection.setBorder(null);
						showVisualization(null);
						showVisualization(previewManager.getSplitJComponent(preview, selection.preview));
					}
				} else {
					if (selection != null) {
						selection.setBorder(null);
						selection = null;
					}

					showVisualization(preview.getJComponent());
				}

			} else if (e.getButton() == MouseEvent.BUTTON3) {
				if (e.isControlDown()) {
					if (selection != null) {
						selection.setBorder(null);
						previewManager.showSplitFrame(preview, selection.preview);
					}
				} else {
					preview.showFrame();
				}
			}

		}
		@Override
		public void mouseEntered(MouseEvent arg0) {

		}
		@Override
		public void mouseExited(MouseEvent arg0) {

		}
		@Override
		public void mousePressed(MouseEvent arg0) {

		}
		@Override
		public void mouseReleased(MouseEvent arg0) {

		}

		@Override
		public void PreviewUpdated() {
			setIcon(new ImageIcon(preview.getPreview().getScaledInstance(64, 40, 0)));
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		chrLabel.setText("Chr: " + chrSlider.getValue());
		locationLabel.setText("Location: " + locationSlider.getValue());
		sizeLabel.setText("Size: " + sizeSlider.getValue());
	}
}

