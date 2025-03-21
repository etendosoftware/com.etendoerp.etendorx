function doLogin(command) {
  doLoginWithToken()
  var extraParams;
  if (
    document.getElementById('resetPassword').value === 'true' &&
    document.getElementById('newPass').value !==
      document.getElementById('password').value
  ) {
    setLoginMessage('Error', errorSamePassword, errorDifferentPasswordInFields);
    return true;
  }
  if (
    focusedWindowElement.id === 'user' &&
    document.getElementById('user').value !== '' &&
    document.getElementById('password').value === ''
  ) {
    setTimeout(function() {
      // To manage browser autocomplete feature if it is active
      if (
        focusedWindowElement.id === 'user' &&
        document.getElementById('password').value === ''
      ) {
        setWindowElementFocus(document.getElementById('password'));
      } else {
        return true;
      }
    }, 10);
  } else if (
    focusedWindowElement.id === 'password' &&
    document.getElementById('password').value !== '' &&
    document.getElementById('user').value === ''
  ) {
    setWindowElementFocus(document.getElementById('user'));
  } else {
    if (
      document.getElementById('user').value === '' ||
      document.getElementById('password').value === ''
    ) {
      setLoginMessage('Error', identificationFailureTitle, errorEmptyContent);
      return true;
    }
    disableButton('buttonOK');
    command =
      command ||
      (document.getElementById('resetPassword').value === 'true'
        ? 'FORCE_RESET_PASSWORD'
        : 'DEFAULT');
    extraParams = '&targetQueryString=' + getURLQueryString();
    submitXmlHttpRequest(
      loginResult,
      document.frmIdentificacion,
      command,
      '../secureApp/LoginHandler.html',
      false,
      extraParams,
      null
    );
  }
  return false;
}


function doLoginWithToken() {
  const params = new URLSearchParams(window.location.hash.substring(1));
  const accessToken = params.get("access_token");

  if (accessToken) {
    var xhr = new XMLHttpRequest();
    const path = window.location.pathname;
    const context = path.split('/')[1];
    var loginUrl = "../../" + context + "/org.openbravo.client.kernel/OBCLKER_Kernel/SessionDynamic";
    var requestData = new FormData();

    requestData.append("access_token", accessToken);
    requestData.append("targetQueryString", getURLQueryString());

    xhr.open("POST", loginUrl, true);
    xhr.setRequestHeader("Authorization", "Bearer " + accessToken);

    xhr.onreadystatechange = function () {
      if (xhr.readyState === 4) {
        if (xhr.status >= 200 && xhr.status < 300) {
          console.log("Login success:", xhr.responseText);
        } else {
          console.error("HTTP error! Status:", xhr.status, xhr.responseText);
        }
      }
    };
    xhr.send(requestData);
  }
}
