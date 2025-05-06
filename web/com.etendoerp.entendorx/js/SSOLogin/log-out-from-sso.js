// ** {{{ Custom OB.Utilities.logout }}} **
// Logout from the application, removes server side session info and redirects
// the client to the Login page.

if (OB.PropertyStore.get('ETRX_AllowSSOLogin') === 'Y') {
  OB.Utilities._originalLogout = OB.Utilities.logout;

  OB.Utilities.logout = function (confirmed) {
    if (!confirmed) {
      isc.confirm(OB.I18N.getLabel('OBUIAPP_LogoutConfirmation'), function (ok) {
        if (ok) {
          OB.Utilities.logout(true);
        }
      });
      return;
    }

    function getSSOAuthType(callback) {
      function callbackOnProcessActionHandler(response, data) {
        if (data.message?.severity === 'error') {
          this.getWindow().showMessage(data.message.text);
        } else {
          const ssoType = data['authtype'];
          const domain = data['domainurl'];
          const clientId = data['clientid'];
          callback(ssoType, domain, clientId);
        }
      }

      OB.RemoteCallManager.call(
        'com.etendoerp.etendorx.GetSSOProperties',
        { properties: 'auth.type, domain.url, client.id' },
        {},
        callbackOnProcessActionHandler
      );
    }

    async function logoutWithPopup(ssoDomain, clientId, sanitizedRedirectUri) {
      const logoutWindow = window.open(
        `https://${ssoDomain}/v2/logout?client_id=${clientId}&returnTo=${encodeURIComponent(sanitizedRedirectUri)}`,
        '_blank',
        'width=1,height=1'
      );

      setTimeout(() => {
        logoutWindow?.close();
        OB.Utilities._originalLogout(true);
      }, 5000);
    }

    getSSOAuthType(function (ssoType, ssoDomain, clientId) {
      const logoutRedirectUri = window.location.origin + OB.Application.contextUrl;
      const sanitizedRedirectUri = logoutRedirectUri.endsWith('/')
        ? logoutRedirectUri.slice(0, -1)
        : logoutRedirectUri;

      if (ssoType === 'Auth0') {
        logoutWithPopup(ssoDomain, clientId, sanitizedRedirectUri)
      } else {
        const middlewareLogoutUrl = `http://localhost:9580/logout`;
        const iframe = document.createElement('iframe');
        iframe.style.display = 'none';
        iframe.src = middlewareLogoutUrl;
        document.body.appendChild(iframe);

        setTimeout(() => {
          document.body.removeChild(iframe);
        }, 1000);
        setTimeout(() => {
          OB.Utilities._originalLogout(true);
        }, 1000);
      }
    });
  };
}
