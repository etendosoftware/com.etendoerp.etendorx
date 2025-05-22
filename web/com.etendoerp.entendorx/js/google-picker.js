OB.ETRX = OB.ETRX || {};
OB.ETRX.openPickerPopup = async function () {
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

    const win = window.open(
      envURL + `web/com.etendoerp.entendorx/js/EtendoPicker/picker.html?accessToken=${encodeURIComponent(accessToken)}&envURL=${encodeURIComponent(envURL)}`,
      'GooglePicker',
      sizeProperties
    );

    if (!win) {
      // TODO: Improve error handling
      alert('El popup fue bloqueado. Permití popups para este sitio.');
    }
  } catch (error) {
    // TODO: Improve error handling
    alert('Error al obtener el token de acceso.');
    console.error(error);
  }
};
