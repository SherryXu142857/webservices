/**
 * This class is used to process the JSON coming from the front-end.
 * It puts the JSON strings into hashmaps. Then it passes the hashmap to class VControl which extracts the bits to be put in the database.
 *
 * @author Yordanka Ivanova
 * @since July 2017
 */
package vcservlet;

import database.DBQuery;
import utils.JsonHelper;
import vcontrol.VControl;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.*;

public class VCForkControl {

    //a logger object which prints to stdout
    Logger log;

    //a helper object which is used to convert json to hashmaps
    //@see class JsonHelper in package utils
    private JsonHelper jsh;

    public VCForkControl(){
        jsh = new JsonHelper();
        log = Logger.getLogger(getClass().getName());
    }

    /**
     * @param jsonInput the json for an edge coming from the front-end
     * @return a string which indicates whether the conversion to hashmap has succeeded
     */
    public String evalJSONEdge(String jsonInput) {
        log.log(Level.INFO, "*** VERSION CONTROL SERVICE - SPLIT JSON EDGE***");

        jsonInput = jsonInput.replaceAll("\"\"", "null");
        HashMap map = jsh.convertInputMap(jsonInput);

        VControl vc = new VControl();
        map = vc.onAddEdges(map);

        String response = jsh.convertInputJson(map);
        return response;
    }
    
    public String evalJSONEdgeProv(String jsonInput) {
        log.log(Level.INFO, "*** VERSION CONTROL SERVICE - SPLIT JSON EDGE PROVENANCE RECORD***");

        jsonInput = jsonInput.replaceAll("\"\"", "null");
        HashMap map = jsh.convertInputMap(jsonInput);

        VControl vc = new VControl();
        map = vc.onAddEdgesProv(map);

        String response = jsh.convertInputJson(map);
        return response;
    }

    /**
     * @param jsonInput the json for a node coming from the front-end
     * @return a string which indicates whether the conversion to hashmap has succeeded
     */
    public String evalJSONNode(String jsonInput) {
        log.log(Level.INFO, "*** VERSION CONTROL SERVICE - SPLIT JSON NODE");

        jsonInput = jsonInput.replaceAll("\"\"", "null");
        HashMap map = jsh.convertInputMap(jsonInput);

        VControl vc = new VControl();
        map = vc.onAddNodes(map);

        String response = jsh.convertInputJson(map);
        return response;
    }
    
    public String evalJSONNodeProv(String jsonInput) {
        log.log(Level.INFO, "*** VERSION CONTROL SERVICE - SPLIT JSON NODE PROVENANCE RECORD");

        jsonInput = jsonInput.replaceAll("\"\"", "null");
        HashMap map = jsh.convertInputMap(jsonInput);

        VControl vc = new VControl();
        map = vc.onAddNodesProv(map);

        String response = jsh.convertInputJson(map);
        return response;
    }

    /**
     * @param graph the json for a graph coming from the client
     */
    public void evalJSONGraph(String graph) {
        log.log(Level.INFO, "*** VERSION CONTROL SERVICE - SPLIT JSON GRAPH");

        graph = graph.replaceAll("\"\"", "null");
        HashMap map = jsh.convertInputMap(graph);

        VControl vc = new VControl();
        vc.onAddGraph(map);

        log.log(Level.INFO, "*** VERSION CONTROL SERVICE - NEW GRAPH ADDED SUCCESSFULLY;");
    }

    public void evalJSONProject(String project) {
        log.log(Level.INFO, "*** VERSION CONTROL SERVICE - SPLIT JSON PROJECT");

        project = project.replaceAll("\"\"", "null");
        HashMap map = jsh.convertInputMap(project);

        VControl vc = new VControl();
        vc.onAddProject(map);

        log.log(Level.INFO, "*** VERSION CONTROL SERVICE - NEW PROJECT ADDED SUCCESSFULLY;");
    }
    
    public void evalJSONGraphProv(String graphProvInfo, int type) {
        log.log(Level.INFO, "*** VERSION CONTROL SERVICE - SPLIT JSON GRAPHPROV");

        graphProvInfo = graphProvInfo.replaceAll("\"\"", "null");
        HashMap map = jsh.convertInputMap(graphProvInfo);

        VControl vc = new VControl();
        vc.onAddGraphProv(map, type);

        log.log(Level.INFO, "*** VERSION CONTROL SERVICE - NEW GRAPHPROV ADDED SUCCESSFULLY;");
    }
    
    /**
     * @param credentials the user credentials in a json string format
     * @return a string indicating whether the user has been validated successfully
     */
    public String evalJSONLoginUser(String credentials) {

        credentials = credentials.replaceAll("\"\"", "null");
        HashMap map = jsh.convertInputMap(credentials);
        VControl vc = new VControl();
        String result = vc.onLoginUser(map);
        return result;
    }
    
   
    public String evalJSONAddUser(String userData) {
        userData = userData.replaceAll("\"\"", "null");
        HashMap map = jsh.convertInputMap(userData);
        VControl vc = new VControl();
        String result = vc.onAddUser(map);
        return result;        
    }

    /**
     * @param graphIDTitleJSON a json containing the information of a graph which will be saved into the database
     * @return a string indicating whether the graph has been saved successfully
     */
    public String evalJSONSaveAnalysis(String graphIDTitleJSON) {
        log.log(Level.INFO, "*** VERSION CONTROL SERVICE - SPLIT JSON ON SAVE ANALYSIS");
        graphIDTitleJSON = graphIDTitleJSON.replaceAll("\"\"", "null");
        HashMap map = jsh.convertInputMap(graphIDTitleJSON);

        VControl vControl = new VControl();
        String result = vControl.onSaveAnalysis(map);

        return result;
    }


    public void evalJSONUpdateAnalysis(String analysis){
        HashMap map = jsh.convertInputMap(analysis);
        DBQuery dbQuery = new DBQuery();
        dbQuery.updateAnalysis(map);

    }
}
