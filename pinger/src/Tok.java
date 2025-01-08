
import java.util.*;


public class Tok
{
  public static List<String> en(String input, String delim)
  {
    ArrayList<String> lst = new ArrayList<>();
    StringTokenizer stok = new StringTokenizer(input, delim);

    while(stok. hasMoreTokens())
    {
      lst.add(stok. nextToken());
    }

    return lst;
  }

  public static List<Integer> ent(String input, String delim)
  {
    List<String> lst = en(input, delim);
    List<Integer> out = new ArrayList<>();
    for(String s : lst)
    {
      try
      {
      out.add(Integer.parseInt(s));
      }
      catch(Exception e)
      {}
    }
    return out;

  }
  public static List<Long> enl(String input, String delim)
  {
    List<String> lst = en(input, delim);
    List<Long> out = new ArrayList<>();
    for(String s : lst)
    {
      try
      {
      out.add(Long.parseLong(s));
      }
      catch(Exception e)
      {}
    }
    return out;

  }


}
