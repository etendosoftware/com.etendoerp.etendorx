OB.ETRX = OB.ETRX || {};
OB.ETRX.openGooglePickerPopup = async function (processEndpointName) {
  const screenWidth = window.screen.width;
  const screenHeight = window.screen.height;
  const popupWidth = screenWidth * 0.5;
  const popupHeight = screenHeight * 0.5;
  const left = (screenWidth - popupWidth) / 2;
  const upperMargin = (screenHeight - popupHeight) / 2;
  const sizeProperties = `width=${popupWidth},height=${popupHeight},left=${left},top=${upperMargin}`;

  try {
    const envURL = OB.Utilities.getLocationUrlWithoutFragment();
    const tokenRes = await fetch(envURL + 'GetOAuthToken');
    const { accessToken } = await tokenRes.json();

    OB.RemoteCallManager.call(
      'com.etendoerp.etendorx.GetSSOProperties',
      { properties: 'google.api.key, google.api.id' },
      {},
      function (response, data) {
        if (data.message?.severity === 'error') {
          this.processOwnerView.messageBar.setMessage('error', OB.I18N.getLabel('ETRX_SSOError'), data.message.text);
          return;
        }

        const googleApiKey = data.googleapikey;
        const googleApiId = data.googleapiid;

        const titleText = encodeURIComponent(OB.I18N.getLabel('ETRX_SelectGDriveFile'));
        const buttonText = encodeURIComponent(OB.I18N.getLabel('ETRX_SelectGDriveFile'));
        const successMessage = encodeURIComponent(OB.I18N.getLabel('ETRX_SuccessFileApprove'));

        const win = window.open(
          envURL +
            `web/com.etendoerp.entendorx/js/EtendoPicker/picker.html?` +
            `accessToken=${encodeURIComponent(accessToken)}&` +
            `envURL=${encodeURIComponent(envURL)}&` +
            `googleApiKey=${encodeURIComponent(googleApiKey)}&` +
            `googleApiId=${encodeURIComponent(googleApiId)}&` +
            `titleText=${titleText}&` +
            `buttonText=${buttonText}&` +
            `successMessage=${successMessage}&` +
            `processEndpoint=${encodeURIComponent(processEndpointName)}`,
          'GooglePicker',
          sizeProperties
        );

        if (!win) {
          OB.ETRX.processOwnerView.messageBar.setMessage('error', OB.I18N.getLabel('ETRX_PopUpBlocked'), OB.I18N.getLabel('ETRX_AllowPopUp'));
        }
      }.bind(this)
    );
  } catch (error) {
    this.processOwnerView.messageBar.setMessage('error', OB.I18N.getLabel('ETRX_TokenError'), error.message);
    console.error(error);
  }
};
