package fi.csc.microarray.client.visualisation.methods.gbrowser;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Timer;

import fi.csc.microarray.client.visualisation.methods.gbrowser.GenomePlot.ReadScale;
import fi.csc.microarray.client.visualisation.methods.gbrowser.dataFetcher.QueueManager;
import fi.csc.microarray.client.visualisation.methods.gbrowser.drawable.Drawable;
import fi.csc.microarray.client.visualisation.methods.gbrowser.fileFormat.ColumnType;
import fi.csc.microarray.client.visualisation.methods.gbrowser.message.AreaRequest;
import fi.csc.microarray.client.visualisation.methods.gbrowser.message.BpCoord;
import fi.csc.microarray.client.visualisation.methods.gbrowser.message.BpCoordDouble;
import fi.csc.microarray.client.visualisation.methods.gbrowser.message.BpCoordRegion;
import fi.csc.microarray.client.visualisation.methods.gbrowser.message.BpCoordRegionDouble;
import fi.csc.microarray.client.visualisation.methods.gbrowser.message.Chromosome;
import fi.csc.microarray.client.visualisation.methods.gbrowser.message.FsfStatus;
import fi.csc.microarray.client.visualisation.methods.gbrowser.track.ProfileTrack;
import fi.csc.microarray.client.visualisation.methods.gbrowser.track.RulerTrack;
import fi.csc.microarray.client.visualisation.methods.gbrowser.track.Track;
import fi.csc.microarray.client.visualisation.methods.gbrowser.track.TrackGroup;

/**
 * Combines track to create a single synchronised view. All tracks within one view move hand-in-hand. View is responsible
 * for allocating space and taking care of actual drawing.
 *
 */
public abstract class View implements MouseListener, MouseMotionListener, MouseWheelListener {

	protected BpCoordRegionDouble bpRegion;
	public BpCoordRegion highlight;

	public Collection<TrackGroup> trackGroups = new LinkedList<TrackGroup>();
	protected Rectangle viewArea = new Rectangle(0, 0, 500, 500);
	private QueueManager queueManager = new QueueManager();
	private Point2D dragStartPoint;
	private boolean dragStarted;

	public GenomePlot parentPlot;

	private static final int FPS = 30;

	private boolean movable;
	protected boolean zoomable;

	protected final float ZOOM_FACTOR = 1.06f;
	protected final float MIN_PIXELS_PER_NUCLEOTIDE = 10f;

	private List<RegionListener> listeners = new LinkedList<RegionListener>();
	public int margin = 0;
	protected Float trackHeight;
	private Point2D dragEndPoint;
	private Point2D dragLastStartPoint;
	private Iterator<Track> trackIter;
	private Iterator<Drawable> drawableIter;
	private boolean continueDrawingLater;
	private Track track;
	private float y;
	private int x;
	private BufferedImage drawBuffer;
	private long dragEventTime;
    private boolean isStatic;
	private static final long DRAG_EXPIRATION_TIME_MS = 50;

	public View(GenomePlot parent, boolean movable, boolean zoomable, boolean selectable) {
		this.parentPlot = parent;
		this.movable = movable;
		this.zoomable = zoomable;
	}

	protected abstract void drawDrawable(Graphics2D g, int x, int y, Drawable drawable);

	/**
	 * Add a single track to this view.
	 * 
	 * A track group with a single track is created.
	 * 
	 * @param track
	 */
    public void addTrack(Track track) {
	    trackGroups.add(new TrackGroup(track));
	}

    /**
     * Add a track group containing one or several tracks.
     * 
     * @param group
     */
	public void addTrackGroup(TrackGroup group) {
		trackGroups.add(group);
	}
	
	/**
	 * Get tracks contained in track groups as a linear collection.
	 */
	public Collection<Track> getTracks() {
	    Collection<Track> tracks = new LinkedList<Track>();
        for (TrackGroup trackGroup : trackGroups) {
            tracks.addAll(trackGroup.getTracks());
        }
        return tracks;
	}
	
	public BpCoord getMaxBp() {
		
		BpCoord max = null;
		
		for (Track t : getTracks()) {
			
			BpCoord trackMax = t.getMaxBp(bpRegion.start.chr);
			
			if (trackMax != null && (max == null || max.compareTo(trackMax) > 0)) {
				max = trackMax;
			}
		}
		
		if (max != null) {
			//Little bit empty space to the end
			max.bp += 10000;
			return max;
			
		} else {
			return null;
		}
	}

	protected void drawView(Graphics2D g, boolean isAnimation) {
	    
		if (bpRegion == null) {
			setBpRegion(new BpCoordRegionDouble(0d, 1024 * 1024 * 250d, new Chromosome("1")), false);
		}
		
        // Recalculate track heights
		updateTrackHeights();

		Rectangle viewClip = g.getClipBounds();
		viewArea = viewClip;

		if (drawBuffer == null) {
			drawBuffer = new BufferedImage((int) viewArea.getWidth(),
			        (int) viewArea.getHeight(), BufferedImage.TYPE_INT_RGB);

			Graphics2D bufG2 = (Graphics2D) drawBuffer.getGraphics();
			bufG2.setPaint(Color.white);
			bufG2.fillRect(0, 0, drawBuffer.getWidth(), drawBuffer.getHeight());
		}

		Graphics2D bufG2 = (Graphics2D) drawBuffer.getGraphics();
		bufG2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

		

		// DOCME what is this continueDrawingLater for?
        // long startTime = System.currentTimeMillis();
        continueDrawingLater = false;
        
        // prepare context object
        TrackContext trackContext = null;

        // prepare coordinates
        y = 0;
        x = 0;

        // track group contains one or several logically-related tracks
        for (TrackGroup group : trackGroups) {
            trackIter = group.getTracks().iterator();
            drawableIter = null;
            
            // draw side menu
            if (group.isMenuVisible()) {
                group.menu.setPosition((int) (viewArea.getX() + viewArea.getWidth()),
                        (int) (viewArea.getY() + y));
            }

            // draw all tracks
            while (trackIter.hasNext() || (drawableIter != null && drawableIter.hasNext())) {
    
                if (drawableIter == null || !drawableIter.hasNext()) {
                    track = trackIter.next();
                }
    
                // draw drawable objects for visible tracks
                if (track.isVisible()) {
                     
                    // decide if we will expand drawable for this track
                    boolean expandDrawables = track.canExpandDrawables();
                                       
                    // create view context for this track only if we will use it
                    // currently only used for tracks that contain information
                    // about reads
                    if (expandDrawables && track instanceof ProfileTrack) {
                        if (parentPlot.getReadScale() == ReadScale.AUTO) {
                            trackContext = new TrackContext(track);
                        } else {
                            // FIXME ReadScale is in "number of reads" and context takes "number of pixels"
                            trackContext = new TrackContext(track, track.getHeight() - parentPlot.getReadScale().numReads);
                        }
                    }
    
                    // get drawable iterator
                    if (drawableIter == null) {
                        Collection<Drawable> drawables = track.getDrawables();
                        drawableIter = drawables.iterator();
                    }
    
                    while (drawableIter.hasNext()) {
    
                        Drawable drawable = drawableIter.next();
                        
                        if(drawable == null) {
                            continue;
                        }
                        
                        // expand drawables to stretch across all height if necessary
                        if (expandDrawables) {
                            drawable.expand(trackContext);
                        }
    
                        // recalculate position for reversed strands
                        int maybeReversedY = (int) y;
                        if (track.isReversed()) {
                            drawable.upsideDown();
                            maybeReversedY += track.getHeight();
                        }
    
                        // draw an object onto the buffer
                        drawDrawable(bufG2, x, maybeReversedY, drawable);
    
    //                    if (System.currentTimeMillis() - startTime >= 1000 / FPS) {
    //                        continueDrawingLater = true;
    //                        this.redraw();
    //                        break;
    //                    }
                    }
    
                    if (continueDrawingLater) {
                        break;
                        
                    } else {
                        drawableIter = null;
                    }
                    
                } else {
                    drawableIter = null;
                }
                
                y += track.getHeight();
            }
        }
        
        g.drawImage(drawBuffer, (int) viewArea.getX(), (int) viewArea.getY(),
                (int) viewArea.getX() + drawBuffer.getWidth(),
                (int) viewArea.getY() + drawBuffer.getHeight(), 0, 0,
                drawBuffer.getWidth(), drawBuffer.getHeight(), null);

        if (!continueDrawingLater) {
            bufG2.setPaint(Color.white);
            bufG2.fillRect(0, 0, drawBuffer.getWidth(), drawBuffer.getHeight());
            trackIter = null;
            drawableIter = null;
        }
	}
	
	/**
	 * Update heights of tracks after zoom, resize etc.
	 */
	private void updateTrackHeights() {
        // Calculate height of stretchable tracks    
        for (Track t : getTracks()) {
            if (t.isStretchable()) {
                t.setHeight(Math.round(getTrackHeight()));
            }
        }
	}

	public float getTrackHeight() {
		trackHeight = (getHeight() - getStaticTrackHeightTotal()) /
		        (float) getStretchableTrackCount();		
		return trackHeight;
	}

	/**
	 * @return sum of heights of tracks with static heights.
	 */
	protected int getStaticTrackHeightTotal() {
		int staticHeightTotal = 0;

		for (Track track : getTracks()) {
			if (!track.isStretchable()) {
				staticHeightTotal += track.getHeight();
			}
		}
		return staticHeightTotal;
	}

	protected int getStretchableTrackCount() {
		int stretchableCount = 0;

		for (Track track : getTracks()) {
			if (track.isStretchable()) {
				stretchableCount++;
			}
		}
		return stretchableCount;
	}

	public int getWidth() {
		return this.viewArea.width;
	}

	public int getHeight() {
		return this.viewArea.height;
	}
	
    public void setHeight(int height) {
        this.viewArea.height = height;
    }
	
	public boolean hasStaticHeight() {
        return isStatic;
	}
	
	public void setStaticHeight(boolean isStatic) {
	    this.isStatic = isStatic;
	}

	public QueueManager getQueueManager() {
		return queueManager;
	}
	
	/**
	 * Fire area requests for all tracks in this view.
	 * 
	 * Only fire one request for a single file. If two tracks ask
	 * for the same file and one of them wants concise data while
	 * the other want wants precise, we should fire separate
	 * requests for them.
	 */
	public void fireAreaRequests() {
	    // Concise data
        Map<DataSource, Set<ColumnType>> conciseDatas = new
                HashMap<DataSource, Set<ColumnType>>();
        // Precise data
        Map<DataSource, Set<ColumnType>> preciseDatas = new
                HashMap<DataSource, Set<ColumnType>>();
        
        // Add all requested columns for each requested file 
        for (Track t : getTracks()) {
            Map<DataSource, Set<ColumnType>> trackDatas = t.requestedData();
            
            // Don't do anything for hidden tracks or tracks without data
            if (trackDatas == null || !t.isVisible()) {
                continue;
            }
            
            for (DataSource file : trackDatas.keySet()) {
                if (file != null) {
                    // Handle concise and precise requests separately
                    Map<DataSource, Set<ColumnType>> datas;
                    datas = preciseDatas;
                    if (t.isConcised()) {
                        datas = conciseDatas;
                    }
                    // Add columns for this requested file
                    Set<ColumnType> columns = datas.get(file);
                    columns = columns != null ? columns : new HashSet<ColumnType>();
                    columns.addAll(trackDatas.get(file));
                    datas.put(file, columns);
                }
            }
        }
        
        // Fire area requests for concise requests
        for (DataSource file : conciseDatas.keySet()) {
            FsfStatus status = new FsfStatus();
            status.clearQueues = true;
            status.concise = true;
            getQueueManager().addAreaRequest(file,
                    new AreaRequest(getBpRegion(), conciseDatas.get(file), status), true);
        }
        
        // Fire area requests for precise requests
        for (DataSource file : preciseDatas.keySet()) {
            FsfStatus status = new FsfStatus();
            status.clearQueues = true;
            status.concise = false;
            getQueueManager().addAreaRequest(file,
                    new AreaRequest(getBpRegion(), preciseDatas.get(file), status), true);
        }
	}

	public void setBpRegion(BpCoordRegionDouble region, boolean disableDrawing) {
		this.bpRegion = region;

		// Bp-region change may change visibility of tracks, calculate sizes again
		trackHeight = null;

		if (!disableDrawing) {
		    fireAreaRequests();            
			dispatchRegionChange();
		}
	}

	public BpCoordRegionDouble getBpRegionDouble() {
		return bpRegion;
	}

	public BpCoordRegion getBpRegion() {
		return new BpCoordRegion((long) (double) bpRegion.start.bp, bpRegion.start.chr, (long) (double) bpRegion.end.bp, bpRegion.end.chr);
	}

	public void mouseClicked(MouseEvent e) {
		// ignore
	}

	public void mouseEntered(MouseEvent e) {
		// ignore
	}

	public void mouseExited(MouseEvent e) {
		// ignore
	}

	public void mousePressed(MouseEvent e) {

		stopAnimation();
		dragStartPoint = scale(e.getPoint());
		dragStarted = false;
	}

	public void mouseReleased(MouseEvent e) {

		if (dragStarted && dragEndPoint != null && dragLastStartPoint != null && Math.abs(dragEndPoint.getX() - dragLastStartPoint.getX()) > 10 && System.currentTimeMillis() - dragEventTime < DRAG_EXPIRATION_TIME_MS) {

			stopAnimation();

			timer = new Timer(1000 / FPS, new ActionListener() {

				private int i = 0;
				private int ANIMATION_FRAMES = 30;
				private long startTime = System.currentTimeMillis();

				public void actionPerformed(ActionEvent arg0) {

					double endX = dragEndPoint.getX();
					double startX = dragLastStartPoint.getX();

					double newX = endX - (endX - startX) / (ANIMATION_FRAMES - i);

					dragEndPoint = new Point2D.Double(newX, dragEndPoint.getY());

					boolean skipFrame = (i < (ANIMATION_FRAMES - 1)) && System.currentTimeMillis() > startTime + (1000 / FPS) * i;

					if (i < ANIMATION_FRAMES) {
						handleDrag(dragLastStartPoint, dragEndPoint, skipFrame);
						i++;
					} else {
						stopAnimation();
					}
				}
			});
			timer.setRepeats(true);
			timer.start();
		}
	}

	public void mouseDragged(MouseEvent e) {
		
		if (movable) {
			dragStarted = true;
			dragEndPoint = scale(e.getPoint());
			dragEventTime = System.currentTimeMillis();

			handleDrag(dragStartPoint, dragEndPoint, false);

		}
		dragLastStartPoint = dragStartPoint;
		dragStartPoint = scale(e.getPoint());
	}

	public void mouseMoved(MouseEvent e) {
		// ignore
	}

	protected abstract void handleDrag(Point2D start, Point2D end, boolean disableDrawing);

	private Timer timer;

	public void mouseWheelMoved(final MouseWheelEvent e) {

		stopAnimation();

		timer = new Timer(1000 / FPS, new ActionListener() {

			private int i = 0;

			// 100 ms to give time for slower machines to view couple animation frames also
			private long startTime = System.currentTimeMillis() + 100;
			private int ANIMATION_FRAMES = 15;

			public void actionPerformed(ActionEvent arg0) {

				boolean skipFrame = (i < (ANIMATION_FRAMES - 1)) && System.currentTimeMillis() > startTime + (1000 / FPS) * i;

				if (i < ANIMATION_FRAMES) {
					zoom((int) scale(e.getPoint()).getX(), e.getWheelRotation(), skipFrame);
					i++;
					
				} else {
					stopAnimation();
				}
			}
		});
		
		timer.setRepeats(true);
		timer.setCoalesce(false);
		timer.start();
	}

	private void stopAnimation() {
		if (timer != null) {
			timer.stop();
			timer = null;
		}
	}

	private void zoom(int lockedX, int wheelRotation, boolean disableDrawing) {

		// not all views are zoomed (e.g., the overview with cytoband) 
		if (zoomable) {
			
			// zoom out
			//if (wheelRotation > 0) {
		    // DOCME why?
			//	lockedX = (int) getWidth() - lockedX + getX() * 2;
			//}

			BpCoordDouble pointerBp = trackToBp(lockedX);
			double pointerRelative = trackToRelative(lockedX);

			double startBp = getBpRegionDouble().start.bp;
			double endBp = getBpRegionDouble().end.bp;			
						
			double width = endBp - startBp;
			width *= Math.pow(ZOOM_FACTOR, wheelRotation);

			int minBpWidth = (int)(((float)parentPlot.chartPanel.getPreferredSize().getSize().width) / MIN_PIXELS_PER_NUCLEOTIDE);
			if (width < minBpWidth) {
				width = minBpWidth;
			}

			startBp = (double) (pointerBp.bp - width * pointerRelative);
			endBp = (double) (pointerBp.bp + width * (1 - pointerRelative));
			
			BpCoord maxBp = getMaxBp();
			
			if (startBp < 0) {
				endBp += -startBp;
				startBp = 0;
			}
			
			if (maxBp != null) {
				// check bounds
				long maxBpVal = maxBp.bp;

				if (endBp > maxBpVal) {
					startBp -= endBp - maxBpVal;
					endBp = maxBpVal;

					if (startBp < 0) {
						startBp = 0;
					}
				}
			}
			setBpRegion(new BpCoordRegionDouble(startBp, getBpRegionDouble().start.chr, endBp, getBpRegionDouble().end.chr), disableDrawing);
		}
	}

	public Integer bpToTrack(BpCoord bp) {
		if (bpRegion.start.chr.equals(bp.chr)) {
			return (int) Math.round(((bp.bp - getBpRegionDouble().start.bp) * bpWidth()) + getX());
			
		} else {
			return null;
		}
	}

	/**
	 * Precisely convert bp coordinate to pixel position in this view.
	 * Rounding should be performed just before drawing.
	 * 
	 * @param bp
	 * @return
	 */
    public Float bpToTrackFloat(BpCoord bp) {
        if (bpRegion.start.chr.equals(bp.chr)) {
            return (float) ((bp.bp - getBpRegionDouble().start.bp) * bpWidth()) + getX();
            
        } else {
            return null;
        }
    }
	
	/**
	 * Calculates width of a single bp in pixels for this view.
	 * Number is a float, so the rounding should be performed just
	 * before drawing.
	 * 
	 * @return width of a single bp in pixels for this view.
	 */
	public Float bpWidth() {
	    return getWidth() / (float) getBpRegionDouble().getLength();
	}

	public BpCoordDouble trackToBp(double d) {
		return new BpCoordDouble((double) (trackToRelative(d) * (getBpRegionDouble().end.bp - getBpRegionDouble().start.bp) + getBpRegionDouble().start.bp), bpRegion.start.chr);
	}

	public double trackToRelative(double track) {
		return (double) (track - getX()) / getWidth();
	}

	public int getX() {
		return viewArea.x;
	}

	public int getY() {
		return viewArea.y;
	}

	public void redraw() {
		parentPlot.redraw();
	}

	public List<Long> getRulerInfo() {
		for (Track t : getTracks()) {
			if (t instanceof RulerTrack) {
				RulerTrack ruler = (RulerTrack) t;
				return ruler.getRulerInfo();
			}
		}
		return null;
	}

	public void addRegionListener(RegionListener listener) {
		listeners.add(listener);
	}

	public void dispatchRegionChange() {
		for (RegionListener listener : listeners) {
			listener.regionChanged(getBpRegion());
		}
	}

	private Point2D scale(Point2D p) {
		return new Point((int) (p.getX() / parentPlot.chartPanel.getScaleX()), (int) (p.getY() / parentPlot.chartPanel.getScaleY()));
	}
}
