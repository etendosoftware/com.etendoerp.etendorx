function getMiddlewareURL() {
  return new Promise((resolve, reject) => {
    const onDone = function (response, data) {
      try {
        if (data?.message?.severity === 'error') {
          OB?.App?.ViewMgr?.getWindow?.()?.showMessage?.(data.message.text);
          reject(new Error(data.message.text));
          return;
        }
        const middlewareURL = data?.middlewareurl || data?.middlewareUrl;
        if (!middlewareURL) {
          reject(new Error(OB.I18N.getLabel('ETRX_ErrorRetrievingMiddlewareURL'))); // 'No se recibió "middlewareurl" en la respuesta.'
          return;
        }
        resolve(middlewareURL);
      } catch (e) {
        reject(e);
      }
    };

    OB.RemoteCallManager.call(
      'com.etendoerp.etendorx.GetSSOProperties',
      { properties: 'middleware.url' },
      {},
      onDone
    );
  });
}

OB.ETRX.openGooglePickerPopup = async function (processEndpointName, ssoBaseFromArg) {
  const screenWidth = window.screen.width;
  const screenHeight = window.screen.height;
  const popupWidth = screenWidth * 0.5;
  const popupHeight = screenHeight * 0.5;
  const left = (screenWidth - popupWidth) / 2;
  const upperMargin = (screenHeight - popupHeight) / 2;
  const sizeProperties = `width=${popupWidth},height=${popupHeight},left=${left},top=${upperMargin}`;

  try {
    const ssoBase = ssoBaseFromArg || await getMiddlewareURL();

    const envURL = OB.Utilities.getLocationUrlWithoutFragment();

    const tokenRes = await fetch(envURL + 'GetOAuthToken');
    const { accessToken: sessionToken } = await tokenRes.json();

    const returnOrigin = window.location.origin;
    const titleText = encodeURIComponent(OB.I18N.getLabel('ETRX_SelectGDriveFile'));
    const buttonText = encodeURIComponent(OB.I18N.getLabel('ETRX_SelectGDriveFile'));
    const successMessage = encodeURIComponent(OB.I18N.getLabel('ETRX_SuccessFileApprove'));

    let url = `${ssoBase}/picker?session=${encodeURIComponent(sessionToken)}&returnOrigin=${encodeURIComponent(returnOrigin)}&titleText=${titleText}&buttonText=${buttonText}&successMessage=${successMessage}`;
    if (processEndpointName) url += `&processEndpoint=${encodeURIComponent(processEndpointName)}`;

    const win = window.open(url, 'GooglePicker', sizeProperties);

    const ssoOrigin = new URL(ssoBase, window.location.href).origin;

    window.addEventListener('message', (evt) => {
      if (evt.origin !== ssoOrigin) return;
      const { type, payload } = evt.data || {};
      if (type === 'PICKER_SUCCESS') {
        if (payload?.processEndpoint) {
          fetch(payload.processEndpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload.doc)
          })
            .then(r => r.json())
            .then(result => {
              if (result.status === 'ok') {
                this.processOwnerView.messageBar.setMessage(
                  'success',
                  OB.I18N.getLabel('ETRX_SuccessFileApprove'),
                  ''
                );
              } else {
                this.processOwnerView.messageBar.setMessage(
                  'error',
                  OB.I18N.getLabel('ETRX_SSOError'),
                  result.message || 'Unknown error'
                );
              }
            })
            .catch(err => {
              this.processOwnerView.messageBar.setMessage(
                'error',
                OB.I18N.getLabel('ETRX_SSOError'),
                err.message
              );
            });
        } else {
          this.processOwnerView.messageBar.setMessage(
            'success',
            OB.I18N.getLabel('ETRX_SuccessFileApprove'),
            ''
          );
        }
      }
    }, { once: true });

  } catch (error) {
    this?.processOwnerView?.messageBar?.setMessage?.('error', OB.I18N.getLabel('ETRX_TokenError'), error.message);
    console.error(error);
  }
};
