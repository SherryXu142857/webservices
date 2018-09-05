#!/bin/bash

echo "### - Cleaning database tables for CISPaces "
source ~/.profile

cd ${CISPACES}/tools/derby

echo "clean database ? (y/n)"
read flag;
if [ "$flag" == "y" ]; 
then echo "# - Cleaning tables from ${CISPACES}/../DATABASE/cleandb.sql "
bin/ij ${CISPACES}/../DATABASE/cleandb.sql
if [ $? -eq 0 ]; then echo "[OK]"; else echo "[Failed]";fi
fi

echo "create test enviornment ? (y/n)"
read flag;
if [ "$flag" == "y" ]; 
then echo "# - Setting environment from ${CISPACES}/../DATABASE/testen.sql "
bin/ij ${CISPACES}/../DATABASE/testen.sql
if [ $? -eq 0 ]; then echo "[OK]"; else echo "[Failed]";fi
exit;
fi

 
