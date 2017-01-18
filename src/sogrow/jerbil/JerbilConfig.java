package sogrow.jerbil;

import java.io.File;

import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;

import com.winterwell.utils.io.FileUtils;

import com.winterwell.utils.io.Option;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;

public class JerbilConfig {
	
	@Override
	public String toString() {
		return "JerbilConfig[server=" + server + ", port=" + port + ", projectdir=" + projectdir + ", gitcheck="
				+ gitcheck + ", preview=" + preview + ", exit=" + exit + "]";
	}

	@Option(description="If true (the default) then Jerbil will run a simple web server. You can also use nginx or apache (or anything else) instead though.")
	public boolean server = true;

	@Option(description="The port to connect to. If you wish to use Jerbil as your primary server, then set this to 80. The standard setup is to use e.g. nginx instead.")
	public int port = 8282;
	
	public File projectdir;	
	
	@Option(description="If the site is in a git-managed directory, then regularly call git pull to keep it up to date. A no-config-required alternative to web-hooks.")
	public Dt gitcheck = new Dt(1, TUnit.MINUTE);
	
	@Option(description="If true, Jerbil will try to open a web browser for you.")
	public boolean preview = true;
	
	@Option(description="If true, Jerbil will exit after build -- this disables the server and git-check.")
	public boolean exit = false;
}
