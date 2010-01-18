package fi.csc.microarray.client.visualisation.methods.genomeBrowser.track;

import java.util.Arrays;
import java.util.Collection;

import fi.csc.microarray.client.visualisation.methods.genomeBrowser.View;
import fi.csc.microarray.client.visualisation.methods.genomeBrowser.drawable.Drawable;
import fi.csc.microarray.client.visualisation.methods.genomeBrowser.fileFormat.ColumnType;
import fi.csc.microarray.client.visualisation.methods.genomeBrowser.message.AreaResult;

public class EmptyTrack extends Track{

	private int height;

	public EmptyTrack(View view, int height) {
		super(view, null);
		this.height = height;
	}

	@Override
	public Collection<Drawable> getDrawables() {
		return getEmptyDrawCollection();
	}

	public void processAreaResult(AreaResult areaResult) {		
	}
	
	@Override
	public int getMaxHeight(){
		return height;
	}
	
	@Override
	public Collection<ColumnType> getDefaultContents() {
		return Arrays.asList(new ColumnType[] {}); 
	}
	
	@Override
	public boolean isConcised() {
		return false;
	}
}
