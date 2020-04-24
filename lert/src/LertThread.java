package duckutil.lert;

import duckutil.PeriodicThread;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

public class LertThread extends PeriodicThread
{
  private LertAgent agent;
  private Lert lert;

  private LertState last_state;
  private long last_state_notify;

  private boolean first_pass_done;

  public LertThread(LertAgent agent)
  {
    super(agent.getConfig().checkIntervalMs());
    setName("Thread:" + agent.getID());

    this.agent = agent;

  }

  public void runPass() throws Exception
  {
    if (!first_pass_done)
    {
      first_pass_done=true;
      loadLastState();
    }
    LertState state = agent.getCurrentState();
    System.out.println(agent.getID() + " - " + agent.getCurrentState());    

    if (!state.equals(last_state))
    {
      notifyState(state);   
      last_state = state;
    }

    if (!state.equals(LertState.OK))
    {
      if (System.currentTimeMillis() - last_state_notify > agent.getConfig().getReAlertTimeMs())
      {
        notifyState(state);
      }
    }

  }
  private void notifyState(LertState state)
    throws Exception
  {
    // do SNS notification
    StringBuilder sb=new StringBuilder();
    sb.append("Agent: " + agent.getID());
    sb.append("\n");
    sb.append("New State: " + state);
    sb.append("\n");


    agent.getLert().publish( agent.getConfig().getAlertTopicArn(),agent.getID() +":" + state,sb.toString());
    


  
    agent.getLert().saveDoc("lert_status", ImmutableMap.of("agent",agent.getID(),"state", state.toString()));
    last_state_notify = System.currentTimeMillis();
  }
  private void loadLastState()
    throws Exception
  {
    Map<String, Object> last_doc = agent.getLert().getTopDoc( agent.getLert().getLatest("lert_status", 1,ImmutableMap.of("agent",agent.getID() )));

    if (last_doc != null)
    {
      last_state_notify = (long) last_doc.get("time_ms");
      last_state = LertState.valueOf( (String) last_doc.get("state") );
    }

  }


}
