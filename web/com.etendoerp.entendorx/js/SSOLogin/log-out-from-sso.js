// ** {{{ Custom OB.Utilities.logout }}} **
// Logout from the application, removes server side session info and redirects
// the client to the Login page.

if (OB.PropertyStore.get('ETRX_AllowSSOLogin') === 'Y') {
  OB.Utilities._originalLogout = OB.Utilities.logout;

  OB.Utilities.logout = function(confirmed) {
    if (!confirmed) {
      isc.confirm(OB.I18N.getLabel('OBUIAPP_LogoutConfirmation'), function(ok) {
        if (ok) {
          OB.Utilities.logout(true);
        }
      });
      return;
    }
    var ssoDomain;
    var clientId;

    function callbackOnProcessActionHandler(response, data, request) {
      if (data.message?.severity === 'error') {
        this.getWindow().showMessage(data.message.text);
      } else {
        ssoDomain = data.domainurl;
        clientId = data.clientid;
        var logoutRedirectUri = window.location.origin + OB.Application.contextUrl;
        if (logoutRedirectUri.endsWith('/')) {
          logoutRedirectUri = logoutRedirectUri.slice(0, -1);
        }
        var logoutUrl = `https://${ssoDomain}/v2/logout?client_id=${clientId}&returnTo=${encodeURIComponent(logoutRedirectUri)}`;

        var iframe = document.createElement("iframe");
        iframe.style.display = "none";
        iframe.src = logoutUrl;
        document.body.appendChild(iframe);

        setTimeout(function () {
          document.body.removeChild(iframe);
        }, 5000);

        setTimeout(function () {
          OB.Utilities._originalLogout(true);
        }, 3000);
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
  };
}
