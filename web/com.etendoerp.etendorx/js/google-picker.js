// --- Helpers de UI súper minimalistas para el popup ---
function popupRenderLoading(win, text = 'Loading…') {
  if (!win || win.closed) return;
  win.document.open();
  win.document.write(`
    <meta charset="utf-8" />
    <title>${text}</title>
    <style>
      html,body { height:100%; margin:0; font-family: system-ui, -apple-system, Segoe UI, Roboto, Ubuntu, Arial, sans-serif; background:#0b0f19; color:#e6eaf2; }
      .wrap { height:100%; display:grid; place-items:center; }
      .card { text-align:center; padding:24px 28px; border:1px solid #1f2a3a; border-radius:14px; background:#0f1523; box-shadow:0 10px 30px rgba(0,0,0,.35); min-width:280px }
      .sp { width:34px; height:34px; border:3px solid #1f2a3a; border-top-color:#6ea8fe; border-radius:50%; margin:0 auto 12px; animation:rot 1s linear infinite }
      .t1 { font-size:15px; font-weight:600; margin:0 }
      .t2 { font-size:13px; opacity:.7; margin:6px 0 0 }
      @keyframes rot { to { transform: rotate(360deg) } }
    </style>
    <div class="wrap">
      <div class="card">
        <div class="sp"></div>
        <p class="t1">${text}</p>
        <p class="t2">${OB.I18N.getLabel('OBUIAPP_PROCESSING')}</p>
      </div>
    </div>
  `);
  win.document.close();
}

function popupRenderError(win, title, message) {
  if (!win || win.closed) return;
  win.document.open();
  win.document.write(`
    <meta charset="utf-8" />
    <title>${title || 'Error'}</title>
    <style>
      html,body { height:100%; margin:0; font-family: system-ui, -apple-system, Segoe UI, Roboto, Ubuntu, Arial, sans-serif; background:#0b0f19; color:#e6eaf2; }
      .wrap { height:100%; display:grid; place-items:center; }
      .card { text-align:center; padding:22px 24px; border:1px solid #1f2a3a; border-radius:14px; background:#0f1523; box-shadow:0 10px 30px rgba(0,0,0,.35); min-width:320px; max-width:520px }
      .ico { font-size:28px; margin-bottom:8px }
      .t1 { font-size:16px; font-weight:700; margin:0 0 4px }
      .t2 { font-size:13px; opacity:.78; margin:0 0 14px; white-space:pre-wrap }
      .row { display:flex; gap:8px; justify-content:center }
      button { appearance:none; border:1px solid #2a3850; background:#131a2b; color:#e6eaf2; border-radius:10px; padding:9px 12px; font-size:13px; cursor:pointer }
    </style>
    <div class="wrap" role="alert" aria-live="assertive">
      <div class="card">
        <div class="ico">⚠️</div>
        <p class="t1">${title || (OB.I18N.getLabel('ETRX_GooglePickerError') || 'Error')}</p>
        <p class="t2">${message || (OB.I18N.getLabel('ETRX_TokenError') || '')}</p>
        <div class="row">
          <button onclick="window.close()">${OB.I18N.getLabel('OBUIAPP_Close') || 'Cerrar'}</button>
        </div>
      </div>
    </div>
  `);
  win.document.close();
}

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
          reject(new Error(OB.I18N.getLabel('ETRX_ErrorRetrievingMiddlewareURL')));
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

  const popup = window.open('about:blank', 'GooglePicker', sizeProperties);
  if (!popup) {
    this?.processOwnerView?.messageBar?.setMessage?.(
      'error',
      OB.I18N.getLabel('ETRX_GooglePickerError'),
      OB.I18N.getLabel('ETRX_PopUpBlocked')
    );
    return;
  }
  popupRenderLoading(popup, OB.I18N.getLabel('ETRX_StartingSSO'));

  try {
    const envURL = OB.Utilities.getLocationUrlWithoutFragment();

    const tokenRes = await fetch(envURL + 'GetOAuthToken', { headers: { 'Accept': 'application/json' } });

    if (!tokenRes.ok) {
      let backendMsg = '';
      try {
        const ct = tokenRes.headers.get('content-type') || '';
        if (ct.includes('application/json')) {
          const j = await tokenRes.json();
          backendMsg = j?.message || j?.error || tokenRes.statusText;
        } else {
          backendMsg = (await tokenRes.text()) || tokenRes.statusText;
        }
      } catch (_) { /* ignore */ }

      const errText = backendMsg || `HTTP ${tokenRes.status}`;
      popupRenderError(
        popup,
        OB.I18N.getLabel('ETRX_TokenError'),
        errText + OB.I18N.getLabel('ETRX_RefreshTokenManually')
      );
      this?.processOwnerView?.messageBar?.setMessage?.(
        'error',
        OB.I18N.getLabel('ETRX_TokenError'),
        errText + OB.I18N.getLabel('ETRX_RefreshTokenManually')
      );
      return;
    }

    const ct = tokenRes.headers.get('content-type') || '';
    if (!ct.includes('application/json')) {
      const raw = await tokenRes.text();
      popupRenderError(
        popup,
        OB.I18N.getLabel('ETRX_TokenError'),
        raw || (OB.I18N.getLabel('ETRX_TokenError'))
      );
      this?.processOwnerView?.messageBar?.setMessage?.(
        'error',
        OB.I18N.getLabel('ETRX_TokenError'),
        raw
      );
      return;
    }

    const data = await tokenRes.json();
    const sessionToken = data?.accessToken;
    if (!sessionToken) {
      const msg = OB.I18N.getLabel('ETRX_TokenError');
      popupRenderError(
        popup,
        OB.I18N.getLabel('ETRX_TokenError'),
        msg
      );
      this?.processOwnerView?.messageBar?.setMessage?.('error', OB.I18N.getLabel('ETRX_TokenError'), msg);
      return;
    }

    const ssoBase = ssoBaseFromArg || await getMiddlewareURL();

    const returnOrigin = window.location.origin;
    const titleText = encodeURIComponent(OB.I18N.getLabel('ETRX_SelectGDriveFile'));
    const buttonText = encodeURIComponent(OB.I18N.getLabel('ETRX_SelectGDriveFile'));
    const successMessage = encodeURIComponent(OB.I18N.getLabel('ETRX_SuccessFileApprove'));

    let url = `${ssoBase}/picker?session=${encodeURIComponent(sessionToken)}&returnOrigin=${encodeURIComponent(returnOrigin)}&titleText=${titleText}&buttonText=${buttonText}&successMessage=${successMessage}`;
    if (processEndpointName) url += `&processEndpoint=${encodeURIComponent(processEndpointName)}`;

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
                  OB.I18N.getLabel('ETRX_GooglePickerError'),
                  result.message
                );
              }
            })
            .catch(err => {
              this.processOwnerView.messageBar.setMessage(
                'error',
                OB.I18N.getLabel('ETRX_GooglePickerError'),
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

    popup.location.replace(url);

  } catch (error) {
    popupRenderError(
      popup,
      OB.I18N.getLabel('ETRX_GooglePickerError'),
      error?.message || ''
    );
    this?.processOwnerView?.messageBar?.setMessage?.(
      'error',
      OB.I18N.getLabel('ETRX_GooglePickerError'),
      error?.message || ''
    );
    console.error(error);
  }
};
