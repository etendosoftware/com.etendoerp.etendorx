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

    fetch(selectedRecord.authorizationEndpoint + '/available-providers')
      .then(response => response.json())
      .then(data => {
        const providers = Object.keys(data).map(providerKey => ({
          name: providerKey.charAt(0).toUpperCase() + providerKey.slice(1),
          authorizationEndpoint: selectedRecord.authorizationEndpoint + '/start',
          scopes: data[providerKey].scopes || [],
          redirectUri: selectedRecord.redirectURI
        }));

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

        const closeBtn = document.createElement('span');
        closeBtn.innerHTML = '&times;';
        closeBtn.style.position = 'absolute';
        closeBtn.style.top = '10px';
        closeBtn.style.right = '15px';
        closeBtn.style.fontSize = '24px';
        closeBtn.style.cursor = 'pointer';
        closeBtn.style.color = '#444';
        closeBtn.title = OB.I18N.getLabel('Close');;
        closeBtn.onclick = () => document.body.removeChild(modal);
        modalContent.appendChild(closeBtn);

        const title = document.createElement('h2');
        title.innerText = OB.I18N.getLabel('ETRX_SelectAProvider');
        title.style.color = '#222';
        title.style.marginTop = '0px';
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

          const providerTitle = document.createElement('h3');
          providerTitle.innerText = provider.name;
          providerTitle.style.marginBottom = '4px';
          providerTitle.style.marginTop = '0px';
          providerTitle.style.color = '#003366';
          providerTitle.style.fontSize = '20px';
          providerCard.appendChild(providerTitle);

          const providerDescription = document.createElement('p');
          providerDescription.innerText = OB.I18N.getLabel('ETRX_SelectScope');
          providerDescription.style.margin = '0 0 10px 0';
          providerDescription.style.fontSize = '13px';
          providerDescription.style.color = '#666';
          providerCard.appendChild(providerDescription);

          const horizontalDivider = document.createElement('hr');
          horizontalDivider.style.border = 'none';
          horizontalDivider.style.borderTop = '1px solid #ccc';
          horizontalDivider.style.margin = '10px 0';
          providerCard.appendChild(horizontalDivider);

          const scopeButtonsContainer = document.createElement('div');
          scopeButtonsContainer.style.display = 'flex';
          scopeButtonsContainer.style.flexWrap = 'wrap';
          scopeButtonsContainer.style.gap = '10px';

          provider.scopes.forEach(scopeData => {
            const { scope, iconUrl, description } = scopeData;

            const label = scope.includes('drive') ? 'Drive Files' :
                          scope.includes('calendar') ? 'Calendar' :
                          scope.includes('gmail') ? 'Gmail' :
                          scope;

            // contenedor del ítem
            const scopeItem = document.createElement('div');
            scopeItem.style.display = 'flex';
            scopeItem.style.flexDirection = 'column';
            scopeItem.style.alignItems = 'center';
            scopeItem.style.width = '64px';

            // botón ícono
            const button = document.createElement('button');
            button.style.width = '48px';
            button.style.height = '48px';
            button.style.border = 'none';
            button.style.borderRadius = '8px';
            button.style.backgroundColor = '#202452';
            button.style.cursor = 'pointer';
            button.style.display = 'flex';
            button.style.alignItems = 'center';
            button.style.justifyContent = 'center';
            button.title = description;

            const iconImg = document.createElement('img');
            iconImg.src = iconUrl;
            iconImg.alt = label + ' icon';
            iconImg.style.width = '24px';
            iconImg.style.height = '24px';

            button.appendChild(iconImg);

            // evento
            button.onclick = () => {
              document.body.removeChild(modal);
              const baseURL = provider.authorizationEndpoint +
                '?provider=' + encodeURIComponent(provider.name.toLowerCase()) +
                '&account_id=etendo_123' +
                '&scope=' + encodeURIComponent(scope) +
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

            // etiqueta debajo del botón
            const labelText = document.createElement('span');
            labelText.innerText = label;
            labelText.style.marginTop = '4px';
            labelText.style.fontSize = '12px';
            labelText.style.color = '#666';
            labelText.style.textAlign = 'center';

            scopeItem.appendChild(button);
            scopeItem.appendChild(labelText);
            scopeButtonsContainer.appendChild(scopeItem);

          });

          providerCard.appendChild(scopeButtonsContainer);
          modalContent.appendChild(providerCard);
        });

        const cancelBtn = document.createElement('button');
        cancelBtn.innerText = OB.I18N.getLabel('UINAVBA_Cancel');
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
      })
      .catch(err => {
        console.error(OB.I18N.getLabel('ETRX_ErrorFetchProviders'), err);
        this.processOwnerView.messageBar.setMessage('error', OB.I18N.getLabel('ETRX_ErrorFetchProviders'), OB.I18N.getLabel('ETRX_ErrorFetchProviders_Description'));
      });
  }
};
