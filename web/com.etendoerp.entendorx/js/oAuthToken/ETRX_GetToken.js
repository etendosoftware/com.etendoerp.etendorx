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

		    // Generate a 'state' to keep userId and providerId
        const selectedRecord = params.button.contextView.viewGrid.getSelectedRecords()[0];
        const state = crypto.randomUUID();
        const userId = OB.User.id;
        const etrxOauthProviderId = selectedRecord.id;
        const encodedState = btoa(JSON.stringify({ state, userId, etrxOauthProviderId }));

        // Function to open the popup window
        const baseURL = selectedRecord.authorizationEndpoint;
        const separator = baseURL.includes('?') ? '&' : '?';
        const popUpURL = baseURL + separator + `state=${encodedState}`;

		const sizeProperties = 'width=' + popupWidth + ',height=' + popupHeight + ',left=' + left + ',top=' + upperMargin;
        const popup = window.open(popUpURL, 'Authentication Popup', sizeProperties);
        if (!popup) {
            console.error(OB.I18N.getLabel('ETRX_PopupNotBeOpened'));
            alert(OB.I18N.getLabel('ETRX_PopupNotBeOpened'));
        }
    }
};