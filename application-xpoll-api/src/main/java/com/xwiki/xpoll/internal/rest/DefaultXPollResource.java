/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xwiki.xpoll.internal.rest;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.resources.pages.ModifiablePageResource;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xwiki.xpoll.XPollException;
import com.xwiki.xpoll.XPollManager;
import com.xwiki.xpoll.rest.model.jaxb.Vote;
import com.xwiki.xpoll.rest.XPollResource;

/**
 * Default implementation of {@link XPollResource}.
 *
 * @version $Id$
 * @since 2.1
 */
@Component
@Named("com.xwiki.xpoll.internal.rest.DefaultXPollResource")
@Singleton
public class DefaultXPollResource extends ModifiablePageResource implements XPollResource
{
    @Inject
    private XPollManager xPollManager;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @Override
    public Response vote(String wikiName, String spaces, String pageName, Vote vote)
        throws XWikiRestException
    {
        DocumentReference documentReference = new DocumentReference(pageName, getSpaceReference(spaces, wikiName));

        if (!contextualAuthorizationManager.hasAccess(Right.EDIT, documentReference)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        XWikiContext context = getXWikiContext();
        try {
            DocumentReference userReference = context.getUserReference();
            xPollManager.vote(documentReference, userReference, vote.getProposals());
            return Response.ok().build();
        } catch (XPollException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e).build();
        }
    }

    // This function is copied from the XWikiResource version 14.0 because it is needed to solve the following issue
    // https://jira.xwiki.org/browse/XWIKI-18782 . The function shall be removed once we upgrade the parent to 13.10.
    @Override
    public List<String> parseSpaceSegments(String spaceSegments) throws XWikiRestException
    {
        // The URL format is: "spaces/A/spaces/B/spaces/C" to actually point to the space "A.B.C".
        List<String> spaces = new ArrayList<>();
        // We actually don't get the first "spaces/" segment so we start from the first space.
        int i = 1;
        for (String space : spaceSegments.split("/")) {
            if (i++ % 2 == 0) {
                // Every 2 segments, we should have "spaces". If not, the URL is malformed
                if (!"spaces".equals(space)) {
                    throw new XWikiRestException("Malformed URL: the spaces section is invalid.");
                }
            } else if (!space.isEmpty()) {
                try {
                    spaces.add(URLDecoder.decode(space, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    // Shouldn't happen.
                    throw new XWikiRestException("Unable to decode space name.", e);
                }
            } else {
                throw new XWikiRestException("Malformed URL: a space name cannot be empty.");
            }
        }
        return spaces;
    }
}
