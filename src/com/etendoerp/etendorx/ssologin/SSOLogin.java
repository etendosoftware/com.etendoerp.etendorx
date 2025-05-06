package com.etendoerp.etendorx.ssologin;

import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.security.SignInProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.system.Client;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * Implementation of the SignInProvider interface for Single Sign-On (SSO) login functionality.
 */
public class SSOLogin implements SignInProvider {

    /**
     * Generates the HTML code for the SSO login button to be displayed on the login page.
     *
     * @return the HTML code for the SSO login button, or an empty string if the SSO domain is not configured
     */
    @Override
    public String getLoginPageSignInHTMLCode() {

        final Properties openbravoProperties = OBPropertiesProvider.getInstance().getOpenbravoProperties();
        String authType = openbravoProperties.getProperty("sso.auth.type");

        if (StringUtils.isBlank(authType)) {
            return "";
        }
        if (StringUtils.equals("Auth0", authType)) {
            String domain = openbravoProperties.getProperty("sso.domain.url");
            String clientId = openbravoProperties.getProperty("sso.client.id");
            String redirectUri = openbravoProperties.getProperty("sso.callback.url");

            if (StringUtils.isBlank(domain) || StringUtils.isBlank(clientId) || StringUtils.isBlank(redirectUri)) {
                return "";
            }

            ConnectionProvider cp = new DalConnectionProvider(false);
            Client systemClient = OBDal.getInstance().get(Client.class, "0");
            String systemLanguage = systemClient.getLanguage().getLanguage();
            String loginButtonMessage = Utility.messageBD(cp, "ETRX_LoginSSO", systemLanguage);

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
                    + "<script src=\"https://cdn.auth0.com/js/auth0/9.18.1/auth0.min.js\"></script>"
//                    + "<script src=\"" + sourceURL + "\"></script>"
                    + "<script>"
                    + "function loginWithSSO() {"
                    + "  var webAuth = new auth0.WebAuth({"
                    + "    domain: '" + domain + "',"
                    + "    clientID: '" + clientId + "',"
                    + "    redirectUri: '" + redirectUri + "',"
                    + "    responseType: 'code',"
                    + "    scope: 'openid profile email'"
                    + "  });"
                    + "  webAuth.authorize({"
                    + "  });"
                    + "}"
                    + "</script>"
                    + "<button class=\"pure-button login-button ButtonLink ButtonLink_default\" onclick=\"loginWithSSO()\">" + loginButtonMessage + "</button>";

            return "<br>" + ssoButton;
        } else {
            return "<style>" +
                    ".sso-login-container {" +
                    "  display: flex;" +
                    "  flex-direction: column;" +
                    "  align-items: center;" +
                    "  margin-top: 12px;" +
                    "  gap: 6px;" +
                    "}" +
                    ".sso-login-button {" +
                    "  display: flex;" +
                    "  align-items: center;" +
                    "  gap: 10px;" +
                    "  background-color: white;" +
                    "  color: #202452;" +
                    "  border: 2px solid #202452;" +
                    "  padding: 8px 12px;" +
                    "  font-size: 14px;" +
                    "  font-weight: bold;" +
                    "  border-radius: 6px;" +
                    "  cursor: pointer;" +
                    "  text-decoration: none;" +
                    "  width: 220px;" +
                    "  justify-content: center;" +
                    "  transition: background-color 0.2s, color 0.2s;" +
                    "}" +
                    ".sso-login-button:hover {" +
                    "  background-color: #202452;" +
                    "  color: white;" +
                    "}" +
                    ".sso-login-button img {" +
                    "  height: 18px;" +
                    "  width: 18px;" +
                    "}" +
                    ".sso-divider-wrapper {" +
                    "  max-width: 280px;" + // limitar ancho
                    "  width: 100%;" +
                    "  margin: 10px auto 6px;" +
                    "}" +
                    ".sso-divider {" +
                    "  display: flex;" +
                    "  align-items: center;" +
                    "  text-align: center;" +
                    "  width: 100%;" +
                    "}" +
                    ".sso-divider span {" +
                    "  padding: 0 10px;" +
                    "  color: #888;" +
                    "  font-weight: 600;" +
                    "  font-size: 13px;" +
                    "}" +
                    ".sso-divider::before, .sso-divider::after {" +
                    "  content: \"\";" +
                    "  flex: 1;" +
                    "  border-bottom: 1px solid #ccc;" +
                    "}" +
                    "</style>" +

                    "<div class='sso-login-container'>" +
                    "<div class='sso-divider-wrapper'>" +
                    "<div class='sso-divider'><span>OR</span></div>" +
                    "</div>" +

                    "<a class='sso-login-button' href='http://localhost:9580/login?provider=google-oauth2&account_id=etendo_123&redirect_uri=http://localhost:8080/oauth/secureApp/LoginHandler.html'>" +
                    "<img src='https://cdn.jsdelivr.net/gh/devicons/devicon/icons/google/google-original.svg' alt='Google'/>Google" +
                    "</a>" +

                    "<a class='sso-login-button' href='http://localhost:9580/login?provider=windowslive&account_id=etendo_123&redirect_uri=http://localhost:8080/oauth/secureApp/LoginHandler.html'>" +
                    "<img src='https://upload.wikimedia.org/wikipedia/commons/4/44/Microsoft_logo.svg' alt='Microsoft'/>Microsoft" +
                    "</a>" +

                    "<a class='sso-login-button' href='http://localhost:9580/login?provider=linkedin&account_id=etendo_123&redirect_uri=http://localhost:8080/oauth/secureApp/LoginHandler.html'>" +
                    "<img src='https://cdn.jsdelivr.net/gh/devicons/devicon/icons/linkedin/linkedin-original.svg' alt='LinkedIn'/>LinkedIn" +
                    "</a>" +

                    "<a class='sso-login-button' href='http://localhost:9580/login?provider=github&account_id=etendo_123&redirect_uri=http://localhost:8080/oauth/secureApp/LoginHandler.html'>" +
                    "<img src='https://cdn.jsdelivr.net/gh/devicons/devicon/icons/github/github-original.svg' alt='GitHub'/>GitHub" +
                    "</a>" +

                    "<a class='sso-login-button' href='http://localhost:9580/login?provider=facebook&account_id=etendo_123&redirect_uri=http://localhost:8080/oauth/secureApp/LoginHandler.html'>" +
                    "<img src='https://cdn.jsdelivr.net/gh/devicons/devicon/icons/facebook/facebook-original.svg' alt='Facebook'/>Facebook" +
                    "</a>" +

                    "</div>";

        }
    }
}
