/*
 * Copyright 2012 Jeanfrancois Arcand
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.atmosphere.interceptor;

import org.atmosphere.cpr.Action;
import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.AtmosphereInterceptor;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResourceEventListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.atmosphere.cpr.ApplicationConfig.*;

/**
 * This {@link AtmosphereInterceptor} automatically suspend the {@link AtmosphereResource} and take care of
 * managing the response's state (flusing, resuming, etc.). When used, call to {@link AtmosphereResource#suspend}
 * aren't necessary. By default AtmosphereResource are suspended when a GET is received. You can change that
 * value by configuring {@link org.atmosphere.cpr.ApplicationConfig#ATMOSPHERERESOURCE_INTERCEPTOR_METHOD}
 * <p/>
 * Use this class when you don't want to manage the suspend/resume operation from your Atmosphere's API implementation
 * ({@link org.atmosphere.cpr.AtmosphereHandler}, {@link org.atmosphere.websocket.WebSocketHandler},
 * {@link org.atmosphere.cpr.Meteor} or extension like GWT, Jersey, Wicket
 * etc.
 * <br/>
 * <strong>The client must set the {@link org.atmosphere.cpr.HeaderConfig#X_ATMOSPHERE_TRANSPORT} header for to make
 * this mechanism to work properly.</strong>
 *
 * @author Jeanfrancois Arcand
 */
public class AtmosphereResourceLifecycleInterceptor implements AtmosphereInterceptor {

    private String method = "GET";
    private static final Logger logger = LoggerFactory.getLogger(SSEAtmosphereInterceptor.class);

    @Override
    public void configure(AtmosphereConfig config) {
        String s = config.getInitParameter(ATMOSPHERERESOURCE_INTERCEPTOR_METHOD);
        if (s != null) {
            method = s;
        }
    }

    /**
     * Automatically suspend the {@link AtmosphereResource} based on {@link AtmosphereResource.TRANSPORT} value.
     *
     * @param r a {@link AtmosphereResource}
     * @return
     */
    @Override
    public Action inspect(AtmosphereResource r) {
        switch (r.transport()) {
            case JSONP:
            case AJAX:
            case LONG_POLLING:
                r.resumeOnBroadcast(true);
                break;
            default:
                break;
        }
        return Action.CONTINUE;
    }

    @Override
    public void postInspect(final AtmosphereResource r) {

        if (r.getRequest().getMethod().equalsIgnoreCase(method)) {
            r.addEventListener(new AtmosphereResourceEventListenerAdapter() {
                @Override
                public void onBroadcast(AtmosphereResourceEvent event) {
                    switch (r.transport()) {
                        case JSONP:
                        case AJAX:
                        case LONG_POLLING:
                            break;
                        default:
                            try {
                                r.getResponse().flushBuffer();
                            } catch (IOException e) {
                                logger.warn("", e);
                            }
                            break;
                    }
                }
            }).suspend();
        }
    }

    public String toString() {
        return "Atmosphere LifeCycle";
    }
}
