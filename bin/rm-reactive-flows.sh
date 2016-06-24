#!/usr/bin/env sh

containers=$(docker ps --quiet --filter "name=reactive-flows")
[[ -n ${containers} ]] && docker rm -f ${containers}
