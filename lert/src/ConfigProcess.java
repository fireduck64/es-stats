package duckutil.lert;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

public class ConfigProcess extends LertConfig
{
  protected ImmutableMap<String, String> filter_terms;
  protected String id;

  public ConfigProcess(String process)
  {
    filter_terms = ImmutableMap.of("process", process);
    id = "proc." + process;
  }

  @Override
  public String getID(){ return id; }

  @Override
  public long getReAlertTimeMs(){ return 2L* 60L * 60L * 1000L;} // 2 hr
  @Override
  public long checkIntervalMs() {return 2L*60L*1000L; } //2 min 
  @Override
  public long maxLookbackMs() {return 15L*60L*1000L; } //15 min

  @Override
  public String getIndexBase(){return "process-report";}

  @Override
  public Map<String,String> getFilterTerms(){return filter_terms;}

  @Override
  public String getValuePath(){return "success";}

  @Override
  public Double getLowVal(){return 0.98;}

  @Override
  public Double getHighVal(){return 1.02;}

}
