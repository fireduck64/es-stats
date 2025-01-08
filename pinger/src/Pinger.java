import java.io.FileInputStream;
import java.util.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import net.minidev.json.JSONObject;
import duckutil.NetUtil;
import duckutil.PeriodicThread;
import java.net.InetAddress;
import java.text.DecimalFormat;

public class Pinger
{
  Random rnd=new Random();

  public static void main(String args[]) throws Exception
  {
    new Pinger();
  }

  public final String source_name;

	public Pinger()
    throws Exception
	{
    source_name = System.getenv("PINGER_SOURCE");
    if (source_name == null)
    {
      throw new RuntimeException("Must set PINGER_SOURCE and PINGER_TARGETS");
    }
    System.out.println("Starting pinger");

    List<String> targets = Tok.en(System.getenv("PINGER_TARGETS"), ",");
    
    for(String t : targets)
    {   
      new PingThread(t).start();
    }

  }

  public static long PING_INTERVAL_MS=750L;
  public static long PING_INTERVAL_SKEW_MS=750L;
  public static long REPORT_INTERVAL_MS=60000L;
  public static int TIMEOUT_MS=5000;
  public static int LOOKBACK=1000;

  public class PingThread extends PeriodicThread
  {
    private String target;
    private long last_report;

    // In microseconds
    private TimeSeries latency;
    private TimeSeries loss;
    public PingThread(String target)
    {
      super(PING_INTERVAL_MS);
      this.target=target;
      setName("Pinger/" + target);

      last_report=System.currentTimeMillis() - rnd.nextInt((int)REPORT_INTERVAL_MS);

      latency = new TimeSeries(LOOKBACK);
      loss = new TimeSeries(LOOKBACK);

    }

    public void runPass() throws Exception
    {
      InetAddress addr = InetAddress.getByName(target);

      long t0 = System.nanoTime();

      if (addr.isReachable(TIMEOUT_MS))
      {
        loss.addValue(0L);
      }
      else
      {
        loss.addValue(1L);
      }

      long t1 = System.nanoTime();
      long t = t1 - t0;
      t = t / 1000;

      latency.addValue(t);
      //System.out.println("ping: " + target + " " + t);

      report();
    }
    private void report()
      throws Exception
    {
      if (last_report + REPORT_INTERVAL_MS < System.currentTimeMillis())
      if (loss.getCount() > 4)
      if (latency.getCount() > 4)
      {
        Map<String, String> m = new TreeMap<>();
        m.put("source", source_name);
        m.put("target", target);
        DecimalFormat df = new DecimalFormat("0.00000");
        m.put("latency", df.format(latency.getAverage()/1000.0));
        m.put("loss", df.format(loss.getAverage()));
        m.put("count", "" + loss.getCount());

        sendReportLocal(m);
        last_report = System.currentTimeMillis();

      }

    }

  }

  
  int sendReportLocal(Map<String, String> query_map)
		throws Exception
  {
    JSONObject post = new JSONObject();

    post.putAll(query_map);

		String date = new SimpleDateFormat("yyyy.MM.dd").format(new Date());

		System.out.println("Report: " + post);

		byte[] send = post.toString().getBytes();

    URL u = new URL(String.format("http://metrics.1209k.com:9200/ping-%s/_doc", date));
    System.out.println(u);
    HttpURLConnection connection = (HttpURLConnection) u.openConnection();
    connection.setDoOutput(true);
    connection.setDoInput(true);
    connection.setRequestMethod("POST");
    connection.setRequestProperty("charset", "utf-8");
    connection.setRequestProperty("Content-Length", "" + Integer.toString(send.length));
    connection.setRequestProperty("Content-Type","application/json");


    OutputStream wr = connection.getOutputStream ();
    wr.write(send);
    wr.flush();
    wr.close();

    return connection.getResponseCode();

  }

}
