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
      const popupWidth = 400;
      const popupHeight = 500;
      const left = (screen.width / 2) - (popupWidth / 2);
      const top = (screen.height / 2) - (popupHeight / 2);

      const popup = window.open('', 'auth0LogoutPopup', `width=${popupWidth},height=${popupHeight},top=${top},left=${left}`);

      if (!popup) return;

      // Escribir contenido HTML en el popup
      popup.document.write(`
        <html>
          <head>
            <title>Logging out</title>
            <style>
              body {
                font-family: Arial, sans-serif;
                display: flex;
                flex-direction: column;
                justify-content: center;
                align-items: center;
                height: 100vh;
                margin: 0;
                background-color: #f5f5f5;
              }
              .loader {
                margin-top: 20px;
                width: 40px;
                height: 40px;
                border: 5px solid #ccc;
                border-top-color: #007bff;
                border-radius: 50%;
                animation: spin 1s linear infinite;
              }
              @keyframes spin {
                to { transform: rotate(360deg); }
              }
              .dots::after {
                content: '';
                animation: dots 1.5s steps(4, end) infinite;
              }
              @keyframes dots {
                0%   { content: ''; }
                25%  { content: '.'; }
                50%  { content: '..'; }
                75%  { content: '...'; }
                100% { content: ''; }
              }
            </style>
          </head>
          <body>
            <div><strong>Logging out from Auth0<span class="dots"></span></strong></div>
            <div class="loader"></div>
          </body>
        </html>
      `);

      // Navegar al logout real
      popup.location.href = `https://${ssoDomain}/v2/logout?client_id=${clientId}&returnTo=${sanitizedRedirectUri}`;

      // Cerrar después de unos segundos y seguir con logout de la app
      setTimeout(() => {
        popup?.close();
        OB.Utilities._originalLogout(true);
      }, 1000);
    }


    getSSOAuthType(function (ssoType, ssoDomain, clientId) {
      const logoutRedirectUri = window.location.origin + OB.Application.contextUrl;
      const sanitizedRedirectUri = logoutRedirectUri.endsWith('/')
        ? logoutRedirectUri.slice(0, -1)
        : logoutRedirectUri;

      if (ssoType === 'Auth0') {
        logoutWithPopup(ssoDomain, clientId, sanitizedRedirectUri + '/web/com.etendoerp.entendorx/resources/logout-auth0.html')
      } else {
        const middlewareLogoutUrl = `http://etendoauth-middleware-env.eba-purewhpv.sa-east-1.elasticbeanstalk.com/logout`;
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
