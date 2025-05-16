OB.ETRX = OB.ETRX || {};
OB.ETRX.middlewareToken = {
  getMiddlewareToken: function (params, view) {
    const screenWidth = window.screen.width;
    const screenHeight = window.screen.height;
    const popupWidth = screenWidth * 0.5;
    const popupHeight = screenHeight * 0.5;
    const left = (screenWidth - popupWidth) / 2;
    const upperMargin = (screenHeight - popupHeight) / 2;

    const selectedRecord = params.button.contextView.viewGrid.getSelectedRecords()[0];
    const userId = OB.User.id;
    const state = crypto.randomUUID();
    const encodedState = btoa(JSON.stringify({ state, userId }));

    const providers = [
      {
        name: 'Google',
        authorizationEndpoint: selectedRecord.authorizationEndpoint,
        // TODO: Obtain the scopes dynamically from Middleware endpoint (maybe add an endpoint to the middleware to get the scopes).
        scope: [
          'https://www.googleapis.com/auth/spreadsheets',
          'openid',
          'profile',
          'email'
        ],
        redirectUri: selectedRecord.redirectURI
      }
      // Más proveedores en el futuro...
    ];

    const oldModal = document.getElementById('middleware-provider-modal');
    if (oldModal) oldModal.remove();

    const modal = document.createElement('div');
    modal.id = 'middleware-provider-modal';
    modal.style.position = 'fixed';
    modal.style.top = '0';
    modal.style.left = '0';
    modal.style.width = '100vw';
    modal.style.height = '100vh';
    modal.style.backgroundColor = 'rgba(0, 0, 0, 0.8)';
    modal.style.display = 'flex';
    modal.style.alignItems = 'center';
    modal.style.justifyContent = 'center';
    modal.style.zIndex = '999999';

    const modalContent = document.createElement('div');
    modalContent.style.backgroundColor = '#f5f5f5';
    modalContent.style.padding = '20px';
    modalContent.style.borderRadius = '10px';
    modalContent.style.minWidth = '400px';
    modalContent.style.maxWidth = '600px';
    modalContent.style.maxHeight = '80vh';
    modalContent.style.overflowY = 'auto';
    modalContent.style.position = 'relative';
    modalContent.style.boxShadow = '0 0 20px rgba(0,0,0,0.4)';
    modalContent.style.fontFamily = 'Arial, sans-serif';

    // Botón de cerrar
    const closeBtn = document.createElement('span');
    closeBtn.innerHTML = '&times;';
    closeBtn.style.position = 'absolute';
    closeBtn.style.top = '10px';
    closeBtn.style.right = '15px';
    closeBtn.style.fontSize = '24px';
    closeBtn.style.cursor = 'pointer';
    closeBtn.style.color = '#444';
    closeBtn.title = 'Cerrar';
    closeBtn.onclick = () => document.body.removeChild(modal);
    modalContent.appendChild(closeBtn);

    const title = document.createElement('h2');
    title.innerText = OB.I18N.getLabel('ETRX_SelectAProvider');
    title.style.color = '#222';
    title.style.borderBottom = '1px solid #ccc';
    title.style.paddingBottom = '10px';
    modalContent.appendChild(title);

    providers.forEach(provider => {
      const providerCard = document.createElement('div');
      providerCard.style.border = '1px solid #ddd';
      providerCard.style.borderRadius = '8px';
      providerCard.style.padding = '15px';
      providerCard.style.marginTop = '20px';
      providerCard.style.backgroundColor = '#fff';
      providerCard.style.boxShadow = '2px 2px 10px rgba(0,0,0,0.1)';

      const providerTitle = document.createElement('h4');
      providerTitle.innerText = provider.name;
      providerTitle.style.marginBottom = '10px';
      providerTitle.style.color = '#003366';
      providerCard.appendChild(providerTitle);

      const scopeContainer = document.createElement('div');
      provider.scope.forEach(scope => {
        const label = document.createElement('label');
        label.style.display = 'block';
        label.style.marginBottom = '5px';
        label.style.fontSize = '14px';

        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';
        checkbox.value = scope;
        checkbox.style.marginRight = '8px';

        label.appendChild(checkbox);
        label.appendChild(document.createTextNode(scope));
        scopeContainer.appendChild(label);
      });

      providerCard.appendChild(scopeContainer);

      const authButton = document.createElement('button');
      authButton.innerText = OB.I18N.getLabel('ETRX_AuthWith').replace('%s', provider.name);
      authButton.style.marginTop = '10px';
      authButton.style.padding = '8px 16px';
      authButton.style.backgroundColor = '#FFCC00';
      authButton.style.border = 'none';
      authButton.style.borderRadius = '4px';
      authButton.style.cursor = 'pointer';
      authButton.style.color = '#000';
      authButton.style.fontWeight = 'bold';

      authButton.onclick = () => {
        const selectedScopes = Array.from(scopeContainer.querySelectorAll('input:checked')).map(cb => cb.value);
        if (selectedScopes.length === 0) {
        // TODO: Better way to check for empty scopes.
          alert('Debes seleccionar al menos un scope.');
          return;
        }

        document.body.removeChild(modal);

        const baseURL = provider.authorizationEndpoint +
          '?provider=' + encodeURIComponent(provider.name.toLowerCase()) +
          // TODO: Change the wat to get the accountID.
          '&account_id=etendo_123' +
          '&scope=' + encodeURIComponent(selectedScopes.join(' ')) +
          '&redirect_uri=' + encodeURIComponent(provider.redirectUri);

        const separator = baseURL.includes('?') ? '&' : '?';
        const popUpURL = baseURL + separator + `state=${encodedState}`;

        const sizeProperties = `width=${popupWidth},height=${popupHeight},left=${left},top=${upperMargin}`;
        const popup = window.open(popUpURL, 'Authentication Popup', sizeProperties);
        if (!popup) {
          console.error(OB.I18N.getLabel('ETRX_PopupNotBeOpened'));
          alert(OB.I18N.getLabel('ETRX_PopupNotBeOpened'));
        }
      };

      providerCard.appendChild(authButton);
      modalContent.appendChild(providerCard);
    });

    // Botón cancelar
    const cancelBtn = document.createElement('button');
    cancelBtn.innerText = 'Cancelar';
    cancelBtn.style.marginTop = '20px';
    cancelBtn.style.padding = '8px 16px';
    cancelBtn.style.backgroundColor = '#ccc';
    cancelBtn.style.border = 'none';
    cancelBtn.style.borderRadius = '4px';
    cancelBtn.style.cursor = 'pointer';
    cancelBtn.onclick = () => document.body.removeChild(modal);
    modalContent.appendChild(cancelBtn);

    modal.appendChild(modalContent);
    document.body.appendChild(modal);
  }
};
