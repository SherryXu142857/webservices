/**
 *  This class consists of the SQL queries for creating the tables which enable the support of Version Control within the application.
 *  Note that these are only put here for reference! A script should be written to create ALL tables.
 *  It also includes queries which will be executed on the back-end to accommodate front-end interaction with the program.
 *  The database used is Derby, so all queries are written to support the Derby SQL dialect.
 *
 *  @author Yordanka Ivanova
 *  @since July 2017
 */

package database;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import utils.TimeHelper;

import java.sql.Timestamp;
import java.util.*;



public class DBQuery {

    /*This object manages the connection to the database and the execution of queries
    * @see DBConnect in the database package
    */
    protected DBConnect dbcn;

    public DBQuery() {
        dbcn = new DBConnect();
    }

    /*
     These methods show the statements used to create the database tables. They are for reference only! A script should be written to create the database tables.
     */
    public void createTableGraph(){
        String query = "CREATE TABLE CISPACES_GRAPH ( graphid varchar (255), userid varchar(255), timest timestamp, title varchar(255), description varchar (5000), isshared boolean, parentgraphid varchar(255), ";
        query+="CONSTRAINT CISPACES_GRAPH_pk PRIMARY KEY (graphid))";
        dbcn.updateSQL(query);
    }

    public void createTableGraphHistory(){
        String query = "CREATE TABLE CISPACES_GRAPH_HISTORY (revisionid varchar(255), graphid varchar(255), userid varchar(255), " +
                "timest timestamp, isshared boolean, parentgraphid varchar(255), ";
        query+="CONSTRAINT GRAPH_SESSION_HISTORY_pk PRIMARY KEY (revisionid))";
        dbcn.updateSQL(query);
    }

    public void createTableGraphHistoryNew(){
        String query = "CREATE TABLE CISPACES_GRAPH_HISTORY (snapid varchar(255), graphid varchar(255), userid varchar(255), " +
                "timest timestamp, analysis clob(2M), title varchar(255), ";
        query+="CONSTRAINT CISPACES_GRAPH_HISTORY_pk PRIMARY KEY (snapid))";
        dbcn.updateSQL(query);
    }

    public void createTableNode(){
        String query="CREATE TABLE CISPACES_NODE ( nodeid varchar(255), source varchar (255), uncert varchar(20), eval varchar(5000), "
                + "txt varchar (5000), inp varchar (20), dtg timestamp, cmt varchar(50), type varchar(10), annot varchar (5000), graphid varchar(255), "
                + "islocked boolean, ";
        query+="CONSTRAINT CISPACES_NODE_pk PRIMARY KEY (nodeid))";
        dbcn.updateSQL(query);
    }

    public void createTableNodeHistory(){
        String query="CREATE TABLE CISPACES_NODE_HISTORY ( nodeid varchar(255), source varchar (255), uncert varchar(20), eval varchar(5000), "
                + "txt varchar (5000), inp varchar (20), dtg timestamp, cmt varchar(50), type varchar(10), annot varchar (5000), graphid varchar(255), "
                + "islocked boolean, revisionid varchar(255), ";
        query+="CONSTRAINT CISPACES_NODE_HISTORY_pk PRIMARY KEY (revisionid))";
        dbcn.updateSQL(query);
    }

    public void createTableEdge(){
        String query="create table cispaces_edge ( edgeID varchar(255), target varchar(255), source varchar(255), formEdgeID varchar(255), graphID varchar(255), islocked boolean, CONSTRAINT CISPACES_EDGE_pk PRIMARY KEY (edgeID))";
        dbcn.updateSQL(query);
    }

    public void createTableEdgeHistory(){
        String query="CREATE TABLE CISPACES_EDGE_HISTORY ( edgeid varchar(255), tonodeid varchar(255), fromnodeid varchar(255), formedgeid varchar(255)," +
                "graphid varchar(255), islocked boolean, revisionid varchar(255), ";
        query+="CONSTRAINT CISPACES_EDGE_pk PRIMARY KEY (revisionid))";
        dbcn.updateSQL(query);
    }

    //shut down the communication between the web service and the database
    public void closeDatabase() {
        dbcn.forceClose();
    }

    public void insertProject(String projectid, String userid, Timestamp timest, String title, String description){
        String sql;
        sql = "INSERT INTO CISPACES_PROJECT(projectid, userid, timest, title, description) VALUES "
                + "( '" + projectid + "' ,"
                + " '" + userid + "' ,"
                + " '" + timest + "' ,"
                + " '" + title +  "' ,"
                + " '" + description +"'"
                +" )";
        
        System.out.println("DBQuery.insertProject: "+sql);
        dbcn.updateSQL(sql);
    }
    
    public boolean deleteProject(String projectID) {
        //ToDo - get nodeIds and delete from CISPACES_INFOPROV
        String[] tables = {"CISPACES_PROJECT", "CISPACES_PROJECT_AUTHORITY"};
        String sql;
        for(String table : tables) {
            sql = "DELETE FROM " + table + " WHERE projectid = '" + projectID + "'";
            if(!dbcn.updateSQL(sql)) {
                System.out.println("Failed to delete Graph from "+table);
                return false;
            }
        }
        
        String sql1 = "SELECT graphid FROM CISPACES_GRAPH WHERE projectid = '" + projectID + "'";
        ArrayList<HashMap<String, Object>> graphs = dbcn.execSQL(sql1);
        if(!graphs.isEmpty()){
            for(HashMap graph : graphs){
                if(!this.deleteAnalysis((String)graph.get("graphid")))
                    return false;
            }
        }
        return true;       
    }
    
    public void insertProjectAuthority(String projectid, String userid, boolean isowner){
        String sql;
        sql = "INSERT INTO CISPACES_PROJECT_AUTHORITY(projectid, userid, isowner) VALUES "
                + "( '" + projectid + "' ,"
                + " '" + userid + "' ,"
                + " '" + isowner + "'"
                +" )";
        System.out.println("DBQuery.insertProjectAuthority: "+sql);
        dbcn.updateSQL(sql);
    }
    
    public String getProjectMeta(String projectID) {
        System.out.println("Fetching project metadata for projectid: "+projectID);
        String getMetaSql = "SELECT * FROM CISPACES_PROJECT WHERE PROJECTID = '" + projectID + "'";
        ArrayList<HashMap<String, Object>> projectMeta = dbcn.execSQL(getMetaSql);
        
        if(!projectMeta.isEmpty()) {
            JSONObject jsonProject = new JSONObject();
            jsonProject.put("projectID", (String) projectMeta.get(0).get("projectid"));
            jsonProject.put("userID", (String) projectMeta.get(0).get("userid"));
            jsonProject.put("timest", (String) projectMeta.get(0).get("timest"));
            jsonProject.put("title", (String) projectMeta.get(0).get("title"));
            jsonProject.put("description", (String) projectMeta.get(0).get("description"));
            return jsonProject.toString();
        }
        else {
            return null; //Not found
        }
    }
    
    public String getProjectMetaByUser(String userID){
        System.out.println("Fetching project metadata by userid: " + userID);
        JSONArray jsonProjectMetaArray = new JSONArray();
        String sql1 = "SELECT * FROM CISPACES_PROJECT_AUTHORITY WHERE USERID = '" +  userID + "'"; 
        ArrayList<HashMap<String,Object>> projects = dbcn.execSQL(sql1);
        for(HashMap project : projects){
            String sql2 = "SELECT * FROM CISPACES_PROJECT WHERE PROJECTID = '" + project.get("projectid") + "'";
            System.out.println(sql2);
            ArrayList<HashMap<String,Object>> projectinfo = dbcn.execSQL(sql2);
            String userid = (String) projectinfo.get(0).get("userid");
            JSONObject Project = new JSONObject();
            Project.put("projectID", (String) project.get("projectid"));
            Project.put("userID", userid);
            Project.put("timest", (String) projectinfo.get(0).get("timest"));
            Project.put("title", (String) projectinfo.get(0).get("title"));
            Project.put("description", (String) projectinfo.get(0).get("description"));
            Project.put("isowner", (String) project.get("isowner"));
            
            String sql3 = "SELECT USERNAME FROM CISPACES_USERS WHERE USER_ID = '" + userid + "'";
            ArrayList<HashMap<String,Object>> username = dbcn.execSQL(sql3);
            Project.put("userName", (String) username.get(0).get("username"));
            jsonProjectMetaArray.add(Project);  
        }

        return jsonProjectMetaArray.toJSONString();
    }
    
    public HashMap constructGraphData(ArrayList<HashMap<String,Object>> graphdata){
        String graphID = (String)graphdata.get(0).get("graphid");
        HashMap graphMeta = new HashMap<String, String>();
        graphMeta.put("graphID", graphID);
        graphMeta.put("title", (String) graphdata.get(0).get("title"));
        graphMeta.put("description", (String) graphdata.get(0).get("description"));
        graphMeta.put("parentgraphid", (String) graphdata.get(0).get("parentgraphid"));
        graphMeta.put("originalgraphid", (String) graphdata.get(0).get("originalgraphid"));

        Iterator<HashMap<String,Object>> iter;
        
        HashMap nodesMeta = new HashMap<String, HashMap<String,Object>>();
        String getNodesSql = "SELECT N.*, NP.PARENTNODEID, NP.ORIGINALNODEID, NP.ISMERGABLE FROM CISPACES_NODE N, CISPACES_NODE_PROV NP WHERE N.NODEID = NP.NODEID AND N.GRAPHID = '" + graphID + "' AND NP.ISMERGABLE = 1";
        ArrayList<HashMap<String,Object>> graphsnodes = dbcn.execSQL(getNodesSql);
        iter = graphsnodes.iterator();
        for(; iter.hasNext();){
            HashMap nodeMeta = iter.next();
            nodesMeta.put(nodeMeta.get("nodeid"), nodeMeta);
        }
        graphMeta.put("nodesMeta", nodesMeta); 
        
        HashMap edgesMeta = new HashMap<String, HashMap<String,Object>>();
        String getEdgesSql = "SELECT E.*, EP.PARENTEDGEID, EP.ORIGINALEDGEID, EP.ISMERGABLE FROM CISPACES_EDGE E, CISPACES_EDGE_PROV EP WHERE EP.EDGEID = E.EDGEID AND E.GRAPHID = '" + graphID + "' AND EP.ISMERGABLE = 1";
        ArrayList<HashMap<String,Object>> graphsedges = dbcn.execSQL(getEdgesSql);
        iter = graphsedges.iterator();
        for(; iter.hasNext();){
            HashMap edgeMeta = iter.next();
            edgesMeta.put(edgeMeta.get("edgeid"), edgeMeta);
        }
        graphMeta.put("edgesMeta", edgesMeta); 
        
        return graphMeta;
    }
    
    public HashMap getOriginalAnalysis(ArrayList graphIDList){
        HashMap OriginalGraphMeta = new HashMap();
        String sql;
        Iterator<String> iter = graphIDList.iterator();
        for(; iter.hasNext();){
            sql = "SELECT G.GRAPHID, G.TITLE, G.DESCRIPTION, GP.PARENTGRAPHID, GP.ORIGINALGRAPHID FROM CISPACES_GRAPH G, CISPACES_GRAPH_PROV GP WHERE G.GRAPHID = GP.GRAPHID AND G.GRAPHID = '" + iter.next() + "'";
            ArrayList<HashMap<String,Object>> originalGraph = dbcn.execSQL(sql);
            if(!originalGraph.isEmpty()){
                HashMap originalGraphMeta = constructGraphData(originalGraph);
                OriginalGraphMeta.put((String)originalGraph.get(0).get("graphid"), originalGraphMeta);
            }
            else return null;
        }
        return OriginalGraphMeta;
    }
    
    public HashMap getSubGraphs(String projectID){
        HashMap subGraphMeta = new HashMap();
        String sql = "SELECT DISTINCT USERID FROM CISPACES_GRAPH WHERE PROJECTID = '" + projectID + "'";
        ArrayList<HashMap<String,Object>> contributors = dbcn.execSQL(sql);
        if(!contributors.isEmpty()){
            for(HashMap c : contributors){
                String sql1 = "SELECT G.GRAPHID, G.TITLE, G.DESCRIPTION, GP.PARENTGRAPHID, GP.ORIGINALGRAPHID FROM CISPACES_GRAPH G, CISPACES_GRAPH_PROV GP WHERE G.GRAPHID = GP.GRAPHID AND G.USERID = '" + c.get("userid") + "' AND G.PROJECTID = '" + projectID + "' AND GP.TYPE = 0 ORDER BY G.TIMEST DESC OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY";
                ArrayList<HashMap<String,Object>> subgraph = dbcn.execSQL(sql1);
                if(!subgraph.isEmpty()){
                    HashMap graphMeta = constructGraphData(subgraph);
                    subGraphMeta.put((String)subgraph.get(0).get("graphid"), graphMeta); 
                }
            }
            return subGraphMeta;
        }
        else return null;
    }
    
    public String getContributorsList(String userID, String projectID) {
        JSONArray jsonAnalysesMetaArray = new JSONArray();
        String sql1 = "SELECT DISTINCT USERID FROM CISPACES_PROJECT_AUTHORITY WHERE USERID <> '"+ userID + "' AND PROJECTID = '" + projectID + "' GROUP BY USERID";
        ArrayList<HashMap<String,Object>> users = dbcn.execSQL(sql1);
        if(!users.isEmpty()){
            for(HashMap user : users){
                String sql2 = "SELECT USERNAME FROM CISPACES_USERS WHERE USER_ID = '"+ user.get("userid") + "'";
                ArrayList<HashMap<String,Object>> username = dbcn.execSQL(sql2);
                JSONObject jsonGraph = new JSONObject();
                jsonGraph.put("userID", (String) user.get("userid"));
                jsonGraph.put("userName", (String) username.get(0).get("username"));

                jsonAnalysesMetaArray.add(jsonGraph);                   
            }
            return jsonAnalysesMetaArray.toJSONString();
        }
        else return null;
    }
    
    //the query to insert a new graph into the database
    public void insertGraph(String graphid, String userid, Timestamp timest, String title, String description, boolean isshared, String parentgraphid, String projectid){
        String sql;
        sql = "INSERT INTO CISPACES_GRAPH(graphid, userid, timest, title, description, isshared, parentgraphid, projectid) VALUES "
                + "( '" + graphid + "' ,"
                + " '" + userid + "' ,"
                + " '" + timest + "' ,"
                + " '" + title +  "' ,"
                + " '" + description +"' ,"
                + " '" + isshared + "' ,"
                + " '" + parentgraphid + "',"
                + " '" + projectid + "'"
                +" )";
        
        System.out.println("DBQuery.insertGraph: "+sql);
        dbcn.updateSQL(sql);
    }
    
    public void insertGraphAuthority(String graphid, String userid, int authoritytype, String projectid){
        String sql;
        sql = "INSERT INTO CISPACES_GRAPH_AUTHORITY(graphid, userid, authoritytype, projectid) VALUES "
                + "( '" + graphid + "' ,"
                + " '" + userid + "' ,"
                + authoritytype + ","
                + " '" + projectid + "'"
                +" )";
        
        System.out.println("DBQuery.insertGraphAuthority: "+sql);
        dbcn.updateSQL(sql);
    }
    
    public void insertGraphProv(String graphID, String parentgraphid, String originalgraphid, int type){
        String sql;
        sql = "INSERT INTO CISPACES_GRAPH_PROV(graphid, parentgraphid, originalgraphid, type) VALUES "
                + "( '" + graphID + "' ,"
                + " '" + parentgraphid + "' ,"
                + " '" + originalgraphid +  "' ,"
                + type
                +" )";
        
        System.out.println("DBQuery.insertGraphProv: "+sql);
        dbcn.updateSQL(sql);
    }

    //queries to insert a node or update an existing node information in the database
    public void insertNode(String nodeID, String source, String uncert, String eval, String txt, String input,
                           Timestamp timestamp, String commit, String type, String annot, String graphID, boolean isLocked)
    {
        String sql;
        sql = "Select * from CISPACES_NODE WHERE nodeid = " + "'" + nodeID + "'";
        ArrayList<HashMap<String,Object>> rs = dbcn.execSQL(sql);
        if(rs.isEmpty()){
            System.out.println("NODE DOESNT EXIST");
            sql = "INSERT INTO CISPACES_NODE (nodeid, source, uncert, eval, txt, inp, dtg, cmt, type, annot, graphid, islocked) VALUES "
                    + "( '"+nodeID+"' ,"
                    + " '"+source+"' ,"
                    + " '"+uncert+"' ,"
                    + " '"+eval+"' ,"
                    + " '"+txt+"' ,"
                    + " '"+input+"' ,"
                    + " '"+timestamp+"' ,"
                    + " '"+commit+"' ,"
                    + " '"+type+"' ,"
                    + " '"+annot+"' ,"
                    + " '"+graphID+"' ,"
                    + " '"+isLocked+"'"
                    + " )";
            System.out.println(sql);
        }else{
            System.out.println("NODE EXISTS");
            sql = "UPDATE CISPACES_NODE SET uncert = " + "'" + uncert + "'" + " ,"
                                        + " eval = " + "'" + eval + "'" + " ,"
                                        + " txt = "  + "'" + txt + "'" + " ,"
                                        + " inp = "  + "'" + input + "'" + " ,"
                                        + " cmt = "  + "'" + commit + "'" + " ,"
                                        + " annot = " + "'" + annot + "'"
                    + " WHERE nodeid = " + "'" + nodeID + "'";
            System.out.println(sql);
        }

        dbcn.updateSQL(sql);
    }

    public void insertNodeProv(String nodeID, String parentnodeid, String originalnodeid, int ismergable, String graphID){
        String sql;
        sql = "Select * from CISPACES_NODE_PROV WHERE nodeid = " + "'" + nodeID + "'";
        ArrayList<HashMap<String,Object>> rs = dbcn.execSQL(sql);
        if(rs.isEmpty()){
            System.out.println("NODE PROVENANCE RECORD DOESNT EXIST");
            sql = "INSERT INTO CISPACES_NODE_PROV VALUES"
                    + "( '" + nodeID + "', " 
                    + " '"+parentnodeid+"' ,"
                    + " '"+originalnodeid+"' ,"
                    + ismergable+" ,"
                    + " '"+graphID+"'"
                    +")";
        }
        else {
            System.out.println("NODE PROVENANCE RECORD EXISTS");
            sql = "UPDATE CISPACES_NODE_PROV SET ismergable = " + ismergable + " WHERE nodeid = '" + nodeID + "'";
        }
        dbcn.updateSQL(sql);
    }
    
    //insert a node in the history table or update the information if it exists
    public void insertNodeHistory(String nodeID, String source, String uncert, String eval, String txt, String input,
                           Timestamp timestamp, String commit, String type, String annot, String graphID, boolean isLocked, String revisionID)
    {
        String sql;
        sql = "Select * from CISPACES_NODE_HISTORY WHERE nodeid = " + "'" + nodeID + "'";
        ArrayList<HashMap<String,Object>> rs = dbcn.execSQL(sql);
        if(rs.isEmpty()){
            System.out.println("NODE DOESNT EXIST IN HISTORY TABLE");
            sql = "INSERT INTO CISPACES_NODE_HISTORY (nodeid, source, uncert, eval, txt, inp, dtg, cmt, type, annot, graphid, islocked, revisionid) VALUES "
                    + "( '"+nodeID+"' ,"
                    + " '"+source+"' ,"
                    + " '"+uncert+"' ,"
                    + " '"+eval+"' ,"
                    + " '"+txt+"' ,"
                    + " '"+input+"' ,"
                    + " '"+timestamp+"' ,"
                    + " '"+commit+"' ,"
                    + " '"+type+"' ,"
                    + " '"+annot+"' ,"
                    + " '"+graphID+"'"
                    + " '"+isLocked+"'"
                    + " '"+revisionID+"'"
                    + " )";
            System.out.println(sql);
        }else{
            System.out.println("NODE EXISTS IN HISTORY TABLE");
            sql = "UPDATE CISPACES_NODE SET uncert = " + "'" + uncert + "'" + " ,"
                    + " eval = " + "'" + eval + "'" + " ,"
                    + " txt = "  + "'" + txt + "'" + " ,"
                    + " inp = "  + "'" + input + "'" + " ,"
                    + " cmt = "  + "'" + commit + "'" + " ,"
                    + " annot = " + "'" + annot + "'" + " ,"
                    + " islocked = " + "'" + isLocked + "'"
                    + " WHERE nodeid = " + "'" + nodeID + "'";
            System.out.println(sql);
        }

        dbcn.updateSQL(sql);
    }

    //insert an edge into the database - edges are never updated!
    public void insertEdge(String toID, String fromID, String formEdgeID, String edgeID, String graphID, boolean isLocked) {
        String sql;
        sql = "Select * from CISPACES_EDGE WHERE edgeid = " + "'" + edgeID + "'";
        ArrayList<HashMap<String,Object>> rs = dbcn.execSQL(sql);
        if(rs.isEmpty()) {
            System.out.println("EDGE DOESNT EXIST");
            sql = "INSERT INTO CISPACES_EDGE (edgeID, target, source, formEdgeID, graphID, islocked) VALUES "
                    + "( '" + edgeID + "' ,"
                    + " '" + toID + "' ,"
                    + " '" + fromID + "' ,"
                    + " '" + formEdgeID + "' ,"
                    + " '" + graphID + "' ,"
                    + " '" + isLocked + "'"
                    + " )";
            dbcn.updateSQL(sql);
        }

        System.out.println(sql);

    }
    
    public void insertEdgeProv(String edgeID, String parentedgeid, String originaledgeid, int ismergable, String graphID){
        String sql;
        sql = "Select * from CISPACES_EDGE_PROV WHERE edgeid = " + "'" + edgeID + "'";
        ArrayList<HashMap<String,Object>> rs = dbcn.execSQL(sql);
        if(rs.isEmpty()){
            System.out.println("EDGE PROVENANCE RECORD DOESNT EXIST");
            sql = "INSERT INTO CISPACES_EDGE_PROV VALUES"
                    + "( '" + edgeID + "', " 
                    + " '"+parentedgeid+"' ,"
                    + " '"+originaledgeid+"' ,"
                    + ismergable+", "
                    + " '"+graphID+"'"
                    +")";
        }
        else {
            System.out.println("EDGE PROVENANCE RECORD EXISTS");
            sql = "UPDATE CISPACES_EDGE_PROV SET ismergable = " + ismergable + " WHERE edgeid = '" + edgeID + "'";
        }
        dbcn.updateSQL(sql);
    }

    //populate the edge history table
    public void insertEdgeHistory(String toID, String fromID, String formEdgeID, String edgeID, String graphID, boolean isLocked, String revisionID) {
        String sql;
        sql = "Select * from CISPACES_EDGE_HISTORY WHERE edgeid = " + "'" + edgeID + "'";
        ArrayList<HashMap<String,Object>> rs = dbcn.execSQL(sql);
        if(rs.isEmpty()) {
            System.out.println("EDGE DOESNT EXIST IN HISTORY TABLE");
            sql = "INSERT INTO CISPACES_EDGE_HISTORY (edgeid, tonodeid, fromnodeid, formedgeid, graphid, islocked, revisionid) VALUES "
                    + "( '" + edgeID + "' ,"
                    + " '" + toID + "' ,"
                    + " '" + fromID + "' ,"
                    + " '" + formEdgeID + "' ,"
                    + " '" + graphID + "' ,"
                    + " '" + isLocked + "' ,"
                    + " '" + revisionID + "'"
                    + " )";
            dbcn.updateSQL(sql);
        }

        System.out.println(sql);

    }

    /**
     * Delete the analysis, all nodes, edges and provenance
     * @param graphID the analysis to delete
     * @return True if the operation succeeded, false if it did not
     */
    public boolean deleteAnalysis(String graphID) {
        //ToDo - get nodeIds and delete from CISPACES_INFOPROV
        String[] tables = {"CISPACES_GRAPH", "CISPACES_GRAPH_HISTORY", "CISPACES_EDGE",
        "CISPACES_EDGE_HISTORY", "CISPACES_NODE", "CISPACES_NODE_HISTORY", 
        "CISPACES_GRAPH_AUTHORITY", "CISPACES_GRAPH_PROV", "CISPACES_NODE_PROV", "CISPACES_EDGE_PROV"};
        String sql;
        for(String table : tables) {
            sql = "DELETE FROM "+table+" WHERE graphid = '" + graphID + "'";
            if(!dbcn.updateSQL(sql)) {
                System.out.println("Failed to delete Graph from "+table);
                return false;
            }
        }        
        return true;       
    }
        
    //delete an edge from the database
    public boolean deleteEdge(String edgeid) {
        String sql = "DELETE FROM CISPACES_EDGE WHERE edgeid = " + "'" + edgeid + "'";
        System.out.println(sql);
        boolean isStatementExecuted = dbcn.updateSQL(sql);

        return isStatementExecuted;
    }

    public boolean deleteEdgeProv(String edgeid) {
        String sql = "DELETE FROM CISPACES_EDGE_PROV WHERE edgeid = " + "'" + edgeid + "'";
        System.out.println(sql);
        boolean isStatementExecuted = dbcn.updateSQL(sql);

        return isStatementExecuted;
    }
    
    //delete a node from the database
    public boolean deleteNode(String nodeid) {
        String sql = "DELETE FROM CISPACES_NODE WHERE nodeid = " + "'" + nodeid + "'";
        System.out.println(sql);
        boolean isStatementExecuted = dbcn.updateSQL(sql);

        return isStatementExecuted;
    }
    
    public boolean deleteNodeProv(String nodeid) {
        String sql = "DELETE FROM CISPACES_NODE_PROV WHERE nodeid = " + "'" + nodeid + "'";
        System.out.println(sql);
        boolean isStatementExecuted = dbcn.updateSQL(sql);

        return isStatementExecuted;
    }

    /**
     * Fetch everything from CISPACES_USERS for the supplied username
     * @param username The username of interest
     * @return a JSON representation of the record, null if none
     */
    public JSONObject getUserByUsername(String username) {
        String sql = "SELECT * FROM CISPACES_USERS WHERE USERNAME = '"+username+"'";
        System.out.println(sql);
        ArrayList<HashMap<String,Object>> rs = dbcn.execSQL(sql); 
        if(rs.isEmpty()) return null;
        else return (JSONObject) getResultListJSON(rs).get(0);
    }
    
    /**
     * Add a user with the supplied username, password hash and affiliation
     * @param username The chosen username
     * @param passwordHash The password hash
     * @param affiliation The affiliation
     * @return The newly generated user_id, or null if the user could not be added
     */
    public String insertUser(String username, String generatedSecuredPasswordHash, String affiliation) {
        String sqlCheckUser = "SELECT * FROM CISPACES_USERS WHERE USERNAME = '"+username+"'";
        System.out.println(sqlCheckUser);
        ArrayList<HashMap<String,Object>> rs = dbcn.execSQL(sqlCheckUser);        
        if(rs.isEmpty()){
            //Username doesn't exist, try to add it        
            String userID = UUID.randomUUID().toString();
            String sql = "INSERT INTO CISPACES_USERS (USER_ID, USERNAME, PASSWORD, AFFILIATION, IS_ADMIN) VALUES( "
                + "'" + userID + "',"
                + "'" + username + "',"
                + "'" + generatedSecuredPasswordHash + "',"
                + "'" + affiliation + "',"
                + "0)";        
            System.out.println(sql);
            if(dbcn.updateSQL(sql)){
                return userID; // User added successfully
            }
            else{
                System.out.println("Failed to add user.");
                return null;
            } // Failed to add - we should probably throw an exception here
        }
        else throw new IllegalArgumentException("The selected username is already in use");        
    }//insertUser

    /**
     * Return metadata for analyses belonging to the supplied user
     * @param userID The user
     * @return The analyses as a JSON formatted string
     *///mode = 0 onwed, mode = 1 shared, mode = 2 merged, mode = 3 getContributor graph
    public String getAnalysesMeta(String userid, String userID, String projectID, int mode) {
        String sql;
        if (userid == null && projectID == "noprojectid" && mode == 0) sql = "SELECT * FROM CISPACES_GRAPH WHERE USERID = '"+ userID + "'";
        else if (userid == null && projectID != "noprojectid" && mode == 0) sql = "SELECT * FROM CISPACES_GRAPH WHERE USERID = '"+ userID + "' AND PROJECTID = '" + projectID + "'";
        else if (userid == null && projectID != "noprojectid" && mode == 1) sql = "SELECT G.*, GA.AUTHORITYTYPE FROM CISPACES_GRAPH G, CISPACES_GRAPH_AUTHORITY GA WHERE G.GRAPHID = GA.GRAPHID AND GA.AUTHORITYTYPE <> 4 AND GA.USERID = '" + userID + "' AND GA.PROJECTID = '" + projectID + "'";
        else if (userid == null && projectID != "noprojectid" && mode == 2) sql = "SELECT G.*, A.AUTHORITYTYPE FROM CISPACES_GRAPH G, CISPACES_GRAPH_PROV P, CISPACES_GRAPH_AUTHORITY A WHERE G.GRAPHID = A.GRAPHID AND G.GRAPHID = P.GRAPHID AND A.USERID = '" + userID + "' AND G.PROJECTID = '"+ projectID + "' AND P.TYPE = 1 ORDER BY G.TIMEST";
        else if (userid != null && mode == 3) sql = "SELECT G.*, A.AUTHORITYTYPE FROM CISPACES_GRAPH G, CISPACES_GRAPH_AUTHORITY A WHERE G.GRAPHID = A.GRAPHID AND G.PROJECTID = '" + projectID + "' AND G.USERID = '" + userid + "' AND A.USERID = '" +userID + "'";
        else return null;
        ArrayList<HashMap<String,Object>> rs = dbcn.execSQL(sql);
        if(!rs.isEmpty()){
            JSONArray jsonAnalysesMetaArray = new JSONArray();
            for(HashMap graphIdMap : rs) { 
                JSONObject jsonGraph = new JSONObject();
                jsonGraph.put("graphID", (String) graphIdMap.get("graphid"));
                jsonGraph.put("userID", (String) graphIdMap.get("userid"));
                jsonGraph.put("title", (String) graphIdMap.get("title"));
                jsonGraph.put("description", (String) graphIdMap.get("description"));
                jsonGraph.put("timest", (String) graphIdMap.get("timest"));
                jsonGraph.put("isshared", (String) graphIdMap.get("isshared"));
                jsonGraph.put("parentgraphid", (String) graphIdMap.get("parentgraphid"));
                if(mode != 0) jsonGraph.put("authorityType", (String) graphIdMap.get("authoritytype"));
                jsonAnalysesMetaArray.add(jsonGraph);                
            }
            return jsonAnalysesMetaArray.toJSONString();
        }
        else return null;
    }

    /**
     * Return metadata for an analysis as JSON, based on a graph id.
     * @param graphID The graph id
     * @return The analysis metadata as a JSON formatted string
     */
    public String getAnalysisMeta(String graphID) {
        System.out.println("Fetching Analysis metadata for graphid: "+graphID);
        String getMetaSql = "SELECT * FROM CISPACES_GRAPH WHERE GRAPHID = '" + graphID + "'";
        ArrayList<HashMap<String, Object>> graphMeta = dbcn.execSQL(getMetaSql);
        if(!graphMeta.isEmpty()) {
            JSONObject jsonGraph = new JSONObject();
            jsonGraph.put("graphID", (String) graphMeta.get(0).get("graphid"));
            jsonGraph.put("userID", (String) graphMeta.get(0).get("userid"));
            jsonGraph.put("title", (String) graphMeta.get(0).get("title"));
            jsonGraph.put("description", (String) graphMeta.get(0).get("description"));
            jsonGraph.put("timest", (String) graphMeta.get(0).get("timest"));
            jsonGraph.put("isshared", (String) graphMeta.get(0).get("isshared"));
            jsonGraph.put("parentgraphid", (String) graphMeta.get(0).get("parentgraphid"));  
            return jsonGraph.toString();
        }
        else {
            return null; //Not found
        }
    }//getAnalysis
    
    /**
     * Return an analysis as JSON, based on a graph id.
     * @param graphID The graph id
     * @return The analysis as a JSON formatted string
     */
    public String getAnalysis(String graphID, String userID) {
        System.out.println("Fetching Analysis for graphid: "+graphID);
        String getMetaSql = "SELECT * FROM CISPACES_GRAPH WHERE GRAPHID = '" + graphID + "'";
        String getGraphProv = "SELECT * FROM CISPACES_GRAPH_PROV WHERE GRAPHID = '" + graphID + "'";
        String getNodesSql = "SELECT * FROM CISPACES_NODE WHERE GRAPHID = '" + graphID + "'";
        String getNodesProvSql = "SELECT NP.* FROM CISPACES_NODE_PROV NP, CISPACES_NODE N WHERE NP.NODEID = N.NODEID AND N.GRAPHID = '" + graphID + "'";
        String getEdgesSql = "SELECT * FROM CISPACES_EDGE WHERE GRAPHID = '" + graphID + "'";
        String getEdgesProvSql = "SELECT EP.* FROM CISPACES_EDGE_PROV EP, CISPACES_EDGE E WHERE EP.EDGEID = E.EDGEID AND E.GRAPHID = '" + graphID + "'";
        String getAuthoritySql = "SELECT authoritytype FROM CISPACES_GRAPH_AUTHORITY WHERE GRAPHID = '" + graphID + "' AND USERID = '" + userID + "'";
        
        ArrayList<HashMap<String, Object>> graphMeta = dbcn.execSQL(getMetaSql);
        ArrayList<HashMap<String, Object>> graphProv = dbcn.execSQL(getGraphProv);
        ArrayList<HashMap<String, Object>> authority = dbcn.execSQL(getAuthoritySql);
        if(!graphMeta.isEmpty()&&!authority.isEmpty()&&!graphProv.isEmpty()) {
            JSONObject jsonGraph = new JSONObject();
            jsonGraph.put("graphID", (String) graphMeta.get(0).get("graphid"));
            jsonGraph.put("userID", (String) graphMeta.get(0).get("userid"));
            jsonGraph.put("title", (String) graphMeta.get(0).get("title"));
            jsonGraph.put("description", (String) graphMeta.get(0).get("description"));
            jsonGraph.put("timest", (String) graphMeta.get(0).get("timest"));
            jsonGraph.put("isshared", (String) graphMeta.get(0).get("isshared"));
            jsonGraph.put("parentgraphid", (String) graphMeta.get(0).get("parentgraphid"));            
            jsonGraph.put("authorityType", (String) authority.get(0).get("authoritytype"));
            
            JSONObject Prov = new JSONObject();
            Prov.put("graphID", (String) graphProv.get(0).get("graphid"));
            Prov.put("parentgraphid", (String) graphProv.get(0).get("parentgraphid"));
            Prov.put("originalgraphid", (String) graphProv.get(0).get("originalgraphid"));
            Prov.put("type", (String) graphProv.get(0).get("type"));
            jsonGraph.put("prov", Prov);
            
            ArrayList<HashMap<String, Object>> resultNodes = dbcn.execSQL(getNodesSql);
            ArrayList<HashMap<String, Object>> provNodes = dbcn.execSQL(getNodesProvSql);
            JSONArray jsonNodesArray = getResultListJSON(resultNodes);   
            JSONArray jsonNodesProvArray = getResultListJSON(provNodes); 
            jsonGraph.put("nodes", jsonNodesArray);  
            jsonGraph.put("nodes_prov", jsonNodesProvArray); 
            
            ArrayList<HashMap<String, Object>> resultEdges = dbcn.execSQL(getEdgesSql);
            ArrayList<HashMap<String, Object>> provEdges = dbcn.execSQL(getEdgesProvSql);
            JSONArray jsonEdgesArray = getResultListJSON(resultEdges);
            JSONArray jsoEdgesProvArray = getResultListJSON(provEdges);  
            jsonGraph.put("edges", jsonEdgesArray);
            jsonGraph.put("edges_prov", jsoEdgesProvArray);

            return jsonGraph.toString();
        }
        else {
            return null; //Not found
        }
    }//getAnalysis     
    
    public String getNodesEdges(String graphID){
        String n_sql = "SELECT NODEID FROM CISPACES_NODE WHERE GRAPHID = '" + graphID + "'";
        String e_sql = "SELECT EDGEID FROM CISPACES_EDGE WHERE GRAPHID = '" + graphID + "'";
        ArrayList<HashMap<String,Object>> nodes = dbcn.execSQL(n_sql);
        ArrayList<HashMap<String,Object>> edges = dbcn.execSQL(e_sql);
        if(!nodes.isEmpty() || !edges.isEmpty()){
            JSONObject List = new JSONObject();
            for(HashMap node : nodes){
                String nodeid = (String) node.get("nodeid");
                List.put(nodeid, 1);
            }
            for(HashMap edge : edges){
                String edgeid = (String) edge.get("edgeid");
                List.put(edgeid, 1);
            }
            return List.toString();
        }
        else return null;
    }

    /*A number of queries used to store an analysis in the database.
    * Retrieves all nodes and edges connected to a graph by its graph id.
    * Constructs a json object and populates it with the JSON arrays of the nodes and edges.
    * Timestamps the event and inserts the data into the database.
    */
     public String saveLatestAnalysis(String graphID, String userID, String title) {
        String getNodesSql = "SELECT * FROM CISPACES_NODE WHERE GRAPHID = " +  "'" + graphID + "'";
        String getEdgesSql = "SELECT * FROM CISPACES_EDGE WHERE GRAPHID = " +  "'" + graphID + "'";

        JSONObject jsonGraph = new JSONObject();
        jsonGraph.put("graphID",graphID);
        ArrayList<HashMap<String,Object>> resultNodes = dbcn.execSQL(getNodesSql);
        ArrayList<HashMap<String,Object>> resultEdges = dbcn.execSQL(getEdgesSql);

        JSONArray jsonNodesArray =  getResultListJSON(resultNodes);
        jsonGraph.put("nodes",jsonNodesArray);


        JSONArray jsonEdgesArray = getResultListJSON(resultEdges);
        jsonGraph.put("edges",jsonEdgesArray);

        Date now = new Date();
        TimeHelper timeHelper = new TimeHelper();
        String json =  jsonGraph.toString().replace("nodeid", "nodeID");
        json = json.replace("N\\/A", "N/A");
        String snapID = UUID.randomUUID().toString();
        Timestamp timestamp = timeHelper.formatDateObjectCIS(now);

        String saveGraphQuery = "INSERT INTO CISPACES_GRAPH_HISTORY(snapid, graphid, userid, timest, analysis, title) VALUES "
                + "( '" + snapID + "' ,"
                + " '" + graphID + "' ,"
                + " '" + userID + "' ,"
                + " '" + timestamp + "' ,"
                + " '" + json + "' ,"
                + " '" + title + "'"
                + " )";
        boolean isExecuted = dbcn.updateSQL(saveGraphQuery);
        JSONObject result = new JSONObject();
        result.put("status",isExecuted);

        System.out.println("DBQuery.saveLatestAnalysis: "+saveGraphQuery);
        
        return result.toString();
    }

    /*helper method to iterate through the result set obtained after executing a query
    *the result sets are a list of hashmaps which have table column titles as keys and database entries as values
    */
    public JSONArray getResultListJSON(ArrayList<HashMap<String,Object>> resultEntities){
        JSONArray jsonArray = new JSONArray();
        for(HashMap<String,Object> edgesEntry : resultEntities){
            Iterator entryIterator = edgesEntry.entrySet().iterator();
            JSONObject obj = new JSONObject(); // the node or edge
            while (entryIterator.hasNext()) {
                //Each property of the node or edge
                Map.Entry pair = (Map.Entry)entryIterator.next();
                //these are reserved words in derby!!!                    
                if(pair.getKey().toString().equals("annot")){                        
                    // Expand the annot and set as a JSONArray                        
                    String annotString = pair.getValue().toString();
                    if(annotString.equals("{}")) continue; //don't care
                    annotString = annotString.replaceAll("\\{", "");
                    annotString = annotString.replaceAll("\\}", "");
                    String[] annotations = annotString.split(",");
                    //JSONArray jaAnnots = new JSONArray();
                    JSONObject joAnnots = new JSONObject();
                    for(String annotation : annotations) {
                        //Split into k,v
                        String[] annotParts = annotation.split("=");
                        String annotKey = annotParts[0];
                        String annotValue = annotParts[1];
                        //JSONObject joAnnot = new JSONObject();
                        joAnnots.put(annotKey, annotValue);
                        //jaAnnots.add(joAnnot);                           
                    }
                    //joAnnots.put("annot", jaAnnots);
                    //jsonArray.add(joAnnots);
                    obj.put("annot", joAnnots);
                } else {                    
                    //not an annot
                    if(pair.getKey().toString().equals("dtg")){
                        // Abuse of the Derby Timestamp field elsewhere can result in dates being stored as strings which are
                        // unparsable by getTimestamp().  So, getString() is used in in DBConnect.convertResultSetToList()
                        // The output can have a trailing .0 which ERS is not expecting and complains. Loudly.
                        // Here we remove the trailing badness.  This is a nasty cludge and does not make me proud.
                        if(pair.getValue().toString().endsWith(".0")) {
                            String badDate = pair.getValue().toString();
                            String goodDate = badDate.substring(0, badDate.length()-2);
                            obj.put("dtg", goodDate);
                        }
                        else obj.put("dtg", pair.getValue().toString());
                    }
                    else if(pair.getKey().toString().equals("inp")){
                        obj.put("input", pair.getValue().toString());
                    }else if(pair.getKey().toString().equals("txt")) {
                        obj.put("text", pair.getValue().toString());
                    }else if(pair.getKey().toString().equals("nodeid")){
                        obj.put("nodeID", pair.getValue().toString());
                    }else if(pair.getKey().toString().equals("edgeid")){
                        obj.put("edgeID", pair.getValue().toString());
                    }else if(pair.getKey().toString().equals("graphid")){
                        obj.put("graphID", pair.getValue().toString());                                  
                    }else {
                        obj.put(pair.getKey(), pair.getValue().toString());
                    }                    
                }//else                
            }//while each node property
            jsonArray.add(obj);
        }//for each node 

        return jsonArray;
    }

        //constructs a json object containing all saved variations of an analysis and returns it in a json string format
    public String getHistoryOfAnalysis(String graphID) {
        String sql = "SELECT * FROM CISPACES_GRAPH_HISTORY WHERE GRAPHID = " + "'" + graphID + "'" + " ORDER BY TIMEST";
        System.out.println(sql);
        ArrayList<HashMap<String,Object>> rs = dbcn.execSQL(sql);
        JSONObject jsonHistory = new JSONObject();
        if(!rs.isEmpty()){
            JSONArray jsonHistoryArray = getResultListJSON(rs);
            jsonHistory.put("history",jsonHistoryArray);

        }else{
            jsonHistory.put("history", new JSONArray());

        }

        return jsonHistory.toString();
    }

    public void updateAnalysis(HashMap analysis) {

       // dbcn.setAutoCommit(false);
        org.json.JSONObject obj = new org.json.JSONObject(analysis);

        String oldGraphID = obj.getString("graphID");
        System.out.println(oldGraphID + " is the graphID");
        String sql = "DELETE FROM CISPACES_NODE WHERE GRAPHID = " + "'" + oldGraphID + "'";
        String sql2 = "DELETE FROM CISPACES_EDGE WHERE GRAPHID = " + "'" + oldGraphID + "'";
        System.out.println(sql);
        dbcn.updateSQL(sql);
        dbcn.updateSQL(sql2);
        dbcn.tryConnect();
            org.json.JSONArray nodes = obj.getJSONArray("nodes");
            //iterate through nodes
            for (int i = 0; i < nodes.length(); i++) {
                String input = tryToGet(nodes.getJSONObject(i), "input");
                String eval = tryToGet(nodes.getJSONObject(i), "eval");
                String source = tryToGet(nodes.getJSONObject(i), "source");
                String uncert = tryToGet(nodes.getJSONObject(i), "uncert");
                String text = tryToGet(nodes.getJSONObject(i), "text");
                String dtg = tryToGet(nodes.getJSONObject(i), "dtg");
                String commit = tryToGet(nodes.getJSONObject(i), "commit");
                String type = tryToGet(nodes.getJSONObject(i), "type");
                String nodeID = tryToGet(nodes.getJSONObject(i), "nodeID");
                System.out.println(nodeID + " is the node id");
                String annot = tryToGet(nodes.getJSONObject(i), "annot");
                String graphID = tryToGet(nodes.getJSONObject(i), "graphID");
                String isLocked = tryToGet(nodes.getJSONObject(i), "islocked");

                String sql3 = "INSERT INTO CISPACES_NODE (nodeid, source, uncert, eval, txt, inp, dtg, cmt, type, annot, graphid, islocked) VALUES "
                        + "( '" + nodeID + "' ,"
                        + " '" + source + "' ,"
                        + " '" + uncert + "' ,"
                        + " '" + eval + "' ,"
                        + " '" + text + "' ,"
                        + " '" + input + "' ,"
                        + " '" + dtg + "' ,"
                        + " '" + commit + "' ,"
                        + " '" + type + "' ,"
                        + " '" + annot + "' ,"
                        + " '" + graphID + "' ,"
                        + " '" + isLocked + "'"
                        + " )";
                dbcn.prepareInsertStatementInTransaction(sql3);
                System.out.println(sql3);
            }
            org.json.JSONArray edges = obj.getJSONArray("edges");
            for (int i = 0; i < edges.length(); i++) {
                String edgeid = tryToGet(edges.getJSONObject(i), "edgeID");
                String islocked = tryToGet(edges.getJSONObject(i), "islocked");
                String source = tryToGet(edges.getJSONObject(i), "source");
                String formedgeid = tryToGet(edges.getJSONObject(i), "formedgeid");
                String graphID = tryToGet(edges.getJSONObject(i), "graphID");
                String target = tryToGet(edges.getJSONObject(i), "target");

                String sql4 = "INSERT INTO CISPACES_EDGE (edgeID, target, source, formEdgeID, graphID, islocked) VALUES "
                        + "( '" + edgeid + "' ,"
                        + " '" + target + "' ,"
                        + " '" + source + "' ,"
                        + " '" + formedgeid + "' ,"
                        + " '" + graphID + "' ,"
                        + " '" + islocked + "'"
                        + " )";
                dbcn.prepareInsertStatementInTransaction(sql4);
                System.out.println(sql4);
            }

            dbcn.commit();
    }

    public static String tryToGet(org.json.JSONObject jsonObj, String key) {
        if (jsonObj.has(key))
            return jsonObj.opt(key).toString();
        return null;
    }
    
}
