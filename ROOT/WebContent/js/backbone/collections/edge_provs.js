/**
 * collection(list of models) for an edge provenance record which is used in cispaces
 */

var app = app || {};

var Edge_Prov_List = Backbone.Collection.extend({

  model: app.Edge_Prov,

  // localStorage: new Backbone.LocalStorage('edges-backbone'),
  url: remote_server + '/VC/rest/edge_prov'
  
});

app.Edge_Provs = new Edge_Prov_List();