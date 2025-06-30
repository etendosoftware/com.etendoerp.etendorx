    (function () {
      var configEtendoMiddleware = {
        action: function(){
          var callback, i, view = this.view, grid = view.viewGrid, selectedRecords = grid.getSelectedRecords();

          // define the callback function which shows the result to the user
          callback = function(rpcResponse, data, rpcRequest) {
            if ('success' === data.severity) {
              isc.say(data.text + '<br>' + 'Refresh the grid to see the changes.');
            } else {
              isc.say(data.text)
            }
          }

          // and call the server
          OB.RemoteCallManager.call('com.etendoerp.etendorx.actionhandler.ConfigEtendoMiddleware',
          {},
          {
            envURL: OB.Utilities.getLocationUrlWithoutFragment()
          },
          callback,
          {refreshGrid: true});
        },
        updateState: function() {
          this.setDisabled(false);
        },
        buttonType: 'etrx_config_middleware',
        prompt: OB.I18N.getLabel('ETRX_Config_Middleware'),
      };

      // register the button for the RX Config tab
      // the first parameter is a unique identification so that one button can not be registered multiple times.
      OB.ToolbarRegistry.registerButton(configEtendoMiddleware.buttonType, isc.OBToolbarIconButton, configEtendoMiddleware, 150, 'FBC953B05883490DAA092C9E29BA58EB');
    }());