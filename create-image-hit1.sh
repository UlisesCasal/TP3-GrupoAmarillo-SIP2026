#!/bin/bash
set -e
# Etapa 1 Centralizado
docker build -t tp3-hit1-etapa1 ./hit1/etapa1/
# Etapa 2 Distribuido
docker build -t tp3-consumer-hit1-et2 ./hit1/etapa2/consumer/
docker build -t tp3-producer-hit1-et2 ./hit1/etapa2/producer/

