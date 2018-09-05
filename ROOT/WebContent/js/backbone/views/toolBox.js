// js/views/toolBox.js

var app = app || {};

/**
 * ToolBox
 * ---------------------------------
 * the UI for 'toolBox'
 */
var userID;
var projectID;

app.ToolBoxView = Backbone.View.extend({
  el: '#nav-toolbox',

  events: {
    'click #settings': 'settings',
    'click #help': 'help',

    // 'click #newWorkBox': 'newWorkBox',
    // browseAnalyses: 'Browse'按钮
    // saveProgree: 'Save'按钮
    // simulation: 'Relocate'按钮
    // checkoutGraph: 'Check-out'按钮
    // commitGraph: 'Commit'按钮
    'click #browseAnalyses': 'callBrowseBox',
    'click #saveProgress': 'save',
    'click #history': 'analysisHistory',
    'click #simulation': 'restartSimulation',

    // 'click .btn-sm': 'createNode',
    'dragstart .btn-sm': 'dataTransfer',
    'dragend .btn-sm': 'createNode',

    'click #commitGraph': 'commitGraph',
    'click #updateGraph': 'updateGraph',
    'click #checkoutGraph': 'checkoutGraph',
    'click #projectList': 'projectList',
    'click #projectDetail': 'projectDetail'
  },

  initialize: function() {
    userID = readCookie('user_id');
    projectID = readCookie('projectid');
    
    this.$el.hide();
    $("#info").hide();
    $("#claim").hide();
    $("#pref").hide();
    $("#con").hide();
    $("#pro").hide();
    $("#delete-node").addClass("disabled");
    $("#link-from").addClass("disabled");
    $("#link-to").addClass("disabled");
    $("#cancel-link").addClass("disabled");
    $("#commitGraph").hide();
    $("#checkoutGraph").hide();
    $("#saveProgress").hide();
    $("#simulation").hide();
    $("#updateGraph").hide();
    $("#browseAnalyses").hide();
    $("#projectDetail").hide();
  },

  render: function() {

  },

  settings: function() {
    // alert('settings');
  },

  help: function() {
    // alert('help');
  },

  changeMode: function(){
      $("#info").hide();
      $("#claim").hide();
      $("#pref").hide();
      $("#con").hide();
      $("#pro").hide();
      
      $("#commitGraph").hide();
      $("#updateGraph").hide();
      $("#checkoutGraph").hide();
      $("#saveProgress").hide();
      $("#browseAnalyses").hide();
      $("#simulation").hide();

      $("#delete-node").addClass("disabled");
      $("#link-from").addClass("disabled");
      $("#link-to").addClass("disabled");
      $("#cancel-link").addClass("disabled");
  },
  
  callBrowseBox: function(obj){

    // Gets the list of analysis from the server
    app.browseBoxView.getAnalysisList();
    
    //this.$el.hide();
    this.changeMode();
    
    $("#row-workbox").hide();
    $("#row-browsebox").show();

    $("#row-contributorbox").show();
    $("#row-mergedgraphbox").show();
  },

  createNode: function(obj) {
    var id = obj.currentTarget.id;

    // creates model of the node
    var attr = app.workBoxView.createNewNode(id);
    var prov = app.workBoxView.createNewNodeProv(attr.nodeID);

    var restart = true;
    if (!chart.nodes || chart.nodes.length < 1)
      restart = false;

    var dot, eventDoc, doc, body, pageX, pageY;
    var ev = obj.originalEvent || window.event;

    if(ev.pageX == null && ev.clientX != null){
      eventDoc = (ev.target && ev.target.ownerDocument) || document;
      doc = eventDoc.documentElement;
      body = eventDoc.body;

      ev.pageX = ev.clientX + (doc && doc.scrollLeft || body && body.scrollLeft || 0) - (doc && doc.clientLeft || body && body.scrollLeft);
      ev.pageY = ev.clientY + (doc && doc.scrollTop || body && body.scrollTop || 0) - (doc && doc.clientTop || body && body.clientTop);
    }

    var x = ev.pageX;
    var y = ev.pageY;

    // draws a new node
    chart.node = addNewNode(attr, prov, x, y);

    // re-start changed graph
    chart.simulation = restart_simulation(chart.simulation, restart);
  },

  save: function() {
    $("#graph_info .modal-header span").text(chart.graphID);

    $("#graph_info .modal-body input").val(chart.title);
    $("#graph_info .modal-body textarea").val(chart.description);

    $("#graph_info .modal-footer .btn-create")
    .text("Save")
    .one("click", function(event){
        //We need to use one(), rather than on(), else the click event is associated multiple times.
      event.preventDefault();
      var title = $("#graph_info .modal-body input").val();
      var description = $("#graph_info .modal-body textarea").val();

      if (title != null) {
        var object = {
          "graphID": chart.graphID,
          "userID": userID,
          "title": title.trim(),
          "description": description.trim()
        };

        Backbone.ajax({
          type: 'POST',
          url: remote_server + '/VC/rest/save',
          //dataType: 'text',
          contentType: 'application/json',
          data: JSON.stringify(object),
          success: function(result) {
            alert("Version " + title + " saved.");

            $("#graph_info").modal('hide');
          },
          error: function(result) {
            alert('Something went wrong. Please try again.');
          }
        });
      }
    });

    $("#graph_info").modal('show');
  },

  analysisHistory: function() {
    var object = {
      "graphID": chart.graphID
    };

    Backbone.ajax({
      type: 'GET',
      url: remote_server + '/VC/rest/history',
      contentType: 'application/json',
      data: JSON.parse(object),
      success: function(result) {
        if (result) {
          $("#history_list").html("");

          if (result.history) {
            var arr = result.history;
            arr.forEach(function(d) {

              var options = $("<label></label>", {
                "class": "list-group-item"
              }).appendTo($("#history_list"));

              var radio_btn = $("<input/>", {
                "type": "radio",
                "name": "history_options_radio",
                "id": d.title
              }).click(function(obj) {
                //display graph upon clicking on a radio button
                if (d.analysis) {
                  $("#selectedAnalysis").text(d.analysis);
                }
              }).appendTo(options);

              var span = $("<span></span>", {
                "text": d.title + " Created on " + d.timest,
                "class": "history-text"
              }).appendTo(options);
            });
          }
          $("#history_result").modal('show');
        }
      },
      error: function(xhr, textStatus, errorThrown) {
        alert("Ajax failed: " + errorThrown);
      }
    });
  },

  importAnalysis: function() {
    var analysis = document.getElementById('selectedAnalysis').innerHTML;
    if (analysis) {
      var json = JSON.parse(analysis);
      var object = {
        "graphID": json.graphID,
        "nodes": json.nodes,
        "edges": json.edges
      };
      Backbone.ajax({
        type: 'POST',
        url: remote_server + '/VC/rest/updateAnalysis',
        contentType: 'application/json',
        data: JSON.stringify(object),
        success: function(result) {
          alert('success');
        },
        error: function(result) {
          alert('Error in the ajax request.');
          //callback(result);
        }
      });

      app.workBoxView.clearWorkBox();

      var ret_graph = draw(json.nodes, json.edges, chart);
      push_graph_data(ret_graph);

      chart.simulation = restart_simulation(chart.simulation, false);

      $('#history_result').modal('hide');
    }

  },

  restartSimulation: function() {

    var length = (chart.nodes) ? chart.nodes.length : 15;

    var ret_simulation = set_simulation(length, chart.svg.width, chart.svg.height);
    push_node_style_data(ret_simulation);

    chart.simulation = restart_simulation(ret_simulation.simulation, false);

    if (chart.nodes) {
      chart.nodes.forEach(function(d) {
        d.fx = null;
        d.fy = null;
      })
    }
  },

  dataTransfer: function(event){
    event.originalEvent.dataTransfer.setData('text/plain', null);
  },
  
  getOriginalMeta: function(graphID, callback){
      Backbone.ajax({
          type: 'GET',
          url: remote_server + '/VC/rest/analysis/' + graphID + '/nodes',
          success: function(response, status, xhr){
              callback(response, status, xhr);
          },
          error: function(xhr) {
              console.error("Ajax failed: " + xhr.statusText);
          }
      });
  },

  commitGraph: function(event){
    app.browseBoxView.toggleViewMode(true);
    var graphID = chart.graphID;
    if (projectID === null) alert("Something went wrong when fetching projectID! ");
    //graphdata:本地保存的graph信息,JSON类型
    //previousGraphData: 服务器端保存的graph信息
    var graphdata = app.browseBoxView.getInfoFromLocalStorage(graphID);
    this.getOriginalMeta(graphID, function(previousGraphMeta, status){
        //数据库中已有关于该graph的node
    if (status === "success"){
        //生成一个新的graph,并将Nodes和Edeges与这个graph关联起来
        var newGraphID = generateUUID();
        var time = generateDate();
        var object_graphdata = {
            "graphID": newGraphID,
            "userID": userID,
            "timest": time,
            "title": graphdata["title"] + time,
            "description": graphdata["description"],
            "isshared": "false",
            "parentgraphid": graphID,
            "projectID": projectID 
        };
        var object_graph_prov = {
            "graphID": newGraphID,    
            "parentgraphid": graphID,
            "originalgraphid": graphdata["originalgraphid"],
            "type": "0"
        };
        
        Backbone.ajax({
            type: 'POST',
            url: remote_server + '/VC/rest/new',
            contentType: 'application/json', 
            data: JSON.stringify(object_graphdata),
            success: function(){
                Backbone.ajax({
                    type: 'POST',
                    url: remote_server + '/VC/rest/graphprov/save/' + 0,
                    contentType: 'application/json',
                    data: JSON.stringify(object_graph_prov),
                    success: function(){
                        if (!$.isEmptyObject(graphdata)){
                        graphdata.nodes.forEach(function(n){
                            var key = n.nodeID; 
                            var n_p = JSON.parse(window.localStorage.getItem("prov-"+key));
                            window.localStorage.removeItem(key);
                            window.localStorage.removeItem("prov-"+key); 
                            if (previousGraphMeta.hasOwnProperty(key)){
                                var newnodeid = generateUUID();
                                n_p.parentnodeid = n.nodeID;
                                n.nodeID = newnodeid;
                                n_p.nodeID = newnodeid;
                                previousGraphMeta[key] = n.nodeID;
                            }
                            n.graphID = newGraphID;
                            n_p.graphID = newGraphID;
                            var orz = app.workBoxView.createNodeModelFromData(n);
                            app.workBoxView.createNodeProvModelFromData(n_p);
                        });
                        graphdata.edges.forEach(function(e){
                            var e_p = JSON.parse(window.localStorage.getItem("prov-"+e.edgeID));
                            window.localStorage.removeItem(e.edgeID);
                            window.localStorage.removeItem("prov-"+e.edgeID);
                            e.graphID = newGraphID;
                            e_p.graphID = newGraphID;
                            if(previousGraphMeta.hasOwnProperty(e.edgeID)){
                                e.formedgeid = e.edgeID;
                                e_p.parentedgeid = e.edgeID;
                                e.edgeID = generateUUID();
                                e_p.edgeID = e.edgeID;
                            }
                            var previoussource = e.source;
                            var previoustarget = e.target;
                            if(previousGraphMeta.hasOwnProperty(e.source))
                                e.source = previousGraphMeta[e.source];
                            if(previousGraphMeta.hasOwnProperty(e.target))
                                e.target = previousGraphMeta[e.target];
                            var orz = app.workBoxView.createEdgeModelFromData(e, true, false, previoussource, previoustarget);                            
                            app.workBoxView.createEdgeProvModelFromData(e_p);
                        }); 
                        window.localStorage.removeItem(graphID);
                        window.localStorage.removeItem("prov-" + graphID);
                        }
                        else {
                            alert("Nothing can be committed! ");
                        }
                    }
                });
            },
            error: function(xhr){
                console.error(xhr);
                alert("An error occurred fetching data");
            }
        });
    }
        //该graph没有node信息
    if (status === "nocontent"){
        if (!$.isEmptyObject(graphdata)){
            app.workBoxView.createModelForGraphData(graphID, graphdata);
        }
        else {
            alert("Nothing can be committed! ");
        }
    }
    });
  },
  
  updateGraph: function(event){
    var graphID = chart.graphID;
    app.browseBoxView.toggleViewMode(true);
    app.browseBoxView.getAnalysis(graphID, function(previousGraphData){
        previousGraphData.nodes.forEach(function(n){
            var id = n.nodeID;
            if(window.localStorage.getItem(id) === null){
                deleteNodes(id, function(isdeleted){
                    if (isdeleted === false)
                        alert("Something went wrong when deleting node!");
                });
            }
            window.localStorage.removeItem(id);
            window.localStorage.removeItem("prov-"+id);
        });
        previousGraphData.edges.forEach(function(e){
            var id = e.edgeID;
            if(window.localStorage.getItem(id) === null){
                deleteEdges(id, function(isdeleted){
                    if (isdeleted === false)
                        alert("Something went wrong when deleting node!");
                });
            }
            window.localStorage.removeItem(id);
            window.localStorage.removeItem("prov-"+id);
        });
        var graphdata = app.browseBoxView.getInfoFromLocalStorage(graphID); 
        app.workBoxView.createModelForGraphData(graphID, graphdata);
        alert("This graph is updated!");
    });
  },
  
  checkoutGraph: function(event){
    app.browseBoxView.toggleViewMode(false);
    app.browseBoxView.getAnalysis(chart.graphID, function(data){
        var result = "success";
        if (result == 'success')
            app.browseBoxView.parseGraphDataToLocalStorage(data);
        else {
            alert(result);
            return ("Fail");
        }
    });
  },
  
  projectList: function() {
      $("#row-mergedgraphbox").hide();
      $("#row-browsebox").hide();
      $("#row-contributorbox").hide();
      $("#row-workbox").hide();
      $("#row-projectbox").show();
      app.projectBoxView.getProjectList();
      this.changeMode();
      
      this.$el.hide();
  },
  
  projectDetail: function() {
      this.changeMode();
      $("#row-mergedgraphbox").show();
      $("#row-browsebox").show();
      app.browseBoxView.getAnalysisList();
      $("#browse_box_own").show();
      $("#row-contributorbox").show();
      $("#row-projectbox").hide();
      $("#row-workbox").hide();
  }
});
