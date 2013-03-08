/*
 * Copyright (c) 2007-2012 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package org.sonatype.sisu.siesta.server;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.sisu.siesta.common.Component;
import org.sonatype.sisu.siesta.common.error.ErrorXO;

/**
 * Support for {@link ExceptionMapper} implementations.
 *
 * @since 1.0
 */
public abstract class ExceptionMapperSupport<E extends Throwable>
    implements Component, ExceptionMapper<E>
{

    protected final Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    private Provider<Request> requestProvider;

    private String generateId() {
        return UUID.randomUUID().toString();
    }

    public Response toResponse( final E exception )
    {
        //noinspection ThrowableResultOfMethodCallIgnored
        checkNotNull( exception );

        final String id = generateId();
        if ( log.isTraceEnabled() )
        {
            log.trace( "(ID {}) Mapping exception: " + exception, id, exception );
        }
        else
        {
            log.debug( "(ID {}) Mapping exception: " + exception, id );
        }

        Response response;
        try
        {
            response = convert( exception, id );
        }
        catch ( Exception e )
        {
            log.warn( "(ID {}) Failed to map exception", id, e );
            response = Response.serverError().entity( new ErrorXO().withId( id ) ).build();
        }

        final Object entity = response.getEntity();
        log.warn(
            "(ID {}) Response: [{}] {}", id, response.getStatus(), entity == null ? "(no entity/body)" : entity
        );

        return response;
    }

    protected abstract Response convert( final E exception, final String id );

    protected Request getRequest()
    {
        return requestProvider.get();
    }

}
