var app = app || {};

/**
 * ProjectBox
 * ---------------------------------
 * the UI for 'projectBox'
 */
var userID;

app.ProjectBoxView = Backbone.View.extend({
    el: '#project_box',
    
    events:{
        'click #new_project': 'newProjectBox',

        'click .btn-view': 'projectDetails',
        'click .btn-delete': 'deleteProject'
    },
    
    initialize: function() {
        userID = readCookie('user_id');
        $("#row-projectbox").show();
        $("#row-contributorbox").hide();
        $("#row-mergedgraphbox").hide();
        $("#row-browsebox").hide();
        this.getProjectList();
    },
    
    render: function() {},
    
    getProjectList: function(){
        var self = this;
        $(".existing-project").remove();

        Backbone.ajax({
            type: 'GET',
            url: remote_server + '/VC/rest/project/user/' + userID,
            success: function(projects) {        
                projects.forEach(function(p) {
                    if(p.isowner === "true")
                        self.createProjectPanel(p, "own", p.isowner);
                    else
                        self.createProjectPanel(p, "shared", p.isowner);
                });
            },
            error: function(xhr) {
                console.error("Ajax failed: " + xhr.statusText);
            }
        });
    },
    
    createProjectPanel: function(project, box, level) {
        var div_panel = $("<div></div>", {
          'class': "panel panel-green"
        }).appendTo($("<div></div>", {
          'class': "existing-project col-lg-2 col-md-4",
          'id': "panel_"+project.projectID
        }).appendTo($("#project_box_" + box)));

        var div_heading = $("<div></div>", {
          'class': "panel-heading"
        }).appendTo(div_panel);

        $("<label></label>", {
          'text': "Title:",
          'style': "margin: 5px 10px"
        }).appendTo($("<div></div>", {
          'class': "row"
        }).appendTo(div_heading)).after($("<span></span>", {
          'text': project.title
        }));
        
        $("<label></label>", {
          'text': "Owner:",
          'style': "margin: 5px 10px"
        }).appendTo($("<div></div>", {
          'class': "row"
        }).appendTo(div_heading)).after($("<span></span>", {
          'text': project.userName
        }));

        $("<label></label>", {
          'text': "Description:",
          'style': "margin: 5px 10px"
        }).appendTo($("<div></div>", {
          'class': "row"
        }).appendTo(div_heading)).after($("<span></span>", {
          'text': project.description
        }));

        $("<label></label>", {
          'text': "Date:",
          'style': "margin: 5px 10px"
        }).appendTo($("<div></div>", {
          'class': "row"
        }).appendTo(div_heading)).after($("<span></span>", {
          'text': project.timest
        }));

        var btn = $("<button></button>", {
            'class': "pull-left btn btn-xs btn-outline btn-success btn-view",
            'name': "btn_" + project.projectID,
            'project_owner': project.userID,
            'id': "btn_projectDetials_" + project.projectID,
            'text': "Details",
        }).appendTo($("<div></div>", {
            'class': "panel-footer"
        }).appendTo(div_panel)).before($("<button></button>", {
          'class': "btn btn-xs btn-outline btn-danger btn-delete",
          'name': "btn_" + project.projectID,
          'id': "btn_delete_" + project.projectID,
          'style': "margin-left: 5px",
          'text': "Delete",
          'title': "Permanently delete this analysis"
        })).after($("<div></div>", {
          'class': "clearfix"
        }));

        if (level !== "true")
            $("#btn_delete_" + project.projectID).hide();
    },
    
    newProjectBox: function(){       
        var self = this;
        $("#graph_info .modal-header span").text("New Project");

        $("#graph_info .modal-body input").val("");
        $("#graph_info .modal-body textarea").val("");

        $("#graph_info .modal-footer .btn-create").text("Create").off("click").on("click", function(event) {
            
            var projectID = generateUUID();
            var title = $("#graph_info .modal-body input").val();
            var description = $("#graph_info .modal-body textarea").val();

            if (_.isEmpty(title)) {
                alert("Please, enter a title");
            } else {
                var object = {
                  "projectID": projectID,
                  "userID": userID,
                  "timest": generateDate(),
                  "title": title.trim(),
                  "description": description.trim()
                };

                Backbone.ajax({
                    type: 'POST',
                    url: remote_server + '/VC/rest/project/new',
                    contentType: 'application/json', 
                    data: JSON.stringify(object),
                    success: function() {
                        self.toggleProjectView(projectID);
                        $("#graph_info").modal('hide');
                    },
                    error: function(xhr) {
                      console.error("Ajax failed: " + xhr.statusText);
                      alert('Something went wrong when creating new project. Please try again.');
                    }
                });
            }
        });
        $("#graph_info").modal('show');
    },
    
    toggleProjectView: function(projectID, ownerID){
        createCookie('projectid',projectID,2);
        
        $("#row-projectbox").hide();
        app.toolBoxView.$el.show();
        
        app.browseBoxView = new app.BrowseBoxView();
        $("#browse_box_own").show();
        app.mergedgraphBoxView = new app.MergedgraphBoxView(ownerID);
        app.contributorBoxView = new app.ContributorBoxView();
    },
    
    
    
    projectDetails: function(event){
        var projectID = event.target.attributes.name.value.replace("btn_", "");
        var ownerID = document.getElementById('btn_projectDetials_' + projectID).getAttribute('project_owner');
        this.toggleProjectView(projectID, ownerID);
    },
    
    deleteProject: function(event){
        if(confirm("Permanently delete this project?")) {
            var projectID = event.target.attributes.name.value.replace("btn_", "");
            Backbone.ajax({
                type: 'DELETE',
                url: remote_server + '/VC/rest/project/' + projectID,
                success: function(result, status){
                    if(status === "success"){
                        alert('Project is deleted!');
                        $("#panel_"+projectID).remove();
                    }
                },
                error: function(xhr) {
                    console.error("Ajax failed: " + xhr.statusText);
                }
            });
        }
    }
});