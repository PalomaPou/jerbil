package sogrow.jerbil;

import java.io.File;

import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.io.Option;

public class JerbilConfig {
	
	@Option(description="If true (the default) then Jerbil will run a simple web server. You can also use nginx or apache (or anything else) instead though.")
	public boolean server = true;

	@Option(description="The port to connect to. If you wish to use Jerbil as your primary server, then set this to 80. The standard setup is to use e.g. nginx instead.")
	public int port = 8282;
	
	public File projectdir;	
	
}
