package duckutil.lert;

import com.google.common.collect.ImmutableMap;
import duckutil.Pair;
import duckutil.PeriodicThread;
import java.util.Map;
import java.util.Random;

public class LertThread extends PeriodicThread
{
  private LertAgent agent;

  private LertState last_state;
  private long last_state_notify;

  private boolean first_pass_done;
  private long last_state_save;

  public LertThread(LertAgent agent)
  {
    super(agent.getConfig().checkIntervalMs(), 50.0);
    setName("Thread:" + agent.getID());

    this.agent = agent;

  }

  public void runPass() throws Exception
  {
    if (!first_pass_done)
    {
      Random rnd = new Random();
      sleep(rnd.nextInt(2000));
      first_pass_done=true;
      loadLastState();
    }

    Pair<LertState, String> p = agent.getCurrentState();
    LertState state = p.getA();
    String msg = p.getB();
    agent.getLert().getReg().save(agent.getConfig(), state, msg);
    System.out.println(agent.getID() + " - " + p);    

    if (!state.equals(last_state))
    {
      notifyState(state, p.getB());   
      last_state = state;
    }
    if (last_state_save + 86400000L < System.currentTimeMillis())
    {
      persistState(state);

    }

    if (!state.equals(LertState.OK))
    {
      if (System.currentTimeMillis() - last_state_notify > agent.getConfig().getReAlertTimeMs())
      {
        notifyState(state, p.getB());
      }
    }

  }
  private void notifyState(LertState state, String msg)
    throws Exception
  {
    // do SNS notification
    StringBuilder sb=new StringBuilder();
    sb.append("Agent: " + agent.getID());
    sb.append("\n");
    sb.append("New State: " + state);
    sb.append("\n");
    sb.append("Message: " + msg);
    sb.append("\n");


    agent.getLert().publish( agent.getConfig().getAlertTopicArn(),agent.getID() +":" + state,sb.toString());
  
    persistState(state);
    last_state_notify = System.currentTimeMillis();
  }
  private void persistState(LertState state)
    throws Exception
  {
    //System.out.println("Persisting state: " + agent.getID() + " " + state.toString());
    agent.getLert().saveDoc("lert_status", ImmutableMap.of("agent",agent.getID(),"state", state.toString()));
    last_state_save=System.currentTimeMillis();

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
    else
    {
      System.out.println("No last state for: " + agent.getID());
      last_state = LertState.OK;
    }

  }


}
