package com.commercetools.sync.commons.utils;

import io.sphere.sdk.client.SolutionInfo;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;

public class SyncSolutionInfo extends SolutionInfo {
    /**
     * Extends {@link SolutionInfo} class of the JVM SDK to append to the User-Agent header with information of the
     * commercetools-sync-java library
     *
     * <p>A User-Agent header with a solution information looks like this:
     * {@code commercetools-jvm-sdk/1.4.1 (AHC/2.0) Java/1.8.0_92-b14 (Mac OS X; x86_64)
     * {@code implementationTitle}/{@code implementationVersion}
     * (+https://github.com/commercetools/commercetools-sync-java; +ps-dev@commercetools.com)}</p>
     *
     */
    public SyncSolutionInfo() {
        final String libraryName = "commercetools-sync-java";
        final String implementationTitle = ofNullable(getClass().getPackage().getImplementationTitle())
            .orElse(libraryName);
        final String implementationVersion = ofNullable(getClass().getPackage().getImplementationVersion())
            .orElse("DEBUG-VERSION");
        setName(implementationTitle);
        setVersion(implementationVersion);
        setWebsite(format("https://github.com/commercetools/%s", libraryName));
        setEmergencyContact("ps-dev@commercetools.com");
    }
}