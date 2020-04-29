#!/bin/sh


curl -s -w "{\"code\": %{http_code}, \"content-type\": \"%{content_type}\", \"size\": %{size_download}, \"curltime\": %{time_total}}" --output /dev/null $1 




