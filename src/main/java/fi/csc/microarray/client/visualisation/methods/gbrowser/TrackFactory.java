package fi.csc.microarray.client.visualisation.methods.gbrowser;

import java.awt.Color;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.List;

import fi.csc.microarray.client.visualisation.methods.gbrowser.dataFetcher.TreeThread;
import fi.csc.microarray.client.visualisation.methods.gbrowser.fileFormat.BEDParser;
import fi.csc.microarray.client.visualisation.methods.gbrowser.fileFormat.CytobandParser;
import fi.csc.microarray.client.visualisation.methods.gbrowser.fileFormat.ElandParser;
import fi.csc.microarray.client.visualisation.methods.gbrowser.fileFormat.GeneParser;
import fi.csc.microarray.client.visualisation.methods.gbrowser.fileFormat.HeaderTsvParser;
import fi.csc.microarray.client.visualisation.methods.gbrowser.fileFormat.SequenceParser;
import fi.csc.microarray.client.visualisation.methods.gbrowser.fileFormat.Strand;
import fi.csc.microarray.client.visualisation.methods.gbrowser.fileFormat.TranscriptParser;
import fi.csc.microarray.client.visualisation.methods.gbrowser.fileFormat.miRNAParser;
import fi.csc.microarray.client.visualisation.methods.gbrowser.track.CytobandTrack;
import fi.csc.microarray.client.visualisation.methods.gbrowser.track.GeneTrack;
import fi.csc.microarray.client.visualisation.methods.gbrowser.track.IntensityTrack;
import fi.csc.microarray.client.visualisation.methods.gbrowser.track.PeakTrack;
import fi.csc.microarray.client.visualisation.methods.gbrowser.track.ProfileTrack;
import fi.csc.microarray.client.visualisation.methods.gbrowser.track.RulerTrack;
import fi.csc.microarray.client.visualisation.methods.gbrowser.track.SeparatorTrack;
import fi.csc.microarray.client.visualisation.methods.gbrowser.track.SeqBlockTrack;
import fi.csc.microarray.client.visualisation.methods.gbrowser.track.SeqTrack;
import fi.csc.microarray.client.visualisation.methods.gbrowser.track.Track;
import fi.csc.microarray.client.visualisation.methods.gbrowser.track.TranscriptTrack;
import fi.csc.microarray.client.visualisation.methods.gbrowser.track.TranscriptTrack.PartColor;

public class TrackFactory {
	
	public static void addGeneTracks(GenomePlot genomePlot, DataSource geneAnnotationFile, DataSource transcriptAnnotationFile) {

		// initialise data source files
		GeneParser geneParser = new GeneParser();
		TranscriptParser transcriptParser = new TranscriptParser();
		View dataView = genomePlot.getDataView();
		
		// Gene, overview, forward 
		IntensityTrack annotationOverview = new IntensityTrack(genomePlot.getDataView(), geneAnnotationFile, TreeThread.class, geneParser, PartColor.CDS.c, 10000000);
		annotationOverview.setStrand(Strand.FORWARD);
		addTrack(dataView, annotationOverview);

		// Gene, detailed, forward
		GeneTrack annotation = new GeneTrack(genomePlot.getDataView(), geneAnnotationFile, TreeThread.class, geneParser, PartColor.CDS.c, 0, 10000000);
		annotation.setStrand(Strand.FORWARD);
		addTrack(dataView, annotation);
		
		// Add Transcript tracks for both strands
		for (Strand strand : Strand.values()) {

			// Transcript, overview
			IntensityTrack transcriptOverview = new IntensityTrack(dataView, transcriptAnnotationFile, TreeThread.class, transcriptParser, PartColor.CDS.c.darker(), 100000);
			transcriptOverview.setStrand(strand);
			addTrack(dataView, transcriptOverview);

			// Transcript, detailed
			TranscriptTrack trancsript = new TranscriptTrack(dataView, transcriptAnnotationFile, TreeThread.class, transcriptParser, Color.DARK_GRAY, 100000);
			trancsript.setStrand(strand);
			addTrack(dataView, trancsript);

			if (strand == Strand.FORWARD) {
				addSeparatorTrack(genomePlot);
			}
		}
		
		// Gene, overview, reverse 
		IntensityTrack annotationOverviewReversed = new IntensityTrack(genomePlot.getDataView(), geneAnnotationFile, TreeThread.class, geneParser, PartColor.CDS.c, 10000000);
		annotationOverviewReversed.setStrand(Strand.REVERSED);
		addTrack(dataView, annotationOverviewReversed);

		// Gene, detailed, reverse
		GeneTrack annotationReversed = new GeneTrack(genomePlot.getDataView(), geneAnnotationFile, TreeThread.class, geneParser, PartColor.CDS.c, 0, 10000000);
		annotationReversed.setStrand(Strand.REVERSED);
		addTrack(dataView, annotationReversed);
	}

	private static void addSeparatorTrack(GenomePlot genomePlot) {
		addSeparatorTrack(genomePlot, Long.MAX_VALUE);
	}
	
	private static void addSeparatorTrack(GenomePlot genomePlot, long maxBpLength) {
		View dataView = genomePlot.getDataView();
		dataView.addTrack(new SeparatorTrack(dataView, Color.gray, 1, 0, maxBpLength));
	}

	static void addThickSeparatorTrack(GenomePlot genomePlot) {
		View dataView = genomePlot.getDataView();
		dataView.addTrack(new SeparatorTrack(dataView, Color.gray.brighter(), 4, 0, Long.MAX_VALUE));
	}

	
	public static void addReadTracks(GenomePlot genomePlot, List<DataSource> treatments, List<DataSource> controls, DataSource seqFile) throws FileNotFoundException, MalformedURLException {
		ElandParser userDataParser = new ElandParser();
		View dataView = genomePlot.getDataView();
		int switchViewsAt = 50000;

		// forward tracks, iterate over both arrays 
		for (int i = 0; i < (treatments.size() + controls.size()); i++) {

			//
			// Initialise
			// 
			
			boolean isTreatment = i < treatments.size();
			DataSource userData = isTreatment ? treatments.get(i) : controls.get(i-treatments.size());
			Color histogramColor = isTreatment ? Color.blue : Color.gray;
			Color fontColor = Color.black;
						
			// 
			// Forward
			//
			
			// Overview
			IntensityTrack readOverview = new IntensityTrack(dataView, userData, TreeThread.class, userDataParser, histogramColor, switchViewsAt);
			addTrack(dataView, readOverview);

			// Detailed
			SeqBlockTrack reads = new SeqBlockTrack(dataView, userData, TreeThread.class, userDataParser, fontColor, 0, switchViewsAt);
			addTrack(dataView, reads);

			addSeparatorTrack(genomePlot);

			//
			// Reference sequence
			//
			
			if (seqFile != null) {
				// Reference sequence
				SeqTrack seq = new SeqTrack(dataView, seqFile, TreeThread.class, new SequenceParser(), 800);
				addTrack(dataView, seq);
				addSeparatorTrack(genomePlot, 800);
			}

			//
			// Reverse
			//
			
			// Overview
			IntensityTrack readOverviewReversed = new IntensityTrack(dataView, userData, TreeThread.class, userDataParser, histogramColor, switchViewsAt);
			readOverviewReversed.setStrand(Strand.REVERSED);
			addTrack(dataView, readOverviewReversed);

			// Detailed
			SeqBlockTrack readsReversed = new SeqBlockTrack(dataView, userData, TreeThread.class, userDataParser, fontColor, 0, switchViewsAt);
			readsReversed.setStrand(Strand.REVERSED);
			addTrack(dataView, readsReversed);
		}
	}

	public static void addWigTrack(GenomePlot plot, DataSource peakFile) {
		miRNAParser miRNAParser = new miRNAParser();
		ProfileTrack annotation = new ProfileTrack(plot.getDataView(), peakFile, TreeThread.class, miRNAParser, Color.BLUE, 0, Long.MAX_VALUE);
		addTrack(plot.getDataView(), annotation);
	}
	
	public static void addPeakTrack(GenomePlot plot, DataSource peaks) {
		BEDParser bedParser = new BEDParser();
		View dataView = plot.getDataView();

		PeakTrack annotation = new PeakTrack(dataView, peaks, TreeThread.class, bedParser, Color.YELLOW, 0, Long.MAX_VALUE);
		addTrack(dataView, annotation);
	}

	public static void addHeaderPeakTrack(GenomePlot plot, DataSource peaks) {
		HeaderTsvParser headerTsvParser = new HeaderTsvParser();
		View dataView = plot.getDataView();

		PeakTrack annotation = new PeakTrack(dataView, peaks, TreeThread.class, headerTsvParser, Color.YELLOW, 0, Long.MAX_VALUE);
		addTrack(dataView, annotation);
	}

	public static void addTranscriptTracks(GenomePlot plot, DataSource transcriptAnnotationFile) {

	}

	public static void addMirnaTracks(GenomePlot genomePlot, DataSource miRNAFile) {
		miRNAParser miRNAParser = new miRNAParser();
		View dataView = genomePlot.getDataView();

		for (Strand strand : Strand.values()) {

			GeneTrack track = new GeneTrack(dataView, miRNAFile, TreeThread.class, miRNAParser, PartColor.CDS.c.darker(), 0, Long.MAX_VALUE);
			track.setStrand(strand);
			dataView.addTrack(track);
			track.initializeListener();

			if (strand == Strand.FORWARD) {
				addSeparatorTrack(genomePlot);
			}
		}
	}

	public static void addCytobandTracks(GenomePlot plot, DataSource cytobandFile) {
		CytobandTrack overviewCytobands = new CytobandTrack(plot.getOverviewView(), cytobandFile, TreeThread.class, new CytobandParser(), false);
		addTrack(plot.getOverviewView(), overviewCytobands);

		CytobandTrack cytobands = new CytobandTrack(plot.getDataView(), cytobandFile, TreeThread.class, new CytobandParser(), true);
		addTrack(plot.getDataView(), cytobands);
	}

	public static void addRulerTrack(GenomePlot plot) {
		plot.getDataView().addTrack(new RulerTrack(plot.getDataView()));
	}
	
	private static void addTrack(View view, Track track) {
		view.addTrack(track);
		track.initializeListener();
	}
	

}
