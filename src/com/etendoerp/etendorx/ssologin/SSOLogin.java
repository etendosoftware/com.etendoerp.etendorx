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
            String ssoLoginUrl = OBPropertiesProvider.getInstance().getOpenbravoProperties()
                    .getProperty("sso.middleware.url") + "/login";

            String redirectUri = OBPropertiesProvider.getInstance().getOpenbravoProperties()
                    .getProperty("sso.middleware.redirectUri");

            String divider =
                "<style>" +
                    ".sso-divider-wrapper {" +
                    "  max-width: 280px;" +
                    "  width: 100%;" +
                    "  margin: 24px auto 12px;" +
                    "}" +
                    ".sso-divider {" +
                    "  display: flex;" +
                    "  align-items: center;" +
                    "  text-align: center;" +
                    "  width: 100%;" +
                    "  color: #999;" +
                    "  font-weight: 600;" +
                    "  font-size: 13px;" +
                    "}" +
                    ".sso-divider::before, .sso-divider::after {" +
                    "  content: \"\";" +
                    "  flex: 1;" +
                    "  border-bottom: 1px solid #ccc;" +
                    "}" +
                    ".sso-divider span {" +
                    "  padding: 0 12px;" +
                    "}" +
                    "</style>" +
                    "<div class='sso-divider-wrapper'>" +
                    "  <div class='sso-divider'><span>OR</span></div>" +
                    "</div>";

            return divider +
                "<style>" +
                ".sso-icon-container {" +
                "  display: flex;" +
                "  justify-content: space-between;" +
                "  gap: 12px;" +
                "  width: 300px;" +
                "  margin: 12px auto 0;" +
                "}" +
                ".sso-icon-button {" +
                "  width: 50px;" +
                "  height: 50px;" +
                "  background-color: #202452;" +
                "  border-radius: 6px;" +
                "  display: flex;" +
                "  justify-content: center;" +
                "  align-items: center;" +
                "  cursor: pointer;" +
                "  transition: background-color 0.2s;" +
                "}" +
                ".sso-icon-button:hover {" +
                "  background-color: #1a1d3d;" +
                "}" +
                ".sso-icon-button img {" +
                "  height: 24px;" +
                "}" +
                "</style>" +

                "<div class='sso-icon-container'>" +

                "<a class='sso-icon-button' href='" + ssoLoginUrl + "?provider=google-oauth2&account_id=etendo_123&redirect_uri=" + redirectUri + "'>" +
                "  <img src='../web/com.etendoerp.etendorx/images/google.png' alt='Google'>" +
                "</a>" +

                "<a class='sso-icon-button' href='" + ssoLoginUrl + "?provider=windowslive&account_id=etendo_123&redirect_uri=" + redirectUri + "'>" +
                "  <img src='../web/com.etendoerp.etendorx/images/microsoft.png' alt='Microsoft'>" +
                "</a>" +

                "<a class='sso-icon-button' href='" + ssoLoginUrl + "?provider=linkedin&account_id=etendo_123&redirect_uri=" + redirectUri + "'>" +
                "  <img src='../web/com.etendoerp.etendorx/images/linkedin.png' alt='LinkedIn'>" +
                "</a>" +

                "<a class='sso-icon-button' href='" + ssoLoginUrl + "?provider=github&account_id=etendo_123&redirect_uri=" + redirectUri + "'>" +
                "  <img src='../web/com.etendoerp.etendorx/images/github.png' alt='GitHub'>" +
                "</a>" +

                "<a class='sso-icon-button' href='" + ssoLoginUrl + "?provider=facebook&account_id=etendo_123&redirect_uri=" + redirectUri + "'>" +
                "  <img src='../web/com.etendoerp.etendorx/images/facebook.png' alt='Facebook'>" +
                "</a>" +

                "</div>";
        }
    }
}
