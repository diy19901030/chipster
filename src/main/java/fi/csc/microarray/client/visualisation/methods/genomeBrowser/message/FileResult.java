package fi.csc.microarray.client.visualisation.methods.genomeBrowser.message;

import fi.csc.microarray.client.visualisation.methods.genomeBrowser.fileFormat.FileParser;


	
	public class FileResult{
		/**
		 * @param fileRequest
		 * @param avg
		 * @param requestQueueSize only to update user interface
		 */
		public FileResult(FileRequest fileRequest, FileParser inputParser, FsfStatus status) {
			this.request = fileRequest;
			this.chunkParser = inputParser;
			this.status = status;
		}
		public FileRequest request;
		public FileParser chunkParser;
		public FsfStatus status;
	}