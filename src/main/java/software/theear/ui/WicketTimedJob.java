package software.theear.ui;

import java.io.Serializable;

import org.apache.wicket.ajax.AjaxRequestTarget;

/** AJAX-job to be executed in the background.
 * 
 * Via the target it shall be possible to manipulate frontend elements.
 * 
 * @author bjoern@liwuest.net
 */
public interface WicketTimedJob extends Serializable {
  /** The job implementation.
   * 
   * The implementation is executed every time the AJAX timer is triggered on the page.
   * 
   * @param Target The request target which may be used to communicate with the frontend.
   * @param RunNumber The actual run number, i.e. how often the AJAX timer has executed already.
   */
  public void execute(AjaxRequestTarget Target, long RunNumber);
}