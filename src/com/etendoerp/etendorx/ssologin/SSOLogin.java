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
        String domain = openbravoProperties.getProperty("sso.domain.url");
        String clientId = openbravoProperties.getProperty("sso.client.id");
        String redirectUri = openbravoProperties.getProperty("sso.callback.url");
        String sourceURL = openbravoProperties.getProperty("sso.source.url");

        if (StringUtils.isBlank(domain) || StringUtils.isBlank(clientId)
            || StringUtils.isBlank(redirectUri)|| StringUtils.isBlank(sourceURL)) {
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
            + "  webAuth.authorize({"
            + "  });"
            + "}"
            + "</script>"
            + "<button class=\"pure-button login-button ButtonLink ButtonLink_default\" onclick=\"loginWithSSO()\">" + loginButtonMessage + "</button>";

        return "<br>" + ssoButton;
    }
}
