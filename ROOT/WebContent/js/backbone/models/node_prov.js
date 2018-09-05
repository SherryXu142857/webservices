/* 
 * model for a node provenance record in cispaces
 */

var app = app || {};

app.Node_Prov = Backbone.Model.extend({
  defaults: {
    nodeID: '',
    parentnodeid: '',
    originalnodeid: '',
    ismergable: 1,
    graphID:''
  }
});

