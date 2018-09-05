var app = app || {};

/**
 * ProjectBox
 * ---------------------------------
 * the UI for 'projectBox'
 */
var projectID;
var userID;

app.ContributorBoxView = Backbone.View.extend({
    el: '#contributor_box',
    
    events:{
        'click .btn-view': 'viewContributorGraphs'
    },
    
    initialize: function() {
        projectID = readCookie('projectid');
        userID = readCookie('user_id');
        $("#row-contributorbox").show();
        this.getContributorsList();
    },
    
    render: function() {},
    
    getContributorsList: function() {
        
        var self = this;
        $(".existing-contributor").remove();
        Backbone.ajax({
            type: 'GET',
            url: remote_server + '/VC/rest/contributor/' + userID + '/' + projectID,
            success: function(contributors){
                if(contributors){
                    contributors.forEach(function(contributor) {
                        self.createContributorPanel(contributor);
                    });
                }
            },
            error: function(xhr){
                console.error("Ajax failed: " + xhr.statusText);
            }
        });
    },
    
    createContributorPanel: function(contributor){
        var div_panel = $("<div></div>", {
          'class': "panel panel-green"
        }).appendTo($("<div></div>", {
          'class': "existing-contributor col-lg-2 col-md-4",
          'id': "panel_"+contributor.userID
        }).appendTo($("#contributor_box")));

        var div_heading = $("<div></div>", {
          'class': "panel-heading"
        }).appendTo(div_panel);

        $("<label></label>", {
          'text': "Name:",
          'style': "margin: 5px 10px"
        }).appendTo($("<div></div>", {
          'class': "row"
        }).appendTo(div_heading)).after($("<span></span>", {
          'text': contributor.userName
        }));

        var btn = $("<button></button>", {
            'class': "pull-left btn btn-xs btn-outline btn-success btn-view",
            'name': "btn_" + contributor.userID,
            'id': "btn_contributor_" + contributor.userID,
            'text': "Details"
        }).appendTo($("<div></div>", {
            'class': "panel-footer"
        }).appendTo(div_panel))
        .after($("<div></div>", {
          'class': "clearfix"
        }));
    },
    
    viewContributorGraphs: function(event){
        $("#projectDetail").show();
        $("#row-mergedgraphbox").hide();
        $("#row-contributorbox").hide();
        
        $(".existing-analysis-browse_box_own").remove();
        $("#browse_box_own").hide();
        $(".existing-analysis-browse_box_contribute").remove();
        
        var userid = event.target.attributes.name.value.replace("btn_", "");

        Backbone.ajax({
            type: 'GET',
            url: remote_server + '/VC/rest/analyses/contributorGraph/' + userid + '/' + userID + '/' + projectID + '/meta',
            success: function(data) {
                if(data){
                    data.forEach(function(analysis) {
                        app.browseBoxView.makeGraphElement(analysis, "browse_box_contribute", analysis.authorityType);
                    });
                }
            },
            error: function(xhr) {
              console.error("Ajax failed: " + xhr.statusText);
            }
    });
    }
});
