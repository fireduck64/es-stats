package duckutil.lert;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import duckutil.TaskMaster;
import duckutil.Pair;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.TreeMap;
import java.net.InetSocketAddress;

public class WebServer
{
  private HttpServer server;
  private final StatusRegistry reg;


  public WebServer(StatusRegistry reg, int port) throws Exception
  {
    this.reg = reg;

    server = HttpServer.create(new InetSocketAddress(port), 0);
    server.createContext("/", new RootHandler());
    server.setExecutor(TaskMaster.getBasicExecutor(8,"wob"));
    server.start();
  }

  class RootHandler implements HttpHandler
  {
    @Override
    public void handle(HttpExchange t)
      throws IOException
    {
      t.getResponseHeaders().add("Content-Language", "en-US");
      t.getResponseHeaders().add("Content-Type", "text/plain");
      ByteArrayOutputStream b_out = new ByteArrayOutputStream();
      PrintStream print_out = new PrintStream(b_out);
      int resp_code = 200;

      TreeMap<LertState, Integer> counts = new TreeMap<>();


      Map<String, Pair<LertState, String> > map = reg.getMap();
      for(String id : map.keySet())
      {
        print_out.println(id + " - " + map.get(id));

        LertState s = map.get(id).getA();
        if (!counts.containsKey(s))
        {
          counts.put(s, 0);
        }
        counts.put(s, counts.get(s) + 1);
      }

      print_out.println(counts);

      byte[] data = b_out.toByteArray();
      t.sendResponseHeaders(resp_code, data.length);
      OutputStream out = t.getResponseBody();
      out.write(data);
      out.close();
    }

  }


}
