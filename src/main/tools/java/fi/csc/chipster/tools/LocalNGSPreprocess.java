package fi.csc.chipster.tools;

import java.io.File;

import fi.csc.chipster.tools.gbrowser.SamBamUtils;
import fi.csc.microarray.client.Session;
import fi.csc.microarray.client.operation.Operation;
import fi.csc.microarray.client.tasks.Task;
import fi.csc.microarray.client.visualisation.methods.gbrowser.fileFormat.ElandParser;
import fi.csc.microarray.client.visualisation.methods.gbrowser.fileFormat.TsvParser;
import fi.csc.microarray.databeans.DataBean;
import fi.csc.microarray.databeans.DataManager;

public class LocalNGSPreprocess implements Runnable {

	private static TsvParser[] parsers = {
			new ElandParser()
	};
	
	private Task task;
	
	public LocalNGSPreprocess(Task task) {
		this.task = task;
	}
	
	public static String getSADL() {
		
		StringBuffer fileFormats = new StringBuffer();
		for (int i = 0; i < parsers.length; i++) {
			fileFormats.append(parsers[i].getName() + ": " + parsers[i].getName());
			
			if (i < parsers.length - 1) {
				fileFormats.append(", ");
			}
		}
		
		// TODO more verbose name, name of the second parameter
		return 	"TOOL \"Preprocess\" / LocalNGSPreprocess.java: \"Preprocess NGS, treatment only\" (Sort primarily using chromosome and secondarily using start " +
				"location of the feature. File format is used to find columns containing " +
				"chromosome and start location. )" + "\n" +
				"INPUT in-treatment.txt: \"Treatment\" TYPE GENERIC" + "\n" +
				"OUTPUT treatment.txt: \"Treatment\"" + "\n" +
				"OUTPUT phenodata.tsv: \"Phenodata\"" + "\n" +
				"PARAMETER file.format: \"Data format\" TYPE [" + fileFormats + "] DEFAULT " + parsers[0].getName() + " (Format of the data)" + "\n";
 	}

	public void run() {
		DataManager dataManager = Session.getSession().getDataManager();
		
		try {

			for (DataBean inputDataBean : task.getInputs()) {
				File inputFile = dataManager.getLocalFile(inputDataBean);
				
				String outputName;
				String indexOutputName; 
				int fileExtensionStartPosition = inputFile.getName().lastIndexOf(".");
				if (fileExtensionStartPosition != -1) {
					outputName = inputFile.getName().substring(0, fileExtensionStartPosition) + "-preprocessed"; 
				} else {
					outputName = inputFile.getName() + "-preprocessed";
				}
				outputName = outputName + ".bam";
				indexOutputName = outputName + ".bai";
				
				File outputFile = dataManager.createNewRepositoryFile(outputName);		
				File indexOutputFile = dataManager.createNewRepositoryFile(indexOutputName);

				// run sorter
				SamBamUtils.preprocessSamBam(inputFile, outputFile, indexOutputFile);

				// create outputs in the client
				DataBean outputBean = dataManager.createDataBean(outputName, outputFile);
				DataBean indexOutputBean = dataManager.createDataBean(indexOutputName, indexOutputFile);
				
				// create new operation instance, without any inputs FIXME parameters are lost, sucks
				outputBean.setOperation(new Operation(Session.getSession().getApplication().getOperationDefinition(task.getOperationID()), new DataBean[] {}));
				indexOutputBean.setOperation(new Operation(Session.getSession().getApplication().getOperationDefinition(task.getOperationID()), new DataBean[] {}));
				dataManager.getRootFolder().addChild(outputBean);
				dataManager.getRootFolder().addChild(indexOutputBean);
				
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
