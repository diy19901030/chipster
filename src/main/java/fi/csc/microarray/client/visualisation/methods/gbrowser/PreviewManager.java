package fi.csc.microarray.client.visualisation.methods.gbrowser;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import org.jfree.chart.JFreeChart;

import fi.csc.microarray.client.visualisation.methods.gbrowser.dataFetcher.SAMHandlerThread;
import fi.csc.microarray.client.visualisation.methods.gbrowser.message.Chromosome;
import fi.csc.microarray.client.visualisation.methods.gbrowser.message.Region;
import fi.csc.microarray.client.visualisation.methods.gbrowser.message.RegionDouble;
import fi.csc.microarray.client.visualisation.methods.gbrowser.track.TrackGroup;

public class PreviewManager {

	protected static int PREVIEW_WIDTH = 1024;
	protected static int PREVIEW_HEIGHT = 576;
	protected static int PREVIEW_UPDATES = 20;

	public class GBrowserPreview {

		private BufferedImage preview;	
		private TooltipAugmentedChartPanel panel;
		private JFrame frame;
		private GenomePlot plot;
		private Timer previewTimer;
		private int previewTimerCounter;
		
		private File bamData;
		private File bamIndex;
		private File cytobandData;
		private File cytobandRegions;
		private File gtfAnnotations;
		
		public GBrowserPreview(File bamData, File bamIndex, File cytobandData, File cytobandRegions, File gtfAnnotation) {
			this.bamData = bamData;
			this.bamIndex = bamIndex;
			this.cytobandData = cytobandData;
			this.cytobandRegions = cytobandRegions;
			this.gtfAnnotations = gtfAnnotation;
		}
		
		private void initIfNeeded() {

			if (panel == null) {
				checkData();

				boolean horizontal = true;

				panel = new TooltipAugmentedChartPanel();

				try {
					plot = new GenomePlot(panel, horizontal);

					TrackFactory.addCytobandTracks(plot, new CytobandDataSource(cytobandData, cytobandRegions));

					TrackFactory.addTitleTrack(plot, "Annotations");		
					TrackFactory.addGeneTracks(plot, new LineDataSource(gtfAnnotations));
					TrackFactory.addThickSeparatorTrack(plot);

//					TrackGroup controlGroup = TrackFactory.addReadTracks(
//							plot, 
//							new SAMDataSource(bamData, bamIndex),
//							SAMHandlerThread.class,
//							null,
//							"Control"
//							);

					TrackGroup treatmentGroup = TrackFactory.addReadTracks(
							plot, 
							new SAMDataSource(bamData, bamIndex),
							SAMHandlerThread.class,
							null,
							"Treatment"
							);
					
					
//					controlGroup.showOrHide("ProfileSNPTrack", false);
//					controlGroup.showOrHide("GelTrack", false);
					treatmentGroup.showOrHide("ProfileSNPTrack", false);
					treatmentGroup.showOrHide("GelTrack", false);
					treatmentGroup.showOrHide("RepeatMaskerTrack", false);
					
					panel.setChart(new JFreeChart(plot));
					panel.setPreferredSize(new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));
					panel.setCursor(new Cursor(Cursor.HAND_CURSOR));

					RegionDouble region = new RegionDouble(144151000d, 144154000d, new Chromosome("1"));

					plot.getDataView().setBpRegion(region, false);
					plot.addDataRegionListener(new RegionListener() {
						@Override
						public void regionChanged(Region bpRegion) {
							previewTimerCounter = 0;
							previewTimer.start();
						}
					});

					for (View view : plot.getViews()){
						panel.addMouseListener(view);
						panel.addMouseMotionListener(view);
						panel.addMouseWheelListener(view);
					}

					preview = new BufferedImage(PREVIEW_WIDTH, PREVIEW_HEIGHT, BufferedImage.TYPE_INT_ARGB);
					previewTimerCounter = 0;

					previewTimer = new Timer(500, new ActionListener() {

						public void actionPerformed(ActionEvent arg0) {

							SwingUtilities.invokeLater(new Runnable() {

								public void run() {
									if (plot != null) {
										Graphics2D g2 = preview.createGraphics();					
										g2.setClip(0, 0, PREVIEW_WIDTH, PREVIEW_HEIGHT);			
										g2.clearRect(0, 0, PREVIEW_WIDTH, PREVIEW_HEIGHT);

										plot.draw(g2, g2.getClipBounds(), null, null, null);
									}

									previewTimerCounter++;					
									if (previewTimerCounter > PREVIEW_UPDATES) {
										previewTimer.stop();
									}
								}
							});
						}
					});
					previewTimer.start();	

				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}		
			}
		}

		public void setRegion(Region region) {
			initIfNeeded();

			plot.getDataView().setBpRegion(new RegionDouble(region), false);	
		}

		public BufferedImage getPreview() {

			return preview;
		}

		public JComponent getJComponent() {
			initIfNeeded();

			return panel;
		}
		
		public JComponent getSplitJComponent(GBrowserPreview other) {
					
			JComponent thisComponent = panel;
			JComponent otherComponent = other.getJComponent();
			thisComponent.setPreferredSize(new Dimension(PREVIEW_WIDTH / 2 - 10, PREVIEW_HEIGHT));
			otherComponent.setPreferredSize(new Dimension(PREVIEW_WIDTH / 2 - 10, PREVIEW_HEIGHT));
			
			JPanel left = new JPanel(new BorderLayout());
			JPanel right = new JPanel(new BorderLayout());
			
			left.add(thisComponent, BorderLayout.CENTER);
			right.add(otherComponent, BorderLayout.CENTER);
			
			
			JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
			split.setLeftComponent(left);
			split.setRightComponent(right);
	
			split.setResizeWeight(0.5);			
			return split;
		}

		public void showFrame() {
			initIfNeeded();

			if (frame == null) {
				frame = new JFrame();
				frame.add(panel);
				frame.pack();

				frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
			}

			frame.setVisible(true);
		}
		
		public void showSplitFrame(GBrowserPreview other) {

			if (frame == null) {
				frame = new JFrame();
				frame.add(getSplitJComponent(other));
				frame.pack();

				frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			}

			frame.setVisible(true);
		}

		private void clean() {
			
			if (!isCleaned()) {

				previewTimer.stop();
				previewTimer = null;
				
				for (View view : plot.getViews()) {
					view.clean();
				}
				
				if (frame != null) {
					frame.dispose();
				}
				
				frame = null;
				panel = null;
				plot = null;
			}
		}
		
		private boolean isCleaned() {
			return panel == null;
		}
		
		private void checkData() {
			File[] files = new File[] { bamData, bamIndex, cytobandData, cytobandRegions, gtfAnnotations };
			boolean fileNotFoundFail = false;
			for (File file : files) {
				if (!file.exists()) {
					System.err.println("File not found: " + file);
					fileNotFoundFail = true;
				}
			}
			if (fileNotFoundFail) {
				System.exit(1);
			}
		}
	}
	
	private List<GBrowserPreview> previews = new LinkedList<GBrowserPreview>();
	
	public  GBrowserPreview createPreview(Region region, File bamData, File bamIndex, File cytobandData, File cytobandRegions, File gtfAnnotation) {
		GBrowserPreview preview = new GBrowserPreview(bamData, bamIndex, cytobandData, cytobandRegions, gtfAnnotation);
		preview.setRegion(region);
		previews.add(preview);
				
		return preview;
	}
	
	public void removePreview(GBrowserPreview preview) {

		previews.remove(preview);
		preview.clean();
	}
}
