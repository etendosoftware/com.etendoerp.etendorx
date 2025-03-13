OB.ETRX = OB.ETRX || {};
OB.ETRX.oAuthToken = {
    /**
     * This function is responsible for opening a popup with the URL returned by the utility.
     *
     * @param {*} params
     * @param {*} view
     */
    getToken: function(params, view) {
        //Center the popup window on the screen
        const screenWidth = window.screen.width;
        const screenHeight = window.screen.height;
        const popupWidth = screenWidth * 0.5;
        const popupHeight = screenHeight * 0.5;
        const left = (screenWidth - popupWidth) / 2;
        const upperMargin = (screenHeight - popupHeight) / 2;

        // Function to open the popup window
        const selectedRecord = params.button.contextView.viewGrid.getSelectedRecords()[0];
        const popUpURL = selectedRecord.authorizationEndpoint + '?userId=' + OB.User.id + '&etrxOauthProviderId=' + selectedRecord.id;
        const popup = window.open(popUpURL, 'Authentication Popup', 'width=' + popupWidth + ',height=' + popupHeight + ',left=' + left + ',top=' + upperMargin);
        if (!popup) {
            console.error(OB.I18N.getLabel('ETRX_PopupNotBeOpened'));
            alert(OB.I18N.getLabel('ETRX_PopupNotBeOpened'));
        }
    }
};