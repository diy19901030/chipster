package fi.csc.microarray.client.visualisation.methods.gbrowser.message;

import fi.csc.microarray.client.visualisation.methods.gbrowser.fileFormat.FileParser;

public class FileResult {

	public FileRequest request;
	public ByteRegion exactRegion;
	public FileParser chunkParser;
	public FsfStatus status;
	public String chunk;

	/**
	 * @param fileRequest
	 * @param avg
	 * @param requestQueueSize
	 *            only to update user interface
	 */
	public FileResult(String chunk, FileRequest fileRequest, FileParser inputParser, ByteRegion exactRegion, FsfStatus status) {
		this.request = fileRequest;
		this.chunkParser = inputParser;
		this.exactRegion = exactRegion;
		this.status = status;
		this.chunk = chunk;
	}

}