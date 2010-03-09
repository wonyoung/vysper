/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.apache.vysper.xmpp.server;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.state.resourcebinding.BindException;
import org.apache.vysper.xmpp.uuid.JVMBuiltinUUIDGenerator;
import org.apache.vysper.xmpp.uuid.UUIDGenerator;
import org.apache.vysper.xmpp.writer.StanzaWriter;

import java.util.HashMap;
import java.util.Map;

/**
 * provides default session context behavior
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public abstract class AbstractSessionContext implements SessionContext {

    protected ServerRuntimeContext serverRuntimeContext;
    protected String sessionId;
    protected String xmlLang;
    protected UUIDGenerator sequence = new JVMBuiltinUUIDGenerator();
    protected SessionStateHolder sessionStateHolder; // be secure: do not provide this via a getter or other means
    protected Entity serverEntity;
    private Entity initiatingEntity;
    private boolean serverToServer = false;
    private Map<String, Object> attributeMap = new HashMap<String, Object>();

    public AbstractSessionContext(ServerRuntimeContext serverRuntimeContext, SessionStateHolder sessionStateHolder) {
        this.serverRuntimeContext = serverRuntimeContext;
        sessionId = serverRuntimeContext.getNextSessionId();
        serverEntity = serverRuntimeContext.getServerEnitity();
        xmlLang = serverRuntimeContext.getDefaultXMLLang();
        this.sessionStateHolder = sessionStateHolder;
    }

    @Override
    public String toString() {
        return sessionId;
    }

    public ServerRuntimeContext getServerRuntimeContext() {
        return serverRuntimeContext;
    }

    public boolean isRemotelyInitiatedSession() {
        return true;
    }

    public Entity getInitiatingEntity() {
        return initiatingEntity;
    }

    public void setInitiatingEntity(Entity entity) {
        if (entity == null) throw new IllegalArgumentException("initiating entity must not be set to NULL");
        if (entity.isResourceSet()) throw new IllegalArgumentException("initiating entity must be bare JID");
        this.initiatingEntity = entity;
    }

    public boolean isServerToServer() {
        return serverToServer;
    }

    public void setServerToServer() {
        serverToServer = true;
    }

    public void setClientToServer() {
        serverToServer = false;
    }

    public SessionState getState() {
        return sessionStateHolder.getState();
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getXMLLang() {
        return xmlLang;
    }

    public void setXMLLang(String languageCode) {
        // TODO think about disallow changing the xmlLang a second time after from default to client value
        xmlLang = languageCode;
    }

    public void endSession(SessionTerminationCause terminationCause) {
        StanzaWriter stanzaWriter = getResponseWriter();
        stanzaWriter.close();
        sessionStateHolder.setState(SessionState.CLOSED); // no more traffic going through
        getServerRuntimeContext().getResourceRegistry().unbindSession(this);
        // TODO send unavailable to all contacts and other resources
        // TODO remove latest availability from LatestPresenceCache
        // TODO close underlying transport (TCP socket)
    }

    public Entity getServerJID() {
        return serverEntity;
    }

    public String bindResource() throws BindException {

        // TODO we should impose a hard limit on the number of bound resources per session (in ServerConfiguration)
        // TODO to avoid DoS attacks based on resource binding and to shield against clients running berserk
        return getServerRuntimeContext().getResourceRegistry().bindSession(this);
    }

    /**
     * creates a unique ID, possibly a UUID, mostly for use as an IQ id.
     * @return unique sequence ID
     */
    public String nextSequenceValue() {
    	return sequence.create();
    }

    public Object putAttribute(String key, Object value) {
        return attributeMap.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributeMap.get(key);
    }
}