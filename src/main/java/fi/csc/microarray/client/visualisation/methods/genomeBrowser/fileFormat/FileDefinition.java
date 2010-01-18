package fi.csc.microarray.client.visualisation.methods.genomeBrowser.fileFormat;

import java.util.Collection;
import java.util.LinkedList;


public class FileDefinition extends LinkedList<ColumnDefinition> {
	
	public FileDefinition(Collection<ColumnDefinition> content){
		
		int offset = 0;
		
		for(ColumnDefinition column : content){
			
			column.offset = offset;
			this.add(column);
			offset += column.length;
		}
	}
	
	public ColumnDefinition getFieldDef(ColumnType type){
		for(ColumnDefinition field: this){
			if ( field.content == type){
				return field;
			}
		}
		
		return null;
	}
	
	public int indexOf(ColumnType content){
		int i = 0;
		for(ColumnDefinition field : this){
			if(field.content == content){
				return i;
			}
			i++;
		}
		return -1;
	}	
}
