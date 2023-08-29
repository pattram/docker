#!/bin/bash
for i in {1..100}
do
   echo "========================================="
   echo "Execute IntegrationTest for the ${i}th time"
   echo "========================================="
   test 2>&1 | tee -a migration.log
done
