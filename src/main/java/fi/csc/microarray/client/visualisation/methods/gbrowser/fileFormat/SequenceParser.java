package fi.csc.microarray.client.visualisation.methods.gbrowser.fileFormat;

import java.util.Arrays;

import fi.csc.microarray.client.visualisation.methods.gbrowser.dataFetcher.Chunk;
import fi.csc.microarray.client.visualisation.methods.gbrowser.message.RegionContent;

/**
 * Parses the internal format of reference genome files.
 * 
 * @author Petri Klemelä
 *
 */
public class SequenceParser extends TsvParser {

	private static FileDefinition fileDef = new FileDefinition(
			Arrays.asList(
					new ColumnDefinition[] {				
							new ColumnDefinition(ColumnType.CHROMOSOME, Type.LONG),
							new ColumnDefinition(ColumnType.BP_START, Type.LONG),
							new ColumnDefinition(ColumnType.BP_END, Type.LONG),
							new ColumnDefinition(ColumnType.STRAND, Type.STRING),							
							new ColumnDefinition(ColumnType.SEQUENCE, Type.STRING) 
					}));
	
	public SequenceParser() {
		super(fileDef);
	}

	@Override
	public RegionContent[] concise(Chunk chunk) {

		// Return empty table, otherwise TreeNode gets stuck in calling this again
		return new RegionContent[0];
	}

	@Override
	public String getName() {
		return "Chipster sequence file";
	}
}