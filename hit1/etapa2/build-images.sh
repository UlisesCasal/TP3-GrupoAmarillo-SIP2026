#!/bin/bash
set -e

# Etapa 2 Distribuido
docker build -t tp3-consumer-hit1-et2 ./consumer/
docker build -t tp3-producer-hit1-et2 ./producer/
docker build -t tp3-joiner-hit1-et2 ./joiner/