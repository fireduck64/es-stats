import java.io.FileInputStream;
import java.util.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.OutputStream;
import duckutil.webserver.DuckWebServer;
import duckutil.webserver.WebContext;
import duckutil.webserver.WebHandler;
import java.net.URI;
import java.net.URL;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import net.minidev.json.JSONObject;
import duckutil.NetUtil;

public class Prob implements WebHandler
{
  Random rnd=new Random();

  public static void main(String args[]) throws Exception
  {
    new Prob();
  }

	public Prob()
    throws Exception
	{
    DuckWebServer ws = new DuckWebServer(null, 12445, this, 10);

    System.out.println("Webserver started on :12445");

  }

  public void handle(WebContext t) throws Exception
  {
    System.out.println(t.getURI());
    URI uri = t.getURI();
    System.out.println("Query: " + uri.getQuery());
    System.out.println(t.getHost());
    System.out.println(t.getRequestBodyString());
    System.out.println(t.getRequestMethod());

    Map<String, String> query_map = splitQuery( uri.getQuery() );
    if (query_map.size() > 0)
    {
      t.setHttpCode(sendReportLocal(query_map));
      sendReportGMC(uri.getQuery());
    }
    else
    {
      t.setHttpCode(404);
    }

  }

  void sendReportGMC(String query)
    throws Exception
  {
    System.out.println(NetUtil.getUrlLine("https://www.gmcmap.com/log2.asp?" + query));
  }
  
  int sendReportLocal(Map<String, String> query_map)
		throws Exception
  {
    JSONObject post = new JSONObject();

    post.putAll(query_map);

    String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date());
		String date = new SimpleDateFormat("yyyy.MM.dd").format(new Date());

    //post.put("timestamp", timestamp);

		System.out.println("Report: " + post);

		byte[] send = post.toString().getBytes();

    URL u = new URL(String.format("http://metrics.1209k.com:9200/radiation-%s/_doc", date));
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

  Map<String, String> splitQuery(String query)
  {
    TreeMap<String, String> map = new TreeMap<>();
    List<String> lst = Tok.en(query, "&");
    for(String elem : lst)
    {
      List<String> e = Tok.en(elem, "=");
      if (e.size() == 2)
      {
        String key = e.get(0);
        String val = e.get(1);
        map.put(key, val);
      }


    }

    return map;

  }

}
