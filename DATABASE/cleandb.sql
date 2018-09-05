-- SQL to clean CISpaces database tables
-- kx1n17@soton.ac.uk

connect 'jdbc:derby://localhost:1527/gaiandb;create=true;user=gaiandb;password=passw0rd';

DELETE FROM CISPACES_EDGE;
DELETE FROM CISPACES_EDGE_HISTORY;
DELETE FROM CISPACES_EDGE_PROV;
DELETE FROM CISPACES_GRAPH; 
DELETE FROM CISPACES_GRAPH_AUTHORITY;
DELETE FROM CISPACES_GRAPH_HISTORY;
DELETE FROM CISPACES_GRAPH_PROV;
DELETE FROM CISPACES_INFOPROV;
DELETE FROM CISPACES_INFOTMP; 
DELETE FROM CISPACES_NODE;
DELETE FROM CISPACES_NODE_HISTORY;
DELETE FROM CISPACES_NODE_PROV;
DELETE FROM CISPACES_PROJECT;
DELETE FROM CISPACES_PROJECT_AUTHORITY;
