    (function () {
      var initRXServButtonProps = {
        action: function(){
          var callback, i, view = this.view, grid = view.viewGrid, selectedRecords = grid.getSelectedRecords();

          // define the callback function which shows the result to the user
          callback = function(rpcResponse, data, rpcRequest) {
            isc.say(data.text + '<br>' + 'Refresh the grid to see the changes.');
          }

          // and call the server
          OB.RemoteCallManager.call('com.etendoerp.etendorx.actionhandler.InitializeRXServices', {}, {}, callback, {refreshGrid: true});
        },
        updateState: function() {
          this.setDisabled(false);
        },
        buttonType: 'etrx_init_services',
        prompt: OB.I18N.getLabel('ETRX_Init_Services'),
      };

      // register the button for the RX Config tab
      // the first parameter is a unique identification so that one button can not be registered multiple times.
      OB.ToolbarRegistry.registerButton(initRXServButtonProps.buttonType, isc.OBToolbarIconButton, initRXServButtonProps, 150, '157BE3AB99E6403592DE2F84BFA1B29F');
    }());