/**
 * collection(list of models) for a node provenance record which is used in cispaces
 */

var app = app || {};

var Node_Prov_List = Backbone.Collection.extend({

  model: app.Node_Prov,

  // localStorage: new Backbone.LocalStorage('edges-backbone'),
  url: remote_server + '/VC/rest/node_prov'
  
});

app.Node_Provs = new Node_Prov_List();

