package software.theear.ui;

import java.io.Serializable;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.theear.service.auth.AuthenticatedSession;
import software.theear.service.auth.AuthorizationService;
import software.theear.ui.auth.FunctionalPermissionGroupOverviewPanel;

public class HomePage extends WebPage {
  private static final long serialVersionUID = 8919240523233153885L;
  /** Class logger */
  private final static Logger log = LoggerFactory.getLogger(AuthorizationService.class);
  
  /** Counter for the executions of the background timer on this page. */
  private final AtomicLong m_WicketJobExecutorRun = new AtomicLong(Long.MIN_VALUE);
  
  /** List of all jobs scheduled for AJAX timer execution. */
  private final LinkedList<WicketTimedJob> m_TimerJobs = new LinkedList<>();
  
  /** Schedule given job for execution in AJAX timer.
   * 
   * @param Job The job to schedule.
   */
  public void schedule(WicketTimedJob Job) { synchronized (this.m_TimerJobs) { this.m_TimerJobs.add(Job); } }
  
  /** Remove given job from execution in AJAX timer.
   * 
   * @param Job The job to remove.
   */
  public void unschedule(WicketTimedJob Job) { synchronized (this.m_TimerJobs) { this.m_TimerJobs.remove(Job); } }
  
  
  // FIXME: javadoc
  public interface BackgroundJob<T extends Serializable> extends Serializable {
    public T call(Object... Args);
  };
  // FIXME: javadoc
  public final class BackgroundJobFuture<T extends Serializable> implements Serializable, Runnable {
    // FIXME: add wait/notify and isDone(long)
    private static final long serialVersionUID = 1658141277857424462L;
    private boolean m_IsDone = false;
    private final BackgroundJob<T> m_Callable;
    private T m_Result = null;
    private final UUID m_JobReference;
    private final Object[] m_Args;
    
    BackgroundJobFuture(BackgroundJob<T> Callable, UUID JobReference, Object... Args) {
      this.m_Callable = Callable;
      this.m_JobReference = JobReference;
      this.m_Args = Args;
    }
    
    @Override public synchronized final void run() {
      if (!this.m_IsDone) {
        log.debug("Execute scheduled callable at HomePage. Reference key {}", m_JobReference);
        try {
          this.m_Result = this.m_Callable.call(this.m_Args);
          this.m_IsDone = true;
        } catch (Throwable T) { log.debug("Exception while executing scheduled callable at HomePage. Reference key " + m_JobReference.toString() + ". See exception for details.", T); }
      }
    }
    
    public final T getResult() { return this.m_Result; }
    public final boolean isDone() { return this.m_IsDone; }
  }
  
  
  
  /** Schedules the given background job to be executed in this page's context.
   * 
   * @param <T> The type of return value provided by the job.
   * @param C The job implementation.
   * @param Args Arguments passed to the job implementation.
   * @return A future on the job which can be observed.
   */
  public <T extends Serializable> BackgroundJobFuture<T> runInBackground(BackgroundJob<T> C, Object... Args) {
    final UUID reference = UUID.randomUUID();
    log.debug("Schedule new callable to run in background at HomePage. Reference key {}", reference);
    BackgroundJobFuture<T> task = new BackgroundJobFuture<>(C, reference, Args);
    new Thread(task, "HomePage background task - " + reference.toString()).start();
    return task;
  }
  
  public HomePage(final PageParameters Parameters) {
    super(Parameters);
    
    final UUID sessionID =  (Session.get() instanceof AuthenticatedSession as) ? as.getUser().UserID : new UUID(0, 0);
    
    // Add component that can run an AJAX timer
    Label timer = new Label("timer");
    timer.add(new AbstractAjaxTimerBehavior(Duration.ofMillis(5000)) { // FIXME: make the "1000" configurable; lower values for more responsiveness, high value for less load
      private static final long serialVersionUID = 5388636980107585897L;
      @Override protected void onTimer(AjaxRequestTarget target) {
        log.trace("Running AJAX timer {}, cycle #{}", sessionID, m_WicketJobExecutorRun.getAndIncrement());
        Collection<WicketTimedJob> jobs;
        synchronized (m_TimerJobs) { jobs = new LinkedList<>(m_TimerJobs); }
        jobs.forEach(c -> {
          try { c.execute(target, m_WicketJobExecutorRun.get()); }
          catch (Throwable T) { log.debug("Failed to execute AJAX job. See exception for details.", T); }
        });
        log.trace("Done AJAX timer {}, #{} run cycle", sessionID, m_WicketJobExecutorRun.getAndIncrement());
      }
    });
    add(timer);
    
    add(new Header("header"));
//    add(new Label("main", this.getApplication().getFrameworkSettings().getVersion()));
    add(new FunctionalPermissionGroupOverviewPanel("main"));
    add(new Footer("footer"));
  }
}
