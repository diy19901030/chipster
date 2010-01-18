package fi.csc.microarray.client.visualisation.methods.genomeBrowser.fileFormat;



	
	public class ColumnDefinition{				
		
		public ColumnDefinition(ColumnType content, Type type) {
			this(content, type, TAB_DELIM);			
		}
		
		public ColumnDefinition(ColumnType content, Type type,
				int length) {
			this.content = content;
			this.type = type;
			this.length = length;
		}
		public ColumnType content;
		public Type type;
		public int length; //in bytes
		public long offset; //Sum of preceding columns, initialised in Translator constructor
		
		public static final int TAB_DELIM = -1;						
	}