var app = app || {};

/**
 * ProjectBox
 * ---------------------------------
 * the UI for 'projectBox'
 */
var projectID;
var userID;

app.MergedgraphBoxView = Backbone.View.extend({
    el: '#mergedgraph_box',
    
    events:{
        'click .btn-view': 'viewAnalysis',
        'click .btn-checkout': 'viewAnalysis',
        'click .btn-export': 'exportToFile',
        'click .btn-delete': 'deleteAnalysis',
        'click #mergeGraph': 'mergeGraph'
    },
    
    initialize: function(ownerID){
        projectID = readCookie('projectid');
        userID = readCookie('user_id');
        $("#row-mergedgraphbox").show();
        if(userID !== ownerID)
            $("#mergeGraphContainer").hide();
        this.getMergedGraphListByProjectID();
    },
    
    render: function() {},
    
    getMergedGraphListByProjectID: function() {
        $(".existing-analysis-mergedgraph_box").remove();
        
        Backbone.ajax({
            type: 'GET',
            url: remote_server + 'VC/rest/analyses/merged/'+ userID + "/" + projectID,
            success: function(data) {
                if(data){
                    data.forEach(function(analysis) {
                        app.browseBoxView.makeGraphElement(analysis, "mergedgraph_box",  analysis.authorityType);
                    });
                }
            },
            error: function(xhr) {
                console.error("Ajax failed: " + xhr.statusText);
            }
        });
    },
    
    viewAnalysis: function(event) {
        $("#projectDetail").show();
        app.browseBoxView.viewAnalysis(event);
    },
    
    reconstructMetaData: function(newMergedGraph){
        var newGraphID = generateUUID();
        var timest = generateDate();
        newMergedGraph.graphID = newGraphID;
        newMergedGraph.timest = timest;
        newMergedGraph.title = newMergedGraph["title"] + "version: " + timest;
        newMergedGraph.userID = userID;
        
        var nodes_prov = [];
        var edges_prov = [];
        var map = {};
        newMergedGraph.nodes.forEach(function(n){
            var nodeID = generateUUID();
            map[n.nodeid] = nodeID;
            n.input = n.inp; n.text = n.txt; n.nodeID = nodeID; n.graphID = newGraphID;
            var prov = {
                "nodeID": nodeID,
                "parentnodeid": n.nodeid,
                "originalnodeid": nodeID,
                "graphID": newGraphID
            };
            nodes_prov.push(prov);
            delete n.graphid; delete n.annot; delete n.inp; delete n.ismergable; delete n.nodeid; delete n.txt;
            delete n.originalnodeid; delete n.parentnodeid;
        });   
        newMergedGraph.edges.forEach(function(e){
            var edgeID = generateUUID();
            e.edgeID = edgeID; e.graphID = newGraphID; e.formedgeid = e.edgeid;
            e.source = map[e.source]; e.target = map[e.target];
            var prov = {
                "edgeID": edgeID,
                "parentedgeid":e.edgeid,
                "originaledgeid": edgeID,
                "graphID": newGraphID
            };
            edges_prov.push(prov);
            delete e.graphid; delete e.edgeid; delete e.ismergable;
            delete e.originaledgeid; delete e.parentedgeid;
        });
        
        newMergedGraph.nodes_prov = nodes_prov;
        newMergedGraph.edges_prov = edges_prov;
    },
    
    mergeGraph: function(event) {
        var self = this;
        Backbone.ajax({
            type: 'GET',
            url: remote_server + 'VC/rest/project/' + projectID + '/merge' ,
            success: function(newMergedGraph){
                self.reconstructMetaData(newMergedGraph);
                //生成新的图
                var object_graphdata = {
                    "graphID": newMergedGraph.graphID,
                    "userID": userID,
                    "timest": newMergedGraph.timest,
                    "title": newMergedGraph.title,
                    "description": newMergedGraph.description,
                    "isshared": "false",
                    "parentgraphid": newMergedGraph.parentgraphid,
                    "projectID": projectID 
                };
                var object_graph_prov = {
                    "graphID": newMergedGraph.graphID,
                    "parentgraphid": newMergedGraph.parentgraphid,
                    "originalgraphid": newMergedGraph.originalgraphid,
                    "type": "1"
                };

                Backbone.ajax({
                    type: 'POST',
                    url: remote_server + '/VC/rest/new',
                    contentType: 'application/json',
                    data: JSON.stringify(object_graphdata),
                    success: function(result) {
                        // initialises a workbox
                        $("#row-workbox").show();
                        app.workBoxView.clearWorkBox();
                        $("#row-workbox").hide();
                        // saves the meta data of the graph
                        var nodes = newMergedGraph['nodes'];
                        var edges = newMergedGraph['edges'];
                        var nodes_prov = newMergedGraph['nodes_prov'];
                        var edges_prov = newMergedGraph['edges_prov'];
                        var ret_graph = draw(nodes, nodes_prov, edges, edges_prov, chart);
                        push_graph_data(ret_graph);
                    },
                    error: function(xhr) {
                        console.error("Ajax failed: " + xhr.statusText);
                        alert('Something went wrong. Please try again.');
                    }
                });
                app.browseBoxView.createNewGraphFromData(object_graphdata, object_graph_prov, 1);
                app.browseBoxView.makeGraphElement(object_graphdata, "mergedgraph_box");
                app.browseBoxView.makeGraphElement(object_graphdata, "browse_box_own");
            },
            error: function(){
                console.error("Ajax failed: " + xhr.statusText);
                alert('An error occurred fetching data.');
            }
        });
    }
});


