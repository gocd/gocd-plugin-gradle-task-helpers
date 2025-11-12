package org.gocd.plugin.taskhelpers.test;

import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

@Extension
public class GoPluginImpl implements GoPlugin {

    @Override
    public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {
        // ignore
    }

    @Override
    public GoPluginApiResponse handle(GoPluginApiRequest goPluginApiRequest) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public GoPluginIdentifier pluginIdentifier() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
