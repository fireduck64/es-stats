package duckutil.lert;

public abstract class LertAgent
{
  public abstract LertState getCurrentState();

  
  public abstract long getReAlertTimeMs();

  public abstract String getAlertTopicArn();


}
