package fi.csc.microarray.client.visualisation.methods.genomeBrowser.dataFetcher;
	public class ByteChunk{
		
		public ByteChunk(int length){
			this.rowIndex = -1;
			this.byteLength = -1;
			byteContent = new byte[length];;
		}
		
		public long rowIndex;
		public long byteLength;
		public byte[] byteContent;
	}