package fi.csc.microarray.client;

import java.util.Collection;

import javax.ws.rs.client.WebTarget;

import fi.csc.chipster.auth.AuthenticationClient;
import fi.csc.chipster.filebroker.RestFileBrokerClient;
import fi.csc.chipster.sessiondb.SessionDbClient;
import fi.csc.microarray.client.operation.ToolModule;
import fi.csc.microarray.client.tasks.TaskExecutor;
import fi.csc.microarray.databeans.DataManager;
import fi.csc.microarray.filebroker.FileBrokerClient;
import fi.csc.microarray.messaging.DescriptionMessageListener;
import fi.csc.microarray.messaging.JMSMessagingEndpoint;
import fi.csc.microarray.messaging.MessagingEndpoint;
import fi.csc.microarray.messaging.MessagingTopic;
import fi.csc.microarray.messaging.MessagingTopic.AccessMode;
import fi.csc.microarray.messaging.NodeBase;
import fi.csc.microarray.messaging.SourceMessageListener;
import fi.csc.microarray.messaging.Topics;
import fi.csc.microarray.messaging.admin.AdminAPI;
import fi.csc.microarray.messaging.auth.AuthenticationRequestListener;
import fi.csc.microarray.messaging.message.CommandMessage;
import fi.csc.microarray.messaging.message.FeedbackMessage;
import fi.csc.microarray.module.Module;

public class RemoteServiceAccessor implements ServiceAccessor {

	protected MessagingEndpoint endpoint;
	protected MessagingTopic requestTopic;
	protected TaskExecutor taskExecutor;
	protected FileBrokerClient filebrokerClient;

	private NodeBase nodeSupport = new NodeBase() {
		public String getName() {
			return "client";
		}
	};
	private Collection<ToolModule> modules = null;
	private SessionDbClient sessionDbClient;
	private String restProxy;
	private AuthenticationClient authService;

	public RemoteServiceAccessor() {
	}
	
	public RemoteServiceAccessor(String restProxy) {
		this.restProxy = restProxy;		
	}


	public void initialise(DataManager manager, AuthenticationRequestListener authenticationRequestListener) throws Exception {								
		
		this.endpoint = new JMSMessagingEndpoint(nodeSupport, authenticationRequestListener, true);		
		this.initialise(endpoint, 
				manager, 
				new RestFileBrokerClient());
	}		

	
	/**
	 * Initialise RemoteServiceAccessor with custom messaging endpoint and custom file broker client. This is used when file broker uses client code to manage example sessions.
	 * 
	 * @param endpoint
	 * @param manager
	 * @param authenticationRequestListener
	 * @throws Exception
	 */
	public void initialise(MessagingEndpoint endpoint, DataManager manager, FileBrokerClient fileBrokerClient) throws Exception {
		this.endpoint = endpoint;
	    this.requestTopic = endpoint.createTopic(Topics.Name.REQUEST_TOPIC,AccessMode.WRITE);	    
		this.filebrokerClient = fileBrokerClient;
	    this.taskExecutor = new TaskExecutor(endpoint, manager);	    
	}

	public TaskExecutor getTaskExecutor() {
		if (taskExecutor == null) {
			throw new IllegalStateException("initialise(...) must be called first");
		}
		return taskExecutor;
	}
	
	@Override
	public String checkRemoteServices() throws Exception {
		AdminAPI api = new AdminAPI(endpoint.createTopic(Topics.Name.ADMIN_TOPIC, AccessMode.READ_WRITE), null);
		if (api.areAllServicesUp(true)) {
			return ALL_SERVICES_OK;
		} else {
			return "required services are not available (" + api.getErrorStatus() + ")";
		}				
	}

	@Override
	public String fetchDescriptions(Module module) throws Exception {
		DescriptionMessageListener descriptionListener = new DescriptionMessageListener(module.getServerModuleNames());
		this.requestTopic.sendReplyableMessage(new CommandMessage(CommandMessage.COMMAND_DESCRIBE), descriptionListener);
		descriptionListener.waitForResponse();
		this.modules = descriptionListener.getModules();
		descriptionListener.cleanUp();
		
		return descriptionListener.getParseErrors();
	}


	@Override
	public Collection<ToolModule> getModules() {
		if (modules == null) {
			throw new IllegalStateException("fetchDescriptions(...) must be called first");
		}
		return modules;
	}
	
	@Override
	public void close() throws Exception {
		endpoint.close();
	}

	@Override
	public SourceMessageListener retrieveSourceCode(String id) throws Exception {
		SourceMessageListener sourceListener = new SourceMessageListener();
		CommandMessage commandMessage = new CommandMessage(CommandMessage.COMMAND_GET_SOURCE);
		commandMessage.addParameter(id);
		this.requestTopic.sendReplyableMessage(commandMessage, sourceListener);
		return sourceListener;
	}

	@Override
	public FileBrokerClient getFileBrokerClient() {
        return filebrokerClient;
	}

	@Override
	public void sendFeedbackMessage(FeedbackMessage message) throws Exception {
        MessagingTopic requestTopic = endpoint.createTopic(Topics.Name.FEEDBACK_TOPIC, AccessMode.WRITE);
        requestTopic.sendMessage(message);
	}

	@Override
	public boolean isStandalone() {
		return false;
	}
	
	public AuthenticationClient getAuthClient() {
		if (authService == null) {
			this.authService = new AuthenticationClient("http://" + restProxy + "/auth/", "client", "clientPassword");
		}
		return authService;
	}


	public SessionDbClient getSessionDbClient() {
		if (sessionDbClient == null) {			
			sessionDbClient = new SessionDbClient("http://" + restProxy + "/sessiondb/", "http://" + restProxy + "/sessiondbevents/",  getAuthClient().getCredentials());
		}
		return sessionDbClient;
	}


	public WebTarget getRestFileBrokerClient() {
		// impelent a proper client API for the file-broker
		return getAuthClient().getAuthenticatedClient().target("http://" + restProxy + "/filebroker/");
	}

}
