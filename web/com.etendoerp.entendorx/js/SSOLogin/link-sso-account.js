if (OB.PropertyStore.get('ETRX_AllowSSOLogin') === 'Y') {
  function createAuth0LinkButton(domainUrl, clientId) {
    const button = isc.OBFormButton.create({
      title: OB.I18N.getLabel('ETRX_LinkSSOAccount'),
      click: function () {
        function initAuth0() {
          let webAuth = new auth0.WebAuth({
            domain: domainUrl,
            clientID: clientId,
            redirectUri: OB.Utilities.getLocationUrlWithoutFragment() + 'web/com.etendoerp.etendorx/LinkAuth0Account.html',
            responseType: 'code',
            scope: 'openid profile email'
          });
          webAuth.authorize({ prompt: 'login' });
        }

        if (typeof auth0 === "undefined") {
          let script = document.createElement("script");
          script.src = OB.Utilities.getLocationUrlWithoutFragment() + 'web/com.etendoerp.entendorx/js/SSOLogin/auth0.min.js';
          script.onload = initAuth0;
          document.head.appendChild(script);
        } else {
          initAuth0();
        }
      },
      baseStyle: "OBFormButton",
      height: 50,
      wrap: true,
      autoFit: true
    });

    button.setWidth(170);
    return isc.HStack.create({
      align: 'center',
      width: '100%',
      members: [button]
    });
  }

  function createProviderIconLayout(middlewareURL) {
    const layout = isc.VStack.create({
      align: 'center',
      width: '100%'
    });

    layout.addMember(isc.Label.create({
      contents: "Link user with:",
      styleName: "OBFormFieldLabel",
      align: "center",
      width: "100%",
      layoutTopMargin: 4,
      layoutBottomMargin: 2,
      height: 20,
      padding: 0,
      margin: 0,
      overflow: 'hidden',
      showEdges: false
    }));

    const iconLayout = isc.HStack.create({
      align: 'center',
      layoutTopMargin: 0,
      layoutBottomMargin: 0,
      membersMargin: 6
    });

    const providerIcons = [
      { providerText: 'Google', provider: 'google-oauth2', img: '../web/com.etendoerp.entendorx/images/google-color.png' },
      { providerText: 'Facebook', provider: 'facebook', img: '../web/com.etendoerp.entendorx/images/facebook-color.png' },
      { providerText: 'GitHub', provider: 'github', img: '../web/com.etendoerp.entendorx/images/github-color.png' },
      { providerText: 'Microsoft', provider: 'windowslive', img: '../web/com.etendoerp.entendorx/images/microsoft-color.png' },
      { providerText: 'LinkedIn', provider: 'linkedin', img: '../web/com.etendoerp.entendorx/images/linkedin-color.png' }
    ];

    providerIcons.forEach(p => {
      const provider = p.provider;
      const img = p.img;
      const providerText = p.providerText;

      iconLayout.addMember(isc.ImgButton.create({
        width: 32,
        height: 32,
        src: img,
        prompt: 'Link with ' + providerText,
        showRollOver: false,
        showDown: false,
        showFocused: false,
        layoutAlign: 'center',
        baseStyle: 'ssoProviderIcon',
        click: function () {
          const redirectUri = OB.Utilities.getLocationUrlWithoutFragment();
          const url = `${middlewareURL}/login?provider=${provider}&account_id=etendo_123&redirect_uri=${redirectUri}web/com.etendoerp.etendorx/LinkAuth0Account.html`;
          window.location.href = url;
        }
      }));
    });

    layout.addMember(iconLayout);
    return layout;
  }

  isc.OBUserProfile.addProperties({
    createProfileForm: function() {
      let me = this,
        profileFormLayout,
        buttonLayout,
        profileForm,
        comboBoxFieldProperties,
        roleField,
        orgField,
        warehouseField,
        languageField,
        checkboxFieldProperties,
        defaultField,
        clientField,
        textFieldProperties;

      if (OB.User.isPortal && !this.showProfileFormInPortal) {
        return false;
      }

      // create a default form field types
      comboBoxFieldProperties = {
        errorOrientation: OB.Styles.OBFormField.DefaultComboBox.errorOrientation,
        cellStyle: OB.Styles.OBFormField.DefaultComboBox.cellStyle,
        titleStyle: OB.Styles.OBFormField.DefaultComboBox.titleStyle,
        textBoxStyle: OB.Styles.OBFormField.DefaultComboBox.textBoxStyle,
        pendingTextBoxStyle:
          OB.Styles.OBFormField.DefaultComboBox.pendingTextBoxStyle,
        controlStyle: OB.Styles.OBFormField.DefaultComboBox.controlStyle,
        width: '*',
        pickListBaseStyle:
          OB.Styles.OBFormField.DefaultComboBox.pickListBaseStyle,
        pickListTallBaseStyle:
          OB.Styles.OBFormField.DefaultComboBox.pickListTallBaseStyle,
        pickerIconSrc: OB.Styles.OBFormField.DefaultComboBox.pickerIconSrc,

        height: OB.Styles.OBFormField.DefaultComboBox.height,
        pickerIconWidth: OB.Styles.OBFormField.DefaultComboBox.pickerIconWidth,
        pickListCellHeight:
          OB.Styles.OBFormField.DefaultComboBox.pickListCellHeight,
        pickListProperties: {
          bodyStyleName:
            OB.Styles.OBFormField.DefaultComboBox.pickListProperties.bodyStyleName
        },

        // workaround for this issue:
        // https://issues.openbravo.com/view.php?id=18501
        setUpPickList: function() {
          this.Super('setUpPickList', arguments);
          if (this.pickList) {
            this.pickList.setBodyStyleName(this.pickListProperties.bodyStyleName);
          }
        },

        titleOrientation: 'top',
        showFocused: true,
        editorType: 'select',
        selectOnFocus: true,
        addUnknownValues: false,
        allowEmptyValue: false,
        defaultToFirstOption: true,

        // to solve: https://issues.openbravo.com/view.php?id=20067
        // in chrome the order of the valueMap object is not retained
        // the solution is to keep a separate entries array with the
        // records in the correct order, see also the setEntries
        // method
        getClientPickListData: function() {
          if (this.entries) {
            return this.entries;
          }
          return this.Super('getClientPickListData', arguments);
        },

        setEntries: function(entries) {
          let length = entries.length,
            i,
            id,
            identifier,
            valueField = this.getValueFieldName(),
            valueMap = {};
          this.entries = [];
          for (i = 0; i < length; i++) {
            id = entries[i][OB.Constants.ID] || '';
            identifier = entries[i][OB.Constants.IDENTIFIER] || '';
            valueMap[id] = identifier.convertTags();
            this.entries[i] = {};
            this.entries[i][valueField] = id;
          }
          this.setValueMap(valueMap);
        }
      };

      roleField = isc.addProperties(
        {
          name: 'role',
          title: OB.I18N.getLabel('UINAVBA_Role'),
          sortField: 'role'
        },
        comboBoxFieldProperties
      );

      orgField = isc.addProperties(
        {
          name: 'organization',
          title: OB.I18N.getLabel('UINAVBA_Organization'),
          sortField: 'organization'
        },
        comboBoxFieldProperties
      );

      warehouseField = isc.addProperties(
        {
          name: 'warehouse',
          title: OB.I18N.getLabel('UINAVBA_Warehouse'),
          sortField: 'warehouse'
        },
        comboBoxFieldProperties
      );

      languageField = isc.addProperties(
        {
          name: 'language',
          title: OB.I18N.getLabel('UINAVBA_Language'),
          sortField: 'language'
        },
        comboBoxFieldProperties
      );

      checkboxFieldProperties = {
        cellStyle: OB.Styles.OBFormField.DefaultCheckbox.cellStyle,
        titleStyle: OB.Styles.OBFormField.DefaultCheckbox.titleStyle,
        textBoxStyle: OB.Styles.OBFormField.DefaultCheckbox.textBoxStyle,
        showValueIconOver:
          OB.Styles.OBFormField.DefaultCheckbox.showValueIconOver,
        showValueIconFocused:
          OB.Styles.OBFormField.DefaultCheckbox.showValueIconFocused,
        showFocused: OB.Styles.OBFormField.DefaultCheckbox.showFocused,
        defaultValue: OB.Styles.OBFormField.DefaultCheckbox.defaultValue,
        checkedImage: OB.Styles.OBFormField.DefaultCheckbox.checkedImage,
        uncheckedImage: OB.Styles.OBFormField.DefaultCheckbox.uncheckedImage,
        titleOrientation: 'right',
        editorType: 'checkbox'
      };

      defaultField = isc.addProperties(
        {
          name: 'default',
          title: OB.I18N.getLabel('UINAVBA_SetAsDefault')
        },
        checkboxFieldProperties
      );

      textFieldProperties = {
        errorOrientation: OB.Styles.OBFormField.DefaultTextItem.errorOrientation,
        cellStyle: OB.Styles.OBFormField.DefaultTextItem.cellStyle,
        titleStyle: OB.Styles.OBFormField.DefaultTextItem.titleStyle,
        textBoxStyle: OB.Styles.OBFormField.DefaultTextItem.textBoxStyle,
        showFocused: true,
        showDisabled: true,
        disabled: true,
        showIcons: false,
        width: '*',
        titleOrientation: 'top',
        editorType: 'TextItem'
      };

      clientField = isc.addProperties(
        {
          name: 'client',
          title: OB.I18N.getLabel('UINAVBA_Client')
        },
        textFieldProperties
      );

      // create the form for the role information
      profileForm = isc.DynamicForm.create({
        autoFocus: true,
        overflow: 'visible',
        numCols: 1,
        width: '100%',
        titleSuffix: '',
        errorsPreamble: '',
        showInlineErrors: false,
        widgetInstance: me,

        initWidget: function() {
          this.Super('initWidget', arguments);
          this.setInitialData(this.widgetInstance.formData);
        },

        itemKeyPress: function(item, keyName, characterValue) {
          if (keyName === 'Escape') {
            if (isc.OBQuickRun.currentQuickRun) {
              isc.OBQuickRun.currentQuickRun.doHide();
            }
          }

          this.Super('itemKeyPress', arguments);
        },

        localFormData: null,
        reset: function() {
          // note order is important, first order item then do ValueMaps
          // then do setValues
          // this is needed because the select items will reject values
          // if the valuemap is not yet set
          this.setValue('role', this.localFormData.initialValues.role);
          this.setOtherEntries();
          // note, need to make a copy of the initial values
          // otherwise they are updated when the form values change!
          this.setValues(isc.addProperties({}, this.localFormData.initialValues));
          this.setWarehouseValueMap();
          //We set initial values again to set warehouse correctly
          this.setValues(isc.addProperties({}, this.localFormData.initialValues));
          if (
            this.getItem('warehouse').getClientPickListData().length > 0 &&
            !this.getItem('warehouse').getValue()
          ) {
            this.getItem('warehouse').moveToFirstValue();
          }
        },
        setInitialData: function(data) {
          // order of these statements is important see comments in reset
          // function
          this.localFormData = data;
          this.getItem('language').setEntries(data.language.valueMap);
          this.getItem('role').setEntries(data.role.valueMap);
          this.setValue('role', data.initialValues.role);
          this.setValue('client', data.initialValues.client);
          this.setOtherEntries();
          //First we set initial values, but warehouse will not work
          //as its combo hasn't yet been filled
          this.setValues(isc.addProperties({}, data.initialValues));
          this.setWarehouseValueMap();
          //We set initial values again to set warehouse correctly
          this.setValues(isc.addProperties({}, data.initialValues));
        },
        // updates the dependent combos
        itemChanged: function(item, newValue) {
          this.setOtherEntries();
          if (item.name === 'role') {
            if (this.getItem('organization').getClientPickListData().length > 0) {
              this.getItem('organization').moveToFirstValue();
            }
          }
          this.setWarehouseValueMap();
          if (
            item.name !== 'warehouse' &&
            item.name !== 'default' &&
            item.name !== 'language'
          ) {
            if (this.getItem('warehouse').getClientPickListData().length > 0) {
              this.getItem('warehouse').moveToFirstValue();
            }
          }
        },
        setOtherEntries: function() {
          let i,
            role,
            roleId = this.getValue('role'),
            length = this.localFormData.role.roles.length;
          for (i = 0; i < length; i++) {
            role = this.localFormData.role.roles[i];
            if (role.id === roleId) {
              this.getItem('organization').setEntries(role.organizationValueMap);
              this.setValue('client', role.client);
              break;
            }
          }
        },
        setWarehouseValueMap: function() {
          let i,
            j,
            warehouseOrg,
            role,
            roleId,
            roleLength,
            length,
            orgId = this.getItem('organization').getValue();
          if (!orgId) {
            return;
          }
          roleLength = this.localFormData.role.roles.length;
          roleId = this.getValue('role');
          for (i = 0; i < roleLength; i++) {
            role = this.localFormData.role.roles[i];
            if (role.id === roleId) {
              length = role.warehouseOrgMap.length;
              for (j = 0; j < length; j++) {
                warehouseOrg = role.warehouseOrgMap[j];
                if (warehouseOrg.orgId === orgId) {
                  this.getItem('warehouse').setEntries(warehouseOrg.warehouseMap);
                  return;
                }
              }
            }
          }
        },

        // call the server to save the information
        doSave: function() {
          OB.User.loggingIn = true;
          OB.RemoteCallManager.call(
            this.widgetInstance.formActionHandler,
            this.getValues(),
            {
              command: 'save'
            },
            this.doSaveCallback
          );
        },

        // and reload
        doSaveCallback: function(rpcResponse, data, rpcRequest) {
          delete OB.User.loggingIn;
          // if not success then an error, can not really occur
          // is handled as an exception is returned anyway
          if (data.result === OB.Constants.SUCCESS) {
            // reload the window to reflect the changed role etc.
            window.location.href = OB.Utilities.getLocationUrlWithoutFragment();
          }
        },

        fields: [
          roleField,
          clientField,
          orgField,
          warehouseField,
          languageField,
          defaultField
        ]
      });

      // create the form layout which contains both the form and the buttons
      profileFormLayout = isc.VStack.create({
        align: 'center',
        overflow: 'visible',
        height: 1,
        width: '100%'
      });
      profileFormLayout.addMembers(profileForm);

      // create the buttons
      buttonLayout = isc.HStack.create({
        layoutTopMargin: 10,
        membersMargin: 10,
        align: 'center',
        overflow: 'visible',
        height: 1,
        width: 248
      });
      if (isc.Page.isRTL()) {
        //HACK: in RTL mode this width is higher than in LTR (Even with width: '100%'). Manual set to a lower value.
        buttonLayout.width = 160;
      }

      buttonLayout.addMembers(
        isc.OBFormButton.create({
          title: OB.I18N.getLabel('OBUIAPP_Apply'),
          click: function() {
            isc.OBQuickRun.currentQuickRun.doHide();
            profileForm.doSave();
          }
        })
      );
      buttonLayout.addMembers(
        isc.OBFormButton.create({
          title: OB.I18N.getLabel('UINAVBA_Cancel'),
          click: isc.OBQuickRun.hide
        })
      );

      let ssoButtonLayout = isc.VStack.create({
        layoutTopMargin: 6,
        align: 'center',
        width: '100%',
        height: 1
      });

    function loadSSOComponents(callback) {
      OB.RemoteCallManager.call(
        'com.etendoerp.etendorx.GetSSOProperties',
        { properties: 'auth.type, domain.url, client.id, middleware.url' },
        {},
        function (response, data) {
          if (data.message?.severity === 'error') {
            this.getWindow().showMessage(data.message.text);
            return;
          }
          callback(data);
        }
      );
    }

    loadSSOComponents(function (data) {
      console.log('SSO Data:', data);
      const ssoType = data['authtype'];
      const middlewareUrl = data['middlewareurl'];
      const domainUrl = data['domainurl'];
      const clientId = data['clientid'];
      if (ssoType === 'Auth0') {
        ssoButtonLayout.addMember(createAuth0LinkButton(domainUrl, clientId));
      } else {
        ssoButtonLayout.addMember(createProviderIconLayout(middlewareUrl));
      }
    });

      profileFormLayout.addMembers(buttonLayout);
      profileFormLayout.addMember(ssoButtonLayout);

      // pointer to the form
      this.profileForm = profileForm;
      this.profileFormLayout = profileFormLayout;

      OB.TestRegistry.register(
        'org.openbravo.client.application.navigationbarcomponents.UserProfileRole.RoleField',
        profileForm.getField('role')
      );
      OB.TestRegistry.register(
        'org.openbravo.client.application.navigationbarcomponents.UserProfileRole.OrgField',
        profileForm.getField('organization')
      );
      OB.TestRegistry.register(
        'org.openbravo.client.application.navigationbarcomponents.UserProfileRole.WarehouseField',
        profileForm.getField('warehouse')
      );
      OB.TestRegistry.register(
        'org.openbravo.client.application.navigationbarcomponents.UserProfileRole.LanguageField',
        profileForm.getField('language')
      );
      OB.TestRegistry.register(
        'org.openbravo.client.application.navigationbarcomponents.UserProfileRole.DefaultField',
        profileForm.getField('default')
      );
      OB.TestRegistry.register(
        'org.openbravo.client.application.navigationbarcomponents.UserProfileRole.ClientField',
        profileForm.getField('client')
      );
      OB.TestRegistry.register(
        'org.openbravo.client.application.navigationbarcomponents.UserProfileRole.Form',
        profileForm
      );
      OB.TestRegistry.register(
        'org.openbravo.client.application.navigationbarcomponents.UserProfileRole.SaveButton',
        buttonLayout.members[0]
      );
      OB.TestRegistry.register(
        'org.openbravo.client.application.navigationbarcomponents.UserProfileRole.CancelButton',
        buttonLayout.members[1]
      );
    }
  });
}
