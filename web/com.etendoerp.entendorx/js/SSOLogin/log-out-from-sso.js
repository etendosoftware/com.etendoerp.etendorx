// ** {{{ Custom OB.Utilities.logout }}} **
// Logout from the application, removes server side session info and redirects
// the client to the Login page.
if (OB.PropertyStore.get('ETRX_AllowSSOLogin') === 'Y') {
  OB.Utilities.logout = function(confirmed) {
    if (!confirmed) {
      isc.confirm(OB.I18N.getLabel('OBUIAPP_LogoutConfirmation'), function(ok) {
        if (ok) {
          OB.Utilities.logout(true);
        }
      });
      return;
    }
    OB.Utilities.logoutWorkQueue = [];
    var q = OB.Utilities.logoutWorkQueue,
      i,
      tabs = OB.MainView.TabSet.tabs,
      tabsLength = tabs.length,
      appFrame;

    logoutFromSSO();

    q.push({
      func: OB.RemoteCallManager.call,
      self: this,
      args: [
        'org.openbravo.client.application.LogOutActionHandler',
        {},
        {},
        function() {
          var logoutRedirectUrl = OB.Application.logoutRedirect;
          window.location.href = (logoutRedirectUrl != null && logoutRedirectUrl != undefined && logoutRedirectUrl != '')
            ? logoutRedirectUrl
            : OB.Application.contextUrl;
        }
      ]
    });

    for (i = 0; i < tabsLength; i++) {
      if (tabs[i].pane.Class === 'OBClassicWindow') {
        appFrame =
          tabs[i].pane.appFrameWindow || tabs[i].pane.getAppFrameWindow();
        if (appFrame && appFrame.isUserChanges) {
          if (appFrame.validate && !appFrame.validate()) {
            q = [];
            return;
          }
          q.push({
            func: tabs[i].pane.saveRecord,
            self: tabs[i].pane,
            args: [tabs[i].ID, OB.Utilities.processLogoutQueue]
          });
        }
      }
    }
    OB.Utilities.processLogoutQueue();
  };

  function logoutFromSSO() {
    var ssoDomain;
    var clientId;
    function callbackOnProcessActionHandler(response, data, request) {
        if (data.message?.severity === 'error') {
            this.getWindow().showMessage(data.message.text)
        } else {
          ssoDomain = data.domainurl;
          clientId = data.clientid;
          var logoutRedirectUri = window.location.origin + OB.Application.contextUrl;
          if (logoutRedirectUri.endsWith('/')) {
            logoutRedirectUri = logoutRedirectUri.slice(0, -1);
          }
          var logoutUrl = `https://${ssoDomain}/v2/logout?client_id=${clientId}&returnTo=${encodeURIComponent(logoutRedirectUri)}`;
          window.location.href = logoutUrl;
        }
    }

    OB.RemoteCallManager.call(
        'com.etendoerp.etendorx.GetSSOProperties',
        {
            properties: 'domain.url, client.id'
        },
        {},
        callbackOnProcessActionHandler
    );
  }
}