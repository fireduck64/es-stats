
package main

import "fmt"
import "encoding/json"
import "os"
import "bufio"
import "strings"

type pool_report struct {
  State string `json:"state"`
  Online int `json:"online"`
}

type status_report struct {
  Pools map[string]pool_report `json:"pools"`
}

func main() {

  var report status_report = status_report{}

  // todo - make the struct do this automatically
  report.Pools = map[string]pool_report{}


  scanner := bufio.NewScanner(os.Stdin)
  var pool_name string = ""

  for scanner.Scan() {
    line := scanner.Text()
    line = strings.Trim(line, " \t")
    if (strings.HasPrefix(line, "pool: ")) {
      pool_name = extractValue(line)
    }
    if (strings.HasPrefix(line, "state: ")) {
      var state string = extractValue(line)
      var pool pool_report = pool_report { State: state };
      if (state == "ONLINE") {
        pool.Online = 1
      }

      report.Pools[pool_name] = pool;

    }

  }



  res1B, _ := json.Marshal(report)
  fmt.Println(string(res1B))
}

func extractValue(line string) string {
  var val string

  _, val, _ = strings.Cut(line, ":")
  val = strings.Trim(val, " ")

  return val

}
