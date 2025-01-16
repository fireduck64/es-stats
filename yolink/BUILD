package(default_visibility = ["//visibility:public"])

java_library(
  name = "problib",
  srcs = glob(["src/**/*.java", "src/*.java"]),
  deps = [
    "@duckutil//:duckutil_lib",
    "@duckutil//:webserver_lib",
    "@maven//:net_minidev_json_smart",
    "@maven//:com_google_guava_guava",
    "@maven//:tools_aqua_z3_turnkey",
  ],
)

java_binary(
  name = "Yolink",
  main_class = "Yolink",
  runtime_deps = [
    ":problib",
  ],
)

