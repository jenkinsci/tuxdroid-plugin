package hudson.plugins.tuxdroid;

import hudson.Extension;
import hudson.Launcher;
import hudson.ProxyConfiguration;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.tasks.Publisher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * @author jean marc taillant
 */
public class TuxDroidPublisher extends Publisher {

	private static Object lock = new Object();
	private List<String> voices;
	private final String tuxDroidVoice;
	private final String tuxDroidMacro;
	private final String tuxDroidSuccessTTS;
	private final String tuxDroidRecoverTTS;
	private final String tuxDroidFailTTS;

	@DataBoundConstructor
	public TuxDroidPublisher(String reportOnSucess, String tuxDroidVoice,
			String tuxDroidMacro, String tuxDroidSuccessTTS,
			String tuxDroidRecoverTTS, String tuxDroidFailTTS) {
		// super();

		this.reportOnSucess = reportOnSucess;
		this.tuxDroidVoice = tuxDroidVoice;
		try {
			this.voices = this.initVoices();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.tuxDroidMacro = tuxDroidMacro;
		this.tuxDroidSuccessTTS = tuxDroidSuccessTTS;
		this.tuxDroidRecoverTTS = tuxDroidRecoverTTS;
		this.tuxDroidFailTTS = tuxDroidFailTTS;
	}

	private List<String> initVoices() throws Exception {

		List<String> list = null;

		InputStream inputStream = null;
		BufferedReader bufferedReader = null;
		final StringBuffer buf = new StringBuffer();
		buf.append(getDescriptor().getTuxDroidUrl());
		buf.append("/");
		buf.append(getDescriptor().getTuxDroidId());
		buf.append("/tts/voices?");
		String listVoicesRequest = buf.toString();
		URLConnection cnx = ProxyConfiguration.open(new URL(listVoicesRequest));
		cnx.connect();

		inputStream = cnx.getInputStream();
		bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
		StringBuilder result = new StringBuilder();
		String strLine = null;
		while ((strLine = bufferedReader.readLine()) != null) {
			result.append(strLine);
		}
		log.finest("API call result : " + result.toString());
		return analyseVoices(result.toString());
	}

	private List<String> analyseVoices(String string) {
		// TODO Auto-generated method stub
		List<String> list = new ArrayList<String>();
		XMLStreamReader xmlStreamReader;
		try {
			xmlStreamReader = XMLInputFactory.newInstance()
					.createXMLStreamReader(new StringReader(string));
			while (xmlStreamReader.hasNext()) {
				int next = xmlStreamReader.next();
				if (next == XMLStreamConstants.START_ELEMENT) {
					String currentElement = xmlStreamReader.getName()
							.getLocalPart();
					if (currentElement.equals("locutor")) {
						String elementText = xmlStreamReader.getElementText();
						list.add(elementText);
					}
				}
			}
		} catch (XMLStreamException e) {
			log.log(Level.WARNING, "Unable to read xml Voices result.", e);
		} catch (FactoryConfigurationError e) {
			log
					.log(
							Level.WARNING,
							"Unable to create xml parser to read xml Voices result.",
							e);
		}
		return list;
	}

	public List<String> getVoices() {
		try {
			this.voices = initVoices();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			return null;
		}
		return voices;
	}

	public String getTuxDroidSuccessTTS() {
		return tuxDroidSuccessTTS;
	}

	public String getTuxDroidRecoverTTS() {
		return tuxDroidRecoverTTS;
	}

	public String getTuxDroidFailTTS() {
		return tuxDroidFailTTS;
	}

	public String reportOnSucess = "false";

	public String getTuxDroidVoice() {
		return tuxDroidVoice;
	}

	public String getTuxDroidMacro() {
		return tuxDroidMacro;
	}

	public boolean isReportOnSucess() {
		if (reportOnSucess.equalsIgnoreCase("ON"))
			return true;
		return false;
	}

	/** the Logger */
	private static java.util.logging.Logger log = java.util.logging.Logger
			.getLogger(TuxDroidPublisher.class.getName());

	/**
	 * @param message
	 * @param earpos
	 * @return
	 */
	private String buildRequest(final String message) {
		final StringBuffer buf = new StringBuffer();
		buf.append(getDescriptor().getTuxDroidUrl());
		buf.append("/");
		buf.append(getDescriptor().getTuxDroidId());
		buf.append("/tts/speak?");
		// http://www.frbred0f07589.com:270/0/tts/speak?text=Ouille!Ouille!!
		buf.append("text=");
		buf.append(message);
		return buf.toString();
	}

	private String moveFlipper() {
		final StringBuffer buf = new StringBuffer();
		buf.append(getDescriptor().getTuxDroidUrl());
		buf.append("/");
		buf.append(getDescriptor().getTuxDroidId());
		buf.append("/");
		// http://www.frbred0f07589.com:270/0/tts/locutor?name=Heather8k
		buf.append("flippers/on_during?");
		buf.append("duration=1.0&final_state=DOWN");
		return buf.toString();
	}

	private String changeVoice() {
		final StringBuffer buf = new StringBuffer();
		buf.append(getDescriptor().getTuxDroidUrl());
		buf.append("/");
		buf.append(getDescriptor().getTuxDroidId());
		buf.append("/");
		// http://www.frbred0f07589.com:270/0/tts/locutor?name=Heather8k
		buf.append("tts/locutor?");
		buf.append("name=" + getTuxDroidVoice());
		return buf.toString();
	}

	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Override
	public boolean perform(final AbstractBuild<?, ?> build,
			final Launcher launcher, final BuildListener listener)
			throws InterruptedException, IOException {

		String msg;

		// Build FAILURE
		if ((build.getResult() == Result.FAILURE)
				|| (build.getResult() == Result.UNSTABLE)) {
			msg = getTuxDroidFailTTS();
			log.finest("TuxDroid Build FAILURE");
			sendRequest(msg, build, listener);
		} else if (build.getResult() == Result.SUCCESS) {

			// Build RECOVERY
			if (build.getPreviousBuild() != null
					&& build.getPreviousBuild().getResult() == Result.FAILURE) {
				msg = getTuxDroidRecoverTTS();
				log.finest("TuxDroid Build RECOVERY");
				sendRequest(msg, build, listener);
			}

			// Build SUCCESS
			else if (isReportOnSucess()) {
				msg = getTuxDroidSuccessTTS();
				log.finest("TuxDroid Build SUCCESS");
				sendRequest(msg, build, listener);
			} else {
				listener
						.getLogger()
						.println(
								"User has choosed not to be notified of success, notification has not been sent.");
			}
		} else {
			listener
					.getLogger()
					.println(
							"Build result not handled by TuxDroid notifier, notification has not been sent.");
		}

		return true;
	}
	
	private void waitTime(int milliseconds){
		synchronized (lock) {
			try {

				lock.wait(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param message
	 * @param earpos
	 * @param build
	 * @param listener
	 * @param build
	 */
	private void sendRequest(final String message, AbstractBuild<?, ?> build,
			BuildListener listener) {
		String substituedMessage = StringUtils.replaceEach(message,
				new String[] { "${projectName}", "${buildNumber}" },
				new String[] { build.getProject().getName(),
						String.valueOf(build.getNumber()) }

		);

		String urlEncodedMessage = null;

		InputStream inputStream = null;
		BufferedReader bufferedReader = null;
		try {
			// setting TuxDroid locutor
			URLConnection cnx = null;
			String changeVoiceRequest = changeVoice();
			log.finest(" sending Tux Droid request : " + changeVoiceRequest);
			cnx = ProxyConfiguration.open(new URL(changeVoiceRequest));
			cnx.connect();
			inputStream = cnx.getInputStream();
			String moveFlipperRequest = moveFlipper();
			log.finest(" sending Tux Droid request : " + moveFlipperRequest);
			URLConnection cnx2 = ProxyConfiguration.open(new URL(
					moveFlipperRequest));
			// URLConnection cnx2 = ProxyConfiguration.open(new
			// URL("http://frbred0f07589:270"));
			cnx2.connect();
			inputStream = cnx2.getInputStream();
			waitTime(1000);
			bufferedReader = new BufferedReader(new InputStreamReader(
					inputStream));
			StringBuilder result = new StringBuilder();
			String strLine;
			while ((strLine = bufferedReader.readLine()) != null) {
				result.append(strLine);
			}
			log.finest("API call result : " + result.toString());

			urlEncodedMessage = URLEncoder.encode(substituedMessage, "UTF-8");
			String requestString = buildRequest(urlEncodedMessage);
			log.finest(" sending Tux Droid request : " + requestString);
			URLConnection cnx3 = ProxyConfiguration
					.open(new URL(requestString));
			cnx3.connect();

			inputStream = cnx3.getInputStream();
			bufferedReader = new BufferedReader(new InputStreamReader(
					inputStream));
			result = new StringBuilder();
			strLine = null;
			while ((strLine = bufferedReader.readLine()) != null) {
				result.append(strLine);
			}

			log.finest("API call result : " + result.toString());
			analyseResult(result.toString(), listener);
		} catch (UnsupportedEncodingException notFatal) {
			log.log(Level.WARNING, "URL is malformed.", notFatal);
			listener.error("Unable to url encode the Nabaztag message.");
		} catch (MalformedURLException dontCare) {
			log.log(Level.WARNING, "URL is malformed.", dontCare);
			listener.error("Unable to build a valid Nabaztag API call.");
		} catch (IOException notImportant) {
			log.log(Level.WARNING,
					"IOException while reading API call result.", notImportant);
			listener.error("TuxDroid has not been successfully notified.");
		} finally {
			if (bufferedReader != null)
				try {
					bufferedReader.close();
				} catch (IOException e) {
					log.log(Level.WARNING,
							"IOException while closing API connection.", e);
				}
			if (inputStream != null)
				try {
					inputStream.close();
				} catch (IOException e) {
					log.log(Level.WARNING,
							"IOException while closing API connection.", e);
				}
		}
	}

	private void analyseResult(String string, BuildListener listener) {
		List<String> expectedCommands = new ArrayList<String>(3);
		expectedCommands.add("Success");

		List<String> unExpectedCommands = new ArrayList<String>();

		XMLStreamReader xmlStreamReader;
		try {
			xmlStreamReader = XMLInputFactory.newInstance()
					.createXMLStreamReader(new StringReader(string));
			while (xmlStreamReader.hasNext()) {
				int next = xmlStreamReader.next();
				if (next == XMLStreamConstants.START_ELEMENT) {
					String currentElement = xmlStreamReader.getName()
							.getLocalPart();
					if (currentElement.equals("result")) {
						String elementText = xmlStreamReader.getElementText();
						if (expectedCommands.contains(elementText)) {
							expectedCommands.remove(elementText);
						} else {
							unExpectedCommands.add(elementText);
						}
					}
				}
			}
		} catch (XMLStreamException e) {
			log.log(Level.WARNING, "Unable to read xml result.", e);
		} catch (FactoryConfigurationError e) {
			log.log(Level.WARNING,
					"Unable to create xml parser to read xml result.", e);
		}

		boolean success = true;
		StringBuilder out = new StringBuilder();
		if (expectedCommands.size() > 0) {
			success = false;
			out
					.append("Following expected confirmations has not been received: ");
			out.append(expectedCommands.toString());
			out.append("\n");
		}
		if (unExpectedCommands.size() > 0) {
			success = false;
			out.append("Following unexpected messages has been received: ");
			out.append(unExpectedCommands.toString());
			out.append(". ");
		}
		if (success) {
			listener.getLogger().println(
					"Tux Droid has been successfully notified.");
		} else {
			listener.getLogger().println(
					"Tux Droid has not been successfully notified: ");
			listener.getLogger().println(out.toString());
		}
	}

	@Extension
	public static final class DescriptorImpl extends Descriptor<Publisher> {

		public String tuxDroidUrl = "http://127.0.0.1:270";
		public String tuxDroidId = "0";
		public String tuxDroidVoice;
		public String tuxDroidSuccessTTS;
		public String tuxDroidRecoverTTS;
		public String tuxDroidMacro;
		public String tuxDroidFailTTS;
		public String reportOnSucess;

		public String getTuxDroidId() {
			return tuxDroidId;
		}

		public String getTuxDroidUrl() {
			return tuxDroidUrl;
		}

		public void setTuxDroidUrl(String tuxDroidUrl) {
			this.tuxDroidUrl = tuxDroidUrl;
		}

		public DescriptorImpl() {
			load();
		}

		@Override
		public boolean configure(final StaplerRequest req, JSONObject json)
				throws FormException {

			tuxDroidUrl = req.getParameter("tuxDroidUrl");
			save();
			return true;
			// return super.configure(req, json);
		}

		@Override
		public String getDisplayName() {
			return "TuxDroid Publisher";
		}

		@Override
		public TuxDroidPublisher newInstance(StaplerRequest req)
				throws hudson.model.Descriptor.FormException {

			// Save configuration for each trigger type
			this.reportOnSucess = req.getParameter("reportOnSucess");
			;
			this.tuxDroidFailTTS = req.getParameter("tuxDroidFailTTS");
			this.tuxDroidMacro = req.getParameter("tuxDroidMacro");
			this.tuxDroidRecoverTTS = req.getParameter("tuxDroidRecoverTTS");
			this.tuxDroidSuccessTTS = req.getParameter("tuxDroidSuccessTTS");
			this.tuxDroidVoice = req.getParameter("tuxDroidVoice");
			TuxDroidPublisher m = new TuxDroidPublisher(this.reportOnSucess,
					this.tuxDroidVoice, this.tuxDroidMacro,
					this.tuxDroidSuccessTTS, this.tuxDroidRecoverTTS,
					this.tuxDroidFailTTS);
			return m;
		}

	}

}
