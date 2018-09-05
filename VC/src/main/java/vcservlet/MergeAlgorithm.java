/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vcservlet;

import database.DBQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class MergeAlgorithm {
    
    public MergeAlgorithm() {}
    
    public void Merge_Interrupted(){
        
    }  
    
    public HashMap setIndexTable(HashMap subGraphList){
        HashMap indexTable = new HashMap();
        String originalgraphid;
        ArrayList<String> originalgraphlist = new ArrayList<>();
        Iterator<HashMap<String,Object>> iter_s = subGraphList.entrySet().iterator();
        Iterator<HashMap<String,Object>> iter;
        while (iter_s.hasNext()){
            HashMap subgraph =(HashMap) ((Map.Entry)iter_s.next()).getValue();
            HashMap originalnodes = ((HashMap)subgraph.get("nodesMeta"));
            HashMap originaledges = ((HashMap)subgraph.get("edgesMeta"));
            HashMap nodes = new HashMap();
            HashMap edges = new HashMap();
            
            iter = originalnodes.entrySet().iterator();
            while(iter.hasNext()){
                HashMap node =(HashMap) ((Map.Entry)iter.next()).getValue();
                nodes.put(node.get("originalnodeid"), node.get("nodeid"));
            }
            iter = originaledges.entrySet().iterator();
            while(iter.hasNext()){
                HashMap edge = (HashMap) ((Map.Entry)iter.next()).getValue();
                edges.put(edge.get("originaledgeid"), edge.get("edgeid"));
            }
            originalgraphid = (String)subgraph.get("originalgraphid");
            if (!originalgraphlist.contains(originalgraphid)) originalgraphlist.add(originalgraphid);
            
            HashMap graphinfo = new HashMap();
            graphinfo.put("nodesMeta", nodes);
            graphinfo.put("edgesMeta", edges);
            indexTable.put(subgraph.get("graphID"), graphinfo);
        }
        indexTable.put("originalgraphlist", originalgraphlist);
        return indexTable;
    }
    
    public HashMap getMergeResult(HashMap originalGraphMeta, HashMap subgraph_indextable, String metaType){
        int flag_0 = 0; int flag_2 = 0;
        String type;
        if(metaType.equals("nodesMeta")) type = "nodeid";
        else type = "edgeid";
        HashMap<String, Integer> mergeResult_original = new HashMap<>();
        int subGraphsNum = subgraph_indextable.size();
        HashMap originalMeta = (HashMap)originalGraphMeta.get(metaType);
        Iterator<HashMap<String,Object>> iter_o = originalMeta.entrySet().iterator();
        while (iter_o.hasNext()) {
            String originalnodeid = (String)((HashMap)((Map.Entry)iter_o.next()).getValue()).get(type);
            Iterator<HashMap<String,Object>> iter_s = subgraph_indextable.entrySet().iterator();
            while (iter_s.hasNext()){
                HashMap subgraphMeta =(HashMap)((HashMap)((Map.Entry)iter_s.next()).getValue()).get(metaType); 
                if(subgraphMeta.containsKey(originalnodeid)) flag_2++;
                else flag_0++;
            }
            if(flag_2 == subGraphsNum) mergeResult_original.put(originalnodeid, 2);
            else if(flag_0 == subGraphsNum) mergeResult_original.put(originalnodeid, 0);
            else mergeResult_original.put(originalnodeid, 1);
            flag_2 = flag_0 = 0;
        }
        return mergeResult_original;
    }
    
    public String validationStep1(HashMap mergeResult1_nodes, HashMap mergeResult1_edges, HashMap subgraph_indextable, HashMap subGraphList){
        JSONObject validationResult = new JSONObject();
        boolean isvalid = true;
        Iterator<HashMap<String, Integer>> sub;
        
        Iterator<HashMap<String, Integer>> it= mergeResult1_nodes.entrySet().iterator();
        JSONObject incompatibleNodes = new JSONObject();
        while (it.hasNext()){
            Entry entry = (Map.Entry)it.next();
            int ismergable =(int)entry.getValue(); 
            if (ismergable == 1){
                isvalid = false;
                JSONObject incNode = new JSONObject();
                JSONArray deleted_n = new JSONArray();
                JSONArray reserved_n = new JSONArray();
                String nodeid = (String)entry.getKey();
                sub = subgraph_indextable.entrySet().iterator();
                while(sub.hasNext()){
                    Entry subgraph = (Map.Entry)sub.next();
                    HashMap subgraph_nodeslist = (HashMap)((HashMap)subgraph.getValue()).get("nodesMeta");
                    if(subgraph_nodeslist.containsKey(nodeid)){
                        JSONObject re_g = new JSONObject();
                        re_g.put("graphid", (String)subgraph.getKey());
                        re_g.put("nodeid", (String)subgraph_nodeslist.get(nodeid));
                        reserved_n.add(re_g);
                    }
                    else{
                        JSONObject de_g = new JSONObject();
                        de_g.put("graphid", (String)subgraph.getKey());
                        deleted_n.add(de_g);
                    }
                }
                incNode.put("deleted", deleted_n);
                incNode.put("reserved", reserved_n);
                incompatibleNodes.put((String)entry.getKey(), incNode);
            }
        }
        validationResult.put("incompatibleNodes", incompatibleNodes);
        
        it= mergeResult1_edges.entrySet().iterator();
        JSONObject incompatibleEdges = new JSONObject();
        while (it.hasNext()){
            Entry entry = (Map.Entry)it.next();
            int ismergable =(int)entry.getValue(); 
            if (ismergable == 1){
                isvalid = false;
                String edgeid = (String)entry.getKey();
                JSONObject incEdge = new JSONObject();
                JSONArray deleted_e = new JSONArray();
                JSONArray reserved_e = new JSONArray();
                sub = subgraph_indextable.entrySet().iterator();
                while(sub.hasNext()){
                    Entry subgraph = (Map.Entry)sub.next();
                    HashMap subgraph_edgeslist = (HashMap)((HashMap)subgraph.getValue()).get("edgesMeta");
                    if(subgraph_edgeslist.containsKey(edgeid)){
                        JSONObject re_g = new JSONObject();
                        re_g.put("graphid", (String)subgraph.getKey());
                        re_g.put("edgeid", (String)subgraph_edgeslist.get(edgeid));
                        reserved_e.add(re_g);
                    }
                    else{
                        JSONObject de_g = new JSONObject();
                        de_g.put("graphid", (String)subgraph.getKey());
                        deleted_e.add(de_g);
                    }
                }
                incEdge.put("deleted", deleted_e);
                incEdge.put("reserved", reserved_e);
                incompatibleEdges.put((String)entry.getKey(), incEdge);
            }
        }
        validationResult.put("incompatibleEdges", incompatibleEdges);
        
        if(isvalid) return null;
        else return validationResult.toString();
    }
    
    
    //mode=0:nodes; mode=1:edges
    public void saveTmpResult(JSONArray Result_Set, HashMap tmpResult_Set, HashMap GraphsMeta, String mode){
        Iterator<HashMap<String, Integer>> it= tmpResult_Set.entrySet().iterator();
        HashMap metaData = new HashMap();
        while (it.hasNext()) {
            Entry entry = (Map.Entry)it.next();
            if ((int)entry.getValue() == 2) {
                String id = (String)entry.getKey();
                metaData = (HashMap)GraphsMeta.get(mode);
                Result_Set.add(metaData.get(id));
            }
        }
    }
    
    public HashMap reconstructOriginalGraph(HashMap nodes_set, HashMap edges_set, HashMap originalGraphMeta){
        HashMap modifiedOriginalGraph = new HashMap();
        HashMap MetaNodes = new HashMap();
        HashMap MetaEdges = new HashMap();
        HashMap reservedNodes = new HashMap();
        HashMap deletedNodes = new HashMap();
        HashMap reservedEdges = new HashMap();
        HashMap deletedEdges = new HashMap();
        
        HashMap nodesMeta = (HashMap)originalGraphMeta.get("nodesMeta");
        HashMap edgesMeta = (HashMap)originalGraphMeta.get("edgesMeta");
        
        Iterator<HashMap<String, Object>> it= nodes_set.entrySet().iterator();
        while (it.hasNext()){
            Entry node = (Map.Entry)it.next();
            int ismergable =(int)node.getValue(); 
            String nodeid = (String)node.getKey();
            if (ismergable == 2) reservedNodes.put(nodeid, nodesMeta.get(nodeid));
            else if (ismergable == 0) deletedNodes.put(nodeid, nodesMeta.get(nodeid));
        }
        MetaNodes.put("reservedNodes", reservedNodes);
        MetaNodes.put("deletedNodes", deletedNodes);
        it= edges_set.entrySet().iterator();
        while (it.hasNext()){
            Entry edge = (Map.Entry)it.next();
            int ismergable =(int)edge.getValue(); 
            String edgeid = (String)edge.getKey();
            if (ismergable == 2) reservedEdges.put(edgeid, edgesMeta.get(edgeid));
            else if (ismergable == 0) deletedEdges.put(edgeid, edgesMeta.get(edgeid));
        }
        MetaEdges.put("reservedEdges", reservedEdges);
        MetaEdges.put("deletedEdges", deletedEdges);
        modifiedOriginalGraph.put("Nodes", MetaNodes);
        modifiedOriginalGraph.put("Edges", MetaEdges);
        return modifiedOriginalGraph;
    }
    
    public HashMap copySubGraphMeta(HashMap subGraphList){
        HashMap subGraphMeta = new HashMap();
        Iterator<HashMap<String, Object>> it= subGraphList.entrySet().iterator();
        while(it.hasNext()){
            Entry graph = (Map.Entry)it.next();
            String graphid = (String)graph.getKey();
            HashMap graphNodesMeta = (HashMap)((HashMap)graph.getValue()).get("nodesMeta");
            HashMap graphEdgesMeta = (HashMap)((HashMap)graph.getValue()).get("edgesMeta");
            HashMap graphMeta = new HashMap();
            HashMap nodesMeta = new HashMap();
            HashMap edgesMeta = new HashMap();
            
            Iterator<HashMap<String, Object>> nodes= graphNodesMeta.entrySet().iterator();
            while(nodes.hasNext()){
                Entry node = (Map.Entry)nodes.next();
                String nodeid = (String)node.getKey();
                HashMap nodeinfo = (HashMap)node.getValue();
                HashMap ninfo = new HashMap();
                ninfo.put("parentnodeid", (String)nodeinfo.get("parentnodeid"));
                ninfo.put("originalnodeid", (String)nodeinfo.get("originalnodeid"));
                ninfo.put("graphid", (String)nodeinfo.get("graphid"));
                nodesMeta.put(nodeid, ninfo);
            }
            graphMeta.put("nodesMeta", nodesMeta);
            Iterator<HashMap<String, Object>> edges= graphEdgesMeta.entrySet().iterator();
            while(edges.hasNext()){
                Entry edge = (Map.Entry)edges.next();
                String edgeid = (String)edge.getKey();
                HashMap edgeinfo = (HashMap)edge.getValue();
                HashMap einfo = new HashMap();
                einfo.put("parentedgeid", (String)edgeinfo.get("parentedgeid"));
                einfo.put("originaledgeid", (String)edgeinfo.get("originaledgeid"));
                einfo.put("source", (String)edgeinfo.get("source"));
                einfo.put("target", (String)edgeinfo.get("target"));
                einfo.put("graphid", (String)edgeinfo.get("graphid"));
                edgesMeta.put(edgeid, einfo);
            }
            graphMeta.put("edgesMeta", edgesMeta);
            subGraphMeta.put(graphid, graphMeta);
        }
        
        return subGraphMeta;
    }
    
    public void deletePath(String nodeid, HashMap nodes, HashMap edges){
        Iterator<HashMap<String, Object>> iter= edges.entrySet().iterator();
        HashSet ToBeDeleted = new HashSet();
        while(iter.hasNext()){
            Entry edge = (Map.Entry) iter.next();
            String edgeid = (String)edge.getKey();
            String source = (String)((HashMap)edge.getValue()).get("source");
            String target = (String)((HashMap)edge.getValue()).get("target");
            if (source.equals(nodeid)){            
                searchRelatedNode(ToBeDeleted, target, edgeid, nodes, edges, 1);
            }
            else if (target.equals(nodeid)){
                searchRelatedNode(ToBeDeleted, source, edgeid, nodes, edges, 0);
            }
        }
        
        Iterator<String> it = ToBeDeleted.iterator();
        while(it.hasNext()){
            String node = (String)it.next();
            it.remove();
            iter= edges.entrySet().iterator();
            while(iter.hasNext()){
                Entry edge = (Map.Entry) iter.next();
                String edgeid = (String)edge.getKey();
                String source = (String)((HashMap)edge.getValue()).get("source");
                String target = (String)((HashMap)edge.getValue()).get("target");
                if (source.equals(nodeid)||target.equals(nodeid)){            
                    iter.remove();
                }
            }
        }
    }
    
    public void searchRelatedNode(HashSet ToBeDeleted, String nodeid, String edgeid, HashMap nodes, HashMap edges, int direction){
        String type = "target";
        String othertype = "source";
        boolean hasOtherDirectionEdge = false;
        boolean hasNextEdge = false;
        if (direction == 1) {
            type = "source";
            othertype = "target";
        }
        HashMap nodeinfo = (HashMap)nodes.get(nodeid);
        String originalnodeid = (String)nodeinfo.get("originalnodeid");
        if(!ToBeDeleted.contains(nodeid)&&originalnodeid.equals("null")){
            ToBeDeleted.add(nodeid);
            Iterator<HashMap<String, Object>> iter= edges.entrySet().iterator();
            while(iter.hasNext()){
                Entry edge = (Map.Entry) iter.next();
                String edge_id = (String)edge.getKey();
                String edgeOattr = (String)((HashMap)edge.getValue()).get(othertype);
                if (edgeOattr.equals(nodeid)&&!edge_id.equals(edgeid)){            
                    hasOtherDirectionEdge = true;
                }  
            }
            if(!hasOtherDirectionEdge){
                iter= edges.entrySet().iterator();
                while(iter.hasNext()){
                    Entry edge = (Map.Entry) iter.next();
                    String edge_id = (String)edge.getKey();
                    String edgeattr = (String)((HashMap)edge.getValue()).get(type);
                    String edgeOattr = (String)((HashMap)edge.getValue()).get(othertype);
                    if (edgeattr.equals(nodeid)){            
                        searchRelatedNode(ToBeDeleted, edgeOattr, edge_id, nodes, edges, direction);
                        hasNextEdge = true;
                    }  
                }
            }
            if(!hasNextEdge||hasOtherDirectionEdge) {
                ToBeDeleted.remove(nodeid);
            }
        }
    }
    
    public void deleteSubGraphMeta(HashMap modifiedOriginalGraph, HashMap subGraphMeta){
        //删除每个子图中继承了原图中节点和边的节点和边
        HashMap deletedNodes = (HashMap)((HashMap)modifiedOriginalGraph.get("Nodes")).get("deletedNodes");
        HashMap deletedEdges = (HashMap)((HashMap)modifiedOriginalGraph.get("Edges")).get("deletedEdges");
        Iterator<HashMap<String, Object>> graphs= subGraphMeta.entrySet().iterator();
        while(graphs.hasNext()){
            Entry graph = (Map.Entry) graphs.next();
            HashMap graphNodes = (HashMap)((HashMap)graph.getValue()).get("nodesMeta");
            HashMap graphEdges = (HashMap)((HashMap)graph.getValue()).get("edgesMeta");
            Iterator<HashMap<String, Object>> edges= graphEdges.entrySet().iterator();
            while(edges.hasNext()){
                Entry edgeinfo = (Map.Entry) edges.next();
                String edgeid = (String)edgeinfo.getKey();
                String originaledgeid = (String)((HashMap)edgeinfo.getValue()).get("originaledgeid");
                if(deletedEdges.containsKey(originaledgeid)) edges.remove();
            }
            Iterator<HashMap<String, Object>> nodes= graphNodes.entrySet().iterator();
            while(nodes.hasNext()){
                Entry nodeinfo = (Map.Entry) nodes.next();
                String nodeid = (String)nodeinfo.getKey();
                String originalnodeid = (String)((HashMap)nodeinfo.getValue()).get("originalnodeid");
                if(deletedNodes.containsKey(originalnodeid)) {
                    nodes.remove();
                    deletePath(nodeid, graphNodes, graphEdges);
                }
            }
        }
    }
    
    public void saveSubGraphMeta(JSONArray Result_Nodes_Set, JSONArray Result_Edges_Set, HashMap modifiedOriginalGraph, HashMap subGraphMeta, HashMap subGraphList){
        HashMap reservedNodes = (HashMap)((HashMap)modifiedOriginalGraph.get("Nodes")).get("reservedNodes");
        HashMap reservedEdges = (HashMap)((HashMap)modifiedOriginalGraph.get("Edges")).get("reservedEdges");
        //删去每个子图中与原图对应的nodes和edges
        Iterator<HashMap<String, Object>> graphs= subGraphMeta.entrySet().iterator();
        while(graphs.hasNext()){
            Entry graph = (Map.Entry) graphs.next();
            String graphid = (String)graph.getKey();
            HashMap graphNodes = (HashMap)((HashMap)graph.getValue()).get("nodesMeta");
            HashMap graphEdges = (HashMap)((HashMap)graph.getValue()).get("edgesMeta");
            HashMap graphMeta = (HashMap)subGraphList.get(graphid);
            Iterator<HashMap<String, Object>> edges= graphEdges.entrySet().iterator();
            while(edges.hasNext()){
                Entry edgeinfo = (Map.Entry) edges.next();
                String edgeid = (String)edgeinfo.getKey();
                String originaledgeid = (String)((HashMap)edgeinfo.getValue()).get("originaledgeid");
                if(reservedEdges.containsKey(originaledgeid)) edges.remove();
            }
            Iterator<HashMap<String, Object>> nodes= graphNodes.entrySet().iterator();
            while(nodes.hasNext()){
                Entry nodeinfo = (Map.Entry) nodes.next();
                String nodeid = (String)nodeinfo.getKey();
                String originalnodeid = (String)((HashMap)nodeinfo.getValue()).get("originalnodeid");
                if(reservedNodes.containsKey(originalnodeid)) {
                    savePath(Result_Nodes_Set, Result_Edges_Set, nodeid, originalnodeid, graphNodes, graphEdges, graphMeta);
                }
            }
        }
    }
    
    public void savePath(JSONArray Result_Nodes_Set, JSONArray Result_Edges_Set, String nodeid, String originalnodeid, HashMap nodes, HashMap edges, HashMap graphMeta){
        HashSet ToBeSaved_nodes = new HashSet();
        HashSet ToBeSaved_edges = new HashSet();
        Iterator<HashMap<String, Object>> iter= edges.entrySet().iterator();
        while(iter.hasNext()){
            Entry edge = (Map.Entry) iter.next();
            String edgeid = (String)edge.getKey();
            String source = (String)((HashMap)edge.getValue()).get("source");
            String target = (String)((HashMap)edge.getValue()).get("target");
            if (source.equals(nodeid)){      
                iter.remove();
                HashMap edgeinfo = (HashMap)((HashMap)graphMeta.get("edgesMeta")).get(edgeid);
                edgeinfo.put("source", originalnodeid);
                String targetOriginal = (String)((HashMap)nodes.get(target)).get("originalnodeid");
                if(targetOriginal.equals("null")) {
                    ToBeSaved_nodes.add(target);
                }
                else edgeinfo.put("target", targetOriginal);
                ToBeSaved_edges.add(edgeid);
                saveRelatedEdge(ToBeSaved_nodes, ToBeSaved_edges, target, nodes, edges, 1, graphMeta);
            }
            else if (target.equals(nodeid)){
                iter.remove();
                HashMap edgeinfo = (HashMap)((HashMap)graphMeta.get("edgesMeta")).get(edgeid);
                edgeinfo.put("target", originalnodeid);
                String sourceOriginal = (String)((HashMap)nodes.get(source)).get("originalnodeid");
                if(sourceOriginal.equals("null")) {
                    ToBeSaved_nodes.add(source);
                }
                else edgeinfo.put("source", sourceOriginal);
                ToBeSaved_edges.add(edgeid);
                saveRelatedEdge(ToBeSaved_nodes, ToBeSaved_edges, source, nodes, edges, 0, graphMeta);
            }
        }
        
        Iterator<String> it = ToBeSaved_nodes.iterator();
        while(it.hasNext()){
            String node_id = (String)it.next();
            it.remove();
            HashMap nodeMeta = (HashMap)((HashMap)graphMeta.get("nodesMeta")).get(node_id);
            Result_Nodes_Set.add(nodeMeta);
        }
        it = ToBeSaved_edges.iterator();
        while(it.hasNext()){
            String edge_id = (String)it.next();
            it.remove();
            HashMap edgeMeta = (HashMap)((HashMap)graphMeta.get("edgesMeta")).get(edge_id);
            Result_Edges_Set.add(edgeMeta);
        }
    }
    
    public void saveRelatedEdge(HashSet ToBeSaved_nodes, HashSet ToBeSaved_edges, String nodeid, HashMap nodes, HashMap edges, int direction, HashMap graphMeta){
        String type = "target";
        String othertype = "source";
        boolean hasOtherDirectionEdge = false;
        boolean hasNextEdge = false;
        if (direction == 1) {
            type = "source";
            othertype = "target";
        }
        Iterator<HashMap<String, Object>> iter= edges.entrySet().iterator();
        while(iter.hasNext()){
            Entry edge = (Map.Entry) iter.next();
            String edgeOattr = (String)((HashMap)edge.getValue()).get(othertype);
            if (edgeOattr.equals(nodeid)){            
                hasOtherDirectionEdge = true;
            }  
        }
        iter= edges.entrySet().iterator();
        while(iter.hasNext()){
            Entry edge = (Map.Entry) iter.next();
            String edgeid = (String)edge.getKey();
            String edgeattr = (String)((HashMap)edge.getValue()).get(type);
            String edgeOattr = (String)((HashMap)edge.getValue()).get(othertype);
            if (edgeattr.equals(nodeid)){ 
                iter.remove();
                    
                String OattrOriginal = (String)((HashMap)nodes.get(othertype)).get("originalnodeid");
                if(OattrOriginal.equals("null")) {
                    ToBeSaved_nodes.add(edgeOattr);
                }
                else {
                    HashMap edgeinfo = (HashMap)((HashMap)graphMeta.get("edgesMeta")).get(edgeid);
                    edgeinfo.put(othertype, OattrOriginal);       
                }             
                ToBeSaved_edges.add(edgeid);
                if(!hasOtherDirectionEdge)
                    saveRelatedEdge(ToBeSaved_nodes, ToBeSaved_edges, edgeOattr, nodes, edges, 1, graphMeta);
                hasNextEdge = true;
            }  
        }
        if(!hasNextEdge) {
            ToBeSaved_nodes.add(nodeid);
        }
    }
    
    public JSONObject Merge_Case1(ArrayList<String> originalgraphlist, HashMap subGraphList, HashMap subgraph_indextable){
        DBQuery dbquery = new DBQuery();
        
        JSONObject Result = new JSONObject();
        JSONArray Result_Nodes = new JSONArray();
        JSONArray Result_Edges = new JSONArray();
        
        HashMap mergeResult_original_nodes = new HashMap();
        HashMap mergeResult_original_edges = new HashMap();
        HashMap modifiedOriginalGraph = new HashMap();
        HashMap subGraphMeta = new HashMap();
        //step1
        HashMap originalGraphData = dbquery.getOriginalAnalysis(originalgraphlist);
        HashMap originalGraphMeta =(HashMap)((Map.Entry)originalGraphData.entrySet().iterator().next()).getValue();
        mergeResult_original_nodes = getMergeResult(originalGraphMeta, subgraph_indextable, "nodesMeta");
        mergeResult_original_edges = getMergeResult(originalGraphMeta, subgraph_indextable, "edgesMeta");
        //validate step1 result
        String validationResult1 = validationStep1(mergeResult_original_nodes, mergeResult_original_edges, subgraph_indextable, subGraphList);
        if(validationResult1 != null){
            Merge_Interrupted();
            return null;
        }
        //将保留的node和edge存在Result_Nodes和Result_Edges中
        saveTmpResult(Result_Nodes, mergeResult_original_nodes, originalGraphMeta, "nodesMeta");
        saveTmpResult(Result_Edges, mergeResult_original_edges, originalGraphMeta, "edgesMeta");
        modifiedOriginalGraph = reconstructOriginalGraph(mergeResult_original_nodes, mergeResult_original_edges, originalGraphMeta);
        subGraphMeta = copySubGraphMeta(subGraphList);
        //step2: 删去各子图中与原图中被删去node和edge有关的node和edge
        deleteSubGraphMeta(modifiedOriginalGraph, subGraphMeta);
        
        //step3: 将个子图可兼容的node和edge添加到结果集中
        saveSubGraphMeta(Result_Nodes, Result_Edges, modifiedOriginalGraph, subGraphMeta, subGraphList);
        
        Result.put("nodes", Result_Nodes);
        Result.put("edges", Result_Edges);
        Result.put("originalgraphid", (String)originalGraphMeta.get("originalgraphid"));
        Result.put("parentgraphid", (String)originalGraphMeta.get("graphID"));
        Result.put("title", (String)originalGraphMeta.get("title"));
        Result.put("description", (String)originalGraphMeta.get("description"));
        return Result;
    }
    
    public String mergeGraph(String projectID){
        JSONObject Result = new JSONObject();
        if(projectID != null) {
            DBQuery dbquery = new DBQuery();
            HashMap subGraphList = new HashMap();
            HashMap subgraph_indextable = new HashMap();
            ArrayList<String> originalgraphlist = new ArrayList<>();
            
            //get subgraph metadata
            subGraphList = dbquery.getSubGraphs(projectID);
            subgraph_indextable = setIndexTable(subGraphList);
            originalgraphlist =(ArrayList) subgraph_indextable.get("originalgraphlist");
            subgraph_indextable.remove("originalgraphlist");
            //get originalgraph metadata
            if(originalgraphlist.isEmpty()) {return null;}    //无原图，返回错误情况１
            if(originalgraphlist.size() == 1)
                Result = Merge_Case1(originalgraphlist, subGraphList, subgraph_indextable);
            return Result.toString();
        }
        else return null;
    }
    
}
