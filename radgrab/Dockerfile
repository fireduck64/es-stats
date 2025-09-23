FROM 1209k/bazel

#RUN apt-get update
#RUN apt-get install -y curl

COPY . .

RUN bazel build :all

CMD [ "./run.sh" ]



