package duckutil.lert;

import java.util.TreeMap;
import java.util.Map;
import duckutil.Pair;
import com.google.common.collect.ImmutableMap;

public class StatusRegistry
{
  private TreeMap<String, Pair<LertState, String> > status_map = new TreeMap<>();

  public synchronized void save(LertConfig config, LertState state, String msg)
  {
    status_map.put(config.getID(), new Pair(state, msg));
  }

  public synchronized Map<String, Pair<LertState, String> > getMap()
  {
    return ImmutableMap.copyOf(status_map);
    
  }
}
