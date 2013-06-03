package fi.csc.chipster.web.model;

import com.vaadin.ui.Label;

import fi.csc.microarray.description.SADLDescription;

public class Output extends InputOutputUI{

	private static final long serialVersionUID = 6280877684047822425L;
	
	@Override
	public Output createUI() {
//		grid.addComponent(new Label("Output"), 0, 0);
		initElements();
		createBasicUI();
		return this;
	}
	
	@Override
	protected void initElements() {
		super.initElements();
		lbId.setValue("Output file:");
		lbOptional = new Label("Output is:");
		lbType = new Label("Output is:");
		lbType2 = new Label("Type:");
	}
	
	public Output createUIWithData(SADLDescription.Output output) {
		createUI();
		fillWithData(output);
		return this;
	}
	
	private void fillWithData(SADLDescription.Output output) {
		name.setValue(output.getName().getDisplayName());
		cbMeta.setValue(output.isMeta());
		description.setValue(getValue(output.getComment()));
		if (output.isOptional()) {
			optional.select(OPTIONAL);
		} else {
			optional.select(NOT_OPTIONAL);
		}
		if(output.getName().getPrefix() == null || output.getName().getPrefix().isEmpty()) {
			type.select(SINGLE_FILE);
			id.setValue(output.getName().getID());
			getSingleFileUI();
		} else {
			type.select(MULTI_FILE);
			prefix.setValue(output.getName().getPrefix());
			postfix.setValue(output.getName().getPostfix());
			getMultipleFilesUI();
		}
	}
	
	public SADLDescription.Output getSadlOutput() {
		SADLDescription.Output output = new SADLDescription.Output();
		output.setName(getNameFromUI(type.getValue().toString()));
		output.setOptional(optional.getValue().equals(OPTIONAL) ? true : false);
		output.setComment(description.getValue());
		output.setMeta(cbMeta.getValue());
		
		return output;
	}

}
