OB.ETRX = OB.ETRX || {};
OB.ETRX.middlewareToken = {
  getMiddlewareToken: function (params, view) {
    const screenWidth = window.screen.width;
    const screenHeight = window.screen.height;
    const popupWidth = Math.round(screenWidth * 0.5);
    const popupHeight = Math.round(screenHeight * 0.5);
    const left = Math.round((screenWidth - popupWidth) / 2);
    const upperMargin = Math.round((screenHeight - popupHeight) / 2);

    const selectedRecord = params.button.contextView.viewGrid.getSelectedRecords()[0];
    if (!selectedRecord) {
      console.error('No hay registro seleccionado');
      return;
    }

    const userId = OB.User.id;
    const state = crypto.randomUUID();
    const encodedState = btoa(JSON.stringify({ state, userId }));

    fetch(selectedRecord.authorizationEndpoint + '/available-providers')
      .then(async (response) => {
        if (!response.ok) throw new Error('HTTP ' + response.status);
        return response.json();
      })
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

        const modalContent = document.createElement('div');
        modalContent.className = 'middleware-modal-content';

        const closeBtn = document.createElement('span');
        closeBtn.innerHTML = '&times;';
        closeBtn.className = 'middleware-close-btn';
        closeBtn.title = OB.I18N.getLabel('Close');
        closeBtn.onclick = () => document.body.removeChild(modal);
        modalContent.appendChild(closeBtn);

        const title = document.createElement('h2');
        title.innerText = OB.I18N.getLabel('ETRX_SelectAProvider');
        title.className = 'middleware-modal-title';
        modalContent.appendChild(title);

        providers.forEach(provider => {
          const providerCard = document.createElement('div');
          providerCard.className = 'middleware-provider-card';

          const providerTitle = document.createElement('h3');
          providerTitle.innerText = provider.name;
          providerTitle.className = 'middleware-provider-title';
          providerCard.appendChild(providerTitle);

          const providerDescription = document.createElement('p');
          providerDescription.innerText = OB.I18N.getLabel('ETRX_SelectScope');
          providerDescription.className = 'middleware-provider-description';
          providerCard.appendChild(providerDescription);

          const horizontalDivider = document.createElement('hr');
          horizontalDivider.className = 'middleware-divider';
          providerCard.appendChild(horizontalDivider);

          const scopeButtonsContainer = document.createElement('div');
          scopeButtonsContainer.className = 'middleware-scope-container';

          provider.scopes.forEach(scopeData => {
            const { scope: rawScope, iconUrl = '', description = '' } = scopeData || {};
            const scope = typeof rawScope === 'string' ? rawScope : String(rawScope || '');

            const labelMap = { drive: 'Drive Files', calendar: 'Calendar', gmail: 'Gmail' };
            const matchedKey = Object.keys(labelMap).find(key => scope.includes(key));
            const label = matchedKey ? labelMap[matchedKey] : scope || 'Scope';

            const scopeItem = document.createElement('div');
            scopeItem.className = 'middleware-scope-item';

            const button = document.createElement('button');
            button.className = 'middleware-scope-button';
            button.title = description;

            const iconImg = document.createElement('img');
            iconImg.src = iconUrl;
            iconImg.alt = label + ' icon';
            iconImg.className = 'middleware-scope-icon';

            button.appendChild(iconImg);

            button.onclick = async () => {
              const sizeProperties = `width=${popupWidth},height=${popupHeight},left=${left},top=${upperMargin}`;
              const popup = window.open('', 'Authentication Popup', sizeProperties);
              if (!popup) {
                console.error(OB.I18N.getLabel('ETRX_PopupNotBeOpened'));
                alert(OB.I18N.getLabel('ETRX_PopupNotBeOpened'));
                return;
              }

              let accountId = '';
              try {
                accountId = await OB.ETRX.middlewareToken.getAccount();
              } catch (e) {
                console.error(OB.I18N.getLabel('ETRX_ErrorFetchAccount'), e);
                this.processOwnerView?.messageBar?.setMessage?.(
                  'error',
                  OB.I18N.getLabel('ETRX_ErrorFetchAccount'),
                  OB.I18N.getLabel('ETRX_ErrorFetchAccount_Description')
                );
                popup.close();
                return;
              }

              document.body.removeChild(modal);

              const qs = new URLSearchParams({
                provider: provider.name.toLowerCase(),
                account_id: accountId,
                scope,
                redirect_uri: provider.redirectUri,
                state: encodedState
              });

              popup.location.href = `${provider.authorizationEndpoint}?${qs.toString()}`;
            };

            const labelText = document.createElement('span');
            labelText.innerText = label;
            labelText.className = 'middleware-scope-label';

            scopeItem.appendChild(button);
            scopeItem.appendChild(labelText);
            scopeButtonsContainer.appendChild(scopeItem);
          });

          providerCard.appendChild(scopeButtonsContainer);
          modalContent.appendChild(providerCard);
        });

        const cancelBtn = document.createElement('button');
        cancelBtn.innerText = OB.I18N.getLabel('UINAVBA_Cancel');
        cancelBtn.className = 'middleware-cancel-btn';
        cancelBtn.onclick = () => document.body.removeChild(modal);
        modalContent.appendChild(cancelBtn);

        modal.appendChild(modalContent);
        document.body.appendChild(modal);
      })
      .catch(err => {
        console.error(OB.I18N.getLabel('ETRX_ErrorFetchProviders'), err);
        this.processOwnerView?.messageBar?.setMessage?.(
          'error',
          OB.I18N.getLabel('ETRX_ErrorFetchProviders'),
          OB.I18N.getLabel('ETRX_ErrorFetchProviders_Description')
        );
      });
  }, 
  
  getAccount: function () {
    return new Promise((resolve, reject) => {
      OB.RemoteCallManager.call(
        'com.etendoerp.etendorx.GetSSOProperties',
        { properties: 'account' },
        {},
        function callbackOnProcessActionHandler(response, data) {
          if (data?.message?.severity === 'error') {
            reject(new Error(data.message.text || 'Account fetch error'));
          } else {
            resolve(data?.account || '');
          }
        }
      );
    });
  }
};
