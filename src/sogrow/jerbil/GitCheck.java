package sogrow.jerbil;

import java.io.File;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.winterwell.bob.tasks.GitTask;
import com.winterwell.utils.io.FileUtils;

import com.winterwell.bob.tasks.GitTask;

import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;

/**
 * Regularly pull from git. Assumes that a separate file-watcher will handle updates.
 * @author daniel
 *
 */
public class GitCheck extends TimerTask {

	private File dir;
	private Timer timer;
	private Dt dt;

	public GitCheck(File projectdir, Dt dt) {
		this.dir = projectdir;
		this.dt = dt;
	}

	public void start() {
		timer = new Timer();
		timer.schedule(this, 10, dt.getMillisecs());
	}

	@Override
	public void run() {
		// are we in a git directory??
		if ( ! isGitDir()) {
			Log.e("gitcheck", dir+" is not a git managed directory. Stopping gitcheck.");
			timer.cancel();
			return;
		}
		try {			
			GitTask pull = new GitTask(GitTask.PULL, dir);
			pull.run();
			String out = pull.getOutput();
			pull.close();
			
			Log.d("gitcheck", out);
		} catch(Throwable ex) {
			Log.escalate(ex);
			// stop??
		}
	}
	
	public static void main(String[] args) {
		GitCheck gc = new GitCheck(FileUtils.getWorkingDirectory(), new Dt(20, TUnit.SECOND));
		gc.start();
	}

	public boolean isGitDir() {
		try {
			Map<String, Object> info = GitTask.getLastCommitInfo(dir);
			System.out.println(info);	
			return true;
		} catch(IllegalArgumentException ex) {
			return false;
		}
	}

}
