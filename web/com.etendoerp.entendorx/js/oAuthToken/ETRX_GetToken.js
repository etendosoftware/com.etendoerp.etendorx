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

        function handleMessage(title, text, type) {
            view.messageBar.setMessage(type, title, text);
        }

		function callbackOnProcessActionHandler(response, data, request) {
            if (data.message?.severity === 'error') {
                this.getWindow().showMessage(data.message.text)
            } else {
                const popup = window.open(data.auth_url, 'Authentication Popup', 'width=' + popupWidth + ',height=' + popupHeight + ',left=' + left + ',top=' + upperMargin);
                if (!popup) {
                    console.error(OB.I18N.getLabel('ETRX_PopupNotBeOpened'));
                    alert(OB.I18N.getLabel('ETRX_PopupNotBeOpened'));
                }
            }
        }
        // Function to open the popup window
        const selectedRecord = params.button.contextView.viewGrid.getSelectedRecords()[0];
        OB.RemoteCallManager.call(
            'com.etendoerp.etendorx.GetTokenURL',
            {
                id: selectedRecord.id
            },
            {},
            callbackOnProcessActionHandler
        );
    }
};