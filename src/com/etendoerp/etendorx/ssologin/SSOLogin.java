package com.etendoerp.etendorx.ssologin;

import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.security.SignInProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.system.Client;
import org.openbravo.service.db.DalConnectionProvider;

public class SSOLogin implements SignInProvider {
    private static final Logger log = Logger.getLogger(SSOLogin.class);

    @Override
    public String getLoginPageSignInHTMLCode() {

        final Properties openbravoProperties = OBPropertiesProvider.getInstance().getOpenbravoProperties();
        String domain = openbravoProperties.getProperty("sso.domain.url");
        String clientId = openbravoProperties.getProperty("sso.client.id");
        String redirectUri = openbravoProperties.getProperty("sso.callback.url");
        String sourceURL = openbravoProperties.getProperty("sso.source.url");

        if (StringUtils.isBlank(domain)) {
            return "";
        }

        ConnectionProvider cp = new DalConnectionProvider(false);
        Client systemClient = OBDal.getInstance().get(Client.class, "0");
        String systemLanguage = systemClient.getLanguage().getLanguage();
        String loginButtonMessage = Utility.messageBD(cp, "ETRX_LoginSSO", systemLanguage);
//    String ssoButton = "<style>"
//        + " #login-buttons {"
//        + "   display: flex;"
//        + "   flex-direction: column;" // ✅ Asegura que los botones estén en columna
//        + "   align-items: center;" // ✅ Centra los botones horizontalmente
//        + "   justify-content: center;" // ✅ Opcional: Si quieres centrar verticalmente también
//        + "   width: 100%;" // ✅ Asegura que el contenedor ocupe todo el ancho
//        + "   margin-top: 20px;" // Espacio superior
//        + " }"
//        + " .sso-login-button {"
//        + "   display: block;"
//        + "   background-color: #202452;"
//        + "   color: white;"
//        + "   padding: 10px 20px;"
//        + "   font-size: 16px;"
//        + "   border-radius: 5px;"
//        + "   cursor: pointer;"
//        + "   text-decoration: none;"
//        + "   margin-top: 10px;" // Espacio entre los botones
//        + "   width: auto;" // Ajusta el tamaño según el contenido
//        + "   text-align: center;" // Centra el texto dentro del botón
//        + " }"
//        + ".sso-login-button:hover { opacity: 80%; }"
//        + "</style>"
//        + "<script src=\"" + soruceURL + "\"></script>"
//        + "<div id=\"login-buttons\"></div>" // ✅ Asegurar que el div existe antes del script
//        + "<script>"
//        + " async function getAuth0Connections() {"
//        + "   const domain = '" + domain + "';"
//        + "   const token = '" + tokenAuth0 + "';"
//        + "   try {"
//        + "       const response = await fetch('https://" + domain + "/api/v2/connections', {"
//        + "           method: 'GET',"
//        + "           headers: {"
//        + "               Authorization: 'Bearer " + tokenAuth0 + "',"
//        + "               'Content-Type': 'application/json'"
//        + "           }"
//        + "       });"
//        + "       const data = await response.json();"
//        + "       console.log('Conexiones de Auth0:', data);"
//        + "       if (!Array.isArray(data)) throw new Error('La API no devolvió un array válido');"
//        + "       return data.filter(conn => conn.enabled_clients.includes('" + clientId + "')).map(conn => conn.name);"
//        + "   } catch (error) {"
//        + "       console.error('Error obteniendo conexiones de Auth0:', error);"
//        + "       return [];"
//        + "   }"
//        + " }"
//        + " async function loginWithSSO(provider) {"
//        + "   const clientId = '" + clientId + "';"
//        + "   const domain = '" + domain + "';"
//        + "   const redirectUri = '" + redirectUri + "';"
//        + "   console.log(`Iniciando sesión con ${provider}`);" // ✅ Debug en consola"
//        + "   const auth0Client = new auth0.WebAuth({"
//        + "       domain: domain,"
//        + "       clientID: clientId,"
//        + "       redirectUri: redirectUri,"
//        + "       responseType: 'token id_token',"
//        + "       scope: 'openid profile email'"
//        + "   });"
//        + "   auth0Client.authorize({ connection: provider });"
//        + " }"
//        + " async function generateLoginButtons() {"
//        + "   const connections = await getAuth0Connections();"
//        + "   console.log('Conexiones disponibles:', connections);"
//        + "   const container = document.getElementById('login-buttons');"
//        + "   if (!container) { console.error('Error: No se encontró el contenedor de botones'); return; }"
//        + "   connections.forEach(provider => {"
//        + "       const button = document.createElement('button');"
//        + "       button.textContent = `Login con ${provider}`;"
//        + "       button.classList.add('sso-login-button');"
//        + "       button.onclick = () => loginWithSSO(provider);"
//        + "       container.appendChild(button);"
//        + "   });"
//        + " }"
//        + " window.onload = generateLoginButtons;"
//        + " </script>";

        String ssoButton = "<style>"
            + ".sso-login-button {"
            + "  display: inline-block;"
            + "  background-color: #202452;"
            + "  color: white;"
            + "  padding: 10px 20px;"
            + "  font-size: 16px;"
            + "  border-radius: 5px;"
            + "  cursor: pointer;"
            + "  text-decoration: none;"
            + "  margin-top: 10px;"
            + "}"
            + ".sso-login-button:hover { opacity: 80%; }"
            + "</style>"
            + "<script src=\"" + sourceURL + "\"></script>"
            + "<script>"
            + "function loginWithSSO() {"
            + "  var webAuth = new auth0.WebAuth({"
            + "    domain: '" + domain + "',"
            + "    clientID: '" + clientId + "',"
            + "    redirectUri: '" + redirectUri + "',"
            + "    responseType: 'code',"
            + "    scope: 'openid profile email'"
            + "  });"
            + "  webAuth.authorize();"
            + "}"
            + "</script>"
            + "<button class=\"pure-button login-button ButtonLink ButtonLink_default\" onclick=\"loginWithSSO()\">" + loginButtonMessage + "</button>";

        return "<br>" + ssoButton;
    }
}
