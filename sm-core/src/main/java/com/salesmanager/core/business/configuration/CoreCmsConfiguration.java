package com.salesmanager.core.business.configuration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.salesmanager.core.business.modules.cms.content.StaticContentFileManagerImpl;
import com.salesmanager.core.business.modules.cms.content.infinispan.CmsStaticContentFileManagerImpl;
import com.salesmanager.core.business.modules.cms.impl.DownloadCacheManagerImpl;
import com.salesmanager.core.business.modules.cms.impl.LocalCacheManagerImpl;
import com.salesmanager.core.business.modules.cms.impl.CMSManager;
import com.salesmanager.core.business.modules.cms.impl.S3CacheManagerImpl;
import com.salesmanager.core.business.modules.cms.impl.StaticContentCacheManagerImpl;
import com.salesmanager.core.business.modules.cms.impl.StoreCacheManagerImpl;
import com.salesmanager.core.business.modules.cms.product.ProductAssetsManager;
import com.salesmanager.core.business.modules.cms.product.ProductFileManagerImpl;
import com.salesmanager.core.business.modules.cms.product.infinispan.CmsImageFileManagerImpl;
import com.salesmanager.core.business.modules.cms.content.ContentAssetsManager;

@Configuration
public class CoreCmsConfiguration {

  @Value("${config.cms.method}")
  private String cmsMethod;

  @Value("${config.cms.store.location}")
  private String cmsStoreLocation;

  @Value("${config.cms.files.location}")
  private String cmsFilesLocation;

  @Value("${config.cms.http.path.location}")
  private String cmsHttpPathLocation;

  @Value("${config.cms.aws.bucket}")
  private String cmsAwsBucket;

  @Value("${config.cms.aws.region}")
  private String cmsAwsRegion;

  @Bean(name = "infinispanProductAssetsManager")
  public StoreCacheManagerImpl infinispanProductAssetsManager() {
    // MIGRATION NOTE: Replaced the legacy CMS XML with Java bean wiring while preserving the Infinispan store root and cache naming.
    return new StoreCacheManagerImpl(cmsStoreLocation, "product-merchant");
  }

  @Bean(name = "infinispanStaticAssetsManager")
  public StaticContentCacheManagerImpl infinispanStaticAssetsManager() {
    // MIGRATION NOTE: Replaced the legacy CMS XML with Java bean wiring while preserving the static content cache location semantics.
    return new StaticContentCacheManagerImpl(cmsFilesLocation, "store-merchant");
  }

  @Bean(name = "downloadsManager")
  public DownloadCacheManagerImpl downloadsManager() {
    // MIGRATION NOTE: Replaced the legacy CMS XML with Java bean wiring while preserving the product download cache root.
    return new DownloadCacheManagerImpl(cmsStoreLocation, "product-file");
  }

  @Bean(name = "httpdAssetsManager")
  public LocalCacheManagerImpl httpdAssetsManager() {
    // MIGRATION NOTE: Replaced the legacy CMS XML with Java bean wiring while preserving the local filesystem asset root.
    return new LocalCacheManagerImpl(cmsHttpPathLocation);
  }

  @Bean(name = "awsAssetsManager")
  public S3CacheManagerImpl awsAssetsManager() {
    // MIGRATION NOTE: Replaced the legacy CMS XML with Java bean wiring while preserving the S3 bucket and region lookup.
    return new S3CacheManagerImpl(cmsAwsBucket, cmsAwsRegion);
  }

  @Bean(name = "defaultProductAssetsManager")
  public ProductAssetsManager defaultProductAssetsManager(
      @Qualifier("infinispanProductAssetsManager") StoreCacheManagerImpl cmsManager) {
    CmsImageFileManagerImpl manager = CmsImageFileManagerImpl.getInstance();
    manager.setCacheManager(cmsManager);
    return manager;
  }

  @Bean(name = "defaultContentAssetsManager")
  public ContentAssetsManager defaultContentAssetsManager(
      @Qualifier("infinispanStaticAssetsManager") StaticContentCacheManagerImpl cmsManager) {
    // MIGRATION NOTE: Keep separate content and download managers so the XML-era store-merchant and product-file roots do not overwrite each other through the legacy singleton.
    CmsStaticContentFileManagerImpl manager = new CmsStaticContentFileManagerImpl();
    manager.setCacheManager(cmsManager);
    return manager;
  }

  @Bean(name = "defaultDownloadsManager")
  public ContentAssetsManager defaultDownloadsManager(
      @Qualifier("downloadsManager") DownloadCacheManagerImpl cmsManager) {
    // MIGRATION NOTE: Keep separate content and download managers so the XML-era store-merchant and product-file roots do not overwrite each other through the legacy singleton.
    CmsStaticContentFileManagerImpl manager = new CmsStaticContentFileManagerImpl();
    manager.setCacheManager(cmsManager);
    return manager;
  }

  @Bean(name = "httpdProductAssetsManager")
  public ProductAssetsManager httpdProductAssetsManager(
      @Qualifier("httpdAssetsManager") LocalCacheManagerImpl cmsManager) {
    com.salesmanager.core.business.modules.cms.product.local.CmsImageFileManagerImpl manager =
        com.salesmanager.core.business.modules.cms.product.local.CmsImageFileManagerImpl.getInstance();
    manager.setCacheManager(cmsManager);
    return manager;
  }

  @Bean(name = "httpdContentAssetsManager")
  public ContentAssetsManager httpdContentAssetsManager(
      @Qualifier("httpdAssetsManager") LocalCacheManagerImpl cmsManager) {
    com.salesmanager.core.business.modules.cms.content.local.CmsStaticContentFileManagerImpl manager =
        new com.salesmanager.core.business.modules.cms.content.local.CmsStaticContentFileManagerImpl();
    manager.setCacheManager(cmsManager);
    return manager;
  }

  @Bean(name = "httpdDownloadsManager")
  public ContentAssetsManager httpdDownloadsManager(
      @Qualifier("httpdAssetsManager") LocalCacheManagerImpl cmsManager) {
    com.salesmanager.core.business.modules.cms.content.local.CmsStaticContentFileManagerImpl manager =
        new com.salesmanager.core.business.modules.cms.content.local.CmsStaticContentFileManagerImpl();
    manager.setCacheManager(cmsManager);
    return manager;
  }

  @Bean(name = "awsProductAssetsManager")
  public ProductAssetsManager awsProductAssetsManager(
      @Qualifier("awsAssetsManager") CMSManager cmsManager) {
    com.salesmanager.core.business.modules.cms.product.aws.S3ProductContentFileManager manager =
        com.salesmanager.core.business.modules.cms.product.aws.S3ProductContentFileManager.getInstance();
    manager.setCmsManager(cmsManager);
    return manager;
  }

  @Bean(name = "awsContentAssetsManager")
  public ContentAssetsManager awsContentAssetsManager(
      @Qualifier("awsAssetsManager") CMSManager cmsManager) {
    com.salesmanager.core.business.modules.cms.content.aws.S3StaticContentAssetsManagerImpl manager =
        com.salesmanager.core.business.modules.cms.content.aws.S3StaticContentAssetsManagerImpl.getInstance();
    manager.setCmsManager(cmsManager);
    return manager;
  }

  @Bean(name = "awsDownloadsManager")
  public ContentAssetsManager awsDownloadsManager(
      @Qualifier("awsAssetsManager") CMSManager cmsManager) {
    com.salesmanager.core.business.modules.cms.content.aws.S3StaticContentAssetsManagerImpl manager =
        com.salesmanager.core.business.modules.cms.content.aws.S3StaticContentAssetsManagerImpl.getInstance();
    manager.setCmsManager(cmsManager);
    return manager;
  }

  @Bean(name = "productFileManager")
  public ProductFileManagerImpl productFileManager(
      @Qualifier("coreConfiguration") com.salesmanager.core.business.utils.CoreConfiguration coreConfiguration,
      @Qualifier("defaultProductAssetsManager") ProductAssetsManager defaultProductAssetsManager,
      @Qualifier("httpdProductAssetsManager") ProductAssetsManager httpdProductAssetsManager,
      @Qualifier("awsProductAssetsManager") ProductAssetsManager awsProductAssetsManager) {
    // MIGRATION NOTE: Replaced XML-selected CMS delegates with Java selection while preserving the same config.cms.method switch.
    ProductAssetsManager assetsManager = switch (cmsMethod) {
      case "httpd" -> httpdProductAssetsManager;
      case "aws", "gcp" -> awsProductAssetsManager;
      default -> defaultProductAssetsManager;
    };

    ProductFileManagerImpl productFileManager = new ProductFileManagerImpl();
    productFileManager.setUploadImage(assetsManager);
    productFileManager.setGetImage(assetsManager);
    productFileManager.setRemoveImage(assetsManager);
    productFileManager.setConfiguration(coreConfiguration);
    return productFileManager;
  }

  @Bean(name = "contentFileManager")
  public StaticContentFileManagerImpl contentFileManager(
      @Qualifier("defaultContentAssetsManager") ContentAssetsManager defaultContentAssetsManager,
      @Qualifier("httpdContentAssetsManager") ContentAssetsManager httpdContentAssetsManager,
      @Qualifier("awsContentAssetsManager") ContentAssetsManager awsContentAssetsManager) {
    // MIGRATION NOTE: Replaced XML-selected CMS delegates with Java selection while preserving the same config.cms.method switch.
    ContentAssetsManager assetsManager = switch (cmsMethod) {
      case "httpd" -> httpdContentAssetsManager;
      case "aws", "gcp" -> awsContentAssetsManager;
      default -> defaultContentAssetsManager;
    };

    StaticContentFileManagerImpl contentFileManager = new StaticContentFileManagerImpl();
    contentFileManager.setUploadFile(assetsManager);
    contentFileManager.setGetFile(assetsManager);
    contentFileManager.setRemoveFile(assetsManager);
    contentFileManager.setAddFolder(assetsManager);
    contentFileManager.setRemoveFolder(assetsManager);
    contentFileManager.setListFolder(assetsManager);
    return contentFileManager;
  }

  @Bean(name = "productDownloadsFileManager")
  public StaticContentFileManagerImpl productDownloadsFileManager(
      @Qualifier("defaultDownloadsManager") ContentAssetsManager defaultDownloadsManager,
      @Qualifier("httpdDownloadsManager") ContentAssetsManager httpdDownloadsManager,
      @Qualifier("awsDownloadsManager") ContentAssetsManager awsDownloadsManager) {
    // MIGRATION NOTE: Replaced XML-selected CMS delegates with Java selection while preserving the same config.cms.method switch.
    ContentAssetsManager assetsManager = switch (cmsMethod) {
      case "httpd" -> httpdDownloadsManager;
      case "aws", "gcp" -> awsDownloadsManager;
      default -> defaultDownloadsManager;
    };

    StaticContentFileManagerImpl productDownloadsFileManager = new StaticContentFileManagerImpl();
    productDownloadsFileManager.setUploadFile(assetsManager);
    productDownloadsFileManager.setGetFile(assetsManager);
    productDownloadsFileManager.setRemoveFile(assetsManager);
    return productDownloadsFileManager;
  }
}
