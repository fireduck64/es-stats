import java.util.LinkedList;
import java.util.List;


public class TimeSeries
{
  private LinkedList<Long> values;
  private int keep_back;
  private long current_sum;

  public TimeSeries(int keep_back)
  {
    values = new LinkedList<>();
    this.keep_back = keep_back;
    current_sum = 0L;

  }

  public void addValue(long v)
  {
    values.add(v);
    current_sum += v;
    while(values.size() > keep_back)
    {
      long rem = values.pollFirst();
      current_sum -= rem;
    }
  }

  public double getAverage()
  {
    if (values.size() == 0)
    {
      return 0.0;
    }
    double n = values.size();
    double s = current_sum;
    return s/n;

  }
  public int getCount()
  {
    return values.size();
  }


}
