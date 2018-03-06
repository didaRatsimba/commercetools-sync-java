package com.commercetools.sync.products.helpers.productreferenceresolver;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductReferenceResolver;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.ProductTypeService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.producttypes.ProductType;
import org.junit.Before;
import org.junit.Test;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockChannelService;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockSupplyChannel;
import static com.commercetools.sync.products.ProductSyncMockUtils.getBuilderWithProductTypeRef;
import static com.commercetools.sync.products.ProductSyncMockUtils.getBuilderWithProductTypeRefId;
import static com.commercetools.sync.products.ProductSyncMockUtils.getBuilderWithRandomProductTypeUuid;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockProductService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockProductTypeService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockStateService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockTaxCategoryService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductTypeReferenceResolverTest {
    private static final String CHANNEL_KEY = "channel-key_1";
    private static final String CHANNEL_ID = "1";
    private static final String PRODUCT_TYPE_ID = "productTypeId";
    private static final String TAX_CATEGORY_ID = "taxCategoryId";
    private static final String STATE_ID = "stateId";
    private static final String PRODUCT_ID = "productId";

    private ProductTypeService productTypeService;
    private ProductReferenceResolver referenceResolver;

    /**
     * Sets up the services and the options needed for reference resolution.
     */
    @Before
    public void setup() {
        productTypeService = getMockProductTypeService(PRODUCT_TYPE_ID);
        final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                        .build();
        referenceResolver = new ProductReferenceResolver(syncOptions, productTypeService, mock(CategoryService.class),
            getMockTypeService(), getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)),
            getMockTaxCategoryService(TAX_CATEGORY_ID), getMockStateService(STATE_ID),
            getMockProductService(PRODUCT_ID));
    }

    @Test
    public void resolveProductTypeReference_WithKeysAsUuidSetAndAllowed_ShouldResolveReference() {
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .allowUuidKeys(true)
                                                                               .build();

        final ProductReferenceResolver productReferenceResolver = new ProductReferenceResolver(productSyncOptions,
            productTypeService, mock(CategoryService.class), getMockTypeService(),
            getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY)),
            getMockTaxCategoryService(TAX_CATEGORY_ID), getMockStateService(STATE_ID),
            getMockProductService(PRODUCT_ID));

        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductTypeUuid();

        final ProductDraftBuilder resolvedDraft = productReferenceResolver.resolveProductTypeReference(productBuilder)
                                                                          .toCompletableFuture().join();

        assertThat(resolvedDraft.getProductType()).isNotNull();
        assertThat(resolvedDraft.getProductType().getId()).isEqualTo(PRODUCT_TYPE_ID);
    }

    @Test
    public void resolveProductTypeReference_WithKeys_ShouldResolveReference() {
        final ProductDraftBuilder productBuilder = getBuilderWithProductTypeRefId("productTypeKey");

        final ProductDraftBuilder resolvedDraft = referenceResolver.resolveProductTypeReference(productBuilder)
                                                                   .toCompletableFuture().join();

        assertThat(resolvedDraft.getProductType()).isNotNull();
        assertThat(resolvedDraft.getProductType().getId()).isEqualTo(PRODUCT_TYPE_ID);
    }

    @Test
    public void resolveProductTypeReference_WithKeysAsUuidSetAndNotAllowed_ShouldNotResolveReference() {
        final ProductDraftBuilder productBuilder = getBuilderWithRandomProductTypeUuid();

        assertThat(referenceResolver.resolveProductTypeReference(productBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve product type reference on ProductDraft"
                + " with key:'" + productBuilder.getKey() + "'. Reason: Found a UUID"
                + " in the id field. Expecting a key without a UUID value. If you want to"
                + " allow UUID values for reference keys, please use the "
                + "allowUuidKeys(true) option in the sync options.");
    }

    @Test
    public void resolveProductTypeReference_WithNonExistentProductType_ShouldNotResolveReference() {
        final ProductDraftBuilder productBuilder = getBuilderWithProductTypeRefId("anyKey")
            .key("dummyKey");

        when(productTypeService.fetchCachedProductTypeId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThat(referenceResolver.resolveProductTypeReference(productBuilder).toCompletableFuture())
            .hasNotFailed()
            .isCompletedWithValueMatching(resolvedDraft ->
                Objects.nonNull(resolvedDraft.getProductType())
                    && Objects.equals(resolvedDraft.getProductType().getId(), "anyKey"));
    }

    @Test
    public void resolveProductTypeReference_WithNullIdOnProductTypeReference_ShouldNotResolveReference() {
        final ProductDraftBuilder productBuilder = getBuilderWithProductTypeRef(
            Reference.of(ProductType.referenceTypeId(), (String)null))
            .key("dummyKey");

        assertThat(referenceResolver.resolveProductTypeReference(productBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve product type reference on ProductDraft"
                + " with key:'" + productBuilder.getKey() + "'. Reason: The value of 'id' field of the Resource"
                + " Identifier is blank (null/empty).");
    }

    @Test
    public void resolveProductTypeReference_WithEmptyIdOnProductTypeReference_ShouldNotResolveReference() {
        final ProductDraftBuilder productBuilder = getBuilderWithProductTypeRefId("")
            .key("dummyKey");

        assertThat(referenceResolver.resolveProductTypeReference(productBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve product type reference on ProductDraft"
                + " with key:'" + productBuilder.getKey() + "'. Reason: The value of 'id' field of the Resource"
                + " Identifier is blank (null/empty).");
    }

    @Test
    public void resolveProductTypeReference_WithExceptionOnProductTypeFetch_ShouldNotResolveReference() {
        final ProductDraftBuilder productBuilder = getBuilderWithProductTypeRefId(PRODUCT_TYPE_ID)
            .key("dummyKey");

        final CompletableFuture<Optional<String>> futureThrowingSphereException = new CompletableFuture<>();
        futureThrowingSphereException.completeExceptionally(new SphereException("CTP error on fetch"));
        when(productTypeService.fetchCachedProductTypeId(anyString())).thenReturn(futureThrowingSphereException);

        assertThat(referenceResolver.resolveProductTypeReference(productBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(SphereException.class)
            .hasMessageContaining("CTP error on fetch");
    }
}
