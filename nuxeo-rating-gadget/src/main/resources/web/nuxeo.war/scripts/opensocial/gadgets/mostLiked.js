var prefs = new gadgets.Prefs();
(function() {
  var objects;

  var Constants = {
    mostLikedOperationId: 'Services.MostLiked',
    queryOperationId: 'Document.Query',
    userHasLikedIcon: '/nuxeo/icons/vote_up_active.png',
    likedIcon: '/nuxeo/icons/vote_up_active2.png',
    prefContextPath: 'contextPath'
  };

  var content;
  var tools;
  var contextPath;

  function displayObjects() {
    var table = jQuery('<table class="dataList"><thead>' + '<th class="iconColumn"></th>' + '<th>' + prefs.getMsg("label.dublincore.title") + '</th>' + '<th>Likes</th>' + '<th>' + prefs.getMsg("label.dublincore.creator") + '</th>' + '</thead></table>');

    for (var i = 0; i < objects.length; i++) {
      var object = objects[i];
      mkCell(object).appendTo(table);
    }

    jQuery(table.appendTo(content)).ready(function() {
      gadgets.window.adjustHeight();
    });
  }

  function mkCell(object) {
    var html = "<tr>";
    if (object.type == 'document') {
      html += '<td><img src="' + NXGadgetContext.clientSideBaseUrl + object.document.properties["common:icon"] + '" /></td>';
      html += '<td><a href="' + object.url + '">' + object.document.properties["dc:title"] + '</a></td>';
      html += '<td><img src="';
      if (object.hasUserLiked) {
        html += Constants.userHasLikedIcon
      } else {
        html += Constants.likedIcon
      }
      html += '"/>' + object.rating + '</td>';
      html += '<td>' + object.document.properties["dc:creator"] + '</td>';
      html += "</tr>";
    } else if (object.type == 'minimessage') {
      html += '<td><img src="' + NXGadgetContext.clientSideBaseUrl + 'icons/activity_message.png" /></td>';
      html += '<td>' + object.message + '</td>';
      html += '<td><img src="';
      if (object.hasUserLiked) {
        html += Constants.userHasLikedIcon;
      } else {
        html += Constants.likedIcon;
      }
      html += '"/>' + object.rating + '</td>';
      html += '<td>' + object.actor + '</td>';
      html += "</tr>";
    }
    return jQuery(html)
  }

  function handleMostLikedResponse(response, params) {
    content.empty();
    if (response.data) {
      objects = response.data.items;
      displayObjects();
    }
  }

  function handleQueryResponse(response, params) {
    if (response.data) {
      var select = jQuery('<select name="combo"></select>').appendTo(jQuery("#domains"));
      select.change(function(obj) {
        contextPath = obj.target.value;
        prefs.set(Constants.prefContextPath, contextPath);
        loadMostLiked();
        return false;
      });

      var savedContextPath = prefs.getString(Constants.prefContextPath) || '/default-domain';
      for (var i = 0; i < response.data.entries.length; i++) {
        var entry = response.data.entries[i];
        var selected = entry.path == savedContextPath ? ' selected="selected"' : '';
        console.log(entry.path)
        jQuery('<option value="' + entry.path + '"' + selected + '>' + entry.title + '</option>').appendTo(select)
      }
      select.change();
    }
  }

  function initToolbar() {
    if (contextPath != "/") {
      loadMostLiked();
      return;
    }
    var toolbar = jQuery('<div class="tools"><div class="floatR" id="contextButton"><a href="#" class="linkButton" title="Edit context settings">Settings</a></div><div id="domains" style="display: none;"></div><div class="clear" /></div>');
    tools = toolbar.prependTo(content.parent());

    jQuery("#contextButton a").click(function() {
      var that = jQuery(this);
      var disp = jQuery("#domains").css('display');
      jQuery("#domains").css('display', disp == "none" ? "block" : "none");
      gadgets.window.adjustHeight();
    });
    loadDomains();
  }

  function loadMostLiked() {
    var NXRequestParams = {
      operationId: Constants.mostLikedOperationId,
      operationParams: {
        contextPath: contextPath,
        limit: 10
      },
      operationContext: {},
      operationCallback: handleMostLikedResponse
    };

    doAutomationRequest(NXRequestParams);
  }

  function loadDomains() {
    var NXRequestParams = {
      operationId: Constants.queryOperationId,
      operationParams: {
        query: 'Select * from Domain where ecm:mixinType <> "HiddenInNavigation" AND ecm:currentLifeCycleState != "deleted"'
      },
      operationContext: {},
      operationCallback: handleQueryResponse,
      displayMethod: function() {}
    };

    doAutomationRequest(NXRequestParams);
  }

  gadgets.util.registerOnLoadHandler(function() {
    content = jQuery("#content");

    contextPath = getTargetContextPath();

    initToolbar();
  });
}());