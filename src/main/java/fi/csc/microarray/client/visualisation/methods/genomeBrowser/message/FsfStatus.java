package fi.csc.microarray.client.visualisation.methods.genomeBrowser.message;
import java.io.File;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;


public class FsfStatus {
	
	public boolean poison; // All threads should send this forward and end themselves
	public long areaRequestCount;
	public long fileRequestCount;
	public long fileResultCount;
//	public long fileThreadWait;
//	public long treeThreadWait;
	public boolean clearQueues;
	public boolean concise;
	public boolean debug;
	
	private Set<Object> clearedAlready = new HashSet<Object>();
	public File file;
	
	public void maybeClearQueue(Object fileResultQueue){
		if(clearQueues && !clearedAlready.contains(fileResultQueue)){

			clearedAlready.add(fileResultQueue);
			((Queue<?>)fileResultQueue).clear();			
		}
	}
}
