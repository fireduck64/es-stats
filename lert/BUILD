package(default_visibility = ["//visibility:public"])

java_library(
  name = "lertlib",
  srcs = glob(["src/**/*.java", "src/*.java"]),
  deps = [
    "@duckutil//:duckutil_lib",
    "@duckutil//:sql_lib",
    "@maven//:commons_pool_commons_pool",
    "@maven//:commons_dbcp_commons_dbcp",
    "@maven//:com_amazonaws_aws_java_sdk",
    "@maven//:net_minidev_json_smart",
    "@maven//:org_elasticsearch_client_elasticsearch_rest_client",
    "@maven//:org_elasticsearch_client_elasticsearch_rest_high_level_client",
    "@maven//:org_apache_httpcomponents_httpcore",
    "@maven//:org_elasticsearch_elasticsearch",
    "@maven//:com_google_guava_guava",
    "@maven//:com_amazonaws_aws_java_sdk_sns",
    "@maven//:com_amazonaws_aws_java_sdk_core",
    "@maven//:org_eclipse_paho_org_eclipse_paho_client_mqttv3",
  ],
)

java_binary(
  name = "Lert",
  main_class = "duckutil.lert.Lert",
  runtime_deps = [
    ":lertlib",
  ],
)

java_binary(
  name = "QMon",
  main_class = "duckutil.lert.QMon",
  runtime_deps = [
    ":lertlib",
  ],
)

