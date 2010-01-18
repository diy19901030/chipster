package fi.csc.microarray.client.visualisation.methods.genomeBrowser.track;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;

import fi.csc.microarray.client.visualisation.methods.genomeBrowser.View;
import fi.csc.microarray.client.visualisation.methods.genomeBrowser.drawable.Drawable;
import fi.csc.microarray.client.visualisation.methods.genomeBrowser.drawable.LineDrawable;
import fi.csc.microarray.client.visualisation.methods.genomeBrowser.fileFormat.ColumnType;
import fi.csc.microarray.client.visualisation.methods.genomeBrowser.message.AreaResult;

public class SeparatorTrack extends Track{

	public SeparatorTrack(View view) {
		super(view, null);
	}

	@Override
	public Collection<Drawable> getDrawables() {
		Collection<Drawable> drawables = getEmptyDrawCollection();
		drawables.add(new LineDrawable(0, 1, getView().getWidth(), 1, Color.gray));
		
		return drawables;				
	}

	public void processAreaResult(AreaResult areaResult) {		
	}
	
	@Override
	public int getMaxHeight(){
		return 3;
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