package hudson.plugins.tuxdroid;

import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.tasks.Publisher;
import hudson.util.FormFieldValidator;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.rmi.ConnectException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.tuxisalive.api.TuxAPI;
import com.tuxisalive.api.TuxAPIConst;

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
	private final String tuxDroidUnstableTTS;
	private TuxAPI tux = null;

	public void connect(){
		try {
			TuxDroid.getInstance().connect(this.getDescriptor().tuxDroidUrl);
		} catch (Exception e) {
			this.log.info(e.getMessage());
		}
	}
	
	public void disconnect(){
		try {
			TuxDroid.getInstance().disconnect();
		} catch (Exception e) {
			this.log.info(e.getMessage());
		}
			
	}
	
	@DataBoundConstructor
	public TuxDroidPublisher(String reportOnSucess, String animatePenguin, String tuxDroidVoice,
			String tuxDroidMacro, String tuxDroidSuccessTTS,
			String tuxDroidRecoverTTS, String tuxDroidFailTTS, String tuxDroidUnstableTTS) {
		connect();
		this.reportOnSucess = reportOnSucess;
		this.animatePenguin = animatePenguin;
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
		this.tuxDroidUnstableTTS = tuxDroidUnstableTTS;
		
	}

	private List<String> initVoices()  {
		List<String> voices = null;
		try {
			voices = TuxDroid.getInstance().getTuxAPI().tts.getVoices();
		} catch (ConnectException e) {
			log.info(e.getMessage());
		} catch (URISyntaxException e) {
			log.info(e.getMessage());
		} catch( NullPointerException e){
			log.info(e.getMessage());
		}
			return voices;
	}

	public List<String> getVoices() {
		connect();
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
	
	public String getTuxDroidUnstableTTS() {
		return tuxDroidUnstableTTS;
	}

	public String reportOnSucess = "false";
	public String animatePenguin = "false";

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
	
	public boolean isAnimatePenguin() {
		if (animatePenguin == null)
			return false;
		if (animatePenguin.equalsIgnoreCase("ON"))
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
	private void say(final String message, BuildListener listener) {		
		try {
			if (isAnimatePenguin())
				TuxDroid.getInstance().getTuxAPI().mouth.onAsync(20, TuxAPIConst.SSV_CLOSE);
			TuxDroid.getInstance().getTuxAPI().tts.speak(message);
			if (isAnimatePenguin())
				TuxDroid.getInstance().getTuxAPI().mouth.off();
		} catch (ConnectException e) {
			log.info(e.getMessage());
		} catch (URISyntaxException e) {
			log.info(e.getMessage());
		} catch( NullPointerException e){
			log.info(e.getMessage());
		}
	}

	private void moveFlipper(BuildListener listener) {
		try {
			if (isAnimatePenguin())
				TuxDroid.getInstance().getTuxAPI().flippers.onDuring(Double.valueOf(1.0), TuxAPIConst.SSV_DOWN);
			//TuxDroid.getInstance().getTuxAPI().flippers.waitMovingOff(Double.valueOf("3.0"));
		} catch (ConnectException e) {
			log.info(e.getMessage());
		} catch (URISyntaxException e) {
			log.info(e.getMessage());
		} catch( NullPointerException e){
			log.info(e.getMessage());
		}
		
	}

	private void changeVoice(BuildListener listener) {
		try {
			TuxDroid.getInstance().getTuxAPI().tts.setLocutor(getTuxDroidVoice());
		} catch (ConnectException e) {
			log.info(e.getMessage());
		} catch (URISyntaxException e) {
			log.info(e.getMessage());
		} catch( NullPointerException e){
			log.info(e.getMessage());
		}
	}

	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Override
	public boolean perform(final AbstractBuild<?, ?> build,
			final Launcher launcher, final BuildListener listener)
			throws InterruptedException, IOException {
		
		String msg;		
		connect();
		// Build FAILURE
		if ((build.getResult() == Result.FAILURE)) {
			msg = getTuxDroidFailTTS();
			listener
			.getLogger()
			.println("TuxDroid Build will say FAILURE message");
			animate(msg, build, listener);
		}else if (build.getResult() == Result.UNSTABLE) {

			// Build RECOVERY
			
				msg = getTuxDroidUnstableTTS();
				listener
				.getLogger()
				.println("TuxDroid Build will say UNSTABLE message");
				animate(msg, build, listener);
			
		}else if (build.getResult() == Result.SUCCESS) {

			// Build RECOVERY
			if (build.getPreviousBuild() != null
					&& build.getPreviousBuild().getResult() == Result.FAILURE) {
				msg = getTuxDroidRecoverTTS();
				listener
				.getLogger()
				.println("TuxDroid Build will say RECOVERY message");
				animate(msg, build, listener);
			}

			// Build SUCCESS
			else if (isReportOnSucess()) {
				msg = getTuxDroidSuccessTTS();
				listener
				.getLogger()
				.println("TuxDroid Build will say SUCCESS message");
				animate(msg, build, listener);
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

	/**
	 * @param message
	 * @param earpos
	 * @param build
	 * @param listener
	 * @param build
	 */
	private void animate(final String message, AbstractBuild<?, ?> build,
			BuildListener listener) {
		
		String substituedMessage = StringUtils.replaceEachRepeatedly(message.toUpperCase(),
				new String[] { "${PROJECTNAME}", "${BUILDNUMBER}", "${BUILDSTATE}"},
				new String[] { build.getProject().getName(),
						String.valueOf(build.getNumber()),  build.getResult().toString()}
		);
		for (Iterator keyIterator = build.getBuildVariables().keySet().iterator(); keyIterator.hasNext();) {
			String key = (String) keyIterator.next();
			substituedMessage = StringUtils.replaceEachRepeatedly(substituedMessage.toUpperCase(), new String[]{"${"+key.toUpperCase()+"}"},new String[]{build.getBuildVariableResolver().resolve(key)});
		}
		String urlEncodedMessage = null;
		
		changeVoice(listener);
	    moveFlipper(listener);
	    try {
			urlEncodedMessage = URLEncoder.encode(substituedMessage, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			listener.getLogger().println("Unsupported Encoding ");
			listener.getLogger().println("Tux Droid has not been notified ");
			return;
		}
	    say(urlEncodedMessage, listener);
		listener.getLogger().println(substituedMessage);
		listener.getLogger().println("Tux Droid has been successfully notified ");
		///////////////////
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
		public String tuxDroidUnstableTTS;
		public String reportOnSucess;
		public String animatePenguin;

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
		
		public void doUrlCheck(final StaplerRequest req, StaplerResponse rsp)
        throws IOException, ServletException {
        new FormFieldValidator(req, rsp, false) {

            /**
             * {@inheritDoc}
             * 
             * @throws IOException {@inheritDoc}
             * @throws ServletException {@inheritDoc}
             * @see hudson.util.FormFieldValidator#check()
             */
         @Override
          protected void check()
                throws IOException, ServletException {
                String tuxDroidUrl = Util.fixEmpty(request.getParameter("tuxDroidUrl"));
                if (tuxDroidUrl == null) { // hosts is not entered yet
                    ok();
                    return;
                }
               
                try {                	
                    TuxDroid.getInstance().connect(tuxDroidUrl);
                    TuxDroid.getInstance().disconnect();
                    ok();
                } catch (Exception e) {
                    error(e.getMessage());
                }
            }
        } .process();
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
			this.animatePenguin = req.getParameter("animatePenguin");
			this.tuxDroidFailTTS = req.getParameter("tuxDroidFailTTS");
			this.tuxDroidMacro = req.getParameter("tuxDroidMacro");
			this.tuxDroidRecoverTTS = req.getParameter("tuxDroidRecoverTTS");
			this.tuxDroidSuccessTTS = req.getParameter("tuxDroidSuccessTTS");
			this.tuxDroidUnstableTTS = req.getParameter("tuxDroidUnstableTTS");
			this.tuxDroidVoice = req.getParameter("tuxDroidVoice");
			TuxDroidPublisher m = new TuxDroidPublisher(this.reportOnSucess, this.animatePenguin,
					this.tuxDroidVoice, this.tuxDroidMacro,
					this.tuxDroidSuccessTTS, this.tuxDroidRecoverTTS,
					this.tuxDroidFailTTS, this.tuxDroidUnstableTTS);
			return m;
		}

	}

}
