package com.diffplug.spotless.extra.eclipse.wtp;

import org.eclipse.emf.common.util.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.wst.common.uriresolver.internal.provisional.URIResolverExtension;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * The URI resolver extension  
 */
public class PreventExternalURIResolverExtension implements URIResolverExtension, BundleActivator {

	private final String REFUSE_EXTERNAL_URI="file://refused.external.uri";
	
	/**
	 * @param file the in-workspace base resource, if one exists
	 * @param baseLocation - the location of the resource that contains the uri
	 * @param publicId - an optional public identifier (i.e. namespace name), or null if none
	 * @param systemId - an absolute or relative URI, or null if none 
	 * 
	 * @return an absolute URI or null if this extension can not resolve this reference
	 */	@Override
	public String resolve(IFile file, String baseLocation, String publicId, String systemId) {
		if(null != systemId) {
			try {
				URI proposalByPreviousResolver = org.eclipse.emf.common.util.URI.createURI(systemId);
				String host = proposalByPreviousResolver.host();
				/*
				 * The host is empty (not null) 
				 */
				if(!(null == host || host.isEmpty()) ) {
					return REFUSE_EXTERNAL_URI;
				}
			}
			catch(IllegalArgumentException ignore) {
				//If it is no a valid URI, there is nothing to do here.
			}
		}
		return null; //Don't alter the proposal of previous resolver extensions by proposing something else
	}

	@Override
	public void start(BundleContext context) throws Exception {
		//Nothing to do. The bundle-activator interface only allows to load this extension as a stand-alone plugin.
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		//Nothing to do. The bundle-activator interface only allows to load this extension as a stand-alone plugin.
	}

}
