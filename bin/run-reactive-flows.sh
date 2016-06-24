#!/usr/bin/env sh

n=0
if [ -n "$1" ]; then
    n="$1"
fi
tag=latest
if [ -n "$2" ]; then
    tag="$2"
fi
: ${HOST:=$(ipconfig getifaddr en0)}
: ${HOST:=$(ipconfig getifaddr en1)}
: ${HOST:=$(ipconfig getifaddr en2)}
: ${HOST:=$(ipconfig getifaddr en3)}
: ${HOST:=$(ipconfig getifaddr en4)}

docker run \
  --detach \
  --name reactive-flows-${n} \
  --publish 800${n}:8000 \
  hseeberger/reactive-flows:${tag} \
  -Dcassandra-journal.contact-points.0=${HOST}:9042 \
  -Dconstructr.coordination.host=${HOST}
