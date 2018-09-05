/* 
 * model for an edge provenance record in cispaces
 */

var app = app || {};

app.Edge_Prov = Backbone.Model.extend({
  defaults: {
    edgeID: '',
    parentedgeid: '',
    originaledgeid: '',
    ismergable: 1,
    graphID:''
  }
});
