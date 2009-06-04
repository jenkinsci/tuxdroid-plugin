package hudson.plugins.tuxdroid;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.rmi.ConnectException;
import java.util.logging.Logger;

import com.tuxisalive.api.TuxAPI;
import com.tuxisalive.api.TuxAPIConst;

public class TuxDroid {
	
	private boolean connected = false;
	private TuxAPI tux = null;
	private static TuxDroid instance = null;
	private static Logger log = null;
	URL tuxUrl = null;
	
	TuxDroid(){
		
	}

	
	public static TuxDroid getInstance(){
		if (instance == null){
			instance = new TuxDroid();;
		}
		return instance;
	}
	
	public boolean isConnected(){
		return connected;
	}
	
	public static void setLog(Logger logger){
		log = logger;
	}
	
	public static void onAllEvent(String name, String value, Double delay)
	{
		
	}
	
	public static void onSoundEvent( String value, Double delay)
	{
		
	}
	
	public void connect(String tuxDroidUrl) throws Exception{
		if (isConnected())
			return;
					this.tuxUrl = new URL(tuxDroidUrl);
					tux = new TuxAPI(tuxUrl.getHost(), tuxUrl.getPort());					
				if (!tux.server.getConnected().booleanValue()){
					tux.server.autoConnect(TuxAPIConst.CLIENT_LEVEL_RESTRICTED, "hudsonPublisher", "0000");
					tux.server.waitConnected(Double.valueOf("0.5"));
					tux.event.handler.register(TuxAPIConst.ST_NAME_TTS_SOUND_STATE, this, "onSoundEvent");		
					connected = true;
				}
				if (! tux.server.getConnected()){
					throw new Exception("Unable to Connect");
				}
	}
	
	public void disconnect() throws MalformedURLException{
		if (!isConnected())
			return;
		if (tux != null){
			tux.server.disconnect();
			connected = false;
		
		}
	}
	
	public TuxAPI getTuxAPI() throws ConnectException, URISyntaxException{
		if (isConnected())
			return tux;
		throw new ConnectException(tuxUrl.toURI().toString());
			
	}
}