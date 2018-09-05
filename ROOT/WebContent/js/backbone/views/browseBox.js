var app = app || {};

/**
 * BrowseBox
 * ---------------------------------
 * the UI for 'browseBox'
 */
var projectID;
var userID;

app.BrowseBoxView = Backbone.View.extend({
  el: '#browse_box',
  events: {
    'click #new_analysis': 'newWorkBox',

    'click #importFromFile': 'selectFile',
    'change #myFile': 'importFromFile',

    'click .btn-view': 'viewAnalysis',
    'click .btn-checkout': 'viewAnalysis',

    'click .btn-export': 'exportToFile',
    'click .btn-delete': 'deleteAnalysis'
  },

  initialize: function() {
    projectID = readCookie('projectid');
    userID = readCookie('user_id');
    $("#row-browsebox").show();
    this.getAnalysisList();
  },

  render: function() {},

  newWorkBox: function() {
    // app.workBoxView.clearWorkBox();

    $("#graph_info .modal-header span").text("New Graph");

    $("#graph_info .modal-body input").val("");
    $("#graph_info .modal-body textarea").val("");
    $("#graph_info").modal('show');
    $("#graph_info .modal-footer .btn-create").text("Create").off("click").on("click", function(event) {

        var graphID = generateUUID();
        var title = $("#graph_info .modal-body input").val();
        var description = $("#graph_info .modal-body textarea").val();

        if (_.isEmpty(title)) {
            alert("Please, enter a title");
        } else {
            var object_graphdata = {
                "graphID": graphID,
                "userID": userID,
                "timest": generateDate(),
                "title": title.trim(),
                "description": description.trim(),
                "isshared": "false",
                "parentgraphid": "null",
                "projectID": projectID 
            };
        
            var object_graph_prov = {
                "graphID": graphID,    
                "parentgraphid": "null",
                "originalgraphid": "null",
                "type": "0"
            };

            Backbone.ajax({
                type: 'POST',
                url: remote_server + '/VC/rest/new',
                //dataType: 'text',
                contentType: 'application/json', //Supply the JWT auth token
                data: JSON.stringify(object_graphdata),
                success: function(result) {
                    Backbone.ajax({
                        type: 'GET',
                        url: remote_server + "/VC/rest/project/" + projectID + "/meta",
                        success: function(data) {
                            if (data) {
                                Backbone.ajax({
                                    type: 'POST',
                                    url: remote_server + '/VC/rest/graphprov/save/' + 0,
                                    contentType: 'application/json',
                                    data: JSON.stringify(object_graph_prov)
                                });
                            }
                        },
                        error: function(xhr) {
                            alert("No project exists with this id in database.");
                        }
                    });
                    $("#row-workbox").show();
                    $("#row-contributorbox").hide();
                    $("#row-mergedgraphbox").hide();

                    app.workBoxView.clearWorkBox();

                    //将graph信息保存在localStorage中
                    window.localStorage.setItem(graphID, JSON.stringify(object_graphdata));
                    //将graph provenance record保存在localStorage中
                    window.localStorage.setItem("prov-" + graphID, JSON.stringify(object_graph_prov));
                    var ret_graph = draw([], [], [], [], chart);
                    push_graph_data(ret_graph);

                    $("#saveProgress").attr("disabled", true);

                    // saves the meta data of the graph
                    chart.graphID = graphID;
                    chart.title = object_graphdata.title;
                    chart.desciption = object_graphdata.description;
                    chart.date = object_graphdata.timest;
                    $("#span-graphTitle").text("[" + chart.title + "]");

                    $("#row-browsebox").hide();
                    app.toolBoxView.$el.show();

                    app.browseBoxView.toggleViewMode(false);

                    $("#graph_info").modal('hide');
                },
                error: function(xhr) {
                    console.error("Ajax failed: " + xhr.statusText);
                    alert('Something went wrong. Please try again.');
                }
            });
            $("#graph_info").modal('hide');
        }
    });
  },

  getAnalysis: function(graphID, callback) {
    Backbone.ajax({
      type: 'GET',
      url: remote_server + '/VC/rest/analysis/' + graphID + "/" + userID,
      success: function(response, status, xhr) {
          console.log(response);
        // change the type of annot
        if(response.nodes){
          response.nodes.forEach(function(d){
            try{
            d.annot = JSON.parse(d.annot);
          } catch(error){
            console.log(error);
          }
          });
        }
        callback(response, status, xhr);
      },
      error: function(xhr) {
        console.error("Ajax failed: " + xhr.statusText);
        alert('An error occurred fetching data.');
      }
    });
  },

  getAnalysisList: function() {
    var self = this;
    $(".existing-analysis-browse_box_own").remove();
    $(".existing-analysis-browse_box_contribute").remove();

    Backbone.ajax({
        type: 'GET',
        url: remote_server + '/VC/rest/analyses/user/' + userID + '/' + projectID + '/meta',
        success: function(data) {   
            if(data){
                data.forEach(function(analysis) {
                    self.makeGraphElement(analysis, "browse_box_own");
                });
            }
        },
        error: function(xhr) {
            console.error("Ajax failed: " + xhr.statusText);
        }
    });
    
    Backbone.ajax({
        type: 'GET',
        url: remote_server + 'VC/rest/analyses/share/' + userID + '/' + projectID + '/meta',
        success: function(data) {
            if(data){
                data.forEach(function(analysis) {
                    self.makeGraphElement(analysis, "browse_box_contribute", analysis.authorityType);
                });
            }
        },
        error: function(xhr) {
            console.error("Ajax failed: " + xhr.statusText);
        }
    });
  },
    
  makeGraphElement: function(analysis, box, level = "4") {
    var div_panel = $("<div></div>", {
      'class': "panel panel-green"
    }).appendTo($("<div></div>", {
      'class': "existing-analysis-"+ box +" col-lg-2 col-md-4",
      'id': "panel_"+analysis.graphID
    }).appendTo($("#"+box)));

    var div_heading = $("<div></div>", {
      'class': "panel-heading"
    }).appendTo(div_panel);

    /*
    $("<label></label>", {
      'text': "graphID",
      'style': "margin: 5px 10px"
    }).appendTo($("<div></div>", {
      'class': "row"
    }).appendTo(div_heading)).after($("<span></span>", {
      'text': analysis.graphID
    }));
    */

    $("<label></label>", {
      'text': "Title:",
      'style': "margin: 5px 10px"
    }).appendTo($("<div></div>", {
      'class': "row"
    }).appendTo(div_heading)).after($("<span></span>", {
      'text': analysis.title
    }));

    $("<label></label>", {
      'text': "Description:",
      'style': "margin: 5px 10px"
    }).appendTo($("<div></div>", {
      'class': "row"
    }).appendTo(div_heading)).after($("<span></span>", {
      'text': analysis.description
    }));

    $("<label></label>", {
      'text': "Date:",
      'style': "margin: 5px 10px"
    }).appendTo($("<div></div>", {
      'class': "row"
    }).appendTo(div_heading)).after($("<span></span>", {
      'text': analysis.timest
    }));

    var btn = $("<button></button>", {
      'class': "pull-right btn btn-xs btn-outline btn-info btn-export",
      'name': "btn_" + analysis.graphID,
      'text': "Export",
      'title': "Export this analysis to file"
    }).appendTo($("<div></div>", {
      'class': "panel-footer"
    }).appendTo(div_panel)).before($("<button></button>", {
      'class': "pull-left btn btn-xs btn-outline btn-success btn-view",
      'name': "btn_" + analysis.graphID,
      'id': "btn_view_" + analysis.graphID,
      'text': "View",
      'title': "View this analysis (read-only)"
    })).before($("<button></button>", {
      'class': "pull-left btn btn-xs btn-outline btn-success btn-checkout",
      'style': "margin-left: 5px",
      'name': "btn_" + analysis.graphID,
      'id': "btn_checkout_" + analysis.graphID,
      'text': "Checkout",
      'title': "Checkout this analysis for editing"
    })).before($("<button></button>", {
      'class': "btn btn-xs btn-outline btn-danger btn-delete",
      'name': "btn_" + analysis.graphID,
      'id': "btn_delete_" + analysis.graphID,
      'style': "margin-left: 5px",
      'text': "Delete",
      'title': "Permanently delete this analysis"
    })).after($("<div></div>", {
      'class': "clearfix"
    }));
    
    if (level === "0")
        $("#btn_checkout_" + analysis.graphID).hide();
    if (level !== "4")
        $("#btn_delete_" + analysis.graphID).hide();
  },

  toggleViewMode: function(_view_flag) {

    view_flag = _view_flag;
    $("#saveProgress").show();
    $("#browseAnalyses").show();
    $("#simulation").show();
    $("#projectDetail").show();
    
    if (view_flag) {
      // If a user click [View] button, the user should not be able to edit the graph
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
      $("#updateGraph").hide();
      $("#checkoutGraph").show();

      $("#span-viewMode").text("(View Only)");
    } else {
      $("#info").show();
      $("#claim").show();
      $("#pref").show();
      $("#con").show();
      $("#pro").show();

      $("#delete-node").removeClass("disabled");
      $("#link-from").removeClass("disabled");
      $("#cancel-link").removeClass("disabled");

      $("#commitGraph").show();
      $("#updateGraph").show();
      $("#checkoutGraph").hide();

      $("#span-viewMode").text("");
    }
  },
  
  parseGraphDataToLocalStorage: function(graph) {
    
    var graphinfo = {
        timest: graph.timest,
        isshared: graph.isshared,
        parentgraphid: graph.parentgraphid,
        description: graph.description,
        graphID: graph.graphID,
        title: graph.title,
        userID: graph.userID,
        authorityType: graph.authorityType
    };
    window.localStorage.setItem(graph.graphID, JSON.stringify(graphinfo));
    window.localStorage.setItem("prov-" + graph.graphID, JSON.stringify(graph.prov));
    
    var nodes = graph.nodes;
    var edges = graph.edges;
    
    var nodes_prov = graph.nodes_prov;
    var edges_prov = graph.edges_prov;
        
    if(nodes){
        nodes.forEach(function(node){
            window.localStorage.setItem(node.nodeID, JSON.stringify(node));
        });
    }
    if(nodes_prov){
        nodes_prov.forEach(function(node_prov){
            window.localStorage.setItem("prov-" + node_prov.nodeID, JSON.stringify(node_prov));
        });
    }
    if(edges){
        edges.forEach(function(edge){
            window.localStorage.setItem(edge.edgeID, JSON.stringify(edge));
        });
    }
    if(edges_prov){
        edges_prov.forEach(function(edge_prov){
            window.localStorage.setItem("prov-" + edge_prov.edgeID, JSON.stringify(edge_prov));
        });
    }
  },
  
  getInfoFromLocalStorage: function(graphid){   
    var graph = JSON.parse(window.localStorage.getItem(graphid));
    var graph_prov = JSON.parse(window.localStorage.getItem("prov-" + graphid));
    var localdata_str = window.localStorage.valueOf(); 
    var metadata = new Array();
    var provdata = new Array();
    var graphdata = {};
    if(graph){
        graphdata = {
            description: graph.description,
            graphID: graphid,
            isshared: graph.isshared,
            timest: graph.timest,
            title: graph.title,
            userID: graph.userID,
            authorityType: graph.authorityType,
            parentgraphid: graph_prov.parentgraphid,
            originalgraphid: graph_prov.originalgraphid
        };
        graphdata.edges = new Array();
        graphdata.nodes = new Array();
        graphdata.nodes_prov = new Array();
        graphdata.edges_prov = new Array();
        
        for(var i=0; i<localdata_str.length; i=i+1){
            var key = localdata_str.key(i);
            var data = localdata_str.getItem(key);
            if (data.substr(0, 1) === '{' && key.substr(0, 4) === "prov")
                provdata.push(JSON.parse(data));
            else if(data.substr(0, 1) === '{')
                metadata.push(JSON.parse(data));
        }
        metadata = metadata.filter(function(data){
            return (data.graphID===graphid);
        });  
        provdata = provdata.filter(function(data){
            return (data.graphID===graphid);
        });  
        
        metadata.forEach(function(gd){
            if (gd.hasOwnProperty("edgeID")) graphdata.edges.push(gd);
            if (gd.hasOwnProperty("nodeID")) graphdata.nodes.push(gd);
        });
        provdata.forEach(function(pd){
            if (pd.hasOwnProperty("edgeID")) graphdata.edges_prov.push(pd);
            if (pd.hasOwnProperty("nodeID")) graphdata.nodes_prov.push(pd);
        });
    }
    return graphdata;
  },
  
  dataVisualization: function(graphdata, flag=true){
      // initialises a workbox
    $("#row-workbox").show();
    $("#row-browsebox").hide();
    app.toolBoxView.$el.show();

    app.workBoxView.clearWorkBox();

    // saves the meta data of the graph
    chart.graphID = graphdata['graphID'];
    chart.title = graphdata['title'];
    chart.desciption = graphdata['description'];
    chart.date = graphdata['timest'];

    var nodes = graphdata['nodes'];
    var edges = graphdata['edges'];
    var nodes_prov = graphdata['nodes_prov'];
    var edges_prov = graphdata['edges_prov'];

    // set up simulations for force-directed graphs
    var ret_simulation = set_simulation(15, chart.svg.width, chart.svg.height);
    push_node_style_data(ret_simulation);

    // the simulation used when drawing a force-directed graph
    chart.simulation = ret_simulation.simulation;

    var ret_graph = draw(nodes, nodes_prov, edges, edges_prov, chart, flag);
    push_graph_data(ret_graph);

    // start simulation for displaying graphsv
    chart.simulation = restart_simulation(chart.simulation, false);

    $("#saveProgress").attr("disabled", true);

    $("#span-graphTitle").text("[" + chart.title + "]");
  },
    
  viewAnalysis: function(event) {
      
    if (!window.localStorage){
        alert("This browser dose not support localStorage!");
        return false;
    }
    else {
        var isGetAnalysis = true;
        var graphID = event.target.attributes.name.value.replace("btn_", "");
        this.toggleViewMode((event.target.attributes.class.value.indexOf("view") > 0));
        $("#row-contributorbox").hide();
        $("#row-mergedgraphbox").hide();
        var mode = event.target.textContent;
        if (mode === "Checkout"){
            var graphdata = app.browseBoxView.getInfoFromLocalStorage(graphID);
            if(!$.isEmptyObject(graphdata)){
                if (graphdata.userID !== userID){
                    $("#updateGraph").hide();
                }
                var choice;
                choice = confirm("You have modified this graph, do you want to continue?");
                if(choice){
                    isGetAnalysis = false;
                    app.browseBoxView.dataVisualization(graphdata, false);
                }
                else{
                    var key;
                    graphdata.nodes.forEach(function(n){
                        key = n.nodeID;
                        if(key) {
                            window.localStorage.removeItem(key);
                            window.localStorage.removeItem("prov-"+key);
                        }  
                    });
                    graphdata.edges.forEach(function(e){
                        key = e.edgeID;
                        if(key) {
                            window.localStorage.removeItem(key);
                            window.localStorage.removeItem("prov-"+key);
                        }   
                    });
                    window.localStorage.removeItem(graphID);
                    window.localStorage.removeItem("prov-"+graphID);
                }
            }
        }
        if(isGetAnalysis){
            this.getAnalysis(graphID, function(data) {
                if (data.userID !== userID){
                    $("#updateGraph").hide();
                }
                var result = "success";
                if (result == 'success') {
                    if (mode === "Checkout")
                        app.browseBoxView.parseGraphDataToLocalStorage(data);
                    app.browseBoxView.dataVisualization(data);
                } else {
                    alert(result);
                    return ("Fail");
                }
            });
        }
    }
  },

  createNodeProv: function(node) {
      var nodeprov = {
          nodeID: node.nodeID,
          parentnodeid: null,
          originalnodeid: null,
          graphID: node.graphID
      };
      return nodeprov;
  },
  
  createEdgeProv: function(edge) {
      var edgeprov = {
          edgeID: edge.edgeID,
          parentedgeid: null,
          originaledgeid: null,
          graphID: edge.graphID
      };
      return edgeprov;
  },

  selectFile: function() {

    // app.workBoxView.clearWorkBox();

    var input_file = $("#myFile").click();

    return input_file;
  },
  
  createNewGraphFromData: function(object_graphdata, object_graph_prov, graphType){

    Backbone.ajax({
        type: 'POST',
        url: remote_server + '/VC/rest/save',
        contentType: 'application/json',
        data: JSON.stringify(object_graphdata),
        success: function(result) {
            confirm("Analysis [" + object_graphdata['title'] + "] saved.");
        },
        error: function(xhr) {
            alert('Something went wrong. Please try again.');
            console.log(xhr);
        }
    });

    Backbone.ajax({
        type: 'GET',
        url: remote_server + "/VC/rest/project/" + projectID + "/meta",
        success: function(data) {
            if (data) {
                Backbone.ajax({
                    type: 'POST',
                    url: remote_server + '/VC/rest/graphprov/save/' + graphType,
                    contentType: 'application/json',
                    data: JSON.stringify(object_graph_prov)
                });
            }
        },
        error: function(xhr) {
            alert("No project exists with this id in database.");
        }
    });
  },

  importFromFile: function(event) {
    readFile(event.target.files, function(jsonData) {
      var graphID = jsonData['graphID'];

      var existing = null;

      // 1. Check this graph belongs to the user or not
      Backbone.ajax({
        type: 'GET',
        url: remote_server + "/VC/rest/analyses/user/" + userID + "/noprojectid/meta",
        success: function(data) {
          if (data) {
            //匿名回调函数
            existing = data.find(function(d) {
              return d.graphID == graphID;
            });
            //_用来调用函数包,相当于jquery的$
            if(!_.isEmpty(existing)){
              alert("A graph exists with this id.");
            }
          }
        },
        error: function(xhr) {
          console.error(xhr);
          alert("An error occurred fetching data");
        },
        complete: function(xhr){

          if(_.isEmpty(existing)){
            // 2. Check this graph is saved in database
            Backbone.ajax({
              type: 'GET',
              url: remote_server + "/VC/rest/analysis/" + graphID + "/meta",
              success: function(data) {
                if (data) {
                  alert("A graph exists with this id in database.");
                }
              },
              error: function(xhr) {
                console.log(xhr);
              },
              complete: function(xhr) {
                  if (xhr.status == 404) { //404表示未找到与文件中相同的图
                // var graphID = jsonData['graphID'];
                  var title = jsonData['title'];
                  var description = jsonData['description'];
                  var object_graphdata = {
                      "graphID": graphID,
                      "userID": userID,
                      "timest": generateDate(),
                      "title": title.trim(),
                      "description": description.trim(),
                      "projectID": projectID,
                      "isshared": false,
                      "parentgraphid": null
                  };

                  var object_graph_prov = {
                      "graphID": graphID,    
                      "parentgraphid": null,
                      "originalgraphid": null
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
                          var nodes = jsonData['nodes'];
                          var edges = jsonData['edges'];
                          var nodes_prov = [];
                          nodes.forEach(function(node){
                              nodes_prov.push(app.browseBoxView.createNodeProv(node));
                          });
                          var edges_prov = [];
                          edges.forEach(function(edge){
                              edges_prov.push(app.browseBoxView.createEdgeProv(edge));
                          });

                          var ret_graph = draw(nodes, nodes_prov, edges, edges_prov, chart);
                          push_graph_data(ret_graph);
                      },
                      error: function(xhr) {
                          console.error("Ajax failed: " + xhr.statusText);
                          alert('Something went wrong. Please try again.');
                      }
                  });
                    // 3. Registers a new graph using jsonData
                  app.browseBoxView.createNewGraphFromData(object_graphdata, object_graph_prov, 0);
                  app.browseBoxView.makeGraphElement(object_graphdata, "browse_box_own");
                }
              }
            });
          }
        }
      });
    });
  },

  exportToFile: function(event) {

    var graphID = event.target.attributes.name.value.replace("btn_", "");

    this.getAnalysis(graphID, function(fileContent, status, xhr) {

      // check for a filename
      var fileName = event.target.parentElement.parentElement.firstElementChild.firstElementChild.getElementsByTagName("span")[0].innerText;
      if (_.isEmpty(fileName)) {
        fileName = graphID;
      }

      fileName = "export_" + fileName + ".cis";

      var disposition = xhr.getResponseHeader('Content-Disposition');
      if (disposition && disposition.indexOf('attachment') !== -1) {
        var fileNameRegex = /fileName[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
        var matches = fileNameRegex.exec(disposition);
        if (matches != null && matches[1]) {
          fileName = matches[1].replace(/['"]/g, '');
        }
      }

      var type = 'text/plain;charset=utf-8'; // xhr.getResponseHeader('Content-type');
      var blob = new Blob([JSON.stringify(fileContent)], {
        type: type
      });

      if (typeof window.navigator.msSaveBlob !== 'undefined') {
        window.navigator.msSaveBlob(blob, fileName);
      } else {
        var URL = window.URL || window.webkitURL;
        var downloadUrl = URL.createObjectURL(blob);

        if (fileName) {
          var a = document.createElement("a");
          if (typeof a.download === 'undefined') {
            window.location = downloadUrl;
          } else {
            a.href = downloadUrl;
            a.download = fileName;
            document.body.appendChild(a);
            a.click();
          }
        } else {
          window.location = downloadUrl;
        }

        setTimeout(function() {
          URL.revokeObjectURL(downloadUrl);
        }, 100);
      }
    });
  },
  
  deleteAnalysis: function(event) {
      if(confirm("Permanently delete this analysis?")) {
        var graphID = event.target.attributes.name.value.replace("btn_", "");
        Backbone.ajax({
            type: 'DELETE',
            url: remote_server + '/VC/rest/analysis/' + graphID,
            success: function(data) {
                $("#panel_"+graphID).remove();
            },
            error: function(xhr) {
              console.error("Deleting graph failed: " + xhr.statusText);
            },
            complete: function(){
              var graphdata = app.browseBoxView.getInfoFromLocalStorage(graphID);
              //clean local data
              if (!$.isEmptyObject(graphdata)){
                  graphdata.nodes.forEach(function(n){
                      window.localStorage.removeItem(n.nodeID);
                      window.localStorage.removeItem("prov-" + n.nodeID);
                  });
                  graphdata.edges.forEach(function(e){
                      window.localStorage.removeItem(e.edgeID); 
                      window.localStorage.removeItem("prov-" + e.edgeID);
                  }); 
                  window.localStorage.removeItem(graphID);
                  window.localStorage.removeItem("prov-" + graphID);
              }
            }
        });
      }
  }
});
